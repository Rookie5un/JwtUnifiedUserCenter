import type { ApiEnvelope, AuthPayload } from '@/types'

const API_BASE = import.meta.env.VITE_API_BASE ?? 'http://localhost:8080'

const ACCESS_TOKEN_KEY = 'atlas_access_token'
const REFRESH_TOKEN_KEY = 'atlas_refresh_token'

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

  const response = await fetch(`${API_BASE}${path}`, {
    ...init,
    headers,
  })

  if (!response.ok) {
    const body = (await response.json().catch(() => null)) as ApiEnvelope<unknown> | null
    throw new Error(body?.message ?? `Request failed with status ${response.status}`)
  }

  const body = (await response.json()) as ApiEnvelope<T>
  return body.data
}

let refreshing: Promise<void> | null = null

async function tryRefresh() {
  const refreshToken = getRefreshToken()
  if (!refreshToken) {
    throw new Error('Session expired.')
  }
  if (!refreshing) {
    refreshing = fetch(`${API_BASE}/auth/refresh`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ refreshToken }),
    })
      .then(async (response) => {
        if (!response.ok) {
          throw new Error('Session expired.')
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
    const message = error instanceof Error ? error.message : 'Request failed.'
    if (withAuth && message.includes('Authentication is required')) {
      await tryRefresh()
      return rawRequest<T>(path, init, withAuth)
    }
    throw error
  }
}
