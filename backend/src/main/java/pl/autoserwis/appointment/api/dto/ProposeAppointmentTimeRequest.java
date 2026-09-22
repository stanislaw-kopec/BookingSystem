package pl.autoserwis.appointment.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record ProposeAppointmentTimeRequest(
    @NotNull(message = "Select a proposed day.")
    LocalDate visitDate,

    @Size(max = 500, message = "Message can have at most 500 characters.")
    String message
) {}
