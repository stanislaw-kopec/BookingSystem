import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { ApiError, errorMessage } from '../../../api/apiClient'
import * as appointmentsApi from '../api/appointmentsApi'
import { formatAppointmentDateTime } from '../dateTime'
import { useAppointmentAvailability } from '../hooks/useAppointmentAvailability'
import type { Appointment } from '../types'
import { AppointmentDetails } from './AppointmentDetails'
import { AvailabilityCalendar } from './AvailabilityCalendar'
import '../appointments.css'

type StaffAction = 'reject' | 'propose'

interface ActiveAction {
  appointmentId: number
  type: StaffAction
}

const statusPriority = {
  PENDING: 0,
  TIME_PROPOSED: 1,
  CONFIRMED: 2,
  CANCELLED: 3,
  REJECTED: 4,
} as const

function sortAppointments(appointments: Appointment[]) {
  return [...appointments].sort((first, second) => {
    const byStatus = statusPriority[first.status] - statusPriority[second.status]
    return byStatus || first.currentStartAt.localeCompare(second.currentStartAt)
  })
}

export function StaffAppointmentsSection() {
  const [appointments, setAppointments] = useState<Appointment[] | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [revision, setRevision] = useState(0)
  const [activeAction, setActiveAction] = useState<ActiveAction | null>(null)
  const [message, setMessage] = useState('')
  const [selectedStartAt, setSelectedStartAt] = useState('')
  const [isSaving, setIsSaving] = useState(false)
  const [actionError, setActionError] = useState<string | null>(null)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [notice, setNotice] = useState<string | null>(null)
  const availability = useAppointmentAvailability()

  useEffect(() => {
    const controller = new AbortController()
    appointmentsApi.getStaffAppointments(controller.signal)
      .then((result) => {
        if (!controller.signal.aborted) {
          setAppointments(sortAppointments(result))
          setLoadError(null)
        }
      })
      .catch((cause: unknown) => {
        if (!controller.signal.aborted) setLoadError(errorMessage(cause))
      })
      .finally(() => {
        if (!controller.signal.aborted) setIsLoading(false)
      })
    return () => controller.abort()
  }, [revision])

  function retry() {
    setIsLoading(true)
    setLoadError(null)
    setRevision((value) => value + 1)
  }

  function replaceAppointment(updated: Appointment) {
    setAppointments((current) => sortAppointments((current ?? []).map((appointment) =>
      appointment.id === updated.id ? updated : appointment)))
  }

  function resetAction() {
    setActiveAction(null)
    setMessage('')
    setSelectedStartAt('')
    setActionError(null)
    setFieldErrors({})
  }

  function beginAction(appointmentId: number, type: StaffAction) {
    resetAction()
    setNotice(null)
    setActiveAction({ appointmentId, type })
  }

  async function runSimpleAction(
    operation: () => Promise<Appointment>,
    successMessage: string,
  ) {
    setIsSaving(true)
    setActionError(null)
    setFieldErrors({})
    setNotice(null)
    try {
      replaceAppointment(await operation())
      resetAction()
      setNotice(successMessage)
      availability.refresh()
    } catch (cause) {
      setActionError(errorMessage(cause))
      if (cause instanceof ApiError) setFieldErrors(cause.fieldErrors)
    } finally {
      setIsSaving(false)
    }
  }

  async function submitDecision(event: FormEvent<HTMLFormElement>, appointmentId: number) {
    event.preventDefault()
    if (!activeAction || activeAction.appointmentId !== appointmentId) return
    setIsSaving(true)
    setActionError(null)
    setFieldErrors({})
    setNotice(null)
    try {
      const updated = activeAction.type === 'reject'
        ? await appointmentsApi.rejectAppointment(appointmentId, message.trim())
        : await appointmentsApi.proposeAppointmentTime(appointmentId, selectedStartAt, message.trim())
      replaceAppointment(updated)
      resetAction()
      setNotice(activeAction.type === 'reject'
        ? 'Zgłoszenie zostało odrzucone.'
        : 'Nowy termin został zapisany i czeka na potwierdzenie.')
      availability.refresh()
    } catch (cause) {
      setActionError(errorMessage(cause))
      if (cause instanceof ApiError) {
        setFieldErrors(cause.fieldErrors)
        if (cause.status === 409 && cause.fieldErrors.slotStartAt) {
          setSelectedStartAt('')
          availability.refresh()
        }
      }
    } finally {
      setIsSaving(false)
    }
  }

  return (
    <section className="page-section appointments-section" aria-labelledby="staff-appointments-heading">
      <div className="section-heading">
        <p className="eyebrow">Panel personelu</p>
        <h2 id="staff-appointments-heading">Zgłoszenia wizyt</h2>
        <p className="muted">Przyjmij lub odrzuć zgłoszenie albo zaproponuj klientowi inny termin.</p>
      </div>
      {notice && <p className="message success" role="status">{notice}</p>}
      {actionError && <p className="message error" role="alert">{actionError}</p>}
      {isLoading && <p role="status">Ładowanie zgłoszeń…</p>}
      {!isLoading && loadError && appointments === null && (
        <div className="message error" role="alert">
          <p>{loadError}</p>
          <button type="button" className="button secondary" onClick={retry}>Spróbuj ponownie</button>
        </div>
      )}
      {appointments?.length === 0 && <p className="empty-state">Nie ma jeszcze żadnych zgłoszeń wizyt.</p>}
      {appointments && appointments.length > 0 && (
        <ul className="appointment-list staff-appointment-list">
          {appointments.map((appointment) => {
            const action = activeAction?.appointmentId === appointment.id ? activeAction.type : null
            return (
              <li className="appointment-card" key={appointment.id}>
                <p className="requester-type">
                  {appointment.requesterType === 'CLIENT' ? 'Klient z kontem' : 'Gość bez konta'}
                </p>
                <AppointmentDetails appointment={appointment} showContact />

                {appointment.status === 'PENDING' && (
                  <div className="appointment-card-actions actions">
                    <button type="button" className="button"
                      disabled={isSaving}
                      onClick={() => void runSimpleAction(
                        () => appointmentsApi.acceptAppointment(appointment.id),
                        'Termin wizyty został potwierdzony.',
                      )}>
                      Przyjmij
                    </button>
                    <button type="button" className="button secondary" disabled={isSaving}
                      onClick={() => beginAction(appointment.id, 'propose')}>
                      Zaproponuj inny termin
                    </button>
                    <button type="button" className="button danger" disabled={isSaving}
                      onClick={() => beginAction(appointment.id, 'reject')}>
                      Odrzuć
                    </button>
                  </div>
                )}

                {appointment.status === 'TIME_PROPOSED' && (
                  <div className="appointment-card-actions actions">
                    {appointment.requesterType === 'GUEST' && (
                      <button type="button" className="button" disabled={isSaving}
                        onClick={() => void runSimpleAction(
                          () => appointmentsApi.confirmGuestProposedTime(appointment.id),
                          'Termin gościa został potwierdzony.',
                        )}>
                        Potwierdź po kontakcie
                      </button>
                    )}
                    <button type="button" className="button secondary" disabled={isSaving}
                      onClick={() => beginAction(appointment.id, 'propose')}>
                      Zaproponuj kolejny termin
                    </button>
                    <button type="button" className="button danger" disabled={isSaving}
                      onClick={() => beginAction(appointment.id, 'reject')}>
                      Odrzuć
                    </button>
                  </div>
                )}

                {action && (
                  <form className="staff-decision-form" onSubmit={(event) => void submitDecision(event, appointment.id)}>
                    <fieldset disabled={isSaving}>
                      <h4>{action === 'reject' ? 'Odrzucenie zgłoszenia' : 'Propozycja nowego terminu'}</h4>
                      {action === 'propose' && (
                        <>
                          <AvailabilityCalendar availability={availability.availability}
                            isLoading={availability.isLoading} error={availability.error}
                            selectedStartAt={selectedStartAt}
                            onSelect={(slot) => {
                              setSelectedStartAt(slot.startAt)
                              setActionError(null)
                              setFieldErrors((current) => ({ ...current, slotStartAt: '' }))
                            }}
                            onRetry={availability.refresh} fieldError={fieldErrors.slotStartAt} />
                          {selectedStartAt && (
                            <p className="selected-slot">
                              Nowy termin: <strong>{formatAppointmentDateTime(selectedStartAt, availability.availability?.timeZone)}</strong>
                            </p>
                          )}
                        </>
                      )}
                      <div className="form-field">
                        <label htmlFor={`staff-message-${appointment.id}`}>
                          Wiadomość dla klienta <span className="muted">(opcjonalnie)</span>
                        </label>
                        <textarea id={`staff-message-${appointment.id}`} rows={3} maxLength={500} value={message}
                          aria-invalid={Boolean(fieldErrors.message)}
                          aria-describedby={fieldErrors.message ? `staff-message-${appointment.id}-error` : undefined}
                          onChange={(event) => {
                            setMessage(event.target.value)
                            setActionError(null)
                            setFieldErrors((current) => ({ ...current, message: '' }))
                          }} />
                        {fieldErrors.message && (
                          <small id={`staff-message-${appointment.id}-error`} className="field-error">
                            {fieldErrors.message}
                          </small>
                        )}
                      </div>
                      <div className="actions">
                        <button type="button" className="button secondary" onClick={resetAction}>Anuluj</button>
                        <button type="submit" className={action === 'reject' ? 'button danger' : 'button'}
                          disabled={action === 'propose' && !selectedStartAt}>
                          {isSaving ? 'Zapisywanie…' : action === 'reject' ? 'Odrzuć zgłoszenie' : 'Wyślij propozycję'}
                        </button>
                      </div>
                    </fieldset>
                  </form>
                )}
              </li>
            )
          })}
        </ul>
      )}
    </section>
  )
}
