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
      <el-button type="primary" size="small" :icon="Plus" @click="handleAdd">新增属性</el-button>
    </div>

    <!-- 属性主表 -->
    <el-table v-loading="loading" :data="dataList" border style="width: 100%">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="name" label="属性名称" min-width="120" />
      
      <!-- 属性值预览 -->
      <el-table-column label="属性值预览" min-width="250">
        <template #default="scope">
          <div v-if="scope.row.values && scope.row.values.length">
            <el-tag
              v-for="val in scope.row.values.slice(0, 5)"
              :key="val.id"
              size="small"
              class="margin-right-5"
              style="margin-right: 5px; margin-bottom: 5px;"
            >
              {{ val.value }}
            </el-tag>
            <span v-if="scope.row.values.length > 5" style="color: #999; font-size: 12px;">...</span>
          </div>
          <span v-else style="color: #ccc; font-size: 12px;">暂无值</span>
        </template>
      </el-table-column>

      <el-table-column prop="createUserName" label="创建人" width="120" />
      <el-table-column prop="createTime" label="创建日期" width="180">
        <template #default="scope">
          {{ formatTime(scope.row.createTime) }}
        </template>
      </el-table-column>

      <el-table-column label="操作" width="220" fixed="right">
        <template #default="scope">
          <el-button type="primary" size="small" :icon="Edit" @click.stop="handleEdit(scope.row)" />
          <el-button type="danger" size="small" :icon="Delete" @click.stop="handleDelete(scope.row.id)" />
        </template>
      </el-table-column>
    </el-table>

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

  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus, Edit, Delete } from '@element-plus/icons-vue'
import * as attributeApi from '@/api/attribute'
import type { AttributeQueryDTO, CreateAttributeDTO, UpdateAttributeDTO } from '@/types'

const loading = ref(false)
const dataList = ref<any[]>([])
const query = reactive<AttributeQueryDTO>({
  pageNum: 1,
  pageSize: 100,
  name: ''
})

// 弹窗状态
const dialogVisible = ref(false)
const dialogTitle = ref('新增属性')
const formRef = ref()
const form = reactive({ id: undefined as number | undefined, name: '' })
const rules = { name: [{ required: true, message: '请输入属性名称', trigger: 'blur' }] }

onMounted(() => fetchData())

const fetchData = async () => {
  loading.value = true
  try {
    const res = await attributeApi.getAllWithValues()
    dataList.value = res
  } catch (error) {
    ElMessage.error('获取数据失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => fetchData()

const handleAdd = () => {
  dialogTitle.value = '新增属性'
  form.id = undefined
  form.name = ''
  dialogVisible.value = true
}

const handleEdit = (row: any) => {
  dialogTitle.value = '编辑属性'
  form.id = row.id
  form.name = row.name
  dialogVisible.value = true
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
