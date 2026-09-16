import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { errorMessage } from '../../../api/apiClient'
import { getStaffSchedule } from '../api/appointmentsApi'
import { appointmentDateKey, mondayKey } from '../dateTime'
import type { StaffSchedule } from '../types'
import { AppointmentStatusBadge } from './AppointmentStatusBadge'
import '../appointments.css'

function currentWeekStart() {
  return mondayKey(appointmentDateKey(new Date().toISOString(), 'Europe/Warsaw'))
}

function addDays(dateKey: string, days: number) {
  const date = new Date(`${dateKey}T00:00:00Z`)
  date.setUTCDate(date.getUTCDate() + days)
  return date.toISOString().slice(0, 10)
}

function scheduleDayLabel(dateKey: string) {
  return new Intl.DateTimeFormat('pl-PL', {
    weekday: 'long', day: '2-digit', month: '2-digit', timeZone: 'UTC',
  }).format(new Date(`${dateKey}T00:00:00Z`))
}

function scheduleWeekLabel(firstDayKey: string) {
  const formatter = new Intl.DateTimeFormat('pl-PL', {
    day: '2-digit', month: 'long', year: 'numeric', timeZone: 'UTC',
  })
  return `${formatter.format(new Date(`${firstDayKey}T00:00:00Z`))} – ${formatter.format(new Date(`${addDays(firstDayKey, 6)}T00:00:00Z`))}`
}

export function StaffScheduleSection() {
  const [schedule, setSchedule] = useState<StaffSchedule | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [revision, setRevision] = useState(0)
  const [selectedWeekStart, setSelectedWeekStart] = useState(currentWeekStart)

  useEffect(() => {
    const controller = new AbortController()
    getStaffSchedule(selectedWeekStart, addDays(selectedWeekStart, 6), controller.signal)
      .then((result) => {
        if (!controller.signal.aborted) setSchedule(result)
      })
      .catch((cause: unknown) => {
        if (!controller.signal.aborted) setError(errorMessage(cause))
      })
      .finally(() => {
        if (!controller.signal.aborted) setIsLoading(false)
      })
    return () => controller.abort()
  }, [selectedWeekStart, revision])

  function reload(weekStart = selectedWeekStart) {
    setSchedule(null)
    setError(null)
    setIsLoading(true)
    setSelectedWeekStart(weekStart)
    setRevision((value) => value + 1)
  }

  const appointmentCount = schedule?.days.reduce((total, day) => total + day.appointments.length, 0) ?? 0

  return (
    <section className="page-section appointments-section staff-schedule-section" aria-labelledby="staff-schedule-heading">
      <div className="section-heading staff-schedule-heading">
        <div>
          <p className="eyebrow">Panel warsztatu</p>
          <h2 id="staff-schedule-heading">Grafik</h2>
          <p className="muted">Tygodniowy widok przyjęć aut z uwzględnieniem dni zamkniętych i wyjątków.</p>
        </div>
        <Link className="button secondary" to="/staff/appointments">Przejdź do listy zgłoszeń</Link>
      </div>
      <div className="staff-schedule-toolbar" aria-label="Zmiana tygodnia">
        <button type="button" className="button secondary" onClick={() => reload(addDays(selectedWeekStart, -7))}>
          Poprzedni tydzień
        </button>
        <div>
          <h3>{scheduleWeekLabel(selectedWeekStart)}</h3>
          {schedule && <p className="muted">{appointmentCount} aktywnych zgłoszeń w tym tygodniu</p>}
        </div>
        <button type="button" className="button secondary" onClick={() => reload(addDays(selectedWeekStart, 7))}>
          Następny tydzień
        </button>
        <button type="button" className="button secondary" onClick={() => reload()} disabled={isLoading}>Odśwież</button>
      </div>
      {isLoading && <p role="status">Ładowanie grafiku…</p>}
      {error && (
        <div className="message error" role="alert">
          <p>{error}</p>
          <button type="button" className="button secondary" onClick={() => reload()}>Spróbuj ponownie</button>
        </div>
      )}
      {schedule && (
        <>
          <div className="staff-schedule-legend" aria-label="Legenda statusów">
            <AppointmentStatusBadge status="PENDING" />
            <AppointmentStatusBadge status="TIME_PROPOSED" />
            <AppointmentStatusBadge status="CONFIRMED" />
          </div>
          <div className="staff-schedule-days">
            {schedule.days.map((day) => (
              <section className={`staff-schedule-day${day.closed ? ' closed' : ''}`} key={day.date}>
                <header className="staff-schedule-day-header">
                  <div>
                    <h3>{scheduleDayLabel(day.date)}</h3>
                    <p className="muted">{day.appointments.length} z {day.capacity} miejsc zajęte</p>
                  </div>
                  <span className={`daily-capacity ${day.closed ? 'closed' : day.remainingCapacity > 0 ? 'open' : 'full'}`}>
                    {day.closed ? 'Zamknięte' : day.remainingCapacity > 0 ? `${day.remainingCapacity} wolne` : 'Brak miejsc'}
                  </span>
                </header>
                {day.appointments.length === 0 ? (
                  <p className="free-day">{day.closed ? 'Warsztat nie przyjmuje aut w tym dniu.' : 'Brak aktywnych zgłoszeń na ten dzień.'}</p>
                ) : (
                  <div className="staff-schedule-card-list">
                    {day.appointments.map((appointment) => (
                      <Link className={`staff-schedule-card schedule-status-${appointment.status.toLowerCase()}`}
                        to={`/staff/appointments/${appointment.id}`} key={appointment.id}
                        aria-label={`Otwórz zgłoszenie ${appointment.vehicleMake} ${appointment.vehicleModel}, ${appointment.vehicleRegistrationNumber}`}>
                        <div className="staff-schedule-card-header">
                          <strong>{appointment.vehicleMake} {appointment.vehicleModel}</strong>
                          <span>{appointment.reference.slice(0, 8).toUpperCase()}</span>
                        </div>
                        <AppointmentStatusBadge status={appointment.status} />
                        <p>{appointment.firstName} {appointment.lastName}</p>
                        <p className="muted">{appointment.vehicleRegistrationNumber}</p>
                        <p className="staff-schedule-problem">{appointment.problemSummary}</p>
                      </Link>
                    ))}
                  </div>
                )}
              </section>
            ))}
          </div>
          {appointmentCount === 0 && <p className="empty-state">W wybranym tygodniu nie ma aktywnych zgłoszeń w grafiku.</p>}
          <p className="muted staff-schedule-note">
            Grafik pokazuje dni przyjęcia auta w strefie {schedule.timeZone}. Pozostałe zgłoszenia są dostępne na liście zgłoszeń.
          </p>
        </>
      )}
    </section>
  )
}
