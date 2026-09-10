package pl.autoserwis.vehicle;

import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import pl.autoserwis.invoice.InvoiceFile;
import pl.autoserwis.vehicle.dto.RepairHistoryEntryResponse;
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
    public List<VehicleResponse> getVehicles(Authentication authentication) {
        return vehicleService.getCurrentClientVehicles(authentication.getName());
    }

    @GetMapping("/{vehicleId}")
    public VehicleResponse getVehicle(Authentication authentication, @PathVariable Long vehicleId) {
        return vehicleService.getCurrentClientVehicle(authentication.getName(), vehicleId);
    }

    @GetMapping("/{vehicleId}/repair-history")
    public List<RepairHistoryEntryResponse> getRepairHistory(Authentication authentication,
            @PathVariable Long vehicleId) {
        return vehicleService.getCurrentClientVehicleRepairHistory(authentication.getName(), vehicleId);
    }

    @GetMapping("/{vehicleId}/repair-history/{appointmentId}/invoice")
    public ResponseEntity<byte[]> getRepairInvoice(Authentication authentication,
            @PathVariable Long vehicleId, @PathVariable Long appointmentId) {
        InvoiceFile invoice = vehicleService.getCurrentClientRepairInvoice(
            authentication.getName(), vehicleId, appointmentId);
        ContentDisposition disposition = ContentDisposition.attachment()
            .filename(invoice.filename(), StandardCharsets.UTF_8)
            .build();
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
            .body(invoice.content());
    }

    @PostMapping
    public ResponseEntity<VehicleResponse> createVehicle(Authentication authentication,
            @Valid @RequestBody VehicleRequest request) {
        VehicleResponse result = vehicleService.create(authentication.getName(), request);
        return ResponseEntity.created(URI.create("/api/vehicles/" + result.id())).body(result);
    }
}
