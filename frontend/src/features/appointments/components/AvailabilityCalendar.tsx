import { useMemo, useState } from 'react'
import type { AppointmentAvailability, AppointmentSlot } from '../types'
import {
  appointmentDateKey,
  formatAppointmentDate,
  formatAppointmentTime,
  mondayKey,
} from '../dateTime'

interface Props {
  availability: AppointmentAvailability | null
  isLoading: boolean
  error: string | null
  selectedStartAt: string
  onSelect: (slot: AppointmentSlot) => void
  onRetry: () => void
  fieldError?: string
}

interface CalendarWeek {
  key: string
  days: Array<{ key: string; slots: AppointmentSlot[] }>
}

function groupIntoWeeks(availability: AppointmentAvailability | null): CalendarWeek[] {
  if (!availability) return []
  const days = new Map<string, AppointmentSlot[]>()
  for (const slot of [...availability.slots].sort((first, second) => first.startAt.localeCompare(second.startAt))) {
    const dayKey = appointmentDateKey(slot.startAt, availability.timeZone)
    days.set(dayKey, [...(days.get(dayKey) ?? []), slot])
  }

  const weeks = new Map<string, CalendarWeek['days']>()
  for (const [dayKey, slots] of days) {
    const weekKey = mondayKey(dayKey)
    weeks.set(weekKey, [...(weeks.get(weekKey) ?? []), { key: dayKey, slots }])
  }
  return [...weeks].map(([key, weekDays]) => ({ key, days: weekDays }))
}

export function AvailabilityCalendar({
  availability,
  isLoading,
  error,
  selectedStartAt,
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
          <h3 id="availability-heading">Wybierz termin</h3>
          <p className="muted">
            Godziny warsztatu są podane w strefie {timeZone}.
            {availability && ` Jeden termin trwa ${availability.slotDurationMinutes} min.`}
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

      {isLoading && <p role="status">Pobieranie wolnych terminów…</p>}
      {!isLoading && error && (
        <div className="message error" role="alert">
          <p>{error}</p>
          <button type="button" className="button secondary" onClick={onRetry}>Spróbuj ponownie</button>
        </div>
      )}
      {!isLoading && !error && weeks.length === 0 && (
        <p className="empty-state">Obecnie nie ma terminów dostępnych do rezerwacji.</p>
      )}
      {!isLoading && !error && week && (
        <>
          <div className="calendar-days">
            {week.days.map((day) => (
              <section className="calendar-day" key={day.key}>
                <h4>{formatAppointmentDate(day.slots[0].startAt, timeZone)}</h4>
                <div className="calendar-slots">
                  {day.slots.map((slot) => {
                    const selected = slot.startAt === selectedStartAt
                    const label = `${formatAppointmentTime(slot.startAt, timeZone)}–${formatAppointmentTime(slot.endAt, timeZone)}`
                    return (
                      <button key={slot.startAt} type="button"
                        className={'calendar-slot' + (selected ? ' selected' : '')}
                        disabled={!slot.available} aria-pressed={selected}
                        aria-label={`${formatAppointmentDate(slot.startAt, timeZone)}, ${label}${slot.available ? '' : ', termin niedostępny'}`}
                        onClick={() => onSelect(slot)}>
                        {label}
                      </button>
                    )
                  })}
                </div>
              </section>
            ))}
          </div>
          <p className="calendar-legend muted">Przekreślone godziny są już niedostępne.</p>
        </>
      )}
      {fieldError && <small className="field-error calendar-error" role="alert">{fieldError}</small>}
    </div>
  )
}
