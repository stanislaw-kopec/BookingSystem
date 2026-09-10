import { formatAppointmentDate } from '../dateTime'
import type { Appointment } from '../types'
import { AppointmentStatusBadge } from './AppointmentStatusBadge'

interface Props {
  appointment: Appointment
  showContact?: boolean
}

export function AppointmentDetails({ appointment, showContact = false }: Props) {
  const timeChanged = appointment.currentStartAt !== appointment.requestedStartAt
  const formattedTotal = appointment.totalGrossAmount === null
    ? ''
    : new Intl.NumberFormat('pl-PL', { style: 'currency', currency: 'PLN' })
      .format(appointment.totalGrossAmount)

  return (
    <div className="appointment-card-content">
      <div className="appointment-card-heading">
        <div>
          <p className="appointment-reference">Zgłoszenie {appointment.reference}</p>
          <h3>{appointment.vehicleMake} {appointment.vehicleModel}</h3>
          <p className="muted">{appointment.vehicleRegistrationNumber} · {appointment.vehicleProductionYear}</p>
        </div>
        <AppointmentStatusBadge status={appointment.status} />
      </div>

      <dl className="appointment-facts">
        <div>
          <dt>{timeChanged ? 'Pierwotnie wybrany dzień' : 'Dzień przyjęcia auta'}</dt>
          <dd>{formatAppointmentDate(appointment.requestedStartAt)}</dd>
        </div>
        {timeChanged && (
          <div className="proposed-time">
            <dt>{appointment.status === 'TIME_PROPOSED' ? 'Nowy dzień do potwierdzenia' : 'Aktualny dzień'}</dt>
            <dd>{formatAppointmentDate(appointment.currentStartAt)}</dd>
          </div>
        )}
        <div>
          <dt>Opis usterki</dt>
          <dd className="appointment-description">{appointment.problemDescription}</dd>
        </div>
        {appointment.staffMessage && (
          <div>
            <dt>Wiadomość z warsztatu</dt>
            <dd>{appointment.staffMessage}</dd>
          </div>
        )}
        {appointment.repairDescription && (
          <>
            <div>
              <dt>Wykonane prace</dt>
              <dd className="appointment-description">{appointment.repairDescription}</dd>
            </div>
            <div>
              <dt>Do zapłaty przy odbiorze</dt>
              <dd>{formattedTotal} brutto</dd>
            </div>
            {appointment.repairCompletedAt && (
              <div>
                <dt>Naprawę zakończono</dt>
                <dd>{formatAppointmentDate(appointment.repairCompletedAt)}</dd>
              </div>
            )}
          </>
        )}
        {showContact && (
          <div>
            <dt>Klient i kontakt</dt>
            <dd>
              {appointment.firstName} {appointment.lastName}
              {appointment.phoneNumber && <> · {appointment.phoneNumber}</>}
              {appointment.contactEmail && <> · {appointment.contactEmail}</>}
            </dd>
          </div>
        )}
      </dl>
    </div>
  )
}
