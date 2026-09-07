package pl.autoserwis.auth;

import java.util.Map;

public class RegistrationValidationException extends RuntimeException {
    private final Map<String, String> fieldErrors;

    public RegistrationValidationException(String message, Map<String, String> fieldErrors) {
        super(message);
        this.fieldErrors = Map.copyOf(fieldErrors);
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
