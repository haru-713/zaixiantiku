<template>
  <div class="exam-record-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>考试记录</span>
          <div class="filter-group">
            <el-select v-model="query.courseId" placeholder="按课程筛选" clearable @change="fetchList" size="small" style="width: 180px">
              <el-option v-for="item in courses" :key="item.id" :label="item.name" :value="item.id" />
            </el-select>
          </div>
        </div>
      </template>

      <el-table v-loading="loading" :data="list" style="width: 100%; margin-top: 16px">
        <el-table-column type="index" label="序号" width="60" />
        <el-table-column prop="examName" label="考试名称" min-width="200" />
        <el-table-column prop="totalScore" label="得分" width="120">
          <template #default="scope">
            <span class="score-text">{{ scope.row.totalScore ?? '-' }}</span>
            <span class="max-score" v-if="scope.row.maxScore"> / {{ scope.row.maxScore }}</span>
          </template>
        </el-table-column>
        <el-table-column label="提交时间" width="180">
          <template #default="scope">
            {{ formatDateTime(scope.row.submitTime) }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="scope">
            <el-tag :type="getRecordStatusType(scope.row.status)">
              {{ getRecordStatusLabel(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="scope">
            <el-button type="primary" link @click="viewDetail(scope.row)">查看详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <el-pagination :current-page="query.page" :page-size="query.size" :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper" :total="total" @size-change="handleSizeChange"
          @current-change="handleCurrentChange" />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import request from '@/utils/request'

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const list = ref([])
const total = ref(0)
const courses = ref([])
const query = reactive({
  page: 1,
  size: 10,
  courseId: route.query.courseId ? Number(route.query.courseId) : null
})

const fetchCourses = async () => {
  try {
    const res = await request.get('/student/analysis/courses')
    if (res.code === 1) {
      courses.value = res.data
    }
  } catch (e) {
    console.error(e)
  }
}

const fetchList = async () => {
  loading.value = true
  try {
    const res = await request.get('/student/exam-records', { params: query })
    list.value = res.data.list
    total.value = res.data.total
  } catch (e) {
    console.error('获取考试记录失败:', e)
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
  if (!row || !row.id) return
  router.push(`/study/exam-record/${row.id}`)
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
  fetchCourses()
  fetchList()
})
</script>

<style scoped>
.exam-record-page {
  padding: 20px;
}
.card-header {
  font-weight: bold;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.score-text {
  font-weight: bold;
  color: #67c23a;
}
.max-score {
  font-size: 12px;
  color: #909399;
  margin-left: 2px;
}
.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
