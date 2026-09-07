import type { ClientProfile } from '../types'

interface Props {
  profile: ClientProfile
  onEdit: () => void
}

function Detail({ label, value }: { label: string; value: string }) {
  return (
    <div className="profile-detail">
      <dt>{label}</dt>
      <dd>{value}</dd>
    </div>
  )
}

export function ProfileDetails({ profile, onEdit }: Props) {
  return (
    <div className="profile-summary">
      <div className="profile-summary-header">
        <div>
          <h3>Twoje dane</h3>
          <p className="muted">Dane przekazywane warsztatowi przy obsłudze Twoich zgłoszeń.</p>
        </div>
        <button type="button" className="button secondary" onClick={onEdit}>Edytuj profil</button>
      </div>

      <section className="profile-details-section" aria-labelledby="contact-details-heading">
        <h4 id="contact-details-heading">Dane kontaktowe</h4>
        <dl className="profile-details-grid">
          <Detail label="Imię i nazwisko" value={`${profile.firstName} ${profile.lastName}`} />
          <Detail label="Numer telefonu" value={profile.phoneNumber} />
          <Detail label="Kontaktowy adres e-mail" value={profile.contactEmail} />
          <Detail label="Adres" value={`${profile.addressLine}, ${profile.postalCode} ${profile.city}`} />
        </dl>
      </section>

      <section className="profile-details-section" aria-labelledby="billing-details-heading">
        <h4 id="billing-details-heading">Dane firmy</h4>
        {profile.hasCompanyData ? (
          <dl className="profile-details-grid">
            <Detail label="Nazwa firmy" value={profile.companyName} />
            <Detail label="NIP" value={profile.taxId} />
            <Detail label="Adres rozliczeniowy"
              value={`${profile.billingAddressLine}, ${profile.billingPostalCode} ${profile.billingCity}`} />
          </dl>
        ) : (
          <p className="muted">Nie podano danych firmy.</p>
        )}
      </section>
    </div>
  )
}
