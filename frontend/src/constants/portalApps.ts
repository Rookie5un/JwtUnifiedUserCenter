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
    requiredPermissions: ['USER_MANAGE', 'ROLE_MANAGE', 'PERMISSION_MANAGE'],
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
    description: '复用现有业绩录入、审批、统计看板作为真实业务示例。',
    iconLabel: '绩',
    routeName: 'portal-app',
    routeParams: { appKey: 'performance' },
    requiredPermissions: ['PERFORMANCE_VIEW_SELF'],
    status: 'online',
    category: '业务审批',
    accent: '#b4683c',
    metrics: [
      { label: '个人台账', value: '启用' },
      { label: '审批流', value: 'JWT' },
    ],
  },
  {
    key: 'logs',
    name: '操作日志系统',
    shortName: '日志',
    description: '审计登录、授权、业务操作和系统访问记录。',
    iconLabel: '志',
    routeName: 'standalone-system',
    routeParams: { appKey: 'logs' },
    requiredPermissions: ['LOG_VIEW'],
    status: 'online',
    category: '安全审计',
    accent: '#3d5872',
    metrics: [
      { label: '审计范围', value: '全局' },
      { label: '登录事件', value: '记录' },
    ],
  },
  {
    key: 'docs',
    name: '接口文档系统',
    shortName: '接口',
    description: '查看 RESTful API、JWT 鉴权和后端接口说明。',
    iconLabel: 'API',
    routeName: 'standalone-system',
    routeParams: { appKey: 'docs' },
    requiredRoles: ['ADMIN'],
    status: 'online',
    category: '开发支撑',
    accent: '#4c6171',
    metrics: [
      { label: '接口规范', value: 'REST' },
      { label: '认证方式', value: 'Bearer' },
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
