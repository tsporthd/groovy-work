import groovy.json.JsonBuilder
import groovy.json.JsonSlurper

class Vault {

    def outputDir
    def baseUrl
    RestClient restClient = new RestClient()
    static final String keyFileName = "Vault.keys"
    static final String ROOT_TOKEN = "root_token"
    static final String UNSEAL_KEYS = "unseal_keys"


    private def getKeyFileName(){
        return "$outputDir$File.separator$keyFileName"
    }

    static save(Object content, String filePath) {
        new File(filePath).write(new JsonBuilder(content).toPrettyString())
    }

    static Object load(String filePath) {
        return new JsonSlurper().parseText(new File(filePath).text)
    }

    def initVault(){
        def endpoint = "$baseUrl/v1/sys/init"
        def headers = ["Content-Type": "application/json"]
        def result = restClient.get(endpoint, headers, "")
        def initialized = result["initialized"]
        if ( !initialized ){
            String body = "{" +
                    "  \"secret_shares\": 10," +
                    "  \"secret_threshold\": 3" +
                    "}"
            result = restClient.put(endpoint,headers,body)
            save(result,getKeyFileName())
            println("Vault Keys saved ")
        }else{
            println("Vault has already been initialized")
            // ok if it has been initialized we need to read keys/unseal it blah blah
            def keyFile = new File(getKeyFileName())
            if (keyFile.exists()) {
                def keySet = load(getKeyFileName())
                def rootToken = keySet[ROOT_TOKEN]
                def unsealKeys = keySet["keys_base64"]
                def keys = ["root_token" : rootToken, "unseal_keys" : unsealKeys]
                return keys
            }else{
                println(getKeyFileName() + " Does not exist")
                return null
            }
        }

    }

    def unseal(String rootKey, def unsealKeys, int numKeysRequired){
        def endpoint = "$baseUrl/v1/sys/unseal"
        def headers = ["Content-Type": "application/json", "X-Vault-Token" : rootKey]
        def sealed
        for(int ctr =0; ctr < numKeysRequired; ctr++){
            def body = "{\"key\":\"" + unsealKeys[ctr] + '\"}'
            def result = restClient.post(endpoint,headers,body)
            sealed = result["sealed"]
            if ( !sealed ){
                break
            }
        }

        return sealed
    }


    def setPolicy(String rootKey, def policyId, def policyFile){
        def endpoint = "$baseUrl/v1/sys/policy/$policyId" // our policy endpoint
        def headers = ["Content-Type": "application/json", "X-Vault-Token" : rootKey]
        String data = new File(policyFile).text
        return restClient.post(endpoint,headers,data)
    }

    def setAppRole(String rootKey){
        def endpoint = "$baseUrl/v1/sys/auth/approle" // our policy endpoint
        def headers = ["Content-Type": "application/json", "X-Vault-Token" : rootKey]
        String data = '{"type":"approle"}'
        def result = restClient.post(endpoint,headers,data)
        println(result)
    }

    def setDevRole(String rootKey, def devRole, def policyId){
        def endpoint = "$baseUrl/v1/auth/approle/role/$devRole" // our policy endpoint
        def headers = ["Content-Type": "application/json", "X-Vault-Token" : rootKey]
        String data = '{"policies" : ["' + policyId +  '"]}'
        return  restClient.post(endpoint,headers,data)
    }

    def getRoleId(String rootKey, def devRole){
        def endpoint = "$baseUrl/v1/auth/approle/role/$devRole/role-id" // our policy endpoint
        def headers = ["Content-Type": "application/json", "X-Vault-Token" : rootKey]
        def roleData = restClient.get(endpoint,headers,"")
        def data = roleData["data"]
        return data["role_id"]
    }

    def getSecret(String rootKey, def devRole){
        def endpoint = "$baseUrl/v1/auth/approle/role/$devRole/secret-id" // our policy endpoint
        def headers = ["Content-Type": "application/json", "X-Vault-Token" : rootKey]
        def roleData = restClient.post(endpoint,headers,"")
        def data = roleData["data"]
        return data["secret_id"]
    }

    def appRoleLogin(String roleId, String secretId){
        def endpoint = "$baseUrl/v1/auth/approle/login" // our policy endpoint
        def headers = ["Content-Type": "application/json"]
        def data = '{"role_id" : "' + roleId + '", "secret_id": "' + secretId + '"}'
        def roleData = restClient.post(endpoint,headers,data)
        def auth = roleData["auth"]
        return auth["client_token"]
    }

}

