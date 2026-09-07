package pl.autoserwis.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import pl.autoserwis.appointment.AppointmentConflictException;
import pl.autoserwis.appointment.AppointmentValidationException;
import pl.autoserwis.auth.RegistrationConflictException;
import pl.autoserwis.auth.RegistrationValidationException;
import pl.autoserwis.profile.ProfileValidationException;
import pl.autoserwis.vehicle.VehicleConflictException;
import pl.autoserwis.vehicle.VehicleValidationException;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validation(MethodArgumentNotValidException exception) {
        Map<String, String> fields = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
            fields.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.badRequest().body(new ApiError(400, "Popraw dane formularza.", fields));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> invalidJson() {
        return error(400, "Nieprawidłowy format danych.");
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ApiError> notFound(ResourceNotFoundException exception) {
        return error(404, exception.getMessage());
    }

    @ExceptionHandler(ResourceConflictException.class)
    ResponseEntity<ApiError> conflict(ResourceConflictException exception) {
        return error(409, exception.getMessage());
    }

    @ExceptionHandler(RegistrationValidationException.class)
    ResponseEntity<ApiError> registrationValidation(RegistrationValidationException exception) {
        return ResponseEntity.badRequest()
            .body(new ApiError(400, exception.getMessage(), exception.getFieldErrors()));
    }

    @ExceptionHandler(RegistrationConflictException.class)
    ResponseEntity<ApiError> registrationConflict(RegistrationConflictException exception) {
        return ResponseEntity.status(409)
            .body(new ApiError(409, exception.getMessage(), exception.getFieldErrors()));
    }

    @ExceptionHandler(ProfileValidationException.class)
    ResponseEntity<ApiError> profileValidation(ProfileValidationException exception) {
        return ResponseEntity.badRequest()
            .body(new ApiError(400, exception.getMessage(), exception.getFieldErrors()));
    }

    @ExceptionHandler(VehicleValidationException.class)
    ResponseEntity<ApiError> vehicleValidation(VehicleValidationException exception) {
        return ResponseEntity.badRequest()
            .body(new ApiError(400, exception.getMessage(), exception.getFieldErrors()));
    }

    @ExceptionHandler(VehicleConflictException.class)
    ResponseEntity<ApiError> vehicleConflict(VehicleConflictException exception) {
        return ResponseEntity.status(409)
            .body(new ApiError(409, exception.getMessage(), exception.getFieldErrors()));
    }

    @ExceptionHandler(AppointmentValidationException.class)
    ResponseEntity<ApiError> appointmentValidation(AppointmentValidationException exception) {
        return ResponseEntity.badRequest()
            .body(new ApiError(400, exception.getMessage(), exception.getFieldErrors()));
    }

    @ExceptionHandler(AppointmentConflictException.class)
    ResponseEntity<ApiError> appointmentConflict(AppointmentConflictException exception) {
        return ResponseEntity.status(409)
            .body(new ApiError(409, exception.getMessage(), exception.getFieldErrors()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> databaseConflict() {
        // Database constraints also protect concurrent writes.
        return error(409, "Operacja powoduje konflikt z istniejącymi danymi. Odśwież widok i spróbuj ponownie.");
    }

    private ResponseEntity<ApiError> error(int status, String message) {
        return ResponseEntity.status(status).body(new ApiError(status, message, Map.of()));
    }
}
