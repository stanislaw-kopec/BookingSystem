package pl.autoserwis.user;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pl.autoserwis.appointment.domain.AppointmentRequest;
import pl.autoserwis.appointment.domain.AppointmentRequesterType;
import pl.autoserwis.appointment.domain.RepairItemDraft;
import pl.autoserwis.appointment.domain.RepairItemType;
import pl.autoserwis.appointment.persistence.AppointmentRepository;
import pl.autoserwis.appointment.schedule.AppointmentSchedule;
import pl.autoserwis.profile.ClientProfile;
import pl.autoserwis.profile.ClientProfileRepository;
import pl.autoserwis.vehicle.Vehicle;
import pl.autoserwis.vehicle.VehicleRepository;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Component
@Profile("local")
public class LocalDemoData implements ApplicationRunner {
    private static final String PASSWORD = "client-local-2026";

    private final UserRepository users;
    private final PasswordEncoder passwords;
    private final ClientProfileRepository profiles;
    private final VehicleRepository vehicles;
    private final AppointmentRepository appointments;
    private final Clock clock;

    public LocalDemoData(UserRepository users, PasswordEncoder passwords,
            ClientProfileRepository profiles, VehicleRepository vehicles,
            AppointmentRepository appointments, Clock workshopClock) {
        this.users = users;
        this.passwords = passwords;
        this.profiles = profiles;
        this.vehicles = vehicles;
        this.appointments = appointments;
        this.clock = workshopClock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        AppUser anna = client("anna.demo", "anna.demo@local.invalid");
        profile(anna, false);
        Vehicle civic = vehicle(anna, "Honda", "Civic", 2018, "DW8CIVIC", "SHHFK2760HU000001");
        Vehicle passat = vehicle(anna, "Volkswagen", "Passat", 2020, "DW2PASS", "WVWZZZ3CZLE000001");

        AppUser firm = client("firma.demo", "firma.demo@local.invalid");
        profile(firm, true);
        Vehicle transit = vehicle(firm, "Ford", "Transit Custom", 2021, "DW5FIRM", "WF0YXXTTGYMD00001");

        AppUser mechanic = user("mechanic", "mechanic@local.invalid", "mechanic-local-2026", UserRole.MECHANIC);
        Instant now = Instant.now(clock).truncatedTo(ChronoUnit.SECONDS);

        pending(anna, civic, workingDate(1), "Podczas hamowania czuć bicie kierownicy i słychać tarcie z przodu.",
            UUID.fromString("11111111-1111-4111-8111-111111111111"), now.minus(5, ChronoUnit.HOURS));
        proposed(anna, passat, mechanic, workingDate(2), workingDate(4),
            "Auto traci moc przy wyprzedzaniu i czasami zapala kontrolkę silnika.",
            UUID.fromString("22222222-2222-4222-8222-222222222222"), now.minus(2, ChronoUnit.DAYS));
        confirmed(firm, transit, mechanic, workingDate(3),
            "W busie dostawczym słychać stukanie z przodu na nierównościach.",
            UUID.fromString("33333333-3333-4333-8333-333333333333"), now.minus(1, ChronoUnit.DAYS));
        readyForPickup(anna, civic, mechanic, workingDate(-1),
            "Wymieniono olej silnikowy, filtr oleju, filtr kabinowy oraz sprawdzono szczelność układu.",
            "Serwis okresowy połączony z kontrolą przed dłuższą trasą.", new BigDecimal("620.00"),
            UUID.fromString("44444444-4444-4444-8444-444444444444"), now.minus(8, ChronoUnit.HOURS));
        completed(firm, transit, mechanic, workingDate(-5),
            "Wymieniono klocki i tarcze hamulcowe na przedniej osi, wyczyszczono prowadnice zacisków i wykonano jazdę próbną.",
            "Hamulce piszczały i auto gorzej hamowało przy większym obciążeniu.", new BigDecimal("1850.00"),
            UUID.fromString("55555555-5555-4555-8555-555555555555"), now.minus(4, ChronoUnit.DAYS));
        completed(anna, civic, mechanic, workingDate(-10),
            "Wymieniono świece zapłonowe i jedną cewkę zapłonową, skasowano błędy oraz sprawdzono parametry pracy silnika.",
            "Silnik nierówno pracował na zimno i migała kontrolka check engine.", new BigDecimal("480.00"),
            UUID.fromString("66666666-6666-4666-8666-666666666666"), now.minus(9, ChronoUnit.DAYS));
    }

    private AppUser client(String username, String email) {
        return user(username, email, PASSWORD, UserRole.CLIENT);
    }

    private AppUser user(String username, String email, String password, UserRole role) {
        return users.findByUsernameIgnoreCase(username)
            .orElseGet(() -> users.save(new AppUser(username, email, passwords.encode(password), role)));
    }

    private void profile(AppUser user, boolean company) {
        if (profiles.findByUser_Id(user.getId()).isPresent()) return;
        ClientProfile profile = new ClientProfile(user);
        if (company) {
            profile.update("Piotr", "Zieliński", "+48 501 222 333", "biuro@zielinski-trans.local",
                "ul. Legnicka 48", "53-674", "Wrocław", true,
                "Zieliński Trans Sp. z o.o.", "8971999999", "ul. Legnicka 48", "53-674", "Wrocław");
        } else {
            profile.update("Anna", "Kowalska", "+48 500 111 222", "anna.kowalska@example.com",
                "ul. Tęczowa 12/4", "53-601", "Wrocław", false,
                null, null, null, null, null);
        }
        profiles.save(profile);
    }

    private Vehicle vehicle(AppUser owner, String make, String model, int year, String registration, String vin) {
        return vehicles.findByOwner_IdAndRegistrationNumberIgnoreCase(owner.getId(), registration)
            .orElseGet(() -> vehicles.save(new Vehicle(owner, make, model, year, registration, vin)));
    }

    private void pending(AppUser client, Vehicle vehicle, LocalDate date, String problem,
            UUID reference, Instant createdAt) {
        createBase(client, vehicle, date, problem, reference, createdAt);
    }

    private void proposed(AppUser client, Vehicle vehicle, AppUser staff, LocalDate requestedDate,
            LocalDate proposedDate, String problem, UUID reference, Instant createdAt) {
        AppointmentRequest appointment = createBase(client, vehicle, requestedDate, problem, reference, createdAt);
        if (appointment != null) {
            appointment.proposeTime(staff, start(proposedDate),
                "Proponujemy inny dzień, bo obecny grafik jest już wypełniony większymi naprawami.",
                createdAt.plus(3, ChronoUnit.HOURS));
            appointments.save(appointment);
        }
    }

    private void confirmed(AppUser client, Vehicle vehicle, AppUser staff, LocalDate date,
            String problem, UUID reference, Instant createdAt) {
        AppointmentRequest appointment = createBase(client, vehicle, date, problem, reference, createdAt);
        if (appointment != null) {
            appointment.accept(staff, createdAt.plus(2, ChronoUnit.HOURS));
            appointments.save(appointment);
        }
    }

    private void readyForPickup(AppUser client, Vehicle vehicle, AppUser staff, LocalDate date,
            String repair, String problem, BigDecimal amount, UUID reference, Instant createdAt) {
        AppointmentRequest appointment = createBase(client, vehicle, date, problem, reference, createdAt);
        if (appointment != null) {
            appointment.accept(staff, createdAt.plus(1, ChronoUnit.HOURS));
            appointment.completeRepair(staff, repair, demoItems(repair, amount), createdAt.plus(7, ChronoUnit.HOURS));
            appointments.save(appointment);
        }
    }

    private void completed(AppUser client, Vehicle vehicle, AppUser staff, LocalDate date,
            String repair, String problem, BigDecimal amount, UUID reference, Instant createdAt) {
        AppointmentRequest appointment = createBase(client, vehicle, date, problem, reference, createdAt);
        if (appointment != null) {
            appointment.accept(staff, createdAt.plus(1, ChronoUnit.HOURS));
            appointment.completeRepair(staff, repair, demoItems(repair, amount), createdAt.plus(6, ChronoUnit.HOURS));
            appointment.markPickedUp(staff, createdAt.plus(8, ChronoUnit.HOURS));
            appointments.save(appointment);
        }
    }

    private java.util.List<RepairItemDraft> demoItems(String repair, BigDecimal amount) {
        BigDecimal labor = amount.multiply(new BigDecimal("0.45")).setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal parts = amount.subtract(labor).setScale(2, java.math.RoundingMode.HALF_UP);
        return java.util.List.of(
            new RepairItemDraft(RepairItemType.LABOR, "Robocizna - " + repair.substring(0, Math.min(repair.length(), 80)), BigDecimal.ONE, labor),
            new RepairItemDraft(RepairItemType.PART, "Części i materiały użyte do naprawy", BigDecimal.ONE, parts)
        );
    }

    private AppointmentRequest createBase(AppUser client, Vehicle vehicle, LocalDate date,
            String problem, UUID reference, Instant createdAt) {
        if (appointments.existsByReference(reference)) return null;
        ClientProfile profile = profiles.findByUser_Id(client.getId()).orElseThrow();
        return appointments.save(new AppointmentRequest(reference, AppointmentRequesterType.CLIENT,
            client, vehicle, profile.getFirstName(), profile.getLastName(), profile.getPhoneNumber(),
            profile.getContactEmail(), vehicle.getMake(), vehicle.getModel(), vehicle.getProductionYear(),
            vehicle.getRegistrationNumber(), vehicle.getVin(), start(date), problem, createdAt));
    }

    private Instant start(LocalDate date) {
        return date.atTime(AppointmentSchedule.WORKDAY_START)
            .atZone(AppointmentSchedule.TIME_ZONE)
            .toInstant()
            .truncatedTo(ChronoUnit.SECONDS);
    }

    private LocalDate workingDate(int workingDaysOffset) {
        LocalDate date = LocalDate.now(clock);
        int remaining = Math.abs(workingDaysOffset);
        int direction = workingDaysOffset < 0 ? -1 : 1;
        while (remaining > 0) {
            date = date.plusDays(direction);
            if (isWorkingDay(date)) remaining--;
        }
        if (!isWorkingDay(date)) return workingDate(workingDaysOffset + direction);
        return date;
    }

    private boolean isWorkingDay(LocalDate date) {
        return date.getDayOfWeek().getValue() <= 5;
    }
}
