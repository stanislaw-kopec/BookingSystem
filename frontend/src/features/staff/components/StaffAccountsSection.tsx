import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { ApiError, errorMessage } from '../../../api/apiClient'
import * as staffAccountsApi from '../api/staffAccountsApi'
import type { StaffAccount, StaffAccountInput, StaffAccountUpdateInput, StaffPasswordResetInput } from '../types'
import '../staff.css'

const emptyCreateForm: StaffAccountInput = {
  username: '',
  email: '',
  password: '',
  passwordConfirmation: '',
}

const emptyPasswordForm: StaffPasswordResetInput = {
  password: '',
  passwordConfirmation: '',
}

export function StaffAccountsSection() {
  const [mechanics, setMechanics] = useState<StaffAccount[] | null>(null)
  const [createForm, setCreateForm] = useState<StaffAccountInput>(emptyCreateForm)
  const [editForms, setEditForms] = useState<Record<number, StaffAccountUpdateInput>>({})
  const [passwordForms, setPasswordForms] = useState<Record<number, StaffPasswordResetInput>>({})
  const [editingId, setEditingId] = useState<number | null>(null)
  const [passwordResetId, setPasswordResetId] = useState<number | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [savingAction, setSavingAction] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})

  useEffect(() => {
    const controller = new AbortController()
    staffAccountsApi.getMechanicAccounts(controller.signal)
      .then((result) => {
        if (!controller.signal.aborted) {
          setMechanics(result)
          setEditForms(formsFrom(result))
          setError(null)
        }
      })
      .catch((cause: unknown) => {
        if (!controller.signal.aborted) setError(errorMessage(cause))
      })
      .finally(() => {
        if (!controller.signal.aborted) setIsLoading(false)
      })
    return () => controller.abort()
  }, [])

  async function submitCreate(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setSavingAction('create')
    setError(null)
    setNotice(null)
    setFieldErrors({})
    try {
      const created = await staffAccountsApi.createMechanicAccount(createForm)
      applyMechanic(created)
      setCreateForm(emptyCreateForm)
      setNotice(`Konto mechanika ${created.username} zostało utworzone.`)
    } catch (cause) {
      handleFailure(cause)
    } finally {
      setSavingAction(null)
    }
  }

  async function submitEdit(event: FormEvent<HTMLFormElement>, mechanic: StaffAccount) {
    event.preventDefault()
    const form = editForms[mechanic.id]
    if (!form) return
    setSavingAction(`edit-${mechanic.id}`)
    setError(null)
    setNotice(null)
    setFieldErrors({})
    try {
      const updated = await staffAccountsApi.updateMechanicAccount(mechanic.id, form)
      applyMechanic(updated)
      setEditingId(null)
      setNotice(`Dane konta ${updated.username} zostały zapisane.`)
    } catch (cause) {
      handleFailure(cause)
    } finally {
      setSavingAction(null)
    }
  }

  async function submitPasswordReset(event: FormEvent<HTMLFormElement>, mechanic: StaffAccount) {
    event.preventDefault()
    const form = passwordForms[mechanic.id] ?? emptyPasswordForm
    setSavingAction(`password-${mechanic.id}`)
    setError(null)
    setNotice(null)
    setFieldErrors({})
    try {
      const updated = await staffAccountsApi.resetMechanicPassword(mechanic.id, form)
      applyMechanic(updated)
      setPasswordForms((current) => ({ ...current, [mechanic.id]: emptyPasswordForm }))
      setPasswordResetId(null)
      setNotice(`Hasło konta ${updated.username} zostało ustawione.`)
    } catch (cause) {
      handleFailure(cause)
    } finally {
      setSavingAction(null)
    }
  }

  async function toggleStatus(mechanic: StaffAccount) {
    const nextEnabled = !mechanic.enabled
    setSavingAction(`status-${mechanic.id}`)
    setError(null)
    setNotice(null)
    setFieldErrors({})
    try {
      const updated = await staffAccountsApi.updateMechanicStatus(mechanic.id, nextEnabled)
      applyMechanic(updated)
      setNotice(nextEnabled
        ? `Konto ${updated.username} zostało włączone.`
        : `Konto ${updated.username} zostało dezaktywowane.`)
    } catch (cause) {
      handleFailure(cause)
    } finally {
      setSavingAction(null)
    }
  }

  function applyMechanic(mechanic: StaffAccount) {
    setMechanics((current) => {
      const list = current ?? []
      const exists = list.some((item) => item.id === mechanic.id)
      const next = exists ? list.map((item) => item.id === mechanic.id ? mechanic : item) : [...list, mechanic]
      return next.sort((first, second) => first.username.localeCompare(second.username))
    })
    setEditForms((current) => ({ ...current, [mechanic.id]: { username: mechanic.username, email: mechanic.email } }))
  }

  function handleFailure(cause: unknown) {
    setError(errorMessage(cause))
    if (cause instanceof ApiError) setFieldErrors(cause.fieldErrors)
  }

  function updateCreateField(field: keyof StaffAccountInput, value: string) {
    setCreateForm((current) => ({ ...current, [field]: value }))
    clearFieldError(field)
  }

  function updateEditField(mechanicId: number, field: keyof StaffAccountUpdateInput, value: string) {
    setEditForms((current) => ({
      ...current,
      [mechanicId]: { ...current[mechanicId], [field]: value },
    }))
    clearFieldError(field)
  }

  function updatePasswordField(mechanicId: number, field: keyof StaffPasswordResetInput, value: string) {
    setPasswordForms((current) => ({
      ...current,
      [mechanicId]: { ...(current[mechanicId] ?? emptyPasswordForm), [field]: value },
    }))
    clearFieldError(field)
  }

  function clearFieldError(field: string) {
    setFieldErrors((current) => ({ ...current, [field]: '' }))
    setError(null)
  }

  const isCreating = savingAction === 'create'

  return (
    <section className="page-section staff-accounts-section" aria-labelledby="staff-accounts-heading">
      <div className="section-heading">
        <p className="eyebrow">Panel administratora</p>
        <h2 id="staff-accounts-heading">Konta mechaników</h2>
        <p className="muted">Twórz konta, poprawiaj dane, ustawiaj nowe hasła i dezaktywuj dostęp mechaników.</p>
      </div>

      {error && <p className="message error" role="alert">{error}</p>}
      {notice && <p className="message success" role="status">{notice}</p>}

      <div className="staff-accounts-layout">
        <form className="staff-account-card" onSubmit={(event) => void submitCreate(event)}>
          <h3>Nowe konto mechanika</h3>
          <label className="form-field">
            <span>Login</span>
            <input autoComplete="username" value={createForm.username} required minLength={3} maxLength={30}
              aria-invalid={Boolean(fieldErrors.username)}
              onChange={(event) => updateCreateField('username', event.target.value)} />
            {fieldErrors.username && <small className="field-error">{fieldErrors.username}</small>}
          </label>
          <label className="form-field">
            <span>E-mail</span>
            <input type="email" autoComplete="email" value={createForm.email} required maxLength={254}
              aria-invalid={Boolean(fieldErrors.email)}
              onChange={(event) => updateCreateField('email', event.target.value)} />
            {fieldErrors.email && <small className="field-error">{fieldErrors.email}</small>}
          </label>
          <label className="form-field">
            <span>Hasło tymczasowe</span>
            <input type="password" autoComplete="new-password" value={createForm.password} required minLength={8} maxLength={64}
              aria-invalid={Boolean(fieldErrors.password)}
              onChange={(event) => updateCreateField('password', event.target.value)} />
            {fieldErrors.password && <small className="field-error">{fieldErrors.password}</small>}
          </label>
          <label className="form-field">
            <span>Powtórz hasło</span>
            <input type="password" autoComplete="new-password" value={createForm.passwordConfirmation} required maxLength={64}
              aria-invalid={Boolean(fieldErrors.passwordConfirmation)}
              onChange={(event) => updateCreateField('passwordConfirmation', event.target.value)} />
            {fieldErrors.passwordConfirmation && <small className="field-error">{fieldErrors.passwordConfirmation}</small>}
          </label>
          <button type="submit" className="button" disabled={isCreating}>
            {isCreating ? 'Tworzenie…' : 'Utwórz konto mechanika'}
          </button>
        </form>

        <div className="staff-account-card staff-account-list-card">
          <h3>Istniejący mechanicy</h3>
          {isLoading && <p role="status">Ładowanie kont mechaników…</p>}
          {mechanics?.length === 0 && <p className="empty-state">Nie ma jeszcze kont mechaników.</p>}
          {mechanics && mechanics.length > 0 && (
            <ul className="staff-account-list">
              {mechanics.map((mechanic) => {
                const editForm = editForms[mechanic.id] ?? { username: mechanic.username, email: mechanic.email }
                const passwordForm = passwordForms[mechanic.id] ?? emptyPasswordForm
                const isEditing = editingId === mechanic.id
                const isResettingPassword = passwordResetId === mechanic.id
                return (
                  <li key={mechanic.id} className={!mechanic.enabled ? 'inactive' : undefined}>
                    <div className="staff-account-summary">
                      <div>
                        <strong>{mechanic.username}</strong>
                        <span className="muted">{mechanic.email}</span>
                      </div>
                      <span className={mechanic.enabled ? 'status-pill active' : 'status-pill inactive'}>
                        {mechanic.enabled ? 'Aktywne' : 'Nieaktywne'}
                      </span>
                    </div>

                    {isEditing ? (
                      <form className="staff-account-inline-form" onSubmit={(event) => void submitEdit(event, mechanic)}>
                        <label className="form-field">
                          <span>Login</span>
                          <input value={editForm.username} required minLength={3} maxLength={30}
                            aria-invalid={Boolean(fieldErrors.username)}
                            onChange={(event) => updateEditField(mechanic.id, 'username', event.target.value)} />
                          {fieldErrors.username && <small className="field-error">{fieldErrors.username}</small>}
                        </label>
                        <label className="form-field">
                          <span>E-mail</span>
                          <input type="email" value={editForm.email} required maxLength={254}
                            aria-invalid={Boolean(fieldErrors.email)}
                            onChange={(event) => updateEditField(mechanic.id, 'email', event.target.value)} />
                          {fieldErrors.email && <small className="field-error">{fieldErrors.email}</small>}
                        </label>
                        <div className="staff-account-actions">
                          <button type="submit" className="button small" disabled={savingAction === `edit-${mechanic.id}`}>Zapisz</button>
                          <button type="button" className="button secondary small" disabled={savingAction !== null}
                            onClick={() => { setEditingId(null); setEditForms((current) => ({ ...current, [mechanic.id]: { username: mechanic.username, email: mechanic.email } })) }}>
                            Anuluj
                          </button>
                        </div>
                      </form>
                    ) : (
                      <div className="staff-account-actions">
                        <button type="button" className="button secondary small" disabled={savingAction !== null}
                          onClick={() => { setEditingId(mechanic.id); setPasswordResetId(null); setFieldErrors({}); setError(null) }}>
                          Edytuj dane
                        </button>
                        <button type="button" className="button secondary small" disabled={savingAction !== null}
                          onClick={() => { setPasswordResetId(mechanic.id); setEditingId(null); setFieldErrors({}); setError(null) }}>
                          Resetuj hasło
                        </button>
                        <button type="button" className={mechanic.enabled ? 'button danger small' : 'button small'} disabled={savingAction !== null}
                          onClick={() => void toggleStatus(mechanic)}>
                          {mechanic.enabled ? 'Dezaktywuj' : 'Aktywuj'}
                        </button>
                      </div>
                    )}

                    {isResettingPassword && (
                      <form className="staff-account-inline-form" onSubmit={(event) => void submitPasswordReset(event, mechanic)}>
                        <label className="form-field">
                          <span>Nowe hasło</span>
                          <input type="password" autoComplete="new-password" value={passwordForm.password} required minLength={8} maxLength={64}
                            aria-invalid={Boolean(fieldErrors.password)}
                            onChange={(event) => updatePasswordField(mechanic.id, 'password', event.target.value)} />
                          {fieldErrors.password && <small className="field-error">{fieldErrors.password}</small>}
                        </label>
                        <label className="form-field">
                          <span>Powtórz nowe hasło</span>
                          <input type="password" autoComplete="new-password" value={passwordForm.passwordConfirmation} required maxLength={64}
                            aria-invalid={Boolean(fieldErrors.passwordConfirmation)}
                            onChange={(event) => updatePasswordField(mechanic.id, 'passwordConfirmation', event.target.value)} />
                          {fieldErrors.passwordConfirmation && <small className="field-error">{fieldErrors.passwordConfirmation}</small>}
                        </label>
                        <div className="staff-account-actions">
                          <button type="submit" className="button small" disabled={savingAction === `password-${mechanic.id}`}>Ustaw hasło</button>
                          <button type="button" className="button secondary small" disabled={savingAction !== null}
                            onClick={() => { setPasswordResetId(null); setPasswordForms((current) => ({ ...current, [mechanic.id]: emptyPasswordForm })) }}>
                            Anuluj
                          </button>
                        </div>
                      </form>
                    )}
                  </li>
                )
              })}
            </ul>
          )}
        </div>
      </div>
    </section>
  )
}

function formsFrom(mechanics: StaffAccount[]): Record<number, StaffAccountUpdateInput> {
  return Object.fromEntries(mechanics.map((mechanic) => [mechanic.id, {
    username: mechanic.username,
    email: mechanic.email,
  }]))
}
