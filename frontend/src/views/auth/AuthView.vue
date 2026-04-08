<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'

import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()

const mode = ref<'login' | 'register'>('login')
const error = ref('')

const loginForm = reactive({
  username: 'admin',
  password: 'Admin@123',
})

const registerForm = reactive({
  username: '',
  password: '',
  displayName: '',
  department: 'East Sales',
  email: '',
  phone: '',
})

const credentials = [
  { role: '管理员', username: 'admin', password: 'Admin@123' },
  { role: '部门经理', username: 'manager', password: 'Manager@123' },
  { role: '普通员工', username: 'employee', password: 'Employee@123' },
]

async function submit() {
  error.value = ''
  try {
    if (mode.value === 'login') {
      await auth.login(loginForm)
      router.replace({ name: 'overview' })
      return
    }
    await auth.register(registerForm)
    loginForm.username = registerForm.username
    loginForm.password = registerForm.password
    mode.value = 'login'
  } catch (err) {
    error.value = err instanceof Error ? err.message : '请求失败，请稍后再试。'
  }
}

function fillCredential(username: string, password: string) {
  mode.value = 'login'
  loginForm.username = username
  loginForm.password = password
}
</script>

<template>
  <div class="auth-page page-shell">
    <section class="poster">
      <div class="poster-inner fade-rise">
        <span class="eyebrow">Atlas ID Workspace</span>
        <h1 class="headline">让认证、权限与业务验证落在一处。</h1>
        <p class="poster-copy">
          基于 JWT 与 RBAC 的统一用户中心，把登录、权限治理和业绩审批变成一套真正可运营的工作面。
        </p>

        <div class="poster-strips">
          <div>
            <span>JWT</span>
            <strong>无状态访问链路</strong>
          </div>
          <div>
            <span>RBAC</span>
            <strong>角色与权限统一分发</strong>
          </div>
          <div>
            <span>Scenario</span>
            <strong>业绩管理与审批验证</strong>
          </div>
        </div>

        <div class="credentials surface fade-rise" style="animation-delay: 140ms">
          <div class="credentials-header">
            <span class="eyebrow">Quick Entry</span>
            <strong>演示账号</strong>
          </div>
          <button
            v-for="item in credentials"
            :key="item.role"
            class="credential-row"
            @click="fillCredential(item.username, item.password)"
          >
            <span>{{ item.role }}</span>
            <strong>{{ item.username }}</strong>
            <small>{{ item.password }}</small>
          </button>
        </div>
      </div>
    </section>

    <section class="auth-panel fade-rise" style="animation-delay: 120ms">
      <div class="panel-head">
        <span class="eyebrow">Secure Access</span>
        <div class="toggle">
          <button :class="{ active: mode === 'login' }" @click="mode = 'login'">登录</button>
          <button :class="{ active: mode === 'register' }" @click="mode = 'register'">注册</button>
        </div>
      </div>

      <form class="auth-form" @submit.prevent="submit">
        <template v-if="mode === 'login'">
          <div class="field">
            <label>用户名</label>
            <input v-model="loginForm.username" autocomplete="username" placeholder="admin" />
          </div>
          <div class="field">
            <label>密码</label>
            <input
              v-model="loginForm.password"
              type="password"
              autocomplete="current-password"
              placeholder="输入登录密码"
            />
          </div>
        </template>

        <template v-else>
          <div class="field">
            <label>用户名</label>
            <input v-model="registerForm.username" placeholder="选择一个账号名" />
          </div>
          <div class="field two-up">
            <div class="field">
              <label>姓名</label>
              <input v-model="registerForm.displayName" placeholder="例如 林初" />
            </div>
            <div class="field">
              <label>部门</label>
              <input v-model="registerForm.department" placeholder="例如 East Sales" />
            </div>
          </div>
          <div class="field">
            <label>邮箱</label>
            <input v-model="registerForm.email" type="email" placeholder="name@company.com" />
          </div>
          <div class="field">
            <label>手机号</label>
            <input v-model="registerForm.phone" placeholder="13800000000" />
          </div>
          <div class="field">
            <label>密码</label>
            <input v-model="registerForm.password" type="password" placeholder="至少 8 位" />
          </div>
        </template>

        <p v-if="error" class="error-copy">{{ error }}</p>

        <button class="button button-primary submit" :disabled="auth.loading">
          {{ auth.loading ? '处理中...' : mode === 'login' ? '进入工作台' : '创建账户' }}
        </button>
      </form>
    </section>
  </div>
</template>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 1.15fr minmax(360px, 0.85fr);
}

.poster {
  position: relative;
  overflow: hidden;
  padding: 2rem;
  display: flex;
  align-items: stretch;
}

.poster::before,
.poster::after {
  content: "";
  position: absolute;
  border-radius: 999px;
  filter: blur(6px);
}

.poster::before {
  width: 420px;
  height: 420px;
  right: -90px;
  top: 10%;
  background: radial-gradient(circle, rgba(180, 104, 60, 0.18), transparent 66%);
}

.poster::after {
  width: 320px;
  height: 320px;
  left: -120px;
  bottom: -80px;
  background: radial-gradient(circle, rgba(23, 22, 26, 0.14), transparent 72%);
}

.poster-inner {
  position: relative;
  z-index: 1;
  width: 100%;
  border-radius: 36px;
  padding: clamp(1.6rem, 3vw, 3rem);
  display: grid;
  align-content: space-between;
  gap: 2rem;
  background:
    linear-gradient(140deg, rgba(255, 252, 247, 0.64), rgba(255, 252, 247, 0.16)),
    linear-gradient(180deg, #f3ede4 0%, #e9e1d3 100%);
  box-shadow: 0 42px 90px rgba(36, 30, 21, 0.14);
}

.poster-copy {
  max-width: 38rem;
  font-size: 1.08rem;
  line-height: 1.8;
  color: var(--ink-soft);
  margin: 1.4rem 0 0;
}

.poster-strips {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 1rem;
  margin-top: 1rem;
}

.poster-strips div {
  padding-top: 1rem;
  border-top: 1px solid var(--line);
  display: grid;
  gap: 0.35rem;
}

.poster-strips span {
  font-size: 0.74rem;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: var(--ink-soft);
}

.poster-strips strong {
  font-size: 1.03rem;
  line-height: 1.4;
}

.credentials {
  border-radius: 30px;
  padding: 1rem;
  display: grid;
  gap: 0.65rem;
  max-width: 520px;
}

.credentials-header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
}

.credential-row {
  display: grid;
  grid-template-columns: 72px 1fr auto;
  gap: 1rem;
  align-items: center;
  padding: 0.9rem 1rem;
  border-radius: 22px;
  background: rgba(255, 252, 247, 0.58);
  border: 1px solid rgba(23, 22, 26, 0.06);
}

.credential-row span {
  color: var(--ink-soft);
}

.credential-row small {
  color: var(--ink-soft);
}

.auth-panel {
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 2rem;
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.toggle {
  display: inline-flex;
  background: rgba(255, 252, 247, 0.7);
  padding: 0.3rem;
  border-radius: 999px;
  border: 1px solid var(--line);
}

.toggle button {
  border: none;
  background: transparent;
  color: var(--ink-soft);
  padding: 0.75rem 1rem;
  border-radius: 999px;
}

.toggle button.active {
  background: var(--ink);
  color: #fdf8f2;
}

.auth-form {
  max-width: 440px;
  background: rgba(255, 252, 247, 0.58);
  border: 1px solid rgba(255, 255, 255, 0.72);
  box-shadow: var(--shadow);
  border-radius: 32px;
  padding: 1.4rem;
  display: grid;
  gap: 1rem;
  backdrop-filter: blur(22px);
}

.two-up {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1rem;
}

.submit {
  margin-top: 0.4rem;
}

.error-copy {
  margin: 0;
  color: var(--danger);
}

@media (max-width: 980px) {
  .auth-page {
    grid-template-columns: 1fr;
  }

  .poster-strips {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .poster,
  .auth-panel {
    padding: 1rem;
  }

  .two-up {
    grid-template-columns: 1fr;
  }
}
</style>
