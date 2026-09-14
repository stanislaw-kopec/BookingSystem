package pl.autoserwis.user.dto;

import java.util.List;

public record AdminAccountPageResponse(
    List<AdminAccountResponse> content,
    int page,
    int size,
    long totalElements,
    int totalPages
) {}
