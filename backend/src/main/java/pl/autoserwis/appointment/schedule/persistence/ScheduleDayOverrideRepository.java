package pl.autoserwis.appointment.schedule.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.autoserwis.appointment.schedule.domain.ScheduleDayOverride;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ScheduleDayOverrideRepository extends JpaRepository<ScheduleDayOverride, Long> {
    Optional<ScheduleDayOverride> findByDate(LocalDate date);
    List<ScheduleDayOverride> findByDateBetweenOrderByDateAsc(LocalDate start, LocalDate end);
}
