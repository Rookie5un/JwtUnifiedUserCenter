<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'

import { api } from '@/api/service'
import { formatCurrency, formatDate } from '@/composables/format'
import AppDialog from '@/components/AppDialog.vue'
import type { PerformanceRecord } from '@/types'
import StatusTag from '@/components/StatusTag.vue'

const queue = ref<PerformanceRecord[]>([])
const activeId = ref<number | null>(null)
const loading = ref(true)
const error = ref('')
const rejectReason = ref('')
const approveConfirmOpen = ref(false)
const rejectConfirmOpen = ref(false)

const selected = computed(() => queue.value.find((item) => item.id === activeId.value) ?? queue.value[0] ?? null)

async function loadQueue() {
  loading.value = true
  error.value = ''
  try {
    queue.value = await api.pendingApprovals()
    activeId.value = queue.value[0]?.id ?? null
  } catch (err) {
    error.value = err instanceof Error ? err.message : '审批队列载入失败。'
  } finally {
    loading.value = false
  }
}

async function approve() {
  if (!selected.value) return
  await api.approveRecord(selected.value.id)
  rejectReason.value = ''
  await loadQueue()
}

async function reject() {
  if (!selected.value || !rejectReason.value.trim()) return
  await api.rejectRecord(selected.value.id, rejectReason.value)
  rejectReason.value = ''
  await loadQueue()
}

function requestApprove() {
  if (!selected.value) return
  approveConfirmOpen.value = true
}

function requestReject() {
  if (!selected.value) return
  if (!rejectReason.value.trim()) {
    error.value = '请先填写驳回原因，再进行确认。'
    return
  }
  rejectConfirmOpen.value = true
}

onMounted(loadQueue)
</script>

<template>
  <div class="approval-page">
    <section class="surface approval-head fade-rise">
      <div>
        <span class="eyebrow">Review Queue</span>
        <h2 class="section-title">审批队列</h2>
        <p class="muted">具备审批权限的用户可处理待审批记录，范围由角色绑定的查看权限决定。</p>
      </div>
      <div class="queue-indicator">
        <span>当前待处理</span>
        <strong>{{ queue.length }}</strong>
      </div>
    </section>

    <div class="approval-grid">
      <section class="surface queue-list fade-rise" style="animation-delay: 80ms">
        <div class="panel-head">
          <div>
            <span class="eyebrow">Pending Items</span>
            <h3>待审批列表</h3>
          </div>
        </div>

        <div class="queue-stack">
          <button
            v-for="item in queue"
            :key="item.id"
            class="queue-row"
            :class="{ active: selected?.id === item.id }"
            @click="activeId = item.id"
          >
            <div>
              <strong>{{ item.ownerName }}</strong>
              <span>{{ item.department }} · {{ item.type }}</span>
            </div>
            <b>{{ formatCurrency(item.amount) }}</b>
          </button>

          <p v-if="!queue.length && !loading" class="muted">当前没有待审批记录。</p>
        </div>
      </section>

      <section class="surface review-panel fade-rise" style="animation-delay: 140ms">
        <div v-if="selected" class="review-content">
          <div class="panel-head">
            <div>
              <span class="eyebrow">Review Detail</span>
              <h3>{{ selected.type }}</h3>
            </div>
            <StatusTag :status="selected.status" />
          </div>

          <div class="detail-matrix">
            <article>
              <span>提交人</span>
              <strong>{{ selected.ownerName }}</strong>
            </article>
            <article>
              <span>部门</span>
              <strong>{{ selected.department }}</strong>
            </article>
            <article>
              <span>金额</span>
              <strong>{{ formatCurrency(selected.amount) }}</strong>
            </article>
            <article>
              <span>发生日期</span>
              <strong>{{ formatDate(selected.occurredOn) }}</strong>
            </article>
          </div>

          <div class="notes">
            <span class="eyebrow">Submission Note</span>
            <p>{{ selected.note || '提交人没有填写补充说明。' }}</p>
          </div>

          <div class="field">
            <label>驳回原因</label>
            <textarea
              v-model="rejectReason"
              rows="4"
              placeholder="如果要驳回，请明确指出缺失材料或调整方向"
            ></textarea>
          </div>

          <div class="review-actions">
            <button class="button button-primary" @click="requestApprove">审批通过</button>
            <button class="button button-secondary" @click="requestReject">填写原因后驳回</button>
          </div>
        </div>

        <p v-else class="muted">选择左侧记录开始审批。</p>
      </section>
    </div>

    <AppDialog
      :open="approveConfirmOpen"
      title="确认审批通过"
      confirm-text="确认通过"
      @close="approveConfirmOpen = false"
      @confirm="approveConfirmOpen = false; approve()"
    >
      <p class="muted">通过后这条业绩会进入已审批状态，并参与后续统计与看板计算。</p>
      <strong v-if="selected">{{ selected.ownerName }} · {{ selected.type }} · {{ formatCurrency(selected.amount) }}</strong>
    </AppDialog>

    <AppDialog
      :open="rejectConfirmOpen"
      title="确认驳回业绩"
      confirm-text="确认驳回"
      tone="danger"
      @close="rejectConfirmOpen = false"
      @confirm="rejectConfirmOpen = false; reject()"
    >
      <p class="muted">驳回前请确认原因填写完整。驳回记录不会计入累计金额和排行。</p>
      <strong v-if="selected">{{ selected.ownerName }} · {{ selected.type }} · {{ formatCurrency(selected.amount) }}</strong>
      <p class="muted">当前原因：{{ rejectReason.trim() || '未填写' }}</p>
    </AppDialog>
  </div>
</template>

<style scoped>
.approval-page {
  display: grid;
  gap: 1rem;
}

.approval-head {
  border-radius: 30px;
  padding: 1.3rem 1.4rem;
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: end;
}

.queue-indicator span {
  display: block;
  color: var(--ink-soft);
  font-size: 0.76rem;
  text-transform: uppercase;
  letter-spacing: 0.14em;
}

.queue-indicator strong {
  display: block;
  margin-top: 0.25rem;
  font-size: 2.1rem;
}

.approval-grid {
  display: grid;
  grid-template-columns: 0.82fr 1.18fr;
  gap: 1rem;
}

.queue-list,
.review-panel {
  border-radius: 28px;
  padding: 1.25rem;
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
}

.queue-stack {
  display: grid;
  gap: 0.85rem;
}

.queue-row {
  border: 1px solid rgba(23, 22, 26, 0.08);
  background: rgba(255, 252, 247, 0.72);
  border-radius: 22px;
  padding: 1rem;
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: center;
  text-align: left;
}

.queue-row.active {
  border-color: rgba(180, 104, 60, 0.42);
  transform: translateX(3px);
}

.queue-row span {
  display: block;
  margin-top: 0.3rem;
  color: var(--ink-soft);
}

.review-content {
  display: grid;
  gap: 1.2rem;
}

.detail-matrix {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.9rem;
}

.detail-matrix article {
  padding: 1rem;
  border-radius: 20px;
  background: rgba(255, 252, 247, 0.72);
}

.detail-matrix span {
  display: block;
  color: var(--ink-soft);
  font-size: 0.74rem;
  text-transform: uppercase;
  letter-spacing: 0.14em;
}

.detail-matrix strong {
  display: block;
  margin-top: 0.35rem;
  font-size: 1.1rem;
}

.notes p {
  margin: 0.55rem 0 0;
  color: var(--ink-soft);
  line-height: 1.8;
}

.review-actions {
  display: flex;
  gap: 0.8rem;
  flex-wrap: wrap;
}

@media (max-width: 980px) {
  .approval-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .approval-head {
    flex-direction: column;
    align-items: start;
  }

  .detail-matrix {
    grid-template-columns: 1fr;
  }
}
</style>
