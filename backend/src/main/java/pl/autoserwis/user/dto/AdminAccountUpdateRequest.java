package pl.autoserwis.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminAccountUpdateRequest(
    @NotBlank(message = "Enter a username.")
    @Size(min = 3, max = 30, message = "Username must be between 3 and 30 characters.")
    @Pattern(regexp = "^[A-Za-z0-9._-]+$",
        message = "Username may contain ASCII letters, digits, dot, hyphen and underscore.")
    String username,

    @NotBlank(message = "Enter an email address.")
    @Size(max = 254, message = "Email address can have at most 254 characters.")
    @Email(message = "Enter a valid email address.")
    String email
) {}
