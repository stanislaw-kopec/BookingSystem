package pl.autoserwis.appointment.repair;

import org.springframework.stereotype.Component;
import pl.autoserwis.appointment.domain.AppointmentRequest;
import pl.autoserwis.appointment.repair.dto.RepairHistoryEntryResponse;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;

@Component
public class RepairHistoryMapper {
    private final Clock clock;
    private final RepairItemResponseMapper repairItems;

    public RepairHistoryMapper(Clock workshopClock, RepairItemResponseMapper repairItems) {
        this.clock = workshopClock;
        this.repairItems = repairItems;
    }

    public RepairHistoryEntryResponse toResponse(AppointmentRequest appointment) {
        return new RepairHistoryEntryResponse(appointment.getId(), appointment.getReference(),
            offset(appointment.getCurrentStartAt()), appointment.getRepairDescription(),
            appointment.getTotalGrossAmount(), repairItems.toResponses(appointment),
            offset(appointment.getRepairCompletedAt()), appointment.getRepairCompletedBy().getUsername(),
            offset(appointment.getVehiclePickedUpAt()), appointment.getVehiclePickedUpBy().getUsername());
    }

    private OffsetDateTime offset(Instant value) {
        return value.atZone(clock.getZone()).toOffsetDateTime();
    }
}
