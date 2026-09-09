const dateFormatterCache = new Map<string, Intl.DateTimeFormat>()

function dateFormatter(timeZone: string) {
  let formatter = dateFormatterCache.get(timeZone)
  if (!formatter) {
    formatter = new Intl.DateTimeFormat('pl-PL', {
      timeZone,
      weekday: 'long',
      day: 'numeric',
      month: 'long',
    })
    dateFormatterCache.set(timeZone, formatter)
  }
  return formatter
}

export function formatAppointmentDate(value: string, timeZone = 'Europe/Warsaw') {
  return dateFormatter(timeZone).format(new Date(value))
}

export function formatAppointmentDay(dateKey: string, timeZone = 'Europe/Warsaw') {
  return formatAppointmentDate(`${dateKey}T00:00:00Z`, timeZone)
}

export function appointmentDateKey(value: string, timeZone: string) {
  const parts = new Intl.DateTimeFormat('en-CA', {
    timeZone,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).formatToParts(new Date(value))
  const part = (type: Intl.DateTimeFormatPartTypes) => parts.find((item) => item.type === type)?.value ?? ''
  return `${part('year')}-${part('month')}-${part('day')}`
}

export function mondayKey(dateKey: string) {
  const date = new Date(`${dateKey}T00:00:00Z`)
  const daysFromMonday = (date.getUTCDay() + 6) % 7
  date.setUTCDate(date.getUTCDate() - daysFromMonday)
  return date.toISOString().slice(0, 10)
}
