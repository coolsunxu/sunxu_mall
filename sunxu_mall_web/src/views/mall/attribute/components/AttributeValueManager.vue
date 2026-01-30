<template>
  <div class="value-manager">
    <div class="header">
      <el-input
        v-model="newValue"
        placeholder="快速添加属性值 (按回车保存)"
        size="small"
        @keyup.enter="handleQuickAdd"
      >
        <template #append>
          <el-button :icon="Plus" @click="handleQuickAdd">添加</el-button>
        </template>
      </el-input>
    </div>

    <el-table :data="dataList" border size="small" style="margin-top: 15px">
      <el-table-column prop="value" label="属性值">
        <template #default="scope">
          <el-input 
            v-if="scope.row.editing" 
            v-model="scope.row.tempValue" 
            size="small" 
            @keyup.enter="saveEdit(scope.row)"
            @blur="cancelEdit(scope.row)"
          />
          <span v-else @dblclick="startEdit(scope.row)">{{ scope.row.value }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="sort" label="排序" width="80">
        <template #default="scope">
          <el-input-number 
            v-model="scope.row.sort" 
            :controls="false" 
            size="small" 
            style="width: 50px"
            @change="handleSortChange(scope.row)"
          />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="100" align="center">
        <template #default="scope">
          <el-button link type="danger" :icon="Delete" @click="handleDelete(scope.row.id)" />
        </template>
      </el-table-column>
    </el-table>
    
    <div class="footer-tip">
      <el-icon><InfoFilled /></el-icon>
      <span>双击属性值可进行快速编辑</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Delete, InfoFilled } from '@element-plus/icons-vue'
import * as attributeValueApi from '@/api/attributeValue'
import type { AttributeValueQueryDTO } from '@/types'

const props = defineProps<{
  attributeId: number | undefined
}>()

const emit = defineEmits(['changed'])

const dataList = ref<any[]>([])
const newValue = ref('')

watch(() => props.attributeId, () => {
  if (props.attributeId) fetchData()
}, { immediate: true })

onMounted(() => {
  console.log('AttributeValueManager 已挂载')
  if (props.attributeId) fetchData()
})

const fetchData = async () => {
  console.log('获取属性值数据，ID:', props.attributeId)
  if (!props.attributeId) return
  try {
    const query: AttributeValueQueryDTO = {
      attributeId: props.attributeId,
      pageNum: 1,
      pageSize: 100 // 一个属性下的值通常不多
    }
    const res = await attributeValueApi.searchByBidirectionalCursor(query)
    dataList.value = res.list.map(item => ({
      ...item,
      editing: false,
      tempValue: item.value
    }))
  } catch (error) {
    console.error(error)
  }
}

const handleQuickAdd = async () => {
  if (!newValue.value.trim() || !props.attributeId) return
  try {
    await attributeValueApi.add({
      attributeId: props.attributeId,
      value: newValue.value.trim(),
      sort: 999
    })
    newValue.value = ''
    fetchData()
    emit('changed') // 通知父组件更新预览
  } catch (error) {
    ElMessage.error('添加失败')
  }
}

const startEdit = (row: any) => {
  row.editing = true
  row.tempValue = row.value
}

const cancelEdit = (row: any) => {
  setTimeout(() => { row.editing = false }, 200) // 延迟以处理 blur/click 冲突
}

const saveEdit = async (row: any) => {
  if (!row.tempValue.trim() || row.tempValue === row.value) {
    row.editing = false
    return
  }
  try {
    await attributeValueApi.edit({
      id: row.id,
      attributeId: props.attributeId!,
      value: row.tempValue.trim(),
      sort: row.sort
    })
    row.value = row.tempValue
    row.editing = false
    emit('changed')
  } catch (error) {
    ElMessage.error('修改失败')
  }
}

const handleSortChange = async (row: any) => {
  try {
    await attributeValueApi.edit({
      id: row.id,
      attributeId: props.attributeId!,
      value: row.value,
      sort: row.sort
    })
    emit('changed')
  } catch (error) {
    ElMessage.error('更新排序失败')
  }
}

const handleDelete = (id: number) => {
  ElMessageBox.confirm('确认删除该属性值吗?', '提示', { type: 'warning' }).then(async () => {
    await attributeValueApi.del([id])
    fetchData()
    emit('changed')
  })
}
</script>

<style scoped>
.value-manager { padding: 0 10px; }
.footer-tip { margin-top: 15px; color: #999; font-size: 12px; display: flex; align-items: center; gap: 5px; }
</style>
