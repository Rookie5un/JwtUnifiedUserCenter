import { clearTokens, getRefreshToken, request, setTokens } from './client'
import type {
  AuthPayload,
  Department,
  DashboardMetric,
  DashboardResponse,
  LoginPayload,
  OperationLog,
  Permission,
  PerformancePayload,
  PerformanceRecord,
  RankingItem,
  RegisterPayload,
  Role,
  UpdateUserPayload,
  User,
} from '@/types'

export const api = {
  login(payload: LoginPayload) {
    return request<AuthPayload>('/auth/login', {
      method: 'POST',
      body: JSON.stringify(payload),
    }, false).then((data) => {
      setTokens(data)
      return data
    })
  },
  register(payload: RegisterPayload) {
    return request<User>('/auth/register', {
      method: 'POST',
      body: JSON.stringify(payload),
    }, false)
  },
  publicDepartments() {
    return request<Department[]>('/departments/public', {}, false)
  },
  me() {
    return request<User>('/auth/me')
  },
  async logout() {
    const refreshToken = getRefreshToken()
    if (refreshToken) {
      await request<void>('/auth/logout', {
        method: 'POST',
        body: JSON.stringify({ refreshToken }),
      })
    }
    clearTokens()
  },
  changePassword(currentPassword: string, newPassword: string) {
    return request<void>('/auth/change-password', {
      method: 'POST',
      body: JSON.stringify({ currentPassword, newPassword }),
    })
  },
  users() {
    return request<User[]>('/users')
  },
  user(id: number) {
    return request<User>(`/users/${id}`)
  },
  updateUser(id: number, payload: UpdateUserPayload) {
    return request<User>(`/users/${id}`, {
      method: 'PUT',
      body: JSON.stringify(payload),
    })
  },
  deleteUser(id: number) {
    return request<void>(`/users/${id}`, { method: 'DELETE' })
  },
  departments() {
    return request<Department[]>('/departments')
  },
  createDepartment(payload: Pick<Department, 'name' | 'description'>) {
    return request<Department>('/departments', {
      method: 'POST',
      body: JSON.stringify(payload),
    })
  },
  updateDepartment(id: number, payload: Pick<Department, 'name' | 'description'>) {
    return request<Department>(`/departments/${id}`, {
      method: 'PUT',
      body: JSON.stringify(payload),
    })
  },
  deleteDepartment(id: number) {
    return request<void>(`/departments/${id}`, { method: 'DELETE' })
  },
  updateUserStatus(id: number, status: User['status']) {
    return request<User>(`/users/${id}/status`, {
      method: 'PUT',
      body: JSON.stringify({ status }),
    })
  },
  resetPassword(id: number, newPassword: string) {
    return request<void>(`/users/${id}/reset-password`, {
      method: 'POST',
      body: JSON.stringify({ newPassword }),
    })
  },
  assignRoles(id: number, roleIds: number[]) {
    return request<User>(`/users/${id}/roles`, {
      method: 'POST',
      body: JSON.stringify({ roleIds }),
    })
  },
  roles() {
    return request<Role[]>('/roles')
  },
  createRole(payload: Omit<Role, 'id' | 'permissions'> & { permissions?: string[] }) {
    return request<Role>('/roles', {
      method: 'POST',
      body: JSON.stringify({
        code: payload.code,
        name: payload.name,
        description: payload.description,
      }),
    })
  },
  updateRole(id: number, payload: Pick<Role, 'code' | 'name' | 'description'>) {
    return request<Role>(`/roles/${id}`, {
      method: 'PUT',
      body: JSON.stringify(payload),
    })
  },
  deleteRole(id: number) {
    return request<void>(`/roles/${id}`, { method: 'DELETE' })
  },
  assignPermissions(roleId: number, permissionIds: number[]) {
    return request<Role>(`/roles/${roleId}/permissions`, {
      method: 'POST',
      body: JSON.stringify({ permissionIds }),
    })
  },
  permissions() {
    return request<Permission[]>('/permissions')
  },
  createPermission(payload: Omit<Permission, 'id'>) {
    return request<Permission>('/permissions', {
      method: 'POST',
      body: JSON.stringify(payload),
    })
  },
  updatePermission(id: number, payload: Omit<Permission, 'id'>) {
    return request<Permission>(`/permissions/${id}`, {
      method: 'PUT',
      body: JSON.stringify(payload),
    })
  },
  deletePermission(id: number) {
    return request<void>(`/permissions/${id}`, { method: 'DELETE' })
  },
  records() {
    return request<PerformanceRecord[]>('/performance/records')
  },
  createRecord(payload: PerformancePayload) {
    return request<PerformanceRecord>('/performance/records', {
      method: 'POST',
      body: JSON.stringify(payload),
    })
  },
  updateRecord(id: number, payload: PerformancePayload) {
    return request<PerformanceRecord>(`/performance/records/${id}`, {
      method: 'PUT',
      body: JSON.stringify(payload),
    })
  },
  pendingApprovals() {
    return request<PerformanceRecord[]>('/performance/approvals/pending')
  },
  approveRecord(id: number) {
    return request<PerformanceRecord>(`/performance/approvals/${id}/approve`, {
      method: 'POST',
    })
  },
  rejectRecord(id: number, reason: string) {
    return request<PerformanceRecord>(`/performance/approvals/${id}/reject`, {
      method: 'POST',
      body: JSON.stringify({ reason }),
    })
  },
  personalDashboard() {
    return request<DashboardResponse>('/performance/dashboard/personal')
  },
  departmentDashboard() {
    return request<DashboardResponse>('/performance/dashboard/department')
  },
  globalDashboard() {
    return request<DashboardResponse>('/performance/dashboard/global')
  },
  monthly(scope: 'personal' | 'department' | 'global') {
    return request<DashboardMetric[]>(`/performance/statistics/monthly?scope=${scope}`)
  },
  ranking(scope: 'personal' | 'department' | 'global') {
    return request<RankingItem[]>(`/performance/statistics/ranking?scope=${scope}`)
  },
  logs(limit = 60) {
    return request<OperationLog[]>(`/logs?limit=${limit}`)
  },
}
