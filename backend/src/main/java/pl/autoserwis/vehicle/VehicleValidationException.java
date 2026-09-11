package pl.autoserwis.vehicle;

import pl.autoserwis.exception.ApiErrorCode;

import java.util.Map;

public class VehicleValidationException extends RuntimeException {
    private final ApiErrorCode code;
    private final Map<String, String> fieldErrors;

    public VehicleValidationException(Map<String, String> fieldErrors) {
        super("Vehicle validation failed.");
        this.code = ApiErrorCode.VEHICLE_VALIDATION_FAILED;
        this.fieldErrors = Map.copyOf(fieldErrors);
    }

    public ApiErrorCode getCode() {
        return code;
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
