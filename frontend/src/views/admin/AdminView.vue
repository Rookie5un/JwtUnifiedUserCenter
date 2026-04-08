<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'

import { api } from '@/api/service'
import { formatDateTime, roleLabel } from '@/composables/format'
import { useAuthStore } from '@/stores/auth'
import type { OperationLog, Permission, Role, User } from '@/types'
import AppDialog from '@/components/AppDialog.vue'
import StatusTag from '@/components/StatusTag.vue'

const auth = useAuthStore()
const router = useRouter()
const tab = ref<'users' | 'roles' | 'permissions' | 'logs'>('users')
const loading = ref(true)
const error = ref('')

const users = ref<User[]>([])
const roles = ref<Role[]>([])
const permissions = ref<Permission[]>([])
const logs = ref<OperationLog[]>([])

const selectedUserId = ref<number | null>(null)
const selectedRoleId = ref<number | null>(null)
const selectedPermissionId = ref<number | null>(null)
const permissionGuideOpen = ref(false)
const statusConfirmOpen = ref(false)
const roleDeleteConfirmOpen = ref(false)
const permissionDeleteConfirmOpen = ref(false)

const userForm = reactive({
  displayName: '',
  department: '',
  email: '',
  phone: '',
  roleIds: [] as number[],
  newPassword: '',
})

const roleForm = reactive({
  code: '',
  name: '',
  description: '',
  permissionIds: [] as number[],
})

const permissionForm = reactive({
  code: '',
  name: '',
  resource: '',
  action: '',
  type: 'API' as Permission['type'],
  description: '',
})

const selectedUser = computed(() => users.value.find((item) => item.id === selectedUserId.value) ?? null)
const selectedRole = computed(() => roles.value.find((item) => item.id === selectedRoleId.value) ?? null)
const selectedPermission = computed(() => permissions.value.find((item) => item.id === selectedPermissionId.value) ?? null)
const permissionRules = [
  {
    title: '编码规则',
    detail: 'code 建议使用全大写下划线，表达“资源 + 能力”，例如 PERFORMANCE_CREATE、USER_MANAGE。',
  },
  {
    title: '资源字段',
    detail: 'resource 填资源归属，如 performance、user、role、permission、logs。',
  },
  {
    title: '动作字段',
    detail: 'action 填动作和范围，如 create、read:self、read:department、manage、approve。',
  },
  {
    title: '类型选择',
    detail: 'MENU 控制板块可见，BUTTON 控制具体操作，API 表示接口或数据访问能力。',
  },
]
const permissionExamples = [
  {
    code: 'PERFORMANCE_CREATE',
    resource: 'performance',
    action: 'create',
    type: 'BUTTON',
    note: '录入新业绩',
  },
  {
    code: 'PERFORMANCE_VIEW_DEPARTMENT',
    resource: 'performance',
    action: 'read:department',
    type: 'MENU',
    note: '查看部门业绩和审批范围',
  },
  {
    code: 'USER_MANAGE',
    resource: 'user',
    action: 'manage',
    type: 'MENU',
    note: '用户管理、分配角色、重置密码',
  },
]
const availableTabs = computed(() =>
  [
    { key: 'users' as const, label: '用户', visible: auth.canManageUsers },
    { key: 'roles' as const, label: '角色', visible: auth.canManageRoles },
    { key: 'permissions' as const, label: '权限', visible: auth.canManagePermissions },
    { key: 'logs' as const, label: '日志', visible: auth.canViewLogs },
  ].filter((item) => item.visible),
)
const canLoadRoles = computed(() => auth.canManageUsers || auth.canManageRoles)
const canLoadPermissions = computed(() => auth.canManageRoles || auth.canManagePermissions)

function syncActiveTab() {
  if (!availableTabs.value.some((item) => item.key === tab.value)) {
    tab.value = availableTabs.value[0]?.key ?? 'users'
  }
}

async function syncCurrentSession() {
  try {
    await auth.refreshProfile()
  } catch {
    await auth.logout()
    await router.replace({ name: 'login' })
    return false
  }

  if (!auth.canAccessAdminConsole) {
    await router.replace({ name: 'overview' })
    return false
  }

  return true
}

async function loadAll() {
  loading.value = true
  error.value = ''
  try {
    if (!auth.canAccessAdminConsole) {
      users.value = []
      roles.value = []
      permissions.value = []
      logs.value = []
      return
    }
    const [loadedUsers, loadedRoles, loadedPermissions, loadedLogs] = await Promise.all([
      auth.canManageUsers ? api.users() : Promise.resolve([] as User[]),
      canLoadRoles.value ? api.roles() : Promise.resolve([] as Role[]),
      canLoadPermissions.value ? api.permissions() : Promise.resolve([] as Permission[]),
      auth.canViewLogs ? api.logs() : Promise.resolve([] as OperationLog[]),
    ])
    users.value = loadedUsers
    roles.value = loadedRoles
    permissions.value = loadedPermissions
    logs.value = loadedLogs

    const matchedUser = loadedUsers.find((item) => item.id === selectedUserId.value)
    const matchedRole = loadedRoles.find((item) => item.id === selectedRoleId.value)
    const matchedPermission = loadedPermissions.find((item) => item.id === selectedPermissionId.value)

    if (auth.canManageUsers) {
      if (matchedUser) hydrateUser(matchedUser)
      else if (!selectedUserId.value && loadedUsers[0]) hydrateUser(loadedUsers[0])
    }

    if (auth.canManageRoles) {
      if (matchedRole) hydrateRole(matchedRole)
      else if (!selectedRoleId.value && loadedRoles[0]) hydrateRole(loadedRoles[0])
    }

    if (auth.canManagePermissions) {
      if (matchedPermission) hydratePermission(matchedPermission)
      else if (!selectedPermissionId.value && loadedPermissions[0]) hydratePermission(loadedPermissions[0])
    }
  } catch (err) {
    error.value = err instanceof Error ? err.message : '管理数据载入失败。'
  } finally {
    loading.value = false
  }
}

function hydrateUser(user: User) {
  selectedUserId.value = user.id
  userForm.displayName = user.displayName
  userForm.department = user.department
  userForm.email = user.email ?? ''
  userForm.phone = user.phone ?? ''
  userForm.roleIds = roles.value.filter((role) => user.roles.includes(role.code)).map((role) => role.id)
  userForm.newPassword = ''
}

function hydrateRole(role: Role | null) {
  selectedRoleId.value = role?.id ?? null
  roleForm.code = role?.code ?? ''
  roleForm.name = role?.name ?? ''
  roleForm.description = role?.description ?? ''
  roleForm.permissionIds = role
    ? permissions.value.filter((permission) => role.permissions.includes(permission.code)).map((permission) => permission.id)
    : []
}

function hydratePermission(permission: Permission | null) {
  selectedPermissionId.value = permission?.id ?? null
  permissionForm.code = permission?.code ?? ''
  permissionForm.name = permission?.name ?? ''
  permissionForm.resource = permission?.resource ?? ''
  permissionForm.action = permission?.action ?? ''
  permissionForm.type = permission?.type ?? 'API'
  permissionForm.description = permission?.description ?? ''
}

async function saveUser() {
  if (!selectedUser.value) return
  await api.updateUser(selectedUser.value.id, {
    displayName: userForm.displayName,
    department: userForm.department,
    email: userForm.email,
    phone: userForm.phone,
  })
  await api.assignRoles(selectedUser.value.id, userForm.roleIds)
  if (userForm.newPassword.trim()) {
    await api.resetPassword(selectedUser.value.id, userForm.newPassword)
    userForm.newPassword = ''
  }
  if (!(await syncCurrentSession())) return
  await loadAll()
}

async function toggleUserStatus() {
  if (!selectedUser.value) return
  const next = selectedUser.value.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE'
  await api.updateUserStatus(selectedUser.value.id, next)
  if (!(await syncCurrentSession())) return
  await loadAll()
}

async function saveRole() {
  let role: Role
  if (selectedRole.value) {
    role = await api.updateRole(selectedRole.value.id, {
      code: roleForm.code,
      name: roleForm.name,
      description: roleForm.description,
    })
  } else {
    role = await api.createRole({
      code: roleForm.code,
      name: roleForm.name,
      description: roleForm.description,
    })
  }
  const updatedRole = await api.assignPermissions(role.id, roleForm.permissionIds)
  selectedRoleId.value = updatedRole.id
  if (!(await syncCurrentSession())) return
  await loadAll()
}

async function deleteRole() {
  if (!selectedRole.value) return
  await api.deleteRole(selectedRole.value.id)
  if (!(await syncCurrentSession())) return
  hydrateRole(null)
  await loadAll()
}

async function savePermission() {
  const payload = {
    code: permissionForm.code,
    name: permissionForm.name,
    resource: permissionForm.resource,
    action: permissionForm.action,
    type: permissionForm.type,
    description: permissionForm.description,
  }

  if (selectedPermission.value) {
    await api.updatePermission(selectedPermission.value.id, payload)
  } else {
    await api.createPermission(payload)
  }
  if (!(await syncCurrentSession())) return
  await loadAll()
}

async function deletePermission() {
  if (!selectedPermission.value) return
  await api.deletePermission(selectedPermission.value.id)
  if (!(await syncCurrentSession())) return
  hydratePermission(null)
  await loadAll()
}

watch(availableTabs, syncActiveTab, { immediate: true })
onMounted(loadAll)
</script>

<template>
  <div class="admin-page">
    <section v-if="!availableTabs.length" class="surface admin-empty fade-rise">
      <span class="eyebrow">No Access</span>
      <h2 class="section-title">当前角色没有系统治理权限</h2>
      <p class="muted">用户、角色、权限与日志板块都会根据权限自动隐藏，避免出现“能看见但不能用”的错位感。</p>
    </section>

    <template v-else>
    <section class="surface admin-head fade-rise">
      <div>
        <span class="eyebrow">Admin Console</span>
        <h2 class="section-title">系统管理台</h2>
        <p class="muted">在一页里维护用户、角色、权限和操作日志，确保治理动作有连续感。</p>
      </div>

      <div class="tab-strip">
        <button
          v-for="item in availableTabs"
          :key="item.key"
          :class="{ active: tab === item.key }"
          @click="tab = item.key"
        >
          {{ item.label }}
        </button>
      </div>
    </section>

    <p v-if="error" class="error-copy">{{ error }}</p>

    <div v-if="tab === 'users'" class="admin-grid">
      <section class="surface list-pane fade-rise" style="animation-delay: 60ms">
        <div class="panel-head">
          <div>
            <span class="eyebrow">User List</span>
            <h3>账户列表</h3>
          </div>
        </div>
        <div class="list-stack">
          <button
            v-for="item in users"
            :key="item.id"
            class="entity-row"
            :class="{ active: selectedUser?.id === item.id }"
            @click="hydrateUser(item)"
          >
            <div>
              <strong>{{ item.displayName }}</strong>
              <span>{{ item.username }} · {{ item.department }}</span>
            </div>
            <StatusTag :status="item.status" />
          </button>
        </div>
      </section>

      <section class="surface editor-pane fade-rise" style="animation-delay: 120ms">
        <div class="panel-head">
          <div>
            <span class="eyebrow">Editor</span>
            <h3>{{ selectedUser?.displayName || '选择一个用户' }}</h3>
          </div>
          <button v-if="selectedUser" class="button button-secondary" @click="statusConfirmOpen = true">
            {{ selectedUser.status === 'ACTIVE' ? '停用账户' : '启用账户' }}
          </button>
        </div>

        <div v-if="selectedUser" class="editor-form">
          <div class="field two-up">
            <div class="field">
              <label>姓名</label>
              <input v-model="userForm.displayName" />
            </div>
            <div class="field">
              <label>部门</label>
              <input v-model="userForm.department" />
            </div>
          </div>
          <div class="field two-up">
            <div class="field">
              <label>邮箱</label>
              <input v-model="userForm.email" />
            </div>
            <div class="field">
              <label>手机号</label>
              <input v-model="userForm.phone" />
            </div>
          </div>

          <div class="selector-grid">
            <label v-for="role in roles" :key="role.id" class="selector-row">
              <input v-model="userForm.roleIds" type="checkbox" :value="role.id" />
              <div>
                <strong>{{ role.name }}</strong>
                <span>{{ roleLabel(role.code) }}</span>
              </div>
            </label>
          </div>

          <div class="field">
            <label>重置密码</label>
            <input v-model="userForm.newPassword" type="password" placeholder="留空则不重置" />
          </div>

          <button class="button button-primary" @click="saveUser">保存用户设置</button>
        </div>
      </section>
    </div>

    <div v-else-if="tab === 'roles'" class="admin-grid">
      <section class="surface list-pane fade-rise" style="animation-delay: 60ms">
        <div class="panel-head">
          <div>
            <span class="eyebrow">Role Library</span>
            <h3>角色定义</h3>
          </div>
          <button class="button button-secondary" @click="hydrateRole(null)">新建</button>
        </div>
        <div class="list-stack">
          <button
            v-for="item in roles"
            :key="item.id"
            class="entity-row"
            :class="{ active: selectedRole?.id === item.id }"
            @click="hydrateRole(item)"
          >
            <div>
              <strong>{{ item.name }}</strong>
              <span>{{ item.code }}</span>
            </div>
            <small>{{ item.permissions.length }} 权限</small>
          </button>
        </div>
      </section>

      <section class="surface editor-pane fade-rise" style="animation-delay: 120ms">
        <div class="panel-head">
          <div>
            <span class="eyebrow">Role Editor</span>
            <h3>{{ selectedRole ? '编辑角色' : '新建角色' }}</h3>
          </div>
          <button v-if="selectedRole" class="button button-secondary" @click="roleDeleteConfirmOpen = true">删除</button>
        </div>

        <div class="editor-form">
          <div class="field two-up">
            <div class="field">
              <label>角色编码</label>
              <input v-model="roleForm.code" />
            </div>
            <div class="field">
              <label>角色名称</label>
              <input v-model="roleForm.name" />
            </div>
          </div>
          <div class="field">
            <label>描述</label>
            <textarea v-model="roleForm.description" rows="4"></textarea>
          </div>
          <div class="selector-grid">
            <label v-for="permission in permissions" :key="permission.id" class="selector-row">
              <input v-model="roleForm.permissionIds" type="checkbox" :value="permission.id" />
              <div>
                <strong>{{ permission.name }}</strong>
                <span>{{ permission.code }}</span>
              </div>
            </label>
          </div>
          <button class="button button-primary" @click="saveRole">保存角色</button>
        </div>
      </section>
    </div>

    <div v-else-if="tab === 'permissions'" class="admin-grid">
      <section class="surface list-pane fade-rise" style="animation-delay: 60ms">
        <div class="panel-head">
          <div>
            <span class="eyebrow">Permission Set</span>
            <h3>权限定义</h3>
          </div>
          <div class="head-actions">
            <button class="button button-secondary" @click="permissionGuideOpen = true">推荐规则</button>
            <button class="button button-secondary" @click="hydratePermission(null)">新建</button>
          </div>
        </div>
        <div class="list-stack">
          <button
            v-for="item in permissions"
            :key="item.id"
            class="entity-row"
            :class="{ active: selectedPermission?.id === item.id }"
            @click="hydratePermission(item)"
          >
            <div>
              <strong>{{ item.name }}</strong>
              <span>{{ item.code }}</span>
            </div>
            <small>{{ item.type }}</small>
          </button>
        </div>
      </section>

      <section class="surface editor-pane fade-rise" style="animation-delay: 120ms">
        <div class="panel-head">
          <div>
            <span class="eyebrow">Permission Editor</span>
            <h3>{{ selectedPermission ? '编辑权限' : '新建权限' }}</h3>
          </div>
          <button v-if="selectedPermission" class="button button-secondary" @click="permissionDeleteConfirmOpen = true">删除</button>
        </div>

        <div class="editor-form">
          <div class="field two-up">
            <div class="field">
              <label>权限编码</label>
              <input v-model="permissionForm.code" />
            </div>
            <div class="field">
              <label>名称</label>
              <input v-model="permissionForm.name" />
            </div>
          </div>
          <div class="field two-up">
            <div class="field">
              <label>资源</label>
              <input v-model="permissionForm.resource" />
            </div>
            <div class="field">
              <label>动作</label>
              <input v-model="permissionForm.action" />
            </div>
          </div>
          <div class="field">
            <label>类型</label>
            <select v-model="permissionForm.type">
              <option value="MENU">MENU</option>
              <option value="BUTTON">BUTTON</option>
              <option value="API">API</option>
            </select>
          </div>
          <div class="field">
            <label>描述</label>
            <textarea v-model="permissionForm.description" rows="4"></textarea>
          </div>
          <button class="button button-primary" @click="savePermission">保存权限</button>
        </div>
      </section>
    </div>

    <section v-else class="surface logs-pane fade-rise" style="animation-delay: 80ms">
      <div class="panel-head">
        <div>
          <span class="eyebrow">Audit Trail</span>
          <h3>最近操作日志</h3>
        </div>
        <button class="button button-secondary" @click="loadAll">刷新</button>
      </div>
      <div class="logs-table">
        <div v-for="log in logs" :key="log.id" class="log-row">
          <div>
            <strong>{{ log.action }}</strong>
            <span>{{ log.actorUsername || 'system' }} · {{ log.resourceType }} · {{ formatDateTime(log.createdAt) }}</span>
          </div>
          <div class="log-meta">
            <StatusTag :status="log.result" />
            <small>{{ log.detail || '无补充说明' }}</small>
          </div>
        </div>
      </div>
    </section>
    </template>

    <AppDialog
      :open="permissionGuideOpen"
      title="权限定义推荐规则"
      width="wide"
      @close="permissionGuideOpen = false"
    >
      <p class="muted">先定义能力，再把前后端都绑定到同一个权限码，避免权限只存在于数据库里。</p>

      <div class="guide-grid">
        <article v-for="rule in permissionRules" :key="rule.title" class="guide-rule">
          <strong>{{ rule.title }}</strong>
          <p>{{ rule.detail }}</p>
        </article>
      </div>

      <div class="guide-examples">
        <span class="eyebrow">Examples</span>
        <div v-for="example in permissionExamples" :key="example.code" class="guide-example">
          <div>
            <strong>{{ example.code }}</strong>
            <span>{{ example.note }}</span>
          </div>
          <small>{{ example.resource }} · {{ example.action }} · {{ example.type }}</small>
        </div>
      </div>

      <template #actions>
        <button class="button button-primary" type="button" @click="permissionGuideOpen = false">我知道了</button>
      </template>
    </AppDialog>

    <AppDialog
      :open="statusConfirmOpen"
      :title="selectedUser?.status === 'ACTIVE' ? '确认停用账户' : '确认启用账户'"
      confirm-text="确认"
      tone="danger"
      @close="statusConfirmOpen = false"
      @confirm="statusConfirmOpen = false; toggleUserStatus()"
    >
      <p class="muted">
        {{ selectedUser?.status === 'ACTIVE' ? '停用后该用户将无法继续登录和使用系统。' : '启用后该用户将恢复登录与业务访问能力。' }}
      </p>
      <strong v-if="selectedUser">{{ selectedUser.displayName }} · {{ selectedUser.username }}</strong>
    </AppDialog>

    <AppDialog
      :open="roleDeleteConfirmOpen"
      title="确认删除角色"
      confirm-text="删除角色"
      tone="danger"
      @close="roleDeleteConfirmOpen = false"
      @confirm="roleDeleteConfirmOpen = false; deleteRole()"
    >
      <p class="muted">删除后该角色定义会被移除，请确认当前没有误删正在使用的角色模板。</p>
      <strong v-if="selectedRole">{{ selectedRole.name }} · {{ selectedRole.code }}</strong>
    </AppDialog>

    <AppDialog
      :open="permissionDeleteConfirmOpen"
      title="确认删除权限"
      confirm-text="删除权限"
      tone="danger"
      @close="permissionDeleteConfirmOpen = false"
      @confirm="permissionDeleteConfirmOpen = false; deletePermission()"
    >
      <p class="muted">删除后角色里的对应能力也会失去语义来源，请确认这是一次有意识的治理动作。</p>
      <strong v-if="selectedPermission">{{ selectedPermission.name }} · {{ selectedPermission.code }}</strong>
    </AppDialog>
  </div>
</template>

<style scoped>
.admin-page {
  display: grid;
  gap: 1rem;
}

.admin-empty {
  border-radius: 30px;
  padding: 1.35rem 1.4rem;
}

.admin-head {
  border-radius: 30px;
  padding: 1.3rem 1.4rem;
  display: flex;
  justify-content: space-between;
  align-items: end;
  gap: 1rem;
}

.tab-strip {
  display: inline-flex;
  flex-wrap: wrap;
  gap: 0.45rem;
}

.head-actions {
  display: inline-flex;
  flex-wrap: wrap;
  gap: 0.55rem;
}

.tab-strip button {
  border: 1px solid var(--line);
  background: rgba(255, 252, 247, 0.72);
  padding: 0.82rem 1rem;
  border-radius: 999px;
}

.tab-strip button.active {
  background: var(--ink);
  color: #fff8f2;
}

.admin-grid {
  display: grid;
  grid-template-columns: 0.82fr 1.18fr;
  gap: 1rem;
}

.list-pane,
.editor-pane,
.logs-pane {
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

.list-stack,
.logs-table {
  display: grid;
  gap: 0.85rem;
}

.entity-row,
.log-row {
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

.entity-row.active {
  border-color: rgba(180, 104, 60, 0.4);
  transform: translateX(3px);
}

.entity-row span,
.log-row span {
  display: block;
  margin-top: 0.28rem;
  color: var(--ink-soft);
}

.editor-form {
  display: grid;
  gap: 1rem;
}

.guide-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.8rem;
}

.guide-rule,
.guide-example {
  padding: 0.9rem 1rem;
  border-radius: 20px;
  background: rgba(255, 252, 247, 0.8);
  border: 1px solid rgba(23, 22, 26, 0.06);
}

.guide-rule strong,
.guide-example strong {
  display: block;
}

.guide-rule p,
.guide-example span {
  margin: 0.35rem 0 0;
  color: var(--ink-soft);
  line-height: 1.7;
}

.guide-examples {
  display: grid;
  gap: 0.75rem;
}

.guide-example {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: center;
}

.guide-example small {
  color: var(--ink-soft);
  text-align: right;
  white-space: nowrap;
}

.two-up {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1rem;
}

.selector-grid {
  display: grid;
  gap: 0.7rem;
}

.selector-row {
  display: flex;
  gap: 0.85rem;
  align-items: start;
  padding: 0.9rem 1rem;
  border-radius: 20px;
  background: rgba(255, 252, 247, 0.72);
  border: 1px solid rgba(23, 22, 26, 0.07);
}

.selector-row span {
  display: block;
  margin-top: 0.25rem;
  color: var(--ink-soft);
}

.log-meta {
  display: grid;
  justify-items: end;
  gap: 0.55rem;
}

.log-meta small {
  color: var(--ink-soft);
  max-width: 22rem;
  text-align: right;
}

.error-copy {
  margin: 0;
  color: var(--danger);
}

@media (max-width: 1080px) {
  .admin-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .admin-head {
    flex-direction: column;
    align-items: start;
  }

  .two-up {
    grid-template-columns: 1fr;
  }

  .guide-grid {
    grid-template-columns: 1fr;
  }

  .entity-row,
  .log-row {
    flex-direction: column;
    align-items: start;
  }

  .guide-example {
    flex-direction: column;
    align-items: start;
  }

  .guide-example small {
    text-align: left;
    white-space: normal;
  }

  .log-meta {
    justify-items: start;
  }
}
</style>
