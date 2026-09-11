package pl.autoserwis.appointment.dto;

import jakarta.validation.constraints.Size;

public record StaffMessageRequest(
    @Size(max = 500, message = "Message can have at most 500 characters.")
    String message
) {}
