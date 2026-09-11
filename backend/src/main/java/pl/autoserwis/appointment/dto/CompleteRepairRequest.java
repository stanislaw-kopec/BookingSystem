package pl.autoserwis.appointment.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CompleteRepairRequest(
    @NotBlank(message = "Describe the completed work.")
    @Size(min = 10, max = 2000, message = "Completed work description must be between 10 and 2000 characters.")
    String repairDescription,

    @NotEmpty(message = "Add at least one repair item.")
    @Size(max = 30, message = "One repair order can have at most 30 items.")
    List<@Valid RepairItemRequest> repairItems
) {}
