def outputDir = "/home/olbdev/vault/vaultData"
def rootUrl = "http://localhost:8200"

Vault vault = new Vault(outputDir: outputDir, baseUrl: rootUrl)

def keys = vault.initVault()
println(keys)


def unseal = vault.unseal(keys[Vault.ROOT_TOKEN], keys[Vault.UNSEAL_KEYS], 3)
println("Vault Seal Status $unseal")


def policyFile = '/home/olbdev/projects/groovy-work/src/main/resources/devPolicy.json'
def vaultPolicy = 'dig-dev'
def policy = vault.setPolicy(keys[Vault.ROOT_TOKEN], vaultPolicy, policyFile)
println(policy)

def appRole = vault.setAppRole(keys[Vault.ROOT_TOKEN])
println(appRole)

def devRole = 'dev-role'
def roleAssign = vault.setDevRole(keys[Vault.ROOT_TOKEN], devRole, vaultPolicy)
println(roleAssign)

def roleId = vault.getRoleId(keys[Vault.ROOT_TOKEN], devRole)
println(roleId)

def secret = vault.getSecret(keys[Vault.ROOT_TOKEN], devRole)
println(secret)


def clientSecret = vault.appRoleLogin(roleId, secret)
println(clientSecret) // used as token for access to secrets

def secretsFile = '/home/olbdev/projects/groovy-work/src/main/resources/devSecrets.json'
vault.writeSecrets(clientSecret, secretsFile)


println(vault.readSecret(clientSecret,"mw-x2-dev"))
println(vault.readSecret(clientSecret,"mw-m1-dev"))