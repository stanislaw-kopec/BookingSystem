package pl.autoserwis.exception;

public class ResourceNotFoundException extends RuntimeException {
    private final ApiErrorCode code;

    public ResourceNotFoundException(String message) {
        this(ApiErrorCode.RESOURCE_NOT_FOUND, message);
    }

    public ResourceNotFoundException(ApiErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public ApiErrorCode getCode() {
        return code;
    }
}
