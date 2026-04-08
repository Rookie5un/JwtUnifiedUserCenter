<script setup lang="ts">
const props = withDefaults(defineProps<{
  open: boolean
  title: string
  confirmText?: string
  cancelText?: string
  tone?: 'primary' | 'danger'
  width?: 'default' | 'wide'
}>(), {
  confirmText: '',
  cancelText: '取消',
  tone: 'primary',
  width: 'default',
})

const emit = defineEmits<{
  close: []
  confirm: []
}>()
</script>

<template>
  <Teleport to="body">
    <div v-if="open" class="dialog-backdrop" @click.self="emit('close')">
      <section class="dialog-card surface" :class="[width, tone]">
        <div class="dialog-head">
          <h3>{{ title }}</h3>
          <button class="dialog-close" type="button" @click="emit('close')">关闭</button>
        </div>

        <div class="dialog-body">
          <slot />
        </div>

        <div v-if="confirmText || $slots.actions" class="dialog-actions">
          <slot name="actions">
            <button class="button button-secondary" type="button" @click="emit('close')">
              {{ cancelText }}
            </button>
            <button
              class="button"
              :class="tone === 'danger' ? 'button-danger' : 'button-primary'"
              type="button"
              @click="emit('confirm')"
            >
              {{ confirmText }}
            </button>
          </slot>
        </div>
      </section>
    </div>
  </Teleport>
</template>

<style scoped>
.dialog-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(23, 22, 26, 0.38);
  backdrop-filter: blur(10px);
  display: grid;
  place-items: center;
  padding: 1.25rem;
  z-index: 1000;
}

.dialog-card {
  width: min(100%, 34rem);
  border-radius: 28px;
  padding: 1.25rem;
  display: grid;
  gap: 1rem;
}

.dialog-card.wide {
  width: min(100%, 52rem);
}

.dialog-head {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: start;
}

.dialog-head h3 {
  margin: 0;
  font-size: 1.35rem;
}

.dialog-close {
  border: 1px solid var(--line-strong);
  background: transparent;
  border-radius: 999px;
  padding: 0.65rem 0.9rem;
  color: var(--ink-soft);
}

.dialog-body {
  display: grid;
  gap: 0.85rem;
  max-height: min(70vh, 42rem);
  overflow-y: auto;
  padding-right: 0.15rem;
}

.dialog-actions {
  display: flex;
  justify-content: end;
  gap: 0.75rem;
}

.button-danger {
  background: var(--danger);
  color: #fff8f2;
}

@media (max-width: 720px) {
  .dialog-actions {
    flex-direction: column-reverse;
  }

  .dialog-actions .button {
    width: 100%;
  }
}
</style>
