package pl.autoserwis.appointment.dto;

import java.util.List;

public record AppointmentAvailabilityResponse(
    String timeZone,
    int dailyCapacity,
    List<AppointmentDayResponse> days
) {}
