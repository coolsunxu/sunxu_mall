/**
 * 后端 API 类型定义
 * 与后端 Java 实体类对应
 */

// 验证码实体 - 对应后端 CaptchaEntity
export interface CaptchaEntity {
  uuid: string
  img: string
}

// 登录请求参数 - 对应后端 AuthUserEntity
export interface AuthUserEntity {
  uuid: string
  username: string
  password: string
  code: string
  phone?: string
}

// 登录响应 - 对应后端 TokenEntity
export interface TokenEntity {
  username: string
  token: string
  roles: string[]
  expiresIn: number
}

// 用户信息 - 对应后端 JwtUserEntity
export interface JwtUserEntity {
  id: number
  username: string
  roles: string[]
}

// 菜单元数据 - 对应后端 MetaDTO
export interface MetaDTO {
  icon?: string
  noCache?: boolean
  title?: string
}

// 菜单树 - 对应后端 MenuTreeDTO
export interface MenuTreeDTO {
  id: number
  label: string
  pid?: number
  sort?: number
  icon?: string
  path?: string
  hidden?: boolean
  isLink?: number
  type?: number
  permission?: string
  url?: string
  component?: string
  createTime?: string
  redirect?: string
  alwaysShow?: boolean
  meta?: MetaDTO
  children?: MenuTreeDTO[]
  leaf?: boolean
  subCount?: number
  hasChildren?: boolean
}

// API 响应格式 - 对应后端 ApiResult
export interface ApiResult<T = any> {
  code: number
  message: string | null
  data: T
}

// 分页响应格式 - 对应后端 ResponsePageEntity
// 注意：后端 searchByPage 已下线，此类型仅保留用于兼容
// @deprecated 推荐使用 ResponseCursorEntity
export interface ResponsePageEntity<T> {
  pageNum: number
  pageSize: number
  total: number
  list?: T[]
  rows?: T[]
  pages?: number
}

// 游标分页响应格式 - 对应后端 ResponseCursorEntity
export interface ResponseCursorEntity<T> {
  pageSize: number
  nextCursorId: number | null
  hasNext: boolean
  list: T[]
  cursorToken?: string // 游标状态令牌
  currentPageNum?: number // 当前页码
  prevCursorId?: number | null // 上一页游标
  hasPrev?: boolean // 是否有上一页
}

// 用户展示对象 - 对应后端 UserVO
export interface UserVO {
  id: number
  userName: string
  nickName: string
  email: string
  phone: string
  deptId: number
  jobId: number
  sex: number // 0-Female, 1-Male, 2-Unknown
  validStatus: boolean
  avatarId: number
  createTime: string
  lastLoginTime: string
  lastLoginCity: string
}

// 用户创建参数 - 对应后端 UserCreateDTO
export interface UserCreateDTO {
  userName: string
  password?: string
  email?: string
  phone?: string
  deptId?: number
  jobId?: number
  nickName?: string
  sex?: number
  validStatus?: boolean
  avatarId?: number
}

// 用户更新参数 - 对应后端 UserUpdateDTO
export interface UserUpdateDTO {
  id: number
  nickName?: string
  email?: string
  phone?: string
  deptId?: number
  jobId?: number
  sex?: number
  validStatus?: boolean
  avatarId?: number
  version?: number
}

// 用户查询参数 - 对应后端 UserQueryDTO
export interface UserQueryDTO {
  pageNum?: number
  pageSize?: number
  cursorId?: number
  cursorDirection?: string // NEXT, PREV
  cursorToken?: string // Base64 encoded cursor state
  userName?: string
  phone?: string
  email?: string
  validStatus?: boolean
  deptId?: number
}

// 商品展示对象 - 对应后端 ProductVO
export interface ProductVO {
  id: number
  categoryId: number
  productGroupId: number
  brandId: number
  unitId: number
  name: string
  model: string
  quantity: number
  remainQuantity: number
  price: number
  coverUrl: string
  createTime: string
  updateTime: string
  version: number
}

// 商品更新参数 - 对应后端 UpdateProductDTO
export interface UpdateProductDTO {
  version: number
  categoryId?: number
  brandId?: number
  unitId?: number
  productGroupId?: number
  name?: string
  model?: string
  quantity?: number
  remainQuantity?: number
  price?: number
  coverUrl?: string
  skuAttributes?: UpdateProductAttributeDTO[]
}

// 商品属性更新请求 - 对应后端 UpdateProductAttributeDTO
export interface UpdateProductAttributeDTO {
  id?: number
  attributeId: number
  attributeValueId: number
  deleted?: boolean
}

// 商品查询参数 - 对应后端 ProductQueryDTO
export interface ProductQueryDTO {
  pageNum?: number
  pageSize?: number
  cursorId?: number
  cursorDirection?: string // NEXT, PREV
  cursorToken?: string // Base64 encoded cursor state
  name?: string
  model?: string
  categoryId?: number
  brandId?: number
  productGroupId?: number
}

// 文件上传响应 - 对应后端 FileDTO
export interface FileDTO {
  originalName: string
  fileName: string
  path: string
  downloadUrl: string
  type: string
  size: number
}

// 分类树节点
export interface CategoryTree {
  id: number
  name: string
  parentId: number
  children: CategoryTree[]
}

// 品牌
export interface Brand {
  id: number
  name: string
}

// 单位
export interface Unit {
  id: number
  name: string
}

// 属性及属性值
export interface AttributeValue {
  id: number
  attributeId: number
  value: string
}

export interface AttributeWithValues {
  id: number
  name: string
  values: AttributeValue[]
}

// 新增商品请求属性
export interface CreateProductAttributeDTO {
  attributeId: number
  attributeValueId: number
}

// 新增商品请求 - 对应后端 CreateProductDTO
export interface CreateProductDTO {
  categoryId: number
  brandId: number
  unitId: number
  name: string
  model?: string
  price: number
  quantity: number
  coverUrl?: string
  detail?: string
  spuAttributes?: CreateProductAttributeDTO[]
  skuAttributes?: CreateProductAttributeDTO[]
  photos?: string[]
}

// 属性值展示对象 - 对应后端 MallAttributeValueEntity
export interface AttributeValueVO {
  id: number
  attributeId: number
  attributeName?: string
  value: string
  sort: number
  createUserId: number
  createUserName: string
  createTime: string
  updateUserId?: number
  updateUserName?: string
  updateTime?: string
}

// 属性值查询参数 - 对应后端 AttributeValueQueryDTO
export interface AttributeValueQueryDTO {
  pageNum?: number
  pageSize?: number
  cursorId?: number
  cursorDirection?: string // NEXT, PREV
  cursorToken?: string // Base64 encoded cursor state
  attributeId?: number
  value?: string
  sort?: number
  createUserId?: number
  createUserName?: string
  startTime?: string
  endTime?: string
}

// 属性值创建参数 - 对应后端 CreateAttributeValueDTO
export interface CreateAttributeValueDTO {
  attributeId: number
  value: string
  sort?: number
}

// 属性值更新参数 - 对应后端 UpdateAttributeValueDTO
export interface UpdateAttributeValueDTO {
  id: number
  attributeId: number
  value: string
  sort?: number
}

// 属性展示对象 - 对应后端 MallAttributeEntity
export interface AttributeVO {
  id: number
  name: string
  createUserId: number
  createUserName: string
  createTime: string
  updateUserId?: number
  updateUserName?: string
  updateTime?: string
}

// 属性查询参数
export interface AttributeQueryDTO {
  pageNum?: number
  pageSize?: number
  cursorId?: number
  cursorDirection?: string // NEXT, PREV
  cursorToken?: string // Base64 encoded cursor state
  name?: string
}

// 属性创建参数
export interface CreateAttributeDTO {
  name: string
}

// 属性更新参数
export interface UpdateAttributeDTO {
  id: number
  name: string
}