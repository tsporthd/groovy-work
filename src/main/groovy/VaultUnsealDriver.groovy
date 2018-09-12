#! /usr/bin/env groovy

def outputDir = "/home/olbdev/vault/vaultData"
def rootUrl = "http://localhost:8200"
def devRole = 'dev-role'


// This would be done prior to service launch to ensure vault is unsealed
Vault vault = new Vault(outputDir: outputDir, baseUrl: rootUrl)
def keys = vault.initVault()
println("KEYS-> $keys")
def unseal = vault.unseal(keys[Vault.ROOT_TOKEN], keys[Vault.UNSEAL_KEYS], 3)
println("Vault Seal Status $unseal")
def roleId = vault.getRoleId(keys[Vault.ROOT_TOKEN], devRole)
println("ROLEID: $roleId")
def secret = vault.getSecret(keys[Vault.ROOT_TOKEN], devRole)
println("SECRET: $secret")



// Normal access to secrets assuming vault is unsealed
def clientSecret = vault.appRoleLogin(roleId, secret)
println("CLIENT SECRET: clientSecret") // used as token for access to secrets

println(vault.readSecret(clientSecret, "mw-x2-dev"))
println(vault.readSecret(clientSecret, "mw-m1-dev"))
