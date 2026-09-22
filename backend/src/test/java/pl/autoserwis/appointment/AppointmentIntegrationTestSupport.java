package pl.autoserwis.appointment;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import pl.autoserwis.appointment.domain.AppointmentRequest;
import pl.autoserwis.appointment.persistence.AppointmentRepository;
import pl.autoserwis.appointment.schedule.AppointmentSchedule;
import pl.autoserwis.profile.ClientProfile;
import pl.autoserwis.profile.ClientProfileRepository;
import pl.autoserwis.user.AppUser;
import pl.autoserwis.user.UserRepository;
import pl.autoserwis.user.UserRole;
import pl.autoserwis.vehicle.Vehicle;
import pl.autoserwis.vehicle.VehicleRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static pl.autoserwis.DatabaseTestUsers.databaseUser;

abstract class AppointmentIntegrationTestSupport {
    @Autowired protected MockMvc mockMvc;
    @Autowired protected AppointmentRepository appointments;
    @Autowired protected UserRepository users;
    @Autowired protected ClientProfileRepository profiles;
    @Autowired protected VehicleRepository vehicles;
    @Autowired protected PasswordEncoder passwords;

    protected void createClientAppointment(AppUser client, Vehicle vehicle, LocalDate visitDate)
            throws Exception {
        createClientAppointment(client, vehicle, visitDate, "Silnik wydaje niepokojący dźwięk.");
    }

    protected void createClientAppointment(AppUser client, Vehicle vehicle, LocalDate visitDate,
            String description) throws Exception {
        mockMvc.perform(post("/api/appointments")
                .with(databaseUser(client.getUsername()).roles("CLIENT"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(clientJson(vehicle.getId(), visitDate, description)))
            .andExpect(status().isCreated());
    }

    protected void createGuestAppointment(LocalDate visitDate, String description) throws Exception {
        mockMvc.perform(post("/api/appointments/guest")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(guestJson(visitDate, "+48 500 600 700", "", "KR123", "", description)))
            .andExpect(status().isCreated());
    }

    protected void propose(AppUser staff, AppointmentRequest appointment, LocalDate proposed)
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

    protected AppUser otherClient() {
        return createClient("invoice-other-client", true);
    }

    protected AppUser createClient(String username, boolean withProfile) {
        AppUser client = createUser(username, UserRole.CLIENT);
        if (withProfile) createProfile(client);
        return client;
    }

    protected AppUser createUser(String username, UserRole role) {
        return users.saveAndFlush(new AppUser(username, username + "@example.com",
            passwords.encode("test-password"), role));
    }

    protected void createProfile(AppUser client) {
        ClientProfile profile = new ClientProfile(client);
        profile.update("Jan", "Kowalski", "+48 500 600 700", "kontakt@example.com",
            "ul. Prosta 1", "00-001", "Warszawa", false,
            null, null, null, null, null);
        profiles.saveAndFlush(profile);
    }

    protected Vehicle createVehicle(AppUser owner, String make, String model,
            String registrationNumber, String vin) {
        return vehicles.saveAndFlush(new Vehicle(owner, make, model, 2020, registrationNumber, vin));
    }

    protected LocalDate workingDate(int workingDaysAfterFirst) {
        LocalDate date = LocalDate.now(AppointmentSchedule.TIME_ZONE).plusDays(1);
        while (isWeekend(date)) date = date.plusDays(1);
        for (int index = 0; index < workingDaysAfterFirst; index++) {
            date = date.plusDays(1);
            while (isWeekend(date)) date = date.plusDays(1);
        }
        return date;
    }

    protected LocalDate next(DayOfWeek dayOfWeek) {
        LocalDate date = LocalDate.now(AppointmentSchedule.TIME_ZONE).plusDays(1);
        while (date.getDayOfWeek() != dayOfWeek) date = date.plusDays(1);
        return date;
    }

    protected boolean isWeekend(LocalDate date) {
        return date.getDayOfWeek() == DayOfWeek.SATURDAY
            || date.getDayOfWeek() == DayOfWeek.SUNDAY;
    }

    protected String apiTime(LocalDate value) {
        return value.atTime(AppointmentSchedule.WORKDAY_START)
            .atZone(AppointmentSchedule.TIME_ZONE)
            .toOffsetDateTime()
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX"));
    }

    protected String clientJson(Long vehicleId, LocalDate visitDate, String description) {
        return """
            {
              "vehicleId": %d,
              "visitDate": "%s",
              "problemDescription": "%s"
            }
            """.formatted(vehicleId, visitDate, description);
    }

    protected String guestJson(LocalDate visitDate, String phoneNumber, String contactEmail,
            String registrationNumber, String vin, String description) {
        return guestJsonWithYear(visitDate, phoneNumber, contactEmail, registrationNumber, vin,
            description, 2020);
    }

    protected String guestJsonWithYear(LocalDate visitDate, String phoneNumber, String contactEmail,
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

    protected String repairJson(String repairDescription, String totalGrossAmount) {
        BigDecimal total = new BigDecimal(totalGrossAmount);
        BigDecimal labor = total.multiply(new BigDecimal("0.40")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal parts = total.subtract(labor).setScale(2, RoundingMode.HALF_UP);
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
