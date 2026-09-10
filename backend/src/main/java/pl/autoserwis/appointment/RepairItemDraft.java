package pl.autoserwis.appointment;

import java.math.BigDecimal;

public record RepairItemDraft(
    RepairItemType type,
    String name,
    BigDecimal quantity,
    BigDecimal unitGrossAmount
) {}
