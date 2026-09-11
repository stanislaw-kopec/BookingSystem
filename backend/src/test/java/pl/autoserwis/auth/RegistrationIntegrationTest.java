package pl.autoserwis.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import pl.autoserwis.PostgresTestConfiguration;
import pl.autoserwis.user.AppUser;
import pl.autoserwis.user.UserRepository;
import pl.autoserwis.user.UserRole;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.docker.compose.enabled=false")
@AutoConfigureMockMvc
@Import(PostgresTestConfiguration.class)
@ActiveProfiles("test")
class RegistrationIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwords;

    @Test
    void createsClientWithNormalizedEmailAndAllowsLogin() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(registrationJson(
                    "new-client", "NEW.CLIENT@Example.COM", "safe-password-2026", "safe-password-2026")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.username").value("new-client"));

        AppUser saved = users.findByUsernameIgnoreCase("NEW-CLIENT").orElseThrow();
        assertThat(saved.getEmail()).isEqualTo("new.client@example.com");
        assertThat(saved.getRole()).isEqualTo(UserRole.CLIENT);
        assertThat(saved.getPasswordHash()).isNotEqualTo("safe-password-2026");
        assertThat(passwords.matches("safe-password-2026", saved.getPasswordHash())).isTrue();

        MockHttpSession session = (MockHttpSession) mockMvc.perform(post("/api/auth/login")
                .with(csrf())
                .param("username", "NEW-CLIENT")
                .param("password", "safe-password-2026"))
            .andExpect(status().isNoContent())
            .andReturn().getRequest().getSession(false);

        mockMvc.perform(get("/api/auth/me").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.user.username").value("new-client"))
            .andExpect(jsonPath("$.user.roles[0]").value("CLIENT"));
    }

    @Test
    void publicRegistrationIgnoresSubmittedRoleAndCreatesClientOnly() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username": "role-smuggling-client",
                      "email": "role-smuggling@example.com",
                      "password": "safe-password-2026",
                      "passwordConfirmation": "safe-password-2026",
                      "role": "ADMIN",
                      "enabled": false
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.username").value("role-smuggling-client"));

        AppUser saved = users.findByUsernameIgnoreCase("role-smuggling-client").orElseThrow();
        assertThat(saved.getRole()).isEqualTo(UserRole.CLIENT);
        assertThat(saved.isEnabled()).isTrue();
    }


    @Test
    void adminCreatesMechanicAccount() throws Exception {
        mockMvc.perform(post("/api/admin/staff/mechanics")
                .with(user("admin").roles("ADMIN"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(registrationJson(
                    "new-mechanic", "NEW.MECHANIC@Example.COM", "mechanic-password-2026", "mechanic-password-2026")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.username").value("new-mechanic"))
            .andExpect(jsonPath("$.email").value("new.mechanic@example.com"))
            .andExpect(jsonPath("$.role").value("MECHANIC"))
            .andExpect(jsonPath("$.enabled").value(true));

        AppUser saved = users.findByUsernameIgnoreCase("NEW-MECHANIC").orElseThrow();
        assertThat(saved.getRole()).isEqualTo(UserRole.MECHANIC);
        assertThat(passwords.matches("mechanic-password-2026", saved.getPasswordHash())).isTrue();

        mockMvc.perform(get("/api/admin/staff/mechanics")
                .with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].username", hasItem("new-mechanic")));
    }

    @Test
    void onlyAdminCreatesMechanicAccounts() throws Exception {
        mockMvc.perform(post("/api/admin/staff/mechanics")
                .with(user("mechanic").roles("MECHANIC"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(registrationJson(
                    "blocked-mechanic", "blocked@example.com", "mechanic-password", "mechanic-password")))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/staff/mechanics")
                .with(user("client").roles("CLIENT")))
            .andExpect(status().isForbidden());

        assertThat(users.findByUsernameIgnoreCase("blocked-mechanic")).isEmpty();
    }

    @Test
    void anonymousCannotAccessStaffAccountPanel() throws Exception {
        AppUser mechanic = users.saveAndFlush(new AppUser(
            "anonymous-boundary-mechanic", "anonymous-boundary-mechanic@example.com",
            passwords.encode("old-password"), UserRole.MECHANIC));

        mockMvc.perform(get("/api/admin/staff/mechanics"))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/admin/staff/mechanics")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(registrationJson(
                    "anonymous-created-mechanic", "anonymous-created@example.com",
                    "mechanic-password", "mechanic-password")))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(put("/api/admin/staff/mechanics/{mechanicId}", mechanic.getId())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(staffUpdateJson("anonymous-updated", "anonymous-updated@example.com")))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(put("/api/admin/staff/mechanics/{mechanicId}/password", mechanic.getId())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(passwordResetJson("new-password-2026", "new-password-2026")))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(put("/api/admin/staff/mechanics/{mechanicId}/status", mechanic.getId())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(statusJson(false)))
            .andExpect(status().isUnauthorized());

        assertThat(users.findByUsernameIgnoreCase("anonymous-created-mechanic")).isEmpty();
    }

    @Test
    void clientAndMechanicCannotManageStaffAccounts() throws Exception {
        AppUser mechanic = users.saveAndFlush(new AppUser(
            "role-boundary-mechanic", "role-boundary-mechanic@example.com",
            passwords.encode("old-password"), UserRole.MECHANIC));

        for (String role : new String[] {"CLIENT", "MECHANIC"}) {
            mockMvc.perform(get("/api/admin/staff/mechanics")
                    .with(user("blocked-" + role.toLowerCase()).roles(role)))
                .andExpect(status().isForbidden());

            mockMvc.perform(post("/api/admin/staff/mechanics")
                    .with(user("blocked-" + role.toLowerCase()).roles(role))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(registrationJson(
                        "blocked-" + role.toLowerCase() + "-mechanic",
                        "blocked-" + role.toLowerCase() + "@example.com",
                        "mechanic-password", "mechanic-password")))
                .andExpect(status().isForbidden());

            mockMvc.perform(put("/api/admin/staff/mechanics/{mechanicId}", mechanic.getId())
                    .with(user("blocked-" + role.toLowerCase()).roles(role))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(staffUpdateJson("blocked-update-" + role.toLowerCase(),
                        "blocked-update-" + role.toLowerCase() + "@example.com")))
                .andExpect(status().isForbidden());

            mockMvc.perform(put("/api/admin/staff/mechanics/{mechanicId}/password", mechanic.getId())
                    .with(user("blocked-" + role.toLowerCase()).roles(role))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(passwordResetJson("new-password-2026", "new-password-2026")))
                .andExpect(status().isForbidden());

            mockMvc.perform(put("/api/admin/staff/mechanics/{mechanicId}/status", mechanic.getId())
                    .with(user("blocked-" + role.toLowerCase()).roles(role))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(statusJson(false)))
                .andExpect(status().isForbidden());
        }

        AppUser unchanged = users.findById(mechanic.getId()).orElseThrow();
        assertThat(unchanged.getUsername()).isEqualTo("role-boundary-mechanic");
        assertThat(unchanged.isEnabled()).isTrue();
        assertThat(passwords.matches("old-password", unchanged.getPasswordHash())).isTrue();
        assertThat(users.findByUsernameIgnoreCase("blocked-client-mechanic")).isEmpty();
        assertThat(users.findByUsernameIgnoreCase("blocked-mechanic-mechanic")).isEmpty();
    }

    @Test
    void staffAccountMutationsRequireCsrfToken() throws Exception {
        AppUser mechanic = users.saveAndFlush(new AppUser(
            "csrf-staff-mechanic", "csrf-staff-mechanic@example.com",
            passwords.encode("old-password"), UserRole.MECHANIC));

        mockMvc.perform(post("/api/admin/staff/mechanics")
                .with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(registrationJson(
                    "csrf-created-mechanic", "csrf-created@example.com",
                    "mechanic-password", "mechanic-password")))
            .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/admin/staff/mechanics/{mechanicId}", mechanic.getId())
                .with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(staffUpdateJson("csrf-updated-mechanic", "csrf-updated@example.com")))
            .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/admin/staff/mechanics/{mechanicId}/password", mechanic.getId())
                .with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(passwordResetJson("new-password-2026", "new-password-2026")))
            .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/admin/staff/mechanics/{mechanicId}/status", mechanic.getId())
                .with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(statusJson(false)))
            .andExpect(status().isForbidden());

        AppUser unchanged = users.findById(mechanic.getId()).orElseThrow();
        assertThat(unchanged.getUsername()).isEqualTo("csrf-staff-mechanic");
        assertThat(unchanged.isEnabled()).isTrue();
        assertThat(passwords.matches("old-password", unchanged.getPasswordHash())).isTrue();
        assertThat(users.findByUsernameIgnoreCase("csrf-created-mechanic")).isEmpty();
    }

    @Test
    void validatesMechanicAccountLikeRegistration() throws Exception {
        users.saveAndFlush(new AppUser(
            "existing-mechanic", "existing-mechanic@example.com", passwords.encode("existing-password"), UserRole.MECHANIC));

        mockMvc.perform(post("/api/admin/staff/mechanics")
                .with(user("admin").roles("ADMIN"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(registrationJson(
                    "EXISTING-MECHANIC", "other@example.com", "password-one", "password-two")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors.passwordConfirmation").exists());

        mockMvc.perform(post("/api/admin/staff/mechanics")
                .with(user("admin").roles("ADMIN"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(registrationJson(
                    "EXISTING-MECHANIC", "other@example.com", "new-password", "new-password")))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.fieldErrors.username").exists());
    }

    @Test
    void adminUpdatesMechanicAccount() throws Exception {
        AppUser mechanic = users.saveAndFlush(new AppUser(
            "editable-mechanic", "editable-mechanic@example.com", passwords.encode("old-password"), UserRole.MECHANIC));

        mockMvc.perform(put("/api/admin/staff/mechanics/{mechanicId}", mechanic.getId())
                .with(user("admin").roles("ADMIN"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username": "updated-mechanic",
                      "email": "UPDATED.MECHANIC@Example.COM"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("updated-mechanic"))
            .andExpect(jsonPath("$.email").value("updated.mechanic@example.com"))
            .andExpect(jsonPath("$.enabled").value(true));

        AppUser saved = users.findById(mechanic.getId()).orElseThrow();
        assertThat(saved.getUsername()).isEqualTo("updated-mechanic");
        assertThat(saved.getEmail()).isEqualTo("updated.mechanic@example.com");
    }

    @Test
    void adminResetsMechanicPassword() throws Exception {
        AppUser mechanic = users.saveAndFlush(new AppUser(
            "password-mechanic", "password-mechanic@example.com", passwords.encode("old-password"), UserRole.MECHANIC));

        mockMvc.perform(put("/api/admin/staff/mechanics/{mechanicId}/password", mechanic.getId())
                .with(user("admin").roles("ADMIN"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "password": "new-password-2026",
                      "passwordConfirmation": "new-password-2026"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("password-mechanic"));

        AppUser saved = users.findById(mechanic.getId()).orElseThrow();
        assertThat(passwords.matches("new-password-2026", saved.getPasswordHash())).isTrue();
    }

    @Test
    void adminDisablesMechanicLoginAndCanEnableAccountAgain() throws Exception {
        AppUser mechanic = users.saveAndFlush(new AppUser(
            "disabled-mechanic", "disabled-mechanic@example.com", passwords.encode("mechanic-password"), UserRole.MECHANIC));

        mockMvc.perform(put("/api/admin/staff/mechanics/{mechanicId}/status", mechanic.getId())
                .with(user("admin").roles("ADMIN"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "enabled": false }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.enabled").value(false));

        mockMvc.perform(post("/api/auth/login")
                .with(csrf())
                .param("username", "disabled-mechanic")
                .param("password", "mechanic-password"))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(put("/api/admin/staff/mechanics/{mechanicId}/status", mechanic.getId())
                .with(user("admin").roles("ADMIN"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "enabled": true }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.enabled").value(true));

        mockMvc.perform(post("/api/auth/login")
                .with(csrf())
                .param("username", "disabled-mechanic")
                .param("password", "mechanic-password"))
            .andExpect(status().isNoContent());
    }

    @Test
    void adminStaffPanelDoesNotModifyNonMechanicAccounts() throws Exception {
        AppUser client = users.saveAndFlush(new AppUser(
            "staff-panel-client", "staff-panel-client@example.com", passwords.encode("client-password"), UserRole.CLIENT));

        mockMvc.perform(put("/api/admin/staff/mechanics/{mechanicId}", client.getId())
                .with(user("admin").roles("ADMIN"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username": "wrong-account",
                      "email": "wrong-account@example.com"
                    }
                    """))
            .andExpect(status().isNotFound());

        mockMvc.perform(put("/api/admin/staff/mechanics/{mechanicId}/password", client.getId())
                .with(user("admin").roles("ADMIN"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(passwordResetJson("new-password-2026", "new-password-2026")))
            .andExpect(status().isNotFound());

        mockMvc.perform(put("/api/admin/staff/mechanics/{mechanicId}/status", client.getId())
                .with(user("admin").roles("ADMIN"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(statusJson(false)))
            .andExpect(status().isNotFound());

        AppUser unchanged = users.findById(client.getId()).orElseThrow();
        assertThat(unchanged.getRole()).isEqualTo(UserRole.CLIENT);
        assertThat(unchanged.getUsername()).isEqualTo("staff-panel-client");
        assertThat(unchanged.isEnabled()).isTrue();
        assertThat(passwords.matches("client-password", unchanged.getPasswordHash())).isTrue();
    }

    @Test
    void rejectsInvalidRegistrationFields() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(registrationJson("a!", "not-an-email", "short", "different")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors.username").exists())
            .andExpect(jsonPath("$.fieldErrors.email").exists())
            .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    @Test
    void rejectsPasswordConfirmationMismatch() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(registrationJson(
                    "mismatch-client", "mismatch@example.com", "password-one", "password-two")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("REGISTRATION_VALIDATION_FAILED"))
            .andExpect(jsonPath("$.fieldErrors.passwordConfirmation").value("Passwords do not match."));
    }

    @Test
    void rejectsPasswordThatExceedsBcryptByteLimit() throws Exception {
        String password = "ą".repeat(40);

        mockMvc.perform(post("/api/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(registrationJson("long-password", "long-password@example.com", password, password)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("REGISTRATION_VALIDATION_FAILED"))
            .andExpect(jsonPath("$.fieldErrors.password").value("Password is too long after encoding."));
    }

    @Test
    void reportsCaseInsensitiveUsernameAndEmailConflicts() throws Exception {
        users.saveAndFlush(new AppUser(
            "existing-client", "existing@example.com", passwords.encode("existing-password"), UserRole.CLIENT));

        mockMvc.perform(post("/api/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(registrationJson(
                    "EXISTING-CLIENT", "free@example.com", "new-password", "new-password")))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("REGISTRATION_CONFLICT"))
            .andExpect(jsonPath("$.fieldErrors.username").value("This username is already taken."));

        mockMvc.perform(post("/api/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(registrationJson(
                    "free-client", "EXISTING@EXAMPLE.COM", "new-password", "new-password")))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("REGISTRATION_CONFLICT"))
            .andExpect(jsonPath("$.fieldErrors.email").value("An account with this email already exists."));
    }

    @Test
    void requiresCsrfToken() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registrationJson(
                    "csrf-client", "csrf@example.com", "new-password", "new-password")))
            .andExpect(status().isForbidden());

        assertThat(users.findByUsernameIgnoreCase("csrf-client")).isEmpty();
    }

    private String registrationJson(String username, String email, String password, String confirmation) {
        return """
            {
              "username": "%s",
              "email": "%s",
              "password": "%s",
              "passwordConfirmation": "%s"
            }
            """.formatted(username, email, password, confirmation);
    }

    private String staffUpdateJson(String username, String email) {
        return """
            {
              "username": "%s",
              "email": "%s"
            }
            """.formatted(username, email);
    }

    private String passwordResetJson(String password, String confirmation) {
        return """
            {
              "password": "%s",
              "passwordConfirmation": "%s"
            }
            """.formatted(password, confirmation);
    }

    private String statusJson(boolean enabled) {
        return """
            {
              "enabled": %s
            }
            """.formatted(enabled);
    }
}

