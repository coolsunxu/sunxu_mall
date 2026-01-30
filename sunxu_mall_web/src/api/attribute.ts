import request from '../utils/request'
import type { AttributeVO, CreateAttributeDTO, UpdateAttributeDTO } from '../types'

/**
 * 获取所有属性
 */
export function getAll(): Promise<AttributeVO[]> {
  return request.get('/attribute/all')
}

/**
 * 获取所有属性及其值
 */
export function getAllWithValues(): Promise<any[]> {
  return request.get('/attribute/allWithValues')
}

/**
 * 新增属性
 */
export function add(data: CreateAttributeDTO): Promise<boolean> {
  return request.post('/attribute/insert', data).then(res => !!res)
}

/**
 * 更新属性
 */
export function edit(data: UpdateAttributeDTO): Promise<boolean> {
  return request.post('/attribute/update', data).then(res => !!res)
}

/**
 * 批量删除属性
 */
export function del(ids: number[]): Promise<boolean> {
  return request.post('/attribute/deleteByIds', ids).then(res => !!res)
}
