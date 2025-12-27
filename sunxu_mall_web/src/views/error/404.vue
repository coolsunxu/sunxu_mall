<template>
  <div class="error-page">
    <h1>404</h1>
    <h2>页面不存在</h2>
    <p>抱歉，您访问的页面不存在</p>
    <el-button type="primary" @click="goHome">返回首页</el-button>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

const goHome = () => {
  router.push('/')
}
</script>

<style scoped>
.error-page {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  text-align: center;
}

h1 {
  font-size: 120px;
  margin: 0;
}

h2 {
  font-size: 32px;
  margin: 20px 0;
}

p {
  font-size: 16px;
  margin-bottom: 30px;
}
</style>