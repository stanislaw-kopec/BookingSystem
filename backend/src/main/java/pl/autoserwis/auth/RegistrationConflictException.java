package pl.autoserwis.auth;

import java.util.Map;

public class RegistrationConflictException extends RuntimeException {
    private final Map<String, String> fieldErrors;

    public RegistrationConflictException(String message, Map<String, String> fieldErrors) {
        super(message);
        this.fieldErrors = Map.copyOf(fieldErrors);
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
