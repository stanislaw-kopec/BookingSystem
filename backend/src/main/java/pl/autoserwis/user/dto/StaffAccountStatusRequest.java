package pl.autoserwis.user.dto;

import jakarta.validation.constraints.NotNull;

public record StaffAccountStatusRequest(
    @NotNull(message = "Wybierz status konta.")
    Boolean enabled
) {}
