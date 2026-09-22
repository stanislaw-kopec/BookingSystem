package pl.autoserwis.appointment.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record ClientAppointmentRequest(
    @NotNull(message = "Select a vehicle.")
    Long vehicleId,

    @NotNull(message = "Select an appointment day.")
    LocalDate visitDate,

    @NotBlank(message = "Describe the vehicle problem.")
    @Size(min = 10, max = 2000, message = "Problem description must be between 10 and 2000 characters.")
    String problemDescription
) {}
