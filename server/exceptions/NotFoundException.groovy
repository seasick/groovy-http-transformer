package server.exceptions

class NotFoundException extends HttpStatusException {

    int code = 404

    NotFoundException(String message) {
        super(message)
    }

}
