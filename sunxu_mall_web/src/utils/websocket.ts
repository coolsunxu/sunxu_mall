import { ElNotification } from 'element-plus'
import { useNotifyStore } from '../stores/notify'

let websocket: WebSocket | null = null

/**
 * 初始化WebSocket连接
 * @param userId 用户ID
 */
export const initWebSocket = (userId: number | string) => {
  if (websocket) {
    websocket.close()
  }

  // 默认连接到本地8011端口
  // 实际项目中应从环境变量获取WebSocket地址
  const wsUrl = `ws://localhost:8011/ws/${userId}`
  
  console.log(`正在连接WebSocket: ${wsUrl}`)
  websocket = new WebSocket(wsUrl)

  websocket.onopen = () => {
    console.log('WebSocket连接成功')
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
  }

  websocket.onerror = (e) => {
    console.error('WebSocket连接发生错误', e)
  }
}

/**
 * 关闭WebSocket连接
 */
export const closeWebSocket = () => {
  if (websocket) {
    websocket.close()
    websocket = null
  }
}
