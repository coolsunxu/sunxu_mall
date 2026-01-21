import { ElNotification } from 'element-plus'
import { useNotifyStore } from '../stores/notify'

let websocket: WebSocket | null = null
let reconnectTimer: number | null = null
let reconnectAttempts = 0
let currentUserId: number | string | null = null
let manualClose = false

function buildWsUrl(userId: number | string): string {
  const configuredBase = import.meta.env.VITE_WS_BASE_URL as string | undefined
  const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws'

  // 约定：VITE_WS_BASE_URL 只配置到 host/port（例如 ws://localhost:8011 或 wss://api.xxx.com）
  if (configuredBase && configuredBase.trim()) {
    return `${configuredBase.replace(/\/$/, '')}/ws/${userId}`
  }

  // 开发环境：默认连后端 8011；生产：默认同域（便于反代/网关）
  const host =
    import.meta.env.DEV ? `${window.location.hostname}:8011` : window.location.host
  return `${protocol}://${host}/ws/${userId}`
}

function clearReconnectTimer() {
  if (reconnectTimer != null) {
    window.clearTimeout(reconnectTimer)
    reconnectTimer = null
  }
}

/**
 * 初始化WebSocket连接
 * @param userId 用户ID
 */
export const initWebSocket = (userId: number | string) => {
  currentUserId = userId
  manualClose = false
  clearReconnectTimer()

  if (websocket) {
    websocket.close()
  }

  const wsUrl = buildWsUrl(userId)
  
  console.log(`正在连接WebSocket: ${wsUrl}`)
  websocket = new WebSocket(wsUrl)

  websocket.onopen = () => {
    console.log('WebSocket连接成功')
    reconnectAttempts = 0
  }

  websocket.onmessage = (event) => {
    try {
      // 可能会收到 "连接成功" 这样的文本消息
      if (event.data === '连接成功') {
        return
      }

      const res = JSON.parse(event.data)
      console.log('收到WebSocket消息:', res)

      // 监听导出成功消息
      if (res.type === 'EXPORT_EXCEL') {
        const notifyStore = useNotifyStore()
        
        // 1. 添加到通知中心
        notifyStore.addNotification({
          title: '导出成功',
          content: `文件 "${res.data.fileName}" 已生成`,
          type: 'success',
          data: {
            fileUrl: res.data.fileUrl
          }
        })

        // 2. 弹出临时通知
        ElNotification({
          title: '导出成功',
          message: '文件已生成，点击本通知或右上角铃铛下载',
          type: 'success',
          duration: 5000,
          onClick: () => {
            if (res.data && res.data.fileUrl) {
              window.open(res.data.fileUrl, '_blank')
            }
          }
        })
      }
    } catch (e) {
      console.error('WebSocket消息解析失败', e)
    }
  }

  websocket.onclose = () => {
    console.log('WebSocket连接已断开')
    websocket = null
    if (manualClose) return

    // 简单重连（指数退避 + 上限），避免网络抖动时频繁重连
    const maxAttempts = 10
    if (reconnectAttempts >= maxAttempts) return
    const delay = Math.min(30_000, 500 * Math.pow(2, reconnectAttempts))
    reconnectAttempts += 1
    clearReconnectTimer()
    reconnectTimer = window.setTimeout(() => {
      if (currentUserId != null) initWebSocket(currentUserId)
    }, delay)
  }

  websocket.onerror = (e) => {
    console.error('WebSocket连接发生错误', e)
  }
}

/**
 * 关闭WebSocket连接
 */
export const closeWebSocket = () => {
  manualClose = true
  clearReconnectTimer()
  if (websocket) {
    websocket.close()
    websocket = null
  }
}
