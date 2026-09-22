package pl.autoserwis.vehicle;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.autoserwis.appointment.domain.AppointmentRequest;
import pl.autoserwis.appointment.domain.AppointmentStatus;
import pl.autoserwis.appointment.persistence.AppointmentRepository;
import pl.autoserwis.appointment.repair.RepairHistoryMapper;
import pl.autoserwis.appointment.repair.dto.RepairHistoryEntryResponse;
import pl.autoserwis.exception.ResourceNotFoundException;
import pl.autoserwis.invoice.InvoiceFile;
import pl.autoserwis.invoice.InvoiceService;
import pl.autoserwis.user.AppUser;
import pl.autoserwis.user.UserRepository;
import pl.autoserwis.vehicle.dto.VehicleRequest;
import pl.autoserwis.vehicle.dto.VehicleResponse;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class VehicleService {
    private final VehicleRepository vehicles;
    private final UserRepository users;
    private final AppointmentRepository appointments;
    private final InvoiceService invoices;
    private final RepairHistoryMapper repairHistory;
    private final VehicleDataNormalizer vehicleData;

    public VehicleService(VehicleRepository vehicles, UserRepository users,
            AppointmentRepository appointments, InvoiceService invoices,
            RepairHistoryMapper repairHistory, VehicleDataNormalizer vehicleData) {
        this.vehicles = vehicles;
        this.users = users;
        this.appointments = appointments;
        this.invoices = invoices;
        this.repairHistory = repairHistory;
        this.vehicleData = vehicleData;
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
        VehicleDataNormalizer.NormalizedVehicleData normalized = normalize(request);
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
        VehicleDataNormalizer.NormalizedVehicleData normalized = normalize(request);
        validateUniqueIdentifiers(owner.getId(), normalized, vehicle.getId());
        vehicle.update(normalized.make(), normalized.model(), normalized.productionYear(),
            normalized.registrationNumber(), normalized.vin());
        return response(vehicle);
    }

    private VehicleDataNormalizer.NormalizedVehicleData normalize(VehicleRequest request) {
        VehicleDataNormalizer.NormalizationResult result = vehicleData.normalize(
            request.make(), request.model(), request.productionYear(),
            request.registrationNumber(), request.vin());
        if (!result.isValid()) {
            throw new VehicleValidationException(result.fieldErrors());
        }
        return result.data();
    }

    private void validateUniqueIdentifiers(Long ownerId,
            VehicleDataNormalizer.NormalizedVehicleData vehicle, Long currentVehicleId) {
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

    private AppUser user(Long userId) {
        return users.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found."));
    }

    private VehicleResponse response(Vehicle vehicle) {
        return new VehicleResponse(vehicle.getId(), vehicle.getMake(), vehicle.getModel(),
            vehicle.getProductionYear(), vehicle.getRegistrationNumber(),
            vehicle.getVin() == null ? "" : vehicle.getVin());
    }

}
