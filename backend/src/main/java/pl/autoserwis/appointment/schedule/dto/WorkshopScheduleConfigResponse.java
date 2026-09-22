package pl.autoserwis.appointment.schedule.dto;

import java.util.List;

public record WorkshopScheduleConfigResponse(
    String timeZone,
    ScheduleSettingsResponse settings,
    List<ScheduleDayOverrideResponse> overrides
) {}
