package pl.autoserwis.appointment;

import java.util.Map;

public class AppointmentConflictException extends RuntimeException {
    private final Map<String, String> fieldErrors;

    public AppointmentConflictException(String message) {
        this(message, Map.of());
    }

    public AppointmentConflictException(String field, String message) {
        this(message, Map.of(field, message));
    }

    private AppointmentConflictException(String message, Map<String, String> fieldErrors) {
        super(message);
        this.fieldErrors = Map.copyOf(fieldErrors);
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
