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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
            .andExpect(jsonPath("$.fieldErrors.passwordConfirmation").value("Hasła nie są takie same."));
    }

    @Test
    void rejectsPasswordThatExceedsBcryptByteLimit() throws Exception {
        String password = "ą".repeat(40);

        mockMvc.perform(post("/api/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(registrationJson("long-password", "long-password@example.com", password, password)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors.password").value("Hasło jest zbyt długie po zakodowaniu."));
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
            .andExpect(jsonPath("$.fieldErrors.username").value("Ten login jest już zajęty."));

        mockMvc.perform(post("/api/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(registrationJson(
                    "free-client", "EXISTING@EXAMPLE.COM", "new-password", "new-password")))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.fieldErrors.email").value("Konto z tym adresem e-mail już istnieje."));
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
}
