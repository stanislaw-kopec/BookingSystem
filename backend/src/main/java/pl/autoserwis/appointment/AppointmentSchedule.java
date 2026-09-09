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
    public static final LocalTime WORKDAY_START = LocalTime.of(8, 0);
    public static final LocalTime WORKDAY_END = LocalTime.of(16, 0);
    public static final int DAILY_CAPACITY = 4;
    public static final int BOOKING_HORIZON_DAYS = 30;

    private static final Set<AppointmentStatus> BLOCKING_STATUSES = EnumSet.of(
        AppointmentStatus.PENDING,
        AppointmentStatus.TIME_PROPOSED,
        AppointmentStatus.CONFIRMED);

    private final AppointmentRepository appointments;

    public AppointmentSchedule(AppointmentRepository appointments) {
        this.appointments = appointments;
    }

    public AppointmentAvailabilityResponse availability() {
        ZonedDateTime now = ZonedDateTime.now(TIME_ZONE);
        LocalDate firstDate = now.toLocalDate();
        LocalDate lastDate = firstDate.plusDays(BOOKING_HORIZON_DAYS);
        Instant rangeStart = firstDate.atStartOfDay(TIME_ZONE).toInstant();
        Instant rangeEnd = lastDate.plusDays(1).atStartOfDay(TIME_ZONE).toInstant();
        Map<LocalDate, Long> activeCounts = appointments.findBlockingStarts(
                BLOCKING_STATUSES, rangeStart, rangeEnd).stream()
            .map(this::dateOf)
            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        List<AppointmentDayResponse> days = IntStream.rangeClosed(0, BOOKING_HORIZON_DAYS)
            .mapToObj(firstDate::plusDays)
            .filter(this::isWorkingDay)
            .filter(date -> date.isAfter(now.toLocalDate()))
            .map(date -> {
                int occupied = activeCounts.getOrDefault(date, 0L).intValue();
                int remainingCapacity = Math.max(DAILY_CAPACITY - occupied, 0);
                return new AppointmentDayResponse(
                    date,
                    dayStart(date).toOffsetDateTime(),
                    dayEnd(date).toOffsetDateTime(),
                    DAILY_CAPACITY,
                    remainingCapacity,
                    remainingCapacity > 0);
            })
            .toList();

        return new AppointmentAvailabilityResponse(TIME_ZONE.getId(), DAILY_CAPACITY, days);
    }

    public Instant validateAndNormalize(LocalDate visitDate) {
        ZonedDateTime now = ZonedDateTime.now(TIME_ZONE);
        LocalDate today = now.toLocalDate();

        if (!visitDate.isAfter(today)) {
            throw invalidVisitDate("Dzień wizyty musi przypadać w przyszłości.");
        }
        if (visitDate.isAfter(today.plusDays(BOOKING_HORIZON_DAYS))) {
            throw invalidVisitDate("Dzień wizyty musi mieścić się w ciągu najbliższych 30 dni.");
        }
        if (!isWorkingDay(visitDate)) {
            throw invalidVisitDate("Wizyty można umawiać od poniedziałku do piątku.");
        }
        return dayStart(visitDate).truncatedTo(ChronoUnit.SECONDS).toInstant();
    }

    public LocalDate visitDate(Instant startAt) {
        return dateOf(startAt);
    }

    public boolean isFull(Instant startAt) {
        LocalDate date = dateOf(startAt);
        Instant rangeStart = date.atStartOfDay(TIME_ZONE).toInstant();
        Instant rangeEnd = date.plusDays(1).atStartOfDay(TIME_ZONE).toInstant();
        return appointments.findBlockingStarts(BLOCKING_STATUSES, rangeStart, rangeEnd)
            .size() >= DAILY_CAPACITY;
    }

    private LocalDate dateOf(Instant startAt) {
        return startAt.atZone(TIME_ZONE).toLocalDate();
    }

    private ZonedDateTime dayStart(LocalDate date) {
        return date.atTime(WORKDAY_START).atZone(TIME_ZONE);
    }

    private ZonedDateTime dayEnd(LocalDate date) {
        return date.atTime(WORKDAY_END).atZone(TIME_ZONE);
    }

    private boolean isWorkingDay(LocalDate date) {
        return date.getDayOfWeek() != DayOfWeek.SATURDAY
            && date.getDayOfWeek() != DayOfWeek.SUNDAY;
    }

    private AppointmentValidationException invalidVisitDate(String message) {
        return new AppointmentValidationException(java.util.Map.of("visitDate", message));
    }
}
