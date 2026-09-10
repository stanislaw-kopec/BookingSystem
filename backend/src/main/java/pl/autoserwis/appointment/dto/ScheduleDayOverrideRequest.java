package pl.autoserwis.appointment.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record ScheduleDayOverrideRequest(
    @NotNull(message = "Podaj dzień wyjątku.")
    LocalDate date,

    @Min(value = 0, message = "Liczba miejsc nie może być ujemna.")
    @Max(value = 20, message = "Liczba miejsc może wynosić maksymalnie 20.")
    int capacity,

    boolean closed,

    @Size(max = 200, message = "Notatka może mieć maksymalnie 200 znaków.")
    String note
) {}
