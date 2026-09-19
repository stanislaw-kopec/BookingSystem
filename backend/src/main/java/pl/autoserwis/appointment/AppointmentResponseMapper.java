package pl.autoserwis.appointment;

import org.springframework.stereotype.Component;
import pl.autoserwis.appointment.dto.AppointmentResponse;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;

@Component
public class AppointmentResponseMapper {
    private final Clock clock;
    private final RepairItemResponseMapper repairItems;

    public AppointmentResponseMapper(Clock workshopClock, RepairItemResponseMapper repairItems) {
        this.clock = workshopClock;
        this.repairItems = repairItems;
    }

    public AppointmentResponse toResponse(AppointmentRequest appointment) {
        return new AppointmentResponse(
            appointment.getId(), appointment.getReference(), appointment.getRequesterType(),
            appointment.getStatus(), appointment.getVehicle() == null ? null : appointment.getVehicle().getId(),
            appointment.getVehicleMake(), appointment.getVehicleModel(),
            appointment.getVehicleProductionYear(), appointment.getVehicleRegistrationNumber(),
            text(appointment.getVehicleVin()), appointment.getFirstName(), appointment.getLastName(),
            text(appointment.getPhoneNumber()), text(appointment.getContactEmail()),
            offset(appointment.getRequestedStartAt()), offset(appointment.getCurrentStartAt()),
            appointment.getProblemDescription(), text(appointment.getStaffMessage()),
            offset(appointment.getCreatedAt()), offset(appointment.getStaffActionAt()),
            appointment.getStaffActionBy() == null ? null : appointment.getStaffActionBy().getUsername(),
            offset(appointment.getClientConfirmedAt()), text(appointment.getRepairDescription()),
            appointment.getTotalGrossAmount(), repairItems.toResponses(appointment),
            offset(appointment.getRepairCompletedAt()),
            appointment.getRepairCompletedBy() == null ? null : appointment.getRepairCompletedBy().getUsername(),
            offset(appointment.getVehiclePickedUpAt()),
            appointment.getVehiclePickedUpBy() == null ? null : appointment.getVehiclePickedUpBy().getUsername());
    }

    private OffsetDateTime offset(Instant value) {
        return value == null ? null : value.atZone(clock.getZone()).toOffsetDateTime();
    }

    private String text(String value) {
        return value == null ? "" : value;
    }
}
