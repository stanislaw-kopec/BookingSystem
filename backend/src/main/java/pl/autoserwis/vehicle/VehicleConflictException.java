package pl.autoserwis.vehicle;

import pl.autoserwis.exception.ApiErrorCode;

import java.util.Map;

public class VehicleConflictException extends RuntimeException {
    private final ApiErrorCode code;
    private final Map<String, String> fieldErrors;

    public VehicleConflictException(String field, String message) {
        super("Vehicle data conflict.");
        this.code = ApiErrorCode.VEHICLE_CONFLICT;
        this.fieldErrors = Map.of(field, message);
    }

    public ApiErrorCode getCode() {
        return code;
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
