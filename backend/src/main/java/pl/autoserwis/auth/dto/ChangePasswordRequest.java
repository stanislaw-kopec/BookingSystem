package pl.autoserwis.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
    @NotBlank(message = "Enter the current password.")
    @Size(max = 64, message = "Current password can have at most 64 characters.")
    String currentPassword,

    @NotBlank(message = "Enter a new password.")
    @Size(min = 8, max = 64, message = "New password must be between 8 and 64 characters.")
    String newPassword,

    @NotBlank(message = "Repeat the new password.")
    @Size(max = 64, message = "Repeated password can have at most 64 characters.")
    String newPasswordConfirmation
) {}
