package pl.autoserwis.appointment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

public record ClientAppointmentRequest(
    @NotNull(message = "Wybierz pojazd.")
    Long vehicleId,

    @NotNull(message = "Wybierz termin wizyty.")
    OffsetDateTime slotStartAt,

    @NotBlank(message = "Opisz problem z pojazdem.")
    @Size(min = 10, max = 2000, message = "Opis problemu musi mieć od 10 do 2000 znaków.")
    String problemDescription
) {}
