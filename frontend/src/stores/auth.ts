import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

import { api } from '@/api/service'
import type { LoginPayload, RegisterPayload, User } from '@/types'

export const useAuthStore = defineStore('auth', () => {
  const user = ref<User | null>(null)
  const loading = ref(false)

  const isAuthenticated = computed(() => Boolean(user.value))
  const roleCodes = computed(() => user.value?.roles ?? [])
  const permissionCodes = computed(() => user.value?.permissions ?? [])

  const isAdmin = computed(() => roleCodes.value.includes('ADMIN'))
  const isManager = computed(() => roleCodes.value.includes('MANAGER'))
  const canManageUsers = computed(() => hasPermission('USER_MANAGE'))
  const canManageRoles = computed(() => hasPermission('ROLE_MANAGE'))
  const canManagePermissions = computed(() => hasPermission('PERMISSION_MANAGE'))
  const canViewLogs = computed(() => hasPermission('LOG_VIEW'))
  const canViewOwnPerformance = computed(() =>
    hasAnyPermission(['PERFORMANCE_VIEW_SELF', 'PERFORMANCE_VIEW_DEPARTMENT', 'PERFORMANCE_VIEW_GLOBAL']),
  )
  const canApprovePerformance = computed(() => hasPermission('PERFORMANCE_APPROVE'))
  const canViewDepartmentPerformance = computed(() =>
    hasAnyPermission(['PERFORMANCE_VIEW_DEPARTMENT', 'PERFORMANCE_VIEW_GLOBAL']),
  )
  const canViewGlobalPerformance = computed(() => hasPermission('PERFORMANCE_VIEW_GLOBAL'))
  const canAccessRecordsPage = computed(() =>
    hasAnyPermission([
      'PERFORMANCE_VIEW_SELF',
      'PERFORMANCE_VIEW_DEPARTMENT',
      'PERFORMANCE_VIEW_GLOBAL',
      'PERFORMANCE_CREATE',
      'PERFORMANCE_EDIT_SELF',
      'PERFORMANCE_DELETE_SELF',
    ]),
  )
  const canAccessApprovals = computed(() => canApprovePerformance.value && canViewDepartmentPerformance.value)
  const canAccessAdminConsole = computed(() =>
    hasAnyPermission(['USER_MANAGE', 'ROLE_MANAGE', 'PERMISSION_MANAGE', 'LOG_VIEW']),
  )

  function hasPermission(permission: string) {
    return permissionCodes.value.includes(permission)
  }

  function hasAnyPermission(permissions: string[]) {
    return permissions.some((permission) => hasPermission(permission))
  }

  function hasAllPermissions(permissions: string[]) {
    return permissions.every((permission) => hasPermission(permission))
  }

  async function bootstrap() {
    try {
      user.value = await api.me()
    } catch {
      user.value = null
    }
  }

  async function login(payload: LoginPayload) {
    loading.value = true
    try {
      const auth = await api.login(payload)
      user.value = auth.user
      return auth
    } finally {
      loading.value = false
    }
  }

  async function register(payload: RegisterPayload) {
    loading.value = true
    try {
      return await api.register(payload)
    } finally {
      loading.value = false
    }
  }

  async function refreshProfile() {
    user.value = await api.me()
  }

  async function logout() {
    await api.logout()
    user.value = null
  }

  return {
    user,
    loading,
    isAuthenticated,
    roleCodes,
    permissionCodes,
    isAdmin,
    isManager,
    canManageUsers,
    canManageRoles,
    canManagePermissions,
    canViewLogs,
    canViewOwnPerformance,
    canApprovePerformance,
    canViewDepartmentPerformance,
    canViewGlobalPerformance,
    canAccessRecordsPage,
    canAccessApprovals,
    canAccessAdminConsole,
    hasPermission,
    hasAnyPermission,
    hasAllPermissions,
    bootstrap,
    login,
    register,
    refreshProfile,
    logout,
  }
})
