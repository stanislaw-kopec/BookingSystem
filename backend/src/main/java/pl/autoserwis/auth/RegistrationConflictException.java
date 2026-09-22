package pl.autoserwis.auth;

import org.springframework.http.HttpStatus;
import pl.autoserwis.exception.ApiException;
import pl.autoserwis.exception.ApiErrorCode;

import java.util.Map;

public class RegistrationConflictException extends ApiException {
    public RegistrationConflictException(String message, Map<String, String> fieldErrors) {
        this(ApiErrorCode.REGISTRATION_CONFLICT, message, fieldErrors);
    }

    public RegistrationConflictException(ApiErrorCode code, String message, Map<String, String> fieldErrors) {
        super(HttpStatus.CONFLICT, code, message, fieldErrors);
    }
}
