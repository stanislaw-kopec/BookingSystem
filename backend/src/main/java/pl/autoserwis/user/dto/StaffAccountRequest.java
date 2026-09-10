package pl.autoserwis.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record StaffAccountRequest(
    @NotBlank(message = "Podaj login.")
    @Size(min = 3, max = 30, message = "Login musi mieć od 3 do 30 znaków.")
    @Pattern(regexp = "^[A-Za-z0-9._-]+$",
        message = "Login może zawierać litery bez polskich znaków, cyfry, kropkę, myślnik i podkreślenie.")
    String username,

    @NotBlank(message = "Podaj adres e-mail.")
    @Size(max = 254, message = "Adres e-mail może mieć maksymalnie 254 znaki.")
    @Email(message = "Podaj prawidłowy adres e-mail.")
    String email,

    @NotBlank(message = "Podaj hasło.")
    @Size(min = 8, max = 64, message = "Hasło musi mieć od 8 do 64 znaków.")
    String password,

    @NotBlank(message = "Powtórz hasło.")
    @Size(max = 64, message = "Powtórzone hasło może mieć maksymalnie 64 znaki.")
    String passwordConfirmation
) {}
