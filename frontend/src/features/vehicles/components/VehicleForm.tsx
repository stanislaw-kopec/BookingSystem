import { useState } from 'react'
import type { FormEvent } from 'react'
import type { VehicleInput } from '../types'

interface Props {
  isSaving: boolean
  fieldErrors: Record<string, string>
  onSave: (input: VehicleInput) => Promise<void>
  onCancel?: () => void
}

const currentYear = new Date().getFullYear()

export function VehicleForm({ isSaving, fieldErrors, onSave, onCancel }: Props) {
  const [make, setMake] = useState('')
  const [model, setModel] = useState('')
  const [productionYear, setProductionYear] = useState(String(currentYear))
  const [registrationNumber, setRegistrationNumber] = useState('')
  const [vin, setVin] = useState('')

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    void onSave({
      make: make.trim(),
      model: model.trim(),
      productionYear: Number(productionYear),
      registrationNumber: registrationNumber.trim(),
      vin: vin.trim(),
    })
  }

  return (
    <form className="vehicle-form" onSubmit={handleSubmit}>
      <fieldset disabled={isSaving}>
        <legend>Nowy pojazd</legend>
        <div className="vehicle-form-grid">
          <div className="form-field">
            <label htmlFor="vehicle-make">Marka</label>
            <input id="vehicle-make" value={make} required maxLength={80}
              aria-invalid={Boolean(fieldErrors.make)}
              aria-describedby={fieldErrors.make ? 'vehicle-make-error' : undefined}
              onChange={(event) => setMake(event.target.value)} />
            {fieldErrors.make && <small id="vehicle-make-error" className="field-error">{fieldErrors.make}</small>}
          </div>
          <div className="form-field">
            <label htmlFor="vehicle-model">Model</label>
            <input id="vehicle-model" value={model} required maxLength={80}
              aria-invalid={Boolean(fieldErrors.model)}
              aria-describedby={fieldErrors.model ? 'vehicle-model-error' : undefined}
              onChange={(event) => setModel(event.target.value)} />
            {fieldErrors.model && <small id="vehicle-model-error" className="field-error">{fieldErrors.model}</small>}
          </div>
          <div className="form-field">
            <label htmlFor="vehicle-production-year">Rok produkcji</label>
            <input id="vehicle-production-year" type="number" min={1886} max={currentYear + 1}
              value={productionYear} required aria-invalid={Boolean(fieldErrors.productionYear)}
              aria-describedby={fieldErrors.productionYear ? 'vehicle-production-year-error' : undefined}
              onChange={(event) => setProductionYear(event.target.value)} />
            {fieldErrors.productionYear && (
              <small id="vehicle-production-year-error" className="field-error">{fieldErrors.productionYear}</small>
            )}
          </div>
          <div className="form-field">
            <label htmlFor="vehicle-registration-number">Numer rejestracyjny</label>
            <input id="vehicle-registration-number" value={registrationNumber} required minLength={2} maxLength={20}
              pattern="[A-Za-z0-9 -]+" aria-invalid={Boolean(fieldErrors.registrationNumber)}
              aria-describedby={fieldErrors.registrationNumber ? 'vehicle-registration-number-error' : undefined}
              onChange={(event) => setRegistrationNumber(event.target.value)} />
            {fieldErrors.registrationNumber && (
              <small id="vehicle-registration-number-error" className="field-error">
                {fieldErrors.registrationNumber}
              </small>
            )}
          </div>
          <div className="form-field vehicle-vin-field">
            <label htmlFor="vehicle-vin">VIN <span className="muted">(opcjonalnie)</span></label>
            <input id="vehicle-vin" value={vin} minLength={17} maxLength={17}
              pattern="[A-HJ-NPR-Za-hj-npr-z0-9]{17}" aria-invalid={Boolean(fieldErrors.vin)}
              aria-describedby={fieldErrors.vin ? 'vehicle-vin-help vehicle-vin-error' : 'vehicle-vin-help'}
              onChange={(event) => setVin(event.target.value)} />
            <small id="vehicle-vin-help" className="muted">17 znaków, bez liter I, O oraz Q.</small>
            {fieldErrors.vin && <small id="vehicle-vin-error" className="field-error">{fieldErrors.vin}</small>}
          </div>
        </div>
        <div className="actions vehicle-form-actions">
          {onCancel && <button type="button" className="button secondary" onClick={onCancel}>Anuluj</button>}
          <button type="submit" className="button">{isSaving ? 'Dodawanie…' : 'Dodaj pojazd'}</button>
        </div>
      </fieldset>
    </form>
  )
}
