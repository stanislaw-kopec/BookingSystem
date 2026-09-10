import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { errorMessage } from '../../../api/apiClient'
import type { RepairHistoryEntry } from '../../vehicles/types'
import * as appointmentsApi from '../api/appointmentsApi'
import type { Appointment } from '../types'
import { AppointmentDetails } from './AppointmentDetails'
import '../../vehicles/vehicles.css'
import '../appointments.css'

export function StaffAppointmentDetailsSection({ appointmentId }: { appointmentId: number }) {
  const [appointment, setAppointment] = useState<Appointment | null>(null)
  const [repairHistory, setRepairHistory] = useState<RepairHistoryEntry[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [revision, setRevision] = useState(0)

  useEffect(() => {
    const controller = new AbortController()
    Promise.all([
      appointmentsApi.getStaffAppointment(appointmentId, controller.signal),
      appointmentsApi.getStaffAppointmentRepairHistory(appointmentId, controller.signal),
    ])
      .then(([appointmentResult, historyResult]) => {
        if (!controller.signal.aborted) {
          setAppointment(appointmentResult)
          setRepairHistory(historyResult)
          setError(null)
        }
      })
      .catch((cause: unknown) => {
        if (!controller.signal.aborted) setError(errorMessage(cause))
      })
    return () => controller.abort()
  }, [appointmentId, revision])

  function retry() {
    setAppointment(null)
    setRepairHistory(null)
    setError(null)
    setRevision((value) => value + 1)
  }

  return (
    <section className="page-section appointments-section staff-appointment-details-section" aria-labelledby="staff-appointment-details-heading">
      <Link className="back-link" to="/staff/schedule">← Wróć do grafiku</Link>
      {!appointment && !error && <p role="status">Ładowanie zgłoszenia…</p>}
      {error && (
        <div className="message error" role="alert">
          <p>{error}</p>
          <button type="button" className="button secondary" onClick={retry}>Spróbuj ponownie</button>
        </div>
      )}
      {appointment && (
        <>
          <div className="section-heading">
            <p className="eyebrow">Szczegóły zgłoszenia</p>
            <h2 id="staff-appointment-details-heading">
              {appointment.vehicleMake} {appointment.vehicleModel} — {appointment.vehicleRegistrationNumber}
            </h2>
            <p className="muted">Pełny opis zgłoszenia oraz historia zakończonych napraw tego pojazdu.</p>
          </div>

          <article className="appointment-card staff-appointment-details-card">
            <p className="requester-type">
              {appointment.requesterType === 'CLIENT' ? 'Klient z kontem' : 'Gość bez konta'}
            </p>
            <AppointmentDetails appointment={appointment} showContact />
          </article>

          <section className="repair-history staff-repair-history" aria-labelledby="staff-repair-history-heading">
            <div className="repair-history-heading">
              <h3 id="staff-repair-history-heading">Historia napraw pojazdu</h3>
              <span className="count-badge">{repairHistory?.length ?? 0} wpisów</span>
            </div>
            {repairHistory === null && <p role="status">Ładowanie historii napraw…</p>}
            {repairHistory?.length === 0 && appointment.vehicleId === null && (
              <p className="empty-state">
                To zgłoszenie pochodzi od gościa, więc pojazd nie ma jeszcze trwałej kartoteki w systemie.
              </p>
            )}
            {repairHistory?.length === 0 && appointment.vehicleId !== null && (
              <p className="empty-state">
                Ten pojazd nie ma jeszcze zakończonych napraw w historii.
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
