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

