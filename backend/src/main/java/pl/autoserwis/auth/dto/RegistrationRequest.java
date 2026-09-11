package pl.autoserwis.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegistrationRequest(
    @NotBlank(message = "Enter a username.")
    @Size(min = 3, max = 30, message = "Username must be between 3 and 30 characters.")
    @Pattern(regexp = "^[A-Za-z0-9._-]+$",
        message = "Username may contain ASCII letters, digits, dot, hyphen and underscore.")
    String username,

    @NotBlank(message = "Enter an email address.")
    @Size(max = 254, message = "Email address can have at most 254 characters.")
    @Email(message = "Enter a valid email address.")
    String email,

    @NotBlank(message = "Enter a password.")
    @Size(min = 8, max = 64, message = "Password must be between 8 and 64 characters.")
    String password,

    @NotBlank(message = "Repeat the password.")
    @Size(max = 64, message = "Repeated password can have at most 64 characters.")
    String passwordConfirmation
) {}
