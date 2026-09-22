package pl.autoserwis.appointment.schedule;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import pl.autoserwis.appointment.domain.AppointmentValidationException;
import pl.autoserwis.appointment.persistence.AppointmentRepository;
import pl.autoserwis.appointment.schedule.domain.ScheduleDayOverride;
import pl.autoserwis.appointment.schedule.domain.WorkshopScheduleSettings;
import pl.autoserwis.appointment.schedule.dto.ScheduleAppointmentResponse;
import pl.autoserwis.appointment.schedule.dto.StaffScheduleDayResponse;
import pl.autoserwis.appointment.schedule.dto.StaffScheduleResponse;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class StaffScheduleService {
    private final AppointmentRepository appointments;
    private final WorkshopScheduleConfigService config;

    public StaffScheduleService(AppointmentRepository appointments, WorkshopScheduleConfigService config) {
        this.appointments = appointments;
        this.config = config;
    }

    // Settings, exceptions and appointments come from the same database snapshot.
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public StaffScheduleResponse schedule(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null || endDate.isBefore(startDate)
                || ChronoUnit.DAYS.between(startDate, endDate) > 30) {
            throw new AppointmentValidationException(Map.of("dateRange", "Select a range of 1 to 31 days."));
        }
        WorkshopScheduleSettings settings = config.currentSettings();
        Map<LocalDate, ScheduleDayOverride> overrides = config.overridesByDate(startDate, endDate);
        Map<LocalDate, List<ScheduleAppointmentResponse>> byDay = appointments.findScheduleAppointments(
                AppointmentSchedule.blockingStatuses(), startDate.atStartOfDay(AppointmentSchedule.TIME_ZONE).toInstant(),
                endDate.plusDays(1).atStartOfDay(AppointmentSchedule.TIME_ZONE).toInstant()).stream()
            .collect(Collectors.groupingBy(item -> item.currentStartAt().atZone(AppointmentSchedule.TIME_ZONE).toLocalDate()));
        List<StaffScheduleDayResponse> days = startDate.datesUntil(endDate.plusDays(1)).map(date -> {
            int capacity = AppointmentSchedule.capacityFor(date, settings, overrides.get(date));
            List<ScheduleAppointmentResponse> items = byDay.getOrDefault(date, List.of());
            return new StaffScheduleDayResponse(date, capacity, Math.max(0, capacity - items.size()), capacity == 0, items);
        }).toList();
        return new StaffScheduleResponse(AppointmentSchedule.TIME_ZONE.getId(), days);
    }
}
