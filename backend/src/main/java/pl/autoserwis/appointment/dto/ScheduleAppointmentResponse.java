package pl.autoserwis.appointment.dto;

import pl.autoserwis.appointment.AppointmentStatus;
import java.time.Instant;
import java.util.UUID;

public record ScheduleAppointmentResponse(Long id, UUID reference, AppointmentStatus status,
        Instant currentStartAt, String vehicleMake, String vehicleModel, String vehicleRegistrationNumber,
        String firstName, String lastName, String problemSummary) {}
