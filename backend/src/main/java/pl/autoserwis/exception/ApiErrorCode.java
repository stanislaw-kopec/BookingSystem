package pl.autoserwis.exception;

public enum ApiErrorCode {
    VALIDATION_FAILED("Validation failed."),
    MALFORMED_REQUEST("Malformed request."),
    RESOURCE_NOT_FOUND("Resource not found."),
    RESOURCE_CONFLICT("Resource conflict."),
    REGISTRATION_VALIDATION_FAILED("Registration validation failed."),
    REGISTRATION_CONFLICT("Registration conflict."),
    ACCOUNT_PASSWORD_VALIDATION_FAILED("Account password validation failed."),
    ACCOUNT_MANAGEMENT_VALIDATION_FAILED("Account management validation failed."),
    PROFILE_VALIDATION_FAILED("Profile validation failed."),
    VEHICLE_VALIDATION_FAILED("Vehicle validation failed."),
    VEHICLE_CONFLICT("Vehicle conflict."),
    APPOINTMENT_VALIDATION_FAILED("Appointment validation failed."),
    APPOINTMENT_CONFLICT("Appointment conflict."),
    APPOINTMENT_DAY_FULL("Appointment day is full."),
    DATA_INTEGRITY_CONFLICT("Data integrity conflict."),
    INVOICE_GENERATION_FAILED("Invoice generation failed."),
    UNAUTHENTICATED("Authentication is required."),
    ACCESS_DENIED("Access denied.");

    private final String defaultMessage;

    ApiErrorCode(String defaultMessage) {
        this.defaultMessage = defaultMessage;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
