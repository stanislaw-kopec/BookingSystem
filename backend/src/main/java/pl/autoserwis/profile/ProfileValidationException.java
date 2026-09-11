package pl.autoserwis.profile;

import pl.autoserwis.exception.ApiErrorCode;

import java.util.Map;

public class ProfileValidationException extends RuntimeException {
    private final ApiErrorCode code;
    private final Map<String, String> fieldErrors;

    public ProfileValidationException(Map<String, String> fieldErrors) {
        super("Company profile data is incomplete.");
        this.code = ApiErrorCode.PROFILE_VALIDATION_FAILED;
        this.fieldErrors = Map.copyOf(fieldErrors);
    }

    public ApiErrorCode getCode() {
        return code;
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
