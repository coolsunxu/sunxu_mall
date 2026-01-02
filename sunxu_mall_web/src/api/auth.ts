import request from '../utils/request'
import type { AuthUserEntity, TokenEntity, JwtUserEntity, CaptchaEntity } from '../types'

/**
 * 用户登录
 */
export function login(data: AuthUserEntity): Promise<TokenEntity> {
  return request.post('/web/user/login', data)
}

/**
 * 获取用户信息
 */
export function getUserInfo(): Promise<JwtUserEntity> {
  return request.get('/web/user/info')
}

/**
 * 退出登录
 */
export function logout(): Promise<void> {
  return request.post('/web/user/logout')
}

/**
 * 获取验证码
 */
export function getCode(): Promise<CaptchaEntity> {
  return request.get('/web/user/code')
}
