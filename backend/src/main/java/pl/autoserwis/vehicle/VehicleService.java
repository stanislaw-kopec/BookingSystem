package pl.autoserwis.vehicle;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.autoserwis.appointment.AppointmentRepository;
import pl.autoserwis.appointment.AppointmentRequest;
import pl.autoserwis.appointment.AppointmentSchedule;
import pl.autoserwis.appointment.AppointmentStatus;
import pl.autoserwis.appointment.dto.RepairItemResponse;
import pl.autoserwis.exception.ResourceNotFoundException;
import pl.autoserwis.invoice.InvoiceFile;
import pl.autoserwis.invoice.InvoicePdfGenerator;
import pl.autoserwis.profile.ClientProfile;
import pl.autoserwis.profile.ClientProfileRepository;
import pl.autoserwis.user.AppUser;
import pl.autoserwis.user.UserRepository;
import pl.autoserwis.vehicle.dto.RepairHistoryEntryResponse;
import pl.autoserwis.vehicle.dto.VehicleRequest;
import pl.autoserwis.vehicle.dto.VehicleResponse;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.Year;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class VehicleService {
    private final VehicleRepository vehicles;
    private final UserRepository users;
    private final AppointmentRepository appointments;
    private final ClientProfileRepository profiles;
    private final InvoicePdfGenerator invoicePdfGenerator;

    public VehicleService(VehicleRepository vehicles, UserRepository users,
            AppointmentRepository appointments, ClientProfileRepository profiles,
            InvoicePdfGenerator invoicePdfGenerator) {
        this.vehicles = vehicles;
        this.users = users;
        this.appointments = appointments;
        this.profiles = profiles;
        this.invoicePdfGenerator = invoicePdfGenerator;
    }

    public List<VehicleResponse> getCurrentClientVehicles(String username) {
        AppUser owner = user(username);
        return vehicles.findByOwner_IdOrderByMakeAscModelAscRegistrationNumberAsc(owner.getId()).stream()
            .map(this::response)
            .toList();
    }

    public VehicleResponse getCurrentClientVehicle(String username, Long vehicleId) {
        AppUser owner = user(username);
        return response(vehicles.findByIdAndOwner_Id(vehicleId, owner.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Nie znaleziono pojazdu.")));
    }

    public List<RepairHistoryEntryResponse> getCurrentClientVehicleRepairHistory(String username, Long vehicleId) {
        AppUser owner = user(username);
        vehicles.findByIdAndOwner_Id(vehicleId, owner.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Nie znaleziono pojazdu."));
        return appointments.findByVehicle_IdAndClient_IdAndStatusOrderByVehiclePickedUpAtDesc(
                vehicleId, owner.getId(), AppointmentStatus.COMPLETED).stream()
            .map(this::repairHistoryEntry)
            .toList();
    }

    public InvoiceFile getCurrentClientRepairInvoice(String username, Long vehicleId, Long appointmentId) {
        AppUser owner = user(username);
        Vehicle vehicle = vehicles.findByIdAndOwner_Id(vehicleId, owner.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Nie znaleziono pojazdu."));
        AppointmentRequest appointment = appointments.findByIdAndVehicle_IdAndClient_IdAndStatus(
                appointmentId, vehicleId, owner.getId(), AppointmentStatus.COMPLETED)
            .orElseThrow(() -> new ResourceNotFoundException("Nie znaleziono zakończonej naprawy."));
        ClientProfile profile = profiles.findByUser_Id(owner.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Nie znaleziono profilu klienta."));
        return invoicePdfGenerator.generate(appointment, vehicle, profile);
    }

    @Transactional
    public VehicleResponse create(String username, VehicleRequest request) {
        validateProductionYear(request.productionYear());
        AppUser owner = user(username);
        String registrationNumber = request.registrationNumber().replaceAll("\\s+", "")
            .toUpperCase(Locale.ROOT);
        String vin = optionalVin(request.vin());

        if (registrationNumber.length() < 2) {
            throw new VehicleValidationException(Map.of("registrationNumber",
                "Numer rejestracyjny musi mieć co najmniej 2 znaki."));
        }
        if (vehicles.existsByOwner_IdAndRegistrationNumberIgnoreCase(owner.getId(), registrationNumber)) {
            throw new VehicleConflictException("registrationNumber", "Masz już pojazd z tym numerem rejestracyjnym.");
        }
        if (vin != null && vehicles.existsByOwner_IdAndVinIgnoreCase(owner.getId(), vin)) {
            throw new VehicleConflictException("vin", "Masz już pojazd z tym numerem VIN.");
        }

        Vehicle vehicle = new Vehicle(owner, request.make().strip(), request.model().strip(),
            request.productionYear(), registrationNumber, vin);
        return response(vehicles.save(vehicle));
    }

    private void validateProductionYear(int productionYear) {
        int latestAllowedYear = Year.now().getValue() + 1;
        if (productionYear > latestAllowedYear) {
            throw new VehicleValidationException(Map.of("productionYear",
                "Rok produkcji nie może być późniejszy niż " + latestAllowedYear + "."));
        }
    }

    private AppUser user(String username) {
        return users.findByUsernameIgnoreCase(username)
            .orElseThrow(() -> new ResourceNotFoundException("Nie znaleziono użytkownika."));
    }

    private String optionalVin(String value) {
        return value == null || value.isBlank() ? null : value.strip().toUpperCase(Locale.ROOT);
    }

    private VehicleResponse response(Vehicle vehicle) {
        return new VehicleResponse(vehicle.getId(), vehicle.getMake(), vehicle.getModel(),
            vehicle.getProductionYear(), vehicle.getRegistrationNumber(),
            vehicle.getVin() == null ? "" : vehicle.getVin());
    }

    private RepairHistoryEntryResponse repairHistoryEntry(AppointmentRequest appointment) {
        return new RepairHistoryEntryResponse(appointment.getId(), appointment.getReference(),
            offset(appointment.getCurrentStartAt()), appointment.getRepairDescription(),
            appointment.getTotalGrossAmount(), repairItems(appointment), offset(appointment.getRepairCompletedAt()),
            appointment.getRepairCompletedBy().getUsername(), offset(appointment.getVehiclePickedUpAt()),
            appointment.getVehiclePickedUpBy().getUsername());
    }

    private List<RepairItemResponse> repairItems(AppointmentRequest appointment) {
        return appointment.getRepairItems().stream()
            .map(item -> new RepairItemResponse(item.getId(), item.getType(), item.getName(),
                item.getQuantity(), item.getUnitGrossAmount(), item.getTotalGrossAmount()))
            .toList();
    }

    private OffsetDateTime offset(Instant value) {
        return value.atZone(AppointmentSchedule.TIME_ZONE).toOffsetDateTime();
    }
}
