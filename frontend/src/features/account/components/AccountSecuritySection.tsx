import { useState } from 'react'
import type { FormEvent } from 'react'
import { ApiError, errorMessage } from '../../../api/apiClient'
import * as accountApi from '../api/accountApi'
import '../account.css'

export function AccountSecuritySection() {
  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [newPasswordConfirmation, setNewPasswordConfirmation] = useState('')
  const [isSaving, setIsSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setIsSaving(true)
    setError(null)
    setNotice(null)
    setFieldErrors({})
    try {
      await accountApi.changePassword({ currentPassword, newPassword, newPasswordConfirmation })
      setCurrentPassword('')
      setNewPassword('')
      setNewPasswordConfirmation('')
      setNotice('Hasło zostało zmienione.')
    } catch (cause) {
      setError(errorMessage(cause))
      if (cause instanceof ApiError) setFieldErrors(cause.fieldErrors)
    } finally {
      setIsSaving(false)
    }
  }

  return (
    <section className="page-section account-security-section" aria-labelledby="account-security-heading">
      <div className="section-heading">
        <p className="eyebrow">Ustawienia konta</p>
        <h2 id="account-security-heading">Bezpieczeństwo konta</h2>
        <p className="muted">Aby ustawić nowe hasło, potwierdź najpierw swoje obecne hasło.</p>
      </div>

      {notice && <p className="message success" role="status">{notice}</p>}
      {error && <p className="message error" role="alert">{error}</p>}

      <form className="account-security-form" onSubmit={handleSubmit}>
        <fieldset disabled={isSaving}>
          <div className="form-field">
            <label htmlFor="current-password">Obecne hasło</label>
            <input id="current-password" type="password" autoComplete="current-password"
              value={currentPassword} required maxLength={64}
              aria-invalid={Boolean(fieldErrors.currentPassword)}
              aria-describedby={fieldErrors.currentPassword ? 'current-password-error' : undefined}
              onChange={(event) => setCurrentPassword(event.target.value)} />
            {fieldErrors.currentPassword && (
              <small id="current-password-error" className="field-error">{fieldErrors.currentPassword}</small>
            )}
          </div>
          <div className="form-field">
            <label htmlFor="new-password">Nowe hasło</label>
            <input id="new-password" type="password" autoComplete="new-password"
              value={newPassword} required minLength={8} maxLength={64}
              aria-invalid={Boolean(fieldErrors.newPassword)}
              aria-describedby={fieldErrors.newPassword ? 'new-password-help new-password-error' : 'new-password-help'}
              onChange={(event) => setNewPassword(event.target.value)} />
            <small id="new-password-help" className="muted">Od 8 do 64 znaków.</small>
            {fieldErrors.newPassword && (
              <small id="new-password-error" className="field-error">{fieldErrors.newPassword}</small>
            )}
          </div>
          <div className="form-field">
            <label htmlFor="new-password-confirmation">Powtórz nowe hasło</label>
            <input id="new-password-confirmation" type="password" autoComplete="new-password"
              value={newPasswordConfirmation} required maxLength={64}
              aria-invalid={Boolean(fieldErrors.newPasswordConfirmation)}
              aria-describedby={fieldErrors.newPasswordConfirmation ? 'new-password-confirmation-error' : undefined}
              onChange={(event) => setNewPasswordConfirmation(event.target.value)} />
            {fieldErrors.newPasswordConfirmation && (
              <small id="new-password-confirmation-error" className="field-error">
                {fieldErrors.newPasswordConfirmation}
              </small>
            )}
          </div>
          <div className="actions account-security-actions">
            <button type="submit" className="button">
              {isSaving ? 'Zmienianie hasła…' : 'Zmień hasło'}
            </button>
          </div>
        </fieldset>
      </form>
    </section>
  )
}
