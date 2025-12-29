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
          <el-button 
            type="text" 
            @click="handleLogout"
            :loading="loading"
          >
            {{ loading ? '退出中...' : '退出登录' }}
          </el-button>
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

const router = useRouter()
const menuStore = useMenuStore()
const userStore = useUserStore()
const loading = ref(false)

// 获取菜单树数据
const menuTree = computed(() => menuStore.menuTree)
// 获取用户信息
const userInfo = computed(() => userStore.userInfo)

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
  } catch (error) {
    console.error('获取菜单或用户信息失败:', error)
  }
})

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
</style>