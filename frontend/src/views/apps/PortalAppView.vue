<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink, useRoute } from 'vue-router'

import { useAuthStore } from '@/stores/auth'
import { usePortalStore } from '@/stores/portal'

const route = useRoute()
const auth = useAuthStore()
const portal = usePortalStore()

const appKey = computed(() => String(route.params.appKey ?? ''))
const app = computed(() => portal.appByKey(appKey.value))

const demoModules = computed(() => {
  if (appKey.value === 'oa') {
    return [
      { title: '统一待办', value: '8', detail: '合同审批、请假申请、采购流转集中处理。' },
      { title: '通知公告', value: '12', detail: '企业制度、部门通知和会议安排统一发布。' },
      { title: '流程申请', value: '5', detail: '常用申请表单已接入统一身份。' },
    ]
  }

  if (appKey.value === 'warehouse') {
    return [
      { title: '库存品类', value: '126', detail: '原料、成品、办公物资库存统一查询。' },
      { title: '入库待验', value: '7', detail: '到货单据进入质检和仓库确认流程。' },
      { title: '异常预警', value: '3', detail: '低库存和超期未处理调拨自动提醒。' },
    ]
  }

  if (appKey.value === 'finance') {
    return [
      { title: '待审报销', value: '6', detail: '差旅、采购和部门费用报销集中处理。' },
      { title: '预算执行', value: '86%', detail: '按部门查看预算使用率和付款计划。' },
      { title: '付款申请', value: '4', detail: '业务系统发起的付款单据免密流转。' },
    ]
  }

  return [
    { title: '个人台账', value: '启用', detail: '员工录入和维护自己的业绩记录。' },
    { title: '审批流', value: auth.canAccessApprovals ? '可处理' : '无待办', detail: '经理角色可免密进入审批队列。' },
    { title: '统计看板', value: '实时', detail: '按个人、部门、全局权限查看业绩数据。' },
  ]
})

const activity = computed(() => {
  if (appKey.value === 'oa') {
    return ['市场部提交用章申请', '行政部发布会议室调整通知', '你的差旅申请等待直属经理审批']
  }
  if (appKey.value === 'warehouse') {
    return ['华东仓完成 18 件物料入库', '办公用品库存低于安全线', '北区调拨单等待仓库确认']
  }
  if (appKey.value === 'finance') {
    return ['差旅报销单进入财务复核', '销售部本月预算执行率更新', '供应商付款申请已生成凭证']
  }
  return ['林初提交续费回款记录', '苏南区经理完成大项目成交审批', '系统刷新本月业绩排行榜']
})
</script>

<template>
  <div v-if="app" class="system-page">
    <section class="system-hero fade-rise" :style="{ '--app-accent': app.accent }">
      <div class="system-icon">{{ app.iconLabel }}</div>
      <div>
        <span class="eyebrow">SSO Application</span>
        <h2>{{ app.name }}</h2>
        <p>{{ app.description }}</p>
      </div>
      <RouterLink class="button button-secondary hero-back" :to="{ name: 'overview' }">返回门户</RouterLink>
    </section>

    <section class="session-band surface fade-rise" style="animation-delay: 80ms">
      <div>
        <span>当前用户</span>
        <strong>{{ auth.user?.displayName }}</strong>
      </div>
      <div>
        <span>所属部门</span>
        <strong>{{ auth.user?.department }}</strong>
      </div>
      <div>
        <span>登录状态</span>
        <strong>统一认证已通过</strong>
      </div>
      <div>
        <span>跳转方式</span>
        <strong>免密进入</strong>
      </div>
    </section>

    <section class="module-grid">
      <article
        v-for="(item, index) in demoModules"
        :key="item.title"
        class="module-card fade-rise"
        :style="{ '--app-accent': app.accent, animationDelay: `${130 + index * 70}ms` }"
      >
        <span>{{ item.title }}</span>
        <strong>{{ item.value }}</strong>
        <p>{{ item.detail }}</p>
      </article>
    </section>

    <section v-if="appKey === 'performance'" class="deep-links surface fade-rise" style="animation-delay: 280ms">
      <div>
        <span class="eyebrow">Real Business Demo</span>
        <h3>进入真实业绩功能</h3>
        <p class="muted">下面两个入口复用现有后端接口和 JWT 权限，不需要再次输入密码。</p>
      </div>
      <div class="deep-actions">
        <RouterLink v-if="auth.canAccessRecordsPage" class="button button-primary" :to="{ name: 'records' }">
          个人业绩台账
        </RouterLink>
        <RouterLink v-if="auth.canAccessApprovals" class="button button-secondary" :to="{ name: 'approvals' }">
          部门审批队列
        </RouterLink>
      </div>
    </section>

    <section class="activity-panel surface fade-rise" style="animation-delay: 330ms">
      <div class="panel-head">
        <div>
          <span class="eyebrow">System Activity</span>
          <h3>系统动态</h3>
        </div>
        <span class="muted">示例数据</span>
      </div>
      <div class="activity-list">
        <article v-for="item in activity" :key="item">
          <span></span>
          <p>{{ item }}</p>
        </article>
      </div>
    </section>
  </div>
</template>

<style scoped>
.system-page {
  display: grid;
  gap: 1rem;
}

.system-hero {
  min-height: 220px;
  border-radius: 30px;
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
  font-weight: 800;
  font-size: 1.18rem;
}

.system-hero .eyebrow,
.system-hero p {
  color: rgba(255, 248, 242, 0.72);
}

.system-hero h2 {
  margin: 0.4rem 0 0;
  font-size: clamp(2rem, 4vw, 4rem);
  line-height: 1;
  letter-spacing: -0.06em;
}

.system-hero p {
  margin: 0.9rem 0 0;
  max-width: 42rem;
  line-height: 1.75;
}

.hero-back {
  color: #fff8f2;
  border-color: rgba(255, 255, 255, 0.3);
  white-space: nowrap;
}

.session-band {
  border-radius: 24px;
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

.deep-links,
.activity-panel {
  border-radius: 28px;
  padding: 1.25rem;
}

.deep-links {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: center;
}

.deep-links h3,
.activity-panel h3 {
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

.panel-head {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: start;
  margin-bottom: 1rem;
}

.activity-list {
  display: grid;
  gap: 0.8rem;
}

.activity-list article {
  display: grid;
  grid-template-columns: 12px minmax(0, 1fr);
  gap: 0.8rem;
  align-items: start;
  padding-top: 0.8rem;
  border-top: 1px solid rgba(23, 22, 26, 0.08);
}

.activity-list article:first-child {
  border-top: none;
  padding-top: 0;
}

.activity-list span {
  width: 10px;
  height: 10px;
  margin-top: 0.45rem;
  border-radius: 50%;
  background: var(--accent);
}

.activity-list p {
  margin: 0;
  color: var(--ink-soft);
  line-height: 1.7;
}

@media (max-width: 920px) {
  .system-hero,
  .session-band,
  .module-grid {
    grid-template-columns: 1fr;
  }

  .deep-links {
    flex-direction: column;
    align-items: flex-start;
  }

  .hero-back {
    justify-self: start;
  }
}
</style>
