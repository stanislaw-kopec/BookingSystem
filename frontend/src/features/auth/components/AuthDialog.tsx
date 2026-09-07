import { useEffect, useRef, useState } from 'react'
import { LoginForm } from './LoginForm'
import { RegistrationForm } from './RegistrationForm'

type AuthMode = 'login' | 'register'

export function AuthDialog({ onClose }: { onClose: () => void }) {
  const dialog = useRef<HTMLDialogElement>(null)
  const [mode, setMode] = useState<AuthMode>('login')

  useEffect(() => {
    const element = dialog.current
    element?.showModal()
    return () => element?.close()
  }, [])

  return (
    <dialog ref={dialog} className="auth-dialog" aria-labelledby="auth-heading" onCancel={onClose}>
      <div className="dialog-heading">
        <h2 id="auth-heading">{mode === 'login' ? 'Logowanie' : 'Rejestracja'}</h2>
        <button type="button" className="button secondary" onClick={onClose}
          aria-label="Zamknij okno logowania i rejestracji">Zamknij</button>
      </div>
      <div className="auth-tabs" aria-label="Wybierz formularz">
        <button type="button" className={'button secondary' + (mode === 'login' ? ' active' : '')}
          aria-pressed={mode === 'login'} onClick={() => setMode('login')}>Logowanie</button>
        <button type="button" className={'button secondary' + (mode === 'register' ? ' active' : '')}
          aria-pressed={mode === 'register'} onClick={() => setMode('register')}>Rejestracja</button>
      </div>
      {mode === 'login' ? <LoginForm onSuccess={onClose} /> : <RegistrationForm onSuccess={onClose} />}
    </dialog>
  )
}
