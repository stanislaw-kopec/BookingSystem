package pl.autoserwis.appointment;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import pl.autoserwis.appointment.dto.AppointmentPageResponse;
import pl.autoserwis.appointment.dto.AppointmentResponse;
import pl.autoserwis.appointment.dto.CompleteRepairRequest;
import pl.autoserwis.appointment.dto.ProposeAppointmentTimeRequest;
import pl.autoserwis.appointment.dto.StaffMessageRequest;
import java.util.List;
@RestController
@RequestMapping("/api/staff/appointments")
public class StaffAppointmentController {
    private final AppointmentService appointmentService;
    public StaffAppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }
    @GetMapping
    public AppointmentPageResponse getAppointments(
            @RequestParam(required = false) AppointmentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(defaultValue = "DESC") String sortDirection) {
        return appointmentService.getStaffAppointments(status, page, size, sortDirection);
    }
    @GetMapping("/all")
    public List<AppointmentResponse> getAllAppointments() {
        return appointmentService.getStaffAppointments();
    }
    @PostMapping("/{appointmentId}/accept")
    public AppointmentResponse accept(Authentication authentication, @PathVariable Long appointmentId) {
        return appointmentService.accept(authentication.getName(), appointmentId);
    }
    @PostMapping("/{appointmentId}/reject")
    public AppointmentResponse reject(Authentication authentication, @PathVariable Long appointmentId,
            @Valid @RequestBody StaffMessageRequest request) {
        return appointmentService.reject(authentication.getName(), appointmentId, request);
    }
    @PostMapping("/{appointmentId}/propose-time")
    public AppointmentResponse proposeTime(Authentication authentication,
            @PathVariable Long appointmentId,
            @Valid @RequestBody ProposeAppointmentTimeRequest request) {
        return appointmentService.proposeTime(authentication.getName(), appointmentId, request);
    }
    @PostMapping("/{appointmentId}/confirm-proposed")
    public AppointmentResponse confirmGuestProposedTime(Authentication authentication,
            @PathVariable Long appointmentId) {
        return appointmentService.confirmGuestProposedTime(authentication.getName(), appointmentId);
    }
    @PostMapping("/{appointmentId}/complete-repair")
    public AppointmentResponse completeRepair(Authentication authentication,
            @PathVariable Long appointmentId,
            @Valid @RequestBody CompleteRepairRequest request) {
        return appointmentService.completeRepair(authentication.getName(), appointmentId, request);
    }
    @PostMapping("/{appointmentId}/mark-picked-up")
    public AppointmentResponse markPickedUp(Authentication authentication, @PathVariable Long appointmentId) {
        return appointmentService.markPickedUp(authentication.getName(), appointmentId);
    }
}
