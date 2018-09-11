import groovy.json.JsonSlurper
import org.junit.platform.commons.util.StringUtils


/**
 * RestClient class
 * This is used to do basic Rest API calls in particular to vault but could be used in other areas however if cookie
 * support was needed one may want to use HttpClient wrapper
 */

class RestClient {

    /**
     * Sets our headers for us prior to call
     * @param connection the connection used
     * @param map of headers
     */
    private static setHeaders(HttpURLConnection connection, def headers) {
        headers.each { k, v -> connection.setRequestProperty(k, v) }
    }


    /**
     * Basic get request
     * @param requestUrl url to invoke
     * @param map of headers
     * @param body to be passed to api empty string if no body
     */
    static Object get(String requestUrl, def headers, String body){
        return doHttpRequestWithJson(requestUrl,headers,body,"GET")
    }

    /**
     * Basic post request
     * @param requestUrl url to invoke
     * @param map of headers
     * @param body to be passed to api empty string if no body
     */
    static Object post(String requestUrl, def headers, String body){
        return doHttpRequestWithJson(requestUrl,headers,body,"POST")
    }

    /**
     * Basic put request
     * @param requestUrl url to invoke
     * @param map of headers
     * @param body to be passed to api empty string if no body
     */
    static Object put(String requestUrl, def headers, String body){
        return doHttpRequestWithJson(requestUrl,headers,body,"PUT")
    }

    /**
     * Basic delete request
     * @param requestUrl url to invoke
     * @param map of headers
     * @param body to be passed to api empty string if no body
     */
    static Object delete(String requestUrl, def headers, String body){
        return doHttpRequestWithJson(requestUrl,headers,body,"DELETE")
    }


    /**
     * Our overloaded http request
     * @param requestUrl url to invoke
     * @param map of headers
     * @param body to be passed to api empty string if no body
     * @param our verb we are going to use GET,PUT ...
     */

    private static Object doHttpRequestWithJson(String requestUrl, def headers, String body, String verb) {
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