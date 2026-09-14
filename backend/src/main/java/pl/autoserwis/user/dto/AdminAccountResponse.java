package pl.autoserwis.user.dto;

import pl.autoserwis.user.UserRole;

public record AdminAccountResponse(
    Long id,
    String username,
    String email,
    UserRole role,
    boolean enabled
) {}
