package pl.autoserwis.appointment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface WorkshopScheduleSettingsRepository extends JpaRepository<WorkshopScheduleSettings, Long> {
    // Two-key advisory locks use a separate namespace from the single-key day locks.
    @Query(value = "select 1 from pg_advisory_xact_lock_shared(73001, 1)", nativeQuery = true)
    Integer lockShared();

    @Query(value = "select 1 from pg_advisory_xact_lock(73001, 1)", nativeQuery = true)
    Integer lockExclusive();
}
