<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'

import { useAuthStore } from '@/stores/auth'
import { roleLabel } from '@/composables/format'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

const items = computed(() => [
  { label: '总览', to: { name: 'overview' }, visible: true },
  { label: '业绩台账', to: { name: 'records' }, visible: auth.canAccessRecordsPage },
  { label: '审批队列', to: { name: 'approvals' }, visible: auth.canAccessApprovals },
  { label: '接口文档', to: { name: 'docs' }, visible: auth.isAdmin },
  { label: '操作日志', to: { name: 'logs' }, visible: auth.canViewLogs },
  { label: '系统管理', to: { name: 'admin' }, visible: auth.canAccessAdminConsole },
])

async function handleLogout() {
  await auth.logout()
  router.replace({ name: 'login' })
}
</script>

<template>
  <div class="workspace page-shell">
    <aside class="rail">
      <div class="brand fade-rise">
        <span class="eyebrow">Atlas ID</span>
        <strong>Unified Core</strong>
        <p>RESTful API、JWT 认证与业务验证共用一套可信工作面。</p>
      </div>

      <nav class="nav fade-rise" style="animation-delay: 120ms">
        <RouterLink
          v-for="item in items.filter((entry) => entry.visible)"
          :key="String(item.to.name)"
          class="nav-link"
          :class="{ active: route.name === item.to.name }"
          :to="item.to"
        >
          {{ item.label }}
        </RouterLink>
      </nav>

      <div class="rail-footer fade-rise" style="animation-delay: 240ms">
        <div class="profile-orbit"></div>
        <div class="profile-copy">
          <span>{{ auth.user?.displayName }}</span>
          <small>{{ auth.user?.roles.map(roleLabel).join(' · ') }}</small>
        </div>
        <button class="button button-secondary logout" @click="handleLogout">退出</button>
      </div>
    </aside>

    <main class="main-stage">
      <header class="stage-header surface fade-rise">
        <div>
          <span class="eyebrow">Workspace</span>
          <h1>{{ auth.user?.department }}</h1>
        </div>
        <div class="header-meta">
          <div>
            <span>账号</span>
            <strong>{{ auth.user?.username }}</strong>
          </div>
          <div>
            <span>状态</span>
            <strong>{{ auth.user?.status === 'ACTIVE' ? '可用' : '停用' }}</strong>
          </div>
        </div>
      </header>

      <section class="stage-body">
        <RouterView />
      </section>
    </main>
  </div>
</template>

<style scoped>
.workspace {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  gap: 1.25rem;
  padding: 1.25rem;
}

.rail {
  min-height: calc(100vh - 2.5rem);
  background:
    radial-gradient(circle at top right, rgba(180, 104, 60, 0.22), transparent 30%),
    linear-gradient(180deg, #17161a 0%, #1d1c22 100%);
  border-radius: 34px;
  padding: 1.6rem;
  color: #f3ede6;
  display: grid;
  grid-template-rows: auto 1fr auto;
  box-shadow: 0 32px 80px rgba(17, 13, 10, 0.28);
  position: sticky;
  top: 1.25rem;
}

.brand strong {
  display: block;
  margin-top: 0.35rem;
  font-size: 1.7rem;
  letter-spacing: -0.04em;
}

.brand p {
  color: rgba(243, 237, 230, 0.68);
  line-height: 1.7;
  margin: 0.9rem 0 0;
}

.nav {
  display: grid;
  align-content: start;
  gap: 0.4rem;
  margin-top: 2rem;
}

.nav-link {
  padding: 0.95rem 1rem;
  border-radius: 18px;
  color: rgba(243, 237, 230, 0.72);
  transition:
    transform 180ms ease,
    background-color 180ms ease,
    color 180ms ease;
}

.nav-link:hover {
  transform: translateX(3px);
  background: rgba(255, 255, 255, 0.05);
  color: #fff8f1;
}

.nav-link.active {
  background: linear-gradient(90deg, rgba(255, 248, 241, 0.12), rgba(180, 104, 60, 0.14));
  color: #fff8f1;
}

.rail-footer {
  align-self: end;
  padding-top: 1.4rem;
  border-top: 1px solid rgba(255, 255, 255, 0.09);
  display: grid;
  gap: 0.85rem;
}

.profile-orbit {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  background:
    radial-gradient(circle at 35% 35%, rgba(255, 255, 255, 0.9), rgba(255, 255, 255, 0.18) 30%, transparent 34%),
    radial-gradient(circle at 60% 40%, rgba(180, 104, 60, 0.7), rgba(180, 104, 60, 0.12) 48%, transparent 52%);
  border: 1px solid rgba(255, 255, 255, 0.18);
}

.profile-copy {
  display: grid;
  gap: 0.22rem;
}

.profile-copy span {
  font-size: 1rem;
}

.profile-copy small {
  color: rgba(243, 237, 230, 0.58);
}

.logout {
  color: #f2ebe2;
  border-color: rgba(255, 255, 255, 0.14);
}

.main-stage {
  min-width: 0;
  display: grid;
  grid-template-rows: auto 1fr;
  gap: 1rem;
}

.stage-header {
  border-radius: 28px;
  padding: 1.1rem 1.4rem;
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: center;
}

.stage-header h1 {
  margin: 0.35rem 0 0;
  font-size: 2rem;
  letter-spacing: -0.05em;
}

.header-meta {
  display: flex;
  gap: 2rem;
}

.header-meta span {
  display: block;
  color: var(--ink-soft);
  font-size: 0.82rem;
  text-transform: uppercase;
  letter-spacing: 0.14em;
}

.header-meta strong {
  display: block;
  margin-top: 0.3rem;
}

.stage-body {
  min-width: 0;
}

@media (max-width: 1080px) {
  .workspace {
    grid-template-columns: 1fr;
  }

  .rail {
    position: relative;
    min-height: auto;
  }
}

@media (max-width: 720px) {
  .workspace {
    padding: 0.85rem;
  }

  .stage-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .header-meta {
    gap: 1rem;
    flex-wrap: wrap;
  }
}
</style>
