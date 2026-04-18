<template>
  <div class="course-student-manage-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>课程学生管理</span>
        </div>
      </template>

      <!-- 筛选栏 -->
      <div class="filter-bar">
        <el-select v-model="courseId" placeholder="请选择课程" style="width: 250px; margin-right: 15px" @change="handleCourseChange">
          <el-option v-for="item in courseOptions" :key="item.id" :label="item.courseName" :value="item.id" />
        </el-select>

        <el-select v-model="queryParams.classId" placeholder="按班级筛选已选课学生" clearable style="width: 200px; margin-right: 15px" @change="handleQuery">
          <el-option v-for="item in allClassOptions" :key="item.id" :label="item.className" :value="item.id" />
        </el-select>

        <el-input v-model="queryParams.keyword" placeholder="搜索学号/姓名" style="width: 200px; margin-right: 15px"
          clearable @clear="handleQuery" @keyup.enter="handleQuery" />
        <el-button type="primary" @click="handleQuery">查询</el-button>
      </div>

      <div v-if="courseId" class="toolbar">
        <div class="batch-action">
          <span class="label">按班级批量添加：</span>
          <el-select v-model="selectedClassForBatch" placeholder="请选择班级" style="width: 200px; margin-right: 10px" clearable>
            <el-option v-for="item in allClassOptions" :key="item.id" :label="item.className" :value="item.id" />
          </el-select>
          <el-button type="success" :disabled="!selectedClassForBatch" @click="handleBatchAddByClass">批量添加班级学生</el-button>
        </div>
        <div class="single-action">
          <el-button type="primary" @click="openStudentPicker">单独添加学生</el-button>
        </div>
      </div>

      <!-- 数据表格 -->
      <el-table v-if="courseId" :data="studentList" v-loading="loading" style="width: 100%; margin-top: 20px">
        <el-table-column prop="username" label="学号/用户名" width="150" />
        <el-table-column prop="name" label="姓名" width="150" />
        <el-table-column prop="className" label="所属班级" min-width="150" />
        <el-table-column label="操作" width="120" fixed="right" align="center">
          <template #default="scope">
            <el-button type="danger" link @click="handleRemove(scope.row)">移除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-else description="请先选择一门课程" />

      <!-- 分页 -->
      <div v-if="courseId" class="pagination-container">
        <el-pagination :current-page="queryParams.page" :page-size="queryParams.size"
          :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next, jumper" :total="total"
          @size-change="handleSizeChange" @current-change="handleCurrentChange" />
      </div>
    </el-card>

    <!-- 学生选择器对话框 -->
    <el-dialog v-model="pickerVisible" title="单独添加学生到课程" width="600px">
      <div class="picker-container">
        <el-input v-model="pickerQuery.keyword" placeholder="搜索学号/姓名" style="margin-bottom: 15px"
          @keyup.enter="fetchPickerStudents">
          <template #append>
            <el-button icon="Search" @click="fetchPickerStudents" />
          </template>
        </el-input>
        <el-table :data="pickerList" v-loading="pickerLoading" @selection-change="handleSelectionChange"
          height="400px">
          <el-table-column type="selection" width="55" />
          <el-table-column prop="username" label="学号" />
          <el-table-column prop="name" label="姓名" />
        </el-table>
      </div>
      <template #footer>
        <el-button @click="pickerVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmAddStudents" :disabled="selectedStudents.length === 0">添加</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

const route = useRoute()
const courseId = ref(null)
const courseOptions = ref([])
const allClassOptions = ref([])
const selectedClassForBatch = ref(null)

const loading = ref(false)
const studentList = ref([])
const total = ref(0)
const queryParams = reactive({
  page: 1,
  size: 10,
  classId: null,
  keyword: ''
})

const pickerVisible = ref(false)
const pickerLoading = ref(false)
const pickerList = ref([])
const pickerQuery = reactive({
  keyword: ''
})
const selectedStudents = ref([])

const fetchCourseOptions = async () => {
  try {
    const res = await request.get('/courses/managed')
    courseOptions.value = res.data
    
    // 如果路由传参了 courseId，则优先使用
    const queryCourseId = route.query.courseId
    if (queryCourseId) {
      courseId.value = Number(queryCourseId)
      handleCourseChange()
    } else if (courseOptions.value.length > 0) {
      courseId.value = courseOptions.value[0].id
      handleCourseChange()
    }
  } catch (e) {
    console.error(e)
  }
}

const fetchAllClasses = async () => {
  try {
    const res = await request.get('/classes', { params: { page: 1, size: 1000 } })
    allClassOptions.value = res.data.list
  } catch (e) {
    console.error(e)
  }
}

const fetchList = async () => {
  if (!courseId.value) return
  loading.value = true
  try {
    const res = await request.get(`/courses/${courseId.value}/students`, { params: queryParams })
    studentList.value = res.data.list
    total.value = res.data.total
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const handleCourseChange = () => {
  queryParams.page = 1
  queryParams.classId = null
  selectedClassForBatch.value = null
  fetchList()
}

const handleQuery = () => {
  queryParams.page = 1
  fetchList()
}

const handleSizeChange = (val) => {
  queryParams.size = val
  fetchList()
}

const handleCurrentChange = (val) => {
  queryParams.page = val
  fetchList()
}

const handleBatchAddByClass = () => {
  const className = allClassOptions.value.find(c => c.id === selectedClassForBatch.value)?.className
  ElMessageBox.confirm(`确定将班级 "${className}" 的所有学生加入当前课程吗？`, '提示', {
    type: 'warning'
  }).then(async () => {
    await request.post(`/courses/${courseId.value}/classes/${selectedClassForBatch.value}/students`)
    ElMessage.success('批量添加成功')
    fetchList()
  })
}

const handleRemove = (row) => {
  ElMessageBox.confirm(`确定从课程中移除学生 ${row.name} 吗？`, '提示', {
    type: 'warning'
  }).then(async () => {
    await request.delete(`/courses/${courseId.value}/students`, { params: { studentId: row.id } })
    ElMessage.success('移除成功')
    fetchList()
  })
}

const openStudentPicker = () => {
  pickerQuery.keyword = ''
  fetchPickerStudents()
  pickerVisible.value = true
}

const fetchPickerStudents = async () => {
  pickerLoading.value = true
  try {
    // 调用现有的学生候选列表接口
    const res = await request.get(`/courses/${courseId.value}/students/candidates`, {
      params: {
        page: 1,
        size: 100,
        keyword: pickerQuery.keyword
      }
    })
    pickerList.value = res.data.list
  } catch (e) {
    console.error(e)
  } finally {
    pickerLoading.value = false
  }
}

const handleSelectionChange = (val) => {
  selectedStudents.value = val
}

const confirmAddStudents = async () => {
  try {
    const studentIds = selectedStudents.value.map(s => s.id)
    await request.post(`/courses/${courseId.value}/students`, { studentIds })
    ElMessage.success('添加成功')
    pickerVisible.value = false
    fetchList()
  } catch (e) {
    console.error(e)
  }
}

onMounted(() => {
  fetchCourseOptions()
  fetchAllClasses()
})
</script>

<style scoped>
.course-student-manage-container {
  padding: 20px;
}

.card-header {
  font-weight: bold;
}

.filter-bar {
  margin-bottom: 20px;
  display: flex;
  align-items: center;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 10px;
  padding: 15px;
  background-color: #f8f9fa;
  border-radius: 4px;
}

.batch-action {
  display: flex;
  align-items: center;
}

.batch-action .label {
  font-size: 14px;
  color: #606266;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.picker-container {
  padding: 10px 0;
}
</style>
