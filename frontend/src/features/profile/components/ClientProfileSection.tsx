import { useEffect, useState } from 'react'
import { ApiError, errorMessage } from '../../../api/apiClient'
import * as profileApi from '../api/profileApi'
import type { ClientProfile, ClientProfileInput } from '../types'
import { ProfileDetails } from './ProfileDetails'
import { ProfileForm } from './ProfileForm'
import '../profile.css'

export function ClientProfileSection() {
  const [profile, setProfile] = useState<ClientProfile | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [isSaving, setIsSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [notice, setNotice] = useState<string | null>(null)
  const [revision, setRevision] = useState(0)
  const [isEditing, setIsEditing] = useState(false)

  useEffect(() => {
    const controller = new AbortController()
    profileApi.getProfile(controller.signal)
      .then((result) => {
        if (!controller.signal.aborted) setProfile(result)
      })
      .catch((cause: unknown) => {
        if (!controller.signal.aborted) setError(errorMessage(cause))
      })
      .finally(() => {
        if (!controller.signal.aborted) setIsLoading(false)
      })
    return () => controller.abort()
  }, [revision])

  function retry() {
    setIsLoading(true)
    setError(null)
    setRevision((value) => value + 1)
  }

  function startEditing() {
    setError(null)
    setFieldErrors({})
    setNotice(null)
    setIsEditing(true)
  }

  function cancelEditing() {
    setError(null)
    setFieldErrors({})
    setIsEditing(false)
  }

  async function save(input: ClientProfileInput) {
    setIsSaving(true)
    setError(null)
    setFieldErrors({})
    setNotice(null)
    try {
      setProfile(await profileApi.saveProfile(input))
      setIsEditing(false)
      setNotice('Profil został zapisany.')
    } catch (cause) {
      setError(errorMessage(cause))
      if (cause instanceof ApiError) setFieldErrors(cause.fieldErrors)
    } finally {
      setIsSaving(false)
    }
  }

  return (
    <section id="client-profile" className="page-section profile-section" aria-labelledby="profile-heading">
      <div className="section-heading">
        <p className="eyebrow">Konto klienta</p>
        <h2 id="profile-heading">Mój profil</h2>
        <p className="muted">Sprawdź swoje dane kontaktowe i rozliczeniowe.</p>
      </div>
      {isLoading && <p role="status">Ładowanie profilu…</p>}
      {!isLoading && error && !profile && (
        <div className="message error" role="alert">
          <p>{error}</p>
          <button type="button" className="button secondary" onClick={retry}>Spróbuj ponownie</button>
        </div>
      )}
      {profile && (
        <>
          {!profile.configured && <p className="message info">Uzupełnij profil przed dodaniem pierwszego pojazdu.</p>}
          {notice && <p className="message success" role="status">{notice}</p>}
          {error && <p className="message error" role="alert">{error}</p>}
          {profile.configured && !isEditing && (
            <ProfileDetails profile={profile} onEdit={startEditing} />
          )}
          {(!profile.configured || isEditing) && (
            <ProfileForm profile={profile} isSaving={isSaving} fieldErrors={fieldErrors}
              onSave={save} onCancel={profile.configured ? cancelEditing : undefined} />
          )}
        </>
      )}
    </section>
  )
}
