<template>
  <div class="app-container">
    <div class="head-container">
      <!-- 搜索栏 -->
      <div class="filter-item">
        <el-input
          v-model="query.name"
          clearable
          size="small"
          placeholder="输入商品名称搜索"
          style="width: 200px;"
          class="filter-item"
          @keyup.enter="handleSearch"
        />
        <el-input
          v-model="query.model"
          clearable
          size="small"
          placeholder="输入型号搜索"
          style="width: 200px; margin-left: 10px;"
          class="filter-item"
          @keyup.enter="handleSearch"
        />
        <el-button class="filter-item" size="small" type="success" icon="Search" @click="handleSearch" style="margin-left: 10px;">搜索</el-button>
        <el-button class="filter-item" size="small" type="info" icon="Refresh" @click="fetchData">刷新</el-button>
      </div>
    </div>

    <!-- 表格 -->
    <el-table
      v-loading="loading"
      :data="tableData"
      style="width: 100%; margin-top: 20px;"
    >
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="name" label="商品名称" />
      <el-table-column prop="model" label="型号" />
      <el-table-column prop="quantity" label="库存" width="100" />
      <el-table-column prop="remainQuantity" label="剩余库存" width="100" />
      <el-table-column prop="price" label="价格" width="100">
        <template #default="scope">
          ¥{{ scope.row.price }}
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="180">
        <template #default="scope">
          {{ formatTime(scope.row.createTime) }}
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="pagination-container">
      <el-pagination
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        :total="total"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { searchProductByPage } from '@/api/product'
import type { ProductVO, ProductQueryDTO } from '@/types'

// 查询参数
const query = reactive<ProductQueryDTO>({
  pageNum: 1,
  pageSize: 10,
  name: '',
  model: '',
  categoryId: undefined,
  brandId: undefined,
  productGroupId: undefined
})

// 表格数据
const loading = ref(false)
const tableData = ref<ProductVO[]>([])
const total = ref(0)

// 获取数据
const fetchData = async () => {
  loading.value = true
  try {
    const res = await searchProductByPage(query)
    tableData.value = res.list
    total.value = res.total
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

// 格式化时间
const formatTime = (time: string) => {
  return time ? time.replace('T', ' ') : ''
}

// 搜索
const handleSearch = () => {
  query.pageNum = 1
  fetchData()
}

// 分页
const handleSizeChange = (val: number) => {
  query.pageSize = val
  fetchData()
}
const handleCurrentChange = (val: number) => {
  query.pageNum = val
  fetchData()
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.app-container {
  padding: 20px;
}
.head-container {
  margin-bottom: 20px;
}
.filter-item {
  display: inline-block;
  vertical-align: middle;
  margin-bottom: 10px;
}
.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
