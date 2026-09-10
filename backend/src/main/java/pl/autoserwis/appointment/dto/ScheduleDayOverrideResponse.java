package pl.autoserwis.appointment.dto;

import java.time.LocalDate;

public record ScheduleDayOverrideResponse(
    Long id,
    LocalDate date,
    int capacity,
    boolean closed,
    String note
) {}
