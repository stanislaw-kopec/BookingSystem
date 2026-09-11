package pl.autoserwis.appointment.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public record ScheduleSettingsRequest(
    @Min(value = 1, message = "Default daily capacity must be greater than 0.")
    @Max(value = 20, message = "Default daily capacity can be at most 20.")
    int defaultDailyCapacity,

    @Min(value = 7, message = "Booking horizon must be at least 7 days.")
    @Max(value = 180, message = "Booking horizon can be at most 180 days.")
    int bookingHorizonDays,

    @NotNull(message = "Enter workday start time.")
    LocalTime workdayStart,

    @NotNull(message = "Enter workday end time.")
    LocalTime workdayEnd
) {}
