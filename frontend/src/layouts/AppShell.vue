<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink, RouterView, useRouter } from 'vue-router'

import { roleLabel } from '@/composables/format'
import { useAuthStore } from '@/stores/auth'
import { usePortalStore } from '@/stores/portal'

const auth = useAuthStore()
const portal = usePortalStore()
const router = useRouter()

const roleText = computed(() => auth.user?.roles.map(roleLabel).join(' · ') || '未分配角色')

async function handleLogout() {
  await auth.logout()
  router.replace({ name: 'login' })
}
</script>

<template>
  <div class="workspace page-shell">
    <header class="topbar surface fade-rise">
      <RouterLink class="brand" :to="{ name: 'overview' }">
        <span class="brand-mark">U</span>
        <span>
          <small>Unified User Center</small>
          <strong>统一用户中心</strong>
        </span>
      </RouterLink>

      <div class="session-meta">
        <div>
          <span>{{ auth.user?.displayName }}</span>
          <small>{{ auth.user?.department }} · {{ roleText }}</small>
        </div>
        <strong>{{ portal.apps.length }} 个系统</strong>
        <button class="button button-secondary logout" type="button" @click="handleLogout">退出登录</button>
      </div>
    </header>

    <main class="main-stage">
      <RouterView />
    </main>
  </div>
</template>

<style scoped>
.workspace {
  min-height: 100vh;
  padding: 1.25rem;
  display: grid;
  grid-template-rows: auto 1fr;
  gap: 1rem;
}

.topbar {
  width: min(100%, 1280px);
  margin: 0 auto;
  border-radius: 24px;
  padding: 0.85rem 1rem;
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: center;
}

.brand {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  min-width: 0;
}

.brand-mark {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  display: grid;
  place-items: center;
  color: #fff8f2;
  background: var(--ink);
  font-weight: 800;
}

.brand small,
.session-meta small {
  display: block;
  color: var(--ink-soft);
  line-height: 1.5;
}

.brand strong {
  display: block;
  font-size: 1.05rem;
}

.session-meta {
  display: flex;
  align-items: center;
  justify-content: end;
  gap: 1rem;
  min-width: 0;
}

.session-meta > div {
  text-align: right;
}

.session-meta span,
.session-meta strong {
  display: block;
}

.session-meta > strong {
  padding: 0.58rem 0.8rem;
  border-radius: 999px;
  background: rgba(23, 22, 26, 0.06);
  white-space: nowrap;
}

.logout {
  white-space: nowrap;
}

.main-stage {
  width: min(100%, 1280px);
  margin: 0 auto;
  min-width: 0;
}

@media (max-width: 760px) {
  .workspace {
    padding: 0.85rem;
  }

  .topbar,
  .session-meta {
    align-items: stretch;
    flex-direction: column;
  }

  .session-meta > div {
    text-align: left;
  }

  .session-meta > strong {
    align-self: flex-start;
  }
}
</style>
