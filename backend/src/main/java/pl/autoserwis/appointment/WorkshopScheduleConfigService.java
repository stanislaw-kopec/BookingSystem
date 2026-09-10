package pl.autoserwis.appointment;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.autoserwis.appointment.dto.*;

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

    public WorkshopScheduleConfigService(WorkshopScheduleSettingsRepository settingsRepository,
            ScheduleDayOverrideRepository overridesRepository, AppointmentRepository appointments) {
        this.settingsRepository = settingsRepository;
        this.overridesRepository = overridesRepository;
        this.appointments = appointments;
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
        LocalDate today = LocalDate.now(AppointmentSchedule.TIME_ZONE);
        return response(settings, overrides(today, today.plusDays(settings.getBookingHorizonDays())));
    }

    @Transactional
    public WorkshopScheduleConfigResponse updateSettings(ScheduleSettingsRequest request) {
        validateHours(request.workdayStart(), request.workdayEnd());
        WorkshopScheduleSettings settings = settingsRepository.findById(WorkshopScheduleSettings.SINGLETON_ID)
            .orElseGet(() -> new WorkshopScheduleSettings(DEFAULT_DAILY_CAPACITY,
                DEFAULT_BOOKING_HORIZON_DAYS, DEFAULT_WORKDAY_START, DEFAULT_WORKDAY_END));
        settings.update(request.defaultDailyCapacity(), request.bookingHorizonDays(),
            request.workdayStart(), request.workdayEnd());
        settingsRepository.save(settings);
        LocalDate today = LocalDate.now(AppointmentSchedule.TIME_ZONE);
        return response(settings, overrides(today, today.plusDays(settings.getBookingHorizonDays())));
    }

    @Transactional
    public WorkshopScheduleConfigResponse saveOverride(ScheduleDayOverrideRequest request) {
        if (request.closed() && request.capacity() != 0) {
            throw new AppointmentValidationException(Map.of("capacity",
                "Dzień zamknięty musi mieć 0 miejsc."));
        }
        if (!request.closed() && request.capacity() < 1) {
            throw new AppointmentValidationException(Map.of("capacity",
                "Otwarty dzień musi mieć co najmniej 1 miejsce."));
        }
        LocalDate date = request.date();
        int targetCapacity = request.closed() ? 0 : request.capacity();
        long occupied = occupiedPlaces(date);
        if (occupied > targetCapacity) {
            throw new AppointmentConflictException("capacity",
                "Nie można ustawić mniej miejsc niż liczba aktywnych zgłoszeń w tym dniu.");
        }
        ScheduleDayOverride override = overridesRepository.findByDate(date)
            .orElseGet(() -> new ScheduleDayOverride(date, targetCapacity, request.closed(), request.note()));
        override.update(targetCapacity, request.closed(), request.note());
        overridesRepository.save(override);
        return config();
    }

    @Transactional
    public WorkshopScheduleConfigResponse deleteOverride(LocalDate date) {
        overridesRepository.findByDate(date).ifPresent(overridesRepository::delete);
        return config();
    }

    private long occupiedPlaces(LocalDate date) {
        return appointments.countBlockingStarts(AppointmentSchedule.blockingStatuses(),
            date.atStartOfDay(AppointmentSchedule.TIME_ZONE).toInstant(),
            date.plusDays(1).atStartOfDay(AppointmentSchedule.TIME_ZONE).toInstant());
    }

    private void validateHours(LocalTime start, LocalTime end) {
        if (start == null || end == null || !start.isBefore(end)) {
            throw new AppointmentValidationException(Map.of("workdayEnd",
                "Godzina zakończenia musi być późniejsza niż rozpoczęcia."));
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
