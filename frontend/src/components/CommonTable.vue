<template>
  <div class="common-table">
    <!-- 搜索栏插槽 -->
    <div v-if="$slots.search" class="search-bar">
      <slot name="search"></slot>
    </div>

    <!-- 数据表格 -->
    <el-table
      v-loading="loading"
      :data="data"
      v-bind="$attrs"
      stripe
      border
      style="width: 100%"
    >
      <slot></slot>
      <template #empty>
        <el-empty :description="emptyText" />
      </template>
    </el-table>

    <!-- 分页 -->
    <div v-if="total > 0" class="pagination-container">
      <el-pagination
        :current-page="currentPage"
        :page-size="pageSize"
        @update:current-page="val => currentPage = val"
        @update:page-size="val => pageSize = val"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        :total="total"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  loading: {
    type: Boolean,
    default: false
  },
  data: {
    type: Array,
    default: () => []
  },
  total: {
    type: Number,
    default: 0
  },
  page: {
    type: Number,
    default: 1
  },
  size: {
    type: Number,
    default: 10
  },
  emptyText: {
    type: String,
    default: '暂无数据'
  }
})

const emit = defineEmits(['update:page', 'update:size', 'pagination'])

const currentPage = computed({
  get: () => props.page,
  set: (val) => emit('update:page', val)
})

const pageSize = computed({
  get: () => props.size,
  set: (val) => emit('update:size', val)
})

const handleSizeChange = (val) => {
  emit('pagination', { page: currentPage.value, size: val })
}

const handleCurrentChange = (val) => {
  emit('pagination', { page: val, size: pageSize.value })
}
</script>

<style scoped>
.search-bar {
  margin-bottom: 20px;
}
.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
