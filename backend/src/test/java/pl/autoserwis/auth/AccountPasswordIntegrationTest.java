package pl.autoserwis.auth;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.docker.compose.enabled=false")
@AutoConfigureMockMvc
@Import(PostgresTestConfiguration.class)
@ActiveProfiles("test")
@Transactional
class AccountPasswordIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwords;

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void authenticatedUserChangesOwnPassword(UserRole role) throws Exception {
        AppUser account = createUser("password-" + role.name().toLowerCase(), role, "current-password");

        mockMvc.perform(put("/api/auth/password")
                .with(databaseUser(account.getUsername()).roles(role.name()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(passwordJson("current-password", "new-password-2026", "new-password-2026")))
            .andExpect(status().isNoContent());

        AppUser updated = users.findById(account.getId()).orElseThrow();
        assertThat(passwords.matches("new-password-2026", updated.getPasswordHash())).isTrue();
        assertThat(passwords.matches("current-password", updated.getPasswordHash())).isFalse();
    }

    @Test
    void rejectsIncorrectCurrentPasswordAndMismatchedConfirmation() throws Exception {
        AppUser account = createUser("invalid-password-client", UserRole.CLIENT, "current-password");

        mockMvc.perform(put("/api/auth/password")
                .with(databaseUser(account.getUsername()).roles("CLIENT"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(passwordJson("wrong-password", "new-password-2026", "different-password")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("ACCOUNT_PASSWORD_VALIDATION_FAILED"))
            .andExpect(jsonPath("$.fieldErrors.currentPassword").exists())
            .andExpect(jsonPath("$.fieldErrors.newPasswordConfirmation").exists());

        assertThat(passwords.matches("current-password",
            users.findById(account.getId()).orElseThrow().getPasswordHash())).isTrue();
    }

    @Test
    void rejectsPasswordLongerThanBcryptByteLimit() throws Exception {
        AppUser account = createUser("long-new-password-client", UserRole.CLIENT, "current-password");
        String longPassword = "ą".repeat(40);

        mockMvc.perform(put("/api/auth/password")
                .with(databaseUser(account.getUsername()).roles("CLIENT"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(passwordJson("current-password", longPassword, longPassword)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("ACCOUNT_PASSWORD_VALIDATION_FAILED"))
            .andExpect(jsonPath("$.fieldErrors.newPassword").exists());
    }

    @Test
    void changePasswordRequiresAuthenticationAndCsrf() throws Exception {
        AppUser account = createUser("secured-password-client", UserRole.CLIENT, "current-password");

        mockMvc.perform(put("/api/auth/password")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(passwordJson("current-password", "new-password-2026", "new-password-2026")))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(put("/api/auth/password")
                .with(databaseUser(account.getUsername()).roles("CLIENT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(passwordJson("current-password", "new-password-2026", "new-password-2026")))
            .andExpect(status().isForbidden());

        assertThat(passwords.matches("current-password",
            users.findById(account.getId()).orElseThrow().getPasswordHash())).isTrue();
    }

    private AppUser createUser(String username, UserRole role, String password) {
        return users.saveAndFlush(new AppUser(username, username + "@example.com",
            passwords.encode(password), role));
    }

    private String passwordJson(String currentPassword, String newPassword, String confirmation) {
        return """
            {
              "currentPassword": "%s",
              "newPassword": "%s",
              "newPasswordConfirmation": "%s"
            }
            """.formatted(currentPassword, newPassword, confirmation);
    }
}
