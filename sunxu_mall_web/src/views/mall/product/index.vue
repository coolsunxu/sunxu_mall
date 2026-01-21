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
        <el-button class="filter-item" size="small" type="info" icon="Refresh" @click="resetAndFetch">刷新</el-button>
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

    <!-- 分页控制 -->
    <PaginationBar
      v-if="tableData.length > 0"
      :current-page="currentPage"
      :max-page-reached="maxPageReached"
      :has-next="hasNext"
      :loading="loading"
      @prev="goToPrevPage"
      @next="goToNextPage"
      @jump="goToPage"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { searchProductByBidirectionalCursor } from '@/api/product'
import PaginationBar from '@/components/PaginationBar.vue'
import type { ProductVO, ProductQueryDTO } from '@/types'

// 查询参数
const query = reactive<ProductQueryDTO>({
  pageNum: 1,
  pageSize: 10,
  name: '',
  model: '',
  categoryId: undefined,
  brandId: undefined,
  productGroupId: undefined,
  cursorId: undefined,
  cursorDirection: 'NEXT',
  cursorToken: undefined
})

// 表格数据
const loading = ref(false)
const tableData = ref<ProductVO[]>([])
const hasNext = ref(true)
const currentPage = ref(1)
const maxPageReached = ref(1) // 记录到达的最大页码

// 获取数据（双向游标）
const fetchData = async () => {
  loading.value = true
  try {
    query.pageNum = currentPage.value
    const res = await searchProductByBidirectionalCursor(query)
    tableData.value = res.list
    hasNext.value = res.hasNext
    query.cursorToken = res.cursorToken
    
    // 如果后端返回了当前页码，则使用它
    if (res.currentPageNum) {
      currentPage.value = res.currentPageNum
    }
    
    // 更新最大页码
    if (currentPage.value > maxPageReached.value) {
      maxPageReached.value = currentPage.value
    }
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

// 重置并获取数据
const resetAndFetch = () => {
  currentPage.value = 1
  maxPageReached.value = 1
  query.cursorToken = undefined
  query.cursorDirection = 'NEXT'
  fetchData()
}

// 上一页
const goToPrevPage = () => {
  if (currentPage.value <= 1) return
  currentPage.value--
  query.cursorDirection = 'PREV'
  fetchData()
}

// 下一页
const goToNextPage = () => {
  if (!hasNext.value) return
  currentPage.value++
  query.cursorDirection = 'NEXT'
  fetchData()
}

// 跳转到指定页（仅限已访问页，不清空 cursorToken）
const goToPage = (pageNum: number) => {
  if (pageNum < 1 || pageNum > maxPageReached.value) {
    ElMessage.warning(`只能跳转到已访问页（1 ~ ${maxPageReached.value}）`)
    return
  }
  currentPage.value = pageNum
  query.cursorDirection = 'NEXT'
  // 不清空 cursorToken，后端依赖它定位到已访问的页
  fetchData()
}

// 格式化时间
const formatTime = (time: string) => {
  return time ? time.replace('T', ' ') : ''
}

// 搜索
const handleSearch = () => {
  currentPage.value = 1
  maxPageReached.value = 1
  query.cursorToken = undefined
  query.cursorDirection = 'NEXT'
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
</style>
