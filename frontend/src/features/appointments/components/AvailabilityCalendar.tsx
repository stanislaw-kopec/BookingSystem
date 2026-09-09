import { useMemo, useState } from 'react'
import type { AppointmentAvailability, AppointmentDay } from '../types'
import {
  formatAppointmentDate,
  mondayKey,
} from '../dateTime'

interface Props {
  availability: AppointmentAvailability | null
  isLoading: boolean
  error: string | null
  selectedVisitDate: string
  onSelect: (day: AppointmentDay) => void
  onRetry: () => void
  fieldError?: string
}

interface CalendarWeek {
  key: string
  days: AppointmentDay[]
}

function groupIntoWeeks(availability: AppointmentAvailability | null): CalendarWeek[] {
  if (!availability) return []
  const weeks = new Map<string, CalendarWeek['days']>()
  for (const day of [...availability.days].sort((first, second) => first.date.localeCompare(second.date))) {
    const weekKey = mondayKey(day.date)
    weeks.set(weekKey, [...(weeks.get(weekKey) ?? []), day])
  }
  return [...weeks].map(([key, weekDays]) => ({ key, days: weekDays }))
}

export function AvailabilityCalendar({
  availability,
  isLoading,
  error,
  selectedVisitDate,
  onSelect,
  onRetry,
  fieldError,
}: Props) {
  const weeks = useMemo(() => groupIntoWeeks(availability), [availability])
  const [weekIndex, setWeekIndex] = useState(0)
  const safeWeekIndex = Math.min(weekIndex, Math.max(weeks.length - 1, 0))
  const week = weeks[safeWeekIndex]
  const timeZone = availability?.timeZone ?? 'Europe/Warsaw'

  return (
    <div className="availability-calendar" aria-labelledby="availability-heading">
      <div className="calendar-heading">
        <div>
          <h3 id="availability-heading">Wybierz dzień</h3>
          <p className="muted">
            Wybierz dzień przyjęcia samochodu. Auto możesz zostawić rano albo po wcześniejszym uzgodnieniu dzień wcześniej.
            {availability && ` Warsztat przyjmuje bazowo ${availability.dailyCapacity} auta dziennie.`}
          </p>
        </div>
        {weeks.length > 0 && (
          <div className="calendar-navigation" aria-label="Zmiana tygodnia">
            <button type="button" className="button secondary small" disabled={safeWeekIndex === 0}
              onClick={() => setWeekIndex(Math.max(0, safeWeekIndex - 1))}>
              ← Poprzedni
            </button>
            <span>Tydzień {safeWeekIndex + 1} z {weeks.length}</span>
            <button type="button" className="button secondary small"
              disabled={safeWeekIndex >= weeks.length - 1}
              onClick={() => setWeekIndex(Math.min(weeks.length - 1, safeWeekIndex + 1))}>
              Następny →
            </button>
          </div>
        )}
      </div>

      {isLoading && <p role="status">Pobieranie wolnych dni…</p>}
      {!isLoading && error && (
        <div className="message error" role="alert">
          <p>{error}</p>
          <button type="button" className="button secondary" onClick={onRetry}>Spróbuj ponownie</button>
        </div>
      )}
      {!isLoading && !error && weeks.length === 0 && (
        <p className="empty-state">Obecnie nie ma dni dostępnych do rezerwacji.</p>
      )}
      {!isLoading && !error && week && (
        <>
          <div className="calendar-days">
            {week.days.map((day) => (
              <section className="calendar-day" key={day.date}>
                <h4>{formatAppointmentDate(day.startAt, timeZone)}</h4>
                <button type="button"
                  className={'calendar-day-choice' + (day.date === selectedVisitDate ? ' selected' : '')}
                  disabled={!day.available} aria-pressed={day.date === selectedVisitDate}
                  aria-label={`${formatAppointmentDate(day.startAt, timeZone)}, ${day.remainingCapacity} wolnych miejsc z ${day.capacity}${day.available ? '' : ', dzień niedostępny'}`}
                  onClick={() => onSelect(day)}>
                  <strong>{day.available ? 'Wybierz dzień' : 'Brak miejsc'}</strong>
                  <span>{day.remainingCapacity} z {day.capacity} wolnych miejsc</span>
                </button>
              </section>
            ))}
          </div>
          <p className="calendar-legend muted">Dni bez wolnych miejsc są niedostępne do rezerwacji.</p>
        </>
      )}
      {fieldError && <small className="field-error calendar-error" role="alert">{fieldError}</small>}
    </div>
  )
}
