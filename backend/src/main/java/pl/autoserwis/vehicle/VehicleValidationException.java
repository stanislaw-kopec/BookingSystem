package pl.autoserwis.vehicle;

import org.springframework.http.HttpStatus;
import pl.autoserwis.exception.ApiException;
import pl.autoserwis.exception.ApiErrorCode;

import java.util.Map;

public class VehicleValidationException extends ApiException {
    public VehicleValidationException(Map<String, String> fieldErrors) {
        super(HttpStatus.BAD_REQUEST, ApiErrorCode.VEHICLE_VALIDATION_FAILED,
            "Vehicle validation failed.", fieldErrors);
    }
}
