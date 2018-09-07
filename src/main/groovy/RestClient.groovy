import groovy.json.JsonSlurper
import org.codehaus.groovy.util.StringUtil
import org.junit.platform.commons.util.StringUtils

class RestClient {

    private setHeaders(HttpURLConnection connection, def headers) {
        headers.each { k, v -> connection.setRequestProperty(k, v) }
    }

    Object get(String requestUrl, def headers, String body){
        return doHttpRequestWithJson(requestUrl,headers,body,"GET")
    }

    Object post(String requestUrl, def headers, String body){
        return doHttpRequestWithJson(requestUrl,headers,body,"POST")
    }

    Object put(String requestUrl, def headers, String body){
        return doHttpRequestWithJson(requestUrl,headers,body,"PUT")
    }

    Object delete(String requestUrl, def headers, String body){
        return doHttpRequestWithJson(requestUrl,headers,body,"DELETE")
    }



    private Object doHttpRequestWithJson(String requestUrl, def headers, String body, String verb) {
        HttpURLConnection connection = (HttpURLConnection) requestUrl.toURL().openConnection()
        connection.setRequestMethod(verb)
        setHeaders(connection, headers)
        connection.doOutput = true
        if (body != null && body.length() > 0) {
            //write the payload to the body of the request
            def writer = new OutputStreamWriter(connection.outputStream)
            writer.write(body)
            writer.flush()
            writer.close()
        }

        //post the request
        connection.connect()

        //parse the response
        RestResponse response = new RestResponse(connection)
        if (response.isFailure()) {
            println("\n$verb to URL: $requestUrl\n BODY: $body\n HTTP Status: $response.statusCode\n Message: $response.message\n Response Body: $response.responseBody")
            return null
        }

        def jsonSlurper = new JsonSlurper()
        def responseBody = response.getResponseBody()
        if (StringUtils.isNotBlank(responseBody)){
            return jsonSlurper.parseText(responseBody)
        }else {
            return "OK"
        }

    }
}