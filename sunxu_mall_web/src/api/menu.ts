import request from '../utils/request'
import type { MenuTreeDTO } from '../types'

/**
 * 获取菜单树
 */
export function getMenuTree(): Promise<MenuTreeDTO[]> {
  return request.get('/menu/getMenuTree')
}
