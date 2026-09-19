package pl.autoserwis.appointment;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AppointmentScheduleTest {

    @Test
    void validatesDateAgainstInjectedWorkshopClock() {
        Clock clock = Clock.fixed(Instant.parse("2026-01-10T23:30:00Z"),
            ZoneId.of("Europe/Warsaw"));
        AppointmentRepository appointments = mock(AppointmentRepository.class);
        WorkshopScheduleConfigService config = mock(WorkshopScheduleConfigService.class);
        WorkshopScheduleSettings settings = new WorkshopScheduleSettings(
            4, 30, LocalTime.of(8, 0), LocalTime.of(16, 0));
        LocalDate visitDate = LocalDate.of(2026, 1, 12);
        when(config.currentSettings()).thenReturn(settings);
        when(config.overridesByDate(visitDate, visitDate)).thenReturn(Map.of());
        AppointmentSchedule schedule = new AppointmentSchedule(appointments, config, clock);

        Instant result = schedule.validateAndNormalize(visitDate);

        assertThat(result).isEqualTo(Instant.parse("2026-01-12T07:00:00Z"));
    }
}
