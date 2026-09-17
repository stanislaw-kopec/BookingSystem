import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { CustomSelect } from '../../../components/ui/CustomSelect'
import { errorMessage } from '../../../api/apiClient'
import * as appointmentsApi from '../api/appointmentsApi'
import * as vehiclesApi from '../../vehicles/api/vehiclesApi'
import type { Appointment, AppointmentPage, AppointmentStatus } from '../types'
import { appointmentStatusLabel } from '../appointmentLabels'
import { AppointmentDetails } from './AppointmentDetails'
import '../appointments.css'

const pageSize = 5

type StatusFilter = AppointmentStatus | 'ALL'
type DateSortDirection = 'DESC' | 'ASC'

const cancellableStatuses = new Set<Appointment['status']>([
  'PENDING',
  'TIME_PROPOSED',
  'CONFIRMED',
])

export function ClientAppointmentsSection() {
  const [appointmentPage, setAppointmentPage] = useState<AppointmentPage | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)
  const [confirmingId, setConfirmingId] = useState<number | null>(null)
  const [cancellingId, setCancellingId] = useState<number | null>(null)
  const [downloadingInvoiceId, setDownloadingInvoiceId] = useState<number | null>(null)
  const [statusFilter, setStatusFilter] = useState<StatusFilter>('ALL')
  const [dateSortDirection, setDateSortDirection] = useState<DateSortDirection>('DESC')
  const [currentPage, setCurrentPage] = useState(0)
  const [notice, setNotice] = useState<string | null>(null)
  const [revision, setRevision] = useState(0)

  useEffect(() => {
    const controller = new AbortController()
    let redirected = false
    appointmentsApi.getClientAppointments(currentPage, pageSize, dateSortDirection, statusFilter, controller.signal)
      .then((result) => {
        if (!controller.signal.aborted) {
          const lastPage = Math.max(0, result.totalPages - 1)
          if (currentPage > lastPage) {
            redirected = true
            setCurrentPage(lastPage)
            return
          }
          setAppointmentPage(result)
          setError(null)
        }
      })
      .catch((cause: unknown) => {
        if (!controller.signal.aborted) {
          setAppointmentPage(null)
          setError(errorMessage(cause))
        }
      })
      .finally(() => {
        if (!controller.signal.aborted && !redirected) setIsLoading(false)
      })
    return () => controller.abort()
  }, [currentPage, dateSortDirection, revision, statusFilter])

  const appointments = appointmentPage?.content ?? []
  const totalElements = appointmentPage?.totalElements ?? 0
  const totalPages = appointmentPage?.totalPages ?? 0
  const firstVisibleIndex = totalElements === 0 ? 0 : currentPage * pageSize + 1
  const lastVisibleIndex = Math.min((currentPage * pageSize) + appointments.length, totalElements)
  const statusFilterOptions: Array<{ value: StatusFilter, label: string }> = [
    { value: 'ALL', label: 'Wszystkie statusy' },
    { value: 'PENDING', label: appointmentStatusLabel('PENDING') },
    { value: 'TIME_PROPOSED', label: appointmentStatusLabel('TIME_PROPOSED') },
    { value: 'CONFIRMED', label: appointmentStatusLabel('CONFIRMED') },
    { value: 'READY_FOR_PICKUP', label: appointmentStatusLabel('READY_FOR_PICKUP') },
    { value: 'COMPLETED', label: appointmentStatusLabel('COMPLETED') },
    { value: 'CANCELLED', label: appointmentStatusLabel('CANCELLED') },
    { value: 'REJECTED', label: appointmentStatusLabel('REJECTED') },
  ]

  function retry() {
    setIsLoading(true)
    setError(null)
    setRevision((value) => value + 1)
  }

  function refreshCurrentPage() {
    setIsLoading(true)
    setError(null)
    setRevision((value) => value + 1)
  }

  function changeStatusFilter(value: StatusFilter) {
    if (value === statusFilter) return
    setIsLoading(true)
    setError(null)
    setStatusFilter(value)
    setCurrentPage(0)
  }

  function changeDateSortDirection(value: DateSortDirection) {
    if (value === dateSortDirection) return
    setIsLoading(true)
    setError(null)
    setDateSortDirection(value)
    setCurrentPage(0)
  }

  async function confirmProposedTime(appointmentId: number) {
    setConfirmingId(appointmentId)
    setActionError(null)
    setNotice(null)
    try {
      await appointmentsApi.confirmProposedTime(appointmentId)
      refreshCurrentPage()
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
      await appointmentsApi.cancelClientAppointment(appointmentId)
      refreshCurrentPage()
      setNotice('Wizyta została odwołana.')
    } catch (cause) {
      setActionError(errorMessage(cause))
    } finally {
      setCancellingId(null)
    }
  }

  async function downloadInvoice(appointment: Appointment) {
    if (appointment.vehicleId === null) return
    setDownloadingInvoiceId(appointment.id)
    setActionError(null)
    setNotice(null)
    try {
      const invoice = await vehiclesApi.downloadRepairInvoice(appointment.vehicleId, appointment.id)
      const url = URL.createObjectURL(invoice)
      const link = document.createElement('a')
      link.href = url
      link.download = `invoice-${appointment.reference}.pdf`
      document.body.append(link)
      link.click()
      link.remove()
      URL.revokeObjectURL(url)
    } catch (cause) {
      setActionError(errorMessage(cause))
    } finally {
      setDownloadingInvoiceId(null)
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
        {!isLoading && error && (
          <div className="message error" role="alert">
            <p>{error}</p>
            <button type="button" className="button secondary" onClick={retry}>Spróbuj ponownie</button>
          </div>
        )}
        {(
          <>
            <div className="appointment-list-controls" aria-label="Filtrowanie i sortowanie wizyt">
              <label className="form-field compact-field">
                <span>Status</span>
                <CustomSelect id="client-appointment-status-filter" value={statusFilter}
                  options={statusFilterOptions}
                  onChange={(value) => changeStatusFilter(value as StatusFilter)} />
              </label>
              <label className="form-field compact-field">
                <span>Sortowanie po dacie</span>
                <CustomSelect id="client-appointment-date-sort" value={dateSortDirection}
                  options={[
                    { value: 'DESC', label: 'Od najnowszych' },
                    { value: 'ASC', label: 'Od najstarszych' },
                  ]}
                  onChange={(value) => changeDateSortDirection(value as DateSortDirection)} />
              </label>
              {!isLoading && !error && appointmentPage && <p className="appointment-list-summary">
                Pokazuję {firstVisibleIndex}–{lastVisibleIndex} z {totalElements} zgłoszeń
              </p>}
            </div>
            {!isLoading && !error && appointmentPage && (appointments.length === 0 ? (
              <p className="empty-state">{statusFilter === 'ALL' ? 'Nie masz jeszcze żadnych zgłoszeń wizyt.' : 'Brak zgłoszeń pasujących do wybranego statusu.'}</p>
            ) : (
              <>
                <ul className="appointment-list">
                  {appointments.map((appointment) => (
                    <li className="appointment-card" key={appointment.id}>
                      <AppointmentDetails appointment={appointment} />
                      {(appointment.status === 'TIME_PROPOSED'
                        || cancellableStatuses.has(appointment.status)
                        || (appointment.status === 'COMPLETED' && appointment.vehicleId !== null)) && (
                        <div className="appointment-card-actions actions">
                          {appointment.status === 'COMPLETED' && appointment.vehicleId !== null && (
                            <button type="button" className="button secondary"
                              disabled={downloadingInvoiceId === appointment.id}
                              onClick={() => void downloadInvoice(appointment)}>
                              {downloadingInvoiceId === appointment.id ? 'Pobieranie…' : 'Pobierz fakturę'}
                            </button>
                          )}
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
                {totalPages > 1 && (
                  <nav className="appointment-pagination" aria-label="Strony wizyt">
                    <button type="button" className="button secondary"
                      disabled={currentPage === 0}
                      onClick={() => {
                        setIsLoading(true)
                        setError(null)
                        setCurrentPage((page) => Math.max(0, page - 1))
                      }}>
                      Poprzednia
                    </button>
                    <span>Strona {currentPage + 1} z {totalPages}</span>
                    <button type="button" className="button secondary"
                      disabled={currentPage >= totalPages - 1}
                      onClick={() => {
                        setIsLoading(true)
                        setError(null)
                        setCurrentPage((page) => Math.min(totalPages - 1, page + 1))
                      }}>
                      Następna
                    </button>
                  </nav>
                )}
              </>
            ))}
          </>
        )}
      </section>
    </section>
  )
}
