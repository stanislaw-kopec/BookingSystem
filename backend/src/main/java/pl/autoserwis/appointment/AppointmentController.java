package pl.autoserwis.appointment;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
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
    public AppointmentPageResponse getCurrentClientAppointments(Authentication authentication,
            @RequestParam(required = false) AppointmentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(defaultValue = "DESC") String sortDirection) {
        return appointmentService.getCurrentClientAppointments(authentication.getName(),
            status, page, size, sortDirection);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AppointmentResponse createForClient(Authentication authentication,
            @Valid @RequestBody ClientAppointmentRequest request) {
        return appointmentService.createForClient(authentication.getName(), request);
    }

    @PostMapping("/guest")
    @ResponseStatus(HttpStatus.CREATED)
    public AppointmentResponse createForGuest(
            @Valid @RequestBody GuestAppointmentRequest request) {
        return appointmentService.createForGuest(request);
    }

    @PostMapping("/{appointmentId}/confirm-proposed")
    public AppointmentResponse confirmProposedTime(Authentication authentication,
            @PathVariable Long appointmentId) {
        return appointmentService.confirmProposedTime(authentication.getName(), appointmentId);
    }

    @PostMapping("/{appointmentId}/cancel")
    public AppointmentResponse cancelClientAppointment(Authentication authentication,
            @PathVariable Long appointmentId) {
        return appointmentService.cancelClientAppointment(authentication.getName(), appointmentId);
    }
}
