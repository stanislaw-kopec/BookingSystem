package pl.autoserwis.appointment.dto;

import pl.autoserwis.appointment.RepairItemType;

import java.math.BigDecimal;

public record RepairItemResponse(
    Long id,
    RepairItemType type,
    String name,
    BigDecimal quantity,
    BigDecimal unitGrossAmount,
    BigDecimal totalGrossAmount
) {}
