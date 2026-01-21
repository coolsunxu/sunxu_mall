import request from '../utils/request'
import type { ProductVO, ProductQueryDTO, ResponseCursorEntity } from '../types'

/**
 * 根据ID查询商品
 */
export function getProductById(id: number): Promise<ProductVO> {
  return request.get('/product/findById', { params: { id } })
}

/**
 * 查询商品列表（游标分页）
 * 支持向前/向后翻页，可指定页码
 * 
 * 注意：searchByPage 和 searchByCursor 已下线，统一使用此接口
 */
export function searchProducts(data: ProductQueryDTO): Promise<ResponseCursorEntity<ProductVO>> {
  return request.post('/product/searchByBidirectionalCursor', data)
}

/**
 * 兼容旧调用（页面仍在使用该名字）
 * 注意：后端仍是 searchByBidirectionalCursor
 */
export function searchProductByBidirectionalCursor(data: ProductQueryDTO): Promise<ResponseCursorEntity<ProductVO>> {
  return searchProducts(data)
}
