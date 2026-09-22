package pl.autoserwis.appointment.domain;

import org.springframework.http.HttpStatus;
import pl.autoserwis.exception.ApiException;
import pl.autoserwis.exception.ApiErrorCode;

import java.util.Map;

public class AppointmentConflictException extends ApiException {
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
        super(HttpStatus.CONFLICT, code, message, fieldErrors);
    }
}
