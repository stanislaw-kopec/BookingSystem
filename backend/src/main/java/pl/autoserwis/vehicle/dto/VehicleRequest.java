package pl.autoserwis.vehicle.dto;

import jakarta.validation.constraints.*;

public record VehicleRequest(
    @NotBlank(message = "Podaj markę.")
    @Size(max = 80, message = "Marka może mieć maksymalnie 80 znaków.")
    String make,

    @NotBlank(message = "Podaj model.")
    @Size(max = 80, message = "Model może mieć maksymalnie 80 znaków.")
    String model,

    @NotNull(message = "Podaj rok produkcji.")
    @Min(value = 1886, message = "Rok produkcji nie może być wcześniejszy niż 1886.")
    Integer productionYear,

    @NotBlank(message = "Podaj numer rejestracyjny.")
    @Size(min = 2, max = 20, message = "Numer rejestracyjny musi mieć od 2 do 20 znaków.")
    @Pattern(regexp = "^[A-Za-z0-9 -]+$", message = "Numer rejestracyjny zawiera niedozwolone znaki.")
    String registrationNumber,

    @Pattern(regexp = "^(?:\\s*|[A-HJ-NPR-Za-hj-npr-z0-9]{17})$",
        message = "VIN musi mieć 17 znaków i nie może zawierać liter I, O ani Q.")
    String vin
) {}
