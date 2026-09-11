package pl.autoserwis.appointment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pl.autoserwis.appointment.RepairItemType;

import java.math.BigDecimal;

public record RepairItemRequest(
    @NotNull(message = "Select an item type.")
    RepairItemType type,

    @NotBlank(message = "Enter an item name.")
    @Size(max = 160, message = "Item name can have at most 160 characters.")
    String name,

    @NotNull(message = "Enter a quantity.")
    @DecimalMin(value = "0.01", message = "Quantity must be greater than 0.")
    @Digits(integer = 6, fraction = 2, message = "Quantity can have at most 6 integer digits and 2 decimal places.")
    BigDecimal quantity,

    @NotNull(message = "Enter an item gross price.")
    @DecimalMin(value = "0.01", message = "Gross price must be greater than 0.")
    @Digits(integer = 8, fraction = 2, message = "Gross price can have at most 8 integer digits and 2 decimal places.")
    BigDecimal unitGrossAmount
) {}
