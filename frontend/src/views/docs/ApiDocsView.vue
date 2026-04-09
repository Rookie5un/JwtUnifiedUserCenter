<script setup lang="ts">
import { computed } from 'vue'

import { resolveApiUrl } from '@/api/client'

const swaggerUiUrl = computed(() => resolveApiUrl('/swagger-ui/index.html'))
const openApiUrl = computed(() => resolveApiUrl('/api-docs'))
</script>

<template>
  <div class="docs-page">
    <section class="surface docs-head fade-rise">
      <div>
        <span class="eyebrow">API Surface</span>
        <h2 class="section-title">动态接口文档</h2>
        <p class="muted">
          基于 Springdoc OpenAPI 与 Swagger UI，接口清单、请求参数、JWT 鉴权说明和返回结构会跟随后端定义实时更新，适合联调、演示和论文截图。
        </p>
      </div>

      <div class="docs-actions">
        <a class="button button-primary" :href="swaggerUiUrl" target="_blank" rel="noreferrer">
          新窗口打开 Swagger
        </a>
        <a class="button button-secondary" :href="openApiUrl" target="_blank" rel="noreferrer">
          查看 OpenAPI JSON
        </a>
      </div>
    </section>

    <section class="surface docs-facts fade-rise" style="animation-delay: 80ms">
      <div>
        <span>生成方式</span>
        <strong>动态生成</strong>
        <p>控制器、DTO 和注解变化后，文档会自动同步。</p>
      </div>
      <div>
        <span>鉴权说明</span>
        <strong>JWT Bearer</strong>
        <p>支持直接在 Swagger UI 里填写令牌进行调试。</p>
      </div>
      <div>
        <span>输出形式</span>
        <strong>UI + JSON</strong>
        <p>既能交互式查看，也能导出 OpenAPI 规范做集成。</p>
      </div>
    </section>

    <section class="surface docs-stage fade-rise" style="animation-delay: 160ms">
      <div class="docs-stage-head">
        <div>
          <span class="eyebrow">Live Preview</span>
          <h3>内嵌 Swagger UI</h3>
        </div>
        <small class="muted">如果浏览器阻止内嵌，可以直接点击上方按钮在新窗口查看。</small>
      </div>

      <div class="docs-frame-shell">
        <iframe :src="swaggerUiUrl" title="Swagger UI API Docs" loading="lazy"></iframe>
      </div>
    </section>
  </div>
</template>

<style scoped>
.docs-page {
  display: grid;
  gap: 1rem;
}

.docs-head,
.docs-facts,
.docs-stage {
  border-radius: 30px;
  padding: 1.35rem 1.4rem;
}

.docs-head {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: end;
}

.docs-head p {
  max-width: 52rem;
  line-height: 1.8;
  margin: 0.9rem 0 0;
}

.docs-actions {
  display: inline-flex;
  flex-wrap: wrap;
  gap: 0.65rem;
}

.docs-facts {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 1rem;
}

.docs-facts div {
  padding-right: 1rem;
  border-right: 1px solid var(--line);
}

.docs-facts div:last-child {
  border-right: none;
  padding-right: 0;
}

.docs-facts span {
  display: block;
  font-size: 0.74rem;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: var(--ink-soft);
}

.docs-facts strong {
  display: block;
  margin-top: 0.4rem;
  font-size: 1.2rem;
}

.docs-facts p {
  margin: 0.55rem 0 0;
  color: var(--ink-soft);
  line-height: 1.7;
}

.docs-stage {
  display: grid;
  gap: 1rem;
}

.docs-stage-head {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: end;
}

.docs-stage-head h3 {
  margin: 0.35rem 0 0;
  font-size: 1.45rem;
}

.docs-frame-shell {
  min-height: 72vh;
  border-radius: 24px;
  overflow: hidden;
  border: 1px solid rgba(23, 22, 26, 0.08);
  background: rgba(255, 255, 255, 0.72);
}

.docs-frame-shell iframe {
  width: 100%;
  min-height: 72vh;
  border: none;
  display: block;
  background: #ffffff;
}

@media (max-width: 980px) {
  .docs-head,
  .docs-stage-head {
    flex-direction: column;
    align-items: flex-start;
  }

  .docs-facts {
    grid-template-columns: 1fr;
  }

  .docs-facts div {
    padding-right: 0;
    padding-bottom: 1rem;
    border-right: none;
    border-bottom: 1px solid var(--line);
  }

  .docs-facts div:last-child {
    padding-bottom: 0;
    border-bottom: none;
  }
}

@media (max-width: 720px) {
  .docs-frame-shell,
  .docs-frame-shell iframe {
    min-height: 62vh;
  }
}
</style>
