package server.exceptions

class MethodNotAllowedException extends HttpStatusException {

    int code = 405

    MethodNotAllowedException(String message) {
        super(message)
    }

}
