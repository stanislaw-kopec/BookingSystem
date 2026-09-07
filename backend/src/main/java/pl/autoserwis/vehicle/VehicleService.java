package pl.autoserwis.vehicle;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.autoserwis.exception.ResourceNotFoundException;
import pl.autoserwis.user.AppUser;
import pl.autoserwis.user.UserRepository;
import pl.autoserwis.vehicle.dto.VehicleRequest;
import pl.autoserwis.vehicle.dto.VehicleResponse;

import java.time.Year;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class VehicleService {
    private final VehicleRepository vehicles;
    private final UserRepository users;

    public VehicleService(VehicleRepository vehicles, UserRepository users) {
        this.vehicles = vehicles;
        this.users = users;
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
}
