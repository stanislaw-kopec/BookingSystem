package pl.autoserwis.offering.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
    @NotBlank(message = "Podaj nazwę kategorii.")
    @Size(max = 100, message = "Nazwa może mieć najwyżej 100 znaków.")
    String name,
    @Size(max = 500, message = "Opis może mieć najwyżej 500 znaków.")
    String description
) {}
