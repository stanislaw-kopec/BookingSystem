import { formatAppointmentDate } from '../dateTime'
import type { Appointment } from '../types'
import { AppointmentStatusBadge } from './AppointmentStatusBadge'

interface Props {
  appointment: Appointment
  showContact?: boolean
}

export function AppointmentDetails({ appointment, showContact = false }: Props) {
  const timeChanged = appointment.currentStartAt !== appointment.requestedStartAt

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
