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
import pl.autoserwis.user.UserRole;
import pl.autoserwis.vehicle.Vehicle;

import java.time.LocalDate;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static pl.autoserwis.DatabaseTestUsers.databaseUser;

@SpringBootTest(properties = "spring.docker.compose.enabled=false")
@AutoConfigureMockMvc
@Import(PostgresTestConfiguration.class)
@ActiveProfiles("test")
@Transactional
class AppointmentAuthorizationIntegrationTest extends AppointmentIntegrationTestSupport {
    @Test
    void onlyAdminCanManageScheduleConfiguration() throws Exception {
        mockMvc.perform(get("/api/admin/schedule")
                .with(databaseUser("mechanic").roles("MECHANIC")))
            .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/admin/schedule/settings")
                .with(databaseUser("client").roles("CLIENT"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"defaultDailyCapacity":4,"bookingHorizonDays":30,"workdayStart":"08:00","workdayEnd":"16:00"}
                    """))
            .andExpect(status().isForbidden());
    }

    @Test
    void changingRequestsRequiresCsrfAndCorrectRole() throws Exception {
        AppUser client = createClient("secured-appointment-client", true);
        AppUser mechanic = createUser("secured-appointment-mechanic", UserRole.MECHANIC);
        Vehicle vehicle = createVehicle(client, "Volvo", "V60", "BI123", null);
        LocalDate visitDate = workingDate(3);

        mockMvc.perform(post("/api/appointments/guest")
                .contentType(MediaType.APPLICATION_JSON)
                .content(guestJson(visitDate, "+48 500 600 700", "", "BI456", "",
                    "Problem z układem hamulcowym.")))
            .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/appointments")
                .with(databaseUser(client.getUsername()).roles("CLIENT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(clientJson(vehicle.getId(), visitDate, "Problem z układem hamulcowym.")))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/appointments"))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/appointments")
                .with(databaseUser(mechanic.getUsername()).roles("MECHANIC")))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/staff/appointments")
                .with(databaseUser(client.getUsername()).roles("CLIENT")))
            .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/staff/appointments/{id}/mark-picked-up", 999L)
                .with(databaseUser(client.getUsername()).roles("CLIENT"))
                .with(csrf()))
            .andExpect(status().isForbidden());
    }
}
