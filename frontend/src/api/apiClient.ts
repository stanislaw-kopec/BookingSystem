export class ApiError extends Error {
  readonly status: number
  readonly fieldErrors: Record<string, string>

  constructor(status: number, message: string, fieldErrors: Record<string, string> = {}) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.fieldErrors = fieldErrors
  }
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
    const message = isRecord(payload) && typeof payload.message === 'string'
      ? payload.message
      : 'Nie udało się wykonać operacji. Spróbuj ponownie.'
    throw new ApiError(response.status, message, fields)
  }
  return payload
}

export async function apiRequest(path: string, options: RequestInit = {}): Promise<unknown> {
  const headers = new Headers(options.headers)
  const method = (options.method ?? 'GET').toUpperCase()

  if (!['GET', 'HEAD', 'OPTIONS'].includes(method)) {
    // A fresh token also works after login, logout or session expiration.
    const csrf = await readResponse(await fetch('/api/auth/csrf', {
      credentials: 'same-origin',
      cache: 'no-store',
    }))
    if (!isRecord(csrf) || typeof csrf.headerName !== 'string' || typeof csrf.token !== 'string') {
      throw new ApiError(502, 'Nie udało się przygotować formularza. Odśwież stronę.')
    }
    headers.set(csrf.headerName, csrf.token)
  }

  return readResponse(await fetch(path, {
    ...options,
    headers,
    credentials: 'same-origin',
    cache: 'no-store',
  }))
}

export function errorMessage(error: unknown): string {
  return error instanceof ApiError
    ? error.message
    : 'Nie udało się połączyć z serwerem. Spróbuj ponownie.'
}
