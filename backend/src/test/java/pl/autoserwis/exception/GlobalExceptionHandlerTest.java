package pl.autoserwis.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import pl.autoserwis.invoice.InvoiceGenerationException;
import pl.autoserwis.vehicle.VehicleValidationException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void mapsApiExceptionStatusCodeMessageAndFieldErrors() {
        ResponseEntity<ApiError> response = handler.apiException(
            new VehicleValidationException(Map.of("vin", "Invalid VIN.")));

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isEqualTo(new ApiError(
            400,
            ApiErrorCode.VEHICLE_VALIDATION_FAILED,
            "Vehicle validation failed.",
            Map.of("vin", "Invalid VIN.")
        ));
    }

    @Test
    void derivesHttpStatusFromEveryApiExceptionKind() {
        assertThat(handler.apiException(new ResourceNotFoundException("Missing.")).getStatusCode().value())
            .isEqualTo(404);
        assertThat(handler.apiException(new ResourceConflictException("Conflict.")).getStatusCode().value())
            .isEqualTo(409);
        InvoiceGenerationException invoiceException =
            new InvoiceGenerationException("Internal detail.", null);
        ResponseEntity<ApiError> invoiceResponse = handler.apiException(invoiceException);
        assertThat(invoiceResponse.getStatusCode().value()).isEqualTo(500);
        assertThat(invoiceResponse.getBody().message()).isEqualTo("Invoice generation failed.");
        assertThat(invoiceException.getMessage()).isEqualTo("Internal detail.");
    }
}
