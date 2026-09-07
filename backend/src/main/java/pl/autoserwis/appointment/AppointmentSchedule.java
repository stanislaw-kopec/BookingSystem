package pl.autoserwis.appointment;

import org.springframework.stereotype.Component;
import pl.autoserwis.appointment.dto.AppointmentAvailabilityResponse;
import pl.autoserwis.appointment.dto.AppointmentSlotResponse;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

@Component
public class AppointmentSchedule {
    public static final ZoneId TIME_ZONE = ZoneId.of("Europe/Warsaw");
    public static final LocalTime WORKDAY_START = LocalTime.of(8, 0);
    public static final LocalTime WORKDAY_END = LocalTime.of(16, 0);
    public static final int SLOT_DURATION_MINUTES = 60;
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
        Set<Instant> occupiedStarts = new HashSet<>(appointments.findOccupiedStarts(
            BLOCKING_STATUSES, rangeStart, rangeEnd));

        List<AppointmentSlotResponse> slots = IntStream.rangeClosed(0, BOOKING_HORIZON_DAYS)
            .mapToObj(firstDate::plusDays)
            .filter(this::isWorkingDay)
            .flatMap(date -> slotsFor(date).stream())
            .filter(start -> start.isAfter(now))
            .map(start -> new AppointmentSlotResponse(
                start.toOffsetDateTime(),
                start.plusMinutes(SLOT_DURATION_MINUTES).toOffsetDateTime(),
                !occupiedStarts.contains(start.toInstant())))
            .toList();

        return new AppointmentAvailabilityResponse(TIME_ZONE.getId(), SLOT_DURATION_MINUTES, slots);
    }

    public Instant validateAndNormalize(OffsetDateTime requestedStart) {
        ZonedDateTime now = ZonedDateTime.now(TIME_ZONE);
        ZonedDateTime localStart = requestedStart.atZoneSameInstant(TIME_ZONE);
        LocalDate today = now.toLocalDate();

        if (!localStart.isAfter(now)) {
            throw invalidSlot("Termin musi przypadać w przyszłości.");
        }
        if (localStart.toLocalDate().isAfter(today.plusDays(BOOKING_HORIZON_DAYS))) {
            throw invalidSlot("Termin musi mieścić się w ciągu najbliższych 30 dni.");
        }
        if (!isWorkingDay(localStart.toLocalDate())) {
            throw invalidSlot("Wizyty można umawiać od poniedziałku do piątku.");
        }
        if (localStart.getMinute() != 0 || localStart.getSecond() != 0
                || localStart.getNano() != 0) {
            throw invalidSlot("Wybierz pełny godzinny termin z kalendarza.");
        }
        LocalTime time = localStart.toLocalTime();
        if (time.isBefore(WORKDAY_START)
                || time.plusMinutes(SLOT_DURATION_MINUTES).isAfter(WORKDAY_END)) {
            throw invalidSlot("Wybierz termin między 08:00 a 16:00.");
        }
        return localStart.truncatedTo(ChronoUnit.SECONDS).toInstant();
    }

    public boolean isOccupied(Instant startAt) {
        return !appointments.findOccupiedStarts(BLOCKING_STATUSES, startAt,
            startAt.plusSeconds(1)).isEmpty();
    }

    private List<ZonedDateTime> slotsFor(LocalDate date) {
        int slots = (int) (Duration.between(WORKDAY_START, WORKDAY_END).toMinutes()
            / SLOT_DURATION_MINUTES);
        return IntStream.range(0, slots)
            .mapToObj(index -> date.atTime(WORKDAY_START)
                .plusMinutes((long) index * SLOT_DURATION_MINUTES)
                .atZone(TIME_ZONE))
            .toList();
    }

    private boolean isWorkingDay(LocalDate date) {
        return date.getDayOfWeek() != DayOfWeek.SATURDAY
            && date.getDayOfWeek() != DayOfWeek.SUNDAY;
    }

    private AppointmentValidationException invalidSlot(String message) {
        return new AppointmentValidationException(java.util.Map.of("slotStartAt", message));
    }
}
