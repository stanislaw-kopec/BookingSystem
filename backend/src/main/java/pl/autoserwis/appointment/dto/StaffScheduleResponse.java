package pl.autoserwis.appointment.dto;

import java.util.List;

public record StaffScheduleResponse(String timeZone, List<StaffScheduleDayResponse> days) {}
