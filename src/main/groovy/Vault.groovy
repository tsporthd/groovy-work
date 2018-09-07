import groovy.json.JsonBuilder

class Vault {

    def outputDir
    def baseUrl
    RestClient restClient = new RestClient()
    static final String keyFileName = "Vault.keys"


    private def getKeyFileName(){
        return "$outputDir$File.separator$keyFileName"
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
            String keyFile = getKeyFileName()
            // Save our keys
            new File(keyFile).write(new JsonBuilder(result).toPrettyString())
        }else{
            println("Vault has already been initialized")
            // ok if it has been initialized we need to read keys/unseal it blah blah

        }

    }






}

