package pl.autoserwis.offering.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
    @NotBlank(message = "Enter a category name.")
    @Size(max = 100, message = "Name can have at most 100 characters.")
    String name,
    @Size(max = 500, message = "Description can have at most 500 characters.")
    String description
) {}
