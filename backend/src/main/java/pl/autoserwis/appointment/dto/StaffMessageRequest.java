package pl.autoserwis.appointment.dto;

import jakarta.validation.constraints.Size;

public record StaffMessageRequest(
    @Size(max = 500, message = "Wiadomość może mieć maksymalnie 500 znaków.")
    String message
) {}
