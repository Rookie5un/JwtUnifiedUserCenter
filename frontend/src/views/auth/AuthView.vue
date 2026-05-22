<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'

import { api } from '@/api/service'
import { useAuthStore } from '@/stores/auth'
import type { Department } from '@/types'

const auth = useAuthStore()
const router = useRouter()

const mode = ref<'login' | 'register'>('login')
const error = ref('')
const departments = ref<Department[]>([])

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

async function loadDepartments() {
  try {
    const loaded = await api.publicDepartments()
    departments.value = loaded
    if (loaded.length && !loaded.some((department) => department.name === registerForm.department)) {
      registerForm.department = loaded[0].name
    }
  } catch {
    departments.value = []
  }
}

onMounted(loadDepartments)
</script>

<template>
  <div class="auth-page page-shell">
    <main class="auth-stage">
      <section class="login-card surface fade-rise">
        <div class="login-head">
          <div>
            <span class="eyebrow">Unified User Center</span>
            <h1>统一用户中心</h1>
            <p>登录后进入集成门户，免密访问已授权业务系统。</p>
          </div>
          <div class="secure-mark">SSO</div>
        </div>

        <div class="toggle" aria-label="认证方式">
          <button type="button" :class="{ active: mode === 'login' }" @click="mode = 'login'">登录</button>
          <button type="button" :class="{ active: mode === 'register' }" @click="mode = 'register'">注册</button>
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
              <input v-model="registerForm.username" autocomplete="username" placeholder="选择一个账号名" />
            </div>
            <div class="field two-up">
              <div class="field">
                <label>姓名</label>
                <input v-model="registerForm.displayName" placeholder="例如 林初" />
              </div>
              <div class="field">
                <label>部门</label>
                <select v-if="departments.length" v-model="registerForm.department">
                  <option v-for="department in departments" :key="department.id" :value="department.name">
                    {{ department.name }}
                  </option>
                </select>
                <input v-else v-model="registerForm.department" placeholder="例如 East Sales" />
              </div>
            </div>
            <div class="field two-up">
              <div class="field">
                <label>邮箱</label>
                <input v-model="registerForm.email" type="email" placeholder="name@company.com" />
              </div>
              <div class="field">
                <label>手机号</label>
                <input v-model="registerForm.phone" placeholder="13800000000" />
              </div>
            </div>
            <div class="field">
              <label>密码</label>
              <input
                v-model="registerForm.password"
                type="password"
                autocomplete="new-password"
                placeholder="至少 8 位"
              />
            </div>
          </template>

          <p v-if="error" class="error-copy">{{ error }}</p>

          <button class="button button-primary submit" :disabled="auth.loading">
            {{ auth.loading ? '处理中...' : mode === 'login' ? '登录并进入门户' : '创建账户' }}
          </button>
        </form>

        <div class="credential-panel">
          <span>演示账号</span>
          <div class="credential-list">
            <button
              v-for="item in credentials"
              :key="item.role"
              type="button"
              class="credential-chip"
              @click="fillCredential(item.username, item.password)"
            >
              <strong>{{ item.role }}</strong>
              <small>{{ item.username }}</small>
            </button>
          </div>
        </div>
      </section>
    </main>
  </div>
</template>

<style scoped>
.auth-page {
  min-height: 100vh;
  position: relative;
  overflow: hidden;
  background:
    linear-gradient(120deg, rgba(18, 20, 25, 0.72), rgba(18, 20, 25, 0.28)),
    url("https://images.unsplash.com/photo-1497366754035-f200968a6e72?auto=format&fit=crop&w=2200&q=80") center/cover;
}

.auth-page::before {
  content: "";
  position: absolute;
  inset: 0;
  background:
    linear-gradient(rgba(255, 255, 255, 0.045) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.045) 1px, transparent 1px);
  background-size: 56px 56px;
  mask-image: linear-gradient(to bottom, rgba(0, 0, 0, 0.82), transparent 92%);
}

.auth-stage {
  position: relative;
  z-index: 1;
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 2rem;
}

.login-card {
  width: min(100%, 520px);
  border-radius: 28px;
  padding: clamp(1.2rem, 3vw, 1.8rem);
  background: rgba(255, 252, 247, 0.88);
  border-color: rgba(255, 255, 255, 0.68);
}

.login-head {
  display: flex;
  justify-content: space-between;
  gap: 1.2rem;
  align-items: start;
}

.login-head h1 {
  margin: 0.35rem 0 0;
  font-size: clamp(2rem, 5vw, 3rem);
  letter-spacing: -0.05em;
}

.login-head p {
  margin: 0.75rem 0 0;
  color: var(--ink-soft);
  line-height: 1.7;
}

.secure-mark {
  width: 58px;
  height: 58px;
  border-radius: 18px;
  display: grid;
  place-items: center;
  flex: 0 0 auto;
  color: #fff8f2;
  background: var(--ink);
  font-size: 0.88rem;
  font-weight: 700;
  letter-spacing: 0.1em;
  box-shadow: 0 18px 40px rgba(23, 22, 26, 0.18);
}

.toggle {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.4rem;
  margin: 1.6rem 0 1.2rem;
  padding: 0.35rem;
  border-radius: 18px;
  background: rgba(23, 22, 26, 0.06);
}

.toggle button {
  border: none;
  border-radius: 14px;
  background: transparent;
  color: var(--ink-soft);
  padding: 0.78rem 1rem;
  transition:
    background-color 180ms ease,
    color 180ms ease,
    transform 180ms ease;
}

.toggle button:hover {
  transform: translateY(-1px);
}

.toggle button.active {
  background: var(--ink);
  color: #fff8f2;
}

.auth-form {
  display: grid;
  gap: 1rem;
}

.two-up {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1rem;
}

.submit {
  width: 100%;
  margin-top: 0.25rem;
}

.error-copy {
  margin: 0;
  color: var(--danger);
}

.credential-panel {
  margin-top: 1.25rem;
  padding-top: 1rem;
  border-top: 1px solid rgba(23, 22, 26, 0.09);
  display: grid;
  gap: 0.75rem;
}

.credential-panel > span {
  color: var(--ink-soft);
  font-size: 0.82rem;
}

.credential-list {
  display: flex;
  flex-wrap: wrap;
  gap: 0.55rem;
}

.credential-chip {
  border: 1px solid rgba(23, 22, 26, 0.1);
  border-radius: 999px;
  background: rgba(255, 252, 247, 0.66);
  padding: 0.62rem 0.8rem;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: var(--ink);
  transition:
    transform 180ms ease,
    border-color 180ms ease,
    background-color 180ms ease;
}

.credential-chip:hover {
  transform: translateY(-1px);
  border-color: rgba(180, 104, 60, 0.34);
  background: rgba(255, 252, 247, 0.94);
}

.credential-chip small {
  color: var(--ink-soft);
}

@media (max-width: 640px) {
  .auth-stage {
    padding: 1rem;
    place-items: stretch;
    align-content: center;
  }

  .login-card {
    border-radius: 24px;
  }

  .login-head {
    align-items: center;
  }

  .secure-mark {
    width: 50px;
    height: 50px;
    border-radius: 16px;
  }

  .two-up {
    grid-template-columns: 1fr;
  }
}
</style>
