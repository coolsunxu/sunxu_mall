<template>
  <div class="dashboard">
    <h1>欢迎来到孙旭商城管理系统</h1>
    <p>基于Vue 3 + TypeScript + Element Plus构建</p>
    
    <el-row :gutter="20" class="mt-20">
      <el-col :span="6">
        <el-card>
          <h3>用户总数</h3>
          <p class="stat-value">1,234</p>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card>
          <h3>商品总数</h3>
          <p class="stat-value">567</p>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card>
          <h3>订单总数</h3>
          <p class="stat-value">890</p>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card>
          <h3>今日收入</h3>
          <p class="stat-value">¥12,345</p>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.dashboard {
  padding: 20px;
}

h1 {
  text-align: center;
  margin-bottom: 20px;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: #409eff;
  margin: 10px 0;
}

.mt-20 {
  margin-top: 20px;
}
</style>