package pl.autoserwis.appointment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pl.autoserwis.appointment.RepairItemType;

import java.math.BigDecimal;

public record RepairItemRequest(
    @NotNull(message = "Wybierz typ pozycji.")
    RepairItemType type,

    @NotBlank(message = "Podaj nazwę pozycji.")
    @Size(max = 160, message = "Nazwa pozycji może mieć maksymalnie 160 znaków.")
    String name,

    @NotNull(message = "Podaj ilość.")
    @DecimalMin(value = "0.01", message = "Ilość musi być większa od 0.")
    @Digits(integer = 6, fraction = 2, message = "Ilość może mieć maksymalnie 6 cyfr przed przecinkiem i 2 po przecinku.")
    BigDecimal quantity,

    @NotNull(message = "Podaj cenę brutto pozycji.")
    @DecimalMin(value = "0.01", message = "Cena brutto musi być większa od 0.")
    @Digits(integer = 8, fraction = 2, message = "Cena brutto może mieć maksymalnie 8 cyfr przed przecinkiem i 2 po przecinku.")
    BigDecimal unitGrossAmount
) {}
