package pl.autoserwis.exception;

import org.springframework.http.HttpStatus;

public class ResourceConflictException extends ApiException {

    public ResourceConflictException(String message) {
        this(ApiErrorCode.RESOURCE_CONFLICT, message);
    }

    public ResourceConflictException(ApiErrorCode code, String message) {
        super(HttpStatus.CONFLICT, code, message);
    }
}
