package pl.autoserwis.user;

import pl.autoserwis.exception.ApiErrorCode;

import java.util.Map;

public class AccountManagementValidationException extends RuntimeException {
    private final Map<String, String> fieldErrors;

    public AccountManagementValidationException(Map<String, String> fieldErrors) {
        super("Account management parameters are invalid.");
        this.fieldErrors = Map.copyOf(fieldErrors);
    }

    public ApiErrorCode getCode() {
        return ApiErrorCode.ACCOUNT_MANAGEMENT_VALIDATION_FAILED;
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
