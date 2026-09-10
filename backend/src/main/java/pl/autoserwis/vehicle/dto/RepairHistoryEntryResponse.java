package pl.autoserwis.vehicle.dto;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
public record RepairHistoryEntryResponse(
    Long appointmentId,
    UUID appointmentReference,
    OffsetDateTime visitDate,
    String repairDescription,
    BigDecimal totalGrossAmount,
    OffsetDateTime repairCompletedAt,
    String repairCompletedBy,
    OffsetDateTime vehiclePickedUpAt,
    String vehiclePickedUpBy
) {}
