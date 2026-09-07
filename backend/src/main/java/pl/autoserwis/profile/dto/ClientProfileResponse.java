package pl.autoserwis.profile.dto;

public record ClientProfileResponse(
    boolean configured,
    String firstName,
    String lastName,
    String phoneNumber,
    String contactEmail,
    String addressLine,
    String postalCode,
    String city,
    boolean hasCompanyData,
    String companyName,
    String taxId,
    String billingAddressLine,
    String billingPostalCode,
    String billingCity
) {}
