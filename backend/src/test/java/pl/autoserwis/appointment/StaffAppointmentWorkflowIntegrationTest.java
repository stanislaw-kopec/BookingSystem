package pl.autoserwis.appointment;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import pl.autoserwis.PostgresTestConfiguration;
import pl.autoserwis.appointment.domain.AppointmentRequest;
import pl.autoserwis.user.AppUser;
import pl.autoserwis.user.UserRole;
import pl.autoserwis.vehicle.Vehicle;

import java.time.LocalDate;

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
class StaffAppointmentWorkflowIntegrationTest extends AppointmentIntegrationTestSupport {
    @Test
    void staffProposesNewDayAndOnlyOwningClientCanConfirmIt() throws Exception {
        AppUser owner = createClient("proposal-owner", true);
        AppUser other = createClient("proposal-other", true);
        AppUser staff = createUser("proposal-mechanic", UserRole.MECHANIC);
        Vehicle vehicle = createVehicle(owner, "Mazda", "3", "LU123", null);
        LocalDate requested = workingDate(1);
        LocalDate proposed = workingDate(2);
        createClientAppointment(owner, vehicle, requested);
        AppointmentRequest appointment = appointments.findAll().getFirst();

        mockMvc.perform(post("/api/staff/appointments/{id}/propose-time", appointment.getId())
                .with(databaseUser(staff.getUsername()).roles("MECHANIC"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"visitDate":"%s","message":"Potrzebujemy innego dnia."}
                    """.formatted(proposed)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("TIME_PROPOSED"))
            .andExpect(jsonPath("$.requestedStartAt").value(apiTime(requested)))
            .andExpect(jsonPath("$.currentStartAt").value(apiTime(proposed)))
            .andExpect(jsonPath("$.staffActionBy").value(staff.getUsername()));

        mockMvc.perform(post("/api/appointments/{id}/confirm-proposed", appointment.getId())
                .with(databaseUser(other.getUsername()).roles("CLIENT"))
                .with(csrf()))
            .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/appointments/{id}/confirm-proposed", appointment.getId())
                .with(databaseUser(owner.getUsername()).roles("CLIENT"))
                .with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CONFIRMED"))
            .andExpect(jsonPath("$.clientConfirmedAt").exists());
    }

    @Test
    void onlyStaffCanConfirmGuestProposalAfterContact() throws Exception {
        AppUser client = createClient("guest-proposal-client", true);
        AppUser staff = createUser("guest-proposal-admin", UserRole.ADMIN);
        LocalDate requested = workingDate(2);
        LocalDate proposed = workingDate(3);
        createGuestAppointment(requested, "Problem z instalacją elektryczną.");
        AppointmentRequest appointment = appointments.findAll().getFirst();

        propose(staff, appointment, proposed);

        mockMvc.perform(post("/api/appointments/{id}/confirm-proposed", appointment.getId())
                .with(databaseUser(client.getUsername()).roles("CLIENT"))
                .with(csrf()))
            .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/staff/appointments/{id}/confirm-proposed", appointment.getId())
                .with(databaseUser(staff.getUsername()).roles("ADMIN"))
                .with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CONFIRMED"))
            .andExpect(jsonPath("$.clientConfirmedAt").doesNotExist())
            .andExpect(jsonPath("$.staffActionBy").value(staff.getUsername()));
    }

    @Test
    void staffAcceptsPendingRequestOnlyOnceAndClientListsOnlyOwnRequests() throws Exception {
        AppUser client = createClient("accepted-client", true);
        AppUser other = createClient("accepted-other", true);
        AppUser staff = createUser("accepting-mechanic", UserRole.MECHANIC);
        Vehicle vehicle = createVehicle(client, "Skoda", "Octavia", "GD123", null);
        createClientAppointment(client, vehicle, workingDate(2));
        AppointmentRequest appointment = appointments.findAll().getFirst();

        mockMvc.perform(post("/api/staff/appointments/{id}/accept", appointment.getId())
                .with(databaseUser(staff.getUsername()).roles("MECHANIC"))
                .with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CONFIRMED"))
            .andExpect(jsonPath("$.staffActionAt").exists());

        mockMvc.perform(post("/api/staff/appointments/{id}/accept", appointment.getId())
                .with(databaseUser(staff.getUsername()).roles("MECHANIC"))
                .with(csrf()))
            .andExpect(status().isConflict());

        mockMvc.perform(get("/api/appointments")
                .with(databaseUser(client.getUsername()).roles("CLIENT")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.totalPages").value(1));

        mockMvc.perform(get("/api/appointments")
                .with(databaseUser(other.getUsername()).roles("CLIENT")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(0))
            .andExpect(jsonPath("$.totalElements").value(0));
    }

    @ParameterizedTest
    @ValueSource(strings = {"MECHANIC", "ADMIN"})
    void staffRolesCanListAllRequests(String role) throws Exception {
        AppUser staff = createUser("list-" + role.toLowerCase(), UserRole.valueOf(role));

        mockMvc.perform(get("/api/staff/appointments")
                .with(databaseUser(staff.getUsername()).roles(role)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void staffAppointmentsSupportBackendPaginationFilteringAndDateSorting() throws Exception {
        AppUser staff = createUser("staff-paged-mechanic", UserRole.MECHANIC);
        createGuestAppointment(workingDate(1), "Pierwsze zgłoszenie do kolejki personelu.");
        createGuestAppointment(workingDate(2), "Drugie zgłoszenie do kolejki personelu.");
        createGuestAppointment(workingDate(3), "Trzecie zgłoszenie do kolejki personelu.");
        AppointmentRequest confirmed = appointments.findAll().get(1);

        mockMvc.perform(post("/api/staff/appointments/{id}/accept", confirmed.getId())
                .with(databaseUser(staff.getUsername()).roles("MECHANIC"))
                .with(csrf()))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/staff/appointments")
                .param("page", "0")
                .param("size", "2")
                .param("sortDirection", "ASC")
                .with(databaseUser(staff.getUsername()).roles("MECHANIC")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(2))
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(2))
            .andExpect(jsonPath("$.totalElements").value(3))
            .andExpect(jsonPath("$.totalPages").value(2))
            .andExpect(jsonPath("$.content[0].currentStartAt").value(apiTime(workingDate(1))))
            .andExpect(jsonPath("$.content[1].currentStartAt").value(apiTime(workingDate(2))));

        mockMvc.perform(get("/api/staff/appointments")
                .param("status", "CONFIRMED")
                .param("page", "0")
                .param("size", "5")
                .with(databaseUser(staff.getUsername()).roles("MECHANIC")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].status").value("CONFIRMED"))
            .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void staffCanOpenAppointmentDetailsWithVehicleRepairHistory() throws Exception {
        AppUser client = createClient("staff-details-client", true);
        AppUser staff = createUser("staff-details-mechanic", UserRole.MECHANIC);
        Vehicle vehicle = createVehicle(client, "Honda", "Accord", "STAFF1", null);
        createClientAppointment(client, vehicle, workingDate(1), "Pierwsza naprawa do historii pojazdu.");
        AppointmentRequest completed = appointments.findAll().getFirst();

        mockMvc.perform(post("/api/staff/appointments/{id}/accept", completed.getId())
                .with(databaseUser(staff.getUsername()).roles("MECHANIC"))
                .with(csrf()))
            .andExpect(status().isOk());
        mockMvc.perform(post("/api/staff/appointments/{id}/complete-repair", completed.getId())
                .with(databaseUser(staff.getUsername()).roles("MECHANIC"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(repairJson("Wymieniono olej i komplet filtrów.", "520.00")))
            .andExpect(status().isOk());
        mockMvc.perform(post("/api/staff/appointments/{id}/mark-picked-up", completed.getId())
                .with(databaseUser(staff.getUsername()).roles("MECHANIC"))
                .with(csrf()))
            .andExpect(status().isOk());

        createClientAppointment(client, vehicle, workingDate(3), "Kolejne zgłoszenie do szczegółów mechanika.");
        AppointmentRequest current = appointments.findByClient_IdOrderByCreatedAtDesc(client.getId()).getFirst();

        mockMvc.perform(get("/api/staff/appointments/{id}", current.getId())
                .with(databaseUser(staff.getUsername()).roles("MECHANIC")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(current.getId()))
            .andExpect(jsonPath("$.vehicleRegistrationNumber").value("STAFF1"))
            .andExpect(jsonPath("$.problemDescription").value("Kolejne zgłoszenie do szczegółów mechanika."));

        mockMvc.perform(get("/api/staff/appointments/{id}/repair-history", current.getId())
                .with(databaseUser(staff.getUsername()).roles("MECHANIC")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].appointmentId").value(completed.getId()))
            .andExpect(jsonPath("$[0].repairDescription").value("Wymieniono olej i komplet filtrów."))
            .andExpect(jsonPath("$[0].repairItems.length()").value(2));
    }
}
