package pl.autoserwis.appointment.dto;

import pl.autoserwis.appointment.AppointmentRequesterType;
import pl.autoserwis.appointment.AppointmentStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AppointmentResponse(
    Long id,
    UUID reference,
    AppointmentRequesterType requesterType,
    AppointmentStatus status,
    Long vehicleId,
    String vehicleMake,
    String vehicleModel,
    int vehicleProductionYear,
    String vehicleRegistrationNumber,
    String vehicleVin,
    String firstName,
    String lastName,
    String phoneNumber,
    String contactEmail,
    OffsetDateTime requestedStartAt,
    OffsetDateTime currentStartAt,
    String problemDescription,
    String staffMessage,
    OffsetDateTime createdAt,
    OffsetDateTime staffActionAt,
    String staffActionBy,
    OffsetDateTime clientConfirmedAt
) {}
