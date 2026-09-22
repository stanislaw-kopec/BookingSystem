package pl.autoserwis.appointment.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pl.autoserwis.appointment.domain.AppointmentRequest;
import pl.autoserwis.appointment.domain.AppointmentStatus;
import pl.autoserwis.appointment.schedule.dto.ScheduleAppointmentResponse;
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

    @Query("""
        select new pl.autoserwis.appointment.schedule.dto.ScheduleAppointmentResponse(
            a.id, a.reference, a.status, a.currentStartAt, a.vehicleMake, a.vehicleModel,
            a.vehicleRegistrationNumber, a.firstName, a.lastName, substring(a.problemDescription, 1, 160))
        from AppointmentRequest a
        where a.status in :statuses and a.currentStartAt >= :rangeStart and a.currentStartAt < :rangeEnd
        order by a.currentStartAt, a.createdAt, a.id
        """)
    List<ScheduleAppointmentResponse> findScheduleAppointments(
        @Param("statuses") Collection<AppointmentStatus> statuses,
        @Param("rangeStart") Instant rangeStart, @Param("rangeEnd") Instant rangeEnd);

    @Query(value = """
        select cast(current_start_at at time zone 'Europe/Warsaw' as date) as "visitDate", count(*) as occupied
        from appointment_requests
        where status in ('PENDING', 'TIME_PROPOSED', 'CONFIRMED') and current_start_at >= :since
        group by 1 order by 1
        """, nativeQuery = true)
    List<DailyAppointmentCount> countActiveDaysFrom(@Param("since") Instant since);

    boolean existsByReference(UUID reference);
    List<AppointmentRequest> findByVehicle_IdAndClient_IdAndStatusOrderByVehiclePickedUpAtDesc(
        Long vehicleId, Long clientId, AppointmentStatus status);

    List<AppointmentRequest> findByVehicle_IdAndStatusOrderByVehiclePickedUpAtDesc(
        Long vehicleId, AppointmentStatus status);

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

    @Query("""
        select count(appointment)
        from AppointmentRequest appointment
        where appointment.status in :statuses
          and appointment.currentStartAt >= :rangeStart
          and appointment.currentStartAt < :rangeEnd
        """)
    long countBlockingStarts(
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
