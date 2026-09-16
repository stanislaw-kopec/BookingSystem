package pl.autoserwis.appointment.dto;

import java.time.LocalDate;
import java.util.List;

public record StaffScheduleDayResponse(LocalDate date, int capacity, int remainingCapacity,
        boolean closed, List<ScheduleAppointmentResponse> appointments) {}
