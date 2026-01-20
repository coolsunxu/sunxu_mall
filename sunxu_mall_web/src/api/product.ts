import request from '../utils/request'
import type { ProductVO, ProductQueryDTO, ResponsePageEntity } from '../types'

/**
 * 根据ID查询商品
 */
export function getProductById(id: number): Promise<ProductVO> {
  return request.get('/product/findById', { params: { id } })
}

/**
 * 分页查询商品列表
 */
export function searchProductByPage(data: ProductQueryDTO): Promise<ResponsePageEntity<ProductVO>> {
  return request.post('/product/searchByPage', data)
}
