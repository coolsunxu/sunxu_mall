import request from '../utils/request'
import type { 
  ProductVO, 
  ProductQueryDTO, 
  ResponseCursorEntity, 
  UpdateProductDTO,
  CategoryTree,
  Brand,
  Unit,
  AttributeWithValues,
  CreateProductDTO,
  FileDTO
} from '../types'

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

/**
 * 新增商品
 */
export function createProduct(data: CreateProductDTO): Promise<ProductVO> {
  return request.post('/product', data)
}

/**
 * 更新商品信息
 * 使用乐观锁控制并发，客户端需传入当前版本号
 * 版本冲突返回409
 */
export function updateProduct(id: number, data: UpdateProductDTO): Promise<ProductVO> {
  return request.put(`/product/${id}`, data)
}

/**
 * 删除商品（软删除）
 */
export function deleteProduct(id: number): Promise<boolean> {
  return request.delete(`/product/${id}`)
}

/**
 * 获取分类树（用于下拉选择）
 */
export function getCategoryTree(): Promise<CategoryTree[]> {
  return request.get('/category/tree')
}

/**
 * 获取所有品牌列表
 */
export function getAllBrands(): Promise<Brand[]> {
  return request.get('/brand/all')
}

/**
 * 获取所有单位列表
 */
export function getAllUnits(): Promise<Unit[]> {
  return request.get('/unit/all')
}

/**
 * 获取所有属性及其值
 */
export function getAllAttributesWithValues(): Promise<AttributeWithValues[]> {
  return request.get('/attribute/allWithValues')
}

/**
 * 上传文件
 */
export function uploadFile(file: File, bizType: string = 'product'): Promise<FileDTO> {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('bizType', bizType)
  return request.post('/file/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

/**
 * 导出商品数据
 */
export function exportProducts(data: ProductQueryDTO): Promise<any> {
  return request.post('/product/export', data)
}
