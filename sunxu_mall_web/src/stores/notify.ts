import { defineStore } from 'pinia'
import { ref } from 'vue'
import { markAsRead, markAllAsRead } from '../api/notify'

export interface NotificationItem {
  id: string
  title: string
  content: string
  time: string
  read: boolean
  type: 'success' | 'warning' | 'info' | 'error'
  data?: any // 存放额外数据，如下载链接
}

export const useNotifyStore = defineStore('notify', () => {
  const notifications = ref<NotificationItem[]>([])
  const unreadCount = ref(0)

  // 添加通知
  const addNotification = (notify: Omit<NotificationItem, 'id' | 'time' | 'read'>) => {
    const newNotify: NotificationItem = {
      ...notify,
      id: Date.now().toString(),
      time: new Date().toLocaleTimeString(),
      read: false
    }
    notifications.value.unshift(newNotify)
    unreadCount.value++
  }

  // 标记单个通知为已读
  const markNotificationAsRead = async (notifyId: string) => {
    const notify = notifications.value.find(item => item.id === notifyId)
    if (notify && !notify.read) {
      try {
        await markAsRead(notifyId)
        notify.read = true
        unreadCount.value = Math.max(0, unreadCount.value - 1)
      } catch (error) {
        console.error('标记通知已读失败', error)
      }
    }
  }

  // 标记所有为已读（点击通知中心时调用）
  const markAllRead = async () => {
    if (unreadCount.value === 0) return
    
    try {
      await markAllAsRead()
      notifications.value.forEach(item => item.read = true)
      unreadCount.value = 0
    } catch (error) {
      console.error('标记所有通知已读失败', error)
    }
  }

  // 清空通知
  const clearNotifications = () => {
    notifications.value = []
    unreadCount.value = 0
  }

  return {
    notifications,
    unreadCount,
    addNotification,
    markNotificationAsRead,
    markAllRead,
    clearNotifications
  }
})
