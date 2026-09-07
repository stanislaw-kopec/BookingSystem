import { useState } from 'react'
import type { FormEvent } from 'react'
import { ApiError, errorMessage } from '../../../api/apiClient'
import { useAuth } from '../hooks/useAuth'

export function RegistrationForm({ onSuccess }: { onSuccess: () => void }) {
  const auth = useAuth()
  const [username, setUsername] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [passwordConfirmation, setPasswordConfirmation] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError(null)
    setFieldErrors({})
    if (password !== passwordConfirmation) {
      setFieldErrors({ passwordConfirmation: 'Hasła nie są takie same.' })
      return
    }

    setIsSubmitting(true)
    try {
      await auth.register({
        username: username.trim(), email: email.trim(), password, passwordConfirmation,
      })
      onSuccess()
    } catch (cause) {
      setError(errorMessage(cause))
      if (cause instanceof ApiError) setFieldErrors(cause.fieldErrors)
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <form onSubmit={handleSubmit}>
      <fieldset disabled={isSubmitting}>
        <label htmlFor="register-username">Login</label>
        <input id="register-username" autoComplete="username" required minLength={3} maxLength={30}
          pattern="[A-Za-z0-9._-]+" aria-invalid={Boolean(fieldErrors.username)}
          aria-describedby={fieldErrors.username ? 'register-username-error' : 'register-username-help'}
          value={username} onChange={(event) => setUsername(event.target.value)} />
        <small id="register-username-help" className="muted">3–30 znaków: litery bez polskich znaków, cyfry, kropka, myślnik lub podkreślenie.</small>
        {fieldErrors.username && <small id="register-username-error" className="field-error">{fieldErrors.username}</small>}

        <label htmlFor="register-email">Adres e-mail</label>
        <input id="register-email" type="email" autoComplete="email" required maxLength={254}
          aria-invalid={Boolean(fieldErrors.email)}
          aria-describedby={fieldErrors.email ? 'register-email-error' : undefined}
          value={email} onChange={(event) => setEmail(event.target.value)} />
        {fieldErrors.email && <small id="register-email-error" className="field-error">{fieldErrors.email}</small>}

        <label htmlFor="register-password">Hasło</label>
        <input id="register-password" type="password" autoComplete="new-password" required minLength={8} maxLength={64}
          aria-invalid={Boolean(fieldErrors.password)}
          aria-describedby={fieldErrors.password ? 'register-password-error' : 'register-password-help'}
          value={password} onChange={(event) => setPassword(event.target.value)} />
        <small id="register-password-help" className="muted">Od 8 do 64 znaków.</small>
        {fieldErrors.password && <small id="register-password-error" className="field-error">{fieldErrors.password}</small>}

        <label htmlFor="register-password-confirmation">Powtórz hasło</label>
        <input id="register-password-confirmation" type="password" autoComplete="new-password" required maxLength={64}
          aria-invalid={Boolean(fieldErrors.passwordConfirmation)}
          aria-describedby={fieldErrors.passwordConfirmation ? 'register-password-confirmation-error' : undefined}
          value={passwordConfirmation} onChange={(event) => setPasswordConfirmation(event.target.value)} />
        {fieldErrors.passwordConfirmation && <small id="register-password-confirmation-error" className="field-error">{fieldErrors.passwordConfirmation}</small>}

        {error && <p className="message error" role="alert">{error}</p>}
        <button className="button" type="submit">{isSubmitting ? 'Tworzenie konta…' : 'Utwórz konto'}</button>
      </fieldset>
    </form>
  )
}
