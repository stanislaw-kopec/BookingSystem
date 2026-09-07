package pl.autoserwis.appointment;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AppointmentRepository extends JpaRepository<AppointmentRequest, Long> {
    List<AppointmentRequest> findByClient_IdOrderByCreatedAtDesc(Long clientId);

    List<AppointmentRequest> findAllByOrderByCreatedAtDesc();

    @Query("""
        select appointment.currentStartAt
        from AppointmentRequest appointment
        where appointment.status in :statuses
          and appointment.currentStartAt >= :rangeStart
          and appointment.currentStartAt < :rangeEnd
        """)
    List<Instant> findOccupiedStarts(
        @Param("statuses") Collection<AppointmentStatus> statuses,
        @Param("rangeStart") Instant rangeStart,
        @Param("rangeEnd") Instant rangeEnd);

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
