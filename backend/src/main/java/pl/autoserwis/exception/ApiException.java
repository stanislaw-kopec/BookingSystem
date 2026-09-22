package pl.autoserwis.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

public abstract class ApiException extends RuntimeException {
    private final HttpStatus status;
    private final ApiErrorCode code;
    private final String responseMessage;
    private final Map<String, String> fieldErrors;

    protected ApiException(HttpStatus status, ApiErrorCode code, String message) {
        this(status, code, message, Map.of(), null);
    }

    protected ApiException(HttpStatus status, ApiErrorCode code, String message,
            Map<String, String> fieldErrors) {
        this(status, code, message, fieldErrors, null);
    }

    protected ApiException(HttpStatus status, ApiErrorCode code, String message,
            Map<String, String> fieldErrors, Throwable cause) {
        this(status, code, message, message, fieldErrors, cause);
    }

    protected ApiException(HttpStatus status, ApiErrorCode code, String internalMessage,
            String responseMessage, Map<String, String> fieldErrors, Throwable cause) {
        super(internalMessage, cause);
        this.status = status;
        this.code = code;
        this.responseMessage = responseMessage;
        this.fieldErrors = Map.copyOf(fieldErrors);
    }

    public HttpStatus getStatus() {
        return status;
    }

    public ApiErrorCode getCode() {
        return code;
    }

    public String getResponseMessage() {
        return responseMessage;
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
