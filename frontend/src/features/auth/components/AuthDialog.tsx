import { useEffect, useRef, useState } from 'react'
import type { FormEvent } from 'react'
import { errorMessage } from '../../../api/apiClient'
import { useAuth } from '../hooks/useAuth'

export function AuthDialog({ onClose }: { onClose: () => void }) {
  const dialog = useRef<HTMLDialogElement>(null)
  const auth = useAuth()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    const element = dialog.current
    element?.showModal()
    return () => element?.close()
  }, [])

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setIsSubmitting(true)
    setError(null)
    try {
      await auth.login(username.trim(), password)
      onClose()
    } catch (cause) {
      setError(errorMessage(cause))
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <dialog ref={dialog} className="auth-dialog" aria-labelledby="login-heading" onCancel={onClose}>
      <div className="dialog-heading">
        <h2 id="login-heading">Logowanie</h2>
        <button type="button" className="button secondary" onClick={onClose} aria-label="Zamknij logowanie">Zamknij</button>
      </div>
      <form onSubmit={handleSubmit}>
        <fieldset disabled={isSubmitting}>
          <label htmlFor="login-username">Login</label>
          <input id="login-username" autoComplete="username" required maxLength={80}
            value={username} onChange={(event) => setUsername(event.target.value)} />
          <label htmlFor="login-password">Hasło</label>
          <input id="login-password" type="password" autoComplete="current-password" required
            value={password} onChange={(event) => setPassword(event.target.value)} />
          {error && <p className="message error" role="alert">{error}</p>}
          <button className="button" type="submit">{isSubmitting ? 'Logowanie…' : 'Zaloguj się'}</button>
        </fieldset>
      </form>
      <p className="muted registration-note">Rejestracja klientów będzie dostępna w kolejnym etapie.</p>
    </dialog>
  )
}
