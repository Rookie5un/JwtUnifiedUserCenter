import type { ApiEnvelope, AuthPayload } from '@/types'

export const API_BASE = import.meta.env.VITE_API_BASE ?? 'http://localhost:8080'
const API_ROOT = API_BASE.endsWith('/') ? API_BASE : `${API_BASE}/`

const ACCESS_TOKEN_KEY = 'atlas_access_token'
const REFRESH_TOKEN_KEY = 'atlas_refresh_token'
export const SESSION_EXPIRED_EVENT = 'atlas-session-expired'

export class ApiRequestError extends Error {
  status: number
  code?: string

  constructor(message: string, status: number, code?: string) {
    super(message)
    this.name = 'ApiRequestError'
    this.status = status
    this.code = code
  }
}

export function getAccessToken() {
  return localStorage.getItem(ACCESS_TOKEN_KEY)
}

export function getRefreshToken() {
  return localStorage.getItem(REFRESH_TOKEN_KEY)
}

export function setTokens(payload: Pick<AuthPayload, 'accessToken' | 'refreshToken'>) {
  localStorage.setItem(ACCESS_TOKEN_KEY, payload.accessToken)
  localStorage.setItem(REFRESH_TOKEN_KEY, payload.refreshToken)
}

export function clearTokens() {
  localStorage.removeItem(ACCESS_TOKEN_KEY)
  localStorage.removeItem(REFRESH_TOKEN_KEY)
}

export function notifySessionExpired(message = '登录状态已失效，请重新登录。') {
  clearTokens()
  window.dispatchEvent(new CustomEvent(SESSION_EXPIRED_EVENT, { detail: { message } }))
}

export function resolveApiUrl(path: string) {
  return new URL(path.replace(/^\//, ''), API_ROOT).toString()
}

function shouldTryRefresh(error: ApiRequestError) {
  return error.status === 401 && !error.code?.startsWith('SSO_TICKET')
}

async function rawRequest<T>(
  path: string,
  init: RequestInit = {},
  withAuth = true,
): Promise<T> {
  const headers = new Headers(init.headers ?? {})
  headers.set('Content-Type', 'application/json')
  if (withAuth) {
    const token = getAccessToken()
    if (token) {
      headers.set('Authorization', `Bearer ${token}`)
    }
  }

  const response = await fetch(resolveApiUrl(path), {
    ...init,
    headers,
  })

  if (!response.ok) {
    const body = (await response.json().catch(() => null)) as ApiEnvelope<unknown> | null
    throw new ApiRequestError(
      body?.message ?? `Request failed with status ${response.status}`,
      response.status,
      body?.code,
    )
  }

  const body = (await response.json()) as ApiEnvelope<T>
  return body.data
}

let refreshing: Promise<void> | null = null

async function tryRefresh() {
  const refreshToken = getRefreshToken()
  if (!refreshToken) {
    throw new ApiRequestError('Session expired.', 401, 'SESSION_EXPIRED')
  }
  if (!refreshing) {
    refreshing = fetch(resolveApiUrl('/auth/refresh'), {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ refreshToken }),
    })
      .then(async (response) => {
        if (!response.ok) {
          const body = (await response.json().catch(() => null)) as ApiEnvelope<unknown> | null
          throw new ApiRequestError(body?.message ?? 'Session expired.', response.status, body?.code)
        }
        const body = (await response.json()) as ApiEnvelope<AuthPayload>
        setTokens(body.data)
      })
      .finally(() => {
        refreshing = null
      })
  }
  return refreshing
}

export async function request<T>(path: string, init: RequestInit = {}, withAuth = true): Promise<T> {
  try {
    return await rawRequest<T>(path, init, withAuth)
  } catch (error) {
    if (withAuth && error instanceof ApiRequestError && shouldTryRefresh(error)) {
      try {
        await tryRefresh()
        return await rawRequest<T>(path, init, withAuth)
      } catch (refreshError) {
        if (refreshError instanceof ApiRequestError && shouldTryRefresh(refreshError)) {
          notifySessionExpired()
        }
        throw refreshError
      }
    }
    throw error
  }
}
