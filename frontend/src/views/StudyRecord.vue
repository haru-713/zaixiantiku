<template>
  <div class="study-record-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>练习记录</span>
        </div>
      </template>

      <el-empty description="暂无练习记录" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import request from '@/utils/request'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({
  page: 1,
  size: 10
})

const fetchList = async () => {
  loading.value = true
  try {
    const res = await request.get('/student/exam-records', { params: query })
    list.value = res.data.list
    total.value = res.data.total
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const handleSizeChange = (val) => {
  query.size = val
  fetchList()
}

const handleCurrentChange = (val) => {
  query.page = val
  fetchList()
}

const getRecordStatusType = (status) => {
  if (status === 0) return 'info'
  if (status === 1) return 'primary'
  if (status === 2) return 'success'
  return ''
}

const getRecordStatusLabel = (status) => {
  const labels = ['考试中', '已交卷', '已批阅']
  return labels[status] || '未知'
}

const viewDetail = (row) => {
  // 详情页逻辑暂未实现
}

const formatDateTime = (value) => {
  if (!value) return '-'
  const str = String(value)
  if (str.includes('T')) {
    const replaced = str.replace('T', ' ')
    return replaced.length >= 19 ? replaced.slice(0, 19) : replaced
  }
  return str
}

onMounted(() => {
  fetchList()
})
</script>

<style scoped>
.study-record-page {
  padding: 20px;
}
.card-header {
  font-weight: bold;
}
.score-text {
  font-weight: bold;
  color: #f56c6c;
}
.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
