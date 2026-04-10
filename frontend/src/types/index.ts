export interface ApiEnvelope<T> {
  success: boolean
  code: string
  message: string
  data: T
  timestamp: string
}

export interface User {
  id: number
  username: string
  displayName: string
  phone?: string
  email?: string
  department: string
  status: 'ACTIVE' | 'DISABLED'
  roles: string[]
  permissions: string[]
  createdAt: string
}

export interface AuthPayload {
  accessToken: string
  refreshToken: string
  accessTokenExpiresAt: string
  user: User
}

export interface LoginPayload {
  username: string
  password: string
}

export interface RegisterPayload {
  username: string
  password: string
  displayName: string
  department: string
  email?: string
  phone?: string
}

export interface UpdateUserPayload {
  username: string
  displayName: string
  department: string
  email?: string
  phone?: string
}

export interface Department {
  id: number
  name: string
  description?: string
  createdAt: string
}

export interface Role {
  id: number
  code: string
  name: string
  description?: string
  permissions: string[]
}

export interface Permission {
  id: number
  code: string
  name: string
  resource: string
  action: string
  type: 'MENU' | 'BUTTON' | 'API'
  description?: string
}

export interface PerformanceRecord {
  id: number
  ownerId: number
  ownerName: string
  department: string
  amount: number
  occurredOn: string
  type: string
  note?: string
  status: 'PENDING' | 'APPROVED' | 'REJECTED'
  rejectedReason?: string
  createdAt: string
  approvedAt?: string
  approvedBy?: string
}

export interface PerformancePayload {
  amount: number
  occurredOn: string
  type: string
  note?: string
}

export interface DashboardMetric {
  month: string
  totalAmount: number
}

export interface RankingItem {
  name: string
  department: string
  totalAmount: number
}

export interface DashboardResponse {
  scope: string
  totalAmount: number
  totalRecords: number
  pendingCount: number
  approvedCount: number
  rejectedCount: number
  monthly: DashboardMetric[]
  ranking: RankingItem[]
}

export interface OperationLog {
  id: number
  actorId?: number
  actorUsername?: string
  action: string
  resourceType: string
  resourceId?: string
  result: 'SUCCESS' | 'FAILURE'
  detail?: string
  createdAt: string
}
