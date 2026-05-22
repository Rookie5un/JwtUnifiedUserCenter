import type { PortalApp, User } from '@/types'

export const portalApps: PortalApp[] = [
  {
    key: 'oa',
    name: 'OA 协同办公系统',
    shortName: 'OA',
    description: '统一待办、通知公告、流程申请和个人办公入口。',
    iconLabel: 'OA',
    routeName: 'portal-app',
    routeParams: { appKey: 'oa' },
    requiredPermissions: ['OA_ACCESS'],
    status: 'demo',
    category: '协同办公',
    accent: '#2f6f73',
    metrics: [
      { label: '今日待办', value: '8' },
      { label: '流程处理中', value: '14' },
    ],
  },
  {
    key: 'permission',
    name: '权限中心',
    shortName: '权限',
    description: '管理用户、角色、权限和组织部门，支撑统一授权。',
    iconLabel: '权',
    routeName: 'standalone-system',
    routeParams: { appKey: 'permission' },
    requiredPermissions: ['PERMISSION_CENTER_ACCESS'],
    status: 'online',
    category: '统一用户中心',
    accent: '#6b5b95',
    metrics: [
      { label: '角色策略', value: '3' },
      { label: '权限点', value: '11' },
    ],
  },
  {
    key: 'warehouse',
    name: '仓库管理系统',
    shortName: '仓库',
    description: '查看库存、入库出库、调拨单和异常预警示例。',
    iconLabel: '仓',
    routeName: 'portal-app',
    routeParams: { appKey: 'warehouse' },
    requiredPermissions: ['WAREHOUSE_ACCESS'],
    status: 'demo',
    category: '供应链',
    accent: '#4d6b42',
    metrics: [
      { label: '库存品类', value: '126' },
      { label: '待处理单据', value: '9' },
    ],
  },
  {
    key: 'finance',
    name: '财务管理系统',
    shortName: '财务',
    description: '模拟费用报销、付款申请、预算执行和凭证查询。',
    iconLabel: '财',
    routeName: 'portal-app',
    routeParams: { appKey: 'finance' },
    requiredPermissions: ['FINANCE_ACCESS'],
    status: 'demo',
    category: '财务运营',
    accent: '#9a6a2f',
    metrics: [
      { label: '待审批报销', value: '6' },
      { label: '本月预算', value: '86%' },
    ],
  },
  {
    key: 'performance',
    name: '业绩审批系统',
    shortName: '业绩',
    description: '复用现有业绩录入、审批、统计看板，并收拢审计与接口文档入口。',
    iconLabel: '绩',
    routeName: 'portal-app',
    routeParams: { appKey: 'performance' },
    requiredPermissions: ['PERFORMANCE_ACCESS'],
    status: 'online',
    category: '业务审批',
    accent: '#b4683c',
    metrics: [
      { label: '个人台账', value: '启用' },
      { label: '支撑入口', value: '日志/接口' },
    ],
  },
]

export function canAccessPortalApp(app: PortalApp, user: User | null) {
  if (!user) return false

  const roleMatched = !app.requiredRoles?.length || app.requiredRoles.some((role) => user.roles.includes(role))
  const permissionMatched =
    !app.requiredPermissions?.length ||
    app.requiredPermissions.every((permission) => user.permissions.includes(permission))

  return roleMatched && permissionMatched
}

export function visiblePortalApps(user: User | null) {
  return portalApps.filter((app) => canAccessPortalApp(app, user))
}

export function findPortalApp(appKey: string) {
  return portalApps.find((app) => app.key === appKey)
}
