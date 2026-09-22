package pl.autoserwis.appointment.domain;

import org.springframework.http.HttpStatus;
import pl.autoserwis.exception.ApiException;
import pl.autoserwis.exception.ApiErrorCode;

import java.util.Map;

public class AppointmentValidationException extends ApiException {
    public AppointmentValidationException(Map<String, String> fieldErrors) {
        this(ApiErrorCode.APPOINTMENT_VALIDATION_FAILED, "Appointment validation failed.", fieldErrors);
    }

    public AppointmentValidationException(ApiErrorCode code, String message, Map<String, String> fieldErrors) {
        super(HttpStatus.BAD_REQUEST, code, message, fieldErrors);
    }
}
