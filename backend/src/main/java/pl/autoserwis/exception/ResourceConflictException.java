package pl.autoserwis.exception;

public class ResourceConflictException extends RuntimeException {
    private final ApiErrorCode code;

    public ResourceConflictException(String message) {
        this(ApiErrorCode.RESOURCE_CONFLICT, message);
    }

    public ResourceConflictException(ApiErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public ApiErrorCode getCode() {
        return code;
    }
}
