package pl.autoserwis.appointment;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import pl.autoserwis.PostgresTestConfiguration;
import pl.autoserwis.user.AppUser;
import pl.autoserwis.vehicle.Vehicle;

import java.time.LocalDate;
import java.time.Year;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static pl.autoserwis.DatabaseTestUsers.databaseUser;

@SpringBootTest(properties = "spring.docker.compose.enabled=false")
@AutoConfigureMockMvc
@Import(PostgresTestConfiguration.class)
@ActiveProfiles("test")
@Transactional
class AppointmentBookingIntegrationTest extends AppointmentIntegrationTestSupport {
    @Test
    void clientCreatesAppointmentWithOwnVehicleAndProfileSnapshot() throws Exception {
        AppUser client = createClient("booking-client", true);
        Vehicle vehicle = createVehicle(client, "Toyota", "Corolla", "KR123", null);
        LocalDate visitDate = workingDate(0);

        mockMvc.perform(post("/api/appointments")
                .with(databaseUser(client.getUsername()).roles("CLIENT"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(clientJson(vehicle.getId(), visitDate, "Silnik nierówno pracuje.")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.reference").isNotEmpty())
            .andExpect(jsonPath("$.requesterType").value("CLIENT"))
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andExpect(jsonPath("$.vehicleId").value(vehicle.getId()))
            .andExpect(jsonPath("$.vehicleRegistrationNumber").value("KR123"))
            .andExpect(jsonPath("$.firstName").value("Jan"))
            .andExpect(jsonPath("$.requestedStartAt").value(apiTime(visitDate)))
            .andExpect(jsonPath("$.currentStartAt").value(apiTime(visitDate)));

        assertThat(appointments.findAll()).singleElement().satisfies(saved -> {
            assertThat(saved.getClient().getId()).isEqualTo(client.getId());
            assertThat(saved.getVehicle().getId()).isEqualTo(vehicle.getId());
            assertThat(saved.getProblemDescription()).isEqualTo("Silnik nierówno pracuje.");
        });
    }

    @Test
    void clientRequiresConfiguredProfileAndOwnVehicle() throws Exception {
        AppUser client = createClient("client-without-profile", false);
        AppUser other = createClient("vehicle-owner", true);
        Vehicle otherVehicle = createVehicle(other, "Ford", "Focus", "PO123", null);
        LocalDate visitDate = workingDate(0);

        mockMvc.perform(post("/api/appointments")
                .with(databaseUser(client.getUsername()).roles("CLIENT"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(clientJson(otherVehicle.getId(), visitDate, "Samochód nie chce odpalić.")))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.fieldErrors.profile").exists());

        createProfile(client);
        mockMvc.perform(post("/api/appointments")
                .with(databaseUser(client.getUsername()).roles("CLIENT"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(clientJson(otherVehicle.getId(), visitDate, "Samochód nie chce odpalić.")))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
            .andExpect(jsonPath("$.message").value("Vehicle not found."));
    }

    @Test
    void guestCreatesAppointmentWithOneContactAndNormalizedVehicleData() throws Exception {
        LocalDate visitDate = workingDate(0);

        mockMvc.perform(post("/api/appointments/guest")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(guestJson(visitDate, "", "GUEST@EXAMPLE.COM", " kr 456 ",
                    "wvwzzz1jzxw000001", "Podczas hamowania słychać pisk.")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.requesterType").value("GUEST"))
            .andExpect(jsonPath("$.vehicleId").doesNotExist())
            .andExpect(jsonPath("$.vehicleRegistrationNumber").value("KR456"))
            .andExpect(jsonPath("$.vehicleVin").value("WVWZZZ1JZXW000001"))
            .andExpect(jsonPath("$.phoneNumber").value(""))
            .andExpect(jsonPath("$.contactEmail").value("guest@example.com"))
            .andExpect(jsonPath("$.requestedStartAt").value(apiTime(visitDate)));
    }

    @Test
    void validatesGuestContactVehicleYearAndDescriptionAfterTrimming() throws Exception {
        LocalDate visitDate = workingDate(0);

        mockMvc.perform(post("/api/appointments/guest")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(guestJsonWithYear(visitDate, "", "", " - ", "",
                    "a         ", Year.now().getValue() + 2)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors.phoneNumber").exists())
            .andExpect(jsonPath("$.fieldErrors.contactEmail").exists())
            .andExpect(jsonPath("$.fieldErrors.vehicleProductionYear").exists())
            .andExpect(jsonPath("$.fieldErrors.vehicleRegistrationNumber").exists())
            .andExpect(jsonPath("$.fieldErrors.problemDescription").exists());
    }
}
