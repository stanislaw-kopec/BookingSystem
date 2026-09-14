export type ManagedAccountRole = 'CLIENT' | 'MECHANIC' | 'ADMIN'
export type CreatableStaffRole = 'MECHANIC' | 'ADMIN'

export interface AdminAccount {
  id: number
  username: string
  email: string
  role: ManagedAccountRole
  enabled: boolean
}

export interface AdminAccountPage {
  content: AdminAccount[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface ManagedAccountInput {
  username: string
  email: string
  password: string
  passwordConfirmation: string
}

export interface AdminAccountUpdateInput {
  username: string
  email: string
}

export interface AdminPasswordResetInput {
  password: string
  passwordConfirmation: string
}

export interface AdminAccountFilters {
  role: ManagedAccountRole | null
  enabled: boolean | null
  query: string
  page: number
  size: number
}
