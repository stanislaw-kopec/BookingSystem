package pl.autoserwis.appointment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CompleteRepairRequest(
    @NotBlank(message = "Opisz wykonane prace.")
    @Size(min = 10, max = 2000, message = "Opis wykonanych prac musi mieć od 10 do 2000 znaków.")
    String repairDescription,

    @NotNull(message = "Podaj kwotę brutto do zapłaty.")
    @DecimalMin(value = "0.01", message = "Kwota brutto musi być większa od 0.")
    @Digits(integer = 8, fraction = 2, message = "Kwota brutto może mieć maksymalnie 8 cyfr przed przecinkiem i 2 po przecinku.")
    BigDecimal totalGrossAmount
) {}
