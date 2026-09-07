package pl.autoserwis.appointment.dto;

import java.time.OffsetDateTime;

public record AppointmentSlotResponse(
    OffsetDateTime startAt,
    OffsetDateTime endAt,
    boolean available
) {}
