import type { CurrentUser } from '../types/auth'
import { apiPost, clearCsrfToken, ensureOk } from './http'

export async function login(email: string, password: string) {
  const user = await apiPost<CurrentUser>('/api/auth/login', { email, password })
  clearCsrfToken()
  return user
}

export async function getCurrentUser() {
  const response = await fetch('/api/me', { credentials: 'include' })
  if (response.status === 401) return null
  await ensureOk(response)
  return (await response.json()) as CurrentUser
}

export async function logout() {
  await apiPost<void>('/api/auth/logout', undefined, true)
  clearCsrfToken()
}

export function roleFromCurrentUser(user: CurrentUser): 'user' | 'creator' | 'admin' {
  if (user.roles.includes('ADMIN')) return 'admin'
  if (user.roles.includes('CREATOR')) return 'creator'
  return 'user'
}
