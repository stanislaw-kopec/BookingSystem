package pl.autoserwis.invoice;

public class InvoiceGenerationException extends RuntimeException {
    public InvoiceGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
