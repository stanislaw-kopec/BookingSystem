package pl.autoserwis.vehicle.dto;

import jakarta.validation.constraints.*;

public record VehicleRequest(
    @NotBlank(message = "Enter a make.")
    @Size(max = 80, message = "Make can have at most 80 characters.")
    String make,

    @NotBlank(message = "Enter a model.")
    @Size(max = 80, message = "Model can have at most 80 characters.")
    String model,

    @NotNull(message = "Enter a production year.")
    @Min(value = 1886, message = "Production year cannot be earlier than 1886.")
    Integer productionYear,

    @NotBlank(message = "Enter a registration number.")
    @Size(min = 2, max = 20, message = "Registration number must be between 2 and 20 characters.")
    @Pattern(regexp = "^[A-Za-z0-9 -]+$", message = "Registration number contains invalid characters.")
    String registrationNumber,

    @Pattern(regexp = "^(?:\\s*|[A-HJ-NPR-Za-hj-npr-z0-9]{17})$",
        message = "VIN must have 17 characters and cannot contain I, O or Q.")
    String vin
) {}
