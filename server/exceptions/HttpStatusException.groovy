package server.exceptions

class HttpStatusException extends Exception {

    int code = 500

    HttpStatusException(String message) {
        super(message)
    }

}
