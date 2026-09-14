import { apiRequest } from '../../../api/apiClient'
import type { ChangePasswordInput } from '../types'

export async function changePassword(input: ChangePasswordInput): Promise<void> {
  await apiRequest('/api/auth/password', {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(input),
  })
}
