package pl.autoserwis.appointment;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
public interface AppointmentRepository extends JpaRepository<AppointmentRequest, Long> {
    List<AppointmentRequest> findByClient_IdOrderByCreatedAtDesc(Long clientId);

    Page<AppointmentRequest> findByClient_Id(Long clientId, Pageable pageable);

    Page<AppointmentRequest> findByClient_IdAndStatus(Long clientId, AppointmentStatus status, Pageable pageable);

    Page<AppointmentRequest> findByStatus(AppointmentStatus status, Pageable pageable);

    List<AppointmentRequest> findAllByOrderByCreatedAtDesc();

    boolean existsByReference(UUID reference);
    List<AppointmentRequest> findByVehicle_IdAndClient_IdAndStatusOrderByVehiclePickedUpAtDesc(
        Long vehicleId, Long clientId, AppointmentStatus status);

    Optional<AppointmentRequest> findByIdAndVehicle_IdAndClient_IdAndStatus(
        Long id, Long vehicleId, Long clientId, AppointmentStatus status);
    @Query("""
        select appointment.currentStartAt
        from AppointmentRequest appointment
        where appointment.status in :statuses
          and appointment.currentStartAt >= :rangeStart
          and appointment.currentStartAt < :rangeEnd
        """)
    List<Instant> findBlockingStarts(
        @Param("statuses") Collection<AppointmentStatus> statuses,
        @Param("rangeStart") Instant rangeStart,
        @Param("rangeEnd") Instant rangeEnd);
    @Query(value = "select 1 from pg_advisory_xact_lock(hashtext(cast(:visitDate as text)))",
        nativeQuery = true)
    Integer lockAppointmentDay(@Param("visitDate") LocalDate visitDate);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select appointment from AppointmentRequest appointment where appointment.id = :id")
    Optional<AppointmentRequest> findByIdForUpdate(@Param("id") Long id);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select appointment
        from AppointmentRequest appointment
        where appointment.id = :id and appointment.client.id = :clientId
        """)
    Optional<AppointmentRequest> findByIdAndClientIdForUpdate(
        @Param("id") Long id, @Param("clientId") Long clientId);
}
