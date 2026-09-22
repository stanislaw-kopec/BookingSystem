package pl.autoserwis.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends ApiException {

    public ResourceNotFoundException(String message) {
        this(ApiErrorCode.RESOURCE_NOT_FOUND, message);
    }

    public ResourceNotFoundException(ApiErrorCode code, String message) {
        super(HttpStatus.NOT_FOUND, code, message);
    }
}
