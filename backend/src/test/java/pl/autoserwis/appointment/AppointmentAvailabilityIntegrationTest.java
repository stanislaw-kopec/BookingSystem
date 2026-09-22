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

import java.time.DayOfWeek;
import java.time.LocalDate;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static pl.autoserwis.DatabaseTestUsers.databaseUser;

@SpringBootTest(properties = "spring.docker.compose.enabled=false")
@AutoConfigureMockMvc
@Import(PostgresTestConfiguration.class)
@ActiveProfiles("test")
@Transactional
class AppointmentAvailabilityIntegrationTest extends AppointmentIntegrationTestSupport {
    @Test
    void returnsPublicAvailabilityForConfiguredWorkingDays() throws Exception {
        mockMvc.perform(get("/api/appointments/availability"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.timeZone").value("Europe/Warsaw"))
            .andExpect(jsonPath("$.dailyCapacity").value(4))
            .andExpect(jsonPath("$.days").isArray())
            .andExpect(jsonPath("$.days[0].date").exists())
            .andExpect(jsonPath("$.days[0].startAt").exists())
            .andExpect(jsonPath("$.days[0].endAt").exists())
            .andExpect(jsonPath("$.days[0].capacity").value(4))
            .andExpect(jsonPath("$.days[0].remainingCapacity").value(4))
            .andExpect(jsonPath("$.days[0].available").value(true));
    }

    @Test
    void adminConfiguresDailyCapacityAndClosedDays() throws Exception {
        LocalDate visitDate = workingDate(1);

        mockMvc.perform(put("/api/admin/schedule/settings")
                .with(databaseUser("admin").roles("ADMIN"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "defaultDailyCapacity": 3,
                      "bookingHorizonDays": 45,
                      "workdayStart": "07:30",
                      "workdayEnd": "15:30"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.settings.defaultDailyCapacity").value(3))
            .andExpect(jsonPath("$.settings.bookingHorizonDays").value(45))
            .andExpect(jsonPath("$.settings.workdayStart").value("07:30:00"))
            .andExpect(jsonPath("$.settings.workdayEnd").value("15:30:00"));

        mockMvc.perform(put("/api/admin/schedule/overrides/{date}", visitDate)
                .with(databaseUser("admin").roles("ADMIN"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"date":"%s","capacity":0,"closed":true,"note":"Szkolenie zespołu"}
                    """.formatted(visitDate)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.overrides[0].date").value(visitDate.toString()))
            .andExpect(jsonPath("$.overrides[0].closed").value(true));

        mockMvc.perform(post("/api/appointments/guest")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(guestJson(visitDate, "+48 500 600 700", "", "KR901", "",
                    "Próba rezerwacji w zamkniętym dniu.")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors.visitDate").exists());
    }

    @Test
    void configuredDayCapacityLimitsNewAppointments() throws Exception {
        LocalDate visitDate = workingDate(2);
        mockMvc.perform(put("/api/admin/schedule/overrides/{date}", visitDate)
                .with(databaseUser("admin").roles("ADMIN"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"date":"%s","capacity":1,"closed":false,"note":"Mniejszy skład"}
                    """.formatted(visitDate)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.overrides[0].capacity").value(1));

        createGuestAppointment(visitDate, "Pierwsze zgłoszenie w dniu z mniejszą pojemnością.");

        mockMvc.perform(post("/api/appointments/guest")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(guestJson(visitDate, "+48 500 600 700", "", "KR902", "",
                    "Drugie zgłoszenie ponad limit dnia.")))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.fieldErrors.visitDate").exists());
    }

    @Test
    void rejectsDaysOutsideSchedule() throws Exception {
        AppUser client = createClient("day-validation-client", true);
        Vehicle vehicle = createVehicle(client, "Honda", "Civic", "WA123", null);
        LocalDate weekend = next(DayOfWeek.SATURDAY);

        mockMvc.perform(post("/api/appointments")
                .with(databaseUser(client.getUsername()).roles("CLIENT"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(clientJson(vehicle.getId(), weekend, "Problem z układem kierowniczym.")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors.visitDate").exists());

        LocalDate today = LocalDate.now(AppointmentSchedule.TIME_ZONE);
        mockMvc.perform(post("/api/appointments")
                .with(databaseUser(client.getUsername()).roles("CLIENT"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(clientJson(vehicle.getId(), today, "Problem z układem kierowniczym.")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors.visitDate").exists());
    }

    @Test
    void dailyCapacityAllowsFourActiveRequestsAndRejectionReleasesPlace() throws Exception {
        AppUser staff = createUser("capacity-mechanic", UserRole.MECHANIC);
        LocalDate visitDate = workingDate(1);

        for (int index = 1; index <= AppointmentSchedule.DAILY_CAPACITY; index++) {
            createGuestAppointment(visitDate, "Zgłoszenie usterki numer " + index + ".");
        }
        AppointmentRequest first = appointments.findAll().getFirst();

        mockMvc.perform(post("/api/appointments/guest")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(guestJson(visitDate, "+48 500 600 700", "", "KR789", "",
                    "Dodatkowe zgłoszenie usterki.")))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.fieldErrors.visitDate").exists());

        mockMvc.perform(post("/api/staff/appointments/{id}/reject", first.getId())
                .with(databaseUser(staff.getUsername()).roles("MECHANIC"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"Brak odpowiedniej części.\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("REJECTED"));

        mockMvc.perform(post("/api/appointments/guest")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(guestJson(visitDate, "+48 500 600 700", "", "KR789", "",
                    "Dodatkowe zgłoszenie usterki.")))
            .andExpect(status().isCreated());
    }
}
