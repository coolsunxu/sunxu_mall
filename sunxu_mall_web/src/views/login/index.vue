<template>
  <div class="login-container">
    <div class="login-form">
      <h1>孙旭商城管理系统</h1>
      <el-form :model="form">
        <el-form-item>
          <el-input v-model="form.username" placeholder="用户名" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" type="password" placeholder="密码" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.code" placeholder="验证码" style="width: 60%" />
          <img :src="captchaUrl" @click="getCaptcha" alt="验证码" style="width: 35%; cursor: pointer; margin-left: 5%; height: 38px; vertical-align: middle;" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleLogin" style="width: 100%" :loading="loading">
            登录
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../../stores/user'
import { useMenuStore } from '../../stores/menu'
import request from '../../utils/request'
import { encrypt } from '../../utils/rsa'

const router = useRouter()
const userStore = useUserStore()
const menuStore = useMenuStore()
const loading = ref(false)
const captchaUrl = ref<string>('')
const uuid = ref<string>('')

const form = reactive({
  username: 'admin',
  password: '123456',
  code: ''
})

type CaptchaEntity = {
  uuid: string
  img: string
}

// 获取验证码
const getCaptcha = async () => {
  try {
    console.log('🔄 开始获取验证码...')
    const response = await request.get<CaptchaEntity>('/web/user/code')
    console.log('✅ 获取验证码成功:', response)
    // 后端返回 CaptchaEntity { uuid, img }
    // img 字段是 base64 字符串，需要添加 data URL 前缀
    const imgBase64 = response.img
    if (imgBase64 && !imgBase64.startsWith('data:')) {
      captchaUrl.value = `data:image/png;base64,${imgBase64}`
    } else {
      captchaUrl.value = imgBase64
    }
    uuid.value = response.uuid
    console.log('✅ 验证码已设置，UUID:', uuid.value)
    console.log('✅ 验证码图片URL长度:', captchaUrl.value.length)
  } catch (error: any) {
    console.error('❌ 获取验证码失败:', error)
    ElMessage.error(error?.message || '获取验证码失败，请检查网络连接和后端服务')
  }
}

const handleLogin = async () => {
  console.log('登录按钮被点击')
  console.log('表单数据:', form)
  console.log('uuid:', uuid.value)
  try {
    loading.value = true
    console.log('开始调用登录接口')
    // 使用RSA加密密码后再发送
    const encryptedPassword = encrypt(form.password)
    console.log('密码已加密')
    // 调用用户状态管理中的登录方法
    const loginResult = await userStore.login({
      uuid: uuid.value,
      username: form.username,
      password: encryptedPassword,
      code: form.code
    })
    console.log('登录接口调用成功:', loginResult)
    
    // 登录成功后调用菜单接口获取菜单树
    console.log('开始调用菜单接口')
    const menuResult = await menuStore.getMenuTree()
    console.log('菜单接口调用成功:', menuResult)
    
    // 跳转到首页
    console.log('开始跳转到首页')
    router.push('/')
  } catch (error: any) {
    console.error('❌ 登录失败:', error)
    ElMessage.error(error?.message || '登录失败，请检查用户名、密码和验证码')
    // 重新获取验证码
    getCaptcha()
  } finally {
    loading.value = false
    console.log('登录流程结束')
  }
}

// 页面加载时获取验证码
onMounted(() => {
  getCaptcha()
})
</script>

<style scoped>
.login-container {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-form {
  width: 400px;
  padding: 40px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  text-align: center;
}

h1 {
  margin-bottom: 30px;
  color: #333;
}
</style>