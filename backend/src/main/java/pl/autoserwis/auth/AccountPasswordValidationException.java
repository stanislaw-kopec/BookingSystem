package pl.autoserwis.auth;

import pl.autoserwis.exception.ApiErrorCode;

import java.util.Map;

public class AccountPasswordValidationException extends RuntimeException {
    private final Map<String, String> fieldErrors;

    public AccountPasswordValidationException(Map<String, String> fieldErrors) {
        super("Password cannot be changed.");
        this.fieldErrors = Map.copyOf(fieldErrors);
    }

    public ApiErrorCode getCode() {
        return ApiErrorCode.ACCOUNT_PASSWORD_VALIDATION_FAILED;
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
