<template>
  <div class="app-container">
    <!-- 工具栏 -->
    <div class="head-container">
      <el-select
        v-model="query.attributeId"
        clearable
        size="small"
        placeholder="所属属性"
        class="filter-item"
        style="width: 150px;"
        @change="handleSearch"
      >
        <el-option
          v-for="item in attributeOptions"
          :key="item.id"
          :label="item.name"
          :value="item.id"
        />
      </el-select>
      <el-input
        v-model="query.value"
        clearable
        size="small"
        placeholder="输入属性值搜索"
        style="width: 200px;"
        class="filter-item"
        @keyup.enter="handleSearch"
      />
      <el-button
        type="primary"
        size="small"
        class="filter-item"
        :icon="Search"
        @click="handleSearch"
      >
        搜索
      </el-button>
      <el-button
        type="info"
        size="small"
        class="filter-item"
        :icon="Refresh"
        @click="resetAndFetch"
      >
        刷新
      </el-button>
      <el-button
        type="primary"
        size="small"
        class="filter-item"
        :icon="Plus"
        @click="handleAdd"
      >
        新增属性值
      </el-button>
    </div>

    <!-- 表格 -->
    <el-table
      v-loading="loading"
      :data="dataList"
      border
      style="width: 100%"
    >
      <el-table-column
        type="selection"
        width="55"
        align="center"
      />
      <el-table-column
        prop="id"
        label="ID"
        width="80"
      />
      <el-table-column
        prop="attributeName"
        label="所属属性"
        min-width="120"
      >
        <template #default="scope">
          <span>{{ scope.row.attributeName }}</span>
          <span style="color: #999; font-size: 12px; margin-left: 5px;">({{ scope.row.attributeId }})</span>
        </template>
      </el-table-column>
      <el-table-column
        prop="value"
        label="属性值"
        min-width="120"
      />
      <el-table-column
        prop="sort"
        label="排序"
        width="80"
      />
      <el-table-column
        prop="createUserName"
        label="创建人"
        width="120"
      />
      <el-table-column
        prop="createTime"
        label="创建日期"
        width="180"
      >
        <template #default="scope">
          {{ formatTime(scope.row.createTime) }}
        </template>
      </el-table-column>
      <el-table-column
        prop="updateUserName"
        label="修改人"
        width="120"
      />
      <el-table-column
        prop="updateTime"
        label="修改日期"
        width="180"
      >
        <template #default="scope">
          {{ formatTime(scope.row.updateTime) }}
        </template>
      </el-table-column>
      <el-table-column
        label="操作"
        width="150"
        fixed="right"
      >
        <template #default="scope">
          <el-button
            type="primary"
            size="small"
            @click="handleEdit(scope.row)"
          >
            编辑
          </el-button>
          <el-button
            type="danger"
            size="small"
            @click="handleDelete(scope.row.id)"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 游标分页控制 -->
    <PaginationBar
      v-if="dataList.length > 0"
      :current-page="currentPage"
      :max-page-reached="maxPageReached"
      :has-next="hasNext"
      :loading="loading"
      @prev="goToPrevPage"
      @next="goToNextPage"
      @jump="goToPage"
    />

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="400px"
    >
      <el-form
        :model="form"
        :rules="rules"
        ref="formRef"
        label-width="80px"
      >
        <el-form-item label="所属属性" prop="attributeId">
          <el-select v-model="form.attributeId" placeholder="请选择属性" style="width: 100%">
            <el-option
              v-for="item in attributeOptions"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="属性值" prop="value">
          <el-input v-model="form.value" placeholder="请输入属性值" />
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="form.sort" :min="1" :max="999" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确认</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { Search, Plus, Refresh } from '@element-plus/icons-vue'
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as attributeValueApi from '@/api/attributeValue'
import * as attributeApi from '@/api/attribute'
import PaginationBar from '@/components/PaginationBar.vue'
import type { AttributeValueVO, AttributeValueQueryDTO, CreateAttributeValueDTO, UpdateAttributeValueDTO, AttributeVO } from '@/types'

// 列表相关
const loading = ref(false)
const dataList = ref<AttributeValueVO[]>([])
const attributeOptions = ref<AttributeVO[]>([])

// 分页相关
const currentPage = ref(1)
const maxPageReached = ref(1)
const hasNext = ref(false)

const query = reactive<AttributeValueQueryDTO>({
  pageNum: 1,
  pageSize: 10,
  attributeId: undefined,
  value: '',
  cursorToken: undefined,
  cursorDirection: 'NEXT'
})

// 表单相关
const dialogVisible = ref(false)
const dialogTitle = ref('新增属性值')
const submitLoading = ref(false)
const formRef = ref()
const form = reactive<any>({
  id: undefined,
  attributeId: undefined,
  value: '',
  sort: 999
})

const rules = {
  attributeId: [{ required: true, message: '请选择所属属性', trigger: 'change' }],
  value: [{ required: true, message: '请输入属性值', trigger: 'blur' }]
}

// 初始化
onMounted(() => {
  loadAttributes()
  fetchData()
})

// 加载属性下拉选项
const loadAttributes = async () => {
  try {
    const res = await attributeApi.getAll()
    attributeOptions.value = res
  } catch (error) {
    console.error('加载属性列表失败:', error)
  }
}

// 获取数据（双向游标分页）
const fetchData = async () => {
  loading.value = true
  try {
    query.pageNum = currentPage.value
    const res = await attributeValueApi.searchByBidirectionalCursor(query)
    dataList.value = res.list
    hasNext.value = res.hasNext
    query.cursorToken = res.cursorToken
    
    if (res.currentPageNum) {
      currentPage.value = res.currentPageNum
    }
    
    if (currentPage.value > maxPageReached.value) {
      maxPageReached.value = currentPage.value
    }
  } catch (error) {
    ElMessage.error('获取数据失败')
    console.error(error)
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
  query.value = ''
  query.attributeId = undefined
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

// 新增
const handleAdd = () => {
  dialogTitle.value = '新增属性值'
  Object.assign(form, {
    id: undefined,
    attributeId: undefined,
    value: '',
    sort: 999
  })
  dialogVisible.value = true
}

// 编辑
const handleEdit = (row: AttributeValueVO) => {
  dialogTitle.value = '编辑属性值'
  Object.assign(form, {
    id: row.id,
    attributeId: row.attributeId,
    value: row.value,
    sort: row.sort
  })
  dialogVisible.value = true
}

// 提交
const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid: boolean) => {
    if (valid) {
      submitLoading.value = true
      try {
        if (form.id) {
          await attributeValueApi.edit(form as UpdateAttributeValueDTO)
          ElMessage.success('更新成功')
        } else {
          await attributeValueApi.add(form as CreateAttributeValueDTO)
          ElMessage.success('新增成功')
        }
        dialogVisible.value = false
        handleSearch() // 提交后回到第一页
      } catch (error: any) {
        ElMessage.error(error.message || '操作失败')
      } finally {
        submitLoading.value = false
      }
    }
  })
}

// 删除
const handleDelete = async (id: number) => {
  ElMessageBox.confirm('确认删除该属性值吗?', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await attributeValueApi.del([id])
      ElMessage.success('删除成功')
      fetchData()
    } catch (error: any) {
      ElMessage.error(error.message || '删除失败')
    }
  })
}

// 时间格式化
const formatTime = (time: string) => {
  return time ? time.replace('T', ' ') : ''
}
</script>

<style scoped>
.app-container {
  padding: 20px;
}

.head-container {
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.filter-item {
  margin-right: 0; /* 使用 gap 代替 */
}

.dialog-footer {
  text-align: right;
}
</style>
