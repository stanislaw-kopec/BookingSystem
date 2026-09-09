package pl.autoserwis.appointment.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record GuestAppointmentRequest(
    @NotBlank(message = "Podaj imię.")
    @Size(max = 60, message = "Imię może mieć maksymalnie 60 znaków.")
    String firstName,

    @NotBlank(message = "Podaj nazwisko.")
    @Size(max = 80, message = "Nazwisko może mieć maksymalnie 80 znaków.")
    String lastName,

    @Size(max = 30, message = "Numer telefonu może mieć maksymalnie 30 znaków.")
    @Pattern(regexp = "^(?:\\s*|[0-9+() .-]{7,30})$", message = "Podaj prawidłowy numer telefonu.")
    String phoneNumber,

    @Size(max = 254, message = "Adres e-mail może mieć maksymalnie 254 znaki.")
    @Email(message = "Podaj prawidłowy adres e-mail.")
    String contactEmail,

    @NotBlank(message = "Podaj markę.")
    @Size(max = 80, message = "Marka może mieć maksymalnie 80 znaków.")
    String vehicleMake,

    @NotBlank(message = "Podaj model.")
    @Size(max = 80, message = "Model może mieć maksymalnie 80 znaków.")
    String vehicleModel,

    @NotNull(message = "Podaj rok produkcji.")
    @Min(value = 1886, message = "Rok produkcji nie może być wcześniejszy niż 1886.")
    Integer vehicleProductionYear,

    @NotBlank(message = "Podaj numer rejestracyjny.")
    @Size(min = 2, max = 20, message = "Numer rejestracyjny musi mieć od 2 do 20 znaków.")
    @Pattern(regexp = "^[A-Za-z0-9 -]+$", message = "Numer rejestracyjny zawiera niedozwolone znaki.")
    String vehicleRegistrationNumber,

    @Pattern(regexp = "^(?:\\s*|[A-HJ-NPR-Za-hj-npr-z0-9]{17})$",
        message = "VIN musi mieć 17 znaków i nie może zawierać liter I, O ani Q.")
    String vehicleVin,

    @NotNull(message = "Wybierz dzień wizyty.")
    LocalDate visitDate,

    @NotBlank(message = "Opisz problem z pojazdem.")
    @Size(min = 10, max = 2000, message = "Opis problemu musi mieć od 10 do 2000 znaków.")
    String problemDescription
) {}
