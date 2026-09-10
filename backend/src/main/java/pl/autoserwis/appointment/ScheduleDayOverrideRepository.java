package pl.autoserwis.appointment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ScheduleDayOverrideRepository extends JpaRepository<ScheduleDayOverride, Long> {
    Optional<ScheduleDayOverride> findByDate(LocalDate date);
    List<ScheduleDayOverride> findByDateBetweenOrderByDateAsc(LocalDate start, LocalDate end);
}
