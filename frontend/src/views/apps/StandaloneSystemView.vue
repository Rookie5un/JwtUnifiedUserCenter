<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'

import { roleLabel } from '@/composables/format'
import { portalApps } from '@/constants/portalApps'
import { usePortalStore } from '@/stores/portal'
import type { PortalApp, SsoTicketVerification } from '@/types'

const route = useRoute()
const portal = usePortalStore()

const verification = ref<SsoTicketVerification | null>(null)
const loading = ref(true)
const error = ref('')

const appKey = computed(() => String(route.params.appKey ?? ''))
const ticket = computed(() => String(route.query.ticket ?? ''))
const fallbackApp = computed(() => portalApps.find((app) => app.key === appKey.value) ?? null)
const app = computed<PortalApp | null>(() => portal.appByKey(appKey.value) ?? fallbackApp.value)
const userRoles = computed(() => verification.value?.user.roles.map(roleLabel).join(' / ') || '统一用户')
const canViewPerformanceApprovals = computed(() =>
  Boolean(
    verification.value?.user.permissions.includes('PERFORMANCE_APPROVE') &&
      verification.value?.user.permissions.includes('PERFORMANCE_VIEW_DEPARTMENT'),
  ),
)
const performanceMenu = computed(() =>
  [
    { label: '系统工作台', value: '总览', to: null, visible: true },
    { label: '个人台账', value: '录入与维护', to: { name: 'records' }, visible: true },
    { label: '审批队列', value: '经理处理', to: { name: 'approvals' }, visible: canViewPerformanceApprovals.value },
    { label: '统计看板', value: '趋势与排行', to: null, visible: true },
  ].filter((item) => item.visible),
)

const modules = computed(() => {
  if (appKey.value === 'oa') {
    return [
      { title: '统一待办', value: '8', detail: '流程、公告、申请统一在 OA 系统内处理。' },
      { title: '流程中心', value: '14', detail: '合同、请假、采购等流程保持登录态流转。' },
      { title: '通知公告', value: '12', detail: '企业通知面向授权用户直接展示。' },
    ]
  }
  if (appKey.value === 'warehouse') {
    return [
      { title: '库存品类', value: '126', detail: '库存、入库、出库、调拨单统一查询。' },
      { title: '待处理单据', value: '9', detail: '仓库单据进入独立系统后继续处理。' },
      { title: '异常预警', value: '3', detail: '低库存和超期单据自动提示。' },
    ]
  }
  if (appKey.value === 'finance') {
    return [
      { title: '待审批报销', value: '6', detail: '差旅、采购、付款申请在财务系统中处理。' },
      { title: '本月预算', value: '86%', detail: '预算执行情况复用当前用户身份查看。' },
      { title: '付款申请', value: '4', detail: '业务侧付款单免密进入财务流转。' },
    ]
  }
  if (appKey.value === 'performance') {
    return [
      { title: '个人台账', value: '启用', detail: '业绩录入、修改和提交继续使用真实业务接口。' },
      { title: '审批流', value: 'JWT', detail: '经理角色进入审批队列时不再次登录。' },
      { title: '统计看板', value: '实时', detail: '按权限查看个人、部门和全局数据。' },
    ]
  }
  if (appKey.value === 'permission') {
    return [
      { title: '用户管理', value: '启用', detail: '维护账号、组织和状态。' },
      { title: '角色策略', value: '3', detail: '管理员维护角色与权限点。' },
      { title: '权限点', value: '11', detail: '统一控制门户和业务接口访问。' },
    ]
  }
  if (appKey.value === 'logs') {
    return [
      { title: '审计范围', value: '全局', detail: '登录、授权和业务操作统一留痕。' },
      { title: '访问事件', value: 'APP', detail: 'SSO 进入系统会记录 APP_ACCESS。' },
      { title: '结果', value: '成功/失败', detail: '成功和拒绝访问都可审计。' },
    ]
  }
  return [
    { title: '接口规范', value: 'REST', detail: '查看统一用户中心后端接口说明。' },
    { title: '认证方式', value: 'Bearer', detail: '接口通过 JWT 访问令牌保护。' },
    { title: '文档状态', value: '在线', detail: '管理员可免密进入接口文档系统。' },
  ]
})

async function verifyTicket() {
  loading.value = true
  error.value = ''
  try {
    if (!ticket.value) {
      error.value = '授权票据缺失，请返回门户重新进入系统。'
      return
    }
    verification.value = await portal.verifyTicket(ticket.value)
    if (verification.value.appKey !== appKey.value) {
      error.value = '授权票据与当前系统不匹配，请返回门户重新进入系统。'
    }
  } catch (err) {
    error.value = err instanceof Error ? err.message : '授权失效，请返回门户重新进入系统。'
  } finally {
    loading.value = false
  }
}

onMounted(verifyTicket)
</script>

<template>
  <main class="standalone-page" :style="{ '--app-accent': app?.accent ?? '#b4683c' }">
    <section v-if="loading" class="system-shell surface fade-rise">
      <span class="eyebrow">SSO Verification</span>
      <h1>正在校验统一认证票据</h1>
      <p class="muted">系统正在确认本次免密访问授权。</p>
    </section>

    <section v-else-if="error || !app || !verification" class="system-shell surface fade-rise">
      <span class="eyebrow">Access Required</span>
      <h1>授权失效</h1>
      <p class="muted">{{ error || '当前系统不存在或不可访问。' }}</p>
      <RouterLink class="button button-primary" :to="{ name: 'overview' }">返回门户</RouterLink>
    </section>

    <template v-else>
      <section class="system-hero fade-rise">
        <div class="system-icon">{{ app.iconLabel }}</div>
        <div>
          <span class="eyebrow">Independent Business System</span>
          <h1>{{ app.name }}</h1>
          <p>{{ app.description }}</p>
        </div>
        <RouterLink class="button button-secondary hero-action" :to="{ name: 'overview' }">返回门户</RouterLink>
      </section>

      <section class="session-band surface fade-rise" style="animation-delay: 80ms">
        <div>
          <span>当前用户</span>
          <strong>{{ verification.user.displayName }}</strong>
        </div>
        <div>
          <span>所属部门</span>
          <strong>{{ verification.user.department }}</strong>
        </div>
        <div>
          <span>角色</span>
          <strong>{{ userRoles }}</strong>
        </div>
        <div>
          <span>认证状态</span>
          <strong>统一认证已通过</strong>
        </div>
      </section>

      <section v-if="appKey === 'performance'" class="system-layout fade-rise" style="animation-delay: 130ms">
        <aside class="system-sidebar surface">
          <span class="eyebrow">System Menu</span>
          <nav>
            <RouterLink
              v-for="item in performanceMenu"
              :key="item.label"
              class="system-menu-item"
              :class="{ muted: !item.to }"
              :to="item.to ?? route.fullPath"
            >
              <span>{{ item.label }}</span>
              <small>{{ item.value }}</small>
            </RouterLink>
          </nav>
        </aside>

        <div class="module-grid in-system">
          <article
            v-for="(item, index) in modules"
            :key="item.title"
            class="module-card"
            :style="{ animationDelay: `${130 + index * 70}ms` }"
          >
            <span>{{ item.title }}</span>
            <strong>{{ item.value }}</strong>
            <p>{{ item.detail }}</p>
          </article>
        </div>
      </section>

      <section v-else class="module-grid">
        <article
          v-for="(item, index) in modules"
          :key="item.title"
          class="module-card fade-rise"
          :style="{ animationDelay: `${130 + index * 70}ms` }"
        >
          <span>{{ item.title }}</span>
          <strong>{{ item.value }}</strong>
          <p>{{ item.detail }}</p>
        </article>
      </section>

      <section v-if="appKey === 'performance'" class="deep-links surface fade-rise" style="animation-delay: 280ms">
        <div>
          <span class="eyebrow">Real Business Demo</span>
          <h2>进入真实业绩功能</h2>
          <p class="muted">以下入口继续复用当前浏览器登录态，不需要再次输入密码。</p>
        </div>
        <div class="deep-actions">
          <RouterLink class="button button-primary" :to="{ name: 'records' }">个人业绩台账</RouterLink>
          <RouterLink
            v-if="canViewPerformanceApprovals"
            class="button button-secondary"
            :to="{ name: 'approvals' }"
          >
            部门审批队列
          </RouterLink>
        </div>
      </section>

      <section
        v-else-if="['permission', 'logs', 'docs'].includes(appKey)"
        class="deep-links surface fade-rise"
        style="animation-delay: 280ms"
      >
        <div>
          <span class="eyebrow">Connected Module</span>
          <h2>进入已有功能页面</h2>
          <p class="muted">下面入口复用当前浏览器登录态，继续保持免密访问。</p>
        </div>
        <div class="deep-actions">
          <RouterLink v-if="appKey === 'permission'" class="button button-primary" :to="{ name: 'admin' }">
            权限中心控制台
          </RouterLink>
          <RouterLink v-if="appKey === 'logs'" class="button button-primary" :to="{ name: 'logs' }">
            操作日志
          </RouterLink>
          <RouterLink v-if="appKey === 'docs'" class="button button-primary" :to="{ name: 'docs' }">
            接口文档
          </RouterLink>
        </div>
      </section>
    </template>
  </main>
</template>

<style scoped>
.standalone-page {
  min-height: 100vh;
  padding: 1.25rem;
  display: grid;
  align-content: start;
  gap: 1rem;
}

.system-shell,
.system-hero,
.session-band,
.module-grid,
.deep-links {
  width: min(100%, 1180px);
  margin: 0 auto;
}

.system-shell {
  margin-top: 10vh;
  border-radius: 24px;
  padding: clamp(1.4rem, 3vw, 2rem);
  display: grid;
  gap: 1rem;
}

.system-shell h1 {
  margin: 0;
  font-size: clamp(2rem, 5vw, 4rem);
  letter-spacing: -0.06em;
}

.system-shell p {
  margin: 0;
}

.system-hero {
  min-height: 240px;
  border-radius: 24px;
  padding: clamp(1.4rem, 3vw, 2rem);
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  gap: 1.4rem;
  align-items: end;
  color: #fff8f2;
  background:
    linear-gradient(120deg, rgba(23, 22, 26, 0.92), rgba(23, 22, 26, 0.54)),
    linear-gradient(135deg, color-mix(in srgb, var(--app-accent) 42%, #17161a), #17161a);
  box-shadow: 0 28px 70px rgba(23, 22, 26, 0.2);
}

.system-icon {
  width: 74px;
  height: 74px;
  border-radius: 8px;
  display: grid;
  place-items: center;
  background: rgba(255, 252, 247, 0.14);
  border: 1px solid rgba(255, 255, 255, 0.18);
  font-size: 1.18rem;
  font-weight: 800;
}

.system-hero .eyebrow,
.system-hero p {
  color: rgba(255, 248, 242, 0.72);
}

.system-hero h1 {
  margin: 0.4rem 0 0;
  font-size: clamp(2rem, 5vw, 4.2rem);
  line-height: 1;
  letter-spacing: -0.06em;
}

.system-hero p {
  max-width: 42rem;
  margin: 0.9rem 0 0;
  line-height: 1.75;
}

.hero-action {
  color: #fff8f2;
  border-color: rgba(255, 255, 255, 0.3);
  white-space: nowrap;
}

.session-band {
  border-radius: 18px;
  padding: 1rem;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 1px;
}

.session-band div {
  padding: 0.55rem 0.7rem;
}

.session-band span {
  display: block;
  color: var(--ink-soft);
  font-size: 0.78rem;
}

.session-band strong {
  display: block;
  margin-top: 0.3rem;
}

.module-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 1rem;
}

.system-layout {
  width: min(100%, 1180px);
  margin: 0 auto;
  display: grid;
  grid-template-columns: 240px minmax(0, 1fr);
  gap: 1rem;
  align-items: start;
}

.system-sidebar {
  border-radius: 18px;
  padding: 1rem;
  display: grid;
  gap: 1rem;
  position: sticky;
  top: 1.25rem;
}

.system-sidebar nav {
  display: grid;
  gap: 0.55rem;
}

.system-menu-item {
  min-height: 58px;
  border-radius: 8px;
  padding: 0.75rem 0.85rem;
  display: grid;
  gap: 0.22rem;
  background: rgba(23, 22, 26, 0.04);
  border: 1px solid rgba(23, 22, 26, 0.06);
  transition:
    transform 180ms ease,
    border-color 180ms ease,
    background-color 180ms ease;
}

.system-menu-item:hover {
  transform: translateX(3px);
  border-color: color-mix(in srgb, var(--app-accent) 36%, transparent);
  background: color-mix(in srgb, var(--app-accent) 8%, white);
}

.system-menu-item span,
.system-menu-item small {
  display: block;
}

.system-menu-item small {
  color: var(--ink-soft);
}

.system-menu-item.muted {
  cursor: default;
}

.module-grid.in-system {
  width: auto;
  margin: 0;
}

.module-card {
  min-height: 190px;
  border-radius: 8px;
  padding: 1.15rem;
  background: rgba(255, 252, 247, 0.82);
  border: 1px solid rgba(23, 22, 26, 0.08);
  box-shadow: 0 18px 44px rgba(28, 24, 19, 0.08);
  display: grid;
  align-content: space-between;
  gap: 1rem;
}

.module-card span {
  color: var(--ink-soft);
}

.module-card strong {
  font-size: clamp(2rem, 4vw, 3rem);
  letter-spacing: -0.06em;
  color: var(--app-accent);
}

.module-card p {
  margin: 0;
  color: var(--ink-soft);
  line-height: 1.7;
}

.deep-links {
  border-radius: 18px;
  padding: 1.25rem;
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: center;
}

.deep-links h2 {
  margin: 0.35rem 0 0;
  font-size: 1.45rem;
  letter-spacing: -0.04em;
}

.deep-links p {
  margin-bottom: 0;
}

.deep-actions {
  display: flex;
  gap: 0.75rem;
  flex-wrap: wrap;
}

@media (max-width: 920px) {
  .system-hero,
  .session-band,
  .module-grid,
  .system-layout {
    grid-template-columns: 1fr;
  }

  .system-sidebar {
    position: static;
  }

  .deep-links {
    flex-direction: column;
    align-items: flex-start;
  }

  .hero-action {
    justify-self: start;
  }
}
</style>
