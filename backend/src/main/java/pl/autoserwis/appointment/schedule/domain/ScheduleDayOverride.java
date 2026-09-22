package pl.autoserwis.appointment.schedule.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "schedule_day_overrides")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ScheduleDayOverride {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "date", nullable = false, unique = true)
    private LocalDate date;

    @Column(name = "capacity", nullable = false)
    private int capacity;

    @Column(name = "closed", nullable = false)
    private boolean closed;

    @Column(name = "note", length = 200)
    private String note;

    public ScheduleDayOverride(LocalDate date, int capacity, boolean closed, String note) {
        this.date = date;
        update(capacity, closed, note);
    }

    public void update(int capacity, boolean closed, String note) {
        this.capacity = closed ? 0 : capacity;
        this.closed = closed;
        this.note = note == null || note.isBlank() ? null : note.strip();
    }
}
