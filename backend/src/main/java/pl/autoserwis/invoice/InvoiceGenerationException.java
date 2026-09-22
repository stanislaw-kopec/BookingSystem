package pl.autoserwis.invoice;

import org.springframework.http.HttpStatus;
import pl.autoserwis.exception.ApiErrorCode;
import pl.autoserwis.exception.ApiException;

import java.util.Map;

public class InvoiceGenerationException extends ApiException {
    public InvoiceGenerationException(String message, Throwable cause) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, ApiErrorCode.INVOICE_GENERATION_FAILED,
            message, ApiErrorCode.INVOICE_GENERATION_FAILED.defaultMessage(), Map.of(), cause);
    }
}
