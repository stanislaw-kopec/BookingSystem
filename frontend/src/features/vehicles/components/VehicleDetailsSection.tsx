import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { errorMessage } from '../../../api/apiClient'
import * as vehiclesApi from '../api/vehiclesApi'
import type { Vehicle } from '../types'
import '../vehicles.css'

export function VehicleDetailsSection({ vehicleId }: { vehicleId: number }) {
  const [vehicle, setVehicle] = useState<Vehicle | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [revision, setRevision] = useState(0)

  useEffect(() => {
    const controller = new AbortController()
    vehiclesApi.getVehicle(vehicleId, controller.signal)
      .then((result) => {
        if (!controller.signal.aborted) setVehicle(result)
      })
      .catch((cause: unknown) => {
        if (!controller.signal.aborted) setError(errorMessage(cause))
      })
    return () => controller.abort()
  }, [vehicleId, revision])

  function retryLoading() {
    setError(null)
    setRevision((value) => value + 1)
  }

  return (
    <section id="vehicle-details" className="page-section vehicle-details-section" aria-labelledby="vehicle-heading">
      <Link className="back-link" to="/vehicles">← Wróć do moich pojazdów</Link>
      {!vehicle && !error && <p role="status">Ładowanie pojazdu…</p>}
      {error && (
        <div className="message error" role="alert">
          <p>{error}</p>
          <button type="button" className="button secondary" onClick={retryLoading}>
            Spróbuj ponownie
          </button>
        </div>
      )}
      {vehicle && (
        <>
          <div className="vehicle-title">
            <p className="eyebrow">Szczegóły pojazdu</p>
            <h2 id="vehicle-heading">{vehicle.make} {vehicle.model}</h2>
            <p className="vehicle-registration">{vehicle.registrationNumber}</p>
          </div>
          <dl className="vehicle-details-card">
            <div><dt>Marka</dt><dd>{vehicle.make}</dd></div>
            <div><dt>Model</dt><dd>{vehicle.model}</dd></div>
            <div><dt>Rok produkcji</dt><dd>{vehicle.productionYear}</dd></div>
            <div><dt>Numer rejestracyjny</dt><dd>{vehicle.registrationNumber}</dd></div>
            <div><dt>VIN</dt><dd>{vehicle.vin || 'Nie podano'}</dd></div>
          </dl>
          <section className="repair-history" aria-labelledby="repair-history-heading">
            <div className="repair-history-heading">
              <h3 id="repair-history-heading">Historia napraw</h3>
              <span className="count-badge">0 wpisów</span>
            </div>
            <p className="empty-state">
              Brak udokumentowanych napraw. Pojawią się tutaj po wystawieniu faktury za wykonane prace.
            </p>
          </section>
        </>
      )}
    </section>
  )
}
