package pl.autoserwis.appointment;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.autoserwis.appointment.dto.AppointmentResponse;
import pl.autoserwis.appointment.dto.CompleteRepairRequest;
import pl.autoserwis.exception.ResourceNotFoundException;
import pl.autoserwis.invoice.InvoiceFile;
import pl.autoserwis.invoice.InvoiceService;
import pl.autoserwis.user.AppUser;
import pl.autoserwis.user.UserRepository;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class RepairWorkflowService {
    private static final int MINIMUM_REPAIR_DESCRIPTION_LENGTH = 10;

    private final AppointmentRepository appointments;
    private final ScheduleLocks locks;
    private final UserRepository users;
    private final InvoiceService invoices;
    private final AppointmentResponseMapper responses;
    private final RepairItemValidator repairItemValidator;
    private final Clock clock;

    public RepairWorkflowService(AppointmentRepository appointments, ScheduleLocks locks,
            UserRepository users, InvoiceService invoices, AppointmentResponseMapper responses,
            RepairItemValidator repairItemValidator, Clock workshopClock) {
        this.appointments = appointments;
        this.locks = locks;
        this.users = users;
        this.invoices = invoices;
        this.responses = responses;
        this.repairItemValidator = repairItemValidator;
        this.clock = workshopClock;
    }

    @Transactional
    public AppointmentResponse completeRepair(Long staffId, Long appointmentId,
            CompleteRepairRequest request) {
        locks.forBooking();
        AppointmentRequest appointment = appointmentForUpdate(appointmentId);
        appointment.requireCanCompleteRepair();
        appointment.completeRepair(user(staffId),
            normalizedRepairDescription(request.repairDescription()),
            repairItemValidator.normalize(request.repairItems()),
            Instant.now(clock));
        return responses.toResponse(appointments.save(appointment));
    }

    @Transactional
    public AppointmentResponse markPickedUp(Long staffId, Long appointmentId) {
        locks.forBooking();
        AppointmentRequest appointment = appointmentForUpdate(appointmentId);
        appointment.markPickedUp(user(staffId), Instant.now(clock));
        if (appointment.getRequesterType() == AppointmentRequesterType.CLIENT) {
            invoices.documentFor(appointment.getId());
        }
        return responses.toResponse(appointments.save(appointment));
    }

    @Transactional
    public InvoiceFile getStaffRepairInvoice(Long appointmentId) {
        AppointmentRequest appointment = appointments.findById(appointmentId)
            .filter(request -> request.getStatus() == AppointmentStatus.COMPLETED)
            .orElseThrow(() -> new ResourceNotFoundException("Completed repair not found."));
        if (appointment.getRequesterType() != AppointmentRequesterType.CLIENT
                || appointment.getClient() == null
                || appointment.getVehicle() == null) {
            throw new ResourceNotFoundException(
                "Invoice is available only for a completed repair of a registered client.");
        }
        return invoices.documentFor(appointment.getId());
    }

    private AppointmentRequest appointmentForUpdate(Long appointmentId) {
        return appointments.findByIdForUpdate(appointmentId)
            .orElseThrow(() -> new ResourceNotFoundException("Appointment request not found."));
    }

    private AppUser user(Long userId) {
        return users.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found."));
    }

    private String normalizedRepairDescription(String value) {
        String normalized = value.strip();
        if (normalized.length() < MINIMUM_REPAIR_DESCRIPTION_LENGTH) {
            throw new AppointmentValidationException(Map.of("repairDescription",
                "Completed work description must have at least 10 characters."));
        }
        return normalized;
    }
}
