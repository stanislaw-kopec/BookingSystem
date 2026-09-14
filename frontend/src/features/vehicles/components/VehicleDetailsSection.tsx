import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { ApiError, errorMessage } from '../../../api/apiClient'
import * as vehiclesApi from '../api/vehiclesApi'
import type { RepairHistoryEntry, Vehicle, VehicleInput } from '../types'
import { VehicleForm } from './VehicleForm'
import { vehicleFieldErrors } from '../vehicleFieldErrors'
import '../vehicles.css'

export function VehicleDetailsSection({ vehicleId }: { vehicleId: number }) {
  const [vehicle, setVehicle] = useState<Vehicle | null>(null)
  const [repairHistory, setRepairHistory] = useState<RepairHistoryEntry[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [invoiceError, setInvoiceError] = useState<string | null>(null)
  const [saveError, setSaveError] = useState<string | null>(null)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [notice, setNotice] = useState<string | null>(null)
  const [isEditing, setIsEditing] = useState(false)
  const [isSaving, setIsSaving] = useState(false)
  const [downloadingInvoiceId, setDownloadingInvoiceId] = useState<number | null>(null)
  const [revision, setRevision] = useState(0)

  useEffect(() => {
    const controller = new AbortController()
    Promise.all([
      vehiclesApi.getVehicle(vehicleId, controller.signal),
      vehiclesApi.getRepairHistory(vehicleId, controller.signal),
    ])
      .then(([vehicleResult, historyResult]) => {
        if (!controller.signal.aborted) {
          setVehicle(vehicleResult)
          setRepairHistory(historyResult)
        }
      })
      .catch((cause: unknown) => {
        if (!controller.signal.aborted) setError(errorMessage(cause))
      })
    return () => controller.abort()
  }, [vehicleId, revision])

  function retryLoading() {
    setError(null)
    setInvoiceError(null)
    setRepairHistory(null)
    setRevision((value) => value + 1)
  }

  async function downloadInvoice(entry: RepairHistoryEntry) {
    setDownloadingInvoiceId(entry.appointmentId)
    setInvoiceError(null)
    try {
      const invoice = await vehiclesApi.downloadRepairInvoice(vehicleId, entry.appointmentId)
      const url = URL.createObjectURL(invoice)
      const link = document.createElement('a')
      link.href = url
      link.download = vehiclesApi.repairInvoiceFilename(entry)
      document.body.append(link)
      link.click()
      link.remove()
      URL.revokeObjectURL(url)
    } catch (cause) {
      setInvoiceError(errorMessage(cause))
    } finally {
      setDownloadingInvoiceId(null)
    }
  }

  function startEditing() {
    setSaveError(null)
    setFieldErrors({})
    setNotice(null)
    setIsEditing(true)
  }

  function cancelEditing() {
    setSaveError(null)
    setFieldErrors({})
    setIsEditing(false)
  }

  async function saveVehicle(input: VehicleInput) {
    setIsSaving(true)
    setSaveError(null)
    setFieldErrors({})
    setNotice(null)
    try {
      setVehicle(await vehiclesApi.updateVehicle(vehicleId, input))
      setIsEditing(false)
      setNotice('Dane pojazdu zostały zapisane.')
    } catch (cause) {
      setSaveError(errorMessage(cause))
      if (cause instanceof ApiError) setFieldErrors(vehicleFieldErrors(cause.fieldErrors))
    } finally {
      setIsSaving(false)
    }
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
          <div className="vehicle-title vehicle-title-header">
            <div>
              <p className="eyebrow">Szczegóły pojazdu</p>
              <h2 id="vehicle-heading">{vehicle.make} {vehicle.model}</h2>
              <p className="vehicle-registration">{vehicle.registrationNumber}</p>
            </div>
            {!isEditing && (
              <button type="button" className="button secondary" onClick={startEditing}>
                Edytuj pojazd
              </button>
            )}
          </div>
          {notice && <p className="message success" role="status">{notice}</p>}
          {saveError && <p className="message error" role="alert">{saveError}</p>}
          {isEditing ? (
            <VehicleForm key={vehicle.id} mode="edit" initialValue={vehicle}
              isSaving={isSaving} fieldErrors={fieldErrors} onSave={saveVehicle} onCancel={cancelEditing} />
          ) : (
            <dl className="vehicle-details-card">
              <div><dt>Marka</dt><dd>{vehicle.make}</dd></div>
              <div><dt>Model</dt><dd>{vehicle.model}</dd></div>
              <div><dt>Rok produkcji</dt><dd>{vehicle.productionYear}</dd></div>
              <div><dt>Numer rejestracyjny</dt><dd>{vehicle.registrationNumber}</dd></div>
              <div><dt>VIN</dt><dd>{vehicle.vin || 'Nie podano'}</dd></div>
            </dl>
          )}
          <section className="repair-history" aria-labelledby="repair-history-heading">
            <div className="repair-history-heading">
              <h3 id="repair-history-heading">Historia napraw</h3>
              <span className="count-badge">{repairHistory?.length ?? 0} wpisów</span>
            </div>
            {invoiceError && <p className="message error" role="alert">{invoiceError}</p>}
            {repairHistory === null && <p role="status">Ładowanie historii napraw…</p>}
            {repairHistory?.length === 0 && (
              <p className="empty-state">
                Brak zakończonych napraw. Wpis pojawi się tutaj po tym, jak warsztat oznaczy samochód jako odebrany.
              </p>
            )}
            {repairHistory && repairHistory.length > 0 && (
              <ul className="repair-history-list">
                {repairHistory.map((entry) => (
                  <li className="repair-history-card" key={entry.appointmentId}>
                    <div className="repair-history-card-heading">
                      <div>
                        <p className="eyebrow">Naprawa zakończona</p>
                        <h4>{formatDate(entry.visitDate)}</h4>
                      </div>
                      <strong>{formatMoney(entry.totalGrossAmount)}</strong>
                    </div>
                    <p>{entry.repairDescription}</p>
                    {entry.repairItems.length > 0 && (
                      <ul className="repair-items-summary">
                        {entry.repairItems.map((item) => (
                          <li key={item.id ?? `${item.type}-${item.name}`}>
                            <span>{item.type === 'LABOR' ? 'Robocizna' : 'Część'}: {item.name}</span>
                            <strong>{formatMoney(item.totalGrossAmount)}</strong>
                          </li>
                        ))}
                      </ul>
                    )}
                    <dl>
                      <div><dt>Numer zgłoszenia</dt><dd>{entry.appointmentReference}</dd></div>
                      <div><dt>Pracę zamknął</dt><dd>{entry.repairCompletedBy}</dd></div>
                      <div><dt>Odbiór potwierdził</dt><dd>{entry.vehiclePickedUpBy}</dd></div>
                    </dl>
                    <div className="repair-history-actions">
                      <button type="button" className="button secondary"
                        disabled={downloadingInvoiceId === entry.appointmentId}
                        onClick={() => void downloadInvoice(entry)}>
                        {downloadingInvoiceId === entry.appointmentId ? 'Pobieranie…' : 'Pobierz fakturę'}
                      </button>
                    </div>
                  </li>
                ))}
              </ul>
            )}
          </section>
        </>
      )}
    </section>
  )
}


function formatDate(value: string) {
  return new Intl.DateTimeFormat('pl-PL', {
    timeZone: 'Europe/Warsaw',
    day: 'numeric',
    month: 'long',
    year: 'numeric',
  }).format(new Date(value))
}

function formatMoney(value: number) {
  return new Intl.NumberFormat('pl-PL', {
    style: 'currency',
    currency: 'PLN',
  }).format(value)
}
