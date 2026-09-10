import type { UserRole } from '../auth/types'

export interface StaffAccount {
  id: number
  username: string
  email: string
  role: UserRole
  enabled: boolean
}

export interface StaffAccountInput {
  username: string
  email: string
  password: string
  passwordConfirmation: string
}

export interface StaffAccountUpdateInput {
  username: string
  email: string
}

export interface StaffPasswordResetInput {
  password: string
  passwordConfirmation: string
}
