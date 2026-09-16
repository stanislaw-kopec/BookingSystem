package pl.autoserwis.appointment;

import org.springframework.stereotype.Component;
import pl.autoserwis.appointment.dto.AppointmentAvailabilityResponse;
import pl.autoserwis.appointment.dto.AppointmentDayResponse;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
public class AppointmentSchedule {
    public static final ZoneId TIME_ZONE = ZoneId.of("Europe/Warsaw");
    public static final LocalTime WORKDAY_START = WorkshopScheduleConfigService.DEFAULT_WORKDAY_START;
    public static final LocalTime WORKDAY_END = WorkshopScheduleConfigService.DEFAULT_WORKDAY_END;
    public static final int DAILY_CAPACITY = WorkshopScheduleConfigService.DEFAULT_DAILY_CAPACITY;
    public static final int BOOKING_HORIZON_DAYS = WorkshopScheduleConfigService.DEFAULT_BOOKING_HORIZON_DAYS;

    private static final Set<AppointmentStatus> BLOCKING_STATUSES = EnumSet.of(
        AppointmentStatus.PENDING,
        AppointmentStatus.TIME_PROPOSED,
        AppointmentStatus.CONFIRMED);

    private final AppointmentRepository appointments;
    private final WorkshopScheduleConfigService scheduleConfig;

    public AppointmentSchedule(AppointmentRepository appointments, WorkshopScheduleConfigService scheduleConfig) {
        this.appointments = appointments;
        this.scheduleConfig = scheduleConfig;
    }

    public AppointmentAvailabilityResponse availability() {
        ZonedDateTime now = ZonedDateTime.now(TIME_ZONE);
        WorkshopScheduleSettings settings = scheduleConfig.currentSettings();
        LocalDate firstDate = now.toLocalDate();
        LocalDate lastDate = firstDate.plusDays(settings.getBookingHorizonDays());
        Instant rangeStart = firstDate.atStartOfDay(TIME_ZONE).toInstant();
        Instant rangeEnd = lastDate.plusDays(1).atStartOfDay(TIME_ZONE).toInstant();
        Map<LocalDate, Long> activeCounts = appointments.findBlockingStarts(
                BLOCKING_STATUSES, rangeStart, rangeEnd).stream()
            .map(this::dateOf)
            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        Map<LocalDate, ScheduleDayOverride> overrides = scheduleConfig.overridesByDate(firstDate, lastDate);
        List<AppointmentDayResponse> days = IntStream.rangeClosed(0, settings.getBookingHorizonDays())
            .mapToObj(firstDate::plusDays)
            .filter(date -> date.isAfter(now.toLocalDate()))
            .filter(date -> isWorkingDay(date) || overrides.containsKey(date))
            .map(date -> day(date, settings, overrides.get(date), activeCounts.getOrDefault(date, 0L).intValue()))
            .toList();

        return new AppointmentAvailabilityResponse(TIME_ZONE.getId(), settings.getDefaultDailyCapacity(), days);
    }

    public Instant validateAndNormalize(LocalDate visitDate) {
        ZonedDateTime now = ZonedDateTime.now(TIME_ZONE);
        LocalDate today = now.toLocalDate();

        if (!visitDate.isAfter(today)) {
            throw invalidVisitDate("Visit day must be in the future.");
        }
        WorkshopScheduleSettings settings = scheduleConfig.currentSettings();
        if (visitDate.isAfter(today.plusDays(settings.getBookingHorizonDays()))) {
            throw invalidVisitDate("Visit day must fit within the current booking horizon.");
        }
        ScheduleDayOverride override = scheduleConfig.overridesByDate(visitDate, visitDate).get(visitDate);
        if (capacityFor(visitDate, settings, override) <= 0) {
            throw invalidVisitDate("This day is unavailable in the workshop schedule.");
        }
        return dayStart(visitDate, settings).truncatedTo(ChronoUnit.SECONDS).toInstant();
    }

    public LocalDate visitDate(Instant startAt) {
        return dateOf(startAt);
    }

    public boolean isFull(Instant startAt) {
        LocalDate date = dateOf(startAt);
        Instant rangeStart = date.atStartOfDay(TIME_ZONE).toInstant();
        Instant rangeEnd = date.plusDays(1).atStartOfDay(TIME_ZONE).toInstant();
        WorkshopScheduleSettings settings = scheduleConfig.currentSettings();
        ScheduleDayOverride override = scheduleConfig.overridesByDate(date, date).get(date);
        int capacity = capacityFor(date, settings, override);
        return capacity <= 0 || appointments.countBlockingStarts(BLOCKING_STATUSES, rangeStart, rangeEnd) >= capacity;
    }

    private LocalDate dateOf(Instant startAt) {
        return startAt.atZone(TIME_ZONE).toLocalDate();
    }

    public static Set<AppointmentStatus> blockingStatuses() {
        return BLOCKING_STATUSES;
    }

    private AppointmentDayResponse day(LocalDate date, WorkshopScheduleSettings settings,
            ScheduleDayOverride override, int occupied) {
        int capacity = capacityFor(date, settings, override);
        int remainingCapacity = Math.max(capacity - occupied, 0);
        return new AppointmentDayResponse(
            date,
            dayStart(date, settings).toOffsetDateTime(),
            dayEnd(date, settings).toOffsetDateTime(),
            capacity,
            remainingCapacity,
            capacity > 0 && remainingCapacity > 0);
    }

    public static int capacityFor(LocalDate date, WorkshopScheduleSettings settings, ScheduleDayOverride override) {
        if (override != null) return override.isClosed() ? 0 : override.getCapacity();
        return isWorkingDay(date) ? settings.getDefaultDailyCapacity() : 0;
    }

    private ZonedDateTime dayStart(LocalDate date, WorkshopScheduleSettings settings) {
        return date.atTime(settings.getWorkdayStart()).atZone(TIME_ZONE);
    }

    private ZonedDateTime dayEnd(LocalDate date, WorkshopScheduleSettings settings) {
        return date.atTime(settings.getWorkdayEnd()).atZone(TIME_ZONE);
    }

    private static boolean isWorkingDay(LocalDate date) {
        return date.getDayOfWeek() != DayOfWeek.SATURDAY
            && date.getDayOfWeek() != DayOfWeek.SUNDAY;
    }

    private AppointmentValidationException invalidVisitDate(String message) {
        return new AppointmentValidationException(java.util.Map.of("visitDate", message));
    }
}
