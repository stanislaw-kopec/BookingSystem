package pl.autoserwis.appointment.persistence;

import java.time.LocalDate;

public interface DailyAppointmentCount {
    LocalDate getVisitDate();
    long getOccupied();
}
