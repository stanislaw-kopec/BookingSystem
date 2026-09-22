package pl.autoserwis.appointment.repair.dto;

import pl.autoserwis.appointment.domain.RepairItemType;

import java.math.BigDecimal;

public record RepairItemResponse(
    Long id,
    RepairItemType type,
    String name,
    BigDecimal quantity,
    BigDecimal unitGrossAmount,
    BigDecimal totalGrossAmount
) {}
