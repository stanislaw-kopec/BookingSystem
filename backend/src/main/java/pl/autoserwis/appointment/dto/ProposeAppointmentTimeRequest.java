package pl.autoserwis.appointment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record ProposeAppointmentTimeRequest(
    @NotNull(message = "Wybierz proponowany dzień.")
    LocalDate visitDate,

    @Size(max = 500, message = "Wiadomość może mieć maksymalnie 500 znaków.")
    String message
) {}
