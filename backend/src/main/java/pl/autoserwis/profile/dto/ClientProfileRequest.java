package pl.autoserwis.profile.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ClientProfileRequest(
    @NotBlank(message = "Enter a first name.")
    @Size(max = 60, message = "First name can have at most 60 characters.")
    String firstName,
    @NotBlank(message = "Enter a last name.")
    @Size(max = 80, message = "Last name can have at most 80 characters.")
    String lastName,
    @NotBlank(message = "Enter a phone number.")
    @Size(max = 30, message = "Phone number can have at most 30 characters.")
    @Pattern(regexp = "^[0-9+() .-]{7,30}$", message = "Enter a valid phone number.")
    String phoneNumber,
    @NotBlank(message = "Enter a contact email address.")
    @Size(max = 254, message = "Email address can have at most 254 characters.")
    @Email(message = "Enter a valid email address.")
    String contactEmail,
    @NotBlank(message = "Enter street and building number.")
    @Size(max = 150, message = "Address can have at most 150 characters.")
    String addressLine,
    @NotBlank(message = "Enter a postal code.")
    @Size(max = 20, message = "Postal code can have at most 20 characters.")
    String postalCode,
    @NotBlank(message = "Enter a city.")
    @Size(max = 80, message = "City can have at most 80 characters.")
    String city,
    boolean hasCompanyData,
    @Size(max = 150, message = "Company name can have at most 150 characters.")
    String companyName,
    @Size(max = 32, message = "Tax ID can have at most 32 characters.")
    String taxId,
    @Size(max = 150, message = "Billing address can have at most 150 characters.")
    String billingAddressLine,
    @Size(max = 20, message = "Postal code can have at most 20 characters.")
    String billingPostalCode,
    @Size(max = 80, message = "City can have at most 80 characters.")
    String billingCity
) {}
