import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { ApiError, errorMessage } from '../../../api/apiClient'
import * as appointmentsApi from '../api/appointmentsApi'
import { formatAppointmentDay } from '../dateTime'
import { useAppointmentAvailability } from '../hooks/useAppointmentAvailability'
import type { Appointment } from '../types'
import { AppointmentDetails } from './AppointmentDetails'
import { AvailabilityCalendar } from './AvailabilityCalendar'
import '../appointments.css'

type StaffAction = 'reject' | 'propose' | 'complete'

interface ActiveAction {
  appointmentId: number
  type: StaffAction
}

const statusPriority = {
  PENDING: 0,
  TIME_PROPOSED: 1,
  CONFIRMED: 2,
  READY_FOR_PICKUP: 3,
  COMPLETED: 4,
  CANCELLED: 5,
  REJECTED: 6,
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
  const [selectedVisitDate, setSelectedVisitDate] = useState('')
  const [repairDescription, setRepairDescription] = useState('')
  const [totalGrossAmount, setTotalGrossAmount] = useState('')
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
    setSelectedVisitDate('')
    setRepairDescription('')
    setTotalGrossAmount('')
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
        : activeAction.type === 'propose'
          ? await appointmentsApi.proposeAppointmentTime(appointmentId, selectedVisitDate, message.trim())
          : await appointmentsApi.completeRepair(
            appointmentId,
            repairDescription.trim(),
            Number(totalGrossAmount.replace(',', '.')),
          )
      replaceAppointment(updated)
      resetAction()
      setNotice(activeAction.type === 'reject'
        ? 'Zgłoszenie zostało odrzucone.'
        : activeAction.type === 'propose'
          ? 'Nowy dzień został zapisany i czeka na potwierdzenie.'
          : 'Naprawa została zakończona. Auto czeka na odbiór i płatność na miejscu.')
      availability.refresh()
    } catch (cause) {
      setActionError(errorMessage(cause))
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

  return (
    <section className="page-section appointments-section" aria-labelledby="staff-appointments-heading">
      <div className="section-heading">
        <p className="eyebrow">Panel personelu</p>
        <h2 id="staff-appointments-heading">Zgłoszenia wizyt</h2>
        <p className="muted">Obsłuż zgłoszenia, zaproponuj inny dzień, zakończ naprawę albo potwierdź odbiór auta.</p>
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
                      Zaproponuj inny dzień
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
                      Zaproponuj kolejny dzień
                    </button>
                    <button type="button" className="button danger" disabled={isSaving}
                      onClick={() => beginAction(appointment.id, 'reject')}>
                      Odrzuć
                    </button>
                  </div>
                )}

                {appointment.status === 'READY_FOR_PICKUP' && (
                  <div className="appointment-card-actions actions">
                    <button type="button" className="button" disabled={isSaving}
                      onClick={() => void runSimpleAction(
                        () => appointmentsApi.markVehiclePickedUp(appointment.id),
                        'Samochód został odebrany. Zgłoszenie jest zakończone.',
                      )}>
                      Samochód został odebrany
                    </button>
                  </div>
                )}

                {appointment.status === 'CONFIRMED' && (
                  <div className="appointment-card-actions actions">
                    <button type="button" className="button" disabled={isSaving}
                      onClick={() => beginAction(appointment.id, 'complete')}>
                      Praca zakończona
                    </button>
                  </div>
                )}

                {action && (
                  <form className="staff-decision-form" onSubmit={(event) => void submitDecision(event, appointment.id)}>
                    <fieldset disabled={isSaving}>
                      <h4>
                        {action === 'reject'
                          ? 'Odrzucenie zgłoszenia'
                          : action === 'propose'
                            ? 'Propozycja nowego dnia'
                            : 'Zakończenie naprawy'}
                      </h4>
                      {action === 'propose' && (
                        <>
                          <AvailabilityCalendar availability={availability.availability}
                            isLoading={availability.isLoading} error={availability.error}
                            selectedVisitDate={selectedVisitDate}
                            onSelect={(day) => {
                              setSelectedVisitDate(day.date)
                              setActionError(null)
                              setFieldErrors((current) => ({ ...current, visitDate: '' }))
                            }}
                            onRetry={availability.refresh} fieldError={fieldErrors.visitDate} />
                          {selectedVisitDate && (
                            <p className="selected-day">
                              Nowy dzień: <strong>{formatAppointmentDay(selectedVisitDate, availability.availability?.timeZone)}</strong>
                            </p>
                          )}
                        </>
                      )}
                      {action === 'complete' ? (
                        <div className="repair-completion-form">
                          <p className="muted">
                            Opisz, co zostało zrobione, i podaj końcową kwotę brutto do zapłaty przy odbiorze auta.
                          </p>
                          <div className="appointment-form-grid">
                            <div className="form-field wide-field">
                              <label htmlFor={`repair-description-${appointment.id}`}>Wykonane prace</label>
                              <textarea id={`repair-description-${appointment.id}`} rows={4} maxLength={2000}
                                value={repairDescription}
                                aria-invalid={Boolean(fieldErrors.repairDescription)}
                                aria-describedby={fieldErrors.repairDescription ? `repair-description-${appointment.id}-error` : undefined}
                                onChange={(event) => {
                                  setRepairDescription(event.target.value)
                                  setActionError(null)
                                  setFieldErrors((current) => ({ ...current, repairDescription: '' }))
                                }} />
                              {fieldErrors.repairDescription && (
                                <small id={`repair-description-${appointment.id}-error`} className="field-error">
                                  {fieldErrors.repairDescription}
                                </small>
                              )}
                            </div>
                            <div className="form-field">
                              <label htmlFor={`repair-total-${appointment.id}`}>Kwota brutto do zapłaty</label>
                              <input id={`repair-total-${appointment.id}`} type="number" min="0.01" step="0.01"
                                value={totalGrossAmount}
                                aria-invalid={Boolean(fieldErrors.totalGrossAmount)}
                                aria-describedby={fieldErrors.totalGrossAmount ? `repair-total-${appointment.id}-error` : undefined}
                                onChange={(event) => {
                                  setTotalGrossAmount(event.target.value)
                                  setActionError(null)
                                  setFieldErrors((current) => ({ ...current, totalGrossAmount: '' }))
                                }} />
                              {fieldErrors.totalGrossAmount && (
                                <small id={`repair-total-${appointment.id}-error`} className="field-error">
                                  {fieldErrors.totalGrossAmount}
                                </small>
                              )}
                            </div>
                          </div>
                        </div>
                      ) : (
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
                      )}
                      <div className="actions">
                        <button type="button" className="button secondary" onClick={resetAction}>Anuluj</button>
                        <button type="submit" className={action === 'reject' ? 'button danger' : 'button'}
                          disabled={(action === 'propose' && !selectedVisitDate)
                            || (action === 'complete' && (!repairDescription.trim() || !totalGrossAmount))}>
                          {isSaving
                            ? 'Zapisywanie…'
                            : action === 'reject'
                              ? 'Odrzuć zgłoszenie'
                              : action === 'propose'
                                ? 'Wyślij propozycję'
                                : 'Zamknij zgłoszenie'}
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
