<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'

import { api } from '@/api/service'
import { formatCurrency, formatDate } from '@/composables/format'
import AppDialog from '@/components/AppDialog.vue'
import { useAuthStore } from '@/stores/auth'
import type { DashboardResponse, PerformancePayload, PerformanceRecord } from '@/types'
import StatusTag from '@/components/StatusTag.vue'

const auth = useAuthStore()

const records = ref<PerformanceRecord[]>([])
const dashboard = ref<DashboardResponse | null>(null)
const loading = ref(true)
const error = ref('')
const activeId = ref<number | null>(null)
const allRecordsOpen = ref(false)
const deleteConfirmOpen = ref(false)
const pendingDeleteRecordId = ref<number | null>(null)
const recordPreviewLimit = 4

const form = reactive({
  amount: '',
  occurredOn: new Date().toISOString().slice(0, 10),
  type: '新签合同',
  note: '',
})

const selectedRecord = computed(() => records.value.find((item) => item.id === activeId.value) ?? null)
const canCreateRecord = computed(() => auth.hasPermission('PERFORMANCE_CREATE'))
const canEditSelfRecord = computed(() => auth.hasPermission('PERFORMANCE_EDIT_SELF'))
const canDeleteSelfRecord = computed(() => auth.hasPermission('PERFORMANCE_DELETE_SELF'))
const canViewLedger = computed(() => auth.canViewOwnPerformance)
const showCreateComposer = computed(() => canCreateRecord.value && activeId.value === null)
const showEditComposer = computed(() => Boolean(activeId.value) && canEditSelfRecord.value)
const showComposer = computed(() => showCreateComposer.value || showEditComposer.value)
const showLedger = computed(() => canViewLedger.value)
const canSubmit = computed(() => (activeId.value ? canEditSelfRecord.value : canCreateRecord.value))
const singlePanel = computed(() => showComposer.value !== showLedger.value)
const previewRecords = computed(() => records.value.slice(0, recordPreviewLimit))
const hiddenRecordCount = computed(() => Math.max(records.value.length - recordPreviewLimit, 0))
const hasHiddenRecords = computed(() => hiddenRecordCount.value > 0)

const canEditRecord = (record: PerformanceRecord) =>
  canEditSelfRecord.value && record.ownerId === auth.user?.id && record.status !== 'APPROVED'

const canDeleteRecord = (record: PerformanceRecord) =>
  canDeleteSelfRecord.value && record.ownerId === auth.user?.id && record.status !== 'APPROVED'
const pendingDeleteRecord = computed(() => records.value.find((item) => item.id === pendingDeleteRecordId.value) ?? null)

function resetForm() {
  activeId.value = null
  form.amount = ''
  form.occurredOn = new Date().toISOString().slice(0, 10)
  form.type = '新签合同'
  form.note = ''
}

function editRecord(record: PerformanceRecord) {
  if (!canEditRecord(record)) return
  allRecordsOpen.value = false
  activeId.value = record.id
  form.amount = String(record.amount)
  form.occurredOn = record.occurredOn
  form.type = record.type
  form.note = record.note ?? ''
}

async function loadData() {
  loading.value = true
  error.value = ''
  try {
    const [ledger, board] = await Promise.all([
      showLedger.value ? api.records() : Promise.resolve([] as PerformanceRecord[]),
      canViewLedger.value ? api.personalDashboard() : Promise.resolve(null),
    ])
    records.value = ledger
    dashboard.value = board
  } catch (err) {
    error.value = err instanceof Error ? err.message : '载入失败。'
  } finally {
    loading.value = false
  }
}

async function submit() {
  error.value = ''
  if (!canSubmit.value) {
    error.value = activeId.value ? '当前角色没有编辑个人业绩的权限。' : '当前角色没有录入业绩的权限。'
    return
  }
  try {
    const payload: PerformancePayload = {
      amount: Number(form.amount),
      occurredOn: form.occurredOn,
      type: form.type,
      note: form.note,
    }

    if (activeId.value) {
      await api.updateRecord(activeId.value, payload)
    } else {
      await api.createRecord(payload)
    }
    resetForm()
    await loadData()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '保存失败。'
  }
}

async function removeRecord(recordId: number) {
  try {
    await api.deleteRecord(recordId)
    if (activeId.value === recordId) resetForm()
    await loadData()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '删除失败。'
  }
}

function requestRemoveRecord(recordId: number) {
  allRecordsOpen.value = false
  pendingDeleteRecordId.value = recordId
  deleteConfirmOpen.value = true
}

function closeDeleteConfirm() {
  deleteConfirmOpen.value = false
  pendingDeleteRecordId.value = null
}

function confirmRemoveRecord() {
  const recordId = pendingDeleteRecordId.value
  closeDeleteConfirm()
  if (recordId) {
    void removeRecord(recordId)
  }
}

onMounted(loadData)
</script>

<template>
  <div class="records-page">
    <section class="records-head surface fade-rise">
      <div>
        <span class="eyebrow">Performance Ledger</span>
        <h2 class="section-title">业绩台账</h2>
        <p class="muted">左侧维护个人业绩，右侧查看你当前可见范围内的记录流。</p>
      </div>
      <div v-if="dashboard" class="head-metrics">
        <article>
          <span>个人已审批</span>
          <strong>{{ formatCurrency(dashboard.totalAmount) }}</strong>
        </article>
        <article>
          <span>待审批</span>
          <strong>{{ dashboard.pendingCount }}</strong>
        </article>
        <article>
          <span>已驳回</span>
          <strong>{{ dashboard.rejectedCount }}</strong>
        </article>
      </div>
    </section>

    <div class="records-grid" :class="{ 'single-panel': singlePanel }">
      <section v-if="showComposer" class="composer surface fade-rise" style="animation-delay: 80ms">
        <div class="panel-head">
          <div>
            <span class="eyebrow">{{ activeId ? 'Edit Draft' : 'New Record' }}</span>
            <h3>{{ activeId ? '修改记录' : '录入新业绩' }}</h3>
          </div>
          <button v-if="activeId" class="button button-secondary" @click="resetForm">清空</button>
        </div>

        <form class="composer-form" @submit.prevent="submit">
          <div class="field">
            <label>业绩金额</label>
            <input v-model="form.amount" type="number" min="0" step="0.01" placeholder="68000" />
          </div>
          <div class="field two-up">
            <div class="field">
              <label>发生日期</label>
              <input v-model="form.occurredOn" type="date" />
            </div>
            <div class="field">
              <label>业绩类型</label>
              <select v-model="form.type">
                <option>新签合同</option>
                <option>续费回款</option>
                <option>新增渠道</option>
                <option>项目拓展</option>
              </select>
            </div>
          </div>
          <div class="field">
            <label>备注</label>
            <textarea v-model="form.note" rows="5" placeholder="补充说明这条业绩的业务背景"></textarea>
          </div>

          <p v-if="selectedRecord" class="muted">
            当前编辑：{{ selectedRecord.type }}，状态为
            <strong>{{ selectedRecord.status }}</strong>。保存后会重新进入待审批。
          </p>
          <p v-if="error" class="error-copy">{{ error }}</p>

          <button class="button button-primary" :disabled="loading || !canSubmit">
            {{ activeId ? '更新并重新提交' : '提交业绩' }}
          </button>
        </form>
      </section>

      <section v-if="showLedger" class="ledger-panel surface fade-rise" style="animation-delay: 140ms">
        <div class="panel-head">
          <div>
            <span class="eyebrow">Visible Ledger</span>
            <h3>记录流</h3>
          </div>
          <div class="panel-meta">
            <span class="muted">{{ loading ? '载入中' : `${records.length} 条记录` }}</span>
            <button
              v-if="hasHiddenRecords"
              class="button button-secondary compact"
              type="button"
              @click="allRecordsOpen = true"
            >
              查看全部
            </button>
          </div>
        </div>

        <p v-if="!canCreateRecord && canEditSelfRecord && !activeId" class="muted edit-hint">
          当前角色没有录入新业绩权限；如需修改已有记录，请在下方选择一条你自己的未审批记录。
        </p>
        <p v-if="records.length" class="muted flow-hint">
          {{
            hasHiddenRecords
              ? `默认先展示最近 ${recordPreviewLimit} 条，剩余 ${hiddenRecordCount} 条通过弹窗查看。`
              : '当前已展示全部记录。'
          }}
        </p>
        <p v-else-if="!loading" class="muted empty-copy">当前没有可显示的业绩记录。</p>

        <div class="ledger-list">
          <article v-for="record in previewRecords" :key="record.id" class="ledger-card">
            <div class="ledger-main">
              <div>
                <h4>{{ record.type }}</h4>
                <p>{{ record.ownerName }} · {{ record.department }} · {{ formatDate(record.occurredOn) }}</p>
              </div>
              <div class="ledger-amount">
                <StatusTag :status="record.status" />
                <strong>{{ formatCurrency(record.amount) }}</strong>
              </div>
            </div>

            <p class="ledger-note">{{ record.note || '没有额外备注。' }}</p>
            <p v-if="record.rejectedReason" class="rejected-note">驳回原因：{{ record.rejectedReason }}</p>

            <div class="ledger-actions">
              <button
                v-if="canEditRecord(record)"
                class="button button-secondary"
                @click="editRecord(record)"
              >
                编辑
              </button>
              <button
                v-if="canDeleteRecord(record)"
                class="button button-secondary"
                @click="requestRemoveRecord(record.id)"
              >
                删除
              </button>
            </div>
          </article>
        </div>
      </section>
    </div>

    <AppDialog
      :open="allRecordsOpen"
      title="全部记录流"
      width="wide"
      @close="allRecordsOpen = false"
    >
      <p class="muted">
        当前可见范围内共 {{ records.length }} 条记录，完整列表保留编辑和删除入口。
      </p>

      <div v-if="records.length" class="dialog-ledger-list">
        <article v-for="record in records" :key="record.id" class="dialog-ledger-card">
          <div class="ledger-main">
            <div>
              <h4>{{ record.type }}</h4>
              <p>{{ record.ownerName }} · {{ record.department }} · {{ formatDate(record.occurredOn) }}</p>
            </div>
            <div class="ledger-amount">
              <StatusTag :status="record.status" />
              <strong>{{ formatCurrency(record.amount) }}</strong>
            </div>
          </div>

          <p class="ledger-note">{{ record.note || '没有额外备注。' }}</p>
          <p v-if="record.rejectedReason" class="rejected-note">驳回原因：{{ record.rejectedReason }}</p>

          <div class="ledger-actions">
            <button
              v-if="canEditRecord(record)"
              class="button button-secondary"
              type="button"
              @click="editRecord(record)"
            >
              编辑
            </button>
            <button
              v-if="canDeleteRecord(record)"
              class="button button-secondary"
              type="button"
              @click="requestRemoveRecord(record.id)"
            >
              删除
            </button>
          </div>
        </article>
      </div>
      <p v-else class="muted">当前没有可显示的业绩记录。</p>

      <template #actions>
        <button class="button button-primary" type="button" @click="allRecordsOpen = false">关闭</button>
      </template>
    </AppDialog>

    <AppDialog
      :open="deleteConfirmOpen"
      title="确认删除业绩记录"
      confirm-text="删除记录"
      tone="danger"
      @close="closeDeleteConfirm"
      @confirm="confirmRemoveRecord"
    >
      <p class="muted">删除后这条记录会从当前台账中移除，无法继续进入审批流程。</p>
      <strong v-if="pendingDeleteRecord">
        {{ pendingDeleteRecord.type }} · {{ formatCurrency(pendingDeleteRecord.amount) }} · {{ formatDate(pendingDeleteRecord.occurredOn) }}
      </strong>
    </AppDialog>
  </div>
</template>

<style scoped>
.records-page {
  display: grid;
  gap: 1rem;
}

.records-head {
  border-radius: 30px;
  padding: 1.3rem 1.4rem;
  display: flex;
  justify-content: space-between;
  align-items: end;
  gap: 1.5rem;
}

.head-metrics {
  display: flex;
  gap: 1.4rem;
  flex-wrap: wrap;
}

.head-metrics article span {
  display: block;
  color: var(--ink-soft);
  font-size: 0.76rem;
  text-transform: uppercase;
  letter-spacing: 0.14em;
}

.head-metrics article strong {
  display: block;
  margin-top: 0.3rem;
  font-size: 1.4rem;
}

.records-grid {
  display: grid;
  grid-template-columns: 0.9fr 1.2fr;
  gap: 1rem;
}

.records-grid.single-panel {
  grid-template-columns: 1fr;
}

.composer,
.ledger-panel {
  border-radius: 28px;
  padding: 1.25rem;
}

.composer {
  position: sticky;
  top: 0;
  align-self: start;
}

.panel-head {
  display: flex;
  justify-content: space-between;
  align-items: start;
  gap: 1rem;
  margin-bottom: 1.2rem;
}

.panel-head h3 {
  margin: 0.35rem 0 0;
  font-size: 1.45rem;
  letter-spacing: -0.04em;
}

.panel-meta {
  display: grid;
  justify-items: end;
  gap: 0.65rem;
}

.compact {
  padding: 0.72rem 1rem;
}

.composer-form {
  display: grid;
  gap: 1rem;
}

.two-up {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1rem;
}

.ledger-list {
  display: grid;
  gap: 0.9rem;
}

.edit-hint {
  margin: 0 0 1rem;
}

.flow-hint,
.empty-copy {
  margin: 0 0 1rem;
}

.ledger-card {
  padding: 1rem 0 0.2rem;
  border-top: 1px solid rgba(23, 22, 26, 0.08);
}

.ledger-card:first-child {
  border-top: none;
  padding-top: 0;
}

.ledger-main {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
}

.ledger-main h4 {
  margin: 0;
  font-size: 1.08rem;
}

.ledger-main p,
.ledger-note {
  color: var(--ink-soft);
  line-height: 1.75;
}

.ledger-amount {
  display: grid;
  justify-items: end;
  gap: 0.7rem;
}

.ledger-note {
  margin: 0.75rem 0 0;
}

.rejected-note {
  color: var(--danger);
  margin: 0.35rem 0 0;
}

.ledger-actions {
  display: flex;
  gap: 0.7rem;
  margin-top: 1rem;
}

.dialog-ledger-list {
  display: grid;
  gap: 0.9rem;
}

.dialog-ledger-card {
  padding: 1rem;
  border-radius: 22px;
  background: rgba(255, 252, 247, 0.8);
  border: 1px solid rgba(23, 22, 26, 0.08);
}

.error-copy {
  margin: 0;
  color: var(--danger);
}

@media (max-width: 1080px) {
  .records-grid {
    grid-template-columns: 1fr;
  }

  .composer {
    position: relative;
  }
}

@media (max-width: 720px) {
  .records-head {
    flex-direction: column;
    align-items: start;
  }

  .panel-meta {
    justify-items: start;
  }

  .two-up {
    grid-template-columns: 1fr;
  }

  .ledger-main {
    flex-direction: column;
  }

  .ledger-amount {
    justify-items: start;
  }
}
</style>
