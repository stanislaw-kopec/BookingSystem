package pl.autoserwis.appointment.domain;

import pl.autoserwis.exception.ApiErrorCode;

import java.util.Map;

public class AppointmentValidationException extends RuntimeException {
    private final ApiErrorCode code;
    private final Map<String, String> fieldErrors;

    public AppointmentValidationException(Map<String, String> fieldErrors) {
        this(ApiErrorCode.APPOINTMENT_VALIDATION_FAILED, "Appointment validation failed.", fieldErrors);
    }

    public AppointmentValidationException(ApiErrorCode code, String message, Map<String, String> fieldErrors) {
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
