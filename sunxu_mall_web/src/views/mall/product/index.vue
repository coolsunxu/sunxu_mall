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
        <el-button class="filter-item" size="small" type="primary" icon="Plus" @click="handleAdd" style="margin-left: 10px;">新增商品</el-button>
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
      <el-table-column label="操作" width="200">
        <template #default="scope">
          <el-button type="primary" size="small" @click="handleEdit(scope.row)">编辑</el-button>
          <el-button type="danger" size="small" @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑商品' : '新增商品'"
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
              <el-col :span="12" v-if="isEdit">
                <el-form-item label="商品组ID" prop="productGroupId">
                  <el-input-number v-model="form.productGroupId" :controls="false" placeholder="请输入商品组ID" style="width: 100%" disabled />
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
                      <el-upload
                        class="upload-demo"
                        action="#"
                        :auto-upload="true"
                        :show-file-list="false"
                        :http-request="handleCoverUpload"
                      >
                        <el-icon style="cursor: pointer"><Upload /></el-icon>
                      </el-upload>
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
              <el-col :span="12" v-if="isEdit">
                <el-form-item label="剩余库存" prop="remainQuantity">
                  <el-input-number v-model="form.remainQuantity" :min="0" style="width: 100%" placeholder="请输入剩余库存"></el-input-number>
                </el-form-item>
              </el-col>
            </el-row>
          </el-tab-pane>

          <!-- 规格属性页签 -->
          <el-tab-pane label="规格属性">
            <el-alert v-if="isEdit" title="此处修改将采用差量更新模式，不勾选删除则视为更新或新增" type="info" :closable="false" style="margin-bottom: 10px;" />
            
            <div v-if="!isEdit" style="margin-bottom: 20px;">
              <h4 style="margin: 0 0 10px 0;">SPU 属性 (用于确定商品组)</h4>
              <el-table :data="form.spuAttributes" border style="width: 100%">
                <el-table-column label="属性名称">
                  <template #default="scope">
                    <el-select v-model="scope.row.attributeId" placeholder="请选择属性" style="width: 100%" size="small">
                      <el-option v-for="item in attributeOptions" :key="item.id" :label="item.name" :value="item.id" />
                    </el-select>
                  </template>
                </el-table-column>
                <el-table-column label="属性值">
                  <template #default="scope">
                    <el-select v-model="scope.row.attributeValueId" placeholder="请选择值" style="width: 100%" size="small">
                      <el-option 
                        v-for="item in getAttrValues(scope.row.attributeId)" 
                        :key="item.id" 
                        :label="item.value" 
                        :value="item.id" 
                      />
                    </el-select>
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="80" align="center">
                  <template #default="scope">
                    <el-button type="danger" icon="Delete" circle size="small" @click="removeAttrRow(form.spuAttributes, scope.$index)" />
                  </template>
                </el-table-column>
              </el-table>
              <el-button type="dashed" style="width: 100%; margin-top: 10px;" @click="addAttrRow(form.spuAttributes!)">
                <el-icon><Plus /></el-icon> 添加 SPU 属性
              </el-button>
            </div>

            <h4 style="margin: 0 0 10px 0;">SKU 属性</h4>
            <el-table :data="form.skuAttributes" border style="width: 100%">
              <el-table-column label="属性名称">
                <template #default="scope">
                  <el-select v-model="scope.row.attributeId" placeholder="请选择属性" style="width: 100%" size="small" :disabled="isEdit && scope.row.id">
                    <el-option v-for="item in attributeOptions" :key="item.id" :label="item.name" :value="item.id" />
                  </el-select>
                </template>
              </el-table-column>
              <el-table-column label="属性值">
                <template #default="scope">
                  <el-select v-model="scope.row.attributeValueId" placeholder="请选择值" style="width: 100%" size="small" :disabled="isEdit && scope.row.id">
                    <el-option 
                      v-for="item in getAttrValues(scope.row.attributeId)" 
                      :key="item.id" 
                      :label="item.value" 
                      :value="item.id" 
                    />
                  </el-select>
                </template>
              </el-table-column>
              <el-table-column v-if="isEdit" label="状态" width="100" align="center">
                <template #default="scope">
                  <el-checkbox v-model="scope.row.deleted" label="删除" />
                </template>
              </el-table-column>
              <el-table-column v-else label="操作" width="80" align="center">
                <template #default="scope">
                  <el-button type="danger" icon="Delete" circle size="small" @click="removeAttrRow(form.skuAttributes!, scope.$index)" />
                </template>
              </el-table-column>
            </el-table>
            <el-button type="dashed" style="width: 100%; margin-top: 10px;" @click="addAttrRow(form.skuAttributes!)">
              <el-icon><Plus /></el-icon> 添加 SKU 属性
            </el-button>
          </el-tab-pane>

          <!-- 轮播图页签 -->
          <el-tab-pane label="轮播图片">
            <el-upload
              v-model:file-list="photoList"
              action="#"
              list-type="picture-card"
              :auto-upload="true"
              :http-request="handleSwiperUpload"
              :on-remove="handleRemovePhoto"
            >
              <el-icon><Plus /></el-icon>
            </el-upload>
          </el-tab-pane>

          <!-- 详情页签 -->
          <el-tab-pane label="详情描述">
            <el-input
              v-model="form.detail"
              type="textarea"
              :rows="15"
              placeholder="请输入商品详情描述（支持 HTML 格式）"
            />
          </el-tab-pane>
        </el-tabs>
        
        <el-input v-if="isEdit" v-model="form.version" type="hidden"></el-input>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSubmit">确定</el-button>
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
import { Picture, Plus, Upload } from '@element-plus/icons-vue'
import { 
  searchProductByBidirectionalCursor, 
  updateProduct, 
  createProduct,
  deleteProduct,
  getCategoryTree, 
  getAllBrands, 
  getAllUnits, 
  getAllAttributesWithValues,
  getProductById, 
  exportProducts,
  uploadFile
} from '@/api/product'
import PaginationBar from '@/components/PaginationBar.vue'
import type { 
  ProductVO, 
  ProductQueryDTO, 
  CategoryTree, 
  Brand, 
  Unit, 
  AttributeWithValues,
  CreateProductDTO
} from '@/types'

// el-upload 自定义上传回调参数（最小字段集即可满足当前用法）
type UploadRequestOption = {
  file: File
  onSuccess: (res: any, file: File) => void
  onError: (err: any) => void
}

// 基础数据选项
const categoryOptions = ref<CategoryTree[]>([])
const brandOptions = ref<Brand[]>([])
const unitOptions = ref<Unit[]>([])
const attributeOptions = ref<AttributeWithValues[]>([])

// 获取基础数据
const loadBaseOptions = async () => {
  try {
    const [categories, brands, units, attributes] = await Promise.all([
      getCategoryTree(),
      getAllBrands(),
      getAllUnits(),
      getAllAttributesWithValues()
    ])
    categoryOptions.value = categories
    brandOptions.value = brands
    unitOptions.value = units
    attributeOptions.value = attributes
  } catch (error) {
    console.error('加载基础数据失败:', error)
  }
}

// 根据属性ID获取其可选值
const getAttrValues = (attrId: number) => {
  const attr = attributeOptions.value.find(a => a.id === attrId)
  return attr ? attr.values : []
}

// 属性行操作
const addAttrRow = (list: any[]) => {
  list.push({
    attributeId: undefined,
    attributeValueId: undefined,
    deleted: false
  })
}

const removeAttrRow = (list: any[], index: number) => {
  list.splice(index, 1)
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

// 对话框状态
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref()
const photoList = ref<any[]>([])

// 综合表单（合并新增与编辑所需字段）
const initialForm = (): any => ({
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
  coverUrl: '',
  detail: '',
  spuAttributes: [],
  skuAttributes: [],
  photos: []
})

const form = reactive<any>(initialForm())

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
  categoryId: [
    { required: true, message: '请选择分类', trigger: 'change' }
  ],
  brandId: [
    { required: true, message: '请选择品牌', trigger: 'change' }
  ],
  unitId: [
    { required: true, message: '请选择单位', trigger: 'change' }
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

// 处理新增
const handleAdd = () => {
  isEdit.value = false
  Object.assign(form, initialForm())
  photoList.value = []
  dialogVisible.value = true
}

// 处理编辑
const handleEdit = async (row: ProductVO) => {
  isEdit.value = true
  Object.assign(form, initialForm())
  
  // 1. 基础数据回显
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
  form.skuAttributes = []

  // 2. 获取详情
  try {
    const detail = await getProductById(row.id)
    if (detail) {
      form.detail = (detail as any).detail || ''
      if ((detail as any).skuAttributeEntityList) {
        form.skuAttributes = (detail as any).skuAttributeEntityList.map((attr: any) => ({
          id: attr.id,
          attributeId: attr.attributeId,
          attributeValueId: attr.attributeValueId,
          deleted: false
        }))
      }
      // 轮播图回显
      if ((detail as any).swiperPhotoList) {
        photoList.value = (detail as any).swiperPhotoList.map((p: any) => ({
          name: p.fileName || 'photo',
          url: p.url
        }))
        form.photos = photoList.value.map(p => p.url)
      }
    }
  } catch (error) {
    console.error('获取商品详情失败:', error)
  }
  
  dialogVisible.value = true
}

// 处理上传
const handleUpload = async (option: UploadRequestOption, type: 'cover' | 'swiper') => {
  try {
    const res = await uploadFile(option.file)
    if (type === 'cover') {
      form.coverUrl = res.downloadUrl
      ElMessage.success('封面上传成功')
    } else {
      // 这里的 photoList 是 el-upload 自动维护的，我们只需要同步到 form.photos
      const url = res.downloadUrl
      // 更新当前上传的这一项的 url
      option.onSuccess(res, option.file)
      form.photos.push(url)
      ElMessage.success('图片上传成功')
    }
  } catch (error) {
    console.error('上传失败:', error)
    option.onError(error)
    ElMessage.error('上传失败')
  }
}

const handleCoverUpload = (option: UploadRequestOption) => handleUpload(option, 'cover')
const handleSwiperUpload = (option: UploadRequestOption) => handleUpload(option, 'swiper')

const handleRemovePhoto = (file: any) => {
  const url = file.url
  const index = form.photos.indexOf(url)
  if (index !== -1) {
    form.photos.splice(index, 1)
  }
}

// 删除商品
const handleDelete = async (row: ProductVO) => {
  try {
    await ElMessageBox.confirm(
      `确认删除商品「${row.name}」吗？删除后将不可在列表中看到。`,
      '删除确认',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    await deleteProduct(row.id)
    ElMessage.success('删除成功')
    // 删除后回到当前页刷新即可
    fetchData()
  } catch (error: any) {
    // 用户取消
    if (error === 'cancel' || error === 'close') return
    console.error('删除失败:', error)
    ElMessage.error('删除失败：' + (error.response?.data?.message || error.message))
  }
}

// 处理提交
const handleSubmit = async () => {
  if (!formRef.value) return
  
  try {
    await formRef.value.validate()
    
    if (isEdit.value) {
      await updateProduct(form.id, form)
      ElMessage.success('更新成功')
    } else {
      // 过滤未选择的属性
      const submitData: CreateProductDTO = {
        ...form,
        spuAttributes: form.spuAttributes.filter((a: any) => a.attributeId && a.attributeValueId),
        skuAttributes: form.skuAttributes.filter((a: any) => a.attributeId && a.attributeValueId)
      }
      await createProduct(submitData)
      ElMessage.success('新增成功')
    }
    
    dialogVisible.value = false
    fetchData()
  } catch (error: any) {
    console.error('提交失败:', error)
    if (error.response?.status === 409) {
      ElMessageBox.alert('数据冲突，请刷新后重试', '提交失败', { type: 'error' })
    } else {
      ElMessage.error('提交失败：' + (error.response?.data?.message || error.message))
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
.no-image {
  background-color: #f5f7fa;
}
</style>
