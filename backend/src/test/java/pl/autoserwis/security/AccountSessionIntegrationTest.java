package pl.autoserwis.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
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
import pl.autoserwis.vehicle.Vehicle;
import pl.autoserwis.vehicle.VehicleRepository;

import java.util.UUID;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
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
class AccountSessionIntegrationTest {
    private static final String INITIAL_PASSWORD = "initial-password-2026";
    private static final String NEW_PASSWORD = "changed-password-2026";

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired VehicleRepository vehicles;
    @Autowired PasswordEncoder passwords;

    private final List<Long> createdAccountIds = new ArrayList<>();

    @AfterEach
    void removeTestAccounts() {
        for (Long id : createdAccountIds) {
            vehicles.deleteAll(vehicles.findByOwner_IdOrderByMakeAscModelAscRegistrationNumberAsc(id));
            users.deleteById(id);
        }
    }

    @Test
    void existingSessionKeepsOriginalOwnerAfterRenameAndReuseOfOldLogin() throws Exception {
        AppUser original = account(UserRole.CLIENT);
        String oldUsername = original.getUsername();
        String newUsername = oldUsername + "-new";
        Vehicle originalVehicle = vehicles.saveAndFlush(new Vehicle(original, "Honda", "Civic", 2020, "KR1001", null));
        MockHttpSession originalSession = login(oldUsername, INITIAL_PASSWORD);
        MockHttpSession adminSession = login(account(UserRole.ADMIN));

        mvc.perform(put("/api/admin/accounts/{id}", original.getId()).session(adminSession).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"%s","email":"%s"}
                    """.formatted(newUsername, original.getEmail())))
            .andExpect(status().isOk());

        mvc.perform(post("/api/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"%s","email":"replacement-%s@example.com",
                     "password":"%s","passwordConfirmation":"%s"}
                    """.formatted(oldUsername, oldUsername, INITIAL_PASSWORD, INITIAL_PASSWORD)))
            .andExpect(status().isCreated());
        AppUser replacement = users.findByUsernameIgnoreCase(oldUsername).orElseThrow();
        createdAccountIds.add(replacement.getId());
        Vehicle otherVehicle = vehicles.saveAndFlush(new Vehicle(replacement, "Toyota", "Yaris", 2021, "KR2002", null));

        mvc.perform(get("/api/vehicles").session(originalSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(originalVehicle.getId()));
        mvc.perform(get("/api/vehicles/{id}", otherVehicle.getId()).session(originalSession))
            .andExpect(status().isNotFound());
        mvc.perform(get("/api/profile/me").session(originalSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.contactEmail").value(original.getEmail()));
        mvc.perform(get("/api/auth/me").session(originalSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.user.username").value(newUsername));

        changeOwnPassword(originalSession, INITIAL_PASSWORD, NEW_PASSWORD);
        assertThat(passwords.matches(NEW_PASSWORD, users.findById(original.getId()).orElseThrow().getPasswordHash())).isTrue();
        assertThat(passwords.matches(INITIAL_PASSWORD, users.findById(replacement.getId()).orElseThrow().getPasswordHash())).isTrue();
    }

    @Test
    void disablingMechanicRevokesExistingSessionsAndReactivationDoesNotRestoreThem() throws Exception {
        AppUser mechanic = account(UserRole.MECHANIC);
        MockHttpSession firstSession = login(mechanic);
        MockHttpSession secondSession = login(mechanic);
        MockHttpSession adminSession = login(account(UserRole.ADMIN));

        setEnabled(adminSession, mechanic.getId(), false);
        mvc.perform(get("/api/staff/appointments").session(firstSession))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
        assertThat(firstSession.isInvalid()).isTrue();
        rejectedLogin(mechanic.getUsername(), INITIAL_PASSWORD);

        setEnabled(adminSession, mechanic.getId(), true);
        // This session was never used while disabled; its stored version must still be rejected.
        mvc.perform(post("/api/service-categories").session(secondSession).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Revoked session category\"}"))
            .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/staff/appointments").session(login(mechanic)))
            .andExpect(status().isOk());
    }

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = {"CLIENT", "MECHANIC"})
    void administratorPasswordResetRevokesAllExistingSessions(UserRole role) throws Exception {
        AppUser account = account(role);
        MockHttpSession firstSession = login(account);
        MockHttpSession secondSession = login(account);
        MockHttpSession adminSession = login(account(UserRole.ADMIN));

        mvc.perform(put("/api/admin/accounts/{id}/password", account.getId()).session(adminSession).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"password":"%s","passwordConfirmation":"%s"}
                    """.formatted(NEW_PASSWORD, NEW_PASSWORD)))
            .andExpect(status().isOk());

        mvc.perform(get("/api/auth/me").session(firstSession)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me").session(secondSession)).andExpect(status().isUnauthorized());
        rejectedLogin(account.getUsername(), INITIAL_PASSWORD);
        mvc.perform(get("/api/auth/me").session(login(account.getUsername(), NEW_PASSWORD)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.user.username").value(account.getUsername()));
    }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void ownPasswordChangeKeepsCurrentSessionAndRevokesOtherSessions(UserRole role) throws Exception {
        AppUser account = account(role);
        MockHttpSession currentSession = login(account);
        MockHttpSession otherSession = login(account);

        changeOwnPassword(currentSession, INITIAL_PASSWORD, NEW_PASSWORD);

        mvc.perform(get("/api/auth/me").session(currentSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.user.username").value(account.getUsername()));
        mvc.perform(get("/api/auth/me").session(otherSession)).andExpect(status().isUnauthorized());
        rejectedLogin(account.getUsername(), INITIAL_PASSWORD);
        login(account.getUsername(), NEW_PASSWORD);
    }

    @Test
    void failedPasswordChangeDoesNotRevokeSessions() throws Exception {
        AppUser account = account(UserRole.CLIENT);
        MockHttpSession currentSession = login(account);
        MockHttpSession otherSession = login(account);
        mvc.perform(put("/api/auth/password").session(currentSession).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"currentPassword":"wrong-password","newPassword":"%s","newPasswordConfirmation":"%s"}
                    """.formatted(NEW_PASSWORD, NEW_PASSWORD)))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/auth/me").session(currentSession)).andExpect(status().isOk());
        mvc.perform(get("/api/auth/me").session(otherSession)).andExpect(status().isOk());
    }

    @Test
    void rejectsLegacyPrincipalWithoutStableAccountIdentity() throws Exception {
        mvc.perform(get("/api/vehicles").with(user("old-session").roles("CLIENT")))
            .andExpect(status().isUnauthorized());
    }

    private AppUser account(UserRole role) {
        String username = "session-" + UUID.randomUUID().toString().substring(0, 12);
        AppUser account = users.saveAndFlush(new AppUser(username, username + "@example.com",
            passwords.encode(INITIAL_PASSWORD), role));
        createdAccountIds.add(account.getId());
        return account;
    }

    private MockHttpSession login(AppUser account) throws Exception {
        return login(account.getUsername(), INITIAL_PASSWORD);
    }

    private MockHttpSession login(String username, String password) throws Exception {
        MockHttpSession session = (MockHttpSession) mvc.perform(post("/api/auth/login").with(csrf())
                .param("username", username).param("password", password))
            .andExpect(status().isNoContent()).andReturn().getRequest().getSession(false);
        assertThat(session).isNotNull();
        return session;
    }

    private void rejectedLogin(String username, String password) throws Exception {
        mvc.perform(post("/api/auth/login").with(csrf())
                .param("username", username).param("password", password))
            .andExpect(status().isUnauthorized());
    }

    private void setEnabled(MockHttpSession admin, Long id, boolean enabled) throws Exception {
        mvc.perform(put("/api/admin/accounts/{id}/status", id).session(admin).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":" + enabled + "}"))
            .andExpect(status().isOk());
    }

    private void changeOwnPassword(MockHttpSession session, String currentPassword, String newPassword) throws Exception {
        mvc.perform(put("/api/auth/password").session(session).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"currentPassword":"%s","newPassword":"%s","newPasswordConfirmation":"%s"}
                    """.formatted(currentPassword, newPassword, newPassword)))
            .andExpect(status().isNoContent());
    }
}
