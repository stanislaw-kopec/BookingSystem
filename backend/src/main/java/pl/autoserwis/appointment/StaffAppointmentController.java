package pl.autoserwis.appointment;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import pl.autoserwis.security.AccountPrincipal;
import org.springframework.web.bind.annotation.*;
import pl.autoserwis.appointment.dto.AppointmentPageResponse;
import pl.autoserwis.appointment.dto.AppointmentResponse;
import pl.autoserwis.appointment.dto.CompleteRepairRequest;
import pl.autoserwis.appointment.dto.ProposeAppointmentTimeRequest;
import pl.autoserwis.appointment.dto.StaffMessageRequest;
import pl.autoserwis.invoice.InvoiceFile;
import pl.autoserwis.vehicle.dto.RepairHistoryEntryResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import pl.autoserwis.appointment.dto.StaffScheduleResponse;
@RestController
@RequestMapping("/api/staff/appointments")
public class StaffAppointmentController {
    private final AppointmentService appointmentService;
    private final StaffScheduleService scheduleService;
    public StaffAppointmentController(AppointmentService appointmentService, StaffScheduleService scheduleService) {
        this.appointmentService = appointmentService;
        this.scheduleService = scheduleService;
    }
    @GetMapping
    public AppointmentPageResponse getAppointments(
            @RequestParam(required = false) AppointmentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(defaultValue = "DESC") String sortDirection) {
        return appointmentService.getStaffAppointments(status, page, size, sortDirection);
    }
    @GetMapping("/schedule")
    public StaffScheduleResponse getSchedule(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return scheduleService.schedule(startDate, endDate);
    }
    @GetMapping("/{appointmentId}")
    public AppointmentResponse getAppointment(@PathVariable Long appointmentId) {
        return appointmentService.getStaffAppointment(appointmentId);
    }
    @GetMapping("/{appointmentId}/repair-history")
    public List<RepairHistoryEntryResponse> getRepairHistory(@PathVariable Long appointmentId) {
        return appointmentService.getStaffAppointmentRepairHistory(appointmentId);
    }

    @GetMapping("/{appointmentId}/invoice")
    public ResponseEntity<byte[]> getRepairInvoice(@PathVariable Long appointmentId) {
        InvoiceFile invoice = appointmentService.getStaffRepairInvoice(appointmentId);
        ContentDisposition disposition = ContentDisposition.attachment()
            .filename(invoice.filename(), StandardCharsets.UTF_8)
            .build();
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
            .body(invoice.content());
    }
    @PostMapping("/{appointmentId}/accept")
    public AppointmentResponse accept(@AuthenticationPrincipal AccountPrincipal principal, @PathVariable Long appointmentId) {
        return appointmentService.accept(principal.getUserId(), appointmentId);
    }
    @PostMapping("/{appointmentId}/reject")
    public AppointmentResponse reject(@AuthenticationPrincipal AccountPrincipal principal, @PathVariable Long appointmentId,
            @Valid @RequestBody StaffMessageRequest request) {
        return appointmentService.reject(principal.getUserId(), appointmentId, request);
    }
    @PostMapping("/{appointmentId}/propose-time")
    public AppointmentResponse proposeTime(@AuthenticationPrincipal AccountPrincipal principal,
            @PathVariable Long appointmentId,
            @Valid @RequestBody ProposeAppointmentTimeRequest request) {
        return appointmentService.proposeTime(principal.getUserId(), appointmentId, request);
    }
    @PostMapping("/{appointmentId}/confirm-proposed")
    public AppointmentResponse confirmGuestProposedTime(@AuthenticationPrincipal AccountPrincipal principal,
            @PathVariable Long appointmentId) {
        return appointmentService.confirmGuestProposedTime(principal.getUserId(), appointmentId);
    }
    @PostMapping("/{appointmentId}/complete-repair")
    public AppointmentResponse completeRepair(@AuthenticationPrincipal AccountPrincipal principal,
            @PathVariable Long appointmentId,
            @Valid @RequestBody CompleteRepairRequest request) {
        return appointmentService.completeRepair(principal.getUserId(), appointmentId, request);
    }
    @PostMapping("/{appointmentId}/mark-picked-up")
    public AppointmentResponse markPickedUp(@AuthenticationPrincipal AccountPrincipal principal, @PathVariable Long appointmentId) {
        return appointmentService.markPickedUp(principal.getUserId(), appointmentId);
    }
}
