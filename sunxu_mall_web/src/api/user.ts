import request from '../utils/request'
import type {
  UserVO,
  UserCreateDTO,
  UserUpdateDTO,
  UserQueryDTO,
  ResponseCursorEntity
} from '../types'

/**
 * 根据ID查询用户
 */
export function getUserById(id: number): Promise<UserVO> {
  return request.get('/user/findById', { params: { id } })
}

/**
 * 查询用户列表（游标分页）
 * 支持向前/向后翻页，可指定页码
 * 
 * 注意：searchByPage 和 searchByCursor 已下线，统一使用此接口
 */
export function searchUsers(data: UserQueryDTO): Promise<ResponseCursorEntity<UserVO>> {
  return request.post('/user/searchByBidirectionalCursor', data)
}

/**
 * 兼容旧调用（页面仍在使用该名字）
 * 注意：后端仍是 searchByBidirectionalCursor
 */
export function searchUserByBidirectionalCursor(data: UserQueryDTO): Promise<ResponseCursorEntity<UserVO>> {
  return searchUsers(data)
}

/**
 * 新增用户
 */
export function insertUser(data: UserCreateDTO): Promise<void> {
  return request.post('/user/insert', data)
}

/**
 * 修改用户
 */
export function updateUser(data: UserUpdateDTO): Promise<number> {
  return request.post('/user/update', data)
}

/**
 * 批量删除用户
 */
export function deleteUserByIds(ids: number[]): Promise<number> {
  return request.post('/user/deleteByIds', ids)
}

/**
 * 批量重置密码
 */
export function resetUserPwd(ids: number[]): Promise<number> {
  return request.post('/user/resetPwd', ids)
}

/**
 * 导出用户数据
 */
export function exportUser(data: UserQueryDTO): Promise<void> {
  return request.post('/user/export', data)
}
