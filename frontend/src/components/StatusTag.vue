<script setup lang="ts">
import { computed } from 'vue'

import { statusLabel } from '@/composables/format'

const props = defineProps<{
  status: string
}>()

const className = computed(() => {
  if (props.status === 'APPROVED' || props.status === 'ACTIVE' || props.status === 'SUCCESS') return 'ok'
  if (props.status === 'REJECTED' || props.status === 'DISABLED' || props.status === 'FAILURE') return 'danger'
  return 'warn'
})
</script>

<template>
  <span class="status-tag" :class="className">
    {{ statusLabel(status) }}
  </span>
</template>

<style scoped>
.status-tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.45rem;
  padding: 0.42rem 0.72rem;
  border-radius: 999px;
  font-size: 0.76rem;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  border: 1px solid currentColor;
}

.ok {
  color: var(--success);
  background: rgba(39, 93, 73, 0.08);
}

.warn {
  color: var(--warn);
  background: rgba(169, 102, 45, 0.08);
}

.danger {
  color: var(--danger);
  background: rgba(143, 61, 51, 0.08);
}
</style>
