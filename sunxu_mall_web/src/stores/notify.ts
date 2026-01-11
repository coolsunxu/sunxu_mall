import { defineStore } from 'pinia'
import { ref } from 'vue'

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

  // 标记所有为已读
  const markAllRead = () => {
    notifications.value.forEach(item => item.read = true)
    unreadCount.value = 0
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
    markAllRead,
    clearNotifications
  }
})
