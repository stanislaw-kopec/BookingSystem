package pl.autoserwis.appointment.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CompleteRepairRequest(
    @NotBlank(message = "Opisz wykonane prace.")
    @Size(min = 10, max = 2000, message = "Opis wykonanych prac musi mieć od 10 do 2000 znaków.")
    String repairDescription,

    @NotEmpty(message = "Dodaj co najmniej jedną pozycję naprawy.")
    @Size(max = 30, message = "Jedno zlecenie może mieć maksymalnie 30 pozycji.")
    List<@Valid RepairItemRequest> repairItems
) {}
