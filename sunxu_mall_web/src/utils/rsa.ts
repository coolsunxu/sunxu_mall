import JSEncrypt from 'jsencrypt'

// RSA公钥 - 从后端application.yaml获取
const PUBLIC_KEY = `-----BEGIN PUBLIC KEY-----
MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQC1aBCsVhTEiAynIOu7Peu6qJZg8HBmTdRWtb1ejzMzTQDAljQfQsSwWzgp1ghewGdWgmfqP3BwBFMO0cIjrJitnDm3JA2SAzjcnvJn3jmggeN6ESbkWtgK2WYsFO7pTVZX3U+3EKoG9eQbx7qkAwxbcUJ+i4/yoMNdfTEk+8dN/QIDAQAB
-----END PUBLIC KEY-----`

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