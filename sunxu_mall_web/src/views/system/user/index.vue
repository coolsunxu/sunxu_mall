<template>
  <div class="app-container">
    <div class="head-container">
      <!-- 搜索栏 -->
      <div class="filter-item">
        <el-input
          v-model="query.userName"
          clearable
          size="small"
          placeholder="输入用户名搜索"
          style="width: 200px;"
          class="filter-item"
          @keyup.enter="handleSearch"
        />
        <el-input
          v-model="query.phone"
          clearable
          size="small"
          placeholder="输入电话搜索"
          style="width: 200px; margin-left: 10px;"
          class="filter-item"
          @keyup.enter="handleSearch"
        />
        <el-input
          v-model="query.email"
          clearable
          size="small"
          placeholder="输入邮箱搜索"
          style="width: 200px; margin-left: 10px;"
          class="filter-item"
          @keyup.enter="handleSearch"
        />
        <el-button class="filter-item" size="small" type="success" icon="Search" @click="handleSearch" style="margin-left: 10px;">搜索</el-button>
        <el-button class="filter-item" size="small" type="primary" icon="Plus" @click="handleAdd">新增</el-button>
        <el-button class="filter-item" size="small" type="danger" icon="Delete" :disabled="selections.length === 0" @click="handleBatchDelete">批量删除</el-button>
        <el-button class="filter-item" size="small" type="warning" icon="RefreshLeft" :disabled="selections.length === 0" @click="handleResetPwd">重置密码</el-button>
        <el-button class="filter-item" size="small" type="success" icon="Download" @click="handleExport">导出</el-button>
        <el-button class="filter-item" size="small" type="info" icon="Refresh" @click="resetAndFetch">刷新</el-button>
      </div>
    </div>

    <!-- 表格 -->
    <el-table
      v-loading="loading"
      :data="tableData"
      style="width: 100%; margin-top: 20px;"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="55" />
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="userName" label="用户名" />
      <el-table-column prop="nickName" label="昵称" />
      <el-table-column prop="sex" label="性别" width="80">
        <template #default="scope">
          <span v-if="scope.row.sex === 1">男</span>
          <span v-else-if="scope.row.sex === 2">女</span>
          <span v-else>未知</span>
        </template>
      </el-table-column>
      <el-table-column prop="phone" label="电话" />
      <el-table-column prop="email" label="邮箱" />
      <el-table-column prop="deptId" label="部门ID" width="100" />
      <el-table-column prop="validStatus" label="状态" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.validStatus ? 'success' : 'danger'">
            {{ scope.row.validStatus ? '激活' : '锁定' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="180">
        <template #default="scope">
          {{ formatTime(scope.row.createTime) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150" align="center" fixed="right">
        <template #default="scope">
          <el-button size="small" type="primary" icon="Edit" @click="handleEdit(scope.row)" />
          <el-button size="small" type="danger" icon="Delete" @click="handleDelete(scope.row)" />
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

    <!-- 表单弹窗 -->
    <el-dialog
      :title="dialogTitle"
      v-model="dialogVisible"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="用户名" prop="userName" v-if="!form.id">
          <el-input v-model="form.userName" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="昵称" prop="nickName">
          <el-input v-model="form.nickName" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="密码" prop="password" v-if="!form.id">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" />
        </el-form-item>
        <el-form-item label="电话" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入电话" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="性别" prop="sex">
          <el-radio-group v-model="form.sex">
            <el-radio :label="1">男</el-radio>
            <el-radio :label="2">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="状态" prop="validStatus">
          <el-radio-group v-model="form.validStatus">
            <el-radio :label="true">激活</el-radio>
            <el-radio :label="false">锁定</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="部门ID" prop="deptId">
          <el-input-number v-model="form.deptId" :min="1" placeholder="请输入部门ID" style="width: 100%" />
        </el-form-item>
        <el-form-item label="岗位ID" prop="jobId">
          <el-input-number v-model="form.jobId" :min="1" placeholder="请输入岗位ID" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确认</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  searchUserByBidirectionalCursor, 
  insertUser, 
  updateUser, 
  deleteUserByIds, 
  resetUserPwd,
  exportUser
} from '@/api/user'
import { encrypt } from '@/utils/rsa'
import PaginationBar from '@/components/PaginationBar.vue'
import type { UserVO, UserQueryDTO, UserCreateDTO, UserUpdateDTO } from '@/types'

// 查询参数
const query = reactive<UserQueryDTO>({
  pageSize: 10,
  userName: '',
  phone: '',
  email: '',
  validStatus: undefined,
  deptId: undefined,
  cursorId: undefined,
  cursorDirection: 'NEXT',
  cursorToken: undefined,
  pageNum: 1
})

// 表格数据
const loading = ref(false)
const tableData = ref<UserVO[]>([])
const selections = ref<UserVO[]>([])
const hasNext = ref(true)
const currentPage = ref(1)
const maxPageReached = ref(1)

// 表单相关
const dialogVisible = ref(false)
const dialogTitle = ref('')
const submitLoading = ref(false)
const formRef = ref()
const form = reactive<any>({
  id: undefined,
  userName: '',
  nickName: '',
  password: '',
  phone: '',
  email: '',
  sex: 1,
  validStatus: true,
  deptId: undefined,
  jobId: undefined,
  version: undefined
})

const rules = {
  userName: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  nickName: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

// 获取数据
const fetchData = async () => {
  loading.value = true
  try {
    query.pageNum = currentPage.value
    const res = await searchUserByBidirectionalCursor(query)
    tableData.value = res.list
    query.cursorToken = res.cursorToken
    hasNext.value = res.hasNext
    
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

// 多选
const handleSelectionChange = (val: UserVO[]) => {
  selections.value = val
}

// 新增
const handleAdd = () => {
  dialogTitle.value = '新增用户'
  form.id = undefined
  form.userName = ''
  form.nickName = ''
  form.password = ''
  form.phone = ''
  form.email = ''
  form.sex = 1
  form.validStatus = true
  form.deptId = undefined
  form.jobId = undefined
  form.version = undefined
  dialogVisible.value = true
}

// 编辑
const handleEdit = (row: UserVO) => {
  dialogTitle.value = '编辑用户'
  form.id = row.id
  form.userName = row.userName
  form.nickName = row.nickName
  form.password = '' // 编辑时不显示密码
  form.phone = row.phone
  form.email = row.email
  form.sex = row.sex
  form.validStatus = row.validStatus
  form.deptId = row.deptId
  form.jobId = row.jobId
  // 注意：实际开发中后端应返回version用于乐观锁，此处假设row中有或需要重新查询
  // form.version = row.version 
  dialogVisible.value = true
}

// 提交表单
const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid: boolean) => {
    if (valid) {
      submitLoading.value = true
      try {
        if (form.id) {
          const updateData: UserUpdateDTO = {
            id: form.id,
            nickName: form.nickName,
            phone: form.phone,
            email: form.email,
            sex: form.sex,
            validStatus: form.validStatus,
            deptId: form.deptId,
            jobId: form.jobId,
            version: form.version
          }
          await updateUser(updateData)
          ElMessage.success('更新成功')
        } else {
          const createData: UserCreateDTO = {
            userName: form.userName,
            password: encrypt(form.password),
            nickName: form.nickName,
            phone: form.phone,
            email: form.email,
            sex: form.sex,
            validStatus: form.validStatus,
            deptId: form.deptId,
            jobId: form.jobId
          }
          await insertUser(createData)
          ElMessage.success('新增成功')
        }
        dialogVisible.value = false
        fetchData()
      } catch (error) {
        console.error(error)
      } finally {
        submitLoading.value = false
      }
    }
  })
}

// 删除
const handleDelete = (row: UserVO) => {
  ElMessageBox.confirm('确认删除该用户吗?', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    await deleteUserByIds([row.id])
    ElMessage.success('删除成功')
    fetchData()
  })
}

// 批量删除
const handleBatchDelete = () => {
  ElMessageBox.confirm(`确认删除选中的 ${selections.value.length} 个用户吗?`, '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    const ids = selections.value.map(item => item.id)
    await deleteUserByIds(ids)
    ElMessage.success('批量删除成功')
    fetchData()
  })
}

// 重置密码
const handleResetPwd = () => {
  ElMessageBox.confirm(`确认重置选中的 ${selections.value.length} 个用户的密码吗?`, '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    const ids = selections.value.map(item => item.id)
    await resetUserPwd(ids)
    ElMessage.success('重置密码成功，新密码为 123456')
  })
}

// 导出
const handleExport = async () => {
  try {
    await exportUser(query)
    ElMessage.success('导出任务已提交，请留意右上角通知')
  } catch (error) {
    console.error(error)
  }
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
