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
  readonly requestId?: string

  constructor(status: number, code: string, message: string, requestId?: string) {
    super(requestId ? `${message}（请求编号：${requestId}）` : message)
    this.name = 'ApiError'
    this.status = status
    this.code = code
    this.requestId = requestId
  }
}
