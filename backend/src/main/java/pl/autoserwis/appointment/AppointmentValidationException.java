package pl.autoserwis.appointment;

import java.util.Map;

public class AppointmentValidationException extends RuntimeException {
    private final Map<String, String> fieldErrors;

    public AppointmentValidationException(Map<String, String> fieldErrors) {
        super("Popraw dane formularza.");
        this.fieldErrors = Map.copyOf(fieldErrors);
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
