package pl.autoserwis.offering.dto;

import jakarta.validation.constraints.*;

public record WorkshopServiceRequest(
    @NotNull(message = "Select a category.")
    @Positive(message = "Select a valid category.")
    Long categoryId,
    @NotBlank(message = "Enter a service name.")
    @Size(max = 120, message = "Name can have at most 120 characters.")
    String name,
    @Size(max = 1000, message = "Description can have at most 1000 characters.")
    String description
) {}
