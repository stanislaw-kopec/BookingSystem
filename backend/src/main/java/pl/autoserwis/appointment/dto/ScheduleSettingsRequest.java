package pl.autoserwis.appointment.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public record ScheduleSettingsRequest(
    @Min(value = 1, message = "Domyślna liczba miejsc musi być większa od 0.")
    @Max(value = 20, message = "Domyślna liczba miejsc może wynosić maksymalnie 20.")
    int defaultDailyCapacity,

    @Min(value = 7, message = "Horyzont rezerwacji musi mieć co najmniej 7 dni.")
    @Max(value = 180, message = "Horyzont rezerwacji może mieć maksymalnie 180 dni.")
    int bookingHorizonDays,

    @NotNull(message = "Podaj godzinę rozpoczęcia pracy.")
    LocalTime workdayStart,

    @NotNull(message = "Podaj godzinę zakończenia pracy.")
    LocalTime workdayEnd
) {}
