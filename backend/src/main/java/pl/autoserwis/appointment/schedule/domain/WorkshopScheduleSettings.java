package pl.autoserwis.appointment.schedule.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Entity
@Table(name = "workshop_schedule_settings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorkshopScheduleSettings {
    public static final long SINGLETON_ID = 1L;

    @Id
    private Long id;

    @Column(name = "default_daily_capacity", nullable = false)
    private int defaultDailyCapacity;

    @Column(name = "booking_horizon_days", nullable = false)
    private int bookingHorizonDays;

    @Column(name = "workday_start", nullable = false)
    private LocalTime workdayStart;

    @Column(name = "workday_end", nullable = false)
    private LocalTime workdayEnd;

    public WorkshopScheduleSettings(int defaultDailyCapacity, int bookingHorizonDays,
            LocalTime workdayStart, LocalTime workdayEnd) {
        this.id = SINGLETON_ID;
        update(defaultDailyCapacity, bookingHorizonDays, workdayStart, workdayEnd);
    }

    public void update(int defaultDailyCapacity, int bookingHorizonDays,
            LocalTime workdayStart, LocalTime workdayEnd) {
        this.defaultDailyCapacity = defaultDailyCapacity;
        this.bookingHorizonDays = bookingHorizonDays;
        this.workdayStart = workdayStart;
        this.workdayEnd = workdayEnd;
    }
}
