import { useState } from 'react'
import type { FormEvent } from 'react'
import { errorMessage } from '../../../api/apiClient'
import { useAuth } from '../hooks/useAuth'

export function LoginForm({ onSuccess }: { onSuccess: () => void }) {
  const auth = useAuth()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setIsSubmitting(true)
    setError(null)
    try {
      await auth.login(username.trim(), password)
      onSuccess()
    } catch (cause) {
      setError(errorMessage(cause))
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
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
  )
}
