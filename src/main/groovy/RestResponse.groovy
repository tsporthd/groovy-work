class RestResponse {
    Integer statusCode
    String message
    boolean failure
    String responseBody

    RestResponse(def connection) {
        statusCode = connection.responseCode
        message = connection.responseMessage
        if ((statusCode >= 200) && (statusCode < 300)) {
            this.responseBody = connection.content.text
        } else {
            this.failure = true
            this.message = connection.getErrorStream().text
        }

    }

}


