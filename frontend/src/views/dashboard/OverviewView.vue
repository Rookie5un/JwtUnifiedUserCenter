<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'

import { api } from '@/api/service'
import { formatCurrency, formatDate, statusLabel } from '@/composables/format'
import AppDialog from '@/components/AppDialog.vue'
import { useAuthStore } from '@/stores/auth'
import type { DashboardResponse, PerformanceRecord } from '@/types'
import StatusTag from '@/components/StatusTag.vue'

const auth = useAuthStore()

const dashboards = ref<Record<string, DashboardResponse>>({})
const records = ref<PerformanceRecord[]>([])
const activeScope = ref<'personal' | 'department' | 'global'>('personal')
const loading = ref(true)
const error = ref('')
const allRecordsOpen = ref(false)
const approvedAmountOpen = ref(false)

const availableScopes = computed(() => {
  const scopes: Array<'personal' | 'department' | 'global'> = []
  if (!auth.canViewOwnPerformance) return [] as Array<'personal' | 'department' | 'global'>
  scopes.push('personal')
  if (auth.canViewDepartmentPerformance) scopes.push('department')
  if (auth.canViewGlobalPerformance) scopes.push('global')
  return scopes
})

const currentDashboard = computed(() => dashboards.value[activeScope.value])
const dashboardScope = computed<'personal' | 'department' | 'global'>(() => {
  const scope = currentDashboard.value?.scope
  if (scope === 'department' || scope === 'global' || scope === 'personal') return scope
  return activeScope.value
})
const scopeMeta = computed(() => {
  if (dashboardScope.value === 'global') {
    return {
      label: '全局视角',
      totalLabel: '全局已审批累计',
      countLabel: '全局记录总数',
      pendingLabel: '全局待审批',
      approvedLabel: '全局已审批',
      rejectedLabel: '全局已驳回',
    }
  }

  if (dashboardScope.value === 'department') {
    return {
      label: '部门视角',
      totalLabel: '部门已审批累计',
      countLabel: '部门记录总数',
      pendingLabel: '部门待审批',
      approvedLabel: '部门已审批',
      rejectedLabel: '部门已驳回',
    }
  }

  return {
    label: '个人视角',
    totalLabel: '个人已审批累计',
    countLabel: '个人记录总数',
    pendingLabel: '个人待审批',
    approvedLabel: '个人已审批',
    rejectedLabel: '个人已驳回',
  }
})
const recordsForCurrentScope = computed(() => {
  if (dashboardScope.value === 'personal') {
    return records.value.filter((item) => item.ownerId === auth.user?.id)
  }

  if (dashboardScope.value === 'department') {
    return records.value.filter((item) => item.department === auth.user?.department)
  }

  return records.value
})
const approvedRecordsForCurrentScope = computed(() =>
  recordsForCurrentScope.value.filter((item) => item.status === 'APPROVED'),
)
const spotlightRecord = computed(() => recordsForCurrentScope.value[0] ?? null)
const scopeSummary = computed(() => ({
  totalAmount: approvedRecordsForCurrentScope.value.reduce((sum, item) => sum + item.amount, 0),
  totalRecords: recordsForCurrentScope.value.length,
  pendingCount: recordsForCurrentScope.value.filter((item) => item.status === 'PENDING').length,
  approvedCount: approvedRecordsForCurrentScope.value.length,
  rejectedCount: recordsForCurrentScope.value.filter((item) => item.status === 'REJECTED').length,
}))

async function loadData() {
  loading.value = true
  error.value = ''
  try {
    if (!auth.canViewOwnPerformance) {
      dashboards.value = {}
      records.value = []
      error.value = '当前角色没有业绩看板查看权限。'
      return
    }
    records.value = await api.records()
    const entries = await Promise.all(
      availableScopes.value.map(async (scope) => {
        if (scope === 'personal') return [scope, await api.personalDashboard()] as const
        if (scope === 'department') return [scope, await api.departmentDashboard()] as const
        return [scope, await api.globalDashboard()] as const
      }),
    )
    dashboards.value = Object.fromEntries(entries)
    activeScope.value = auth.canViewGlobalPerformance ? 'global' : auth.canViewDepartmentPerformance ? 'department' : 'personal'
  } catch (err) {
    error.value = err instanceof Error ? err.message : '总览数据载入失败。'
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<template>
  <div class="overview-page">
    <section class="overview-hero surface fade-rise">
      <div class="hero-copy">
        <span class="eyebrow">Operational Surface</span>
        <h2 class="section-title">工作区总览</h2>
        <p class="muted">
          把当前用户中心和业绩验证流程收拢到同一个视图里，优先呈现状态、节奏和下一步动作。
        </p>
      </div>

      <div class="scope-switch">
        <button
          v-for="scope in availableScopes"
          :key="scope"
          class="scope-pill"
          :class="{ active: activeScope === scope }"
          @click="activeScope = scope"
        >
          {{ scope === 'personal' ? '个人视角' : scope === 'department' ? '部门视角' : '全局视角' }}
        </button>
      </div>
    </section>

    <section v-if="error && !loading" class="surface overview-empty fade-rise" style="animation-delay: 80ms">
      <span class="eyebrow">No Access</span>
      <h3>当前角色看不到业绩总览</h3>
      <p class="muted">{{ error }}</p>
    </section>

    <section v-else-if="currentDashboard && !loading" class="metric-ribbon fade-rise" style="animation-delay: 80ms">
      <article>
        <span>{{ scopeMeta.totalLabel }}</span>
        <strong>{{ formatCurrency(scopeSummary.totalAmount) }}</strong>
        <button
          v-if="approvedRecordsForCurrentScope.length"
          class="button button-secondary compact metric-action"
          @click="approvedAmountOpen = true"
        >
          查看组成
        </button>
      </article>
      <article>
        <span>{{ scopeMeta.countLabel }}</span>
        <strong>{{ scopeSummary.totalRecords }}</strong>
      </article>
      <article>
        <span>{{ scopeMeta.pendingLabel }}</span>
        <strong>{{ scopeSummary.pendingCount }}</strong>
      </article>
      <article>
        <span>{{ scopeMeta.approvedLabel }}</span>
        <strong>{{ scopeSummary.approvedCount }}</strong>
      </article>
      <article>
        <span>{{ scopeMeta.rejectedLabel }}</span>
        <strong>{{ scopeSummary.rejectedCount }}</strong>
      </article>
    </section>

    <div v-if="currentDashboard && !loading" class="overview-grid">
      <section class="surface stream-block fade-rise" style="animation-delay: 140ms">
        <div class="panel-head">
          <div>
            <span class="eyebrow">Monthly Flow</span>
            <h3>月度业绩轨迹</h3>
          </div>
          <span class="muted">只统计已审批记录</span>
        </div>
        <div class="bars">
          <div
            v-for="(item, index) in currentDashboard.monthly"
            :key="item.month"
            class="bar-row"
          >
            <span>{{ item.month }}</span>
            <div class="bar-track">
              <div
                class="bar-fill"
                :style="{
                  width: `${(item.totalAmount / Math.max(...currentDashboard.monthly.map((entry) => entry.totalAmount), 1)) * 100}%`,
                  animationDelay: `${120 + index * 70}ms`,
                }"
              ></div>
            </div>
            <strong>{{ formatCurrency(item.totalAmount) }}</strong>
          </div>
        </div>
      </section>

      <section class="surface ranking-block fade-rise" style="animation-delay: 220ms">
        <div class="panel-head">
          <div>
            <span class="eyebrow">Leaderboard</span>
            <h3>高贡献成员</h3>
          </div>
          <span class="muted">Top 5</span>
        </div>
        <div class="ranking-list">
          <div v-for="(item, index) in currentDashboard.ranking" :key="item.name" class="ranking-row">
            <div>
              <small>#{{ index + 1 }}</small>
              <strong>{{ item.name }}</strong>
              <span>{{ item.department }}</span>
            </div>
            <b>{{ formatCurrency(item.totalAmount) }}</b>
          </div>
        </div>
      </section>

      <section class="surface detail-block fade-rise" style="animation-delay: 280ms">
        <div class="panel-head">
          <div>
            <span class="eyebrow">Recent Ledger</span>
            <h3>最近发生的记录</h3>
          </div>
          <button v-if="recordsForCurrentScope.length" class="button button-secondary compact" @click="allRecordsOpen = true">
            查看全部记录
          </button>
        </div>
        <div class="ledger">
          <div v-for="item in recordsForCurrentScope.slice(0, 6)" :key="item.id" class="ledger-row">
            <div>
              <strong>{{ item.ownerName }}</strong>
              <span>{{ item.type }} · {{ formatDate(item.occurredOn) }}</span>
            </div>
            <div class="ledger-right">
              <StatusTag :status="item.status" />
              <b>{{ formatCurrency(item.amount) }}</b>
            </div>
          </div>
        </div>
      </section>

      <section class="surface focus-block fade-rise" style="animation-delay: 340ms">
        <div class="panel-head">
          <div>
            <span class="eyebrow">Focus</span>
            <h3>当前优先事项</h3>
          </div>
        </div>
        <div v-if="spotlightRecord" class="focus-copy">
          <StatusTag :status="spotlightRecord.status" />
          <strong>{{ spotlightRecord.ownerName }} 的「{{ spotlightRecord.type }}」</strong>
          <p>
            金额 {{ formatCurrency(spotlightRecord.amount) }}，发生于 {{ formatDate(spotlightRecord.occurredOn) }}。
            当前状态：{{ statusLabel(spotlightRecord.status) }}。
          </p>
        </div>
      </section>
    </div>

    <AppDialog
      :open="approvedAmountOpen"
      :title="`${scopeMeta.totalLabel}组成`"
      width="wide"
      @close="approvedAmountOpen = false"
    >
      <p class="muted">
        当前累计仅统计已审批记录，共 {{ approvedRecordsForCurrentScope.length }} 条，
        合计 {{ formatCurrency(scopeSummary.totalAmount) }}。
      </p>

      <div v-if="approvedRecordsForCurrentScope.length" class="dialog-ledger">
        <article v-for="item in approvedRecordsForCurrentScope" :key="item.id" class="dialog-ledger-row">
          <div>
            <strong>{{ item.ownerName }}</strong>
            <span>{{ item.department }} · {{ item.type }} · {{ formatDate(item.occurredOn) }}</span>
            <p>{{ item.note || '没有额外备注。' }}</p>
          </div>
          <div class="dialog-ledger-meta">
            <StatusTag :status="item.status" />
            <b>{{ formatCurrency(item.amount) }}</b>
          </div>
        </article>
      </div>
      <p v-else class="muted">当前没有计入累计的已审批记录。</p>

      <template #actions>
        <button class="button button-primary" type="button" @click="approvedAmountOpen = false">关闭</button>
      </template>
    </AppDialog>

    <AppDialog
      :open="allRecordsOpen"
      title="全部可见记录"
      width="wide"
      @close="allRecordsOpen = false"
    >
      <div v-if="recordsForCurrentScope.length" class="dialog-ledger">
        <article v-for="item in recordsForCurrentScope" :key="item.id" class="dialog-ledger-row">
          <div>
            <strong>{{ item.ownerName }}</strong>
            <span>{{ item.department }} · {{ item.type }} · {{ formatDate(item.occurredOn) }}</span>
            <p>{{ item.note || '没有额外备注。' }}</p>
          </div>
          <div class="dialog-ledger-meta">
            <StatusTag :status="item.status" />
            <b>{{ formatCurrency(item.amount) }}</b>
          </div>
        </article>
      </div>
      <p v-else class="muted">当前视角下没有可显示的记录。</p>

      <template #actions>
        <button class="button button-primary" type="button" @click="allRecordsOpen = false">关闭</button>
      </template>
    </AppDialog>
  </div>
</template>

<style scoped>
.overview-page {
  display: grid;
  gap: 1rem;
}

.overview-hero {
  border-radius: 30px;
  padding: 1.4rem 1.5rem;
  display: flex;
  justify-content: space-between;
  gap: 1.4rem;
  align-items: end;
}

.hero-copy {
  max-width: 640px;
}

.hero-copy h2 {
  margin-top: 0.3rem;
}

.hero-copy p {
  margin-bottom: 0;
  max-width: 44rem;
  line-height: 1.75;
}

.scope-switch {
  display: flex;
  gap: 0.6rem;
  flex-wrap: wrap;
}

.compact {
  padding: 0.72rem 1rem;
}

.overview-empty {
  border-radius: 26px;
  padding: 1.3rem 1.4rem;
}

.scope-pill {
  border: 1px solid var(--line);
  background: rgba(255, 252, 247, 0.68);
  padding: 0.85rem 1rem;
  border-radius: 999px;
  color: var(--ink-soft);
}

.scope-pill.active {
  background: var(--ink);
  color: #fff8f2;
}

.metric-ribbon {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 1px;
  background: var(--line);
  border-radius: 26px;
  overflow: hidden;
  border: 1px solid rgba(23, 22, 26, 0.07);
}

.metric-ribbon article {
  padding: 1.1rem 1rem 1.2rem;
  background: rgba(255, 252, 247, 0.74);
  display: grid;
  gap: 0.4rem;
}

.metric-action {
  justify-self: start;
  margin-top: 0.25rem;
}

.metric-ribbon span {
  color: var(--ink-soft);
  font-size: 0.78rem;
  text-transform: uppercase;
  letter-spacing: 0.12em;
}

.metric-ribbon strong {
  font-size: clamp(1.5rem, 2vw, 2.3rem);
  letter-spacing: -0.06em;
}

.dialog-ledger {
  display: grid;
  gap: 0.85rem;
}

.dialog-ledger-row {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  padding: 1rem;
  border-radius: 20px;
  background: rgba(255, 252, 247, 0.78);
  border: 1px solid rgba(23, 22, 26, 0.08);
}

.dialog-ledger-row span,
.dialog-ledger-row p {
  display: block;
  margin: 0.35rem 0 0;
  color: var(--ink-soft);
  line-height: 1.7;
}

.dialog-ledger-row p {
  margin-top: 0.45rem;
}

.dialog-ledger-meta {
  display: grid;
  justify-items: end;
  align-content: start;
  gap: 0.7rem;
}

.overview-grid {
  display: grid;
  grid-template-columns: 1.4fr 1fr;
  gap: 1rem;
}

.stream-block,
.ranking-block,
.detail-block,
.focus-block {
  border-radius: 28px;
  padding: 1.25rem;
}

.detail-block,
.focus-block {
  grid-column: span 1;
}

.panel-head {
  display: flex;
  justify-content: space-between;
  align-items: start;
  gap: 1rem;
  margin-bottom: 1.25rem;
}

.panel-head h3 {
  margin: 0.35rem 0 0;
  font-size: 1.45rem;
  letter-spacing: -0.04em;
}

.bars {
  display: grid;
  gap: 1rem;
}

.bar-row {
  display: grid;
  grid-template-columns: 90px minmax(0, 1fr) 120px;
  align-items: center;
  gap: 1rem;
}

.bar-track {
  height: 16px;
  background: rgba(23, 22, 26, 0.06);
  border-radius: 999px;
  overflow: hidden;
}

.bar-fill {
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, rgba(180, 104, 60, 0.42), rgba(180, 104, 60, 0.9));
  animation: expand 680ms cubic-bezier(0.22, 1, 0.36, 1) both;
}

@keyframes expand {
  from {
    width: 0;
  }
}

.ranking-list,
.ledger {
  display: grid;
  gap: 0.85rem;
}

.ranking-row,
.ledger-row {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: center;
  padding: 0.9rem 0;
  border-top: 1px solid rgba(23, 22, 26, 0.08);
}

.ranking-row:first-child,
.ledger-row:first-child {
  border-top: none;
  padding-top: 0;
}

.ranking-row small,
.ledger-row span {
  display: block;
  color: var(--ink-soft);
  margin-top: 0.25rem;
}

.ledger-right {
  display: flex;
  align-items: center;
  gap: 0.9rem;
}

.focus-copy {
  display: grid;
  gap: 0.8rem;
}

.focus-copy strong {
  font-size: 1.1rem;
}

.focus-copy p {
  margin: 0;
  color: var(--ink-soft);
  line-height: 1.8;
}

@media (max-width: 1080px) {
  .overview-grid {
    grid-template-columns: 1fr;
  }

  .metric-ribbon {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .dialog-ledger-row {
    flex-direction: column;
  }

  .dialog-ledger-meta {
    justify-items: start;
  }
}

@media (max-width: 720px) {
  .overview-hero {
    flex-direction: column;
    align-items: start;
  }

  .metric-ribbon {
    grid-template-columns: 1fr;
  }

  .bar-row {
    grid-template-columns: 1fr;
  }
}
</style>
