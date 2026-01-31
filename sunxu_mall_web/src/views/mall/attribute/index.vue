<template>
  <div class="app-container">
    <!-- 工具栏 -->
    <div class="head-container">
      <el-input
        v-model="query.name"
        clearable
        size="small"
        placeholder="输入属性名称搜索"
        style="width: 200px;"
        class="filter-item"
        @keyup.enter="handleSearch"
      />
      <el-button type="primary" size="small" :icon="Search" @click="handleSearch">搜索</el-button>
      <el-button type="info" size="small" :icon="Refresh" @click="resetAndFetch">刷新</el-button>
      <el-button type="primary" size="small" :icon="Plus" @click="handleAdd">新增属性</el-button>
    </div>

    <!-- 属性主表 -->
    <el-table v-loading="loading" :data="tableData" border style="width: 100%">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="name" label="属性名称" min-width="120" />
      <el-table-column prop="createUserName" label="创建人" width="120" />
      <el-table-column prop="createTime" label="创建日期" width="180">
        <template #default="scope">
          {{ formatTime(scope.row.createTime) }}
        </template>
      </el-table-column>
      <el-table-column prop="updateUserName" label="修改人" width="120" />
      <el-table-column prop="updateTime" label="修改日期" width="180">
        <template #default="scope">
          {{ formatTime(scope.row.updateTime) }}
        </template>
      </el-table-column>

      <el-table-column label="操作" width="220" fixed="right">
        <template #default="scope">
          <el-button type="primary" size="small" :icon="Edit" @click.stop="handleEdit(scope.row)" />
          <el-button type="danger" size="small" :icon="Delete" @click.stop="handleDelete(scope.row.id)" />
          <el-button type="info" size="small" @click.stop="handleManageValues(scope.row)">管理属性值</el-button>
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

    <!-- 属性名编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="400px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="80px">
        <el-form-item label="属性名称" prop="name">
          <el-input v-model="form.name" placeholder="如：颜色、尺寸" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确认</el-button>
      </template>
    </el-dialog>

    <!-- 属性值管理弹窗 -->
    <el-dialog v-model="valueDialogVisible" title="管理属性值" width="600px">
      <AttributeValueManager 
        :attribute-id="currentAttributeId" 
        :attribute-name="currentAttributeName"
      />
    </el-dialog>

  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus, Edit, Delete, Refresh } from '@element-plus/icons-vue'
import * as attributeApi from '@/api/attribute'
import type { AttributeQueryDTO, CreateAttributeDTO, UpdateAttributeDTO, AttributeVO } from '@/types'
import PaginationBar from '@/components/PaginationBar.vue'
import AttributeValueManager from './components/AttributeValueManager.vue'

// 表格数据
const loading = ref(false)
const tableData = ref<AttributeVO[]>([])
const hasNext = ref(true)
const currentPage = ref(1)
const maxPageReached = ref(1)

// 查询参数
const query = reactive<AttributeQueryDTO>({
  pageNum: 1,
  pageSize: 10,
  name: '',
  cursorDirection: 'NEXT',
  cursorToken: undefined
})

// 弹窗状态
const dialogVisible = ref(false)
const dialogTitle = ref('新增属性')
const formRef = ref()
const form = reactive({ id: undefined as number | undefined, name: '' })
const rules = { name: [{ required: true, message: '请输入属性名称', trigger: 'blur' }] }

// 属性值管理弹窗状态
const valueDialogVisible = ref(false)
const currentAttributeId = ref<number>(0)
const currentAttributeName = ref<string>('')

onMounted(() => fetchData())

// 获取数据（双向游标）
const fetchData = async () => {
  loading.value = true
  try {
    query.pageNum = currentPage.value
    const res = await attributeApi.searchByBidirectionalCursor(query)
    tableData.value = res.list || []
    hasNext.value = res.hasNext || false
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
    ElMessage.error('获取数据失败')
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  currentPage.value = 1
  maxPageReached.value = 1
  query.cursorToken = undefined
  query.cursorDirection = 'NEXT'
  fetchData()
}

// 重置并刷新
const resetAndFetch = () => {
  currentPage.value = 1
  maxPageReached.value = 1
  query.cursorToken = undefined
  query.cursorDirection = 'NEXT'
  query.name = ''
  fetchData()
}

// 分页控制
const goToPrevPage = () => {
  if (currentPage.value <= 1) return
  currentPage.value--
  query.cursorDirection = 'PREV'
  fetchData()
}

const goToNextPage = () => {
  if (!hasNext.value) return
  currentPage.value++
  query.cursorDirection = 'NEXT'
  fetchData()
}

const goToPage = (pageNum: number) => {
  if (pageNum < 1 || pageNum > maxPageReached.value) {
    ElMessage.warning(`只能跳转到已访问页（1 ~ ${maxPageReached.value}）`)
    return
  }
  currentPage.value = pageNum
  query.cursorDirection = 'NEXT'
  fetchData()
}

const handleAdd = () => {
  dialogTitle.value = '新增属性'
  form.id = undefined
  form.name = ''
  dialogVisible.value = true
}

const handleEdit = (row: AttributeVO) => {
  dialogTitle.value = '编辑属性'
  form.id = row.id
  form.name = row.name
  dialogVisible.value = true
}

const handleManageValues = (row: AttributeVO) => {
  currentAttributeId.value = row.id
  currentAttributeName.value = row.name
  valueDialogVisible.value = true
}

const handleSubmit = async () => {
  await formRef.value.validate(async (valid: boolean) => {
    if (valid) {
      try {
        if (form.id) {
          await attributeApi.edit(form as UpdateAttributeDTO)
          ElMessage.success('更新成功')
        } else {
          await attributeApi.add(form as CreateAttributeDTO)
          ElMessage.success('新增成功')
        }
        dialogVisible.value = false
        fetchData()
      } catch (error) {
        ElMessage.error('操作失败')
      }
    }
  })
}

const handleDelete = (id: number) => {
  ElMessageBox.confirm('确认删除吗?', '警告', { type: 'warning' }).then(async () => {
    await attributeApi.del([id])
    ElMessage.success('删除成功')
    fetchData()
  })
}

const formatTime = (time: string) => time ? time.replace('T', ' ') : ''
</script>

<style scoped>
.app-container { padding: 20px; }
.head-container { margin-bottom: 20px; display: flex; gap: 10px; }
</style>