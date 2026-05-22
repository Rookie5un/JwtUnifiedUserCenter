import { ref } from 'vue'
import { defineStore } from 'pinia'

import { api } from '@/api/service'
import type { PortalApp, SsoAuthorization } from '@/types'

export const usePortalStore = defineStore('portal', () => {
  const apps = ref<PortalApp[]>([])
  const loading = ref(false)
  const error = ref('')
  const loadedForUserId = ref<number | null>(null)
  const lastAuthorization = ref<SsoAuthorization | null>(null)

  async function loadApps(userId?: number, force = false) {
    if (!userId) {
      reset()
      return
    }

    if (!force && loadedForUserId.value === userId && apps.value.length) return

    loading.value = true
    error.value = ''
    try {
      apps.value = await api.portalApps()
      loadedForUserId.value = userId
    } catch (err) {
      error.value = err instanceof Error ? err.message : '业务系统目录载入失败。'
      apps.value = []
      loadedForUserId.value = null
    } finally {
      loading.value = false
    }
  }

  async function authorize(appKey: string) {
    lastAuthorization.value = await api.authorizePortalApp(appKey)
    return lastAuthorization.value
  }

  async function verifyTicket(ticket: string) {
    return api.verifySsoTicket(ticket)
  }

  function appByKey(appKey: string) {
    return apps.value.find((app) => app.key === appKey)
  }

  function canAccess(appKey: string) {
    return Boolean(appByKey(appKey))
  }

  function reset() {
    apps.value = []
    loading.value = false
    error.value = ''
    loadedForUserId.value = null
    lastAuthorization.value = null
  }

  return {
    apps,
    loading,
    error,
    loadedForUserId,
    lastAuthorization,
    loadApps,
    authorize,
    verifyTicket,
    appByKey,
    canAccess,
    reset,
  }
})
