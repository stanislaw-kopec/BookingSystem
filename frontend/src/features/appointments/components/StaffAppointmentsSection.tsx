import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { ApiError, errorMessage } from '../../../api/apiClient'
import * as appointmentsApi from '../api/appointmentsApi'
import { formatAppointmentDay } from '../dateTime'
import { useAppointmentAvailability } from '../hooks/useAppointmentAvailability'
import type { Appointment, AppointmentPage, AppointmentStatus, RepairItemInput, RepairItemType } from '../types'
import { AppointmentDetails } from './AppointmentDetails'
import { AvailabilityCalendar } from './AvailabilityCalendar'
import '../appointments.css'

type StaffAction = 'reject' | 'propose' | 'complete'
type StatusFilter = AppointmentStatus | 'ALL'
type DateSortDirection = 'DESC' | 'ASC'

interface RepairItemForm {
  type: RepairItemType
  name: string
  quantity: string
  unitGrossAmount: string
}

interface ActiveAction {
  appointmentId: number
  type: StaffAction
}

const pageSize = 5

function emptyRepairItem(): RepairItemForm {
  return { type: 'LABOR', name: '', quantity: '1', unitGrossAmount: '' }
}

const statusLabels: Record<AppointmentStatus, string> = {
  PENDING: 'Oczekuje na decyzję',
  TIME_PROPOSED: 'Zaproponowano inny dzień',
  CONFIRMED: 'Potwierdzona',
  READY_FOR_PICKUP: 'Czeka na odbiór',
  COMPLETED: 'Zakończona',
  CANCELLED: 'Odwołana',
  REJECTED: 'Odrzucona',
}

const statusFilterOptions: Array<{ value: StatusFilter, label: string }> = [
  { value: 'ALL', label: 'Wszystkie statusy' },
  { value: 'PENDING', label: statusLabels.PENDING },
  { value: 'TIME_PROPOSED', label: statusLabels.TIME_PROPOSED },
  { value: 'CONFIRMED', label: statusLabels.CONFIRMED },
  { value: 'READY_FOR_PICKUP', label: statusLabels.READY_FOR_PICKUP },
  { value: 'COMPLETED', label: statusLabels.COMPLETED },
  { value: 'CANCELLED', label: statusLabels.CANCELLED },
  { value: 'REJECTED', label: statusLabels.REJECTED },
]

export function StaffAppointmentsSection() {
  const [appointmentPage, setAppointmentPage] = useState<AppointmentPage | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [revision, setRevision] = useState(0)
  const [activeAction, setActiveAction] = useState<ActiveAction | null>(null)
  const [message, setMessage] = useState('')
  const [selectedVisitDate, setSelectedVisitDate] = useState('')
  const [repairDescription, setRepairDescription] = useState('')
  const [repairItems, setRepairItems] = useState<RepairItemForm[]>([emptyRepairItem()])
  const [isSaving, setIsSaving] = useState(false)
  const [actionError, setActionError] = useState<string | null>(null)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [statusFilter, setStatusFilter] = useState<StatusFilter>('ALL')
  const [dateSortDirection, setDateSortDirection] = useState<DateSortDirection>('DESC')
  const [currentPage, setCurrentPage] = useState(0)
  const [notice, setNotice] = useState<string | null>(null)
  const [downloadingInvoiceId, setDownloadingInvoiceId] = useState<number | null>(null)
  const availability = useAppointmentAvailability()

  useEffect(() => {
    const controller = new AbortController()
    appointmentsApi.getStaffAppointments(currentPage, pageSize, dateSortDirection, statusFilter, controller.signal)
      .then((result) => {
        if (!controller.signal.aborted) {
          const lastPage = Math.max(0, result.totalPages - 1)
          if (currentPage > lastPage) {
            setCurrentPage(lastPage)
            return
          }
          setAppointmentPage(result)
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
  }, [currentPage, dateSortDirection, revision, statusFilter])

  const appointments = appointmentPage?.content ?? []
  const totalElements = appointmentPage?.totalElements ?? 0
  const totalPages = appointmentPage?.totalPages ?? 0
  const firstVisibleIndex = totalElements === 0 ? 0 : currentPage * pageSize + 1
  const lastVisibleIndex = Math.min((currentPage * pageSize) + appointments.length, totalElements)

  function retry() {
    setIsLoading(true)
    setLoadError(null)
    setRevision((value) => value + 1)
  }

  function refreshCurrentPage() {
    setIsLoading(true)
    setRevision((value) => value + 1)
  }

  function changeStatusFilter(value: StatusFilter) {
    setIsLoading(true)
    setStatusFilter(value)
    setCurrentPage(0)
  }

  function changeDateSortDirection(value: DateSortDirection) {
    setIsLoading(true)
    setDateSortDirection(value)
    setCurrentPage(0)
  }

  function resetAction() {
    setActiveAction(null)
    setMessage('')
    setSelectedVisitDate('')
    setRepairDescription('')
    setRepairItems([emptyRepairItem()])
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
      await operation()
      resetAction()
      refreshCurrentPage()
      setNotice(successMessage)
      availability.refresh()
    } catch (cause) {
      setActionError(errorMessage(cause))
      if (cause instanceof ApiError) setFieldErrors(cause.fieldErrors)
    } finally {
      setIsSaving(false)
    }
  }

  async function downloadInvoice(appointment: Appointment) {
    setDownloadingInvoiceId(appointment.id)
    setActionError(null)
    setNotice(null)
    try {
      const invoice = await appointmentsApi.downloadStaffRepairInvoice(appointment.id)
      const url = URL.createObjectURL(invoice)
      const link = document.createElement('a')
      link.href = url
      link.download = appointmentsApi.staffRepairInvoiceFilename(appointment)
      link.click()
      URL.revokeObjectURL(url)
    } catch (cause) {
      setActionError(errorMessage(cause))
    } finally {
      setDownloadingInvoiceId(null)
    }
  }

  const repairItemsTotal = repairItems.reduce((sum, item) => {
    const quantity = Number(item.quantity.replace(',', '.'))
    const unitGrossAmount = Number(item.unitGrossAmount.replace(',', '.'))
    return Number.isFinite(quantity) && Number.isFinite(unitGrossAmount)
      ? sum + (quantity * unitGrossAmount)
      : sum
  }, 0)

  const canSubmitRepair = repairDescription.trim().length > 0
    && repairItems.length > 0
    && repairItems.every((item) => item.name.trim() && item.quantity && item.unitGrossAmount)

  function changeRepairItem(index: number, value: Partial<RepairItemForm>) {
    setRepairItems((current) => current.map((item, itemIndex) => (
      itemIndex === index ? { ...item, ...value } : item
    )))
    setActionError(null)
    setFieldErrors({})
  }

  function addRepairItem() {
    setRepairItems((current) => [...current, emptyRepairItem()])
    setActionError(null)
    setFieldErrors({})
  }

  function removeRepairItem(index: number) {
    setRepairItems((current) => current.filter((_, itemIndex) => itemIndex !== index))
    setActionError(null)
    setFieldErrors({})
  }

  function repairItemInput(item: RepairItemForm): RepairItemInput {
    return {
      type: item.type,
      name: item.name.trim(),
      quantity: Number(item.quantity.replace(',', '.')),
      unitGrossAmount: Number(item.unitGrossAmount.replace(',', '.')),
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
      await (activeAction.type === 'reject'
        ? appointmentsApi.rejectAppointment(appointmentId, message.trim())
        : activeAction.type === 'propose'
          ? appointmentsApi.proposeAppointmentTime(appointmentId, selectedVisitDate, message.trim())
          : appointmentsApi.completeRepair(
            appointmentId,
            repairDescription.trim(),
            repairItems.map(repairItemInput),
          ))
      const successMessage = activeAction.type === 'reject'
        ? 'Zgłoszenie zostało odrzucone.'
        : activeAction.type === 'propose'
          ? 'Nowy dzień został zapisany i czeka na potwierdzenie.'
          : 'Naprawa została zakończona. Pozycje naprawy trafią do historii i faktury.'
      resetAction()
      refreshCurrentPage()
      setNotice(successMessage)
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
      {!isLoading && loadError && appointmentPage === null && (
        <div className="message error" role="alert">
          <p>{loadError}</p>
          <button type="button" className="button secondary" onClick={retry}>Spróbuj ponownie</button>
        </div>
      )}
      {appointmentPage && appointmentPage.totalElements === 0 && statusFilter === 'ALL' && (
        <p className="empty-state">Nie ma jeszcze żadnych zgłoszeń wizyt.</p>
      )}
      {appointmentPage && (appointmentPage.totalElements > 0 || statusFilter !== 'ALL') && (
        <>
          <div className="appointment-list-controls" aria-label="Filtrowanie i sortowanie zgłoszeń">
            <label className="form-field compact-field">
              <span>Status</span>
              <select value={statusFilter} onChange={(event) => changeStatusFilter(event.target.value as StatusFilter)}>
                {statusFilterOptions.map((option) => (
                  <option key={option.value} value={option.value}>{option.label}</option>
                ))}
              </select>
            </label>
            <label className="form-field compact-field">
              <span>Sortowanie po dacie</span>
              <select value={dateSortDirection} onChange={(event) => changeDateSortDirection(event.target.value as DateSortDirection)}>
                <option value="DESC">Od najnowszych</option>
                <option value="ASC">Od najstarszych</option>
              </select>
            </label>
            <p className="appointment-list-summary">
              Pokazuję {firstVisibleIndex}–{lastVisibleIndex} z {totalElements} zgłoszeń
            </p>
          </div>
          {appointments.length === 0 ? (
            <p className="empty-state">Brak zgłoszeń pasujących do wybranego statusu.</p>
          ) : (
            <>
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

                      {appointment.status === 'COMPLETED' && appointment.requesterType === 'CLIENT' && (
                        <div className="appointment-card-actions actions">
                          <button type="button" className="button secondary"
                            disabled={downloadingInvoiceId === appointment.id}
                            onClick={() => void downloadInvoice(appointment)}>
                            {downloadingInvoiceId === appointment.id ? 'Pobieranie…' : 'Pobierz fakturę'}
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
                                  Opisz, co zostało zrobione, i dodaj pozycje naprawy. Suma brutto zostanie policzona z pozycji przez backend.
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
                                  <div className="form-field wide-field repair-items-field">
                                    <span className="field-label">Pozycje naprawy</span>
                                    <div className="repair-items-list">
                                      {repairItems.map((item, index) => (
                                        <div className="repair-item-row" key={index}>
                                          <label>
                                            <span>Typ</span>
                                            <select value={item.type} onChange={(event) => changeRepairItem(index, { type: event.target.value as RepairItemType })}>
                                              <option value="LABOR">Robocizna</option>
                                              <option value="PART">Część</option>
                                            </select>
                                          </label>
                                          <label>
                                            <span>Nazwa</span>
                                            <input value={item.name} maxLength={160} onChange={(event) => changeRepairItem(index, { name: event.target.value })} />
                                          </label>
                                          <label>
                                            <span>Ilość</span>
                                            <input type="number" min="0.01" step="0.01" value={item.quantity} onChange={(event) => changeRepairItem(index, { quantity: event.target.value })} />
                                          </label>
                                          <label>
                                            <span>Cena brutto</span>
                                            <input type="number" min="0.01" step="0.01" value={item.unitGrossAmount} onChange={(event) => changeRepairItem(index, { unitGrossAmount: event.target.value })} />
                                          </label>
                                          {repairItems.length > 1 && (
                                            <button type="button" className="button secondary" onClick={() => removeRepairItem(index)}>Usuń</button>
                                          )}
                                        </div>
                                      ))}
                                    </div>
                                    {fieldErrors.repairItems && <small className="field-error">{fieldErrors.repairItems}</small>}
                                    <button type="button" className="button secondary" onClick={addRepairItem}>Dodaj pozycję</button>
                                    <p className="repair-items-total">Podgląd sumy: <strong>{formatMoney(repairItemsTotal)}</strong> brutto</p>
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
                                  || (action === 'complete' && !canSubmitRepair)}>
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
              {totalPages > 1 && (
                <nav className="appointment-pagination" aria-label="Strony zgłoszeń">
                  <button type="button" className="button secondary"
                    disabled={currentPage === 0}
                    onClick={() => {
                      setIsLoading(true)
                      setCurrentPage((page) => Math.max(0, page - 1))
                    }}>
                    Poprzednia
                  </button>
                  <span>Strona {currentPage + 1} z {totalPages}</span>
                  <button type="button" className="button secondary"
                    disabled={currentPage >= totalPages - 1}
                    onClick={() => {
                      setIsLoading(true)
                      setCurrentPage((page) => Math.min(totalPages - 1, page + 1))
                    }}>
                    Następna
                  </button>
                </nav>
              )}
            </>
          )}
        </>
      )}
    </section>
  )
}


function formatMoney(value: number) {
  return new Intl.NumberFormat('pl-PL', { style: 'currency', currency: 'PLN' }).format(value)
}
