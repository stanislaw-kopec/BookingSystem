package pl.autoserwis.appointment.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record AppointmentDayResponse(
    LocalDate date,
    OffsetDateTime startAt,
    OffsetDateTime endAt,
    int capacity,
    int remainingCapacity,
    boolean available
) {}
