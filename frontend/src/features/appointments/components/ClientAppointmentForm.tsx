import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { CustomSelect } from '../../../components/ui/CustomSelect'
import { ApiError, errorMessage } from '../../../api/apiClient'
import * as vehiclesApi from '../../vehicles/api/vehiclesApi'
import { VehicleForm } from '../../vehicles/components/VehicleForm'
import type { Vehicle, VehicleInput } from '../../vehicles/types'
import '../../vehicles/vehicles.css'
import * as appointmentsApi from '../api/appointmentsApi'
import { formatAppointmentDay } from '../dateTime'
import { useAppointmentAvailability } from '../hooks/useAppointmentAvailability'
import { AvailabilityCalendar } from './AvailabilityCalendar'

function sortVehicles(vehicles: Vehicle[]) {
  return [...vehicles].sort((first, second) =>
    `${first.make} ${first.model} ${first.registrationNumber}`
      .localeCompare(`${second.make} ${second.model} ${second.registrationNumber}`, 'pl'))
}

export function ClientAppointmentForm() {
  const [vehicles, setVehicles] = useState<Vehicle[] | null>(null)
  const [vehicleLoadError, setVehicleLoadError] = useState<string | null>(null)
  const [vehicleRevision, setVehicleRevision] = useState(0)
  const [isAddingVehicle, setIsAddingVehicle] = useState(false)
  const [isSavingVehicle, setIsSavingVehicle] = useState(false)
  const [vehicleError, setVehicleError] = useState<string | null>(null)
  const [vehicleFieldErrors, setVehicleFieldErrors] = useState<Record<string, string>>({})
  const [selectedVehicleId, setSelectedVehicleId] = useState('')
  const [selectedVisitDate, setSelectedVisitDate] = useState('')
  const [problemDescription, setProblemDescription] = useState('')
  const [isSaving, setIsSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [notice, setNotice] = useState<string | null>(null)
  const availability = useAppointmentAvailability()

  useEffect(() => {
    const controller = new AbortController()
    vehiclesApi.getVehicles(controller.signal)
      .then((result) => {
        if (!controller.signal.aborted) {
          const sorted = sortVehicles(result)
          setVehicles(sorted)
          setVehicleLoadError(null)
          if (sorted.length > 0) setSelectedVehicleId((current) => current || String(sorted[0].id))
        }
      })
      .catch((cause: unknown) => {
        if (!controller.signal.aborted) setVehicleLoadError(errorMessage(cause))
      })
    return () => controller.abort()
  }, [vehicleRevision])

  function retryVehicles() {
    setVehicles(null)
    setVehicleLoadError(null)
    setVehicleRevision((value) => value + 1)
  }

  function startAddingVehicle() {
    setVehicleError(null)
    setVehicleFieldErrors({})
    setIsAddingVehicle(true)
  }

  async function saveVehicle(input: VehicleInput) {
    setIsSavingVehicle(true)
    setVehicleError(null)
    setVehicleFieldErrors({})
    try {
      const created = await vehiclesApi.createVehicle(input)
      setVehicles((current) => sortVehicles([...(current ?? []), created]))
      setSelectedVehicleId(String(created.id))
      setIsAddingVehicle(false)
    } catch (cause) {
      setVehicleError(errorMessage(cause))
      if (cause instanceof ApiError) setVehicleFieldErrors(cause.fieldErrors)
    } finally {
      setIsSavingVehicle(false)
    }
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setIsSaving(true)
    setError(null)
    setFieldErrors({})
    setNotice(null)
    try {
      await appointmentsApi.createClientAppointment({
        vehicleId: Number(selectedVehicleId),
        visitDate: selectedVisitDate,
        problemDescription: problemDescription.trim(),
      })
      setSelectedVisitDate('')
      setProblemDescription('')
      setNotice('Zgłoszenie zostało wysłane i oczekuje na decyzję warsztatu.')
      availability.refresh()
    } catch (cause) {
      setError(errorMessage(cause))
      if (cause instanceof ApiError) {
        setFieldErrors(cause.fieldErrors)
        if (cause.status === 409 && cause.fieldErrors.visitDate) {
          setSelectedVisitDate('')
          availability.refresh()
        }
      }
    } finally {
      setIsSaving(false)
    }
  }

  const needsVehicleForm = vehicles?.length === 0 || isAddingVehicle

  return (
    <section className="appointment-block" aria-labelledby="client-booking-heading">
      <div className="section-heading">
        <p className="eyebrow">Nowe zgłoszenie</p>
        <h2 id="client-booking-heading">Umów wizytę</h2>
        <p className="muted">Wybierz pojazd i dzień przyjęcia samochodu, a następnie opisz usterkę.</p>
      </div>

      {notice && (
        <p className="message success" role="status">
          {notice} <Link to="/my-appointments">Przejdź do moich wizyt</Link>.
        </p>
      )}
      {error && !fieldErrors.profile && <p className="message error" role="alert">{error}</p>}
      {fieldErrors.profile && (
        <p className="message error" role="alert">
          {fieldErrors.profile} <Link to="/profile">Przejdź do profilu</Link>.
        </p>
      )}

      <div className="vehicle-choice">
        <div className="appointment-subheading">
          <h3>1. Pojazd</h3>
          {vehicles && vehicles.length > 0 && !isAddingVehicle && (
            <button type="button" className="button secondary small" onClick={startAddingVehicle}>
              Dodaj nowy pojazd
            </button>
          )}
        </div>
        {vehicles === null && !vehicleLoadError && <p role="status">Ładowanie pojazdów…</p>}
        {vehicleLoadError && (
          <div className="message error" role="alert">
            <p>{vehicleLoadError}</p>
            <button type="button" className="button secondary" onClick={retryVehicles}>Spróbuj ponownie</button>
          </div>
        )}
        {vehicles && vehicles.length > 0 && (
          <div className="form-field">
            <label htmlFor="appointment-vehicle">Wybierz pojazd</label>
            <CustomSelect id="appointment-vehicle" value={selectedVehicleId}
              invalid={Boolean(fieldErrors.vehicleId)}
              describedBy={fieldErrors.vehicleId ? 'appointment-vehicle-error' : undefined}
              options={vehicles.map((vehicle) => ({
                value: String(vehicle.id),
                label: `${vehicle.make} ${vehicle.model} · ${vehicle.registrationNumber}`,
              }))}
              onChange={(value) => {
                setSelectedVehicleId(value)
                setError(null)
                setFieldErrors((current) => ({ ...current, vehicleId: '' }))
              }} />
            {fieldErrors.vehicleId && (
              <small id="appointment-vehicle-error" className="field-error">{fieldErrors.vehicleId}</small>
            )}
          </div>
        )}
        {vehicleError && <p className="message error" role="alert">{vehicleError}</p>}
        {needsVehicleForm && vehicles && (
          <VehicleForm isSaving={isSavingVehicle} fieldErrors={vehicleFieldErrors} onSave={saveVehicle}
            onCancel={vehicles.length > 0 ? () => setIsAddingVehicle(false) : undefined} />
        )}
      </div>

      <form className="appointment-form" onSubmit={(event) => void handleSubmit(event)}>
        <fieldset disabled={isSaving}>
          <div className="appointment-step">
            <p className="step-label">2. Termin</p>
            <AvailabilityCalendar availability={availability.availability} isLoading={availability.isLoading}
              error={availability.error} selectedVisitDate={selectedVisitDate}
              onSelect={(day) => {
                setSelectedVisitDate(day.date)
                setError(null)
                setFieldErrors((current) => ({ ...current, visitDate: '' }))
              }}
              onRetry={availability.refresh} fieldError={fieldErrors.visitDate} />
            {selectedVisitDate && (
              <p className="selected-day" role="status">
                Wybrany dzień: <strong>{formatAppointmentDay(selectedVisitDate, availability.availability?.timeZone)}</strong>
              </p>
            )}
          </div>
          <div className="appointment-step form-field">
            <label htmlFor="client-problem-description">3. Opis usterki</label>
            <textarea id="client-problem-description" rows={6} required minLength={10} maxLength={2000}
              value={problemDescription} aria-invalid={Boolean(fieldErrors.problemDescription)}
              aria-describedby={fieldErrors.problemDescription
                ? 'client-problem-description-help client-problem-description-error'
                : 'client-problem-description-help'}
              onChange={(event) => {
                setProblemDescription(event.target.value)
                setError(null)
                setFieldErrors((current) => ({ ...current, problemDescription: '' }))
              }} />
            <small id="client-problem-description-help" className="muted">
              Opisz objawy i okoliczności, w których pojawia się problem.
            </small>
            {fieldErrors.problemDescription && (
              <small id="client-problem-description-error" className="field-error">{fieldErrors.problemDescription}</small>
            )}
          </div>
          <button type="submit" className="button appointment-submit"
            disabled={!selectedVehicleId || !selectedVisitDate || problemDescription.trim().length < 10}>
            {isSaving ? 'Wysyłanie…' : 'Wyślij zgłoszenie'}
          </button>
        </fieldset>
      </form>
    </section>
  )
}
