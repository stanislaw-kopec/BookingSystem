import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { ApiError, errorMessage } from '../../../api/apiClient'
import * as scheduleApi from '../api/scheduleApi'
import type { ScheduleDayOverrideInput, ScheduleSettings, WorkshopScheduleConfig } from '../types'
import '../schedule.css'

const emptyOverride: ScheduleDayOverrideInput = {
  date: '',
  capacity: 4,
  closed: false,
  note: '',
}

export function ScheduleSettingsSection() {
  const [config, setConfig] = useState<WorkshopScheduleConfig | null>(null)
  const [settings, setSettings] = useState<ScheduleSettings | null>(null)
  const [overrideForm, setOverrideForm] = useState<ScheduleDayOverrideInput>(emptyOverride)
  const [isLoading, setIsLoading] = useState(true)
  const [isSaving, setIsSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})

  useEffect(() => {
    const controller = new AbortController()
    scheduleApi.getScheduleConfig(controller.signal)
      .then((result) => {
        if (!controller.signal.aborted) {
          setConfig(result)
          setSettings(normalizeSettings(result.settings))
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
  }, [])

  async function saveSettings(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!settings) return
    await save(() => scheduleApi.updateScheduleSettings(settings), 'Ustawienia grafiku zostały zapisane.')
  }

  async function saveOverride(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    await save(
      () => scheduleApi.saveScheduleOverride({
        ...overrideForm,
        capacity: overrideForm.closed ? 0 : overrideForm.capacity,
      }),
      'Wyjątek dnia został zapisany.',
      () => setOverrideForm(emptyOverride),
    )
  }

  async function deleteOverride(date: string) {
    await save(() => scheduleApi.deleteScheduleOverride(date), 'Wyjątek dnia został usunięty.')
  }

  async function save(
    operation: () => Promise<WorkshopScheduleConfig>,
    successMessage: string,
    onSuccess?: () => void,
  ) {
    setIsSaving(true)
    setError(null)
    setNotice(null)
    setFieldErrors({})
    try {
      const result = await operation()
      setConfig(result)
      setSettings(normalizeSettings(result.settings))
      onSuccess?.()
      setNotice(successMessage)
    } catch (cause) {
      setError(errorMessage(cause))
      if (cause instanceof ApiError) setFieldErrors(cause.fieldErrors)
    } finally {
      setIsSaving(false)
    }
  }

  return (
    <section className="page-section schedule-settings-section" aria-labelledby="schedule-settings-heading">
      <div className="section-heading">
        <p className="eyebrow">Panel administratora</p>
        <h2 id="schedule-settings-heading">Konfiguracja grafiku warsztatu</h2>
        <p className="muted">Ustaw domyślny grafik przyjęć oraz wyjątki dla konkretnych dni, np. święta, urlop albo większą liczbę miejsc.</p>
      </div>

      {isLoading && <p role="status">Ładowanie konfiguracji grafiku…</p>}
      {error && <p className="message error" role="alert">{error}</p>}
      {notice && <p className="message success" role="status">{notice}</p>}

      {config && settings && (
        <div className="schedule-settings-layout">
          <form className="schedule-settings-card" onSubmit={(event) => void saveSettings(event)}>
            <h3>Ustawienia domyślne</h3>
            <p className="muted">Strefa grafiku: {config.timeZone}</p>
            <div className="appointment-form-grid">
              <label className="form-field">
                <span>Domyślna liczba miejsc dziennie</span>
                <input type="number" min="1" max="20" value={settings.defaultDailyCapacity}
                  aria-invalid={Boolean(fieldErrors.defaultDailyCapacity)}
                  onChange={(event) => setSettings({ ...settings, defaultDailyCapacity: Number(event.target.value) })} />
                {fieldErrors.defaultDailyCapacity && <small className="field-error">{fieldErrors.defaultDailyCapacity}</small>}
              </label>
              <label className="form-field">
                <span>Horyzont rezerwacji w dniach</span>
                <input type="number" min="7" max="180" value={settings.bookingHorizonDays}
                  aria-invalid={Boolean(fieldErrors.bookingHorizonDays)}
                  onChange={(event) => setSettings({ ...settings, bookingHorizonDays: Number(event.target.value) })} />
                {fieldErrors.bookingHorizonDays && <small className="field-error">{fieldErrors.bookingHorizonDays}</small>}
              </label>
              <label className="form-field">
                <span>Godzina rozpoczęcia pracy</span>
                <input type="time" value={settings.workdayStart}
                  aria-invalid={Boolean(fieldErrors.workdayStart)}
                  onChange={(event) => setSettings({ ...settings, workdayStart: event.target.value })} />
                {fieldErrors.workdayStart && <small className="field-error">{fieldErrors.workdayStart}</small>}
              </label>
              <label className="form-field">
                <span>Godzina zakończenia pracy</span>
                <input type="time" value={settings.workdayEnd}
                  aria-invalid={Boolean(fieldErrors.workdayEnd)}
                  onChange={(event) => setSettings({ ...settings, workdayEnd: event.target.value })} />
                {fieldErrors.workdayEnd && <small className="field-error">{fieldErrors.workdayEnd}</small>}
              </label>
            </div>
            <button type="submit" className="button" disabled={isSaving}>Zapisz ustawienia</button>
          </form>

          <form className="schedule-settings-card" onSubmit={(event) => void saveOverride(event)}>
            <h3>Wyjątek dla dnia</h3>
            <div className="appointment-form-grid">
              <label className="form-field">
                <span>Dzień</span>
                <input type="date" required value={overrideForm.date}
                  onChange={(event) => setOverrideForm({ ...overrideForm, date: event.target.value })} />
              </label>
              <label className="form-field checkbox-field">
                <input type="checkbox" checked={overrideForm.closed}
                  onChange={(event) => setOverrideForm({ ...overrideForm, closed: event.target.checked })} />
                <span>Dzień zamknięty</span>
              </label>
              {!overrideForm.closed && (
                <label className="form-field">
                  <span>Liczba miejsc w tym dniu</span>
                  <input type="number" min="1" max="20" value={overrideForm.capacity}
                    aria-invalid={Boolean(fieldErrors.capacity)}
                    onChange={(event) => setOverrideForm({ ...overrideForm, capacity: Number(event.target.value) })} />
                  {fieldErrors.capacity && <small className="field-error">{fieldErrors.capacity}</small>}
                </label>
              )}
              <label className="form-field wide-field">
                <span>Notatka</span>
                <input maxLength={200} placeholder="Np. święto, urlop, dodatkowy mechanik" value={overrideForm.note}
                  onChange={(event) => setOverrideForm({ ...overrideForm, note: event.target.value })} />
              </label>
            </div>
            <button type="submit" className="button" disabled={isSaving || !overrideForm.date}>Zapisz wyjątek</button>
          </form>

          <div className="schedule-settings-card wide-card">
            <h3>Zapisane wyjątki</h3>
            {config.overrides.length === 0 ? (
              <p className="empty-state">Nie ma jeszcze wyjątków w aktualnym horyzoncie rezerwacji.</p>
            ) : (
              <ul className="schedule-overrides-list">
                {config.overrides.map((override) => (
                  <li key={override.id}>
                    <div>
                      <strong>{formatDate(override.date)}</strong>
                      <p className="muted">
                        {override.closed ? 'Dzień zamknięty' : `${override.capacity} miejsc`}
                        {override.note && ` · ${override.note}`}
                      </p>
                    </div>
                    <button type="button" className="button secondary" disabled={isSaving}
                      onClick={() => void deleteOverride(override.date)}>
                      Usuń
                    </button>
                  </li>
                ))}
              </ul>
            )}
          </div>
        </div>
      )}
    </section>
  )
}

function normalizeSettings(settings: ScheduleSettings): ScheduleSettings {
  return {
    ...settings,
    workdayStart: settings.workdayStart.slice(0, 5),
    workdayEnd: settings.workdayEnd.slice(0, 5),
  }
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat('pl-PL', {
    timeZone: 'Europe/Warsaw',
    day: 'numeric',
    month: 'long',
    year: 'numeric',
  }).format(new Date(`${value}T12:00:00`))
}
