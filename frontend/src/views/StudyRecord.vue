<template>
  <div class="study-record-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>练习记录</span>
          <div class="filter-group">
            <el-select v-model="query.courseId" placeholder="按课程筛选" clearable @change="fetchList" size="small" style="width: 180px">
              <el-option v-for="item in courses" :key="item.id" :label="item.name" :value="item.id" />
            </el-select>
          </div>
        </div>
      </template>

      <el-table v-loading="loading" :data="list" style="width: 100%" @sort-change="handleSortChange">
        <el-table-column type="index" label="序号" width="80" :index="indexMethod" />
        <el-table-column prop="courseName" label="所属课程" min-width="150" />
        <el-table-column prop="startTime" label="开始时间" width="180" sortable="custom">
          <template #default="scope">
            {{ formatDateTime(scope.row.startTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="submitTime" label="提交时间" width="180" sortable="custom">
          <template #default="scope">
            <el-tag v-if="!scope.row.submitTime" type="info" size="small">未完成</el-tag>
            <span v-else>{{ formatDateTime(scope.row.submitTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="totalScore" label="得分" width="100" sortable="custom">
          <template #default="scope">
            <span v-if="scope.row.submitTime !== null" class="score-text">{{ scope.row.totalScore }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="totalDuration" label="总耗时" width="120">
          <template #default="scope">
            {{ formatDuration(scope.row.totalDuration) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180">
          <template #default="scope">
            <el-button type="primary" link @click="viewDetail(scope.row)">查看报告</el-button>
            <el-popconfirm title="确定要删除这条记录吗？" @confirm="handleDelete(scope.row)">
              <template #reference>
                <el-button type="danger" link>删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <el-pagination
          :current-page="query.page"
          :page-size="query.size"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          :total="total"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const list = ref([])
const total = ref(0)
const courses = ref([])
const query = reactive({
  page: 1,
  size: 10,
  courseId: route.query.courseId ? Number(route.query.courseId) : null,
  sortBy: '',
  order: ''
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
    const res = await request.get('/student/practice-records', { 
      params: {
        page: query.page,
        size: query.size,
        courseId: query.courseId,
        sortBy: query.sortBy,
        order: query.order
      } 
    })
    if (res.code === 1) {
      list.value = res.data.list
      total.value = res.data.total
    }
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const handleSortChange = ({ prop, order }) => {
  query.sortBy = prop
  query.order = order === 'ascending' ? 'asc' : order === 'descending' ? 'desc' : ''
  fetchList()
}

const indexMethod = (index) => {
  return (query.page - 1) * query.size + index + 1
}

const handleDelete = async (row) => {
  try {
    const res = await request.delete(`/student/practice-records/${row.id}`)
    if (res.code === 1) {
      ElMessage.success('删除成功')
      fetchList()
    }
  } catch (e) {
    console.error(e)
    ElMessage.error('删除失败')
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

const viewDetail = (row) => {
  router.push(`/study/practice-report/${row.id}`)
}

const formatDateTime = (value) => {
  if (!value) return '-'
  return value.replace('T', ' ').substring(0, 19)
}

const formatDuration = (seconds) => {
  if (!seconds) return '0秒'
  const m = Math.floor(seconds / 60)
  const s = seconds % 60
  return m > 0 ? `${m}分${s}秒` : `${s}秒`
}

onMounted(() => {
  fetchCourses()
  fetchList()
})
</script>

<style scoped>
.study-record-page {
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
  color: #f56c6c;
}
.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
