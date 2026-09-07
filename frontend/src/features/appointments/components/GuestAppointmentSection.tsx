import { useState } from 'react'
import type { FormEvent, HTMLInputAutoCompleteAttribute } from 'react'
import { ApiError, errorMessage } from '../../../api/apiClient'
import * as appointmentsApi from '../api/appointmentsApi'
import { formatAppointmentDateTime } from '../dateTime'
import { useAppointmentAvailability } from '../hooks/useAppointmentAvailability'
import type { Appointment, GuestAppointmentInput } from '../types'
import { AvailabilityCalendar } from './AvailabilityCalendar'
import '../appointments.css'

interface TextFieldProps {
  id: string
  label: string
  value: string
  error?: string
  maxLength: number
  required?: boolean
  type?: 'text' | 'email' | 'tel' | 'number'
  minLength?: number
  pattern?: string
  min?: number
  max?: number
  autoComplete?: HTMLInputAutoCompleteAttribute
  help?: string
  onChange: (value: string) => void
}

const currentYear = new Date().getFullYear()

const initialForm: GuestAppointmentInput = {
  firstName: '',
  lastName: '',
  phoneNumber: '',
  contactEmail: '',
  vehicleMake: '',
  vehicleModel: '',
  vehicleProductionYear: currentYear,
  vehicleRegistrationNumber: '',
  vehicleVin: '',
  slotStartAt: '',
  problemDescription: '',
}

function TextField({ id, label, value, error, maxLength, required = false, type = 'text',
    minLength, pattern, min, max, autoComplete, help, onChange }: TextFieldProps) {
  const describedBy = [help ? `${id}-help` : '', error ? `${id}-error` : ''].filter(Boolean).join(' ') || undefined
  return (
    <div className="form-field">
      <label htmlFor={id}>{label}</label>
      <input id={id} type={type} value={value} required={required} minLength={minLength} maxLength={maxLength}
        pattern={pattern} min={min} max={max} autoComplete={autoComplete}
        aria-invalid={Boolean(error)} aria-describedby={describedBy}
        onChange={(event) => onChange(event.target.value)} />
      {help && <small id={`${id}-help`} className="muted">{help}</small>}
      {error && <small id={`${id}-error`} className="field-error">{error}</small>}
    </div>
  )
}

export function GuestAppointmentSection() {
  const [form, setForm] = useState<GuestAppointmentInput>(initialForm)
  const [productionYear, setProductionYear] = useState(String(currentYear))
  const [isSaving, setIsSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [created, setCreated] = useState<Appointment | null>(null)
  const availability = useAppointmentAvailability()

  function update<K extends keyof GuestAppointmentInput>(field: K, value: GuestAppointmentInput[K]) {
    setForm((current) => ({ ...current, [field]: value }))
    setError(null)
    setFieldErrors((current) => {
      const next = { ...current, [field]: '' }
      if ((field === 'phoneNumber' || field === 'contactEmail') && String(value).trim()) {
        next.phoneNumber = ''
        next.contactEmail = ''
      }
      return next
    })
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const phoneNumber = form.phoneNumber.trim()
    const contactEmail = form.contactEmail.trim()
    if (!phoneNumber && !contactEmail) {
      setError('Podaj przynajmniej jeden sposób kontaktu.')
      setFieldErrors((current) => ({
        ...current,
        phoneNumber: 'Podaj numer telefonu lub adres e-mail.',
        contactEmail: 'Podaj adres e-mail lub numer telefonu.',
      }))
      return
    }

    setIsSaving(true)
    setError(null)
    setFieldErrors({})
    try {
      const result = await appointmentsApi.createGuestAppointment({
        ...form,
        firstName: form.firstName.trim(),
        lastName: form.lastName.trim(),
        phoneNumber,
        contactEmail,
        vehicleMake: form.vehicleMake.trim(),
        vehicleModel: form.vehicleModel.trim(),
        vehicleProductionYear: Number(productionYear),
        vehicleRegistrationNumber: form.vehicleRegistrationNumber.trim(),
        vehicleVin: form.vehicleVin.trim(),
        problemDescription: form.problemDescription.trim(),
      })
      setCreated(result)
      availability.refresh()
    } catch (cause) {
      setError(errorMessage(cause))
      if (cause instanceof ApiError) {
        setFieldErrors(cause.fieldErrors)
        if (cause.status === 409 && cause.fieldErrors.slotStartAt) {
          setForm((current) => ({ ...current, slotStartAt: '' }))
          availability.refresh()
        }
      }
    } finally {
      setIsSaving(false)
    }
  }

  function startAnother() {
    setForm({ ...initialForm, vehicleProductionYear: currentYear })
    setProductionYear(String(currentYear))
    setCreated(null)
    setError(null)
    setFieldErrors({})
  }

  if (created) {
    return (
      <section className="page-section appointments-section" aria-labelledby="guest-success-heading">
        <div className="guest-success">
          <p className="eyebrow">Zgłoszenie przyjęte</p>
          <h2 id="guest-success-heading">Dziękujemy za kontakt</h2>
          <p className="message success" role="status">
            Zgłoszenie zostało wysłane i oczekuje na decyzję warsztatu. Wybrany termin nie jest jeszcze potwierdzony.
          </p>
          <dl className="appointment-facts">
            <div><dt>Numer zgłoszenia</dt><dd>{created.reference}</dd></div>
            <div><dt>Wybrany termin</dt><dd>{formatAppointmentDateTime(created.requestedStartAt)}</dd></div>
          </dl>
          <p>Warsztat skontaktuje się z Tobą telefonicznie lub mailowo.</p>
          <button type="button" className="button secondary" onClick={startAnother}>Wyślij kolejne zgłoszenie</button>
        </div>
      </section>
    )
  }

  return (
    <section className="page-section appointments-section" aria-labelledby="guest-booking-heading">
      <div className="section-heading">
        <p className="eyebrow">Zgłoszenie bez konta</p>
        <h2 id="guest-booking-heading">Umów wizytę</h2>
        <p className="muted">
          Podaj dane kontaktowe i informacje o samochodzie. Warsztat skontaktuje się z Tobą po sprawdzeniu zgłoszenia.
        </p>
        <p className="guest-account-hint">
          Masz konto klienta? Zaloguj się przyciskiem w prawym górnym rogu, aby wybrać zapisany pojazd i śledzić zgłoszenie.
        </p>
      </div>
      {error && <p className="message error" role="alert">{error}</p>}

      <form className="appointment-form guest-appointment-form" onSubmit={(event) => void handleSubmit(event)}>
        <fieldset disabled={isSaving}>
          <div className="appointment-step">
            <h3>1. Dane kontaktowe</h3>
            <div className="appointment-form-grid">
              <TextField id="guest-first-name" label="Imię" value={form.firstName}
                error={fieldErrors.firstName} maxLength={60} required autoComplete="given-name"
                onChange={(value) => update('firstName', value)} />
              <TextField id="guest-last-name" label="Nazwisko" value={form.lastName}
                error={fieldErrors.lastName} maxLength={80} required autoComplete="family-name"
                onChange={(value) => update('lastName', value)} />
              <TextField id="guest-phone" label="Numer telefonu" value={form.phoneNumber}
                error={fieldErrors.phoneNumber} maxLength={30} type="tel" autoComplete="tel"
                pattern="[0-9+() .-]{7,30}" help="Podaj telefon lub adres e-mail."
                onChange={(value) => update('phoneNumber', value)} />
              <TextField id="guest-email" label="Adres e-mail" value={form.contactEmail}
                error={fieldErrors.contactEmail} maxLength={254} type="email" autoComplete="email"
                help="Podaj e-mail lub numer telefonu."
                onChange={(value) => update('contactEmail', value)} />
            </div>
          </div>

          <div className="appointment-step">
            <h3>2. Dane pojazdu</h3>
            <div className="appointment-form-grid">
              <TextField id="guest-vehicle-make" label="Marka" value={form.vehicleMake}
                error={fieldErrors.vehicleMake} maxLength={80} required
                onChange={(value) => update('vehicleMake', value)} />
              <TextField id="guest-vehicle-model" label="Model" value={form.vehicleModel}
                error={fieldErrors.vehicleModel} maxLength={80} required
                onChange={(value) => update('vehicleModel', value)} />
              <TextField id="guest-vehicle-year" label="Rok produkcji" value={productionYear}
                error={fieldErrors.vehicleProductionYear} maxLength={4} required type="number"
                min={1886} max={currentYear + 1}
                onChange={(value) => {
                  setProductionYear(value)
                  setError(null)
                  setFieldErrors((current) => ({ ...current, vehicleProductionYear: '' }))
                }} />
              <TextField id="guest-vehicle-registration" label="Numer rejestracyjny"
                value={form.vehicleRegistrationNumber} error={fieldErrors.vehicleRegistrationNumber}
                minLength={2} maxLength={20} required pattern="[A-Za-z0-9 -]+"
                onChange={(value) => update('vehicleRegistrationNumber', value)} />
              <div className="wide-field">
                <TextField id="guest-vehicle-vin" label="VIN (opcjonalnie)" value={form.vehicleVin}
                  error={fieldErrors.vehicleVin} minLength={17} maxLength={17}
                  pattern="[A-HJ-NPR-Za-hj-npr-z0-9]{17}" help="17 znaków, bez liter I, O oraz Q."
                  onChange={(value) => update('vehicleVin', value)} />
              </div>
            </div>
          </div>

          <div className="appointment-step">
            <p className="step-label">3. Termin</p>
            <AvailabilityCalendar availability={availability.availability} isLoading={availability.isLoading}
              error={availability.error} selectedStartAt={form.slotStartAt}
              onSelect={(slot) => {
                update('slotStartAt', slot.startAt)
                setError(null)
              }} onRetry={availability.refresh}
              fieldError={fieldErrors.slotStartAt} />
            {form.slotStartAt && (
              <p className="selected-slot" role="status">
                Wybrany termin: <strong>{formatAppointmentDateTime(form.slotStartAt, availability.availability?.timeZone)}</strong>
              </p>
            )}
          </div>

          <div className="appointment-step form-field">
            <label htmlFor="guest-problem-description">4. Opis usterki</label>
            <textarea id="guest-problem-description" rows={6} required minLength={10} maxLength={2000}
              value={form.problemDescription} aria-invalid={Boolean(fieldErrors.problemDescription)}
              aria-describedby={fieldErrors.problemDescription
                ? 'guest-problem-description-help guest-problem-description-error'
                : 'guest-problem-description-help'}
              onChange={(event) => update('problemDescription', event.target.value)} />
            <small id="guest-problem-description-help" className="muted">
              Opisz objawy i okoliczności, w których pojawia się problem.
            </small>
            {fieldErrors.problemDescription && (
              <small id="guest-problem-description-error" className="field-error">{fieldErrors.problemDescription}</small>
            )}
          </div>

          <button type="submit" className="button appointment-submit"
            disabled={!form.slotStartAt || form.problemDescription.trim().length < 10}>
            {isSaving ? 'Wysyłanie…' : 'Wyślij zgłoszenie'}
          </button>
        </fieldset>
      </form>
    </section>
  )
}
