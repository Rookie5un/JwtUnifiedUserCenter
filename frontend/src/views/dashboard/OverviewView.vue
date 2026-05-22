<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'

import { roleLabel } from '@/composables/format'
import { portalApps } from '@/constants/portalApps'
import { useAuthStore } from '@/stores/auth'
import { usePortalStore } from '@/stores/portal'
import type { PortalApp } from '@/types'

const auth = useAuthStore()
const portal = usePortalStore()
const router = useRouter()

const openingKey = ref('')
const apps = computed(() => portal.apps)
const unavailableCount = computed(() => Math.max(portalApps.length - apps.value.length, 0))
const userRoles = computed(() => auth.user?.roles.map(roleLabel).join(' / ') || '未分配角色')
const portalStats = computed(() => [
  { label: '可访问系统', value: String(apps.value.length) },
  { label: '授权角色', value: String(auth.user?.roles.length ?? 0) },
  { label: '权限点', value: String(auth.user?.permissions.length ?? 0) },
])

async function openApp(app: PortalApp) {
  openingKey.value = app.key
  const appWindow = window.open('', '_blank')
  try {
    await portal.authorize(app.key)
    const target = portal.lastAuthorization?.entryPath ?? router.resolve({ name: app.routeName, params: app.routeParams }).href
    if (appWindow) {
      appWindow.location.href = target
    } else {
      await router.push(target)
    }
  } catch (err) {
    appWindow?.close()
    throw err
  } finally {
    openingKey.value = ''
  }
}
</script>

<template>
  <div class="portal-page">
    <section class="portal-hero fade-rise">
      <div class="hero-copy">
        <span class="eyebrow">Single Sign-On Portal</span>
        <h2>集成门户</h2>
        <p>当前用户已通过统一认证，点击业务系统将在新标签页免密进入。</p>
      </div>
      <div class="hero-account surface">
        <span>当前登录</span>
        <strong>{{ auth.user?.displayName }}</strong>
        <small>{{ auth.user?.department }} · {{ userRoles }}</small>
      </div>
    </section>

    <section class="stat-strip fade-rise" style="animation-delay: 80ms">
      <article v-for="stat in portalStats" :key="stat.label">
        <span>{{ stat.label }}</span>
        <strong>{{ stat.value }}</strong>
      </article>
      <article>
        <span>免密状态</span>
        <strong>已启用</strong>
      </article>
    </section>

    <section class="apps-section fade-rise" style="animation-delay: 150ms">
      <div class="section-head">
        <div>
          <span class="eyebrow">Authorized Systems</span>
          <h3>我的业务系统</h3>
        </div>
        <p v-if="unavailableCount" class="muted">{{ unavailableCount }} 个系统因角色权限未展示。</p>
      </div>

      <div class="app-grid">
        <button
          v-for="(app, index) in apps"
          :key="app.key"
          type="button"
          class="app-card"
          :style="{ '--app-accent': app.accent, animationDelay: `${180 + index * 55}ms` }"
          @click="openApp(app)"
        >
          <div class="app-topline">
            <div class="app-icon">{{ app.iconLabel }}</div>
            <span>{{ app.status === 'online' ? '已接入' : '示例页' }}</span>
          </div>
          <div>
            <small>{{ app.category }}</small>
            <h4>{{ app.name }}</h4>
            <p>{{ app.description }}</p>
          </div>
          <div class="app-metrics">
            <div v-for="metric in app.metrics" :key="metric.label">
              <span>{{ metric.label }}</span>
              <strong>{{ metric.value }}</strong>
            </div>
          </div>
          <span class="open-hint">{{ openingKey === app.key ? '授权中' : '进入系统' }}</span>
        </button>
      </div>
    </section>
  </div>
</template>

<style scoped>
.portal-page {
  display: grid;
  gap: 1rem;
}

.portal-hero {
  min-height: 220px;
  border-radius: 24px;
  padding: clamp(1.4rem, 3vw, 2rem);
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 1.4rem;
  color: #fff8f2;
  overflow: hidden;
  background:
    linear-gradient(120deg, rgba(23, 22, 26, 0.9), rgba(23, 22, 26, 0.42)),
    url("https://images.unsplash.com/photo-1497366811353-6870744d04b2?auto=format&fit=crop&w=1800&q=80") center/cover;
  box-shadow: 0 28px 70px rgba(23, 22, 26, 0.2);
}

.portal-hero .eyebrow,
.portal-hero p {
  color: rgba(255, 248, 242, 0.74);
}

.hero-copy {
  max-width: 660px;
}

.hero-copy h2 {
  margin: 0.45rem 0 0;
  font-size: clamp(2.2rem, 5vw, 4.2rem);
  line-height: 0.98;
  letter-spacing: -0.06em;
}

.hero-copy p {
  max-width: 38rem;
  margin: 1rem 0 0;
  line-height: 1.8;
}

.hero-account {
  min-width: min(100%, 280px);
  border-radius: 18px;
  padding: 1rem;
  background: rgba(255, 252, 247, 0.14);
  border-color: rgba(255, 255, 255, 0.18);
  color: #fff8f2;
}

.hero-account span,
.hero-account small {
  display: block;
  color: rgba(255, 248, 242, 0.72);
}

.hero-account strong {
  display: block;
  margin: 0.32rem 0;
  font-size: 1.35rem;
}

.stat-strip {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  overflow: hidden;
  border-radius: 18px;
  border: 1px solid rgba(23, 22, 26, 0.07);
  background: var(--line);
  gap: 1px;
}

.stat-strip article {
  padding: 1rem;
  background: rgba(255, 252, 247, 0.78);
}

.stat-strip span {
  display: block;
  color: var(--ink-soft);
  font-size: 0.78rem;
}

.stat-strip strong {
  display: block;
  margin-top: 0.35rem;
  font-size: 1.35rem;
}

.apps-section {
  display: grid;
  gap: 1rem;
}

.section-head {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: end;
}

.section-head h3 {
  margin: 0.35rem 0 0;
  font-size: 1.5rem;
  letter-spacing: -0.04em;
}

.section-head p {
  margin: 0;
}

.app-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 1rem;
}

.app-card {
  width: 100%;
  text-align: left;
  color: inherit;
  min-height: 260px;
  border-radius: 8px;
  padding: 1.1rem;
  background: rgba(255, 252, 247, 0.82);
  border: 1px solid rgba(23, 22, 26, 0.08);
  display: grid;
  align-content: space-between;
  gap: 1.2rem;
  box-shadow: 0 18px 44px rgba(28, 24, 19, 0.08);
  cursor: pointer;
  animation: fade-rise 640ms cubic-bezier(0.22, 1, 0.36, 1) both;
  transition:
    transform 180ms ease,
    border-color 180ms ease,
    box-shadow 180ms ease;
}

.app-card:hover {
  transform: translateY(-4px);
  border-color: color-mix(in srgb, var(--app-accent) 46%, transparent);
  box-shadow: 0 24px 56px rgba(28, 24, 19, 0.14);
}

.app-topline {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: start;
}

.app-icon {
  width: 54px;
  height: 54px;
  border-radius: 8px;
  display: grid;
  place-items: center;
  background: color-mix(in srgb, var(--app-accent) 16%, white);
  color: var(--app-accent);
  border: 1px solid color-mix(in srgb, var(--app-accent) 26%, transparent);
  font-weight: 800;
}

.app-topline > span {
  color: var(--app-accent);
  font-size: 0.78rem;
  padding: 0.35rem 0.55rem;
  border-radius: 999px;
  background: color-mix(in srgb, var(--app-accent) 10%, white);
}

.app-card small,
.app-card p,
.app-metrics span {
  color: var(--ink-soft);
}

.app-card h4 {
  margin: 0.38rem 0 0;
  font-size: 1.25rem;
  letter-spacing: -0.03em;
}

.app-card p {
  margin: 0.7rem 0 0;
  line-height: 1.7;
}

.app-metrics {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.8rem;
  padding-top: 1rem;
  border-top: 1px solid rgba(23, 22, 26, 0.08);
}

.app-metrics span,
.app-metrics strong {
  display: block;
}

.app-metrics strong {
  margin-top: 0.24rem;
}

.open-hint {
  justify-self: start;
  color: var(--app-accent);
  font-size: 0.86rem;
  font-weight: 700;
}

@media (max-width: 1120px) {
  .app-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .portal-hero,
  .section-head {
    grid-template-columns: 1fr;
    flex-direction: column;
    align-items: stretch;
  }

  .stat-strip,
  .app-grid {
    grid-template-columns: 1fr;
  }
}
</style>
