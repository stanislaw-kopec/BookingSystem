package pl.autoserwis.appointment.schedule.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record ScheduleDayOverrideRequest(
    @NotNull(message = "Enter an override date.")
    LocalDate date,

    @Min(value = 0, message = "Capacity cannot be negative.")
    @Max(value = 20, message = "Capacity can be at most 20.")
    int capacity,

    boolean closed,

    @Size(max = 200, message = "Note can have at most 200 characters.")
    String note
) {}
