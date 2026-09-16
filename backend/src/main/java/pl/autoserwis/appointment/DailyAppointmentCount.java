package pl.autoserwis.appointment;

import java.time.LocalDate;

public interface DailyAppointmentCount {
    LocalDate getVisitDate();
    long getOccupied();
}
