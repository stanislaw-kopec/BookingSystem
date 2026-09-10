package pl.autoserwis.appointment.dto;

import java.util.List;

public record AppointmentPageResponse(
    List<AppointmentResponse> content,
    int page,
    int size,
    long totalElements,
    int totalPages
) {
}
