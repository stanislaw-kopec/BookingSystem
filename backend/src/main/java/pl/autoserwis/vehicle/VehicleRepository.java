package pl.autoserwis.vehicle;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    List<Vehicle> findByOwner_IdOrderByMakeAscModelAscRegistrationNumberAsc(Long ownerId);
    Optional<Vehicle> findByIdAndOwner_Id(Long id, Long ownerId);

    Optional<Vehicle> findByOwner_IdAndRegistrationNumberIgnoreCase(Long ownerId, String registrationNumber);
    boolean existsByOwner_IdAndRegistrationNumberIgnoreCase(Long ownerId, String registrationNumber);
    boolean existsByOwner_IdAndVinIgnoreCase(Long ownerId, String vin);
}
