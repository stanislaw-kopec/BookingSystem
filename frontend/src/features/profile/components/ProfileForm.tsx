import { useState } from 'react'
import type { FormEvent, HTMLInputAutoCompleteAttribute } from 'react'
import type { ClientProfile, ClientProfileInput } from '../types'

interface TextFieldProps {
  id: string
  label: string
  value: string
  error?: string
  maxLength: number
  required?: boolean
  type?: 'text' | 'email' | 'tel'
  autoComplete?: HTMLInputAutoCompleteAttribute
  onChange: (value: string) => void
}

interface Props {
  profile: ClientProfile
  isSaving: boolean
  fieldErrors: Record<string, string>
  onSave: (input: ClientProfileInput) => Promise<void>
}

function TextField({ id, label, value, error, maxLength, required = false,
    type = 'text', autoComplete, onChange }: TextFieldProps) {
  const errorId = id + '-error'
  return (
    <div className="form-field">
      <label htmlFor={id}>{label}</label>
      <input id={id} type={type} autoComplete={autoComplete} required={required} maxLength={maxLength}
        value={value} aria-invalid={Boolean(error)} aria-describedby={error ? errorId : undefined}
        onChange={(event) => onChange(event.target.value)} />
      {error && <small id={errorId} className="field-error">{error}</small>}
    </div>
  )
}

export function ProfileForm({ profile, isSaving, fieldErrors, onSave }: Props) {
  const { configured: _configured, ...initialValues } = profile
  const [form, setForm] = useState<ClientProfileInput>(initialValues)

  function update<K extends keyof ClientProfileInput>(field: K, value: ClientProfileInput[K]) {
    setForm((current) => ({ ...current, [field]: value }))
  }

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    void onSave({
      ...form,
      firstName: form.firstName.trim(), lastName: form.lastName.trim(),
      phoneNumber: form.phoneNumber.trim(), contactEmail: form.contactEmail.trim(),
      addressLine: form.addressLine.trim(), postalCode: form.postalCode.trim(), city: form.city.trim(),
      companyName: form.companyName.trim(), taxId: form.taxId.trim(),
      billingAddressLine: form.billingAddressLine.trim(),
      billingPostalCode: form.billingPostalCode.trim(), billingCity: form.billingCity.trim(),
    })
  }

  return (
    <form className="profile-form" onSubmit={handleSubmit}>
      <fieldset disabled={isSaving}>
        <div className="form-section">
          <h3>Dane kontaktowe</h3>
          <div className="profile-form-grid">
            <TextField id="profile-first-name" label="Imię" value={form.firstName}
              error={fieldErrors.firstName} maxLength={60} required autoComplete="given-name"
              onChange={(value) => update('firstName', value)} />
            <TextField id="profile-last-name" label="Nazwisko" value={form.lastName}
              error={fieldErrors.lastName} maxLength={80} required autoComplete="family-name"
              onChange={(value) => update('lastName', value)} />
            <TextField id="profile-phone" label="Numer telefonu" value={form.phoneNumber}
              error={fieldErrors.phoneNumber} maxLength={30} required type="tel" autoComplete="tel"
              onChange={(value) => update('phoneNumber', value)} />
            <TextField id="profile-email" label="Kontaktowy adres e-mail" value={form.contactEmail}
              error={fieldErrors.contactEmail} maxLength={254} required type="email" autoComplete="email"
              onChange={(value) => update('contactEmail', value)} />
          </div>
        </div>

        <div className="form-section">
          <h3>Adres</h3>
          <div className="profile-form-grid">
            <TextField id="profile-address" label="Ulica i numer" value={form.addressLine}
              error={fieldErrors.addressLine} maxLength={150} required autoComplete="street-address"
              onChange={(value) => update('addressLine', value)} />
            <TextField id="profile-postal-code" label="Kod pocztowy" value={form.postalCode}
              error={fieldErrors.postalCode} maxLength={20} required autoComplete="postal-code"
              onChange={(value) => update('postalCode', value)} />
            <TextField id="profile-city" label="Miejscowość" value={form.city}
              error={fieldErrors.city} maxLength={80} required autoComplete="address-level2"
              onChange={(value) => update('city', value)} />
          </div>
        </div>

        <label className="checkbox-field" htmlFor="profile-company">
          <input id="profile-company" type="checkbox" checked={form.hasCompanyData}
            onChange={(event) => update('hasCompanyData', event.target.checked)} />
          Chcę podać dane firmy do rozliczeń
        </label>

        {form.hasCompanyData && (
          <div className="form-section company-fields">
            <h3>Dane firmy</h3>
            <div className="profile-form-grid">
              <TextField id="profile-company-name" label="Nazwa firmy" value={form.companyName}
                error={fieldErrors.companyName} maxLength={150} required autoComplete="organization"
                onChange={(value) => update('companyName', value)} />
              <TextField id="profile-tax-id" label="NIP" value={form.taxId}
                error={fieldErrors.taxId} maxLength={32} required
                onChange={(value) => update('taxId', value)} />
              <TextField id="profile-billing-address" label="Ulica i numer — adres rozliczeniowy"
                value={form.billingAddressLine} error={fieldErrors.billingAddressLine}
                maxLength={150} required onChange={(value) => update('billingAddressLine', value)} />
              <TextField id="profile-billing-postal-code" label="Kod pocztowy" value={form.billingPostalCode}
                error={fieldErrors.billingPostalCode} maxLength={20} required
                onChange={(value) => update('billingPostalCode', value)} />
              <TextField id="profile-billing-city" label="Miejscowość" value={form.billingCity}
                error={fieldErrors.billingCity} maxLength={80} required
                onChange={(value) => update('billingCity', value)} />
            </div>
          </div>
        )}

        <button className="button" type="submit">{isSaving ? 'Zapisywanie…' : 'Zapisz profil'}</button>
      </fieldset>
    </form>
  )
}
