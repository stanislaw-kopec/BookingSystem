export type UserRole = 'CLIENT' | 'MECHANIC' | 'ADMIN'

export interface CurrentUser {
  username: string
  roles: UserRole[]
}

export interface RegistrationInput {
  username: string
  email: string
  password: string
  passwordConfirmation: string
}
