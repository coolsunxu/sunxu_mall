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
export interface ResponsePageEntity<T> {
  pageNum: number
  pageSize: number
  total: number
  list: T[]
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
  pageNum: number
  pageSize: number
  userName?: string
  phone?: string
  email?: string
  validStatus?: boolean
  deptId?: number
}
