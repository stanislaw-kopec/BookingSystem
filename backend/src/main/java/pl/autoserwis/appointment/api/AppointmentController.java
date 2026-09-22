package pl.autoserwis.appointment.api;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pl.autoserwis.appointment.api.dto.AppointmentPageResponse;
import pl.autoserwis.appointment.api.dto.AppointmentResponse;
import pl.autoserwis.appointment.api.dto.ClientAppointmentRequest;
import pl.autoserwis.appointment.api.dto.GuestAppointmentRequest;
import pl.autoserwis.appointment.application.AppointmentBookingService;
import pl.autoserwis.appointment.application.AppointmentQueryService;
import pl.autoserwis.appointment.application.ClientAppointmentService;
import pl.autoserwis.appointment.domain.AppointmentStatus;
import pl.autoserwis.appointment.schedule.dto.AppointmentAvailabilityResponse;
import pl.autoserwis.security.AccountPrincipal;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {
    private final AppointmentBookingService bookingService;
    private final AppointmentQueryService queryService;
    private final ClientAppointmentService clientAppointmentService;

    public AppointmentController(AppointmentBookingService bookingService,
            AppointmentQueryService queryService,
            ClientAppointmentService clientAppointmentService) {
        this.bookingService = bookingService;
        this.queryService = queryService;
        this.clientAppointmentService = clientAppointmentService;
    }

    @GetMapping("/availability")
    public AppointmentAvailabilityResponse getAvailability() {
        return bookingService.getAvailability();
    }

    @GetMapping
    public AppointmentPageResponse getCurrentClientAppointments(@AuthenticationPrincipal AccountPrincipal principal,
            @RequestParam(required = false) AppointmentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(defaultValue = "DESC") String sortDirection) {
        return queryService.getCurrentClientAppointments(
            principal.getUserId(), status, page, size, sortDirection);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AppointmentResponse createForClient(@AuthenticationPrincipal AccountPrincipal principal,
            @Valid @RequestBody ClientAppointmentRequest request) {
        return bookingService.createForClient(principal.getUserId(), request);
    }

    @PostMapping("/guest")
    @ResponseStatus(HttpStatus.CREATED)
    public AppointmentResponse createForGuest(
            @Valid @RequestBody GuestAppointmentRequest request) {
        return bookingService.createForGuest(request);
    }

    @PostMapping("/{appointmentId}/confirm-proposed")
    public AppointmentResponse confirmProposedTime(@AuthenticationPrincipal AccountPrincipal principal,
            @PathVariable Long appointmentId) {
        return clientAppointmentService.confirmProposedTime(principal.getUserId(), appointmentId);
    }

    @PostMapping("/{appointmentId}/cancel")
    public AppointmentResponse cancelClientAppointment(@AuthenticationPrincipal AccountPrincipal principal,
            @PathVariable Long appointmentId) {
        return clientAppointmentService.cancel(principal.getUserId(), appointmentId);
    }
}
