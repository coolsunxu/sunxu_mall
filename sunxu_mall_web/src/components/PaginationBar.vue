<template>
  <div class="pagination-bar">
    <div class="pagination-controls">
      <!-- 上一页按钮 -->
      <el-button
        size="small"
        :disabled="currentPage <= 1 || loading"
        @click="handlePrev"
      >
        <el-icon><ArrowLeft /></el-icon>
        上一页
      </el-button>

      <!-- 页码信息 -->
      <span class="page-info">
        第 <strong>{{ currentPage }}</strong> 页
        <span v-if="maxPageReached > 1" class="max-page-hint">
          / 已访问至第 {{ maxPageReached }} 页
        </span>
      </span>

      <!-- 下一页按钮 -->
      <el-button
        size="small"
        :disabled="!hasNext || loading"
        @click="handleNext"
      >
        下一页
        <el-icon><ArrowRight /></el-icon>
      </el-button>

      <!-- 跳转区域 -->
      <div class="jump-section" v-if="maxPageReached > 1">
        <span class="jump-label">跳转到</span>
        <el-input-number
          v-model="jumpPage"
          size="small"
          :min="1"
          :max="maxPageReached"
          :disabled="loading"
          controls-position="right"
          class="jump-input"
        />
        <el-button
          size="small"
          type="primary"
          :disabled="loading || !isValidJump"
          @click="handleJump"
        >
          跳转
        </el-button>
      </div>
    </div>

    <!-- 提示信息 -->
    <div class="pagination-hint">
      <el-icon><InfoFilled /></el-icon>
      <span>仅可跳转到已访问页（为避免深分页性能问题）</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ArrowLeft, ArrowRight, InfoFilled } from '@element-plus/icons-vue'

interface Props {
  currentPage: number
  maxPageReached: number
  hasNext: boolean
  loading?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  loading: false
})

const emit = defineEmits<{
  (e: 'prev'): void
  (e: 'next'): void
  (e: 'jump', pageNum: number): void
}>()

// 跳转页码
const jumpPage = ref(props.currentPage)

// 监听 currentPage 变化，同步更新 jumpPage
watch(() => props.currentPage, (newVal) => {
  jumpPage.value = newVal
})

// 是否为有效跳转
const isValidJump = computed(() => {
  return jumpPage.value >= 1 && 
         jumpPage.value <= props.maxPageReached && 
         jumpPage.value !== props.currentPage
})

// 上一页
const handlePrev = () => {
  if (props.currentPage > 1 && !props.loading) {
    emit('prev')
  }
}

// 下一页
const handleNext = () => {
  if (props.hasNext && !props.loading) {
    emit('next')
  }
}

// 跳转
const handleJump = () => {
  if (isValidJump.value && !props.loading) {
    emit('jump', jumpPage.value)
  }
}
</script>

<style scoped>
.pagination-bar {
  margin-top: 20px;
  padding: 16px;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  border-radius: 8px;
  border: 1px solid #e2e8f0;
}

.pagination-controls {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  flex-wrap: wrap;
}

.page-info {
  font-size: 14px;
  color: #475569;
  padding: 0 12px;
}

.page-info strong {
  color: #0f172a;
  font-size: 16px;
}

.max-page-hint {
  color: #94a3b8;
  font-size: 13px;
}

.jump-section {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: 16px;
  padding-left: 16px;
  border-left: 1px solid #e2e8f0;
}

.jump-label {
  font-size: 14px;
  color: #64748b;
}

.jump-input {
  width: 100px;
}

.pagination-hint {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  margin-top: 12px;
  font-size: 12px;
  color: #94a3b8;
}

.pagination-hint .el-icon {
  font-size: 14px;
}

/* 响应式调整 */
@media (max-width: 768px) {
  .pagination-controls {
    gap: 8px;
  }

  .jump-section {
    margin-left: 0;
    padding-left: 0;
    border-left: none;
    margin-top: 12px;
    width: 100%;
    justify-content: center;
  }

  .page-info {
    width: 100%;
    text-align: center;
    order: -1;
    margin-bottom: 8px;
  }
}
</style>
