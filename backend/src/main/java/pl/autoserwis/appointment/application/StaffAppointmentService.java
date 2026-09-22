package pl.autoserwis.appointment.application;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.autoserwis.appointment.api.dto.AppointmentResponse;
import pl.autoserwis.appointment.api.dto.ProposeAppointmentTimeRequest;
import pl.autoserwis.appointment.api.dto.StaffMessageRequest;
import pl.autoserwis.appointment.domain.AppointmentConflictException;
import pl.autoserwis.appointment.domain.AppointmentRequest;
import pl.autoserwis.appointment.domain.AppointmentValidationException;
import pl.autoserwis.appointment.persistence.AppointmentRepository;
import pl.autoserwis.appointment.schedule.AppointmentSchedule;
import pl.autoserwis.appointment.schedule.ScheduleLocks;
import pl.autoserwis.exception.ApiErrorCode;
import pl.autoserwis.exception.ResourceNotFoundException;
import pl.autoserwis.user.AppUser;
import pl.autoserwis.user.UserRepository;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class StaffAppointmentService {
    private final AppointmentRepository appointments;
    private final AppointmentSchedule schedule;
    private final ScheduleLocks locks;
    private final UserRepository users;
    private final AppointmentResponseMapper responses;
    private final Clock clock;

    public StaffAppointmentService(AppointmentRepository appointments, AppointmentSchedule schedule,
            ScheduleLocks locks, UserRepository users, AppointmentResponseMapper responses,
            Clock workshopClock) {
        this.appointments = appointments;
        this.schedule = schedule;
        this.locks = locks;
        this.users = users;
        this.responses = responses;
        this.clock = workshopClock;
    }

    @Transactional
    public AppointmentResponse accept(Long staffId, Long appointmentId) {
        locks.forBooking();
        AppointmentRequest appointment = appointmentForUpdate(appointmentId);
        appointment.accept(user(staffId), Instant.now(clock));
        return responses.toResponse(appointments.save(appointment));
    }

    @Transactional
    public AppointmentResponse reject(Long staffId, Long appointmentId, StaffMessageRequest request) {
        locks.forBooking();
        AppointmentRequest appointment = appointmentForUpdate(appointmentId);
        appointment.reject(user(staffId), optional(request.message()), Instant.now(clock));
        return responses.toResponse(appointments.save(appointment));
    }

    @Transactional
    public AppointmentResponse proposeTime(Long staffId, Long appointmentId,
            ProposeAppointmentTimeRequest request) {
        locks.forBooking();
        AppointmentRequest appointment = appointmentForUpdate(appointmentId);
        appointment.requireCanProposeTime();
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
    public AppointmentResponse confirmGuestProposedTime(Long staffId, Long appointmentId) {
        locks.forBooking();
        AppointmentRequest appointment = appointmentForUpdate(appointmentId);
        appointment.confirmGuestProposedTime(user(staffId), Instant.now(clock));
        return responses.toResponse(appointments.save(appointment));
    }

    private AppointmentRequest appointmentForUpdate(Long appointmentId) {
        return appointments.findByIdForUpdate(appointmentId)
            .orElseThrow(() -> new ResourceNotFoundException("Appointment request not found."));
    }

    private AppUser user(Long userId) {
        return users.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found."));
    }

    private AppointmentConflictException unavailableDay() {
        return new AppointmentConflictException(ApiErrorCode.APPOINTMENT_DAY_FULL, "visitDate",
            "This day has no available places. Select another day.");
    }

    private String optional(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
