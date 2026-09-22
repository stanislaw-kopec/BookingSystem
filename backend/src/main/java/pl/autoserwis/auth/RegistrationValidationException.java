package pl.autoserwis.auth;

import org.springframework.http.HttpStatus;
import pl.autoserwis.exception.ApiException;
import pl.autoserwis.exception.ApiErrorCode;

import java.util.Map;

public class RegistrationValidationException extends ApiException {
    public RegistrationValidationException(String message, Map<String, String> fieldErrors) {
        this(ApiErrorCode.REGISTRATION_VALIDATION_FAILED, message, fieldErrors);
    }

    public RegistrationValidationException(ApiErrorCode code, String message, Map<String, String> fieldErrors) {
        super(HttpStatus.BAD_REQUEST, code, message, fieldErrors);
    }
}
