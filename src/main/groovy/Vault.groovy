import groovy.json.JsonBuilder
import groovy.json.JsonSlurper

/**
 * Vault class to wrap some of the common vault rest api's we will need for our development work
 * It is assumed that we have a clean docker image and/or one that has been initialized by these apis
 */

class Vault {

    def outputDir
    def baseUrl
    static final String keyFileName = "Vault.keys"
    static final String ROOT_TOKEN = "root_token"
    static final String UNSEAL_KEYS = "unseal_keys"


    private def getKeyFileName() {
        return "$outputDir$File.separator$keyFileName"
    }

    /**
     * Helper method to save json object to text file
     *
     * @param content - json content to be saved to a file as text.
     * @param filePath - String absolute filepath where to save the file
     */
    static save(Object content, String filePath) {
        new File(filePath).write(new JsonBuilder(content).toPrettyString())
    }

    /**
     * Helper method to load json object from a text file
     *
     * @param filePath - absolute path which specifies json text file
     * @return returns object which will be json in memory representation LazyMap typically
     */
    static Object load(String filePath) {
        return new JsonSlurper().parseText(new File(filePath).text)
    }

    /**
     * Helper method to load Vault keys from previously saved Vault.keys
     *
     * @return map containing root token and unseal keys
     */
    def getKeys() {
        def keySet = load(getKeyFileName())
        def rootToken = keySet[ROOT_TOKEN]
        def unsealKeys = keySet["keys_base64"]
        def keys = ["root_token": rootToken, "unseal_keys": unsealKeys]
        return keys
    }

    /**
     * Method to initialize vault if it can't find via api that vault has been initialized via sys/init
     * @return map containing root token and unseal keys or null in failure
     */
    def initVault() {
        def endpoint = "$baseUrl/v1/sys/init"
        def headers = ["Content-Type": "application/json"]
        def result = RestClient.get(endpoint, headers, "")
        def initialized = result["initialized"]
        if (!initialized) {
            String body = "{" +
                    "  \"secret_shares\": 10," +
                    "  \"secret_threshold\": 3" +
                    "}"
            result = RestClient.put(endpoint, headers, body)
            save(result, getKeyFileName())
            println("Vault Keys saved ")
            return getKeys()
        } else {
            println("Vault has already been initialized")
            // ok if it has been initialized we need to read keys/unseal it blah blah
            def keyFile = new File(getKeyFileName())
            if (keyFile.exists()) {
                return getKeys()
            } else {
                println(getKeyFileName() + " Does not exist")
                return null
            }
        }

    }

    /**
     * Method to Unseal vault and specifies number of keys required to unseal it
     * @param rootKey our vault root key obtained via init
     * @param unsealKeys this is a list of unseal keys
     * @param numKeysRequired how many unseal keys should we use to unseal the vault
     * @return sealed should be false if vault was unsealed
     */
    def unseal(String rootKey, def unsealKeys, int numKeysRequired) {
        def endpoint = "$baseUrl/v1/sys/unseal"
        def headers = ["Content-Type": "application/json", "X-Vault-Token": rootKey]
        def sealed = false
        for (int ctr = 0; ctr < numKeysRequired; ctr++) {
            def body = "{\"key\":\"" + unsealKeys.getAt(ctr) + '\"}'
            def result = RestClient.post(endpoint, headers, body)
            sealed = result["sealed"]
            if (!sealed) {
                break
            }
        }
        return sealed
    }

    /**
     * Method to set Vault policy for dev
     * @param rootKey our vault root key obtained via init
     * @param policyId for example dig-dev
     * @param policyFile absolute file path to policy file.  NOTE: Format is tricky so be careful
     * @return should return OK
     */

    def setPolicy(String rootKey, def policyId, def policyFile) {
        def endpoint = "$baseUrl/v1/sys/policy/$policyId" // our policy endpoint
        def headers = ["Content-Type": "application/json", "X-Vault-Token": rootKey]
        String data = new File(policyFile).text
        return RestClient.post(endpoint, headers, data)
    }

    /**
     * Set our appRole
     * @param rootKey our vault root key
     * @return OK if role was assigned or null otherwise
     */
    def setAppRole(String rootKey) {
        def endpoint = "$baseUrl/v1/sys/auth/approle" // our policy endpoint
        def headers = ["Content-Type": "application/json", "X-Vault-Token": rootKey]
        String data = '{"type":"approle"}'
        return (RestClient.post(endpoint, headers, data))
    }

    /**
     * Set our devRole
     * @param rootKey our vault root key
     * @param devRole this is the role we are tying to setup
     * @param policyid of the policy we created
     * @return OK
     */
    def setDevRole(String rootKey, def devRole, def policyId) {
        def endpoint = "$baseUrl/v1/auth/approle/role/$devRole" // our policy endpoint
        def headers = ["Content-Type": "application/json", "X-Vault-Token": rootKey]
        String data = '{"policies" : ["' + policyId + '"]}'
        return RestClient.post(endpoint, headers, data)
    }

    /**
     * Get a RoleId so we can login
     * @param rootKey our vault root key
     * @param devRole this is role we had setup
     * @return the roleid to use on appRoleLogin
     */
    def getRoleId(String rootKey, def devRole) {
        def endpoint = "$baseUrl/v1/auth/approle/role/$devRole/role-id" // our policy endpoint
        def headers = ["Content-Type": "application/json", "X-Vault-Token": rootKey]
        def roleData = RestClient.get(endpoint, headers, "")
        def data = roleData["data"]
        return data["role_id"]
    }

    /**
     * Get a Secret to be used for appRoleLogin
     * @param rootKey our vault root key
     * @param devRole this is role we had setup
     * @return returns secret
     */
    def getSecret(String rootKey, def devRole) {
        def endpoint = "$baseUrl/v1/auth/approle/role/$devRole/secret-id" // our policy endpoint
        def headers = ["Content-Type": "application/json", "X-Vault-Token": rootKey]
        def roleData = RestClient.post(endpoint, headers, "")
        def data = roleData["data"]
        return data["secret_id"]
    }

    /**
     * This will do a role based login to vault
     * @param roleId returned from getRoleId
     * @param secretId secret returned from getSecret
     * @return returns new access key client_token which is now used in header via X-Vault-Token to get secret
     */
    def appRoleLogin(String roleId, String secretId) {
        def endpoint = "$baseUrl/v1/auth/approle/login" // our policy endpoint
        def headers = ["Content-Type": "application/json"]
        def data = '{"role_id" : "' + roleId + '", "secret_id": "' + secretId + '"}'
        def roleData = RestClient.post(endpoint, headers, data)
        def auth = roleData["auth"]
        return auth["client_token"]
    }

    /**
     * Reads Secrets from a json file and writes them to vault
     * @param clientToken returned by appRoleLogin
     * @param absolute path of secrets json file 
     * @return returns secrets object that was written to vault
     */
    def writeSecrets(String clientToken, def secretDataFile) {
        def headers = ["Content-Type": "application/json", "X-Vault-Token": clientToken]
        def endpoint = "$baseUrl/v1/secret" // our policy endpoint
        def secretsData = load(secretDataFile)
        def secrets = secretsData["secrets"]
        secrets.each { secret ->
            def key = secret["key"]
            def data = secret["data"]
            def secretUrl = "$endpoint/$key"
            def stringJson = new JsonBuilder(data).toPrettyString()
            def secretReturn = RestClient.post(secretUrl, headers, stringJson)
            if (secretReturn == null) {
                return null // work on this
            }
        }
        return secrets
    }

    /**
     * Reads a secret from vault
     * @param clientToken returned by appRoleLogin
     * @param individual secret we are looking for 
     * @return our secret data
     */
    def readSecret(String clientToken, def secret) {
        def headers = ["Content-Type": "application/json", "X-Vault-Token": clientToken]
        def endpoint = "$baseUrl/v1/secret/$secret"
        def result = RestClient.get(endpoint, headers, "")
        return result["data"]
    }

    /**
     * Seals our vault back up
     * @param rootToken
     * @return OK
     */
    def sealVault(def rootToken) {
        def headers = ["Content-Type": "application/json", "X-Vault-Token": rootToken]
        def endpoint = "$baseUrl/v1/sys/seal" // our policy endpoint
        return (RestClient.put(endpoint, headers, ""))
    }

}

