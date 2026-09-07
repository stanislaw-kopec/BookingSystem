import { useEffect, useState } from 'react'
import { errorMessage } from '../../../api/apiClient'
import * as appointmentsApi from '../api/appointmentsApi'
import type { Appointment } from '../types'
import { AppointmentDetails } from './AppointmentDetails'
import { ClientAppointmentForm } from './ClientAppointmentForm'
import '../appointments.css'

const statusPriority = {
  TIME_PROPOSED: 0,
  PENDING: 1,
  CONFIRMED: 2,
  REJECTED: 3,
} as const

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

  function addCreated(created: Appointment) {
    setAppointments((current) => sortAppointments([created, ...(current ?? [])]))
  }

  async function confirmProposedTime(appointmentId: number) {
    setConfirmingId(appointmentId)
    setActionError(null)
    setNotice(null)
    try {
      const updated = await appointmentsApi.confirmProposedTime(appointmentId)
      setAppointments((current) => sortAppointments((current ?? []).map((appointment) =>
        appointment.id === updated.id ? updated : appointment)))
      setNotice('Nowy termin wizyty został potwierdzony.')
    } catch (cause) {
      setActionError(errorMessage(cause))
    } finally {
      setConfirmingId(null)
    }
  }

  return (
    <section className="page-section appointments-section" aria-label="Umawianie wizyty i moje zgłoszenia">
      <ClientAppointmentForm onCreated={addCreated} />

      <section className="appointment-block appointment-list-section" aria-labelledby="client-appointments-heading">
        <div className="section-heading">
          <p className="eyebrow">Konto klienta</p>
          <h2 id="client-appointments-heading">Moje zgłoszenia wizyt</h2>
          <p className="muted">Tutaj sprawdzisz decyzję warsztatu i potwierdzisz zaproponowany termin.</p>
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
                {appointment.status === 'TIME_PROPOSED' && (
                  <div className="appointment-card-actions">
                    <p>Sprawdź nowy termin wskazany przez warsztat i potwierdź, jeśli Ci odpowiada.</p>
                    <button type="button" className="button"
                      disabled={confirmingId !== null}
                      onClick={() => void confirmProposedTime(appointment.id)}>
                      {confirmingId === appointment.id ? 'Potwierdzanie…' : 'Potwierdź nowy termin'}
                    </button>
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
