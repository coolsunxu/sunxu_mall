import request from '../utils/request'
import type { AttributeValueVO, AttributeValueQueryDTO, CreateAttributeValueDTO, UpdateAttributeValueDTO, ResponseCursorEntity } from '../types'

/**
 * 分页查询属性值（旧版，不推荐）
 */
export function searchByPage(data: AttributeValueQueryDTO): Promise<any> {
  return request.post('/attributeValue/searchByPage', data)
}

/**
 * 双向游标分页查询属性值
 */
export function searchByBidirectionalCursor(data: AttributeValueQueryDTO): Promise<ResponseCursorEntity<AttributeValueVO>> {
  return request.post('/attributeValue/searchByBidirectionalCursor', data)
}

/**
 * 新增属性值
 */
export function add(data: CreateAttributeValueDTO): Promise<boolean> {
  return request.post('/attributeValue/insert', data).then(res => !!res)
}

/**
 * 更新属性值
 */
export function edit(data: UpdateAttributeValueDTO): Promise<boolean> {
  return request.post('/attributeValue/update', data).then(res => !!res)
}

/**
 * 批量删除属性值
 */
export function del(ids: number[]): Promise<boolean> {
  return request.post('/attributeValue/deleteByIds', ids).then(res => !!res)
}
