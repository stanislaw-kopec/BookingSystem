package pl.autoserwis.user;

import org.junit.jupiter.api.Test;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static pl.autoserwis.DatabaseTestUsers.databaseUser;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.docker.compose.enabled=false")
@AutoConfigureMockMvc
@Import(PostgresTestConfiguration.class)
@ActiveProfiles("test")
@Transactional
class AdminAccountIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwords;

    @Test
    void listsAllAccountRolesWithBackendFiltersAndPagination() throws Exception {
        createUser("portfolio-filter-anna", UserRole.CLIENT, true);
        createUser("portfolio-filter-beta", UserRole.CLIENT, false);
        createUser("portfolio-filter-car", UserRole.MECHANIC, true);
        createUser("portfolio-filter-admin", UserRole.ADMIN, true);

        mockMvc.perform(get("/api/admin/accounts")
                .with(databaseUser("admin").roles("ADMIN"))
                .param("query", "portfolio-filter")
                .param("page", "0")
                .param("size", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(2))
            .andExpect(jsonPath("$.totalElements").value(4))
            .andExpect(jsonPath("$.totalPages").value(2))
            .andExpect(jsonPath("$.content[0].username").value("portfolio-filter-admin"));

        mockMvc.perform(get("/api/admin/accounts")
                .with(databaseUser("admin").roles("ADMIN"))
                .param("role", "CLIENT")
                .param("enabled", "false")
                .param("query", "portfolio-filter-beta"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].username").value("portfolio-filter-beta"))
            .andExpect(jsonPath("$.content[0].role").value("CLIENT"))
            .andExpect(jsonPath("$.content[0].enabled").value(false));

        mockMvc.perform(get("/api/admin/accounts")
                .with(databaseUser("admin").roles("ADMIN"))
                .param("role", "ADMIN")
                .param("query", "portfolio-filter-admin"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].role").value("ADMIN"));
    }

    @Test
    void adminCreatesAnotherAdministrator() throws Exception {
        mockMvc.perform(post("/api/admin/accounts/administrators")
                .with(databaseUser("admin").roles("ADMIN"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username": "new-portfolio-admin",
                      "email": "NEW-ADMIN@EXAMPLE.COM",
                      "password": "secure-admin-password",
                      "passwordConfirmation": "secure-admin-password"
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.username").value("new-portfolio-admin"))
            .andExpect(jsonPath("$.email").value("new-admin@example.com"))
            .andExpect(jsonPath("$.role").value("ADMIN"))
            .andExpect(jsonPath("$.enabled").value(true));

        AppUser administrator = users.findByUsernameIgnoreCase("new-portfolio-admin").orElseThrow();
        assertThat(administrator.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(passwords.matches("secure-admin-password", administrator.getPasswordHash())).isTrue();
    }

    @Test
    void adminUpdatesDisablesAndResetsClientPassword() throws Exception {
        AppUser client = createUser("managed-client", UserRole.CLIENT, true);

        mockMvc.perform(put("/api/admin/accounts/{accountId}", client.getId())
                .with(databaseUser("admin").roles("ADMIN"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(accountJson("updated-client", "UPDATED@EXAMPLE.COM")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("updated-client"))
            .andExpect(jsonPath("$.email").value("updated@example.com"));

        mockMvc.perform(put("/api/admin/accounts/{accountId}/password", client.getId())
                .with(databaseUser("admin").roles("ADMIN"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(passwordJson("temporary-password", "temporary-password")))
            .andExpect(status().isOk());

        mockMvc.perform(put("/api/admin/accounts/{accountId}/status", client.getId())
                .with(databaseUser("admin").roles("ADMIN"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"enabled\":false}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.enabled").value(false));

        AppUser updated = users.findById(client.getId()).orElseThrow();
        assertThat(updated.getUsername()).isEqualTo("updated-client");
        assertThat(updated.getEmail()).isEqualTo("updated@example.com");
        assertThat(updated.isEnabled()).isFalse();
        assertThat(passwords.matches("temporary-password", updated.getPasswordHash())).isTrue();
    }

    @Test
    void accountPanelListsButDoesNotModifyAdministrators() throws Exception {
        AppUser anotherAdmin = createUser("protected-admin", UserRole.ADMIN, true);

        mockMvc.perform(get("/api/admin/accounts")
                .with(databaseUser("admin").roles("ADMIN"))
                .param("role", "ADMIN")
                .param("query", "protected-admin"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].username").value("protected-admin"));

        mockMvc.perform(put("/api/admin/accounts/{accountId}/password", anotherAdmin.getId())
                .with(databaseUser("admin").roles("ADMIN"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(passwordJson("changed-password", "changed-password")))
            .andExpect(status().isNotFound());

        mockMvc.perform(put("/api/admin/accounts/{accountId}", anotherAdmin.getId())
                .with(databaseUser("admin").roles("ADMIN"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(accountJson("changed-admin", "changed-admin@example.com")))
            .andExpect(status().isNotFound());

        mockMvc.perform(put("/api/admin/accounts/{accountId}/status", anotherAdmin.getId())
                .with(databaseUser("admin").roles("ADMIN"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"enabled\":false}"))
            .andExpect(status().isNotFound());

        assertThat(passwords.matches("test-password", anotherAdmin.getPasswordHash())).isTrue();
        assertThat(anotherAdmin.getUsername()).isEqualTo("protected-admin");
        assertThat(anotherAdmin.isEnabled()).isTrue();
    }

    @Test
    void accountManagementRequiresAdminRoleAndCsrf() throws Exception {
        AppUser client = createUser("secured-managed-client", UserRole.CLIENT, true);
        String administratorJson = """
            {
              "username": "unauthorized-admin",
              "email": "unauthorized-admin@example.com",
              "password": "secure-admin-password",
              "passwordConfirmation": "secure-admin-password"
            }
            """;

        mockMvc.perform(get("/api/admin/accounts"))
            .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/accounts").with(databaseUser("client").roles("CLIENT")))
            .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/accounts").with(databaseUser("mechanic").roles("MECHANIC")))
            .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/admin/accounts/administrators")
                .with(databaseUser("mechanic").roles("MECHANIC"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(administratorJson))
            .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/admin/accounts/administrators")
                .with(databaseUser("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(administratorJson))
            .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/admin/accounts/{accountId}/password", client.getId())
                .with(databaseUser("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(passwordJson("changed-password", "changed-password")))
            .andExpect(status().isForbidden());

        assertThat(passwords.matches("test-password", client.getPasswordHash())).isTrue();
        assertThat(users.findByUsernameIgnoreCase("unauthorized-admin")).isEmpty();
    }

    private AppUser createUser(String username, UserRole role, boolean enabled) {
        AppUser user = new AppUser(username, username.replace("-client", "") + "@example.com",
            passwords.encode("test-password"), role);
        user.setEnabled(enabled);
        return users.saveAndFlush(user);
    }

    private String accountJson(String username, String email) {
        return """
            { "username": "%s", "email": "%s" }
            """.formatted(username, email);
    }

    private String passwordJson(String password, String confirmation) {
        return """
            { "password": "%s", "passwordConfirmation": "%s" }
            """.formatted(password, confirmation);
    }
}
