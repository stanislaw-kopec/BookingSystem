package pl.autoserwis.appointment.schedule.dto;

import java.time.LocalDate;

public record ScheduleDayOverrideResponse(
    Long id,
    LocalDate date,
    int capacity,
    boolean closed,
    String note
) {}
