<template>
  <div class="exam-schedule-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>考试安排</span>
          <div class="header-actions">
            <el-button type="primary" @click="openCreate">创建考试</el-button>
          </div>
        </div>
      </template>

      <div class="search-bar">
        <el-select v-model="query.courseId" filterable remote clearable :remote-method="fetchCourseOptions"
          :loading="courseLoading" placeholder="请选择课程" style="width: 200px; margin-right: 10px"
          @visible-change="handleCourseDropdown" @change="handleCourseChangeQuery">
          <el-option v-for="c in courseOptions" :key="c.id" :label="c.courseName" :value="c.id" />
        </el-select>
        <el-select v-model="query.classId" placeholder="按班级筛选" clearable style="width: 180px; margin-right: 10px"
          @change="handleQuery">
          <el-option v-for="c in allClassOptions" :key="c.id" :label="c.className" :value="c.id" />
        </el-select>
        <el-input v-model="query.keyword" placeholder="考试名称关键字" clearable style="width: 200px; margin-right: 10px"
          @keyup.enter="handleQuery" />
        <el-select v-model="query.status" placeholder="状态" clearable style="width: 100px; margin-right: 10px"
          @change="handleQuery">
          <el-option label="未开始" :value="0" />
          <el-option label="进行中" :value="1" />
          <el-option label="已结束" :value="2" />
        </el-select>
        <el-button type="primary" @click="handleQuery">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </div>

      <el-table v-loading="loading" :data="list" style="width: 100%; margin-top: 16px">
        <el-table-column type="index" label="序号" width="60" />
        <el-table-column prop="examName" label="考试名称" min-width="180" show-overflow-tooltip>
          <template #default="scope">
            <el-link type="primary" @click="viewDetail(scope.row)">{{ scope.row.examName }}</el-link>
          </template>
        </el-table-column>
        <el-table-column prop="paperName" label="试卷名称" min-width="150" show-overflow-tooltip />
        <el-table-column label="起止时间" width="300">
          <template #default="scope">
            <div style="font-size: 12px">
              {{ formatDateTime(scope.row.startTime) }}<br />
              {{ formatDateTime(scope.row.endTime) }}
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="duration" label="时长(分)" width="90" align="center" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="scope">
            <el-tag :type="getStatusType(scope.row.status)" size="small">
              {{ getStatusLabel(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="targetClasses" label="参与班级" min-width="150" show-overflow-tooltip>
          <template #default="scope">
            <span :class="{ 'student-target': scope.row.targetType === 'STUDENT' }">
              {{ scope.row.targetClasses }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="scope">
            <el-button type="primary" size="small" :disabled="scope.row.status !== 0"
              @click="openEdit(scope.row)">修改</el-button>
            <el-button type="success" size="small" @click="handleCopy(scope.row)">复制</el-button>
            <el-button type="danger" size="small" :disabled="scope.row.status !== 0"
              @click="handleCancel(scope.row)">取消</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <el-pagination :current-page="query.page" :page-size="query.size" :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper" :total="total" @size-change="handleSizeChange"
          @current-change="handleCurrentChange" />
      </div>
    </el-card>

    <!-- 创建/修改考试对话框 -->
    <el-dialog v-model="dialogVisible" :title="editingId ? '修改考试' : '创建考试'" width="600px">
      <el-form :model="form" label-width="100px" :rules="rules" ref="formRef">
        <el-form-item label="考试名称" prop="examName">
          <el-input v-model="form.examName" placeholder="请输入考试名称" />
        </el-form-item>
        <el-form-item label="所属课程" prop="courseId">
          <el-select v-model="form.courseId" filterable remote clearable :remote-method="fetchCourseOptions"
            :loading="courseLoading" placeholder="选择课程" style="width: 100%" @change="handleCourseChange"
            @visible-change="handleCourseDropdown">
            <el-option v-for="c in courseOptions" :key="c.id" :label="c.courseName" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="选择试卷" prop="paperId">
          <el-select v-model="form.paperId" filterable placeholder="选择试卷" style="width: 100%"
            :disabled="!form.courseId">
            <el-option v-for="p in paperOptions" :key="p.id" :label="p.paperName" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="开始时间" required>
          <div class="date-time-inputs">
            <el-date-picker v-model="temp.startDate" type="date" placeholder="选择日期" value-format="YYYY-MM-DD"
              style="width: 180px" @change="updateStartTime" />
            <el-time-picker v-model="temp.startTime" placeholder="具体时间" value-format="HH:mm:ss"
              style="width: 150px; margin-left: 10px" @change="updateStartTime" />
          </div>
          <div v-if="formError.startTime" class="form-error">{{ formError.startTime }}</div>
        </el-form-item>
        <el-form-item label="结束时间" required>
          <div class="date-time-inputs">
            <el-date-picker v-model="temp.endDate" type="date" placeholder="选择日期" value-format="YYYY-MM-DD"
              style="width: 180px" @change="updateEndTime" />
            <el-time-picker v-model="temp.endTime" placeholder="具体时间" value-format="HH:mm:ss"
              style="width: 150px; margin-left: 10px" @change="updateEndTime" />
          </div>
          <div v-if="formError.endTime" class="form-error">{{ formError.endTime }}</div>
        </el-form-item>
        <el-form-item label="考试时长" prop="duration">
          <el-input-number v-model="form.duration" :min="1" />
          <span style="margin-left: 10px">分钟</span>
        </el-form-item>
        <el-form-item label="公布成绩">
          <el-radio-group v-model="form.publishScore">
            <el-radio :value="0">否</el-radio>
            <el-radio :value="1">是</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="参与班级">
          <el-select v-model="form.classIds" multiple placeholder="请选择班级" style="width: 100%"
            :disabled="!form.courseId">
            <el-option v-for="c in formClassOptions" :key="c.id" :label="c.className" :value="c.id" />
          </el-select>
          <div v-if="form.courseId && formClassOptions.length === 0"
            style="color: #909399; font-size: 12px; margin-top: 4px;">
            该课程暂无学生选课，请先添加学生。
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import request from '@/utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({
  page: 1,
  size: 10,
  keyword: '',
  courseId: null,
  classId: null,
  status: null
})

const courseOptions = ref([])
const allClassOptions = ref([])
const formClassOptions = ref([])
const courseLoading = ref(false)
const paperOptions = ref([])

const dialogVisible = ref(false)
const editingId = ref(null)
const submitting = ref(false)
const formRef = ref(null)

const form = reactive({
  examName: '',
  courseId: null,
  paperId: null,
  startTime: '',
  endTime: '',
  duration: 120,
  publishScore: 0,
  classIds: [],
  studentIds: []
})

const temp = reactive({
  startDate: '',
  startTime: '',
  endDate: '',
  endTime: ''
})

const formError = reactive({
  startTime: '',
  endTime: ''
})

const updateStartTime = () => {
  if (temp.startDate && temp.startTime) {
    form.startTime = `${temp.startDate}T${temp.startTime}`
    formError.startTime = ''
  } else {
    form.startTime = ''
  }
}

const updateEndTime = () => {
  if (temp.endDate && temp.endTime) {
    const fullEndTime = `${temp.endDate}T${temp.endTime}`
    if (form.startTime && fullEndTime < form.startTime) {
      formError.endTime = '结束时间不能早于开始时间'
      form.endTime = ''
    } else {
      form.endTime = fullEndTime
      formError.endTime = ''
    }
  } else {
    form.endTime = ''
  }
}

const rules = {
  examName: [{ required: true, message: '请输入考试名称', trigger: 'blur' }],
  courseId: [{ required: true, message: '请选择课程', trigger: 'change' }],
  paperId: [{ required: true, message: '请选择试卷', trigger: 'change' }],
  duration: [{ required: true, message: '请输入考试时长', trigger: 'blur' }]
}

const fetchList = async () => {
  loading.value = true
  try {
    const res = await request.get('/exams', { params: query })
    list.value = res.data.list
    total.value = res.data.total
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const fetchCourseOptions = async () => {
  courseLoading.value = true
  try {
    const res = await request.get('/courses/managed')
    courseOptions.value = res.data
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

const handleCourseChange = async (val, keepPaperId = false) => {
  if (!keepPaperId) {
    form.paperId = null
    form.classIds = []
  }
  paperOptions.value = []
  formClassOptions.value = []
  if (val) {
    try {
      // 1. 获取试卷列表
      const paperRes = await request.get('/papers', { params: { courseId: val, status: 1, page: 1, size: 100 } })
      paperOptions.value = paperRes.data.list

      // 2. 获取该课程关联的班级列表
      const classRes = await request.get('/classes/by-course', { params: { courseId: val } })
      formClassOptions.value = classRes.data || []
    } catch (e) {
      console.error(e)
    }
  }
}

const fetchAllClasses = async () => {
  try {
    const res = await request.get('/teacher/analysis/classes', {
      params: { courseId: query.courseId }
    })
    if (res.code === 1) {
      allClassOptions.value = res.data
    }
  } catch (e) {
    console.error(e)
  }
}

const handleCourseChangeQuery = () => {
  query.classId = null
  fetchAllClasses()
  handleQuery()
}

const handleQuery = () => {
  query.page = 1
  fetchList()
}

const handleCopy = (row) => {
  editingId.value = null
  Object.assign(form, {
    examName: row.examName + ' - 副本',
    courseId: row.courseId,
    paperId: row.paperId,
    startTime: row.startTime,
    endTime: row.endTime,
    duration: row.duration,
    publishScore: row.publishScore,
    classIds: [], // 复制时班级和学生通常需要重新确认，或者根据需求决定是否复制
    studentIds: []
  })

  // 填充时间显示
  if (row.startTime) {
    const splitChar = row.startTime.includes('T') ? 'T' : ' '
    const [d, t] = row.startTime.split(splitChar)
    temp.startDate = d
    temp.startTime = t
  }
  if (row.endTime) {
    const splitChar = row.endTime.includes('T') ? 'T' : ' '
    const [d, t] = row.endTime.split(splitChar)
    temp.endDate = d
    temp.endTime = t
  }

  handleCourseChange(row.courseId, true)
  dialogVisible.value = true
}

const viewDetail = (row) => {
  ElMessageBox.alert(`
    <div style="text-align: left">
      <p><b>考试名称：</b>${row.examName}</p>
      <p><b>试卷名称：</b>${row.paperName}</p>
      <p><b>参与班级：</b>${row.targetClasses}</p>
      <p><b>起止时间：</b>${formatDateTime(row.startTime)} ~ ${formatDateTime(row.endTime)}</p>
      <p><b>考试时长：</b>${row.duration} 分钟</p>
    </div>
  `, '考试详情', {
    dangerouslyUseHTMLString: true,
    confirmButtonText: '确定'
  })
}

const resetQuery = () => {
  query.keyword = ''
  query.courseId = null
  query.classId = null
  query.status = null
  allClassOptions.value = []
  handleQuery()
}

const handleSizeChange = (val) => {
  query.size = val
  fetchList()
}

const handleCurrentChange = (val) => {
  query.page = val
  fetchList()
}

const openCreate = () => {
  editingId.value = null
  Object.assign(form, {
    examName: '',
    courseId: null,
    paperId: null,
    startTime: '',
    endTime: '',
    duration: 120,
    publishScore: 0,
    classIds: [],
    studentIds: []
  })
  Object.assign(temp, {
    startDate: '',
    startTime: '',
    endDate: '',
    endTime: ''
  })
  formError.startTime = ''
  formError.endTime = ''
  dialogVisible.value = true
}

const openEdit = (row) => {
  editingId.value = row.id
  Object.assign(form, {
    examName: row.examName,
    courseId: row.courseId,
    paperId: row.paperId,
    startTime: row.startTime,
    endTime: row.endTime,
    duration: row.duration,
    publishScore: row.publishScore,
    classIds: [], // 暂时留空
    studentIds: [] // 暂时留空
  })
  if (row.startTime) {
    const splitChar = row.startTime.includes('T') ? 'T' : ' '
    const [d, t] = row.startTime.split(splitChar)
    temp.startDate = d
    temp.startTime = t
  }
  if (row.endTime) {
    const splitChar = row.endTime.includes('T') ? 'T' : ' '
    const [d, t] = row.endTime.split(splitChar)
    temp.endDate = d
    temp.endTime = t
  }
  formError.startTime = ''
  formError.endTime = ''
  handleCourseChange(row.courseId, true)
  dialogVisible.value = true
}

const handleSubmit = async () => {
  await formRef.value.validate()
  if (!form.startTime) {
    formError.startTime = '请完善开始时间'
    return
  }
  if (!form.endTime) {
    formError.endTime = '请完善结束时间'
    return
  }
  if (form.endTime < form.startTime) {
    formError.endTime = '结束时间不能早于开始时间'
    return
  }

  submitting.value = true
  try {
    const data = { ...form }

    if (editingId.value) {
      await request.put(`/exams/${editingId.value}`, data)
      ElMessage.success('修改成功')
    } else {
      await request.post('/exams', data)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchList()
  } catch (e) {
    console.error(e)
  } finally {
    submitting.value = false
  }
}

const handleCancel = (row) => {
  ElMessageBox.confirm('确定要取消该考试安排吗？', '提示', {
    type: 'warning'
  }).then(async () => {
    try {
      await request.delete(`/exams/${row.id}`)
      ElMessage.success('取消成功')
      fetchList()
    } catch (e) {
      console.error(e)
    }
  })
}

const formatDateTime = (value) => {
  if (!value) {
    return '-'
  }
  const str = String(value)
  if (str.includes('T')) {
    const replaced = str.replace('T', ' ')
    return replaced.length >= 19 ? replaced.slice(0, 19) : replaced
  }
  return str
}

const getStatusLabel = (status) => {
  const labels = ['未开始', '进行中', '已结束']
  return labels[status] || '未知'
}

const getStatusType = (status) => {
  const types = ['info', 'success', 'warning']
  return types[status] || 'info'
}

onMounted(() => {
  fetchList()
  fetchAllClasses()
})
</script>

<style scoped>
.exam-schedule-page {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.search-bar {
  margin-bottom: 20px;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.date-time-inputs {
  display: flex;
  align-items: center;
}

.form-error {
  color: #f56c6c;
  font-size: 12px;
  line-height: 1;
  padding-top: 4px;
  position: absolute;
  top: 100%;
  left: 0;
}

.student-target {
  color: #e6a23c;
  font-weight: 500;
}
</style>
