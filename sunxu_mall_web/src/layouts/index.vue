<template>
  <el-container class="layout-container">
    <el-aside width="200px" class="sidebar">
      <div class="logo">
        <h3>孙旭商城管理系统</h3>
      </div>
      <el-menu
        :default-active="$route.path"
        router
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409eff"
      >
        <!-- 首页固定菜单 -->
        <el-menu-item index="/dashboard">
          <el-icon><HomeFilled /></el-icon>
          <span>首页</span>
        </el-menu-item>
        
        <!-- 动态菜单渲染 -->
        <template v-for="menu in menuTree" :key="menu.id">
          <!-- 有子菜单的情况 -->
          <el-sub-menu v-if="menu.children && menu.children.length > 0" :index="menu.path || String(menu.id)">
            <template #title>
              <el-icon v-if="menu.icon"><component :is="menu.icon" /></el-icon>
              <span>{{ menu.label }}</span>
            </template>
            <el-menu-item 
              v-for="child in menu.children" 
              :key="child.id" 
              :index="child.path"
            >
              {{ child.label }}
            </el-menu-item>
          </el-sub-menu>
          
          <!-- 没有子菜单的情况 -->
          <el-menu-item v-else :index="menu.path" :disabled="menu.hidden">
            <el-icon v-if="menu.icon"><component :is="menu.icon" /></el-icon>
            <span>{{ menu.label }}</span>
          </el-menu-item>
        </template>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="header-content">
          <span>欢迎回来，{{ userInfo?.username || '用户' }}！</span>
          <div class="right-menu">
            <!-- 通知中心 -->
            <el-popover
              placement="bottom"
              :width="300"
              trigger="click"
            >
              <template #reference>
                <div class="notify-container">
                  <el-badge :value="unreadCount" :hidden="unreadCount === 0" class="item">
                    <el-icon class="bell-icon" :size="20"><Bell /></el-icon>
                  </el-badge>
                </div>
              </template>
              
              <div class="notify-list">
                <div class="notify-header">
                  <span>通知中心</span>
                  <el-button link type="primary" size="small" @click="handleClearNotify">清空</el-button>
                </div>
                <el-divider style="margin: 10px 0" />
                
                <div v-if="notifications.length === 0" class="notify-empty">
                  暂无通知
                </div>
                
                <div v-else class="notify-scroll">
                  <div 
                    v-for="item in notifications" 
                    :key="item.id" 
                    class="notify-item"
                    :class="{ 'unread': !item.read }"
                  >
                    <div class="notify-title">
                      <el-tag size="small" :type="item.type">{{ item.type === 'success' ? '成功' : '通知' }}</el-tag>
                      <span class="title-text">{{ item.title }}</span>
                      <span class="time">{{ item.time }}</span>
                    </div>
                    <div class="notify-content">{{ item.content }}</div>
                    <div class="notify-action" v-if="item.data?.fileUrl">
                      <el-button link type="primary" size="small" @click="handleDownload(item.data.fileUrl)">点击下载</el-button>
                    </div>
                  </div>
                </div>
              </div>
            </el-popover>

            <el-button 
              type="text" 
              @click="handleLogout"
              :loading="loading"
            >
              {{ loading ? '退出中...' : '退出登录' }}
            </el-button>
          </div>
        </div>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { computed, onMounted, ref } from 'vue'
import { ElMessageBox, ElMessage } from 'element-plus'
import { useMenuStore } from '../stores/menu'
import { useUserStore } from '../stores/user'
import { useNotifyStore } from '../stores/notify'
import { initWebSocket, closeWebSocket } from '@/utils/websocket'

const router = useRouter()
const menuStore = useMenuStore()
const userStore = useUserStore()
const notifyStore = useNotifyStore()
const loading = ref(false)

// 获取菜单树数据
const menuTree = computed(() => menuStore.menuTree)
// 获取用户信息
const userInfo = computed(() => userStore.userInfo)
// 通知数据
const notifications = computed(() => notifyStore.notifications)
const unreadCount = computed(() => notifyStore.unreadCount)

// 页面加载时获取菜单和用户信息
onMounted(async () => {
  try {
    // 如果菜单为空，则获取菜单
    if (menuTree.value.length === 0) {
      await menuStore.getMenuTree()
    }
    // 如果用户信息为空，则获取用户信息
    if (!userInfo.value) {
      await userStore.checkLoginStatus()
    }
    
    // 初始化WebSocket
    if (userInfo.value && userInfo.value.id) {
      initWebSocket(userInfo.value.id)
    }
  } catch (error) {
    console.error('获取菜单或用户信息失败:', error)
  }
})

const handleClearNotify = () => {
  notifyStore.clearNotifications()
}

const handleDownload = (url: string) => {
  window.open(url, '_blank')
}

const handleLogout = async () => {
  try {
    // 显示确认对话框
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    loading.value = true
    // 调用logout方法
    await userStore.logout()
    
    // 关闭WebSocket
    closeWebSocket()
    
    ElMessage.success('退出登录成功')
    // 跳转到登录页
    router.push('/login')
  } catch (error) {
    // 用户取消操作
    if (error === 'cancel') {
      return
    }
    
    // 退出失败也要跳转到登录页，因为本地状态已经清除
    console.error('退出登录失败:', error)
    ElMessage.error('退出登录失败，请重新登录')
    router.push('/login')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.layout-container {
  height: 100vh;
}

.sidebar {
  background-color: #304156;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  border-bottom: 1px solid #434a50;
}

.header {
  background-color: #fff;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
}

.header-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

.right-menu {
  display: flex;
  align-items: center;
}

.notify-container {
  margin-right: 20px;
  cursor: pointer;
  height: 30px;
  display: flex;
  align-items: center;
}

.bell-icon {
  color: #606266;
}

.bell-icon:hover {
  color: #409eff;
}

.notify-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 5px;
}

.notify-scroll {
  max-height: 300px;
  overflow-y: auto;
}

.notify-item {
  padding: 10px;
  border-bottom: 1px solid #EBEEF5;
}

.notify-item:last-child {
  border-bottom: none;
}

.notify-title {
  display: flex;
  align-items: center;
  margin-bottom: 5px;
}

.title-text {
  font-weight: bold;
  margin-left: 5px;
  flex: 1;
}

.time {
  font-size: 12px;
  color: #909399;
}

.notify-content {
  font-size: 13px;
  color: #606266;
  margin-bottom: 5px;
}

.notify-empty {
  text-align: center;
  color: #909399;
  padding: 20px 0;
}
</style>