import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { errorMessage } from '../../../api/apiClient'
import * as appointmentsApi from '../api/appointmentsApi'
import { appointmentDateKey, mondayKey } from '../dateTime'
import type { Appointment } from '../types'
import { AppointmentStatusBadge } from './AppointmentStatusBadge'
import '../appointments.css'

const scheduleTimeZone = 'Europe/Warsaw'
const activeStatuses = new Set<Appointment['status']>(['PENDING', 'TIME_PROPOSED', 'CONFIRMED'])
const defaultDailyCapacity = 4

const statusPriority = {
  PENDING: 0,
  TIME_PROPOSED: 1,
  CONFIRMED: 2,
  CANCELLED: 3,
  REJECTED: 4,
} as const

function currentDateKey() {
  return appointmentDateKey(new Date().toISOString(), scheduleTimeZone)
}

function addDays(dateKey: string, days: number) {
  const date = new Date(`${dateKey}T00:00:00Z`)
  date.setUTCDate(date.getUTCDate() + days)
  return date.toISOString().slice(0, 10)
}

function scheduleDayLabel(dateKey: string) {
  return new Intl.DateTimeFormat('pl-PL', {
    weekday: 'long',
    day: '2-digit',
    month: '2-digit',
    timeZone: 'UTC',
  }).format(new Date(`${dateKey}T00:00:00Z`))
}

function scheduleWeekLabel(firstDayKey: string) {
  const lastDayKey = addDays(firstDayKey, 4)
  const formatter = new Intl.DateTimeFormat('pl-PL', {
    day: '2-digit',
    month: 'long',
    year: 'numeric',
    timeZone: 'UTC',
  })
  return `${formatter.format(new Date(`${firstDayKey}T00:00:00Z`))} - ${formatter.format(new Date(`${lastDayKey}T00:00:00Z`))}`
}

function sortAppointments(appointments: Appointment[]) {
  return [...appointments].sort((first, second) => {
    const byDate = first.currentStartAt.localeCompare(second.currentStartAt)
    const byStatus = statusPriority[first.status] - statusPriority[second.status]
    return byDate || byStatus || first.createdAt.localeCompare(second.createdAt)
  })
}

function shortReference(reference: string) {
  return reference.slice(0, 8).toUpperCase()
}

export function StaffScheduleSection() {
  const [appointments, setAppointments] = useState<Appointment[] | null>(null)
  const [dailyCapacity, setDailyCapacity] = useState(defaultDailyCapacity)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [revision, setRevision] = useState(0)
  const [selectedWeekStart, setSelectedWeekStart] = useState(() => mondayKey(currentDateKey()))

  const weekDayKeys = useMemo(() =>
    [0, 1, 2, 3, 4].map((offset) => addDays(selectedWeekStart, offset)),
  [selectedWeekStart])

  const appointmentsByDay = useMemo(() => {
    const grouped = new Map<string, Appointment[]>()
    for (const appointment of appointments ?? []) {
      if (!activeStatuses.has(appointment.status)) continue
      const dayKey = appointmentDateKey(appointment.currentStartAt, scheduleTimeZone)
      if (!weekDayKeys.includes(dayKey)) continue
      grouped.set(dayKey, sortAppointments([...(grouped.get(dayKey) ?? []), appointment]))
    }
    return grouped
  }, [appointments, weekDayKeys])

  const visibleAppointments = useMemo(() =>
    sortAppointments([...appointmentsByDay.values()].flat()),
  [appointmentsByDay])

  useEffect(() => {
    const controller = new AbortController()
    Promise.all([
      appointmentsApi.getStaffAppointments(controller.signal),
      appointmentsApi.getAvailability(controller.signal),
    ])
      .then(([staffAppointments, availability]) => {
        if (!controller.signal.aborted) {
          setAppointments(sortAppointments(staffAppointments))
          setDailyCapacity(availability.dailyCapacity)
          setError(null)
        }
      })
      .catch((cause: unknown) => {
        if (!controller.signal.aborted) setError(errorMessage(cause))
      })
      .finally(() => {
        if (!controller.signal.aborted) setIsLoading(false)
      })
    return () => controller.abort()
  }, [revision])

  function retry() {
    setIsLoading(true)
    setError(null)
    setRevision((value) => value + 1)
  }

  return (
    <section className="page-section appointments-section staff-schedule-section" aria-labelledby="staff-schedule-heading">
      <div className="section-heading staff-schedule-heading">
        <div>
          <p className="eyebrow">Panel warsztatu</p>
          <h2 id="staff-schedule-heading">Grafik</h2>
          <p className="muted">Tygodniowy widok dni przyjęć samochodów i aktywnych zgłoszeń.</p>
        </div>
        <Link className="button secondary" to="/staff/appointments">Przejdź do listy zgłoszeń</Link>
      </div>

      <div className="staff-schedule-toolbar" aria-label="Zmiana tygodnia">
        <button type="button" className="button secondary" onClick={() => setSelectedWeekStart(addDays(selectedWeekStart, -7))}>
          Poprzedni tydzień
        </button>
        <div>
          <h3>{scheduleWeekLabel(selectedWeekStart)}</h3>
          <p className="muted">{visibleAppointments.length} aktywnych zgłoszeń w tym tygodniu</p>
        </div>
        <button type="button" className="button secondary" onClick={() => setSelectedWeekStart(addDays(selectedWeekStart, 7))}>
          Następny tydzień
        </button>
      </div>

      {isLoading && <p role="status">Ładowanie grafiku…</p>}
      {!isLoading && error && appointments === null && (
        <div className="message error" role="alert">
          <p>{error}</p>
          <button type="button" className="button secondary" onClick={retry}>Spróbuj ponownie</button>
        </div>
      )}

      {appointments && (
        <>
          <div className="staff-schedule-legend" aria-label="Legenda statusów">
            <AppointmentStatusBadge status="PENDING" />
            <AppointmentStatusBadge status="TIME_PROPOSED" />
            <AppointmentStatusBadge status="CONFIRMED" />
          </div>
          <div className="staff-schedule-days">
            {weekDayKeys.map((dayKey) => {
              const dayAppointments = appointmentsByDay.get(dayKey) ?? []
              const remainingCapacity = Math.max(dailyCapacity - dayAppointments.length, 0)
              return (
                <section className="staff-schedule-day" key={dayKey}>
                  <header className="staff-schedule-day-header">
                    <div>
                      <h3>{scheduleDayLabel(dayKey)}</h3>
                      <p className="muted">{dayAppointments.length} z {dailyCapacity} miejsc zajęte</p>
                    </div>
                    <span className={remainingCapacity > 0 ? 'daily-capacity open' : 'daily-capacity full'}>
                      {remainingCapacity > 0 ? `${remainingCapacity} wolne` : 'Brak miejsc'}
                    </span>
                  </header>

                  {dayAppointments.length === 0 ? (
                    <p className="free-day">Brak aktywnych zgłoszeń na ten dzień.</p>
                  ) : (
                    <div className="staff-schedule-card-list">
                      {dayAppointments.map((appointment) => (
                        <article className={`staff-schedule-card schedule-status-${appointment.status.toLowerCase()}`}
                          key={appointment.id}>
                          <div className="staff-schedule-card-header">
                            <strong>{appointment.vehicleMake} {appointment.vehicleModel}</strong>
                            <span>{shortReference(appointment.reference)}</span>
                          </div>
                          <AppointmentStatusBadge status={appointment.status} />
                          <p>{appointment.firstName} {appointment.lastName}</p>
                          <p className="muted">{appointment.vehicleRegistrationNumber}</p>
                          <p className="staff-schedule-problem">{appointment.problemDescription}</p>
                        </article>
                      ))}
                    </div>
                  )}
                </section>
              )
            })}
          </div>
          {visibleAppointments.length === 0 && (
            <p className="empty-state">W wybranym tygodniu nie ma aktywnych zgłoszeń w grafiku.</p>
          )}
          <p className="muted staff-schedule-note">
            Grafik pokazuje dni przyjęcia auta. Odrzucone i odwołane zgłoszenia są dostępne na liście zgłoszeń.
          </p>
        </>
      )}
    </section>
  )
}
