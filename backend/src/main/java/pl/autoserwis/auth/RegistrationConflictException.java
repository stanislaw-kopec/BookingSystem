package pl.autoserwis.auth;

import pl.autoserwis.exception.ApiErrorCode;

import java.util.Map;

public class RegistrationConflictException extends RuntimeException {
    private final ApiErrorCode code;
    private final Map<String, String> fieldErrors;

    public RegistrationConflictException(String message, Map<String, String> fieldErrors) {
        this(ApiErrorCode.REGISTRATION_CONFLICT, message, fieldErrors);
    }

    public RegistrationConflictException(ApiErrorCode code, String message, Map<String, String> fieldErrors) {
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
