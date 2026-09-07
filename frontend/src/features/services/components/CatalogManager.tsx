import { useState } from 'react'
import { ApiError, errorMessage } from '../../../api/apiClient'
import * as servicesApi from '../api/servicesApi'
import type { CategoryInput, ServiceCategory, ServiceInput, WorkshopService } from '../types'
import { CategoryForm } from './CategoryForm'
import { ServiceForm } from './ServiceForm'

type FormSource = 'category' | 'service' | 'delete'
interface MutationError { source: FormSource; message: string; fields: Record<string, string> }
interface DeleteTarget { type: 'category' | 'service'; id: number; name: string }
interface Props {
  categories: ServiceCategory[]
  isRefreshing: boolean
  onChanged: () => void
}

export function CatalogManager({ categories, isRefreshing, onChanged }: Props) {
  const [editingCategory, setEditingCategory] = useState<ServiceCategory | null>(null)
  const [editingService, setEditingService] = useState<WorkshopService | null>(null)
  const [categoryFormVersion, setCategoryFormVersion] = useState(0)
  const [serviceFormVersion, setServiceFormVersion] = useState(0)
  const [deleteTarget, setDeleteTarget] = useState<DeleteTarget | null>(null)
  const [isSaving, setIsSaving] = useState(false)
  const [error, setError] = useState<MutationError | null>(null)
  const [notice, setNotice] = useState<string | null>(null)
  const disabled = isSaving || isRefreshing

  async function mutate(source: FormSource, action: () => Promise<void>, success: string): Promise<boolean> {
    setIsSaving(true)
    setError(null)
    setNotice(null)
    try {
      await action()
      onChanged()
      setNotice(success)
      return true
    } catch (cause) {
      setError({ source, message: errorMessage(cause), fields: cause instanceof ApiError ? cause.fieldErrors : {} })
      return false
    } finally {
      setIsSaving(false)
    }
  }

  async function handleCategorySave(input: CategoryInput) {
    if (await mutate('category', () => servicesApi.saveCategory(input, editingCategory?.id), 'Kategoria została zapisana.')) {
      setEditingCategory(null)
      setCategoryFormVersion((value) => value + 1)
    }
  }

  async function handleServiceSave(input: ServiceInput) {
    if (await mutate('service', () => servicesApi.saveService(input, editingService?.id), 'Usługa została zapisana.')) {
      setEditingService(null)
      setServiceFormVersion((value) => value + 1)
    }
  }

  async function handleDelete() {
    if (!deleteTarget) return
    const target = deleteTarget
    const action = () => target.type === 'category'
      ? servicesApi.deleteCategory(target.id) : servicesApi.deleteService(target.id)
    if (await mutate('delete', action, 'Element został usunięty.')) {
      if (target.type === 'category' && editingCategory?.id === target.id) setEditingCategory(null)
      if (target.type === 'service' && editingService?.id === target.id) setEditingService(null)
      setDeleteTarget(null)
    }
  }

  function editCategory(category: ServiceCategory) {
    setEditingCategory(category)
    setError(null)
    document.getElementById('category-form-heading')?.scrollIntoView({ block: 'center' })
  }

  function editService(service: WorkshopService) {
    setEditingService(service)
    setError(null)
    document.getElementById('service-form-heading')?.scrollIntoView({ block: 'center' })
  }

  return (
    <section id="service-management" className="page-section" aria-labelledby="manager-heading">
      <div className="section-heading">
        <p className="eyebrow">Panel personelu</p>
        <h2 id="manager-heading">Zarządzanie ofertą</h2>
        <p className="muted">Dodawaj i edytuj kategorie oraz przypisane do nich usługi.</p>
      </div>
      {notice && <p className="message success" role="status">{notice}</p>}
      {error && <p className="message error" role="alert">{error.message}</p>}
      {isSaving && <p role="status">Zapisywanie zmian…</p>}
      <div className="editor-grid">
        <CategoryForm key={'category-' + (editingCategory?.id ?? 'new') + '-' + categoryFormVersion}
          category={editingCategory} disabled={disabled}
          fieldErrors={error?.source === 'category' ? error.fields : {}}
          onSave={handleCategorySave} onCancel={() => { setEditingCategory(null); setError(null) }} />
        <ServiceForm key={'service-' + (editingService?.id ?? 'new') + '-' + serviceFormVersion}
          service={editingService} categories={categories} disabled={disabled}
          fieldErrors={error?.source === 'service' ? error.fields : {}}
          onSave={handleServiceSave} onCancel={() => { setEditingService(null); setError(null) }} />
      </div>

      <h3>Kategorie i usługi do edycji</h3>
      {deleteTarget && (
        <div className="delete-confirmation" role="group" aria-label="Potwierdzenie usunięcia">
          <p>Czy usunąć „{deleteTarget.name}”?</p>
          <div className="actions">
            <button type="button" className="button danger" disabled={disabled} onClick={() => void handleDelete()}>Potwierdź usunięcie</button>
            <button type="button" className="button secondary" disabled={disabled} onClick={() => setDeleteTarget(null)}>Anuluj usunięcie</button>
          </div>
        </div>
      )}
      {categories.length === 0 && <p className="empty-state">Nie ma jeszcze kategorii. Dodaj pierwszą powyżej.</p>}
      <ul className="management-list">
        {categories.map((category) => (
          <li key={category.id}>
            <div className="management-row">
              <h4>{category.name}</h4>
              <div className="actions">
                <button type="button" className="button secondary small" disabled={disabled}
                  aria-label={'Edytuj kategorię ' + category.name} onClick={() => editCategory(category)}>Edytuj kategorię</button>
                <button type="button" className="button secondary small" disabled={disabled || category.services.length > 0}
                  title={category.services.length > 0 ? 'Najpierw przenieś lub usuń usługi z tej kategorii.' : undefined}
                  aria-label={'Usuń kategorię ' + category.name}
                  onClick={() => setDeleteTarget({ type: 'category', id: category.id, name: category.name })}>Usuń kategorię</button>
              </div>
            </div>
            {category.services.length > 0 && (
              <ul>
                {category.services.map((service) => (
                  <li key={service.id} className="management-row">
                    <span>{service.name}</span>
                    <div className="actions">
                      <button type="button" className="button secondary small" disabled={disabled}
                        aria-label={'Edytuj usługę ' + service.name} onClick={() => editService(service)}>Edytuj</button>
                      <button type="button" className="button secondary small" disabled={disabled}
                        aria-label={'Usuń usługę ' + service.name}
                        onClick={() => setDeleteTarget({ type: 'service', id: service.id, name: service.name })}>Usuń</button>
                    </div>
                  </li>
                ))}
              </ul>
            )}
          </li>
        ))}
      </ul>
    </section>
  )
}
