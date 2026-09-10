import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { errorMessage } from '../../../api/apiClient'
import * as appointmentsApi from '../api/appointmentsApi'
import type { Appointment } from '../types'
import { AppointmentDetails } from './AppointmentDetails'
import '../appointments.css'

const statusPriority = {
  TIME_PROPOSED: 0,
  PENDING: 1,
  CONFIRMED: 2,
  READY_FOR_PICKUP: 3,
  COMPLETED: 4,
  CANCELLED: 5,
  REJECTED: 6,
} as const

const cancellableStatuses = new Set<Appointment['status']>([
  'PENDING',
  'TIME_PROPOSED',
  'CONFIRMED',
])

function sortAppointments(appointments: Appointment[]) {
  return [...appointments].sort((first, second) => {
    const byStatus = statusPriority[first.status] - statusPriority[second.status]
    return byStatus || second.createdAt.localeCompare(first.createdAt)
  })
}

export function ClientAppointmentsSection() {
  const [appointments, setAppointments] = useState<Appointment[] | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)
  const [confirmingId, setConfirmingId] = useState<number | null>(null)
  const [cancellingId, setCancellingId] = useState<number | null>(null)
  const [notice, setNotice] = useState<string | null>(null)
  const [revision, setRevision] = useState(0)

  useEffect(() => {
    const controller = new AbortController()
    appointmentsApi.getClientAppointments(controller.signal)
      .then((result) => {
        if (!controller.signal.aborted) {
          setAppointments(sortAppointments(result))
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
  }, [revision])

  function retry() {
    setIsLoading(true)
    setError(null)
    setRevision((value) => value + 1)
  }

  async function confirmProposedTime(appointmentId: number) {
    setConfirmingId(appointmentId)
    setActionError(null)
    setNotice(null)
    try {
      const updated = await appointmentsApi.confirmProposedTime(appointmentId)
      setAppointments((current) => sortAppointments((current ?? []).map((appointment) =>
        appointment.id === updated.id ? updated : appointment)))
      setNotice('Nowy dzień wizyty został potwierdzony.')
    } catch (cause) {
      setActionError(errorMessage(cause))
    } finally {
      setConfirmingId(null)
    }
  }

  async function cancelAppointment(appointmentId: number) {
    setCancellingId(appointmentId)
    setActionError(null)
    setNotice(null)
    try {
      const updated = await appointmentsApi.cancelClientAppointment(appointmentId)
      setAppointments((current) => sortAppointments((current ?? []).map((appointment) =>
        appointment.id === updated.id ? updated : appointment)))
      setNotice('Wizyta została odwołana.')
    } catch (cause) {
      setActionError(errorMessage(cause))
    } finally {
      setCancellingId(null)
    }
  }

  return (
    <section className="page-section appointments-section" aria-label="Moje wizyty">
      <section className="appointment-block appointment-list-section" aria-labelledby="client-appointments-heading">
        <div className="section-heading">
          <p className="eyebrow">Konto klienta</p>
          <h2 id="client-appointments-heading">Moje wizyty</h2>
          <p className="muted">Tutaj sprawdzisz decyzję warsztatu i potwierdzisz zaproponowany dzień.</p>
          <Link className="button" to="/appointments">Umów wizytę</Link>
        </div>
        {notice && <p className="message success" role="status">{notice}</p>}
        {actionError && <p className="message error" role="alert">{actionError}</p>}
        {isLoading && <p role="status">Ładowanie zgłoszeń…</p>}
        {!isLoading && error && appointments === null && (
          <div className="message error" role="alert">
            <p>{error}</p>
            <button type="button" className="button secondary" onClick={retry}>Spróbuj ponownie</button>
          </div>
        )}
        {appointments?.length === 0 && (
          <p className="empty-state">Nie masz jeszcze żadnych zgłoszeń wizyt.</p>
        )}
        {appointments && appointments.length > 0 && (
          <ul className="appointment-list">
            {appointments.map((appointment) => (
              <li className="appointment-card" key={appointment.id}>
                <AppointmentDetails appointment={appointment} />
                {(appointment.status === 'TIME_PROPOSED' || cancellableStatuses.has(appointment.status)) && (
                  <div className="appointment-card-actions actions">
                    {appointment.status === 'TIME_PROPOSED' && (
                      <>
                        <p>Sprawdź nowy dzień wskazany przez warsztat i potwierdź, jeśli Ci odpowiada.</p>
                        <button type="button" className="button"
                          disabled={confirmingId !== null || cancellingId !== null}
                          onClick={() => void confirmProposedTime(appointment.id)}>
                          {confirmingId === appointment.id ? 'Potwierdzanie…' : 'Potwierdź nowy dzień'}
                        </button>
                      </>
                    )}
                    {cancellableStatuses.has(appointment.status) && (
                      <button type="button" className="button secondary danger-button"
                        disabled={confirmingId !== null || cancellingId !== null}
                        onClick={() => void cancelAppointment(appointment.id)}>
                        {cancellingId === appointment.id ? 'Odwoływanie…' : 'Odwołaj wizytę'}
                      </button>
                    )}
                  </div>
                )}
              </li>
            ))}
          </ul>
        )}
      </section>
    </section>
  )
}
