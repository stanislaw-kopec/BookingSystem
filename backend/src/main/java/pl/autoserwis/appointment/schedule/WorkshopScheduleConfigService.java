package pl.autoserwis.appointment.schedule;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.autoserwis.appointment.domain.AppointmentConflictException;
import pl.autoserwis.appointment.domain.AppointmentValidationException;
import pl.autoserwis.appointment.persistence.AppointmentRepository;
import pl.autoserwis.appointment.persistence.DailyAppointmentCount;
import pl.autoserwis.appointment.schedule.domain.ScheduleDayOverride;
import pl.autoserwis.appointment.schedule.domain.WorkshopScheduleSettings;
import pl.autoserwis.appointment.schedule.dto.ScheduleDayOverrideRequest;
import pl.autoserwis.appointment.schedule.dto.ScheduleDayOverrideResponse;
import pl.autoserwis.appointment.schedule.dto.ScheduleSettingsRequest;
import pl.autoserwis.appointment.schedule.dto.ScheduleSettingsResponse;
import pl.autoserwis.appointment.schedule.dto.WorkshopScheduleConfigResponse;
import pl.autoserwis.appointment.schedule.persistence.ScheduleDayOverrideRepository;
import pl.autoserwis.appointment.schedule.persistence.WorkshopScheduleSettingsRepository;
import pl.autoserwis.exception.ApiErrorCode;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class WorkshopScheduleConfigService {
    public static final int DEFAULT_DAILY_CAPACITY = 4;
    public static final int DEFAULT_BOOKING_HORIZON_DAYS = 30;
    public static final LocalTime DEFAULT_WORKDAY_START = LocalTime.of(8, 0);
    public static final LocalTime DEFAULT_WORKDAY_END = LocalTime.of(16, 0);

    private final WorkshopScheduleSettingsRepository settingsRepository;
    private final ScheduleDayOverrideRepository overridesRepository;
    private final AppointmentRepository appointments;
    private final ScheduleLocks locks;
    private final Clock clock;

    public WorkshopScheduleConfigService(WorkshopScheduleSettingsRepository settingsRepository,
            ScheduleDayOverrideRepository overridesRepository, AppointmentRepository appointments,
            ScheduleLocks locks, Clock workshopClock) {
        this.settingsRepository = settingsRepository;
        this.overridesRepository = overridesRepository;
        this.appointments = appointments;
        this.locks = locks;
        this.clock = workshopClock;
    }

    public WorkshopScheduleSettings currentSettings() {
        return settingsRepository.findById(WorkshopScheduleSettings.SINGLETON_ID)
            .orElseGet(() -> new WorkshopScheduleSettings(DEFAULT_DAILY_CAPACITY,
                DEFAULT_BOOKING_HORIZON_DAYS, DEFAULT_WORKDAY_START, DEFAULT_WORKDAY_END));
    }

    public List<ScheduleDayOverride> overrides(LocalDate start, LocalDate end) {
        return overridesRepository.findByDateBetweenOrderByDateAsc(start, end);
    }

    public Map<LocalDate, ScheduleDayOverride> overridesByDate(LocalDate start, LocalDate end) {
        Map<LocalDate, ScheduleDayOverride> result = new LinkedHashMap<>();
        overrides(start, end).forEach(override -> result.put(override.getDate(), override));
        return result;
    }

    public WorkshopScheduleConfigResponse config() {
        WorkshopScheduleSettings settings = currentSettings();
        LocalDate today = LocalDate.now(clock);
        return response(settings, overrides(today, today.plusDays(settings.getBookingHorizonDays())));
    }

    @Transactional
    public WorkshopScheduleConfigResponse updateSettings(ScheduleSettingsRequest request) {
        locks.forConfiguration();
        validateHours(request.workdayStart(), request.workdayEnd());
        LocalDate today = LocalDate.now(clock);
        WorkshopScheduleSettings proposed = new WorkshopScheduleSettings(request.defaultDailyCapacity(),
            request.bookingHorizonDays(), request.workdayStart(), request.workdayEnd());
        // Include reservations beyond a newly shortened booking horizon.
        List<DailyAppointmentCount> occupiedDays = appointments.countActiveDaysFrom(
            today.atStartOfDay(AppointmentSchedule.TIME_ZONE).toInstant());
        LocalDate lastOccupiedDate = occupiedDays.isEmpty() ? today : occupiedDays.getLast().getVisitDate();
        Map<LocalDate, ScheduleDayOverride> overrides = overridesByDate(today, lastOccupiedDate);
        for (DailyAppointmentCount day : occupiedDays) {
            requireCapacity(day.getOccupied(), AppointmentSchedule.capacityFor(day.getVisitDate(), proposed,
                overrides.get(day.getVisitDate())), "defaultDailyCapacity");
        }
        WorkshopScheduleSettings settings = settingsRepository.findById(WorkshopScheduleSettings.SINGLETON_ID)
            .orElseGet(() -> new WorkshopScheduleSettings(DEFAULT_DAILY_CAPACITY,
                DEFAULT_BOOKING_HORIZON_DAYS, DEFAULT_WORKDAY_START, DEFAULT_WORKDAY_END));
        settings.update(request.defaultDailyCapacity(), request.bookingHorizonDays(),
            request.workdayStart(), request.workdayEnd());
        settingsRepository.save(settings);
        return response(settings, overrides(today, today.plusDays(settings.getBookingHorizonDays())));
    }

    @Transactional
    public WorkshopScheduleConfigResponse saveOverride(ScheduleDayOverrideRequest request) {
        locks.forConfiguration();
        if (request.closed() && request.capacity() != 0) {
            throw new AppointmentValidationException(Map.of("capacity",
                "A closed day must have 0 places."));
        }
        if (!request.closed() && request.capacity() < 1) {
            throw new AppointmentValidationException(Map.of("capacity",
                "An open day must have at least 1 place."));
        }
        LocalDate date = request.date();
        int targetCapacity = request.closed() ? 0 : request.capacity();
        long occupied = occupiedPlaces(date);
        requireCapacity(occupied, targetCapacity, "capacity");
        ScheduleDayOverride override = overridesRepository.findByDate(date)
            .orElseGet(() -> new ScheduleDayOverride(date, targetCapacity, request.closed(), request.note()));
        override.update(targetCapacity, request.closed(), request.note());
        overridesRepository.save(override);
        return config();
    }

    @Transactional
    public WorkshopScheduleConfigResponse deleteOverride(LocalDate date) {
        locks.forConfiguration();
        overridesRepository.findByDate(date).ifPresent(override -> {
            requireCapacity(occupiedPlaces(date), AppointmentSchedule.capacityFor(date, currentSettings(), null), "capacity");
            overridesRepository.delete(override);
        });
        return config();
    }

    private void requireCapacity(long occupied, int capacity, String field) {
        if (occupied > capacity) {
            throw new AppointmentConflictException(ApiErrorCode.SCHEDULE_CAPACITY_CONFLICT, field,
                "Capacity cannot be lower than active appointment requests. Reschedule or cancel them first.");
        }
    }

    private long occupiedPlaces(LocalDate date) {
        return appointments.countBlockingStarts(AppointmentSchedule.blockingStatuses(),
            date.atStartOfDay(AppointmentSchedule.TIME_ZONE).toInstant(),
            date.plusDays(1).atStartOfDay(AppointmentSchedule.TIME_ZONE).toInstant());
    }

    private void validateHours(LocalTime start, LocalTime end) {
        if (start == null || end == null || !start.isBefore(end)) {
            throw new AppointmentValidationException(Map.of("workdayEnd",
                "Workday end time must be later than start time."));
        }
    }

    private WorkshopScheduleConfigResponse response(WorkshopScheduleSettings settings,
            List<ScheduleDayOverride> overrides) {
        return new WorkshopScheduleConfigResponse(AppointmentSchedule.TIME_ZONE.getId(),
            new ScheduleSettingsResponse(settings.getDefaultDailyCapacity(),
                settings.getBookingHorizonDays(), settings.getWorkdayStart(), settings.getWorkdayEnd()),
            overrides.stream().map(this::response).toList());
    }

    private ScheduleDayOverrideResponse response(ScheduleDayOverride override) {
        return new ScheduleDayOverrideResponse(override.getId(), override.getDate(),
            override.getCapacity(), override.isClosed(), override.getNote() == null ? "" : override.getNote());
    }
}
