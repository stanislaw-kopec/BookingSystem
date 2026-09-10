package pl.autoserwis.appointment;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.autoserwis.appointment.dto.*;
import pl.autoserwis.exception.ResourceNotFoundException;
import pl.autoserwis.profile.ClientProfile;
import pl.autoserwis.profile.ClientProfileRepository;
import pl.autoserwis.user.AppUser;
import pl.autoserwis.user.UserRepository;
import pl.autoserwis.vehicle.Vehicle;
import pl.autoserwis.vehicle.VehicleRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.Year;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
@Service
@Transactional(readOnly = true)
public class AppointmentService {
    private static final int MINIMUM_PROBLEM_DESCRIPTION_LENGTH = 10;
    private final AppointmentRepository appointments;
    private final AppointmentSchedule schedule;
    private final UserRepository users;
    private final ClientProfileRepository profiles;
    private final VehicleRepository vehicles;
    public AppointmentService(AppointmentRepository appointments, AppointmentSchedule schedule,
            UserRepository users, ClientProfileRepository profiles, VehicleRepository vehicles) {
        this.appointments = appointments;
        this.schedule = schedule;
        this.users = users;
        this.profiles = profiles;
        this.vehicles = vehicles;
    }
    public AppointmentAvailabilityResponse getAvailability() {
        return schedule.availability();
    }
    public List<AppointmentResponse> getCurrentClientAppointments(String username) {
        AppUser client = user(username);
        return appointments.findByClient_IdOrderByCreatedAtDesc(client.getId()).stream()
            .map(this::response)
            .toList();
    }
    public List<AppointmentResponse> getStaffAppointments() {
        return appointments.findAllByOrderByCreatedAtDesc().stream()
            .map(this::response)
            .toList();
    }
    @Transactional
    public AppointmentResponse createForClient(String username, ClientAppointmentRequest request) {
        AppUser client = user(username);
        ClientProfile profile = profiles.findByUser_Id(client.getId())
            .orElseThrow(() -> new AppointmentConflictException("profile",
                "Uzupełnij profil przed umówieniem wizyty."));
        Vehicle vehicle = vehicles.findByIdAndOwner_Id(request.vehicleId(), client.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Nie znaleziono pojazdu."));
        Instant startAt = schedule.validateAndNormalize(request.visitDate());
        String description = normalizedDescription(request.problemDescription());
        AppointmentRequest appointment = new AppointmentRequest(UUID.randomUUID(),
            AppointmentRequesterType.CLIENT, client, vehicle,
            profile.getFirstName(), profile.getLastName(), profile.getPhoneNumber(),
            profile.getContactEmail(), vehicle.getMake(), vehicle.getModel(),
            vehicle.getProductionYear(), vehicle.getRegistrationNumber(), optional(vehicle.getVin()),
            startAt, description, Instant.now());
        return saveInAvailableDay(appointment);
    }
    @Transactional
    public AppointmentResponse createForGuest(GuestAppointmentRequest request) {
        validateGuest(request);
        Instant startAt = schedule.validateAndNormalize(request.visitDate());
        String registrationNumber = normalizedRegistration(request.vehicleRegistrationNumber());
        String description = normalizedDescription(request.problemDescription());
        AppointmentRequest appointment = new AppointmentRequest(UUID.randomUUID(),
            AppointmentRequesterType.GUEST, null, null,
            request.firstName().strip(), request.lastName().strip(), optional(request.phoneNumber()),
            optionalLowercase(request.contactEmail()), request.vehicleMake().strip(),
            request.vehicleModel().strip(), request.vehicleProductionYear(), registrationNumber,
            optionalUppercase(request.vehicleVin()), startAt, description, Instant.now());
        return saveInAvailableDay(appointment);
    }
    @Transactional
    public AppointmentResponse accept(String staffUsername, Long appointmentId) {
        AppointmentRequest appointment = appointmentForStaffUpdate(appointmentId);
        requireStatus(appointment, AppointmentStatus.PENDING,
            "Można przyjąć wyłącznie oczekujące zgłoszenie.");
        appointment.accept(user(staffUsername), Instant.now());
        return response(appointments.save(appointment));
    }
    @Transactional
    public AppointmentResponse reject(String staffUsername, Long appointmentId,
            StaffMessageRequest request) {
        AppointmentRequest appointment = appointmentForStaffUpdate(appointmentId);
        if (appointment.getStatus() != AppointmentStatus.PENDING
                && appointment.getStatus() != AppointmentStatus.TIME_PROPOSED) {
            throw new AppointmentConflictException(
                "Można odrzucić wyłącznie oczekujące zgłoszenie lub propozycję dnia.");
        }
        appointment.reject(user(staffUsername), optional(request.message()), Instant.now());
        return response(appointments.save(appointment));
    }
    @Transactional
    public AppointmentResponse proposeTime(String staffUsername, Long appointmentId,
            ProposeAppointmentTimeRequest request) {
        AppointmentRequest appointment = appointmentForStaffUpdate(appointmentId);
        if (appointment.getStatus() != AppointmentStatus.PENDING
                && appointment.getStatus() != AppointmentStatus.TIME_PROPOSED) {
            throw new AppointmentConflictException(
                "Nowy dzień można zaproponować tylko dla oczekującego zgłoszenia.");
        }
        Instant proposedStartAt = schedule.validateAndNormalize(request.visitDate());
        if (proposedStartAt.equals(appointment.getCurrentStartAt())) {
            throw new AppointmentValidationException(Map.of("visitDate",
                "Zaproponuj dzień inny niż obecny."));
        }
        appointments.lockAppointmentDay(schedule.visitDate(proposedStartAt));
        if (schedule.isFull(proposedStartAt)) {
            throw unavailableDay();
        }
        appointment.proposeTime(user(staffUsername), proposedStartAt,
            optional(request.message()), Instant.now());
        try {
            return response(appointments.saveAndFlush(appointment));
        } catch (DataIntegrityViolationException exception) {
            throw unavailableDay();
        }
    }
    @Transactional
    public AppointmentResponse confirmProposedTime(String username, Long appointmentId) {
        AppUser client = user(username);
        AppointmentRequest appointment = appointments.findByIdAndClientIdForUpdate(
                appointmentId, client.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Nie znaleziono zgłoszenia wizyty."));
        requireStatus(appointment, AppointmentStatus.TIME_PROPOSED,
            "To zgłoszenie nie oczekuje na potwierdzenie nowego dnia.");
        appointment.confirmProposedTime(Instant.now());
        return response(appointments.save(appointment));
    }
    @Transactional
    public AppointmentResponse cancelClientAppointment(String username, Long appointmentId) {
        AppUser client = user(username);
        AppointmentRequest appointment = appointments.findByIdAndClientIdForUpdate(
                appointmentId, client.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Nie znaleziono zgłoszenia wizyty."));
        if (appointment.getStatus() != AppointmentStatus.PENDING
                && appointment.getStatus() != AppointmentStatus.TIME_PROPOSED
                && appointment.getStatus() != AppointmentStatus.CONFIRMED) {
            throw new AppointmentConflictException(
                "Można odwołać wyłącznie aktywną wizytę.");
        }
        appointment.cancel(Instant.now());
        return response(appointments.save(appointment));
    }
    @Transactional
    public AppointmentResponse confirmGuestProposedTime(String staffUsername, Long appointmentId) {
        AppointmentRequest appointment = appointmentForStaffUpdate(appointmentId);
        if (appointment.getRequesterType() != AppointmentRequesterType.GUEST) {
            throw new AppointmentConflictException(
                "Klient posiadający konto sam potwierdza zaproponowany dzień.");
        }
        requireStatus(appointment, AppointmentStatus.TIME_PROPOSED,
            "To zgłoszenie nie oczekuje na potwierdzenie nowego dnia.");
        appointment.confirmGuestProposedTime(user(staffUsername), Instant.now());
        return response(appointments.save(appointment));
    }
    @Transactional
    public AppointmentResponse completeRepair(String staffUsername, Long appointmentId,
            CompleteRepairRequest request) {
        AppointmentRequest appointment = appointmentForStaffUpdate(appointmentId);
        requireStatus(appointment, AppointmentStatus.CONFIRMED,
            "Naprawę można zakończyć tylko dla potwierdzonej wizyty.");
        appointment.completeRepair(user(staffUsername),
            normalizedRepairDescription(request.repairDescription()),
            normalizedMoney(request.totalGrossAmount()),
            Instant.now());
        return response(appointments.save(appointment));
    }
    @Transactional
    public AppointmentResponse markPickedUp(String staffUsername, Long appointmentId) {
        AppointmentRequest appointment = appointmentForStaffUpdate(appointmentId);
        requireStatus(appointment, AppointmentStatus.READY_FOR_PICKUP,
            "Odbior samochodu mozna potwierdzic tylko dla auta oczekujacego na odbior.");
        appointment.markPickedUp(user(staffUsername), Instant.now());
        return response(appointments.save(appointment));
    }
    private AppointmentResponse saveInAvailableDay(AppointmentRequest appointment) {
        appointments.lockAppointmentDay(schedule.visitDate(appointment.getCurrentStartAt()));
        if (schedule.isFull(appointment.getCurrentStartAt())) {
            throw unavailableDay();
        }
        try {
            return response(appointments.saveAndFlush(appointment));
        } catch (DataIntegrityViolationException exception) {
            throw unavailableDay();
        }
    }
    private AppointmentRequest appointmentForStaffUpdate(Long appointmentId) {
        return appointments.findByIdForUpdate(appointmentId)
            .orElseThrow(() -> new ResourceNotFoundException("Nie znaleziono zgłoszenia wizyty."));
    }
    private void requireStatus(AppointmentRequest appointment, AppointmentStatus expected,
            String message) {
        if (appointment.getStatus() != expected) {
            throw new AppointmentConflictException(message);
        }
    }
    private void validateGuest(GuestAppointmentRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (blank(request.phoneNumber()) && blank(request.contactEmail())) {
            String message = "Podaj numer telefonu lub adres e-mail.";
            errors.put("phoneNumber", message);
            errors.put("contactEmail", message);
        }
        int latestAllowedYear = Year.now().getValue() + 1;
        if (request.vehicleProductionYear() > latestAllowedYear) {
            errors.put("vehicleProductionYear",
                "Rok produkcji nie może być późniejszy niż " + latestAllowedYear + ".");
        }
        String registrationNumber = request.vehicleRegistrationNumber().replaceAll("\\s+", "");
        if (registrationNumber.length() < 2) {
            errors.put("vehicleRegistrationNumber",
                "Numer rejestracyjny musi mieć co najmniej 2 znaki.");
        }
        validateNormalizedDescription(errors, request.problemDescription());
        if (!errors.isEmpty()) {
            throw new AppointmentValidationException(errors);
        }
    }
    private String normalizedDescription(String value) {
        String normalized = value.strip();
        if (normalized.length() < MINIMUM_PROBLEM_DESCRIPTION_LENGTH) {
            throw new AppointmentValidationException(Map.of("problemDescription",
                "Opis problemu musi mieć co najmniej 10 znaków."));
        }
        return normalized;
    }
    private String normalizedRepairDescription(String value) {
        String normalized = value.strip();
        if (normalized.length() < MINIMUM_PROBLEM_DESCRIPTION_LENGTH) {
            throw new AppointmentValidationException(Map.of("repairDescription",
                "Opis wykonanych prac musi mieć co najmniej 10 znaków."));
        }
        return normalized;
    }
    private BigDecimal normalizedMoney(BigDecimal value) {
        return value.setScale(2, RoundingMode.UNNECESSARY);
    }
    private void validateNormalizedDescription(Map<String, String> errors, String value) {
        if (value != null && value.strip().length() < MINIMUM_PROBLEM_DESCRIPTION_LENGTH) {
            errors.put("problemDescription", "Opis problemu musi mieć co najmniej 10 znaków.");
        }
    }
    private String normalizedRegistration(String value) {
        return value.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
    }
    private AppUser user(String username) {
        return users.findByUsernameIgnoreCase(username)
            .orElseThrow(() -> new ResourceNotFoundException("Nie znaleziono użytkownika."));
    }
    private AppointmentConflictException unavailableDay() {
        return new AppointmentConflictException("visitDate",
            "Ten dzień nie ma już wolnych miejsc. Wybierz inny dzień.");
    }
    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
    private String optional(String value) {
        return blank(value) ? null : value.strip();
    }
    private String optionalLowercase(String value) {
        String normalized = optional(value);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }
    private String optionalUppercase(String value) {
        String normalized = optional(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }
    private AppointmentResponse response(AppointmentRequest appointment) {
        return new AppointmentResponse(
            appointment.getId(), appointment.getReference(), appointment.getRequesterType(),
            appointment.getStatus(), appointment.getVehicle() == null ? null : appointment.getVehicle().getId(),
            appointment.getVehicleMake(), appointment.getVehicleModel(),
            appointment.getVehicleProductionYear(), appointment.getVehicleRegistrationNumber(),
            text(appointment.getVehicleVin()), appointment.getFirstName(), appointment.getLastName(),
            text(appointment.getPhoneNumber()), text(appointment.getContactEmail()),
            offset(appointment.getRequestedStartAt()), offset(appointment.getCurrentStartAt()),
            appointment.getProblemDescription(), text(appointment.getStaffMessage()),
            offset(appointment.getCreatedAt()), offset(appointment.getStaffActionAt()),
            appointment.getStaffActionBy() == null ? null : appointment.getStaffActionBy().getUsername(),
            offset(appointment.getClientConfirmedAt()), text(appointment.getRepairDescription()),
            appointment.getTotalGrossAmount(), offset(appointment.getRepairCompletedAt()),
            appointment.getRepairCompletedBy() == null ? null : appointment.getRepairCompletedBy().getUsername(),
            offset(appointment.getVehiclePickedUpAt()),
            appointment.getVehiclePickedUpBy() == null ? null : appointment.getVehiclePickedUpBy().getUsername());
    }
    private OffsetDateTime offset(Instant value) {
        return value == null ? null : value.atZone(AppointmentSchedule.TIME_ZONE).toOffsetDateTime();
    }
    private String text(String value) {
        return value == null ? "" : value;
    }
}
