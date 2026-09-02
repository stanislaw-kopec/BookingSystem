package pl.autoserwis.offering.dto;

import jakarta.validation.constraints.*;

public record WorkshopServiceRequest(
    @NotNull(message = "Wybierz kategorię.")
    @Positive(message = "Wybierz prawidłową kategorię.")
    Long categoryId,
    @NotBlank(message = "Podaj nazwę usługi.")
    @Size(max = 120, message = "Nazwa może mieć najwyżej 120 znaków.")
    String name,
    @Size(max = 1000, message = "Opis może mieć najwyżej 1000 znaków.")
    String description
) {}
