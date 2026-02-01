import request from '../utils/request'

/**
 * 标记通知为已读
 * @param notifyId 通知ID
 */
export function markAsRead(notifyId: string | number): Promise<void> {
  return request.post(`/notify/${notifyId}/read`)
}

/**
 * 标记所有通知为已读
 * @returns 更新的记录数
 */
export function markAllAsRead(): Promise<number> {
  return request.post('/notify/read-all')
}
