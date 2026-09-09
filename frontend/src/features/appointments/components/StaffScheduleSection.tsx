import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { errorMessage } from '../../../api/apiClient'
import * as appointmentsApi from '../api/appointmentsApi'
import { appointmentDateKey, formatAppointmentTime, mondayKey } from '../dateTime'
import type { Appointment } from '../types'
import { AppointmentStatusBadge } from './AppointmentStatusBadge'
import '../appointments.css'

const scheduleTimeZone = 'Europe/Warsaw'
const workHours = [8, 9, 10, 11, 12, 13, 14, 15]
const activeStatuses = new Set<Appointment['status']>(['PENDING', 'TIME_PROPOSED', 'CONFIRMED'])

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
    weekday: 'short',
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

function scheduleTimeKey(value: string) {
  return formatAppointmentTime(value, scheduleTimeZone)
}

function slotKey(dayKey: string, hour: number) {
  return `${dayKey}|${String(hour).padStart(2, '0')}:00`
}

function sortAppointments(appointments: Appointment[]) {
  return [...appointments].sort((first, second) => {
    const byTime = first.currentStartAt.localeCompare(second.currentStartAt)
    const byStatus = statusPriority[first.status] - statusPriority[second.status]
    return byTime || byStatus || first.createdAt.localeCompare(second.createdAt)
  })
}

function shortReference(reference: string) {
  return reference.slice(0, 8).toUpperCase()
}

export function StaffScheduleSection() {
  const [appointments, setAppointments] = useState<Appointment[] | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [revision, setRevision] = useState(0)
  const [selectedWeekStart, setSelectedWeekStart] = useState(() => mondayKey(currentDateKey()))

  const weekDayKeys = useMemo(() =>
    [0, 1, 2, 3, 4].map((offset) => addDays(selectedWeekStart, offset)),
  [selectedWeekStart])

  const appointmentsBySlot = useMemo(() => {
    const grouped = new Map<string, Appointment[]>()
    for (const appointment of appointments ?? []) {
      if (!activeStatuses.has(appointment.status)) continue
      const dayKey = appointmentDateKey(appointment.currentStartAt, scheduleTimeZone)
      if (!weekDayKeys.includes(dayKey)) continue
      const key = `${dayKey}|${scheduleTimeKey(appointment.currentStartAt)}`
      grouped.set(key, sortAppointments([...(grouped.get(key) ?? []), appointment]))
    }
    return grouped
  }, [appointments, weekDayKeys])

  const visibleAppointments = useMemo(() =>
    sortAppointments([...appointmentsBySlot.values()].flat()),
  [appointmentsBySlot])

  useEffect(() => {
    const controller = new AbortController()
    appointmentsApi.getStaffAppointments(controller.signal)
      .then((result) => {
        if (!controller.signal.aborted) {
          setAppointments(sortAppointments(result))
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
          <p className="muted">Tygodniowy widok aktywnych zgłoszeń w godzinach pracy warsztatu.</p>
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
          <div className="staff-schedule-scroll">
            <table className="staff-schedule-grid">
              <thead>
                <tr>
                  <th scope="col">Godzina</th>
                  {weekDayKeys.map((dayKey) => (
                    <th scope="col" key={dayKey}>{scheduleDayLabel(dayKey)}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {workHours.map((hour) => (
                  <tr key={hour}>
                    <th scope="row">{String(hour).padStart(2, '0')}:00</th>
                    {weekDayKeys.map((dayKey) => {
                      const slotAppointments = appointmentsBySlot.get(slotKey(dayKey, hour)) ?? []
                      return (
                        <td className={slotAppointments.length ? 'occupied-slot' : 'free-slot'} key={dayKey}>
                          {slotAppointments.length === 0 ? (
                            <span>Wolne</span>
                          ) : (
                            slotAppointments.map((appointment) => (
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
                            ))
                          )}
                        </td>
                      )
                    })}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          {visibleAppointments.length === 0 && (
            <p className="empty-state">W wybranym tygodniu nie ma aktywnych zgłoszeń w grafiku.</p>
          )}
          <p className="muted staff-schedule-note">
            Grafik pokazuje aktualne terminy zgłoszeń. Odrzucone i odwołane zgłoszenia są dostępne na liście zgłoszeń.
          </p>
        </>
      )}
    </section>
  )
}
