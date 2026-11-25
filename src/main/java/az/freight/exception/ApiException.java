package az.freight.exception;

import org.springframework.http.HttpStatus;

/** Konkret HTTP statusu olan biznes xətası (409, 422, 502 və s.). */
public class ApiException extends RuntimeException {
    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus status() { return status; }
}
