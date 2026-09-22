package pl.autoserwis.appointment;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import pl.autoserwis.PostgresTestConfiguration;
import pl.autoserwis.appointment.domain.AppointmentRequest;
import pl.autoserwis.appointment.schedule.AppointmentSchedule;
import pl.autoserwis.user.AppUser;
import pl.autoserwis.user.UserRole;
import pl.autoserwis.vehicle.Vehicle;

import java.time.LocalDate;
import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static pl.autoserwis.DatabaseTestUsers.databaseUser;

@SpringBootTest(properties = "spring.docker.compose.enabled=false")
@AutoConfigureMockMvc
@Import(PostgresTestConfiguration.class)
@ActiveProfiles("test")
@Transactional
class ClientAppointmentIntegrationTest extends AppointmentIntegrationTestSupport {
    @Test
    void clientAppointmentsSupportBackendPaginationFilteringAndDateSorting() throws Exception {
        AppUser client = createClient("paged-client", true);
        AppUser staff = createUser("paged-mechanic", UserRole.MECHANIC);
        Vehicle vehicle = createVehicle(client, "Toyota", "Avensis", "PG123", null);
        createClientAppointment(client, vehicle, workingDate(1));
        createClientAppointment(client, vehicle, workingDate(3));
        createClientAppointment(client, vehicle, workingDate(5));
        List<AppointmentRequest> clientAppointments = appointments.findByClient_IdOrderByCreatedAtDesc(client.getId());
        AppointmentRequest confirmed = clientAppointments.get(1);

        mockMvc.perform(post("/api/staff/appointments/{id}/accept", confirmed.getId())
                .with(databaseUser(staff.getUsername()).roles("MECHANIC"))
                .with(csrf()))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/appointments")
                .param("page", "0")
                .param("size", "2")
                .param("sortDirection", "ASC")
                .with(databaseUser(client.getUsername()).roles("CLIENT")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(2))
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(2))
            .andExpect(jsonPath("$.totalElements").value(3))
            .andExpect(jsonPath("$.totalPages").value(2))
            .andExpect(jsonPath("$.content[0].currentStartAt").value(apiTime(workingDate(1))))
            .andExpect(jsonPath("$.content[1].currentStartAt").value(apiTime(workingDate(3))));

        mockMvc.perform(get("/api/appointments")
                .param("status", "CONFIRMED")
                .param("page", "0")
                .param("size", "5")
                .param("sortDirection", "DESC")
                .with(databaseUser(client.getUsername()).roles("CLIENT")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].status").value("CONFIRMED"))
            .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void owningClientCanCancelActiveAppointmentAndReleaseDailyPlace() throws Exception {
        AppUser client = createClient("cancelling-client", true);
        AppUser other = createClient("cancelling-other", true);
        Vehicle vehicle = createVehicle(client, "Opel", "Astra", "DW123", null);
        LocalDate visitDate = workingDate(3);

        for (int index = 1; index <= AppointmentSchedule.DAILY_CAPACITY; index++) {
            createClientAppointment(client, vehicle, visitDate, "Aktywne zgłoszenie numer " + index + ".");
        }
        AppointmentRequest appointment = appointments.findAll().getFirst();

        mockMvc.perform(post("/api/appointments/guest")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(guestJson(visitDate, "+48 500 600 700", "", "DW456", "",
                    "Kolejne zgłoszenie ponad limit.")))
            .andExpect(status().isConflict());

        mockMvc.perform(post("/api/appointments/{id}/cancel", appointment.getId())
                .with(databaseUser(other.getUsername()).roles("CLIENT"))
                .with(csrf()))
            .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/appointments/{id}/cancel", appointment.getId())
                .with(databaseUser(client.getUsername()).roles("CLIENT"))
                .with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CANCELLED"));

        mockMvc.perform(post("/api/appointments")
                .with(databaseUser(client.getUsername()).roles("CLIENT"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(clientJson(vehicle.getId(), visitDate, "Kontrolka silnika świeci się stale.")))
            .andExpect(status().isCreated());
    }
}
