package pl.autoserwis.vehicle;

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

import java.time.Year;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.docker.compose.enabled=false")
@AutoConfigureMockMvc
@Import(PostgresTestConfiguration.class)
@ActiveProfiles("test")
@Transactional
class VehicleIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired UserRepository users;
    @Autowired VehicleRepository vehicles;
    @Autowired PasswordEncoder passwords;

    @Test
    void createsVehicleForAuthenticatedClientAndNormalizesIdentifiers() throws Exception {
        AppUser client = createUser("vehicle-client", UserRole.CLIENT);

        mockMvc.perform(post("/api/vehicles")
                .with(user(client.getUsername()).roles("CLIENT"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(vehicleJson("Toyota", "Corolla", 2020, " kr 12 ab ", "wvwzzz1jzxw000001")))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern("/api/vehicles/\\d+")))
            .andExpect(jsonPath("$.make").value("Toyota"))
            .andExpect(jsonPath("$.registrationNumber").value("KR12AB"))
            .andExpect(jsonPath("$.vin").value("WVWZZZ1JZXW000001"));

        Vehicle saved = vehicles.findAll().getFirst();
        assertThat(saved.getOwner().getId()).isEqualTo(client.getId());
    }

    @Test
    void listsOnlyCurrentClientsVehiclesInStableOrder() throws Exception {
        AppUser client = createUser("list-client", UserRole.CLIENT);
        AppUser other = createUser("other-list-client", UserRole.CLIENT);
        vehicles.save(new Vehicle(client, "Toyota", "Corolla", 2020, "KR2", null));
        vehicles.save(new Vehicle(client, "Audi", "A4", 2018, "KR1", null));
        vehicles.save(new Vehicle(other, "BMW", "X3", 2022, "WX1", null));

        mockMvc.perform(get("/api/vehicles").with(user(client.getUsername()).roles("CLIENT")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].make").value("Audi"))
            .andExpect(jsonPath("$[1].make").value("Toyota"));
    }

    @Test
    void returnsOwnVehicleAndHidesAnotherClientsVehicle() throws Exception {
        AppUser owner = createUser("details-owner", UserRole.CLIENT);
        AppUser other = createUser("details-other", UserRole.CLIENT);
        Vehicle vehicle = vehicles.save(new Vehicle(owner, "Ford", "Focus", 2017, "PO123", null));

        mockMvc.perform(get("/api/vehicles/{id}", vehicle.getId())
                .with(user(owner.getUsername()).roles("CLIENT")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.registrationNumber").value("PO123"));

        mockMvc.perform(get("/api/vehicles/{id}", vehicle.getId())
                .with(user(other.getUsername()).roles("CLIENT")))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Nie znaleziono pojazdu."));
    }

    @Test
    void validatesVehicleFieldsAndCurrentProductionYear() throws Exception {
        AppUser client = createUser("validation-vehicle-client", UserRole.CLIENT);

        mockMvc.perform(post("/api/vehicles")
                .with(user(client.getUsername()).roles("CLIENT"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(vehicleJson(" ", "", 1885, "!", "invalid")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors.make").exists())
            .andExpect(jsonPath("$.fieldErrors.model").exists())
            .andExpect(jsonPath("$.fieldErrors.productionYear").exists())
            .andExpect(jsonPath("$.fieldErrors.registrationNumber").exists())
            .andExpect(jsonPath("$.fieldErrors.vin").exists());

        int futureYear = Year.now().getValue() + 2;
        mockMvc.perform(post("/api/vehicles")
                .with(user(client.getUsername()).roles("CLIENT"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(vehicleJson("Ford", "Focus", futureYear, "PO123", "")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors.productionYear").exists());
    }

    @Test
    void rejectsDuplicateRegistrationNumberAndVinForOneOwner() throws Exception {
        AppUser client = createUser("duplicate-vehicle-client", UserRole.CLIENT);
        vehicles.saveAndFlush(new Vehicle(client, "Ford", "Focus", 2018,
            "PO123", "WVWZZZ1JZXW000001"));

        mockMvc.perform(post("/api/vehicles")
                .with(user(client.getUsername()).roles("CLIENT"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(vehicleJson("Ford", "Fiesta", 2019, " po 123 ", "")))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.fieldErrors.registrationNumber").exists());

        mockMvc.perform(post("/api/vehicles")
                .with(user(client.getUsername()).roles("CLIENT"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(vehicleJson("Volkswagen", "Golf", 2019, "PO456", "wvwzzz1jzxw000001")))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.fieldErrors.vin").exists());
    }

    @Test
    void requiresAuthenticationAndCsrf() throws Exception {
        AppUser client = createUser("secured-vehicle-client", UserRole.CLIENT);

        mockMvc.perform(get("/api/vehicles"))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/vehicles")
                .with(user(client.getUsername()).roles("CLIENT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(vehicleJson("Ford", "Focus", 2020, "PO123", "")))
            .andExpect(status().isForbidden());

        assertThat(vehicles.count()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"MECHANIC", "ADMIN"})
    void staffRolesCannotUseClientVehicleEndpoints(String role) throws Exception {
        mockMvc.perform(get("/api/vehicles").with(user("staff").roles(role)))
            .andExpect(status().isForbidden());
    }

    private AppUser createUser(String username, UserRole role) {
        return users.saveAndFlush(new AppUser(username, username + "@example.com",
            passwords.encode("test-password"), role));
    }

    private String vehicleJson(String make, String model, int productionYear,
            String registrationNumber, String vin) {
        return """
            {
              "make": "%s", "model": "%s", "productionYear": %d,
              "registrationNumber": "%s", "vin": "%s"
            }
            """.formatted(make, model, productionYear, registrationNumber, vin);
    }
}
