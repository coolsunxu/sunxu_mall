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
        <el-button class="filter-item" size="small" type="warning" icon="Download" @click="handleExport" style="margin-left: 10px;">导出</el-button>
      </div>
    </div>

    <!-- 表格 -->
    <el-table
      v-loading="loading"
      :data="tableData"
      style="width: 100%; margin-top: 20px;"
    >
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="coverUrl" label="商品图片" width="100">
        <template #default="scope">
          <el-image
            v-if="scope.row.coverUrl"
            :src="scope.row.coverUrl"
            fit="cover"
            style="width: 80px; height: 80px; border: 1px solid #eee;"
            :preview-src-list="[scope.row.coverUrl]"
          >
            <template #error>
              <div class="image-slot">
                <el-icon><Picture /></el-icon>
                <span>加载失败</span>
              </div>
            </template>
          </el-image>
          <div v-else class="no-image" style="width: 80px; height: 80px; border: 1px solid #eee; display: flex; align-items: center; justify-content: center; color: #999;">
            <el-icon><Picture /></el-icon>
            <span style="margin-left: 5px;">暂无图片</span>
          </div>
        </template>
      </el-table-column>
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
      <el-table-column label="操作" width="120">
        <template #default="scope">
          <el-button type="primary" size="small" @click="handleEdit(scope.row)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      title="编辑商品"
      width="70%"
    >
      <el-form :model="form" label-width="100px" :rules="rules" ref="formRef">
        <el-tabs type="border-card">
          <!-- 基础信息页签 -->
          <el-tab-pane label="基础信息">
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="商品名称" prop="name">
                  <el-input v-model="form.name" placeholder="请输入商品名称"></el-input>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="型号" prop="model">
                  <el-input v-model="form.model" placeholder="请输入型号"></el-input>
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="分类" prop="categoryId">
                  <el-tree-select
                    v-model="form.categoryId"
                    :data="categoryOptions"
                    :props="{ label: 'name', value: 'id', children: 'children' }"
                    placeholder="请选择分类"
                    check-strictly
                    style="width: 100%"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="品牌" prop="brandId">
                  <el-select v-model="form.brandId" placeholder="请选择品牌" style="width: 100%" clearable>
                    <el-option v-for="item in brandOptions" :key="item.id" :label="item.name" :value="item.id" />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="单位" prop="unitId">
                  <el-select v-model="form.unitId" placeholder="请选择单位" style="width: 100%" clearable>
                    <el-option v-for="item in unitOptions" :key="item.id" :label="item.name" :value="item.id" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="商品组ID" prop="productGroupId">
                  <el-input-number v-model="form.productGroupId" :controls="false" placeholder="请输入商品组ID" style="width: 100%" />
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="价格" prop="price">
                  <el-input-number v-model="form.price" :min="0" :step="0.01" style="width: 100%" placeholder="请输入价格"></el-input-number>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="封面图片" prop="coverUrl">
                  <el-input v-model="form.coverUrl" placeholder="请输入图片URL地址">
                    <template #append>
                      <el-popover placement="top" :width="200" trigger="hover" v-if="form.coverUrl">
                        <template #reference>
                          <el-icon style="cursor: pointer"><Picture /></el-icon>
                        </template>
                        <el-image :src="form.coverUrl" fit="cover"></el-image>
                      </el-popover>
                    </template>
                  </el-input>
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="库存" prop="quantity">
                  <el-input-number v-model="form.quantity" :min="0" style="width: 100%" placeholder="请输入库存"></el-input-number>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="剩余库存" prop="remainQuantity">
                  <el-input-number v-model="form.remainQuantity" :min="0" style="width: 100%" placeholder="请输入剩余库存"></el-input-number>
                </el-form-item>
              </el-col>
            </el-row>
          </el-tab-pane>

          <!-- 规格属性页签 -->
          <el-tab-pane label="规格属性">
            <el-alert title="此处修改将采用差量更新模式，不勾选删除则视为更新或新增" type="info" :closable="false" style="margin-bottom: 10px;" />
            <el-table :data="form.skuAttributes" border style="width: 100%">
              <el-table-column label="属性ID" width="120">
                <template #default="scope">
                  <el-input-number v-model="scope.row.attributeId" :controls="false" size="small" />
                </template>
              </el-table-column>
              <el-table-column label="属性值ID">
                <template #default="scope">
                  <el-input-number v-model="scope.row.attributeValueId" :controls="false" size="small" />
                </template>
              </el-table-column>
              <el-table-column label="操作" width="100" align="center">
                <template #default="scope">
                  <el-checkbox v-model="scope.row.deleted" label="删除" />
                </template>
              </el-table-column>
            </el-table>
            <el-button type="dashed" style="width: 100%; margin-top: 10px;" @click="addAttrRow">
              <el-icon><Plus /></el-icon> 添加规格属性
            </el-button>
          </el-tab-pane>
        </el-tabs>
        
        <el-input v-model="form.version" type="hidden"></el-input>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleUpdate">确定</el-button>
        </span>
      </template>
    </el-dialog>

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
import { ElMessage, ElMessageBox } from 'element-plus'
import { Picture, Plus } from '@element-plus/icons-vue'
import { searchProductByBidirectionalCursor, updateProduct, getCategoryTree, getAllBrands, getAllUnits, getProductById, exportProducts } from '@/api/product'
import PaginationBar from '@/components/PaginationBar.vue'
import type { ProductVO, ProductQueryDTO, UpdateProductDTO } from '@/types'

// 基础数据选项
const categoryOptions = ref<any[]>([])
const brandOptions = ref<any[]>([])
const unitOptions = ref<any[]>([])

// 获取基础数据
const loadBaseOptions = async () => {
  try {
    const [categories, brands, units] = await Promise.all([
      getCategoryTree(),
      getAllBrands(),
      getAllUnits()
    ])
    categoryOptions.value = categories
    brandOptions.value = brands
    unitOptions.value = units
  } catch (error) {
    console.error('加载基础数据失败:', error)
  }
}

// 属性行操作
const addAttrRow = () => {
  if (!form.skuAttributes) {
    form.skuAttributes = []
  }
  form.skuAttributes.push({
    attributeId: 0,
    attributeValueId: 0,
    deleted: false
  })
}

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

// 编辑对话框
const dialogVisible = ref(false)
const formRef = ref()
const form = reactive<UpdateProductDTO & { id: number }>({
  id: 0,
  version: 0,
  name: '',
  model: '',
  price: 0,
  quantity: 0,
  remainQuantity: 0,
  categoryId: undefined,
  brandId: undefined,
  unitId: undefined,
  productGroupId: undefined,
  coverUrl: ''
})

// 表单规则
const rules = reactive({
  name: [
    { required: true, message: '请输入商品名称', trigger: 'blur' },
    { max: 200, message: '商品名称不能超过200个字符', trigger: 'blur' }
  ],
  price: [
    { required: true, message: '请输入价格', trigger: 'blur' }
  ],
  quantity: [
    { required: true, message: '请输入库存', trigger: 'blur' }
  ],
  remainQuantity: [
    { required: true, message: '请输入剩余库存', trigger: 'blur' }
  ]
})

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

// 处理编辑
const handleEdit = async (row: ProductVO) => {
  // 1. 先进行基础数据回显
  form.id = row.id
  form.version = row.version
  form.name = row.name
  form.model = row.model
  form.price = row.price
  form.quantity = row.quantity
  form.remainQuantity = row.remainQuantity
  form.categoryId = row.categoryId
  form.brandId = row.brandId
  form.unitId = row.unitId
  form.productGroupId = row.productGroupId
  form.coverUrl = row.coverUrl
  form.skuAttributes = [] // 清空旧数据

  // 2. 调用详情接口获取完整的 SKU 属性信息（后端 ProductDetailDTO 包含属性列表）
  try {
    const detail = await getProductById(row.id)
    if (detail && (detail as any).skuAttributeEntityList) {
      form.skuAttributes = (detail as any).skuAttributeEntityList.map((attr: any) => ({
        id: attr.id,
        attributeId: attr.attributeId,
        attributeValueId: attr.attributeValueId,
        deleted: false
      }))
    }
  } catch (error) {
    console.error('获取商品详情失败:', error)
  }
  
  // 打开对话框
  dialogVisible.value = true
}

// 处理更新
const handleUpdate = async () => {
  if (!formRef.value) return
  
  try {
    // 验证表单
    await formRef.value.validate()
    
    // 调用更新 API
    await updateProduct(form.id, form)
    
    // 更新成功
    ElMessage.success('更新成功')
    dialogVisible.value = false
    
    // 刷新数据
    fetchData()
  } catch (error: any) {
    // 处理表单验证错误
    if (error.name === 'Error') {
      // 处理 API 错误
      if (error.response?.status === 409) {
        ElMessageBox.alert('版本冲突，数据已被其他用户修改，请刷新页面后重试', '更新失败', {
          confirmButtonText: '确定',
          type: 'error'
        })
      } else if (error.response?.status === 404) {
        ElMessage.error('商品不存在')
      } else {
        ElMessage.error('更新失败：' + (error.response?.data?.message || error.message))
      }
    }
  }
}

// 处理导出
const handleExport = async () => {
  try {
    await exportProducts(query)
    ElMessage.success('导出请求已提交，将在后台异步处理，请稍后查看结果')
  } catch (error: any) {
    console.error('导出失败:', error)
    ElMessage.error('导出失败：' + (error.response?.data?.message || error.message))
  }
}

onMounted(() => {
  fetchData()
  loadBaseOptions()
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
