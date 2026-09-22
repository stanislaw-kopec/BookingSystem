package pl.autoserwis.appointment.schedule.dto;

import java.util.List;

public record StaffScheduleResponse(String timeZone, List<StaffScheduleDayResponse> days) {}
