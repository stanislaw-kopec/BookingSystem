package pl.autoserwis.profile.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ClientProfileRequest(
    @NotBlank(message = "Podaj imię.")
    @Size(max = 60, message = "Imię może mieć maksymalnie 60 znaków.")
    String firstName,
    @NotBlank(message = "Podaj nazwisko.")
    @Size(max = 80, message = "Nazwisko może mieć maksymalnie 80 znaków.")
    String lastName,
    @NotBlank(message = "Podaj numer telefonu.")
    @Size(max = 30, message = "Numer telefonu może mieć maksymalnie 30 znaków.")
    @Pattern(regexp = "^[0-9+() .-]{7,30}$", message = "Podaj prawidłowy numer telefonu.")
    String phoneNumber,
    @NotBlank(message = "Podaj kontaktowy adres e-mail.")
    @Size(max = 254, message = "Adres e-mail może mieć maksymalnie 254 znaki.")
    @Email(message = "Podaj prawidłowy adres e-mail.")
    String contactEmail,
    @NotBlank(message = "Podaj ulicę i numer.")
    @Size(max = 150, message = "Adres może mieć maksymalnie 150 znaków.")
    String addressLine,
    @NotBlank(message = "Podaj kod pocztowy.")
    @Size(max = 20, message = "Kod pocztowy może mieć maksymalnie 20 znaków.")
    String postalCode,
    @NotBlank(message = "Podaj miejscowość.")
    @Size(max = 80, message = "Miejscowość może mieć maksymalnie 80 znaków.")
    String city,
    boolean hasCompanyData,
    @Size(max = 150, message = "Nazwa firmy może mieć maksymalnie 150 znaków.")
    String companyName,
    @Size(max = 32, message = "NIP może mieć maksymalnie 32 znaki.")
    String taxId,
    @Size(max = 150, message = "Adres rozliczeniowy może mieć maksymalnie 150 znaków.")
    String billingAddressLine,
    @Size(max = 20, message = "Kod pocztowy może mieć maksymalnie 20 znaków.")
    String billingPostalCode,
    @Size(max = 80, message = "Miejscowość może mieć maksymalnie 80 znaków.")
    String billingCity
) {}
