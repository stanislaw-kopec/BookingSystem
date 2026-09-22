package pl.autoserwis.appointment.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.autoserwis.appointment.api.dto.AppointmentResponse;
import pl.autoserwis.appointment.domain.AppointmentRequest;
import pl.autoserwis.appointment.persistence.AppointmentRepository;
import pl.autoserwis.appointment.schedule.ScheduleLocks;
import pl.autoserwis.exception.ResourceNotFoundException;

import java.time.Clock;
import java.time.Instant;

@Service
@Transactional(readOnly = true)
public class ClientAppointmentService {
    private final AppointmentRepository appointments;
    private final ScheduleLocks locks;
    private final AppointmentResponseMapper responses;
    private final Clock clock;

    public ClientAppointmentService(AppointmentRepository appointments, ScheduleLocks locks,
            AppointmentResponseMapper responses, Clock workshopClock) {
        this.appointments = appointments;
        this.locks = locks;
        this.responses = responses;
        this.clock = workshopClock;
    }

    @Transactional
    public AppointmentResponse confirmProposedTime(Long userId, Long appointmentId) {
        locks.forBooking();
        AppointmentRequest appointment = appointmentForClientUpdate(userId, appointmentId);
        appointment.confirmProposedTime(Instant.now(clock));
        return responses.toResponse(appointments.save(appointment));
    }

    @Transactional
    public AppointmentResponse cancel(Long userId, Long appointmentId) {
        locks.forBooking();
        AppointmentRequest appointment = appointmentForClientUpdate(userId, appointmentId);
        appointment.cancel(Instant.now(clock));
        return responses.toResponse(appointments.save(appointment));
    }

    private AppointmentRequest appointmentForClientUpdate(Long userId, Long appointmentId) {
        return appointments.findByIdAndClientIdForUpdate(appointmentId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Appointment request not found."));
    }
}
