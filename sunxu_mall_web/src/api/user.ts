import request from '../utils/request'
import type { 
  UserVO, 
  UserCreateDTO, 
  UserUpdateDTO, 
  UserQueryDTO, 
  ResponsePageEntity 
} from '../types'

/**
 * 根据ID查询用户
 */
export function getUserById(id: number): Promise<UserVO> {
  return request.get('/user/findById', { params: { id } })
}

/**
 * 分页查询用户列表
 */
export function searchUserByPage(data: UserQueryDTO): Promise<ResponsePageEntity<UserVO>> {
  return request.post('/user/searchByPage', data)
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
