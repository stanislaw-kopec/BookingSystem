package pl.autoserwis.vehicle;

import java.util.Map;

public class VehicleConflictException extends RuntimeException {
    private final Map<String, String> fieldErrors;

    public VehicleConflictException(String field, String message) {
        super("Pojazd o takich danych już istnieje.");
        this.fieldErrors = Map.of(field, message);
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
