package pl.autoserwis.appointment;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.autoserwis.appointment.dto.AppointmentAvailabilityResponse;
import pl.autoserwis.appointment.dto.AppointmentResponse;
import pl.autoserwis.appointment.dto.ClientAppointmentRequest;
import pl.autoserwis.appointment.dto.GuestAppointmentRequest;
import pl.autoserwis.exception.ApiErrorCode;
import pl.autoserwis.exception.ResourceNotFoundException;
import pl.autoserwis.profile.ClientProfile;
import pl.autoserwis.profile.ClientProfileRepository;
import pl.autoserwis.user.AppUser;
import pl.autoserwis.user.UserRepository;
import pl.autoserwis.vehicle.Vehicle;
import pl.autoserwis.vehicle.VehicleRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.Year;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class AppointmentBookingService {
    private static final int MINIMUM_PROBLEM_DESCRIPTION_LENGTH = 10;

    private final AppointmentRepository appointments;
    private final AppointmentSchedule schedule;
    private final ScheduleLocks locks;
    private final UserRepository users;
    private final ClientProfileRepository profiles;
    private final VehicleRepository vehicles;
    private final AppointmentResponseMapper responses;
    private final Clock clock;

    public AppointmentBookingService(AppointmentRepository appointments, AppointmentSchedule schedule,
            ScheduleLocks locks, UserRepository users, ClientProfileRepository profiles,
            VehicleRepository vehicles, AppointmentResponseMapper responses, Clock workshopClock) {
        this.appointments = appointments;
        this.schedule = schedule;
        this.locks = locks;
        this.users = users;
        this.profiles = profiles;
        this.vehicles = vehicles;
        this.responses = responses;
        this.clock = workshopClock;
    }

    public AppointmentAvailabilityResponse getAvailability() {
        return schedule.availability();
    }

    @Transactional
    public AppointmentResponse createForClient(Long userId, ClientAppointmentRequest request) {
        locks.forBooking();
        AppUser client = user(userId);
        ClientProfile profile = profiles.findByUser_Id(client.getId())
            .orElseThrow(() -> new AppointmentConflictException("profile",
                "Complete your profile before booking an appointment."));
        Vehicle vehicle = vehicles.findByIdAndOwner_Id(request.vehicleId(), client.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found."));
        Instant startAt = schedule.validateAndNormalize(request.visitDate());
        AppointmentRequest appointment = new AppointmentRequest(UUID.randomUUID(),
            AppointmentRequesterType.CLIENT, client, vehicle,
            profile.getFirstName(), profile.getLastName(), profile.getPhoneNumber(),
            profile.getContactEmail(), vehicle.getMake(), vehicle.getModel(),
            vehicle.getProductionYear(), vehicle.getRegistrationNumber(), optional(vehicle.getVin()),
            startAt, normalizedDescription(request.problemDescription()), Instant.now(clock));
        return saveInAvailableDay(appointment);
    }

    @Transactional
    public AppointmentResponse createForGuest(GuestAppointmentRequest request) {
        locks.forBooking();
        validateGuest(request);
        Instant startAt = schedule.validateAndNormalize(request.visitDate());
        AppointmentRequest appointment = new AppointmentRequest(UUID.randomUUID(),
            AppointmentRequesterType.GUEST, null, null,
            request.firstName().strip(), request.lastName().strip(), optional(request.phoneNumber()),
            optionalLowercase(request.contactEmail()), request.vehicleMake().strip(),
            request.vehicleModel().strip(), request.vehicleProductionYear(),
            normalizedRegistration(request.vehicleRegistrationNumber()),
            optionalUppercase(request.vehicleVin()), startAt,
            normalizedDescription(request.problemDescription()), Instant.now(clock));
        return saveInAvailableDay(appointment);
    }

    private AppointmentResponse saveInAvailableDay(AppointmentRequest appointment) {
        appointments.lockAppointmentDay(schedule.visitDate(appointment.getCurrentStartAt()));
        if (schedule.isFull(appointment.getCurrentStartAt())) {
            throw unavailableDay();
        }
        try {
            return responses.toResponse(appointments.saveAndFlush(appointment));
        } catch (DataIntegrityViolationException exception) {
            throw unavailableDay();
        }
    }

    private void validateGuest(GuestAppointmentRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (blank(request.phoneNumber()) && blank(request.contactEmail())) {
            String message = "Enter a phone number or an email address.";
            errors.put("phoneNumber", message);
            errors.put("contactEmail", message);
        }
        int latestAllowedYear = Year.now(clock).getValue() + 1;
        if (request.vehicleProductionYear() > latestAllowedYear) {
            errors.put("vehicleProductionYear",
                "Production year cannot be later than " + latestAllowedYear + ".");
        }
        if (request.vehicleRegistrationNumber().replaceAll("\\s+", "").length() < 2) {
            errors.put("vehicleRegistrationNumber",
                "Registration number must have at least 2 characters.");
        }
        if (request.problemDescription() != null
                && request.problemDescription().strip().length() < MINIMUM_PROBLEM_DESCRIPTION_LENGTH) {
            errors.put("problemDescription", "Problem description must have at least 10 characters.");
        }
        if (!errors.isEmpty()) {
            throw new AppointmentValidationException(errors);
        }
    }

    private String normalizedDescription(String value) {
        String normalized = value.strip();
        if (normalized.length() < MINIMUM_PROBLEM_DESCRIPTION_LENGTH) {
            throw new AppointmentValidationException(Map.of("problemDescription",
                "Problem description must have at least 10 characters."));
        }
        return normalized;
    }

    private String normalizedRegistration(String value) {
        return value.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
    }

    private AppUser user(Long userId) {
        return users.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found."));
    }

    private AppointmentConflictException unavailableDay() {
        return new AppointmentConflictException(ApiErrorCode.APPOINTMENT_DAY_FULL, "visitDate",
            "This day has no available places. Select another day.");
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
}
