package pl.autoserwis.appointment;

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
import pl.autoserwis.profile.ClientProfile;
import pl.autoserwis.profile.ClientProfileRepository;
import pl.autoserwis.user.AppUser;
import pl.autoserwis.user.UserRepository;
import pl.autoserwis.user.UserRole;
import pl.autoserwis.vehicle.Vehicle;
import pl.autoserwis.vehicle.VehicleRepository;

import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static pl.autoserwis.DatabaseTestUsers.databaseUser;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.docker.compose.enabled=false")
@AutoConfigureMockMvc
@Import(PostgresTestConfiguration.class)
@ActiveProfiles("test")
@Transactional
class AppointmentIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired AppointmentRepository appointments;
    @Autowired UserRepository users;
    @Autowired ClientProfileRepository profiles;
    @Autowired VehicleRepository vehicles;
    @Autowired PasswordEncoder passwords;

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

    private void createClientAppointment(AppUser client, Vehicle vehicle, LocalDate visitDate)
            throws Exception {
        createClientAppointment(client, vehicle, visitDate, "Silnik wydaje niepokojący dźwięk.");
    }

    private void createClientAppointment(AppUser client, Vehicle vehicle, LocalDate visitDate,
            String description) throws Exception {
        mockMvc.perform(post("/api/appointments")
                .with(databaseUser(client.getUsername()).roles("CLIENT"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(clientJson(vehicle.getId(), visitDate, description)))
            .andExpect(status().isCreated());
    }

    private void createGuestAppointment(LocalDate visitDate, String description) throws Exception {
        mockMvc.perform(post("/api/appointments/guest")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(guestJson(visitDate, "+48 500 600 700", "", "KR123", "", description)))
            .andExpect(status().isCreated());
    }

    private void propose(AppUser staff, AppointmentRequest appointment, LocalDate proposed)
            throws Exception {
        mockMvc.perform(post("/api/staff/appointments/{id}/propose-time", appointment.getId())
                .with(databaseUser(staff.getUsername()).roles(staff.getRole().name()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"visitDate\":\"%s\",\"message\":\"Nowy dzień po kontakcie.\"}"
                    .formatted(proposed)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("TIME_PROPOSED"));
    }

    private AppUser otherClient() {
        return createClient("invoice-other-client", true);
    }

    private AppUser createClient(String username, boolean withProfile) {
        AppUser client = createUser(username, UserRole.CLIENT);
        if (withProfile) createProfile(client);
        return client;
    }

    private AppUser createUser(String username, UserRole role) {
        return users.saveAndFlush(new AppUser(username, username + "@example.com",
            passwords.encode("test-password"), role));
    }

    private void createProfile(AppUser client) {
        ClientProfile profile = new ClientProfile(client);
        profile.update("Jan", "Kowalski", "+48 500 600 700", "kontakt@example.com",
            "ul. Prosta 1", "00-001", "Warszawa", false,
            null, null, null, null, null);
        profiles.saveAndFlush(profile);
    }

    private Vehicle createVehicle(AppUser owner, String make, String model,
            String registrationNumber, String vin) {
        return vehicles.saveAndFlush(new Vehicle(owner, make, model, 2020, registrationNumber, vin));
    }

    private LocalDate workingDate(int workingDaysAfterFirst) {
        LocalDate date = LocalDate.now(AppointmentSchedule.TIME_ZONE).plusDays(1);
        while (isWeekend(date)) date = date.plusDays(1);
        for (int index = 0; index < workingDaysAfterFirst; index++) {
            date = date.plusDays(1);
            while (isWeekend(date)) date = date.plusDays(1);
        }
        return date;
    }

    private LocalDate next(DayOfWeek dayOfWeek) {
        LocalDate date = LocalDate.now(AppointmentSchedule.TIME_ZONE).plusDays(1);
        while (date.getDayOfWeek() != dayOfWeek) date = date.plusDays(1);
        return date;
    }

    private boolean isWeekend(LocalDate date) {
        return date.getDayOfWeek() == DayOfWeek.SATURDAY
            || date.getDayOfWeek() == DayOfWeek.SUNDAY;
    }

    private String apiTime(LocalDate value) {
        return value.atTime(AppointmentSchedule.WORKDAY_START)
            .atZone(AppointmentSchedule.TIME_ZONE)
            .toOffsetDateTime()
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX"));
    }

    private String clientJson(Long vehicleId, LocalDate visitDate, String description) {
        return """
            {
              "vehicleId": %d,
              "visitDate": "%s",
              "problemDescription": "%s"
            }
            """.formatted(vehicleId, visitDate, description);
    }

    private String guestJson(LocalDate visitDate, String phoneNumber, String contactEmail,
            String registrationNumber, String vin, String description) {
        return guestJsonWithYear(visitDate, phoneNumber, contactEmail, registrationNumber, vin,
            description, 2020);
    }

    private String guestJsonWithYear(LocalDate visitDate, String phoneNumber, String contactEmail,
            String registrationNumber, String vin, String description, int productionYear) {
        return """
            {
              "firstName": "Anna", "lastName": "Nowak",
              "phoneNumber": "%s", "contactEmail": "%s",
              "vehicleMake": "Toyota", "vehicleModel": "Yaris",
              "vehicleProductionYear": %d,
              "vehicleRegistrationNumber": "%s", "vehicleVin": "%s",
              "visitDate": "%s", "problemDescription": "%s"
            }
            """.formatted(phoneNumber, contactEmail, productionYear, registrationNumber,
                vin, visitDate, description);
    }

    private String repairJson(String repairDescription, String totalGrossAmount) {
        BigDecimal total = new BigDecimal(totalGrossAmount);
        BigDecimal labor = total.multiply(new BigDecimal("0.40")).setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal parts = total.subtract(labor).setScale(2, java.math.RoundingMode.HALF_UP);
        return """
            {
              "repairDescription": "%s",
              "repairItems": [
                {
                  "type": "LABOR",
                  "name": "Robocizna testowa",
                  "quantity": 1,
                  "unitGrossAmount": %s
                },
                {
                  "type": "PART",
                  "name": "Części testowe",
                  "quantity": 1,
                  "unitGrossAmount": %s
                }
              ]
            }
            """.formatted(repairDescription, labor, parts);
    }

}
