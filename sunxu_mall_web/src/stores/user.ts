import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi, logout as logoutApi, getUserInfo as getUserInfoApi } from '../api/auth'
import type { AuthUserEntity, TokenEntity, JwtUserEntity } from '../types'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem('token') || '')
  const userInfo = ref<JwtUserEntity | null>(null)
  
  const isLoggedIn = computed(() => !!token.value)
  
  const login = async (credentials: AuthUserEntity): Promise<TokenEntity> => {
    console.log('用户状态管理：开始登录', credentials.username)
    try {
      const response: TokenEntity = await loginApi(credentials)
      console.log('用户状态管理：登录成功', response)
      // 保存token到本地存储
      token.value = response.token
      localStorage.setItem('token', token.value)
      return response
    } catch (error) {
      console.error('用户状态管理：登录失败', error)
      throw error
    }
  }
  
  const logout = async (): Promise<void> => {
    console.log('用户状态管理：开始退出登录')
    
    try {
      if (token.value) {
        await logoutApi()
        console.log('用户状态管理：后端退出登录成功')
      }
    } catch (error) {
      console.warn('用户状态管理：后端退出登录失败，但仍将清除本地状态', error)
    } finally {
      // 清除本地状态
      token.value = ''
      userInfo.value = null
      localStorage.removeItem('token')
      console.log('用户状态管理：本地状态已清除')
    }
  }
  
  const checkLoginStatus = async () => {
    console.log('检查登录状态')
    if (token.value) {
      console.log('有token，开始获取用户信息')
      try {
        const response: JwtUserEntity = await getUserInfoApi()
        userInfo.value = response
        console.log('获取用户信息成功', userInfo.value)
      } catch (error) {
        console.log('获取用户信息失败，可能是token已过期或无效，清除token')
        logout()
      }
    } else {
      console.log('没有token，无需获取用户信息')
    }
  }
  
  return {
    token,
    userInfo,
    isLoggedIn,
    login,
    logout,
    checkLoginStatus,
  }
})
