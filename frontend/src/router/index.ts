import { createRouter, createWebHistory } from 'vue-router'

import AuthView from '@/views/auth/AuthView.vue'
import OverviewView from '@/views/dashboard/OverviewView.vue'
import RecordsView from '@/views/performance/RecordsView.vue'
import ApprovalsView from '@/views/performance/ApprovalsView.vue'
import AdminView from '@/views/admin/AdminView.vue'
import AppShell from '@/layouts/AppShell.vue'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: AuthView,
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
  if (!auth.user && to.name !== 'login') {
    await auth.bootstrap()
  }

  if (to.name === 'login' && auth.isAuthenticated) {
    return { name: 'overview' }
  }

  if (to.name !== 'login' && !auth.isAuthenticated) {
    return { name: 'login' }
  }

  if (to.name !== 'login' && auth.isAuthenticated) {
    try {
      await auth.refreshProfile()
    } catch {
      await auth.logout()
      return { name: 'login' }
    }
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

  return true
})

export default router
