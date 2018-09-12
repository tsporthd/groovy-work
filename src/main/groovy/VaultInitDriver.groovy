#! /usr/bin/env groovy
def outputDir = "/home/olbdev/vault/vaultData"
def rootUrl = "http://localhost:8200"
def devRole = 'dev-role'
def policyFile = '/home/olbdev/projects/groovy-work/src/main/resources/devPolicy.json'
def vaultPolicy = 'dig-dev'


Vault vault = new Vault(outputDir: outputDir, baseUrl: rootUrl)

def keys = vault.initVault()
println("KEYS-> $keys")


def unseal = vault.unseal(keys[Vault.ROOT_TOKEN], keys[Vault.UNSEAL_KEYS], 3)
println("Vault Seal Status $unseal")



def policy = vault.setPolicy(keys[Vault.ROOT_TOKEN], vaultPolicy, policyFile)
println("POLICY RETURN: $policy")

def appRole = vault.setAppRole(keys[Vault.ROOT_TOKEN])
println("APP ROLE RETURN: $appRole")


def roleAssign = vault.setDevRole(keys[Vault.ROOT_TOKEN], devRole, vaultPolicy)
println("ROLE ASSIGN RETURN: $roleAssign")


def roleId = vault.getRoleId(keys[Vault.ROOT_TOKEN], devRole)
println("ROLEID: $roleId")

def secret = vault.getSecret(keys[Vault.ROOT_TOKEN], devRole)
println("SECRET: $secret")


def clientSecret = vault.appRoleLogin(roleId, secret)
println("CLIENT SECRET: clientSecret") // used as token for access to secrets

def secretsFile = '/home/olbdev/projects/groovy-work/src/main/resources/devSecrets.json'
vault.writeSecrets(clientSecret, secretsFile)


println(vault.readSecret(clientSecret,"mw-x2-dev"))
println(vault.readSecret(clientSecret,"mw-m1-dev"))


def sealResult = vault.sealVault(keys[Vault.ROOT_TOKEN])
println("Vault seal return $sealResult")





