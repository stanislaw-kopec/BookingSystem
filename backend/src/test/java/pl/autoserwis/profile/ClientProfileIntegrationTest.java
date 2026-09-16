package pl.autoserwis.profile;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import pl.autoserwis.PostgresTestConfiguration;
import pl.autoserwis.user.AppUser;
import pl.autoserwis.user.UserRepository;
import pl.autoserwis.user.UserRole;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static pl.autoserwis.DatabaseTestUsers.databaseUser;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.docker.compose.enabled=false")
@AutoConfigureMockMvc
@Import(PostgresTestConfiguration.class)
@ActiveProfiles("test")
@Transactional
class ClientProfileIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired UserRepository users;
    @Autowired ClientProfileRepository profiles;
    @Autowired PasswordEncoder passwords;

    @Test
    void returnsDraftWithAccountEmailBeforeFirstSave() throws Exception {
        createUser("draft-client", "draft@example.com", UserRole.CLIENT);

        mockMvc.perform(get("/api/profile/me").with(databaseUser("draft-client").roles("CLIENT")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.configured").value(false))
            .andExpect(jsonPath("$.contactEmail").value("draft@example.com"))
            .andExpect(jsonPath("$.firstName").value(""))
            .andExpect(jsonPath("$.hasCompanyData").value(false));
    }

    @Test
    void createsThenUpdatesOwnProfileAndClearsCompanyData() throws Exception {
        createUser("profile-client", "account@example.com", UserRole.CLIENT);

        mockMvc.perform(put("/api/profile/me")
                .with(databaseUser("profile-client").roles("CLIENT"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(profileJson(true)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.configured").value(true))
            .andExpect(jsonPath("$.firstName").value("Jan"))
            .andExpect(jsonPath("$.contactEmail").value("kontakt@example.com"))
            .andExpect(jsonPath("$.companyName").value("Warsztat Testowy"));

        mockMvc.perform(put("/api/profile/me")
                .with(databaseUser("profile-client").roles("CLIENT"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(profileWithoutCompanyJson()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.hasCompanyData").value(false))
            .andExpect(jsonPath("$.companyName").value(""))
            .andExpect(jsonPath("$.taxId").value(""));

        ClientProfile saved = profiles.findByUser_Id(
            users.findByUsernameIgnoreCase("profile-client").orElseThrow().getId()).orElseThrow();
        assertThat(saved.getFirstName()).isEqualTo("Jan");
        assertThat(saved.getCompanyName()).isNull();
        assertThat(saved.getTaxId()).isNull();
        assertThat(profiles.count()).isEqualTo(1);
    }

    @Test
    void neverReturnsAnotherClientsProfile() throws Exception {
        createUser("first-client", "first@example.com", UserRole.CLIENT);
        createUser("second-client", "second@example.com", UserRole.CLIENT);

        mockMvc.perform(put("/api/profile/me")
                .with(databaseUser("first-client").roles("CLIENT"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(profileWithoutCompanyJson()))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/profile/me").with(databaseUser("second-client").roles("CLIENT")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.configured").value(false))
            .andExpect(jsonPath("$.contactEmail").value("second@example.com"))
            .andExpect(jsonPath("$.firstName").value(""));
    }

    @Test
    void validatesRequiredPersonalAndCompanyFields() throws Exception {
        createUser("validation-client", "validation@example.com", UserRole.CLIENT);

        mockMvc.perform(put("/api/profile/me")
                .with(databaseUser("validation-client").roles("CLIENT"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "firstName": "", "lastName": "", "phoneNumber": "abc",
                      "contactEmail": "bad", "addressLine": "", "postalCode": "", "city": "",
                      "hasCompanyData": false
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors.firstName").exists())
            .andExpect(jsonPath("$.fieldErrors.phoneNumber").exists())
            .andExpect(jsonPath("$.fieldErrors.contactEmail").exists())
            .andExpect(jsonPath("$.fieldErrors.city").exists());

        mockMvc.perform(put("/api/profile/me")
                .with(databaseUser("validation-client").roles("CLIENT"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(profileWithEmptyCompanyJson()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors.companyName").exists())
            .andExpect(jsonPath("$.fieldErrors.taxId").exists())
            .andExpect(jsonPath("$.fieldErrors.billingAddressLine").exists())
            .andExpect(jsonPath("$.fieldErrors.billingPostalCode").exists())
            .andExpect(jsonPath("$.fieldErrors.billingCity").exists());
    }

    @Test
    void requiresAuthenticationAndCsrf() throws Exception {
        createUser("csrf-profile-client", "csrf-profile@example.com", UserRole.CLIENT);

        mockMvc.perform(get("/api/profile/me"))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(put("/api/profile/me")
                .with(databaseUser("csrf-profile-client").roles("CLIENT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(profileWithoutCompanyJson()))
            .andExpect(status().isForbidden());

        assertThat(profiles.count()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"MECHANIC", "ADMIN"})
    void staffRolesCannotUseClientProfile(String role) throws Exception {
        mockMvc.perform(get("/api/profile/me").with(databaseUser("staff").roles(role)))
            .andExpect(status().isForbidden());
    }

    private void createUser(String username, String email, UserRole role) {
        users.saveAndFlush(new AppUser(username, email, passwords.encode("test-password"), role));
    }

    private String profileJson(boolean company) {
        return """
            {
              "firstName": " Jan ", "lastName": "Kowalski", "phoneNumber": "+48 500 600 700",
              "contactEmail": "KONTAKT@EXAMPLE.COM", "addressLine": "ul. Prosta 1",
              "postalCode": "00-001", "city": "Warszawa", "hasCompanyData": %s,
              "companyName": "Warsztat Testowy", "taxId": "1234567890",
              "billingAddressLine": "ul. Firmowa 2", "billingPostalCode": "00-002",
              "billingCity": "Warszawa"
            }
            """.formatted(company);
    }

    private String profileWithoutCompanyJson() {
        return """
            {
              "firstName": "Jan", "lastName": "Kowalski", "phoneNumber": "+48 500 600 700",
              "contactEmail": "kontakt@example.com", "addressLine": "ul. Prosta 1",
              "postalCode": "00-001", "city": "Warszawa", "hasCompanyData": false,
              "companyName": "wartość do wyczyszczenia", "taxId": "1234567890",
              "billingAddressLine": "ul. Firmowa 2", "billingPostalCode": "00-002",
              "billingCity": "Warszawa"
            }
            """;
    }

    private String profileWithEmptyCompanyJson() {
        return """
            {
              "firstName": "Jan", "lastName": "Kowalski", "phoneNumber": "+48 500 600 700",
              "contactEmail": "kontakt@example.com", "addressLine": "ul. Prosta 1",
              "postalCode": "00-001", "city": "Warszawa", "hasCompanyData": true,
              "companyName": " ", "taxId": "", "billingAddressLine": null,
              "billingPostalCode": "", "billingCity": ""
            }
            """;
    }
}

