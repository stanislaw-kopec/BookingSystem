// A revision prevents responses started in an earlier session from updating the UI.
let revision = 0
const listeners = new Set<() => void>()

export const sessionStorageKey = 'workshop-session-change'
export const sessionRevision = () => revision
export function advanceSession() { revision += 1 }

export function assertCurrentSession(startedAt: number) {
  if (startedAt !== revision) throw new DOMException('Session changed', 'AbortError')
}

export function subscribeToSessionExpiry(listener: () => void) {
  listeners.add(listener)
  return () => { listeners.delete(listener) }
}

export function expireSession(startedAt: number) {
  if (startedAt !== revision) return
  advanceSession()
  listeners.forEach((listener) => listener())
}

export function broadcastSessionChange() {
  // Only a change notification is shared, never identity, passwords or tokens.
  try { localStorage.setItem(sessionStorageKey, crypto.randomUUID()) } catch { /* Storage may be disabled. */ }
}
