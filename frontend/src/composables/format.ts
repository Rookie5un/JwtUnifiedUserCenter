export function formatCurrency(value: number) {
  return new Intl.NumberFormat('zh-CN', {
    style: 'currency',
    currency: 'CNY',
    maximumFractionDigits: 0,
  }).format(value)
}

export function formatDate(value?: string) {
  if (!value) return '未记录'
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  }).format(new Date(value))
}

export function formatDateTime(value?: string) {
  if (!value) return '未记录'
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value))
}

export function roleLabel(code: string) {
  return (
    {
      ADMIN: '系统管理员',
      MANAGER: '部门经理',
      EMPLOYEE: '普通员工',
    }[code] ?? code
  )
}

export function statusLabel(status: string) {
  return (
    {
      PENDING: '待审批',
      APPROVED: '已审批',
      REJECTED: '已驳回',
      ACTIVE: '启用',
      DISABLED: '停用',
      SUCCESS: '成功',
      FAILURE: '失败',
    }[status] ?? status
  )
}
