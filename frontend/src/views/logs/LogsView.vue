<script setup lang="ts">
import { onMounted, ref } from 'vue'

import { api } from '@/api/service'
import { formatDateTime } from '@/composables/format'
import type { OperationLog } from '@/types'
import StatusTag from '@/components/StatusTag.vue'

const logs = ref<OperationLog[]>([])
const loading = ref(true)
const error = ref('')
const limit = ref(60)
const limitOptions = [30, 60, 120, 200]

async function loadLogs() {
  loading.value = true
  error.value = ''

  try {
    logs.value = await api.logs(limit.value)
  } catch (err) {
    error.value = err instanceof Error ? err.message : '日志载入失败。'
    logs.value = []
  } finally {
    loading.value = false
  }
}

onMounted(loadLogs)
</script>

<template>
  <div class="logs-page">
    <section class="surface logs-head fade-rise">
      <div>
        <span class="eyebrow">Audit Trail</span>
        <h2 class="section-title">操作日志</h2>
        <p class="muted">集中查看登录、权限调整、角色配置和业务操作记录，方便演示系统留痕能力与日常审计。</p>
      </div>

      <div class="logs-controls">
        <label class="field limit-field">
          <span>最近条数</span>
          <select v-model.number="limit" @change="loadLogs">
            <option v-for="value in limitOptions" :key="value" :value="value">{{ value }} 条</option>
          </select>
        </label>
        <button class="button button-secondary" @click="loadLogs">刷新日志</button>
      </div>
    </section>

    <p v-if="error" class="error-copy">{{ error }}</p>

    <section v-if="loading" class="surface logs-empty fade-rise" style="animation-delay: 80ms">
      <span class="eyebrow">Loading</span>
      <h3>正在同步最近操作</h3>
      <p class="muted">系统会按时间倒序拉取最新日志。</p>
    </section>

    <section v-else-if="logs.length" class="surface logs-stream fade-rise" style="animation-delay: 80ms">
      <div class="stream-head">
        <div>
          <span class="eyebrow">Recent Activity</span>
          <h3>最近 {{ logs.length }} 条日志</h3>
        </div>
        <small class="muted">默认只展示最新记录，便于快速定位关键操作。</small>
      </div>

      <article v-for="log in logs" :key="log.id" class="log-entry">
        <div class="entry-marker"></div>
        <div class="entry-main">
          <div class="entry-head">
            <div>
              <strong>{{ log.action }}</strong>
              <span>
                {{ log.actorUsername || 'system' }} · {{ log.resourceType }}
                <template v-if="log.resourceId"> · #{{ log.resourceId }}</template>
              </span>
            </div>
            <StatusTag :status="log.result" />
          </div>

          <p>{{ log.detail || '无补充说明' }}</p>
          <small>{{ formatDateTime(log.createdAt) }}</small>
        </div>
      </article>
    </section>

    <section v-else class="surface logs-empty fade-rise" style="animation-delay: 80ms">
      <span class="eyebrow">No Data</span>
      <h3>当前没有可显示的日志</h3>
      <p class="muted">可以稍后刷新，或者先执行一次登录、录入或权限配置操作再回来查看。</p>
    </section>
  </div>
</template>

<style scoped>
.logs-page {
  display: grid;
  gap: 1rem;
}

.logs-head,
.logs-stream,
.logs-empty {
  border-radius: 30px;
  padding: 1.35rem 1.4rem;
}

.logs-head {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: end;
}

.logs-head p {
  max-width: 44rem;
  margin: 0.9rem 0 0;
  line-height: 1.75;
}

.logs-controls {
  display: flex;
  gap: 0.75rem;
  align-items: end;
  flex-wrap: wrap;
}

.limit-field {
  min-width: 140px;
}

.limit-field span {
  font-size: 0.78rem;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: var(--ink-soft);
}

.stream-head {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: end;
  margin-bottom: 1.15rem;
}

.stream-head h3,
.logs-empty h3 {
  margin: 0.35rem 0 0;
  font-size: 1.45rem;
}

.log-entry {
  display: grid;
  grid-template-columns: 16px minmax(0, 1fr);
  gap: 1rem;
  padding: 1rem 0;
  border-top: 1px solid rgba(23, 22, 26, 0.08);
}

.log-entry:first-of-type {
  border-top: none;
  padding-top: 0;
}

.entry-marker {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  margin-top: 0.45rem;
  background: radial-gradient(circle, rgba(180, 104, 60, 0.95), rgba(180, 104, 60, 0.24));
  box-shadow: 0 0 0 6px rgba(180, 104, 60, 0.12);
}

.entry-main {
  display: grid;
  gap: 0.55rem;
}

.entry-head {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: start;
}

.entry-head span {
  display: block;
  margin-top: 0.28rem;
  color: var(--ink-soft);
}

.entry-main p,
.logs-empty p {
  margin: 0;
  color: var(--ink-soft);
  line-height: 1.75;
}

.entry-main small {
  color: var(--ink-soft);
}

@media (max-width: 980px) {
  .logs-head,
  .stream-head,
  .entry-head {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
