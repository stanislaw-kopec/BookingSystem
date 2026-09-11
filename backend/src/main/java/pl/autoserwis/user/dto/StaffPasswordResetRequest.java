package pl.autoserwis.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StaffPasswordResetRequest(
    @NotBlank(message = "Enter a new password.")
    @Size(min = 8, max = 64, message = "Password must be between 8 and 64 characters.")
    String password,

    @NotBlank(message = "Repeat the new password.")
    @Size(max = 64, message = "Repeated password can have at most 64 characters.")
    String passwordConfirmation
) {}
