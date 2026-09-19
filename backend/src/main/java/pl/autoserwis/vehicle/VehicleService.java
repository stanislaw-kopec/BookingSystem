package pl.autoserwis.vehicle;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.autoserwis.appointment.AppointmentRepository;
import pl.autoserwis.appointment.AppointmentRequest;
import pl.autoserwis.appointment.AppointmentStatus;
import pl.autoserwis.appointment.RepairHistoryMapper;
import pl.autoserwis.exception.ResourceNotFoundException;
import pl.autoserwis.invoice.InvoiceFile;
import pl.autoserwis.invoice.InvoiceService;
import pl.autoserwis.user.AppUser;
import pl.autoserwis.user.UserRepository;
import pl.autoserwis.vehicle.dto.RepairHistoryEntryResponse;
import pl.autoserwis.vehicle.dto.VehicleRequest;
import pl.autoserwis.vehicle.dto.VehicleResponse;

import java.time.Clock;
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
    private final InvoiceService invoices;
    private final RepairHistoryMapper repairHistory;
    private final Clock clock;

    public VehicleService(VehicleRepository vehicles, UserRepository users,
            AppointmentRepository appointments, InvoiceService invoices,
            RepairHistoryMapper repairHistory, Clock workshopClock) {
        this.vehicles = vehicles;
        this.users = users;
        this.appointments = appointments;
        this.invoices = invoices;
        this.repairHistory = repairHistory;
        this.clock = workshopClock;
    }

    public List<VehicleResponse> getCurrentClientVehicles(Long userId) {
        AppUser owner = user(userId);
        return vehicles.findByOwner_IdOrderByMakeAscModelAscRegistrationNumberAsc(owner.getId()).stream()
            .map(this::response)
            .toList();
    }

    public VehicleResponse getCurrentClientVehicle(Long userId, Long vehicleId) {
        AppUser owner = user(userId);
        return response(vehicles.findByIdAndOwner_Id(vehicleId, owner.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found.")));
    }

    public List<RepairHistoryEntryResponse> getCurrentClientVehicleRepairHistory(Long userId, Long vehicleId) {
        AppUser owner = user(userId);
        vehicles.findByIdAndOwner_Id(vehicleId, owner.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found."));
        return appointments.findByVehicle_IdAndClient_IdAndStatusOrderByVehiclePickedUpAtDesc(
                vehicleId, owner.getId(), AppointmentStatus.COMPLETED).stream()
            .map(repairHistory::toResponse)
            .toList();
    }

    @Transactional
    public InvoiceFile getCurrentClientRepairInvoice(Long userId, Long vehicleId, Long appointmentId) {
        AppUser owner = user(userId);
        vehicles.findByIdAndOwner_Id(vehicleId, owner.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found."));
        AppointmentRequest appointment = appointments.findByIdAndVehicle_IdAndClient_IdAndStatus(
                appointmentId, vehicleId, owner.getId(), AppointmentStatus.COMPLETED)
            .orElseThrow(() -> new ResourceNotFoundException("Completed repair not found."));
        return invoices.documentFor(appointment.getId());
    }

    @Transactional
    public VehicleResponse create(Long userId, VehicleRequest request) {
        AppUser owner = user(userId);
        NormalizedVehicle normalized = normalize(request);
        validateUniqueIdentifiers(owner.getId(), normalized, null);

        Vehicle vehicle = new Vehicle(owner, normalized.make(), normalized.model(),
            normalized.productionYear(), normalized.registrationNumber(), normalized.vin());
        return response(vehicles.save(vehicle));
    }

    @Transactional
    public VehicleResponse update(Long userId, Long vehicleId, VehicleRequest request) {
        AppUser owner = user(userId);
        Vehicle vehicle = vehicles.findByIdAndOwner_Id(vehicleId, owner.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found."));
        NormalizedVehicle normalized = normalize(request);
        validateUniqueIdentifiers(owner.getId(), normalized, vehicle.getId());
        vehicle.update(normalized.make(), normalized.model(), normalized.productionYear(),
            normalized.registrationNumber(), normalized.vin());
        return response(vehicle);
    }

    private NormalizedVehicle normalize(VehicleRequest request) {
        validateProductionYear(request.productionYear());
        String registrationNumber = request.registrationNumber().replaceAll("\\s+", "")
            .toUpperCase(Locale.ROOT);
        if (registrationNumber.length() < 2) {
            throw new VehicleValidationException(Map.of("registrationNumber",
                "Registration number must have at least 2 characters."));
        }
        return new NormalizedVehicle(request.make().strip(), request.model().strip(),
            request.productionYear(), registrationNumber, optionalVin(request.vin()));
    }

    private void validateUniqueIdentifiers(Long ownerId, NormalizedVehicle vehicle, Long currentVehicleId) {
        boolean registrationExists = currentVehicleId == null
            ? vehicles.existsByOwner_IdAndRegistrationNumberIgnoreCase(ownerId, vehicle.registrationNumber())
            : vehicles.existsByOwner_IdAndRegistrationNumberIgnoreCaseAndIdNot(
                ownerId, vehicle.registrationNumber(), currentVehicleId);
        if (registrationExists) {
            throw new VehicleConflictException("registrationNumber",
                "You already have a vehicle with this registration number.");
        }
        if (vehicle.vin() == null) return;
        boolean vinExists = currentVehicleId == null
            ? vehicles.existsByOwner_IdAndVinIgnoreCase(ownerId, vehicle.vin())
            : vehicles.existsByOwner_IdAndVinIgnoreCaseAndIdNot(ownerId, vehicle.vin(), currentVehicleId);
        if (vinExists) {
            throw new VehicleConflictException("vin", "You already have a vehicle with this VIN.");
        }
    }

    private void validateProductionYear(int productionYear) {
        int latestAllowedYear = Year.now(clock).getValue() + 1;
        if (productionYear > latestAllowedYear) {
            throw new VehicleValidationException(Map.of("productionYear",
                "Production year cannot be later than " + latestAllowedYear + "."));
        }
    }

    private AppUser user(Long userId) {
        return users.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found."));
    }

    private String optionalVin(String value) {
        return value == null || value.isBlank() ? null : value.strip().toUpperCase(Locale.ROOT);
    }

    private VehicleResponse response(Vehicle vehicle) {
        return new VehicleResponse(vehicle.getId(), vehicle.getMake(), vehicle.getModel(),
            vehicle.getProductionYear(), vehicle.getRegistrationNumber(),
            vehicle.getVin() == null ? "" : vehicle.getVin());
    }

    private record NormalizedVehicle(
        String make,
        String model,
        int productionYear,
        String registrationNumber,
        String vin
    ) {}
}
