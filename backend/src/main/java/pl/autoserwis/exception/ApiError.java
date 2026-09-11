package pl.autoserwis.exception;

import java.util.Map;

public record ApiError(int status, String code, String message, Map<String, String> fieldErrors) {
    public ApiError(int status, ApiErrorCode code, Map<String, String> fieldErrors) {
        this(status, code.name(), code.defaultMessage(), fieldErrors);
    }

    public ApiError(int status, ApiErrorCode code, String message, Map<String, String> fieldErrors) {
        this(status, code.name(), message, fieldErrors);
    }
}
