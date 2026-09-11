package pl.autoserwis.appointment;

import pl.autoserwis.exception.ApiErrorCode;

import java.util.Map;

public class AppointmentConflictException extends RuntimeException {
    private final ApiErrorCode code;
    private final Map<String, String> fieldErrors;

    public AppointmentConflictException(String message) {
        this(ApiErrorCode.APPOINTMENT_CONFLICT, message, Map.of());
    }

    public AppointmentConflictException(String field, String message) {
        this(ApiErrorCode.APPOINTMENT_CONFLICT, message, Map.of(field, message));
    }

    public AppointmentConflictException(ApiErrorCode code, String field, String message) {
        this(code, message, Map.of(field, message));
    }

    private AppointmentConflictException(ApiErrorCode code, String message, Map<String, String> fieldErrors) {
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
