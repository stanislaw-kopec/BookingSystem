import { useState } from 'react'
import type { FormEvent } from 'react'
import type { ServiceCategory, ServiceInput, WorkshopService } from '../types'

interface Props {
  service: WorkshopService | null
  categories: ServiceCategory[]
  disabled: boolean
  fieldErrors: Record<string, string>
  onSave: (input: ServiceInput) => Promise<void>
  onCancel: () => void
}

export function ServiceForm({ service, categories, disabled, fieldErrors, onSave, onCancel }: Props) {
  const [categoryId, setCategoryId] = useState(service ? String(service.categoryId) : '')
  const [name, setName] = useState(service?.name ?? '')
  const [description, setDescription] = useState(service?.description ?? '')

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    void onSave({ categoryId: Number(categoryId), name, description })
  }

  return (
    <form onSubmit={handleSubmit} className="editor-form" aria-labelledby="service-form-heading">
      <h3 id="service-form-heading">{service ? 'Edytuj usługę' : 'Nowa usługa'}</h3>
      {categories.length === 0 && <p className="muted">Najpierw dodaj kategorię.</p>}
      <fieldset disabled={disabled || categories.length === 0}>
        <label htmlFor="service-category">Kategoria usługi</label>
        <select id="service-category" value={categoryId} onChange={(event) => setCategoryId(event.target.value)}
          required aria-invalid={Boolean(fieldErrors.categoryId)} aria-describedby="service-category-error">
          <option value="">Wybierz kategorię</option>
          {categories.map((category) => <option key={category.id} value={category.id}>{category.name}</option>)}
        </select>
        <span id="service-category-error" className="field-error">{fieldErrors.categoryId}</span>
        <label htmlFor="service-name">Nazwa usługi</label>
        <input id="service-name" value={name} onChange={(event) => setName(event.target.value)}
          required maxLength={120} aria-invalid={Boolean(fieldErrors.name)} aria-describedby="service-name-error" />
        <span id="service-name-error" className="field-error">{fieldErrors.name}</span>
        <label htmlFor="service-description">Opis usługi (opcjonalnie)</label>
        <textarea id="service-description" value={description} onChange={(event) => setDescription(event.target.value)}
          maxLength={1000} rows={3} aria-invalid={Boolean(fieldErrors.description)} aria-describedby="service-description-error" />
        <span id="service-description-error" className="field-error">{fieldErrors.description}</span>
        <div className="actions">
          <button className="button" type="submit">{service ? 'Zapisz usługę' : 'Dodaj usługę'}</button>
          {service && <button className="button secondary" type="button" onClick={onCancel}>Anuluj edycję usługi</button>}
        </div>
      </fieldset>
    </form>
  )
}
