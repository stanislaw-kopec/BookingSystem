package pl.autoserwis.vehicle;

import java.util.Map;

public class VehicleValidationException extends RuntimeException {
    private final Map<String, String> fieldErrors;

    public VehicleValidationException(Map<String, String> fieldErrors) {
        super("Popraw dane pojazdu.");
        this.fieldErrors = Map.copyOf(fieldErrors);
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
