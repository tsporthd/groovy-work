def outputDir = "/home/olbdev/vault/vaultData"
def rootUrl = "http://localhost:8200"

Vault vault = new Vault(outputDir: outputDir, baseUrl: rootUrl)

vault.initVault()

