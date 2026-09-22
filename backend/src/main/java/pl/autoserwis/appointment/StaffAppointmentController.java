package pl.autoserwis.appointment;

import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.autoserwis.appointment.dto.AppointmentPageResponse;
import pl.autoserwis.appointment.dto.AppointmentResponse;
import pl.autoserwis.appointment.dto.CompleteRepairRequest;
import pl.autoserwis.appointment.dto.ProposeAppointmentTimeRequest;
import pl.autoserwis.appointment.dto.StaffMessageRequest;
import pl.autoserwis.appointment.dto.StaffScheduleResponse;
import pl.autoserwis.invoice.InvoiceFile;
import pl.autoserwis.security.AccountPrincipal;
import pl.autoserwis.vehicle.dto.RepairHistoryEntryResponse;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/staff/appointments")
public class StaffAppointmentController {
    private final AppointmentQueryService queryService;
    private final StaffAppointmentService staffAppointmentService;
    private final RepairWorkflowService repairWorkflowService;
    private final StaffScheduleService scheduleService;

    public StaffAppointmentController(AppointmentQueryService queryService,
            StaffAppointmentService staffAppointmentService,
            RepairWorkflowService repairWorkflowService,
            StaffScheduleService scheduleService) {
        this.queryService = queryService;
        this.staffAppointmentService = staffAppointmentService;
        this.repairWorkflowService = repairWorkflowService;
        this.scheduleService = scheduleService;
    }

    @GetMapping
    public AppointmentPageResponse getAppointments(
            @RequestParam(required = false) AppointmentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(defaultValue = "DESC") String sortDirection) {
        return queryService.getStaffAppointments(status, page, size, sortDirection);
    }

    @GetMapping("/schedule")
    public StaffScheduleResponse getSchedule(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return scheduleService.schedule(startDate, endDate);
    }

    @GetMapping("/{appointmentId}")
    public AppointmentResponse getAppointment(@PathVariable Long appointmentId) {
        return queryService.getStaffAppointment(appointmentId);
    }

    @GetMapping("/{appointmentId}/repair-history")
    public List<RepairHistoryEntryResponse> getRepairHistory(@PathVariable Long appointmentId) {
        return queryService.getStaffAppointmentRepairHistory(appointmentId);
    }

    @GetMapping("/{appointmentId}/invoice")
    public ResponseEntity<byte[]> getRepairInvoice(@PathVariable Long appointmentId) {
        InvoiceFile invoice = repairWorkflowService.getStaffRepairInvoice(appointmentId);
        ContentDisposition disposition = ContentDisposition.attachment()
            .filename(invoice.filename(), StandardCharsets.UTF_8)
            .build();
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
            .body(invoice.content());
    }

    @PostMapping("/{appointmentId}/accept")
    public AppointmentResponse accept(@AuthenticationPrincipal AccountPrincipal principal,
            @PathVariable Long appointmentId) {
        return staffAppointmentService.accept(principal.getUserId(), appointmentId);
    }

    @PostMapping("/{appointmentId}/reject")
    public AppointmentResponse reject(@AuthenticationPrincipal AccountPrincipal principal,
            @PathVariable Long appointmentId,
            @Valid @RequestBody StaffMessageRequest request) {
        return staffAppointmentService.reject(principal.getUserId(), appointmentId, request);
    }

    @PostMapping("/{appointmentId}/propose-time")
    public AppointmentResponse proposeTime(@AuthenticationPrincipal AccountPrincipal principal,
            @PathVariable Long appointmentId,
            @Valid @RequestBody ProposeAppointmentTimeRequest request) {
        return staffAppointmentService.proposeTime(principal.getUserId(), appointmentId, request);
    }

    @PostMapping("/{appointmentId}/confirm-proposed")
    public AppointmentResponse confirmGuestProposedTime(@AuthenticationPrincipal AccountPrincipal principal,
            @PathVariable Long appointmentId) {
        return staffAppointmentService.confirmGuestProposedTime(principal.getUserId(), appointmentId);
    }

    @PostMapping("/{appointmentId}/complete-repair")
    public AppointmentResponse completeRepair(@AuthenticationPrincipal AccountPrincipal principal,
            @PathVariable Long appointmentId,
            @Valid @RequestBody CompleteRepairRequest request) {
        return repairWorkflowService.completeRepair(principal.getUserId(), appointmentId, request);
    }

    @PostMapping("/{appointmentId}/mark-picked-up")
    public AppointmentResponse markPickedUp(@AuthenticationPrincipal AccountPrincipal principal,
            @PathVariable Long appointmentId) {
        return repairWorkflowService.markPickedUp(principal.getUserId(), appointmentId);
    }
}
