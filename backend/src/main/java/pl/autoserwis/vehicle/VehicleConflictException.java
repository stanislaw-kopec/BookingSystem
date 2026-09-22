package pl.autoserwis.vehicle;

import org.springframework.http.HttpStatus;
import pl.autoserwis.exception.ApiException;
import pl.autoserwis.exception.ApiErrorCode;

import java.util.Map;

public class VehicleConflictException extends ApiException {
    public VehicleConflictException(String field, String message) {
        super(HttpStatus.CONFLICT, ApiErrorCode.VEHICLE_CONFLICT,
            "Vehicle data conflict.", Map.of(field, message));
    }
}
