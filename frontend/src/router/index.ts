import { createRouter, createWebHistory } from 'vue-router'

import AuthView from '@/views/auth/AuthView.vue'
import OverviewView from '@/views/dashboard/OverviewView.vue'
import RecordsView from '@/views/performance/RecordsView.vue'
import ApprovalsView from '@/views/performance/ApprovalsView.vue'
import ApiDocsView from '@/views/docs/ApiDocsView.vue'
import LogsView from '@/views/logs/LogsView.vue'
import AdminView from '@/views/admin/AdminView.vue'
import PortalAppView from '@/views/apps/PortalAppView.vue'
import StandaloneSystemView from '@/views/apps/StandaloneSystemView.vue'
import AppShell from '@/layouts/AppShell.vue'
import { useAuthStore } from '@/stores/auth'
import { usePortalStore } from '@/stores/portal'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: AuthView,
    },
    {
      path: '/systems/:appKey',
      name: 'standalone-system',
      component: StandaloneSystemView,
    },
    {
      path: '/',
      component: AppShell,
      children: [
        {
          path: '',
          name: 'overview',
          component: OverviewView,
        },
        {
          path: 'records',
          name: 'records',
          component: RecordsView,
        },
        {
          path: 'approvals',
          name: 'approvals',
          component: ApprovalsView,
          meta: { managerOnly: true },
        },
        {
          path: 'apps/:appKey',
          name: 'portal-app',
          component: PortalAppView,
        },
        {
          path: 'docs',
          name: 'docs',
          component: ApiDocsView,
          meta: { adminRoleOnly: true },
        },
        {
          path: 'logs',
          name: 'logs',
          component: LogsView,
          meta: { logViewOnly: true },
        },
        {
          path: 'admin',
          name: 'admin',
          component: AdminView,
          meta: { adminOnly: true },
        },
      ],
    },
  ],
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  const portal = usePortalStore()
  if (!auth.user && to.name !== 'login') {
    await auth.bootstrap()
  }

  if (to.name === 'login' && auth.isAuthenticated) {
    return { name: 'overview' }
  }

  if (to.name !== 'login' && !auth.isAuthenticated) {
    if (to.name === 'standalone-system') return true
    return { name: 'login' }
  }

  if (to.name === 'standalone-system') {
    return true
  }

  if (to.name !== 'login' && auth.isAuthenticated) {
    try {
      await auth.refreshProfile()
    } catch {
      await auth.logout()
      return { name: 'login' }
    }
  }

  if (to.name === 'portal-app') {
    await portal.loadApps(auth.user?.id)
    const appKey = String(to.params.appKey ?? '')
    if (!portal.canAccess(appKey)) {
      return { name: 'overview' }
    }
    return { name: 'standalone-system', params: { appKey }, query: to.query, replace: true }
  }

  if (to.name === 'records' && !auth.canAccessRecordsPage) {
    return { name: 'overview' }
  }

  if (to.meta.adminOnly && !auth.canAccessAdminConsole) {
    return { name: 'overview' }
  }

  if (to.meta.managerOnly && !auth.canAccessApprovals) {
    return { name: 'overview' }
  }

  if (to.meta.adminRoleOnly && !auth.isAdmin) {
    return { name: 'overview' }
  }

  if (to.meta.logViewOnly && !auth.canViewLogs) {
    return { name: 'overview' }
  }

  return true
})

export default router
