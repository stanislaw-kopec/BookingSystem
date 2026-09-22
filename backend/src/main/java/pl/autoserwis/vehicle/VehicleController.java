package pl.autoserwis.vehicle;

import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.autoserwis.appointment.repair.dto.RepairHistoryEntryResponse;
import pl.autoserwis.invoice.InvoiceFile;
import pl.autoserwis.security.AccountPrincipal;
import pl.autoserwis.vehicle.dto.VehicleRequest;
import pl.autoserwis.vehicle.dto.VehicleResponse;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/vehicles")
public class VehicleController {
    private final VehicleService vehicleService;

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @GetMapping
    public List<VehicleResponse> getVehicles(@AuthenticationPrincipal AccountPrincipal principal) {
        return vehicleService.getCurrentClientVehicles(principal.getUserId());
    }

    @GetMapping("/{vehicleId}")
    public VehicleResponse getVehicle(@AuthenticationPrincipal AccountPrincipal principal, @PathVariable Long vehicleId) {
        return vehicleService.getCurrentClientVehicle(principal.getUserId(), vehicleId);
    }

    @GetMapping("/{vehicleId}/repair-history")
    public List<RepairHistoryEntryResponse> getRepairHistory(@AuthenticationPrincipal AccountPrincipal principal,
            @PathVariable Long vehicleId) {
        return vehicleService.getCurrentClientVehicleRepairHistory(principal.getUserId(), vehicleId);
    }

    @GetMapping("/{vehicleId}/repair-history/{appointmentId}/invoice")
    public ResponseEntity<byte[]> getRepairInvoice(@AuthenticationPrincipal AccountPrincipal principal,
            @PathVariable Long vehicleId, @PathVariable Long appointmentId) {
        InvoiceFile invoice = vehicleService.getCurrentClientRepairInvoice(
            principal.getUserId(), vehicleId, appointmentId);
        ContentDisposition disposition = ContentDisposition.attachment()
            .filename(invoice.filename(), StandardCharsets.UTF_8)
            .build();
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
            .body(invoice.content());
    }

    @PostMapping
    public ResponseEntity<VehicleResponse> createVehicle(@AuthenticationPrincipal AccountPrincipal principal,
            @Valid @RequestBody VehicleRequest request) {
        VehicleResponse result = vehicleService.create(principal.getUserId(), request);
        return ResponseEntity.created(URI.create("/api/vehicles/" + result.id())).body(result);
    }

    @PutMapping("/{vehicleId}")
    public VehicleResponse updateVehicle(@AuthenticationPrincipal AccountPrincipal principal, @PathVariable Long vehicleId,
            @Valid @RequestBody VehicleRequest request) {
        return vehicleService.update(principal.getUserId(), vehicleId, request);
    }
}
