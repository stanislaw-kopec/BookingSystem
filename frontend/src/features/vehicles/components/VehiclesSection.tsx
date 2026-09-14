import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { ApiError, errorMessage } from '../../../api/apiClient'
import * as vehiclesApi from '../api/vehiclesApi'
import type { Vehicle, VehicleInput } from '../types'
import { VehicleForm } from './VehicleForm'
import { vehicleFieldErrors } from '../vehicleFieldErrors'
import '../vehicles.css'

function sortVehicles(vehicles: Vehicle[]) {
  return [...vehicles].sort((first, second) =>
    `${first.make} ${first.model} ${first.registrationNumber}`
      .localeCompare(`${second.make} ${second.model} ${second.registrationNumber}`, 'pl'))
}

export function VehiclesSection() {
  const [vehicles, setVehicles] = useState<Vehicle[] | null>(null)
  const [isAdding, setIsAdding] = useState(false)
  const [isSaving, setIsSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [notice, setNotice] = useState<string | null>(null)
  const [revision, setRevision] = useState(0)

  useEffect(() => {
    const controller = new AbortController()
    vehiclesApi.getVehicles(controller.signal)
      .then((result) => {
        if (!controller.signal.aborted) setVehicles(result)
      })
      .catch((cause: unknown) => {
        if (!controller.signal.aborted) setError(errorMessage(cause))
      })
    return () => controller.abort()
  }, [revision])

  function startAdding() {
    setError(null)
    setFieldErrors({})
    setNotice(null)
    setIsAdding(true)
  }

  function cancelAdding() {
    setError(null)
    setFieldErrors({})
    setIsAdding(false)
  }

  function retryLoading() {
    setError(null)
    setVehicles(null)
    setRevision((value) => value + 1)
  }

  async function saveVehicle(input: VehicleInput) {
    setIsSaving(true)
    setError(null)
    setFieldErrors({})
    setNotice(null)
    try {
      const created = await vehiclesApi.createVehicle(input)
      setVehicles((current) => sortVehicles([...(current ?? []), created]))
      setIsAdding(false)
      setNotice('Pojazd został dodany.')
    } catch (cause) {
      setError(errorMessage(cause))
      if (cause instanceof ApiError) setFieldErrors(vehicleFieldErrors(cause.fieldErrors))
    } finally {
      setIsSaving(false)
    }
  }

  return (
    <section id="client-vehicles" className="page-section vehicles-section" aria-labelledby="vehicles-heading">
      <div className="vehicles-heading">
        <div>
          <p className="eyebrow">Konto klienta</p>
          <h2 id="vehicles-heading">Moje pojazdy</h2>
          <p className="muted">Dodaj samochód i otwórz jego historię napraw.</p>
        </div>
        {!isAdding && <button type="button" className="button" onClick={startAdding}>Dodaj pojazd</button>}
      </div>

      {notice && <p className="message success" role="status">{notice}</p>}
      {error && <p className="message error" role="alert">{error}</p>}
      {isAdding && <VehicleForm isSaving={isSaving} fieldErrors={fieldErrors}
        onSave={saveVehicle} onCancel={cancelAdding} />}

      {vehicles === null && !error && <p role="status">Ładowanie pojazdów…</p>}
      {vehicles?.length === 0 && !isAdding && (
        <p className="empty-state">Nie masz jeszcze żadnych pojazdów. Dodaj pierwszy pojazd, aby rozpocząć.</p>
      )}
      {vehicles && vehicles.length > 0 && (
        <ul className="vehicle-list">
          {vehicles.map((vehicle) => (
            <li key={vehicle.id}>
              <Link className="vehicle-card" to={`/vehicles/${vehicle.id}`}>
                <span>
                  <strong>{vehicle.make} {vehicle.model}</strong>
                  <small className="muted">Rok produkcji: {vehicle.productionYear}</small>
                </span>
                <span className="vehicle-registration">{vehicle.registrationNumber}</span>
              </Link>
            </li>
          ))}
        </ul>
      )}

      {error && vehicles === null && (
        <button type="button" className="button secondary" onClick={retryLoading}>
          Spróbuj ponownie
        </button>
      )}
    </section>
  )
}
