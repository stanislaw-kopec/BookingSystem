export type UserRole = 'CLIENT' | 'MECHANIC' | 'ADMIN'

export interface CurrentUser {
  username: string
  roles: UserRole[]
}
