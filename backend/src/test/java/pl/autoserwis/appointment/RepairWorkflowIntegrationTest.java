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
import pl.autoserwis.user.AppUser;
import pl.autoserwis.user.UserRole;
import pl.autoserwis.vehicle.Vehicle;


import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static pl.autoserwis.DatabaseTestUsers.databaseUser;

@SpringBootTest(properties = "spring.docker.compose.enabled=false")
@AutoConfigureMockMvc
@Import(PostgresTestConfiguration.class)
@ActiveProfiles("test")
@Transactional
class RepairWorkflowIntegrationTest extends AppointmentIntegrationTestSupport {
    @Test
    void staffCompletesConfirmedRepairAndMarksAppointmentReadyForPickup() throws Exception {
        AppUser client = createClient("completed-repair-client", true);
        AppUser staff = createUser("completed-repair-mechanic", UserRole.MECHANIC);
        Vehicle vehicle = createVehicle(client, "Renault", "Megane", "WX123", null);
        createClientAppointment(client, vehicle, workingDate(2));
        AppointmentRequest appointment = appointments.findAll().getFirst();

        mockMvc.perform(post("/api/staff/appointments/{id}/complete-repair", appointment.getId())
                .with(databaseUser(staff.getUsername()).roles("MECHANIC"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(repairJson("Wymieniono tarcze i klocki hamulcowe.", "850.00")))
            .andExpect(status().isConflict());

        mockMvc.perform(post("/api/staff/appointments/{id}/accept", appointment.getId())
                .with(databaseUser(staff.getUsername()).roles("MECHANIC"))
                .with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CONFIRMED"));

        mockMvc.perform(post("/api/staff/appointments/{id}/complete-repair", appointment.getId())
                .with(databaseUser(staff.getUsername()).roles("MECHANIC"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(repairJson("Wymieniono tarcze i klocki hamulcowe.", "850.00")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("READY_FOR_PICKUP"))
            .andExpect(jsonPath("$.repairDescription").value("Wymieniono tarcze i klocki hamulcowe."))
            .andExpect(jsonPath("$.totalGrossAmount").value(850.00))
            .andExpect(jsonPath("$.repairItems.length()").value(2))
            .andExpect(jsonPath("$.repairItems[0].type").value("LABOR"))
            .andExpect(jsonPath("$.repairItems[1].type").value("PART"))
            .andExpect(jsonPath("$.repairCompletedAt").exists())
            .andExpect(jsonPath("$.repairCompletedBy").value(staff.getUsername()));

        mockMvc.perform(get("/api/vehicles/{id}/repair-history", vehicle.getId())
                .with(databaseUser(client.getUsername()).roles("CLIENT")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(post("/api/staff/appointments/{id}/mark-picked-up", appointment.getId())
                .with(databaseUser(staff.getUsername()).roles("MECHANIC"))
                .with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("COMPLETED"))
            .andExpect(jsonPath("$.vehiclePickedUpAt").exists())
            .andExpect(jsonPath("$.vehiclePickedUpBy").value(staff.getUsername()));

        mockMvc.perform(get("/api/vehicles/{id}/repair-history", vehicle.getId())
                .with(databaseUser(client.getUsername()).roles("CLIENT")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].appointmentId").value(appointment.getId()))
            .andExpect(jsonPath("$[0].appointmentReference").exists())
            .andExpect(jsonPath("$[0].repairDescription").value("Wymieniono tarcze i klocki hamulcowe."))
            .andExpect(jsonPath("$[0].totalGrossAmount").value(850.00))
            .andExpect(jsonPath("$[0].repairItems.length()").value(2))
            .andExpect(jsonPath("$[0].repairItems[0].name").value("Robocizna testowa"))
            .andExpect(jsonPath("$[0].vehiclePickedUpBy").value(staff.getUsername()));

        byte[] invoice = mockMvc.perform(get("/api/vehicles/{vehicleId}/repair-history/{appointmentId}/invoice",
                    vehicle.getId(), appointment.getId())
                .with(databaseUser(client.getUsername()).roles("CLIENT")))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", "application/pdf"))
            .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment")))
            .andReturn().getResponse().getContentAsByteArray();
        assertThat(new String(invoice, 0, 4)).isEqualTo("%PDF");

        byte[] staffInvoice = mockMvc.perform(get("/api/staff/appointments/{appointmentId}/invoice",
                    appointment.getId())
                .with(databaseUser(staff.getUsername()).roles("MECHANIC")))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", "application/pdf"))
            .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment")))
            .andReturn().getResponse().getContentAsByteArray();
        assertThat(new String(staffInvoice, 0, 4)).isEqualTo("%PDF");

        mockMvc.perform(get("/api/vehicles/{vehicleId}/repair-history/{appointmentId}/invoice",
                    vehicle.getId(), appointment.getId())
                .with(databaseUser(otherClient().getUsername()).roles("CLIENT")))
            .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/staff/appointments/{id}/complete-repair", appointment.getId())
                .with(databaseUser(staff.getUsername()).roles("MECHANIC"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(repairJson("Ponowny opis wykonanych prac.", "100.00")))
            .andExpect(status().isConflict());

        mockMvc.perform(post("/api/staff/appointments/{id}/mark-picked-up", appointment.getId())
                .with(databaseUser(staff.getUsername()).roles("MECHANIC"))
                .with(csrf()))
            .andExpect(status().isConflict());
    }

    @Test
    void validatesCompletedRepairDescriptionAndGrossAmount() throws Exception {
        AppUser client = createClient("repair-validation-client", true);
        AppUser staff = createUser("repair-validation-mechanic", UserRole.MECHANIC);
        Vehicle vehicle = createVehicle(client, "Seat", "Leon", "RZ123", null);
        createClientAppointment(client, vehicle, workingDate(2));
        AppointmentRequest appointment = appointments.findAll().getFirst();

        mockMvc.perform(post("/api/staff/appointments/{id}/accept", appointment.getId())
                .with(databaseUser(staff.getUsername()).roles("MECHANIC"))
                .with(csrf()))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/staff/appointments/{id}/complete-repair", appointment.getId())
                .with(databaseUser(staff.getUsername()).roles("MECHANIC"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(repairJson("Za krótko", "0.00")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors.repairDescription").exists())
            .andExpect(jsonPath("$.fieldErrors['repairItems[0].unitGrossAmount']").exists());
    }
}
