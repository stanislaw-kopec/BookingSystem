package pl.autoserwis.appointment.schedule.dto;

import java.time.LocalTime;

public record ScheduleSettingsResponse(
    int defaultDailyCapacity,
    int bookingHorizonDays,
    LocalTime workdayStart,
    LocalTime workdayEnd
) {}
