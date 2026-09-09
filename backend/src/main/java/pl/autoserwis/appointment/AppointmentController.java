package pl.autoserwis.appointment;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import pl.autoserwis.appointment.dto.*;

import java.util.List;

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
    public List<AppointmentResponse> getCurrentClientAppointments(Authentication authentication) {
        return appointmentService.getCurrentClientAppointments(authentication.getName());
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
