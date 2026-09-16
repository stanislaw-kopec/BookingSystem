package pl.autoserwis.appointment;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import pl.autoserwis.security.AccountPrincipal;
import org.springframework.web.bind.annotation.*;
import pl.autoserwis.appointment.dto.*;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {
    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @GetMapping("/availability")
    public AppointmentAvailabilityResponse getAvailability() {
        return appointmentService.getAvailability();
    }

    @GetMapping
    public AppointmentPageResponse getCurrentClientAppointments(@AuthenticationPrincipal AccountPrincipal principal,
            @RequestParam(required = false) AppointmentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(defaultValue = "DESC") String sortDirection) {
        return appointmentService.getCurrentClientAppointments(principal.getUserId(),
            status, page, size, sortDirection);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AppointmentResponse createForClient(@AuthenticationPrincipal AccountPrincipal principal,
            @Valid @RequestBody ClientAppointmentRequest request) {
        return appointmentService.createForClient(principal.getUserId(), request);
    }

    @PostMapping("/guest")
    @ResponseStatus(HttpStatus.CREATED)
    public AppointmentResponse createForGuest(
            @Valid @RequestBody GuestAppointmentRequest request) {
        return appointmentService.createForGuest(request);
    }

    @PostMapping("/{appointmentId}/confirm-proposed")
    public AppointmentResponse confirmProposedTime(@AuthenticationPrincipal AccountPrincipal principal,
            @PathVariable Long appointmentId) {
        return appointmentService.confirmProposedTime(principal.getUserId(), appointmentId);
    }

    @PostMapping("/{appointmentId}/cancel")
    public AppointmentResponse cancelClientAppointment(@AuthenticationPrincipal AccountPrincipal principal,
            @PathVariable Long appointmentId) {
        return appointmentService.cancelClientAppointment(principal.getUserId(), appointmentId);
    }
}
