package pl.autoserwis.user.dto;

import jakarta.validation.constraints.NotNull;

public record StaffAccountStatusRequest(
    @NotNull(message = "Select an account status.")
    Boolean enabled
) {}
