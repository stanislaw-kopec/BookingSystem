import { useState } from 'react'
import type { FormEvent } from 'react'
import type { CategoryInput, ServiceCategory } from '../types'

interface Props {
  category: ServiceCategory | null
  disabled: boolean
  fieldErrors: Record<string, string>
  onSave: (input: CategoryInput) => Promise<void>
  onCancel: () => void
}

export function CategoryForm({ category, disabled, fieldErrors, onSave, onCancel }: Props) {
  const [name, setName] = useState(category?.name ?? '')
  const [description, setDescription] = useState(category?.description ?? '')

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    void onSave({ name, description })
  }

  return (
    <form onSubmit={handleSubmit} className="editor-form" aria-labelledby="category-form-heading">
      <h3 id="category-form-heading">{category ? 'Edytuj kategorię' : 'Nowa kategoria'}</h3>
      <fieldset disabled={disabled}>
        <label htmlFor="category-name">Nazwa kategorii</label>
        <input id="category-name" value={name} onChange={(event) => setName(event.target.value)}
          required maxLength={100} aria-invalid={Boolean(fieldErrors.name)} aria-describedby="category-name-error" />
        <span id="category-name-error" className="field-error">{fieldErrors.name}</span>
        <label htmlFor="category-description">Opis kategorii (opcjonalnie)</label>
        <textarea id="category-description" value={description} onChange={(event) => setDescription(event.target.value)}
          maxLength={500} rows={3} aria-invalid={Boolean(fieldErrors.description)} aria-describedby="category-description-error" />
        <span id="category-description-error" className="field-error">{fieldErrors.description}</span>
        <div className="actions">
          <button className="button" type="submit">{category ? 'Zapisz kategorię' : 'Dodaj kategorię'}</button>
          {category && <button className="button secondary" type="button" onClick={onCancel}>Anuluj edycję kategorii</button>}
        </div>
      </fieldset>
    </form>
  )
}
