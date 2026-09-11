package pl.autoserwis.auth;

import pl.autoserwis.exception.ApiErrorCode;

import java.util.Map;

public class RegistrationValidationException extends RuntimeException {
    private final ApiErrorCode code;
    private final Map<String, String> fieldErrors;

    public RegistrationValidationException(String message, Map<String, String> fieldErrors) {
        this(ApiErrorCode.REGISTRATION_VALIDATION_FAILED, message, fieldErrors);
    }

    public RegistrationValidationException(ApiErrorCode code, String message, Map<String, String> fieldErrors) {
        super(message);
        this.code = code;
        this.fieldErrors = Map.copyOf(fieldErrors);
    }

    public ApiErrorCode getCode() {
        return code;
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
