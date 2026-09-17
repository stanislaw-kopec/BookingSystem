import { assertCurrentSession, expireSession, sessionRevision } from './sessionEvents'
import { validationMessage } from './validationMessages'

export class ApiError extends Error {
  readonly status: number
  readonly code: string | null
  readonly fieldErrors: Record<string, string>

  constructor(
    status: number,
    message: string,
    fieldErrors: Record<string, string> = {},
    code: string | null = null,
  ) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.code = code
    this.fieldErrors = Object.fromEntries(Object.entries(fieldErrors).map(([field, text]) => [field, validationMessage(text)]))
  }
}

const defaultErrorMessages: Record<string, string> = {
  ACCOUNT_PASSWORD_VALIDATION_FAILED: 'Sprawdź dane formularza zmiany hasła.',
  ACCOUNT_MANAGEMENT_VALIDATION_FAILED: 'Sprawdź parametry listy kont.',
  ACCESS_DENIED: 'Nie masz uprawnień do wykonania tej operacji.',
  APPOINTMENT_CONFLICT: 'Nie można wykonać tej operacji dla aktualnego stanu wizyty.',
  APPOINTMENT_DAY_FULL: 'Ten dzień nie ma już wolnych miejsc. Wybierz inny dzień.',
  SCHEDULE_CAPACITY_CONFLICT: 'Zmiana pozostawiłaby mniej miejsc niż aktywnych wizyt. Najpierw przełóż lub odwołaj odpowiednie zgłoszenia.',
  APPOINTMENT_VALIDATION_FAILED: 'Sprawdź dane zgłoszenia wizyty.',
  CSRF_TOKEN_UNAVAILABLE: 'Nie udało się przygotować formularza. Odśwież stronę.',
  DATA_INTEGRITY_CONFLICT: 'Te dane są już używane albo naruszają ograniczenia systemu.',
  INVOICE_GENERATION_FAILED: 'Nie udało się wygenerować faktury. Spróbuj ponownie.',
  MALFORMED_REQUEST: 'Serwer nie mógł odczytać wysłanych danych.',
  PROFILE_VALIDATION_FAILED: 'Sprawdź dane profilu.',
  REGISTRATION_CONFLICT: 'Konto z takimi danymi już istnieje.',
  REGISTRATION_VALIDATION_FAILED: 'Sprawdź dane rejestracji.',
  RESOURCE_CONFLICT: 'Nie można zapisać zmian, ponieważ zasób jest w konflikcie.',
  RESOURCE_NOT_FOUND: 'Nie znaleziono zasobu albo nie masz do niego dostępu.',
  UNAUTHENTICATED: 'Zaloguj się, aby wykonać tę operację.',
  VALIDATION_FAILED: 'Sprawdź poprawność formularza.',
  VEHICLE_CONFLICT: 'Masz już pojazd z takimi danymi.',
  VEHICLE_VALIDATION_FAILED: 'Sprawdź dane pojazdu.',
}

export function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}

async function readResponse(response: Response): Promise<unknown> {
  if (response.status === 204) return null
  const payload: unknown = await response.json().catch(() => null)

  if (!response.ok) {
    const fields: Record<string, string> = {}
    if (isRecord(payload) && isRecord(payload.fieldErrors)) {
      for (const [key, value] of Object.entries(payload.fieldErrors)) {
        if (typeof value === 'string') fields[key] = value
      }
    }
    const code = isRecord(payload) && typeof payload.code === 'string'
      ? payload.code
      : null
    const message = isRecord(payload) && typeof payload.message === 'string'
      ? payload.message
      : 'Nie udało się wykonać operacji. Spróbuj ponownie.'
    throw new ApiError(response.status, message, fields, code)
  }
  return payload
}

async function request(path: string, options: RequestInit = {}): Promise<Response> {
  const startedAt = sessionRevision()
  const headers = new Headers(options.headers)
  const method = (options.method ?? 'GET').toUpperCase()

  if (!['GET', 'HEAD', 'OPTIONS'].includes(method)) {
    // A fresh token also works after login, logout or session expiration.
    const csrf = await readResponse(await checkedFetch('/api/auth/csrf', {
      credentials: 'same-origin',
      cache: 'no-store',
      signal: options.signal,
    }, startedAt))
    if (!isRecord(csrf) || typeof csrf.headerName !== 'string' || typeof csrf.token !== 'string') {
      throw new ApiError(502, 'Nie udało się przygotować formularza. Odśwież stronę.', {}, 'CSRF_TOKEN_UNAVAILABLE')
    }
    headers.set(csrf.headerName, csrf.token)
  }

  assertCurrentSession(startedAt)
  return checkedFetch(path, {
    ...options,
    headers,
    credentials: 'same-origin',
    cache: 'no-store',
  }, startedAt)
}

async function checkedFetch(path: string, options: RequestInit, startedAt: number): Promise<Response> {
  const response = await fetch(path, options)
  assertCurrentSession(startedAt)
  if (response.status === 401 && path !== '/api/auth/login') expireSession(startedAt)
  return response
}

export async function apiRequest(path: string, options: RequestInit = {}): Promise<unknown> {
  const startedAt = sessionRevision()
  const payload = await readResponse(await request(path, options))
  assertCurrentSession(startedAt)
  return payload
}

export async function apiDownload(path: string): Promise<Blob> {
  const startedAt = sessionRevision()
  const response = await request(path)
  if (!response.ok) await readResponse(response)
  const blob = await response.blob()
  assertCurrentSession(startedAt)
  return blob
}

export function errorMessage(error: unknown): string {
  if (error instanceof ApiError && error.code !== null && error.code in defaultErrorMessages) {
    return defaultErrorMessages[error.code]
  }
  return error instanceof ApiError
    ? error.message
    : 'Nie udało się połączyć z serwerem. Spróbuj ponownie.'
}
