<template>
  <div class="my-exams-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>我的考试</span>
        </div>
      </template>

      <div class="search-bar">
        <el-select v-model="query.courseId" filterable remote clearable :remote-method="fetchCourseOptions"
          :loading="courseLoading" placeholder="筛选课程" style="width: 240px; margin-right: 10px"
          @visible-change="handleCourseDropdown">
          <el-option v-for="c in courseOptions" :key="c.id" :label="c.courseName" :value="c.id" />
        </el-select>
        <el-button type="primary" @click="fetchList">刷新</el-button>
      </div>

      <el-table v-loading="loading" :data="list" style="width: 100%; margin-top: 16px">
        <el-table-column prop="examName" label="考试名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="paperName" label="试卷" min-width="150" show-overflow-tooltip />
        <el-table-column label="起止时间" width="320">
          <template #default="scope">
            {{ formatDateTime(scope.row.startTime) }} ~ {{ formatDateTime(scope.row.endTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="duration" label="时长(分)" width="100" />
        <el-table-column label="状态" width="100">
          <template #default="scope">
            <el-tag :type="getStatusType(scope.row.status)">
              {{ getStatusLabel(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="scope">
            <el-button type="primary" size="small" :disabled="scope.row.status !== 1" @click="enterExam(scope.row)">
              进入考试
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'

const router = useRouter()
const loading = ref(false)
const list = ref([])
const query = reactive({
  courseId: null
})

const courseOptions = ref([])
const courseLoading = ref(false)

const fetchList = async () => {
  loading.value = true
  try {
    const res = await request.get('/student/exams', { params: query })
    list.value = res.data.list
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const fetchCourseOptions = async (keyword) => {
  courseLoading.value = true
  try {
    const res = await request.get('/courses', { params: { keyword, page: 1, size: 50 } })
    courseOptions.value = res.data.list
  } catch (e) {
    console.error(e)
  } finally {
    courseLoading.value = false
  }
}

const handleCourseDropdown = (visible) => {
  if (visible && courseOptions.value.length === 0) {
    fetchCourseOptions()
  }
}

const getStatusType = (status) => {
  if (status === 0) return 'info'
  if (status === 2) return 'warning'
  return 'success'
}

const getStatusLabel = (status) => {
  if (status === 0) return '未开始'
  if (status === 2) return '已结束'
  return '进行中'
}

const enterExam = (row) => {
  router.push(`/study/exam/${row.id}`)
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
.my-exams-page {
  padding: 20px;
}
.card-header {
  font-weight: bold;
}
.search-bar {
  margin-bottom: 20px;
}
</style>
