export type AuthRole = 'USER' | 'CREATOR' | 'ADMIN'

export type CurrentUser = {
  id: number
  email: string
  displayName: string
  roles: AuthRole[]
}

export class ApiError extends Error {
  readonly status: number
  readonly code: string

  constructor(status: number, code: string, message: string) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.code = code
  }
}
