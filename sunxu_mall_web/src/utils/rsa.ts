import JSEncrypt from 'jsencrypt'

function normalizePublicKey(key: string): string {
  const trimmed = key.trim()
  if (trimmed.includes('BEGIN PUBLIC KEY')) return trimmed
  // 兼容只配置 base64 内容的场景
  return `-----BEGIN PUBLIC KEY-----\n${trimmed}\n-----END PUBLIC KEY-----`
}

// RSA 公钥：
// - 推荐通过环境变量注入：VITE_PASSWORD_PUBLIC_KEY（可为 PEM 或 base64 内容）
// - 兜底：使用当前内置 key（仅用于本地/示例）
const DEFAULT_PUBLIC_KEY = `-----BEGIN PUBLIC KEY-----
MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQC1aBCsVhTEiAynIOu7Peu6qJZg8HBmTdRWtb1ejzMzTQDAljQfQsSwWzgp1ghewGdWgmfqP3BwBFMO0cIjrJitnDm3JA2SAzjcnvJn3jmggeN6ESbkWtgK2WYsFO7pTVZX3U+3EKoG9eQbx7qkAwxbcUJ+i4/yoMNdfTEk+8dN/QIDAQAB
-----END PUBLIC KEY-----`

const PUBLIC_KEY = import.meta.env.VITE_PASSWORD_PUBLIC_KEY
  ? normalizePublicKey(import.meta.env.VITE_PASSWORD_PUBLIC_KEY)
  : DEFAULT_PUBLIC_KEY

/**
 * RSA加密
 * @param data 需要加密的数据
 * @returns 加密后的base64字符串
 */
export function encrypt(data: string): string {
  const encryptor = new JSEncrypt()
  encryptor.setPublicKey(PUBLIC_KEY)
  return encryptor.encrypt(data) || ''
}