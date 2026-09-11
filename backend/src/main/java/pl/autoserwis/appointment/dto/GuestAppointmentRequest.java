package pl.autoserwis.appointment.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record GuestAppointmentRequest(
    @NotBlank(message = "Enter a first name.")
    @Size(max = 60, message = "First name can have at most 60 characters.")
    String firstName,

    @NotBlank(message = "Enter a last name.")
    @Size(max = 80, message = "Last name can have at most 80 characters.")
    String lastName,

    @Size(max = 30, message = "Phone number can have at most 30 characters.")
    @Pattern(regexp = "^(?:\\s*|[0-9+() .-]{7,30})$", message = "Enter a valid phone number.")
    String phoneNumber,

    @Size(max = 254, message = "Email address can have at most 254 characters.")
    @Email(message = "Enter a valid email address.")
    String contactEmail,

    @NotBlank(message = "Enter a make.")
    @Size(max = 80, message = "Make can have at most 80 characters.")
    String vehicleMake,

    @NotBlank(message = "Enter a model.")
    @Size(max = 80, message = "Model can have at most 80 characters.")
    String vehicleModel,

    @NotNull(message = "Enter a production year.")
    @Min(value = 1886, message = "Production year cannot be earlier than 1886.")
    Integer vehicleProductionYear,

    @NotBlank(message = "Enter a registration number.")
    @Size(min = 2, max = 20, message = "Registration number must be between 2 and 20 characters.")
    @Pattern(regexp = "^[A-Za-z0-9 -]+$", message = "Registration number contains invalid characters.")
    String vehicleRegistrationNumber,

    @Pattern(regexp = "^(?:\\s*|[A-HJ-NPR-Za-hj-npr-z0-9]{17})$",
        message = "VIN must have 17 characters and cannot contain I, O or Q.")
    String vehicleVin,

    @NotNull(message = "Select an appointment day.")
    LocalDate visitDate,

    @NotBlank(message = "Describe the vehicle problem.")
    @Size(min = 10, max = 2000, message = "Problem description must be between 10 and 2000 characters.")
    String problemDescription
) {}
