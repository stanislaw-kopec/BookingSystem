package pl.autoserwis.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StaffPasswordResetRequest(
    @NotBlank(message = "Podaj nowe hasło.")
    @Size(min = 8, max = 64, message = "Hasło musi mieć od 8 do 64 znaków.")
    String password,

    @NotBlank(message = "Powtórz nowe hasło.")
    @Size(max = 64, message = "Powtórzone hasło może mieć maksymalnie 64 znaki.")
    String passwordConfirmation
) {}
