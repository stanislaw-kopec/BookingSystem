package pl.autoserwis.appointment;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.autoserwis.appointment.dto.*;
import pl.autoserwis.exception.ApiErrorCode;
import pl.autoserwis.exception.ResourceNotFoundException;
import pl.autoserwis.invoice.InvoiceFile;
import pl.autoserwis.invoice.InvoiceService;
import pl.autoserwis.profile.ClientProfile;
import pl.autoserwis.profile.ClientProfileRepository;
import pl.autoserwis.user.AppUser;
import pl.autoserwis.user.UserRepository;
import pl.autoserwis.vehicle.Vehicle;
import pl.autoserwis.vehicle.VehicleRepository;
import pl.autoserwis.vehicle.dto.RepairHistoryEntryResponse;
import java.time.Clock;
import java.time.Instant;
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
    private static final int DEFAULT_PAGE_SIZE = 5;
    private static final int MAXIMUM_PAGE_SIZE = 50;
    private final AppointmentRepository appointments;
    private final AppointmentSchedule schedule;
    private final ScheduleLocks locks;
    private final UserRepository users;
    private final ClientProfileRepository profiles;
    private final VehicleRepository vehicles;
    private final InvoiceService invoices;
    private final AppointmentResponseMapper responses;
    private final RepairHistoryMapper repairHistory;
    private final RepairItemValidator repairItemValidator;
    private final Clock clock;
    public AppointmentService(AppointmentRepository appointments, AppointmentSchedule schedule,
            UserRepository users, ClientProfileRepository profiles, VehicleRepository vehicles,
            InvoiceService invoices, ScheduleLocks locks, AppointmentResponseMapper responses,
            RepairHistoryMapper repairHistory, RepairItemValidator repairItemValidator,
            Clock workshopClock) {
        this.appointments = appointments;
        this.schedule = schedule;
        this.locks = locks;
        this.users = users;
        this.profiles = profiles;
        this.vehicles = vehicles;
        this.invoices = invoices;
        this.responses = responses;
        this.repairHistory = repairHistory;
        this.repairItemValidator = repairItemValidator;
        this.clock = workshopClock;
    }
    public AppointmentAvailabilityResponse getAvailability() {
        return schedule.availability();
    }
    public List<AppointmentResponse> getCurrentClientAppointments(Long userId) {
        AppUser client = user(userId);
        return appointments.findByClient_IdOrderByCreatedAtDesc(client.getId()).stream()
            .map(responses::toResponse)
            .toList();
    }
    public AppointmentPageResponse getCurrentClientAppointments(Long userId,
            AppointmentStatus status, int page, int size, String direction) {
        AppUser client = user(userId);
        int pageNumber = Math.max(page, 0);
        int pageSize = normalizedPageSize(size);
        Sort.Direction sortDirection = "ASC".equalsIgnoreCase(direction)
            ? Sort.Direction.ASC
            : Sort.Direction.DESC;
        PageRequest pageable = PageRequest.of(pageNumber, pageSize,
            Sort.by(sortDirection, "currentStartAt").and(Sort.by(Sort.Direction.ASC, "id")));
        Page<AppointmentRequest> result = status == null
            ? appointments.findByClient_Id(client.getId(), pageable)
            : appointments.findByClient_IdAndStatus(client.getId(), status, pageable);
        return new AppointmentPageResponse(
            result.getContent().stream().map(responses::toResponse).toList(),
            result.getNumber(),
            result.getSize(),
            result.getTotalElements(),
            result.getTotalPages());
    }
    public AppointmentPageResponse getStaffAppointments(AppointmentStatus status,
            int page, int size, String direction) {
        int pageNumber = Math.max(page, 0);
        int pageSize = normalizedPageSize(size);
        Sort.Direction sortDirection = "ASC".equalsIgnoreCase(direction)
            ? Sort.Direction.ASC
            : Sort.Direction.DESC;
        PageRequest pageable = PageRequest.of(pageNumber, pageSize,
            Sort.by(sortDirection, "currentStartAt").and(Sort.by(Sort.Direction.ASC, "id")));
        Page<AppointmentRequest> result = status == null
            ? appointments.findAll(pageable)
            : appointments.findByStatus(status, pageable);
        return new AppointmentPageResponse(
            result.getContent().stream().map(responses::toResponse).toList(),
            result.getNumber(),
            result.getSize(),
            result.getTotalElements(),
            result.getTotalPages());
    }
    public AppointmentResponse getStaffAppointment(Long appointmentId) {
        return responses.toResponse(appointments.findById(appointmentId)
            .orElseThrow(() -> new ResourceNotFoundException("Appointment request not found.")));
    }
    public List<RepairHistoryEntryResponse> getStaffAppointmentRepairHistory(Long appointmentId) {
        AppointmentRequest appointment = appointments.findById(appointmentId)
            .orElseThrow(() -> new ResourceNotFoundException("Appointment request not found."));
        if (appointment.getVehicle() == null) return List.of();
        return appointments.findByVehicle_IdAndStatusOrderByVehiclePickedUpAtDesc(
                appointment.getVehicle().getId(), AppointmentStatus.COMPLETED).stream()
            .map(repairHistory::toResponse)
            .toList();
    }
    @Transactional
    public InvoiceFile getStaffRepairInvoice(Long appointmentId) {
        AppointmentRequest appointment = appointments.findById(appointmentId)
            .filter(request -> request.getStatus() == AppointmentStatus.COMPLETED)
            .orElseThrow(() -> new ResourceNotFoundException("Completed repair not found."));
        if (appointment.getRequesterType() != AppointmentRequesterType.CLIENT
                || appointment.getClient() == null
                || appointment.getVehicle() == null) {
            throw new ResourceNotFoundException("Invoice is available only for a completed repair of a registered client.");
        }
        return invoices.documentFor(appointment.getId());
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
        String description = normalizedDescription(request.problemDescription());
        AppointmentRequest appointment = new AppointmentRequest(UUID.randomUUID(),
            AppointmentRequesterType.CLIENT, client, vehicle,
            profile.getFirstName(), profile.getLastName(), profile.getPhoneNumber(),
            profile.getContactEmail(), vehicle.getMake(), vehicle.getModel(),
            vehicle.getProductionYear(), vehicle.getRegistrationNumber(), optional(vehicle.getVin()),
            startAt, description, Instant.now(clock));
        return saveInAvailableDay(appointment);
    }
    @Transactional
    public AppointmentResponse createForGuest(GuestAppointmentRequest request) {
        locks.forBooking();
        validateGuest(request);
        Instant startAt = schedule.validateAndNormalize(request.visitDate());
        String registrationNumber = normalizedRegistration(request.vehicleRegistrationNumber());
        String description = normalizedDescription(request.problemDescription());
        AppointmentRequest appointment = new AppointmentRequest(UUID.randomUUID(),
            AppointmentRequesterType.GUEST, null, null,
            request.firstName().strip(), request.lastName().strip(), optional(request.phoneNumber()),
            optionalLowercase(request.contactEmail()), request.vehicleMake().strip(),
            request.vehicleModel().strip(), request.vehicleProductionYear(), registrationNumber,
            optionalUppercase(request.vehicleVin()), startAt, description, Instant.now(clock));
        return saveInAvailableDay(appointment);
    }
    @Transactional
    public AppointmentResponse accept(Long staffId, Long appointmentId) {
        locks.forBooking();
        AppointmentRequest appointment = appointmentForStaffUpdate(appointmentId);
        requireStatus(appointment, AppointmentStatus.PENDING,
            "Only a pending appointment request can be accepted.");
        appointment.accept(user(staffId), Instant.now(clock));
        return responses.toResponse(appointments.save(appointment));
    }
    @Transactional
    public AppointmentResponse reject(Long staffId, Long appointmentId,
            StaffMessageRequest request) {
        locks.forBooking();
        AppointmentRequest appointment = appointmentForStaffUpdate(appointmentId);
        if (appointment.getStatus() != AppointmentStatus.PENDING
                && appointment.getStatus() != AppointmentStatus.TIME_PROPOSED) {
            throw new AppointmentConflictException(
                "Only a pending request or a day proposal can be rejected.");
        }
        appointment.reject(user(staffId), optional(request.message()), Instant.now(clock));
        return responses.toResponse(appointments.save(appointment));
    }
    @Transactional
    public AppointmentResponse proposeTime(Long staffId, Long appointmentId,
            ProposeAppointmentTimeRequest request) {
        locks.forBooking();
        AppointmentRequest appointment = appointmentForStaffUpdate(appointmentId);
        if (appointment.getStatus() != AppointmentStatus.PENDING
                && appointment.getStatus() != AppointmentStatus.TIME_PROPOSED) {
            throw new AppointmentConflictException(
                "A new day can be proposed only for a pending request.");
        }
        Instant proposedStartAt = schedule.validateAndNormalize(request.visitDate());
        if (proposedStartAt.equals(appointment.getCurrentStartAt())) {
            throw new AppointmentValidationException(Map.of("visitDate",
                "Propose a day different from the current one."));
        }
        appointments.lockAppointmentDay(schedule.visitDate(proposedStartAt));
        if (schedule.isFull(proposedStartAt)) {
            throw unavailableDay();
        }
        appointment.proposeTime(user(staffId), proposedStartAt,
            optional(request.message()), Instant.now(clock));
        try {
            return responses.toResponse(appointments.saveAndFlush(appointment));
        } catch (DataIntegrityViolationException exception) {
            throw unavailableDay();
        }
    }
    @Transactional
    public AppointmentResponse confirmProposedTime(Long userId, Long appointmentId) {
        locks.forBooking();
        AppUser client = user(userId);
        AppointmentRequest appointment = appointments.findByIdAndClientIdForUpdate(
                appointmentId, client.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Appointment request not found."));
        requireStatus(appointment, AppointmentStatus.TIME_PROPOSED,
            "This request is not waiting for a proposed day confirmation.");
        appointment.confirmProposedTime(Instant.now(clock));
        return responses.toResponse(appointments.save(appointment));
    }
    @Transactional
    public AppointmentResponse cancelClientAppointment(Long userId, Long appointmentId) {
        locks.forBooking();
        AppUser client = user(userId);
        AppointmentRequest appointment = appointments.findByIdAndClientIdForUpdate(
                appointmentId, client.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Appointment request not found."));
        if (appointment.getStatus() != AppointmentStatus.PENDING
                && appointment.getStatus() != AppointmentStatus.TIME_PROPOSED
                && appointment.getStatus() != AppointmentStatus.CONFIRMED) {
            throw new AppointmentConflictException(
                "Only an active appointment can be cancelled.");
        }
        appointment.cancel(Instant.now(clock));
        return responses.toResponse(appointments.save(appointment));
    }
    @Transactional
    public AppointmentResponse confirmGuestProposedTime(Long staffId, Long appointmentId) {
        locks.forBooking();
        AppointmentRequest appointment = appointmentForStaffUpdate(appointmentId);
        if (appointment.getRequesterType() != AppointmentRequesterType.GUEST) {
            throw new AppointmentConflictException(
                "A registered client confirms the proposed day personally.");
        }
        requireStatus(appointment, AppointmentStatus.TIME_PROPOSED,
            "This request is not waiting for a proposed day confirmation.");
        appointment.confirmGuestProposedTime(user(staffId), Instant.now(clock));
        return responses.toResponse(appointments.save(appointment));
    }
    @Transactional
    public AppointmentResponse completeRepair(Long staffId, Long appointmentId,
            CompleteRepairRequest request) {
        locks.forBooking();
        AppointmentRequest appointment = appointmentForStaffUpdate(appointmentId);
        requireStatus(appointment, AppointmentStatus.CONFIRMED,
            "Repair can be completed only for a confirmed appointment.");
        appointment.completeRepair(user(staffId),
            normalizedRepairDescription(request.repairDescription()),
            repairItemValidator.normalize(request.repairItems()),
            Instant.now(clock));
        return responses.toResponse(appointments.save(appointment));
    }
    @Transactional
    public AppointmentResponse markPickedUp(Long staffId, Long appointmentId) {
        locks.forBooking();
        AppointmentRequest appointment = appointmentForStaffUpdate(appointmentId);
        requireStatus(appointment, AppointmentStatus.READY_FOR_PICKUP,
            "Vehicle pickup can be confirmed only for a vehicle ready for pickup.");
        appointment.markPickedUp(user(staffId), Instant.now(clock));
        if (appointment.getRequesterType() == AppointmentRequesterType.CLIENT) {
            invoices.documentFor(appointment.getId());
        }
        return responses.toResponse(appointments.save(appointment));
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
    private AppointmentRequest appointmentForStaffUpdate(Long appointmentId) {
        return appointments.findByIdForUpdate(appointmentId)
            .orElseThrow(() -> new ResourceNotFoundException("Appointment request not found."));
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
            String message = "Enter a phone number or an email address.";
            errors.put("phoneNumber", message);
            errors.put("contactEmail", message);
        }
        int latestAllowedYear = Year.now(clock).getValue() + 1;
        if (request.vehicleProductionYear() > latestAllowedYear) {
            errors.put("vehicleProductionYear",
                "Production year cannot be later than " + latestAllowedYear + ".");
        }
        String registrationNumber = request.vehicleRegistrationNumber().replaceAll("\\s+", "");
        if (registrationNumber.length() < 2) {
            errors.put("vehicleRegistrationNumber",
                "Registration number must have at least 2 characters.");
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
                "Problem description must have at least 10 characters."));
        }
        return normalized;
    }
    private String normalizedRepairDescription(String value) {
        String normalized = value.strip();
        if (normalized.length() < MINIMUM_PROBLEM_DESCRIPTION_LENGTH) {
            throw new AppointmentValidationException(Map.of("repairDescription",
                "Completed work description must have at least 10 characters."));
        }
        return normalized;
    }
    private int normalizedPageSize(int size) {
        if (size < 1) return DEFAULT_PAGE_SIZE;
        return Math.min(size, MAXIMUM_PAGE_SIZE);
    }
    private void validateNormalizedDescription(Map<String, String> errors, String value) {
        if (value != null && value.strip().length() < MINIMUM_PROBLEM_DESCRIPTION_LENGTH) {
            errors.put("problemDescription", "Problem description must have at least 10 characters.");
        }
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
