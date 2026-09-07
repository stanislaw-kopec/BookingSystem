package pl.autoserwis.appointment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

public record ProposeAppointmentTimeRequest(
    @NotNull(message = "Wybierz proponowany termin.")
    OffsetDateTime slotStartAt,

    @Size(max = 500, message = "Wiadomość może mieć maksymalnie 500 znaków.")
    String message
) {}
