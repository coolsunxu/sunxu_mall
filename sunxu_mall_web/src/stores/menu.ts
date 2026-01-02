import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getMenuTree as getMenuTreeApi } from '../api/menu'
import type { MenuTreeDTO } from '../types'

export const useMenuStore = defineStore('menu', () => {
  const menuTree = ref<MenuTreeDTO[]>([])
  const loading = ref(false)
  
  const getMenuTree = async (): Promise<MenuTreeDTO[]> => {
    console.log('菜单状态管理：开始获取菜单树')
    loading.value = true
    try {
      const response: MenuTreeDTO[] = await getMenuTreeApi()
      console.log('菜单状态管理：获取菜单树成功', response)
      menuTree.value = response
      return menuTree.value
    } catch (error) {
      console.error('菜单状态管理：获取菜单树失败', error)
      throw error
    } finally {
      loading.value = false
      console.log('菜单状态管理：获取菜单树结束')
    }
  }
  
  // 清空菜单
  const clearMenu = () => {
    menuTree.value = []
  }
  
  return {
    menuTree,
    loading,
    getMenuTree,
    clearMenu,
  }
})
