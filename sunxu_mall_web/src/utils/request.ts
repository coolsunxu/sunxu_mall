import axios from 'axios'
import { useUserStore } from '../stores/user'

// 创建axios实例
const service = axios.create({
  baseURL: '/api', // 使用相对路径，配合vite代理
  timeout: 10000, // 请求超时时间
})

// 请求拦截器
service.interceptors.request.use(
  (config) => {
    const fullUrl = `${config.baseURL}${config.url}`
    console.group(`🚀 发送请求 [${config.method?.toUpperCase()}]`)
    console.log('完整URL:', fullUrl)
    console.log('请求参数:', config.params)
    console.log('请求体:', config.data)
    console.log('请求头:', config.headers)
    console.groupEnd()
    
    // 从本地存储获取token
    const token = localStorage.getItem('token')
    if (token) {
      // 将token添加到请求头
      config.headers.Authorization = `Bearer ${token}`
      console.log('✅ 已添加Token到请求头')
    }
    return config
  },
  (error) => {
    // 请求错误处理
    console.error('❌ 请求拦截器错误:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  (response) => {
    const fullUrl = `${response.config.baseURL}${response.config.url}`
    console.group(`✅ 请求成功 [${response.status}]`)
    console.log('完整URL:', fullUrl)
    console.log('响应数据:', response.data)
    console.groupEnd()
    
    // 2xx范围内的状态码都会触发该函数
    const apiResult = response.data
    // 后端 ApiResult.OK = HttpStatus.OK.value() = 200
    if (apiResult.code === 200) {
      // 请求成功，返回data字段
      return apiResult.data
    } else {
      // 请求失败，抛出错误
      console.error('❌ 业务错误:', apiResult.message || '请求失败', '错误码:', apiResult.code)
      return Promise.reject(new Error(apiResult.message || '请求失败'))
    }
  },
  (error) => {
    const fullUrl = error.config ? `${error.config.baseURL}${error.config.url}` : '未知URL'
    console.group(`❌ 请求失败`)
    console.log('完整URL:', fullUrl)
    console.log('错误状态码:', error.response?.status)
    console.log('错误信息:', error.message)
    console.log('错误详情:', error.response?.data)
    console.log('完整错误对象:', error)
    console.groupEnd()
    
    // 超出2xx范围的状态码都会触发该函数
    // 处理网络错误（没有响应）
    if (!error.response) {
      console.error('❌ 网络错误：无法连接到服务器，请检查：')
      console.error('1. 后端服务是否启动 (http://localhost:8011)')
      console.error('2. Vite 代理配置是否正确')
      console.error('3. 浏览器控制台是否有 CORS 错误')
    }
    
    // 处理token过期等错误
    if (error.response?.status === 401) {
      console.warn('⚠️ Token已过期，清除本地Token并跳转登录页')
      // 清除本地存储的token
      localStorage.removeItem('token')
      window.location.href = '/login'
    }
    // 处理ApiResult格式的错误响应
    if (error.response?.data?.code && error.response?.data?.message) {
      return Promise.reject(new Error(error.response.data.message))
    }
    return Promise.reject(error)
  }
)

export default service