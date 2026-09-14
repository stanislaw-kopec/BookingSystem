import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { ApiError, errorMessage } from '../../../api/apiClient'
import { CustomSelect } from '../../../components/ui/CustomSelect'
import * as adminAccountsApi from '../api/adminAccountsApi'
import type {
  AdminAccount,
  AdminAccountPage,
  AdminAccountUpdateInput,
  AdminPasswordResetInput,
  CreatableStaffRole,
  ManagedAccountInput,
  ManagedAccountRole,
} from '../types'
import '../adminAccounts.css'

const pageSize = 8
const emptyCreateForm: ManagedAccountInput = { username: '', email: '', password: '', passwordConfirmation: '' }
const emptyPasswordForm: AdminPasswordResetInput = { password: '', passwordConfirmation: '' }

export function AdminAccountsSection() {
  const [accountPage, setAccountPage] = useState<AdminAccountPage | null>(null)
  const [queryInput, setQueryInput] = useState('')
  const [query, setQuery] = useState('')
  const [role, setRole] = useState<ManagedAccountRole | null>(null)
  const [enabled, setEnabled] = useState<boolean | null>(null)
  const [page, setPage] = useState(0)
  const [revision, setRevision] = useState(0)
  const [createForm, setCreateForm] = useState<ManagedAccountInput>(emptyCreateForm)
  const [createRole, setCreateRole] = useState<CreatableStaffRole>('MECHANIC')
  const [editForms, setEditForms] = useState<Record<number, AdminAccountUpdateInput>>({})
  const [passwordForms, setPasswordForms] = useState<Record<number, AdminPasswordResetInput>>({})
  const [editingId, setEditingId] = useState<number | null>(null)
  const [passwordResetId, setPasswordResetId] = useState<number | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [savingAction, setSavingAction] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [fieldErrorScope, setFieldErrorScope] = useState<string | null>(null)

  useEffect(() => {
    const controller = new AbortController()
    adminAccountsApi.getAccounts({ role, enabled, query, page, size: pageSize }, controller.signal)
      .then((result) => {
        if (controller.signal.aborted) return
        if (result.totalPages > 0 && page >= result.totalPages) {
          setPage(result.totalPages - 1)
          return
        }
        setAccountPage(result)
        setEditForms(formsFrom(result.content))
        setError(null)
      })
      .catch((cause: unknown) => {
        if (!controller.signal.aborted) setError(errorMessage(cause))
      })
      .finally(() => {
        if (!controller.signal.aborted) setIsLoading(false)
      })
    return () => controller.abort()
  }, [enabled, page, query, revision, role])

  function refreshAccounts() {
    setEditingId(null)
    setPasswordResetId(null)
    setIsLoading(true)
    setRevision((value) => value + 1)
  }

  function applySearch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setIsLoading(true)
    setPage(0)
    setQuery(queryInput.trim())
    setRevision((value) => value + 1)
  }

  function changeRoleFilter(value: string) {
    const nextRole = value === 'CLIENT' || value === 'MECHANIC' || value === 'ADMIN' ? value : null
    if (nextRole === role) return
    setIsLoading(true)
    setPage(0)
    setRole(nextRole)
  }

  function changeStatusFilter(value: string) {
    const nextEnabled = value === 'true' ? true : value === 'false' ? false : null
    if (nextEnabled === enabled) return
    setIsLoading(true)
    setPage(0)
    setEnabled(nextEnabled)
  }

  async function submitCreate(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setSavingAction('create')
    prepareMutation('create')
    try {
      const created = await adminAccountsApi.createAccount(createRole, createForm)
      setCreateForm(emptyCreateForm)
      setNotice(`Konto ${roleGenitiveLabel(created.role)} ${created.username} zostało utworzone.`)
      refreshAccounts()
    } catch (cause) {
      handleFailure(cause)
    } finally {
      setSavingAction(null)
    }
  }

  async function submitEdit(event: FormEvent<HTMLFormElement>, account: AdminAccount) {
    event.preventDefault()
    const form = editForms[account.id]
    if (!form) return
    setSavingAction(`edit-${account.id}`)
    prepareMutation(`account-${account.id}`)
    try {
      const updated = await adminAccountsApi.updateAccount(account.id, form)
      setNotice(`Dane konta ${updated.username} zostały zapisane.`)
      refreshAccounts()
    } catch (cause) {
      handleFailure(cause)
    } finally {
      setSavingAction(null)
    }
  }

  async function submitPasswordReset(event: FormEvent<HTMLFormElement>, account: AdminAccount) {
    event.preventDefault()
    const form = passwordForms[account.id] ?? emptyPasswordForm
    setSavingAction(`password-${account.id}`)
    prepareMutation(`account-${account.id}`)
    try {
      const updated = await adminAccountsApi.resetPassword(account.id, form)
      setPasswordForms((current) => ({ ...current, [account.id]: emptyPasswordForm }))
      setNotice(`Nowe hasło konta ${updated.username} zostało ustawione.`)
      refreshAccounts()
    } catch (cause) {
      handleFailure(cause)
    } finally {
      setSavingAction(null)
    }
  }

  async function toggleStatus(account: AdminAccount) {
    const nextEnabled = !account.enabled
    setSavingAction(`status-${account.id}`)
    prepareMutation(`account-${account.id}`)
    try {
      const updated = await adminAccountsApi.updateStatus(account.id, nextEnabled)
      setNotice(nextEnabled
        ? `Konto ${updated.username} zostało aktywowane.`
        : `Konto ${updated.username} zostało dezaktywowane.`)
      refreshAccounts()
    } catch (cause) {
      handleFailure(cause)
    } finally {
      setSavingAction(null)
    }
  }

  function prepareMutation(scope: string) {
    setError(null)
    setNotice(null)
    setFieldErrors({})
    setFieldErrorScope(scope)
  }

  function handleFailure(cause: unknown) {
    setError(errorMessage(cause))
    if (cause instanceof ApiError) setFieldErrors(localizedFieldErrors(cause.fieldErrors))
  }

  function clearFieldError(field: string) {
    setFieldErrors((current) => ({ ...current, [field]: '' }))
    setError(null)
  }

  const accounts = accountPage?.content ?? []
  const totalPages = accountPage?.totalPages ?? 0

  return (
    <section className="page-section admin-accounts-section" aria-labelledby="admin-accounts-heading">
      <div className="section-heading">
        <p className="eyebrow">Panel administratora</p>
        <h2 id="admin-accounts-heading">Konta użytkowników</h2>
        <p className="muted">Zarządzaj dostępem klientów i mechaników oraz twórz konta administratorów.</p>
      </div>

      {error && <p className="message error" role="alert">{error}</p>}
      {notice && <p className="message success" role="status">{notice}</p>}

      <div className="admin-accounts-layout">
        <form className="admin-account-card" onSubmit={(event) => void submitCreate(event)}>
          <h3>Nowe konto personelu</h3>
          <p className="muted admin-account-help">Konta klientów powstają przez publiczną rejestrację.</p>
          <label className="form-field">
            <span>Rola nowego konta</span>
            <CustomSelect id="admin-account-create-role" value={createRole}
              options={[
                { value: 'MECHANIC', label: 'Mechanik' },
                { value: 'ADMIN', label: 'Administrator' },
              ]}
              onChange={(value) => setCreateRole(value === 'ADMIN' ? 'ADMIN' : 'MECHANIC')} />
          </label>
          <AccountField label="Login" type="text" value={createForm.username} field="username"
            error={fieldErrorScope === 'create' ? fieldErrors.username : undefined} autoComplete="username" minLength={3} maxLength={30}
            onChange={(value) => { setCreateForm((current) => ({ ...current, username: value })); clearFieldError('username') }} />
          <AccountField label="E-mail" type="email" value={createForm.email} field="email"
            error={fieldErrorScope === 'create' ? fieldErrors.email : undefined} autoComplete="email" maxLength={254}
            onChange={(value) => { setCreateForm((current) => ({ ...current, email: value })); clearFieldError('email') }} />
          <AccountField label="Hasło początkowe" type="password" value={createForm.password} field="password"
            error={fieldErrorScope === 'create' ? fieldErrors.password : undefined} autoComplete="new-password" minLength={8} maxLength={64}
            onChange={(value) => { setCreateForm((current) => ({ ...current, password: value })); clearFieldError('password') }} />
          <AccountField label="Powtórz hasło" type="password" value={createForm.passwordConfirmation}
            field="passwordConfirmation" error={fieldErrorScope === 'create' ? fieldErrors.passwordConfirmation : undefined} autoComplete="new-password" maxLength={64}
            onChange={(value) => { setCreateForm((current) => ({ ...current, passwordConfirmation: value })); clearFieldError('passwordConfirmation') }} />
          <button type="submit" className="button" disabled={savingAction === 'create'}>
            {savingAction === 'create' ? 'Tworzenie…' : `Utwórz konto ${createRole === 'ADMIN' ? 'administratora' : 'mechanika'}`}
          </button>
        </form>

        <div className="admin-account-card">
          <h3>Wszystkie konta</h3>
          <form className="admin-account-filters" onSubmit={applySearch}>
            <label className="form-field">
              <span>Login lub e-mail</span>
              <span className="admin-account-search">
                <input type="search" value={queryInput} maxLength={254}
                  onChange={(event) => setQueryInput(event.target.value)} />
                <button type="submit" className="button secondary small">Szukaj</button>
              </span>
            </label>
            <label className="form-field">
              <span>Rola</span>
              <CustomSelect id="admin-account-role-filter" value={role ?? ''}
                options={[
                  { value: '', label: 'Wszystkie' },
                  { value: 'CLIENT', label: 'Klienci' },
                  { value: 'MECHANIC', label: 'Mechanicy' },
                  { value: 'ADMIN', label: 'Administratorzy' },
                ]}
                onChange={changeRoleFilter} />
            </label>
            <label className="form-field">
              <span>Status</span>
              <CustomSelect id="admin-account-status-filter" value={enabled === null ? '' : String(enabled)}
                options={[
                  { value: '', label: 'Wszystkie' },
                  { value: 'true', label: 'Aktywne' },
                  { value: 'false', label: 'Nieaktywne' },
                ]}
                onChange={changeStatusFilter} />
            </label>
          </form>

          {isLoading && <p role="status">Ładowanie kont…</p>}
          {!isLoading && accounts.length === 0 && <p className="empty-state">Nie znaleziono kont spełniających wybrane kryteria.</p>}
          {!isLoading && accounts.length > 0 && (
            <ul className="admin-account-list">
              {accounts.map((account) => {
                const editForm = editForms[account.id] ?? { username: account.username, email: account.email }
                const passwordForm = passwordForms[account.id] ?? emptyPasswordForm
                const isEditing = editingId === account.id
                const isResettingPassword = passwordResetId === account.id
                return (
                  <li key={account.id} className={!account.enabled ? 'inactive' : undefined}>
                    <div className="admin-account-summary">
                      <div className="admin-account-summary-main">
                        <strong>{account.username}</strong>
                        <span className="muted">{account.email}</span>
                        <span className="admin-account-meta">
                          <span className="admin-account-role">{roleLabel(account.role)}</span>
                          <span className={account.enabled ? 'status-pill active' : 'status-pill inactive'}>
                            {account.enabled ? 'Aktywne' : 'Nieaktywne'}
                          </span>
                        </span>
                      </div>
                    </div>

                    {isEditing && (
                      <form className="admin-account-inline-form" onSubmit={(event) => void submitEdit(event, account)}>
                        <AccountField label="Login" type="text" value={editForm.username} field="username"
                          error={fieldErrorScope === `account-${account.id}` ? fieldErrors.username : undefined} minLength={3} maxLength={30}
                          onChange={(value) => { setEditForms((current) => ({ ...current, [account.id]: { ...editForm, username: value } })); clearFieldError('username') }} />
                        <AccountField label="E-mail" type="email" value={editForm.email} field="email"
                          error={fieldErrorScope === `account-${account.id}` ? fieldErrors.email : undefined} maxLength={254}
                          onChange={(value) => { setEditForms((current) => ({ ...current, [account.id]: { ...editForm, email: value } })); clearFieldError('email') }} />
                        <div className="admin-account-actions">
                          <button type="submit" className="button small" disabled={savingAction === `edit-${account.id}`}>Zapisz</button>
                          <button type="button" className="button secondary small" disabled={savingAction !== null}
                            onClick={() => setEditingId(null)}>Anuluj</button>
                        </div>
                      </form>
                    )}

                    {isResettingPassword && (
                      <form className="admin-account-inline-form" onSubmit={(event) => void submitPasswordReset(event, account)}>
                        <p className="muted admin-account-help">Ustaw hasło uzgodnione z użytkownikiem podczas kontaktu w warsztacie.</p>
                        <AccountField label="Nowe hasło" type="password" value={passwordForm.password} field="password"
                          error={fieldErrorScope === `account-${account.id}` ? fieldErrors.password : undefined} autoComplete="new-password" minLength={8} maxLength={64}
                          onChange={(value) => { setPasswordForms((current) => ({ ...current, [account.id]: { ...passwordForm, password: value } })); clearFieldError('password') }} />
                        <AccountField label="Powtórz nowe hasło" type="password" value={passwordForm.passwordConfirmation}
                          field="passwordConfirmation" error={fieldErrorScope === `account-${account.id}` ? fieldErrors.passwordConfirmation : undefined} autoComplete="new-password" maxLength={64}
                          onChange={(value) => { setPasswordForms((current) => ({ ...current, [account.id]: { ...passwordForm, passwordConfirmation: value } })); clearFieldError('passwordConfirmation') }} />
                        <div className="admin-account-actions">
                          <button type="submit" className="button small" disabled={savingAction === `password-${account.id}`}>Ustaw nowe hasło</button>
                          <button type="button" className="button secondary small" disabled={savingAction !== null}
                            onClick={() => setPasswordResetId(null)}>Anuluj</button>
                        </div>
                      </form>
                    )}

                    {!isEditing && !isResettingPassword && (
                      account.role === 'ADMIN'
                        ? <p className="muted admin-account-protected">Konto administratora jest chronione przed zmianami w tym panelu.</p>
                        : (
                          <div className="admin-account-actions">
                            <button type="button" className="button secondary small" disabled={savingAction !== null}
                              onClick={() => { setEditingId(account.id); setPasswordResetId(null); setFieldErrors({}); setFieldErrorScope(null); setError(null) }}>
                              Edytuj dane
                            </button>
                            <button type="button" className="button secondary small" disabled={savingAction !== null}
                              onClick={() => { setPasswordResetId(account.id); setEditingId(null); setFieldErrors({}); setFieldErrorScope(null); setError(null) }}>
                              Ustaw nowe hasło
                            </button>
                            <button type="button" className={account.enabled ? 'button danger small' : 'button small'}
                              disabled={savingAction !== null} onClick={() => void toggleStatus(account)}>
                              {account.enabled ? 'Dezaktywuj' : 'Aktywuj'}
                            </button>
                          </div>
                        )
                    )}
                  </li>
                )
              })}
            </ul>
          )}

          {!isLoading && totalPages > 1 && (
            <div className="admin-account-pagination">
              <button type="button" className="button secondary small" disabled={page === 0}
                onClick={() => { setIsLoading(true); setPage((value) => Math.max(0, value - 1)) }}>Poprzednia</button>
              <span>Strona {page + 1} z {totalPages} · {accountPage?.totalElements ?? 0} kont</span>
              <button type="button" className="button secondary small" disabled={page >= totalPages - 1}
                onClick={() => { setIsLoading(true); setPage((value) => Math.min(totalPages - 1, value + 1)) }}>Następna</button>
            </div>
          )}
        </div>
      </div>
    </section>
  )
}

interface AccountFieldProps {
  label: string
  type: 'text' | 'email' | 'password'
  value: string
  field: string
  error?: string
  autoComplete?: string
  minLength?: number
  maxLength: number
  onChange: (value: string) => void
}

function AccountField({ label, type, value, field, error, autoComplete, minLength, maxLength, onChange }: AccountFieldProps) {
  return (
    <label className="form-field">
      <span>{label}</span>
      <input type={type} value={value} required autoComplete={autoComplete} minLength={minLength} maxLength={maxLength}
        aria-invalid={Boolean(error)} onChange={(event) => onChange(event.target.value)} />
      {error && <small className="field-error" data-field={field}>{error}</small>}
    </label>
  )
}

function formsFrom(accounts: AdminAccount[]): Record<number, AdminAccountUpdateInput> {
  return Object.fromEntries(accounts.map((account) => [account.id, {
    username: account.username,
    email: account.email,
  }]))
}

function roleLabel(role: ManagedAccountRole) {
  if (role === 'CLIENT') return 'Klient'
  if (role === 'MECHANIC') return 'Mechanik'
  return 'Administrator'
}

function roleGenitiveLabel(role: ManagedAccountRole) {
  if (role === 'CLIENT') return 'klienta'
  if (role === 'MECHANIC') return 'mechanika'
  return 'administratora'
}

function localizedFieldErrors(fields: Record<string, string>): Record<string, string> {
  const messages: Record<string, string> = {
    username: 'Login musi mieć od 3 do 30 znaków i może zawierać litery, cyfry, kropkę, myślnik oraz podkreślenie.',
    email: 'Podaj poprawny i nieużywany adres e-mail.',
    password: 'Hasło musi mieć od 8 do 64 znaków i mieścić się w limicie BCrypt.',
    passwordConfirmation: 'Hasła nie są takie same.',
  }
  return Object.fromEntries(Object.keys(fields).map((field) => [field, messages[field] ?? fields[field]]))
}
