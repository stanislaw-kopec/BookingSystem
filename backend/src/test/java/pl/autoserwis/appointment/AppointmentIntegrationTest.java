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

import java.time.*;
import java.time.format.DateTimeFormatter;

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
class AppointmentIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired AppointmentRepository appointments;
    @Autowired UserRepository users;
    @Autowired ClientProfileRepository profiles;
    @Autowired VehicleRepository vehicles;
    @Autowired PasswordEncoder passwords;

    @Test
    void returnsPublicAvailabilityForConfiguredWorkingHours() throws Exception {
        mockMvc.perform(get("/api/appointments/availability"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.timeZone").value("Europe/Warsaw"))
            .andExpect(jsonPath("$.slotDurationMinutes").value(60))
            .andExpect(jsonPath("$.slots").isArray())
            .andExpect(jsonPath("$.slots[0].startAt").exists())
            .andExpect(jsonPath("$.slots[0].endAt").exists())
            .andExpect(jsonPath("$.slots[0].available").value(true));
    }

    @Test
    void clientCreatesAppointmentWithOwnVehicleAndProfileSnapshot() throws Exception {
        AppUser client = createClient("booking-client", true);
        Vehicle vehicle = createVehicle(client, "Toyota", "Corolla", "KR123", null);
        OffsetDateTime startAt = workingSlot(0, 8);

        mockMvc.perform(post("/api/appointments")
                .with(user(client.getUsername()).roles("CLIENT"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(clientJson(vehicle.getId(), startAt, "Silnik nierówno pracuje.")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.reference").isNotEmpty())
            .andExpect(jsonPath("$.requesterType").value("CLIENT"))
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andExpect(jsonPath("$.vehicleId").value(vehicle.getId()))
            .andExpect(jsonPath("$.vehicleRegistrationNumber").value("KR123"))
            .andExpect(jsonPath("$.firstName").value("Jan"))
            .andExpect(jsonPath("$.requestedStartAt").value(apiTime(startAt)))
            .andExpect(jsonPath("$.currentStartAt").value(apiTime(startAt)));

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
        OffsetDateTime startAt = workingSlot(0, 9);

        mockMvc.perform(post("/api/appointments")
                .with(user(client.getUsername()).roles("CLIENT"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(clientJson(otherVehicle.getId(), startAt, "Samochód nie chce odpalić.")))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.fieldErrors.profile").exists());

        createProfile(client);
        mockMvc.perform(post("/api/appointments")
                .with(user(client.getUsername()).roles("CLIENT"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(clientJson(otherVehicle.getId(), startAt, "Samochód nie chce odpalić.")))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Nie znaleziono pojazdu."));
    }

    @Test
    void guestCreatesAppointmentWithOneContactAndNormalizedVehicleData() throws Exception {
        OffsetDateTime startAt = workingSlot(0, 10);

        mockMvc.perform(post("/api/appointments/guest")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(guestJson(startAt, "", "GUEST@EXAMPLE.COM", " kr 456 ",
                    "wvwzzz1jzxw000001", "Podczas hamowania słychać pisk.")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.requesterType").value("GUEST"))
            .andExpect(jsonPath("$.vehicleId").doesNotExist())
            .andExpect(jsonPath("$.vehicleRegistrationNumber").value("KR456"))
            .andExpect(jsonPath("$.vehicleVin").value("WVWZZZ1JZXW000001"))
            .andExpect(jsonPath("$.phoneNumber").value(""))
            .andExpect(jsonPath("$.contactEmail").value("guest@example.com"));
    }

    @Test
    void validatesGuestContactVehicleYearAndDescriptionAfterTrimming() throws Exception {
        OffsetDateTime startAt = workingSlot(0, 11);

        mockMvc.perform(post("/api/appointments/guest")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(guestJsonWithYear(startAt, "", "", " - ", "",
                    "a         ", Year.now().getValue() + 2)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors.phoneNumber").exists())
            .andExpect(jsonPath("$.fieldErrors.contactEmail").exists())
            .andExpect(jsonPath("$.fieldErrors.vehicleProductionYear").exists())
            .andExpect(jsonPath("$.fieldErrors.vehicleRegistrationNumber").exists())
            .andExpect(jsonPath("$.fieldErrors.problemDescription").exists());
    }

    @Test
    void rejectsSlotsOutsideScheduleAndSlotsWithSubsecondPrecision() throws Exception {
        AppUser client = createClient("slot-validation-client", true);
        Vehicle vehicle = createVehicle(client, "Honda", "Civic", "WA123", null);
        LocalDate weekend = next(DayOfWeek.SATURDAY);
        OffsetDateTime weekendSlot = weekend.atTime(8, 0)
            .atZone(AppointmentSchedule.TIME_ZONE).toOffsetDateTime();

        mockMvc.perform(post("/api/appointments")
                .with(user(client.getUsername()).roles("CLIENT"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(clientJson(vehicle.getId(), weekendSlot, "Problem z układem kierowniczym.")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors.slotStartAt").exists());

        OffsetDateTime preciseSlot = workingSlot(0, 12).plusNanos(500_000_000);
        mockMvc.perform(post("/api/appointments")
                .with(user(client.getUsername()).roles("CLIENT"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(clientJson(vehicle.getId(), preciseSlot, "Problem z układem kierowniczym.")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors.slotStartAt").exists());
    }

    @Test
    void activeRequestBlocksSlotAndRejectionReleasesIt() throws Exception {
        AppUser staff = createUser("slot-mechanic", UserRole.MECHANIC);
        OffsetDateTime startAt = workingSlot(1, 8);

        createGuestAppointment(startAt, "Pierwsze zgłoszenie usterki.");
        AppointmentRequest first = appointments.findAll().getFirst();

        mockMvc.perform(post("/api/appointments/guest")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(guestJson(startAt, "+48 500 600 700", "", "KR789", "",
                    "Drugie zgłoszenie usterki.")))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.fieldErrors.slotStartAt").exists());

        mockMvc.perform(post("/api/staff/appointments/{id}/reject", first.getId())
                .with(user(staff.getUsername()).roles("MECHANIC"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"Brak odpowiedniej części.\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("REJECTED"));

        mockMvc.perform(post("/api/appointments/guest")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(guestJson(startAt, "+48 500 600 700", "", "KR789", "",
                    "Drugie zgłoszenie usterki.")))
            .andExpect(status().isCreated());
    }

    @Test
    void staffProposesTimeAndOnlyOwningClientCanConfirmIt() throws Exception {
        AppUser owner = createClient("proposal-owner", true);
        AppUser other = createClient("proposal-other", true);
        AppUser staff = createUser("proposal-mechanic", UserRole.MECHANIC);
        Vehicle vehicle = createVehicle(owner, "Mazda", "3", "LU123", null);
        OffsetDateTime requested = workingSlot(1, 9);
        OffsetDateTime proposed = workingSlot(1, 10);
        createClientAppointment(owner, vehicle, requested);
        AppointmentRequest appointment = appointments.findAll().getFirst();

        mockMvc.perform(post("/api/staff/appointments/{id}/propose-time", appointment.getId())
                .with(user(staff.getUsername()).roles("MECHANIC"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"slotStartAt":"%s","message":"Potrzebujemy dłuższego okna."}
                    """.formatted(proposed)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("TIME_PROPOSED"))
            .andExpect(jsonPath("$.requestedStartAt").value(apiTime(requested)))
            .andExpect(jsonPath("$.currentStartAt").value(apiTime(proposed)))
            .andExpect(jsonPath("$.staffActionBy").value(staff.getUsername()));

        mockMvc.perform(post("/api/appointments/{id}/confirm-proposed", appointment.getId())
                .with(user(other.getUsername()).roles("CLIENT"))
                .with(csrf()))
            .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/appointments/{id}/confirm-proposed", appointment.getId())
                .with(user(owner.getUsername()).roles("CLIENT"))
                .with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CONFIRMED"))
            .andExpect(jsonPath("$.clientConfirmedAt").exists());
    }

    @Test
    void onlyStaffCanConfirmGuestProposalAfterContact() throws Exception {
        AppUser client = createClient("guest-proposal-client", true);
        AppUser staff = createUser("guest-proposal-admin", UserRole.ADMIN);
        OffsetDateTime requested = workingSlot(2, 8);
        OffsetDateTime proposed = workingSlot(2, 9);
        createGuestAppointment(requested, "Problem z instalacją elektryczną.");
        AppointmentRequest appointment = appointments.findAll().getFirst();

        propose(staff, appointment, proposed);

        mockMvc.perform(post("/api/appointments/{id}/confirm-proposed", appointment.getId())
                .with(user(client.getUsername()).roles("CLIENT"))
                .with(csrf()))
            .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/staff/appointments/{id}/confirm-proposed", appointment.getId())
                .with(user(staff.getUsername()).roles("ADMIN"))
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
        createClientAppointment(client, vehicle, workingSlot(2, 10));
        AppointmentRequest appointment = appointments.findAll().getFirst();

        mockMvc.perform(post("/api/staff/appointments/{id}/accept", appointment.getId())
                .with(user(staff.getUsername()).roles("MECHANIC"))
                .with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CONFIRMED"))
            .andExpect(jsonPath("$.staffActionAt").exists());

        mockMvc.perform(post("/api/staff/appointments/{id}/accept", appointment.getId())
                .with(user(staff.getUsername()).roles("MECHANIC"))
                .with(csrf()))
            .andExpect(status().isConflict());

        mockMvc.perform(get("/api/appointments")
                .with(user(client.getUsername()).roles("CLIENT")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(get("/api/appointments")
                .with(user(other.getUsername()).roles("CLIENT")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void changingRequestsRequiresCsrfAndCorrectRole() throws Exception {
        AppUser client = createClient("secured-appointment-client", true);
        AppUser mechanic = createUser("secured-appointment-mechanic", UserRole.MECHANIC);
        Vehicle vehicle = createVehicle(client, "Volvo", "V60", "BI123", null);
        OffsetDateTime startAt = workingSlot(3, 8);

        mockMvc.perform(post("/api/appointments/guest")
                .contentType(MediaType.APPLICATION_JSON)
                .content(guestJson(startAt, "+48 500 600 700", "", "BI456", "",
                    "Problem z układem hamulcowym.")))
            .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/appointments")
                .with(user(client.getUsername()).roles("CLIENT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(clientJson(vehicle.getId(), startAt, "Problem z układem hamulcowym.")))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/appointments"))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/appointments")
                .with(user(mechanic.getUsername()).roles("MECHANIC")))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/staff/appointments")
                .with(user(client.getUsername()).roles("CLIENT")))
            .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {"MECHANIC", "ADMIN"})
    void staffRolesCanListAllRequests(String role) throws Exception {
        AppUser staff = createUser("list-" + role.toLowerCase(), UserRole.valueOf(role));

        mockMvc.perform(get("/api/staff/appointments")
                .with(user(staff.getUsername()).roles(role)))
            .andExpect(status().isOk());
    }

    private void createClientAppointment(AppUser client, Vehicle vehicle, OffsetDateTime startAt)
            throws Exception {
        mockMvc.perform(post("/api/appointments")
                .with(user(client.getUsername()).roles("CLIENT"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(clientJson(vehicle.getId(), startAt, "Silnik wydaje niepokojący dźwięk.")))
            .andExpect(status().isCreated());
    }

    private void createGuestAppointment(OffsetDateTime startAt, String description) throws Exception {
        mockMvc.perform(post("/api/appointments/guest")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(guestJson(startAt, "+48 500 600 700", "", "KR123", "", description)))
            .andExpect(status().isCreated());
    }

    private void propose(AppUser staff, AppointmentRequest appointment, OffsetDateTime proposed)
            throws Exception {
        mockMvc.perform(post("/api/staff/appointments/{id}/propose-time", appointment.getId())
                .with(user(staff.getUsername()).roles(staff.getRole().name()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"slotStartAt\":\"%s\",\"message\":\"Nowy termin po kontakcie.\"}"
                    .formatted(proposed)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("TIME_PROPOSED"));
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

    private OffsetDateTime workingSlot(int workingDaysAfterFirst, int hour) {
        LocalDate date = LocalDate.now(AppointmentSchedule.TIME_ZONE).plusDays(1);
        while (isWeekend(date)) date = date.plusDays(1);
        for (int index = 0; index < workingDaysAfterFirst; index++) {
            date = date.plusDays(1);
            while (isWeekend(date)) date = date.plusDays(1);
        }
        return date.atTime(hour, 0).atZone(AppointmentSchedule.TIME_ZONE).toOffsetDateTime();
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

    private String apiTime(OffsetDateTime value) {
        return value.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX"));
    }

    private String clientJson(Long vehicleId, OffsetDateTime startAt, String description) {
        return """
            {
              "vehicleId": %d,
              "slotStartAt": "%s",
              "problemDescription": "%s"
            }
            """.formatted(vehicleId, startAt, description);
    }

    private String guestJson(OffsetDateTime startAt, String phoneNumber, String contactEmail,
            String registrationNumber, String vin, String description) {
        return guestJsonWithYear(startAt, phoneNumber, contactEmail, registrationNumber, vin,
            description, 2020);
    }

    private String guestJsonWithYear(OffsetDateTime startAt, String phoneNumber, String contactEmail,
            String registrationNumber, String vin, String description, int productionYear) {
        return """
            {
              "firstName": "Anna", "lastName": "Nowak",
              "phoneNumber": "%s", "contactEmail": "%s",
              "vehicleMake": "Toyota", "vehicleModel": "Yaris",
              "vehicleProductionYear": %d,
              "vehicleRegistrationNumber": "%s", "vehicleVin": "%s",
              "slotStartAt": "%s", "problemDescription": "%s"
            }
            """.formatted(phoneNumber, contactEmail, productionYear, registrationNumber,
                vin, startAt, description);
    }
}
