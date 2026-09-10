package pl.autoserwis.vehicle.dto;
import java.math.BigDecimal;
import pl.autoserwis.appointment.dto.RepairItemResponse;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
public record RepairHistoryEntryResponse(
    Long appointmentId,
    UUID appointmentReference,
    OffsetDateTime visitDate,
    String repairDescription,
    BigDecimal totalGrossAmount,
    List<RepairItemResponse> repairItems,
    OffsetDateTime repairCompletedAt,
    String repairCompletedBy,
    OffsetDateTime vehiclePickedUpAt,
    String vehiclePickedUpBy
) {}
