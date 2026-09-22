package pl.autoserwis.appointment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.autoserwis.appointment.dto.AppointmentPageResponse;
import pl.autoserwis.appointment.dto.AppointmentResponse;
import pl.autoserwis.exception.ResourceNotFoundException;
import pl.autoserwis.vehicle.dto.RepairHistoryEntryResponse;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class AppointmentQueryService {
    private static final int DEFAULT_PAGE_SIZE = 5;
    private static final int MAXIMUM_PAGE_SIZE = 50;

    private final AppointmentRepository appointments;
    private final AppointmentResponseMapper responses;
    private final RepairHistoryMapper repairHistory;

    public AppointmentQueryService(AppointmentRepository appointments,
            AppointmentResponseMapper responses, RepairHistoryMapper repairHistory) {
        this.appointments = appointments;
        this.responses = responses;
        this.repairHistory = repairHistory;
    }

    public AppointmentPageResponse getCurrentClientAppointments(Long userId,
            AppointmentStatus status, int page, int size, String direction) {
        PageRequest pageable = pageRequest(page, size, direction);
        Page<AppointmentRequest> result = status == null
            ? appointments.findByClient_Id(userId, pageable)
            : appointments.findByClient_IdAndStatus(userId, status, pageable);
        return pageResponse(result);
    }

    public AppointmentPageResponse getStaffAppointments(AppointmentStatus status,
            int page, int size, String direction) {
        PageRequest pageable = pageRequest(page, size, direction);
        Page<AppointmentRequest> result = status == null
            ? appointments.findAll(pageable)
            : appointments.findByStatus(status, pageable);
        return pageResponse(result);
    }

    public AppointmentResponse getStaffAppointment(Long appointmentId) {
        return responses.toResponse(appointments.findById(appointmentId)
            .orElseThrow(() -> new ResourceNotFoundException("Appointment request not found.")));
    }

    public List<RepairHistoryEntryResponse> getStaffAppointmentRepairHistory(Long appointmentId) {
        AppointmentRequest appointment = appointments.findById(appointmentId)
            .orElseThrow(() -> new ResourceNotFoundException("Appointment request not found."));
        if (appointment.getVehicle() == null) {
            return List.of();
        }
        return appointments.findByVehicle_IdAndStatusOrderByVehiclePickedUpAtDesc(
                appointment.getVehicle().getId(), AppointmentStatus.COMPLETED).stream()
            .map(repairHistory::toResponse)
            .toList();
    }

    private PageRequest pageRequest(int page, int size, String direction) {
        int pageNumber = Math.max(page, 0);
        int pageSize = size < 1 ? DEFAULT_PAGE_SIZE : Math.min(size, MAXIMUM_PAGE_SIZE);
        Sort.Direction sortDirection = "ASC".equalsIgnoreCase(direction)
            ? Sort.Direction.ASC
            : Sort.Direction.DESC;
        return PageRequest.of(pageNumber, pageSize,
            Sort.by(sortDirection, "currentStartAt").and(Sort.by(Sort.Direction.ASC, "id")));
    }

    private AppointmentPageResponse pageResponse(Page<AppointmentRequest> page) {
        return new AppointmentPageResponse(
            page.getContent().stream().map(responses::toResponse).toList(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages());
    }
}
