import { ApiError } from '../types/auth'

type CsrfTokenResponse = {
  headerName: string
  parameterName?: string
  token: string
}

let csrfToken: CsrfTokenResponse | null = null

async function readError(response: Response) {
  const responseRequestId = response.headers.get('X-Request-Id') ?? undefined
  try {
    const body = (await response.json()) as { code?: string; message?: string; requestId?: string }
    return {
      code: body.code ?? 'REQUEST_FAILED',
      message: body.message ?? `请求失败（${response.status}）`,
      requestId: body.requestId ?? responseRequestId,
    }
  } catch {
    return {
      code: 'REQUEST_FAILED',
      message: `请求失败（${response.status}）`,
      requestId: responseRequestId,
    }
  }
}

export async function ensureOk(response: Response) {
  if (response.ok) return
  const error = await readError(response)
  throw new ApiError(response.status, error.code, error.message, error.requestId)
}

export function clearCsrfToken() {
  csrfToken = null
}

export async function getCsrfToken(forceRefresh = false) {
  if (csrfToken && !forceRefresh) return csrfToken
  const response = await fetch('/api/auth/csrf', { credentials: 'include' })
  await ensureOk(response)
  csrfToken = (await response.json()) as CsrfTokenResponse
  return csrfToken
}

export async function apiGet<T>(path: string, signal?: AbortSignal) {
  const response = await fetch(path, { credentials: 'include', signal })
  await ensureOk(response)
  return (await response.json()) as T
}

export async function apiPost<T>(path: string, body?: unknown, forceFreshCsrf = false) {
  let token = await getCsrfToken(forceFreshCsrf)
  const request = () =>
    fetch(path, {
      method: 'POST',
      credentials: 'include',
      headers: {
        ...(body === undefined ? {} : { 'Content-Type': 'application/json' }),
        [token.headerName]: token.token,
      },
      body: body === undefined ? undefined : JSON.stringify(body),
    })

  let response = await request()
  if (response.status === 403) {
    token = await getCsrfToken(true)
    response = await request()
  }
  await ensureOk(response)
  return response.status === 204 ? (undefined as T) : ((await response.json()) as T)
}
