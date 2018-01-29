package uk.ac.hw.ilab.fel_server.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class InvalidFELRequest extends RuntimeException {
    public InvalidFELRequest() {
        super();
    }

    public InvalidFELRequest(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidFELRequest(String message) {
        super(message);
    }

    public InvalidFELRequest(Throwable cause) {
        super(cause);
    }
}