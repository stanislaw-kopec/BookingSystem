package pl.autoserwis.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validation(MethodArgumentNotValidException exception) {
        Map<String, String> fields = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
            fields.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.badRequest().body(new ApiError(400, ApiErrorCode.VALIDATION_FAILED, fields));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> invalidJson() {
        return error(400, ApiErrorCode.MALFORMED_REQUEST);
    }

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ApiError> apiException(ApiException exception) {
        int status = exception.getStatus().value();
        return ResponseEntity.status(exception.getStatus())
            .body(new ApiError(status, exception.getCode(), exception.getResponseMessage(),
                exception.getFieldErrors()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> databaseConflict() {
        // Database constraints also protect concurrent writes.
        return error(409, ApiErrorCode.DATA_INTEGRITY_CONFLICT);
    }

    private ResponseEntity<ApiError> error(int status, ApiErrorCode code) {
        return ResponseEntity.status(status).body(new ApiError(status, code, Map.of()));
    }

}
