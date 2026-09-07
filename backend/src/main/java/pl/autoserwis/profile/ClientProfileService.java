package pl.autoserwis.profile;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.autoserwis.exception.ResourceNotFoundException;
import pl.autoserwis.profile.dto.ClientProfileRequest;
import pl.autoserwis.profile.dto.ClientProfileResponse;
import pl.autoserwis.user.AppUser;
import pl.autoserwis.user.UserRepository;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class ClientProfileService {
    private final ClientProfileRepository profiles;
    private final UserRepository users;

    public ClientProfileService(ClientProfileRepository profiles, UserRepository users) {
        this.profiles = profiles;
        this.users = users;
    }

    public ClientProfileResponse getCurrent(String username) {
        AppUser user = user(username);
        return profiles.findByUser_Id(user.getId())
            .map(profile -> response(profile, true))
            .orElseGet(() -> emptyResponse(user.getEmail()));
    }

    @Transactional
    public ClientProfileResponse saveCurrent(String username, ClientProfileRequest request) {
        validateCompany(request);
        AppUser user = user(username);
        ClientProfile profile = profiles.findByUser_Id(user.getId())
            .orElseGet(() -> new ClientProfile(user));

        boolean company = request.hasCompanyData();
        profile.update(required(request.firstName()), required(request.lastName()),
            required(request.phoneNumber()), required(request.contactEmail()).toLowerCase(Locale.ROOT),
            required(request.addressLine()), required(request.postalCode()), required(request.city()),
            company, company ? required(request.companyName()) : null,
            company ? required(request.taxId()) : null,
            company ? required(request.billingAddressLine()) : null,
            company ? required(request.billingPostalCode()) : null,
            company ? required(request.billingCity()) : null);
        return response(profiles.save(profile), true);
    }

    private void validateCompany(ClientProfileRequest request) {
        if (!request.hasCompanyData()) return;
        Map<String, String> errors = new LinkedHashMap<>();
        requiredCompany(errors, "companyName", request.companyName(), "Podaj nazwę firmy.");
        requiredCompany(errors, "taxId", request.taxId(), "Podaj NIP.");
        requiredCompany(errors, "billingAddressLine", request.billingAddressLine(), "Podaj adres rozliczeniowy.");
        requiredCompany(errors, "billingPostalCode", request.billingPostalCode(), "Podaj kod pocztowy.");
        requiredCompany(errors, "billingCity", request.billingCity(), "Podaj miejscowość.");
        if (!errors.isEmpty()) throw new ProfileValidationException(errors);
    }

    private void requiredCompany(Map<String, String> errors, String field, String value, String message) {
        if (value == null || value.isBlank()) errors.put(field, message);
    }

    private AppUser user(String username) {
        return users.findByUsernameIgnoreCase(username)
            .orElseThrow(() -> new ResourceNotFoundException("Nie znaleziono użytkownika."));
    }

    private String required(String value) {
        return value.strip();
    }

    private ClientProfileResponse emptyResponse(String accountEmail) {
        return new ClientProfileResponse(false, "", "", "", accountEmail, "", "", "",
            false, "", "", "", "", "");
    }

    private ClientProfileResponse response(ClientProfile profile, boolean configured) {
        return new ClientProfileResponse(configured, profile.getFirstName(), profile.getLastName(),
            profile.getPhoneNumber(), profile.getContactEmail(), profile.getAddressLine(),
            profile.getPostalCode(), profile.getCity(), profile.isHasCompanyData(),
            text(profile.getCompanyName()), text(profile.getTaxId()), text(profile.getBillingAddressLine()),
            text(profile.getBillingPostalCode()), text(profile.getBillingCity()));
    }

    private String text(String value) {
        return value == null ? "" : value;
    }
}
