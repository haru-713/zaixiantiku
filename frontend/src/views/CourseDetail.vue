<template>
  <div class="course-detail-page">
    <el-card v-loading="loading">
      <template #header>
        <div class="card-header">
          <span>课程详情</span>
          <div class="header-actions">
            <el-button @click="goBack">返回</el-button>
          </div>
        </div>
      </template>

      <el-descriptions :column="2" border>
        <el-descriptions-item label="ID">{{ detail.id }}</el-descriptions-item>
        <el-descriptions-item label="课程名称">{{ detail.courseName }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="detail.status === 1 ? 'success' : 'danger'">
            {{ detail.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatDateTime(detail.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ formatDateTime(detail.updateTime) }}</el-descriptions-item>
        <el-descriptions-item label="封面" :span="2">
          <el-link v-if="detail.cover" :href="detail.cover" target="_blank">{{ detail.cover }}</el-link>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item label="课程描述" :span="2">
          <span>{{ detail.description || '-' }}</span>
        </el-descriptions-item>
      </el-descriptions>

      <el-divider />

      <div class="section-title">教师</div>
      <div v-if="canAddTeacherUI" class="teacher-actions">
        <el-button type="primary" @click="openTeacherPicker">选择教师</el-button>
      </div>

      <el-table :data="detail.teachers || []" style="width: 100%">
        <el-table-column type="index" label="序号" width="80" />
        <el-table-column prop="name" label="姓名" />
        <el-table-column v-if="canRemoveTeacherUI" label="操作" width="120">
          <template #default="scope">
            <el-button v-if="canRemoveTeacherRow(scope.row.id)" type="danger" size="small"
              @click="handleRemoveTeacher(scope.row.id)">移除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-divider />

      <div class="section-title">学生</div>
      <div v-if="canAddStudentUI" class="teacher-actions">
        <el-button type="primary" @click="openStudentPicker">选择学生</el-button>
      </div>
      <el-table :data="detail.students || []" style="width: 100%">
        <el-table-column type="index" label="序号" width="80" />
        <el-table-column prop="name" label="姓名" />
        <el-table-column v-if="canRemoveStudentUI" label="操作" width="120">
          <template #default="scope">
            <el-button type="danger" size="small" @click="handleRemoveStudent(scope.row.id)">移除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="teacherPickerVisible" title="选择教师" width="720px">
      <div class="picker-toolbar">
        <el-input v-model="teacherQuery.keyword" placeholder="姓名/用户名" clearable style="width: 260px"
          @clear="handleTeacherQuery" @keyup.enter="handleTeacherQuery" />
        <el-button type="primary" @click="handleTeacherQuery">查询</el-button>
        <el-button @click="resetTeacherQuery">重置</el-button>
      </div>

      <el-table :data="teacherList" v-loading="teacherLoading" style="width: 100%; margin-top: 12px"
        @selection-change="handleTeacherSelectionChange">
        <el-table-column type="selection" width="55" />
        <el-table-column type="index" label="序号" width="80" :index="teacherIndexMethod" />
        <el-table-column prop="name" label="姓名" />
      </el-table>

      <div class="pagination-container">
        <el-pagination :current-page="teacherQuery.page" :page-size="teacherQuery.size" :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper" :total="teacherTotal" @size-change="handleTeacherSizeChange"
          @current-change="handleTeacherCurrentChange" />
      </div>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="teacherPickerVisible = false">取消</el-button>
          <el-button type="primary" :loading="teacherSaving" @click="confirmAddTeachers">添加</el-button>
        </span>
      </template>
    </el-dialog>

    <el-dialog v-model="studentPickerVisible" title="选择学生" width="720px">
      <div class="picker-toolbar">
        <el-input v-model="studentQuery.keyword" placeholder="姓名/用户名/手机号" clearable style="width: 260px"
          @clear="handleStudentQuery" @keyup.enter="handleStudentQuery" />
        <el-button type="primary" @click="handleStudentQuery">查询</el-button>
        <el-button @click="resetStudentQuery">重置</el-button>
      </div>

      <el-table :data="studentList" v-loading="studentLoading" style="width: 100%; margin-top: 12px"
        @selection-change="handleStudentSelectionChange">
        <el-table-column type="selection" width="55" />
        <el-table-column type="index" label="序号" width="80" :index="studentIndexMethod" />
        <el-table-column prop="name" label="姓名" />
      </el-table>

      <div class="pagination-container">
        <el-pagination :current-page="studentQuery.page" :page-size="studentQuery.size" :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper" :total="studentTotal" @size-change="handleStudentSizeChange"
          @current-change="handleStudentCurrentChange" />
      </div>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="studentPickerVisible = false">取消</el-button>
          <el-button type="primary" :loading="studentSaving" @click="confirmAddStudents">添加</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import request from '@/utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const teacherSaving = ref(false)
const teacherPickerVisible = ref(false)
const teacherLoading = ref(false)
const teacherTotal = ref(0)
const teacherList = ref([])
const selectedTeacherIds = ref([])
const teacherQuery = reactive({
  page: 1,
  size: 10,
  keyword: ''
})

const loading = ref(false)
const detail = ref({
  id: null,
  courseName: '',
  description: '',
  cover: '',
  status: null,
  auditStatus: null,
  auditReason: '',
  teachers: [],
  students: [],
  canAddTeacher: false,
  canRemoveTeacher: false,
  canAddStudent: false,
  canRemoveStudent: false,
  createTime: '',
  updateTime: ''
})

const localIsAdmin = computed(() => {
  const roles = userStore.userInfo?.roles || []
  return roles.includes('ADMIN') || roles.includes('ROLE_ADMIN')
})

const currentUserId = computed(() => userStore.userInfo?.id)

const localCanManageStudents = computed(() => {
  const roles = userStore.userInfo?.roles || []
  return roles.includes('ADMIN') || roles.includes('ROLE_ADMIN') || roles.includes('TEACHER') || roles.includes('ROLE_TEACHER')
})

const canAddTeacherUI = computed(() => {
  if (detail.value.canAddTeacher === true) {
    return true
  }
  if (detail.value.canAddTeacher == null) {
    return localIsAdmin.value
  }
  return false
})

const canRemoveTeacherUI = computed(() => {
  if (detail.value.canRemoveTeacher === true) {
    return true
  }
  if (detail.value.canRemoveTeacher == null) {
    return localIsAdmin.value
  }
  return false
})

const canAddStudentUI = computed(() => {
  if (detail.value.canAddStudent === true) {
    return true
  }
  if (detail.value.canAddStudent == null) {
    return localCanManageStudents.value
  }
  return false
})

const canRemoveStudentUI = computed(() => {
  if (detail.value.canRemoveStudent === true) {
    return true
  }
  if (detail.value.canRemoveStudent == null) {
    return localCanManageStudents.value
  }
  return false
})

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

const canRemoveTeacherRow = (teacherId) => {
  if (!canRemoveTeacherUI.value) {
    return false
  }
  const teachers = detail.value.teachers || []
  if (teachers.length <= 1) {
    return false
  }
  if (!localIsAdmin.value && currentUserId.value && teacherId === currentUserId.value) {
    return false
  }
  return true
}

const fetchDetail = async () => {
  const courseId = route.params.courseId
  if (!courseId) {
    ElMessage.error('courseId 不能为空')
    return
  }
  loading.value = true
  try {
    const res = await request.get(`/courses/${courseId}`)
    detail.value = res.data
  } catch (e) {
    console.error('获取课程详情失败:', e)
  } finally {
    loading.value = false
  }
}

const fetchTeacherCandidates = async () => {
  const courseId = route.params.courseId
  if (!courseId) {
    return
  }
  teacherLoading.value = true
  try {
    const res = await request.get(`/courses/${courseId}/teachers/candidates`, { params: teacherQuery })
    teacherList.value = res.data.list
    teacherTotal.value = res.data.total
  } catch (e) {
    console.error('获取教师候选列表失败:', e)
  } finally {
    teacherLoading.value = false
  }
}

const openTeacherPicker = () => {
  if (!canAddTeacherUI.value) {
    ElMessage.error('没有权限操作')
    return
  }
  teacherQuery.page = 1
  teacherQuery.size = 10
  teacherQuery.keyword = ''
  selectedTeacherIds.value = []
  teacherPickerVisible.value = true
  fetchTeacherCandidates()
}

const studentSaving = ref(false)
const studentPickerVisible = ref(false)
const studentLoading = ref(false)
const studentTotal = ref(0)
const studentList = ref([])
const selectedStudentIds = ref([])
const studentQuery = reactive({
  page: 1,
  size: 10,
  keyword: ''
})

const fetchStudentCandidates = async () => {
  const courseId = route.params.courseId
  if (!courseId) {
    return
  }
  studentLoading.value = true
  try {
    const res = await request.get(`/courses/${courseId}/students/candidates`, { params: studentQuery })
    studentList.value = res.data.list
    studentTotal.value = res.data.total
  } catch (e) {
    console.error('获取学生候选列表失败:', e)
  } finally {
    studentLoading.value = false
  }
}

const openStudentPicker = () => {
  if (!canAddStudentUI.value) {
    ElMessage.error('没有权限操作')
    return
  }
  studentQuery.page = 1
  studentQuery.size = 10
  studentQuery.keyword = ''
  selectedStudentIds.value = []
  studentPickerVisible.value = true
  fetchStudentCandidates()
}

const handleStudentSelectionChange = (rows) => {
  selectedStudentIds.value = (rows || []).map((r) => r.id).filter((id) => id)
}

const handleStudentQuery = () => {
  studentQuery.page = 1
  fetchStudentCandidates()
}

const resetStudentQuery = () => {
  studentQuery.keyword = ''
  handleStudentQuery()
}

const handleStudentSizeChange = (val) => {
  studentQuery.size = val
  fetchStudentCandidates()
}

const handleStudentCurrentChange = (val) => {
  studentQuery.page = val
  fetchStudentCandidates()
}

const studentIndexMethod = (index) => {
  return (studentQuery.page - 1) * studentQuery.size + index + 1
}

const confirmAddStudents = async () => {
  const courseId = route.params.courseId
  if (!courseId) {
    return
  }
  if (!selectedStudentIds.value || selectedStudentIds.value.length === 0) {
    ElMessage.warning('请选择学生')
    return
  }

  studentSaving.value = true
  try {
    await request.post(`/courses/${courseId}/students`, { studentIds: selectedStudentIds.value })
    ElMessage.success('添加成功')
    studentPickerVisible.value = false
    fetchDetail()
  } catch (e) {
    console.error('添加学生失败:', e)
  } finally {
    studentSaving.value = false
  }
}

const handleTeacherSelectionChange = (rows) => {
  selectedTeacherIds.value = (rows || []).map((r) => r.id).filter((id) => id)
}

const handleTeacherQuery = () => {
  teacherQuery.page = 1
  fetchTeacherCandidates()
}

const resetTeacherQuery = () => {
  teacherQuery.keyword = ''
  handleTeacherQuery()
}

const handleTeacherSizeChange = (val) => {
  teacherQuery.size = val
  fetchTeacherCandidates()
}

const handleTeacherCurrentChange = (val) => {
  teacherQuery.page = val
  fetchTeacherCandidates()
}

const teacherIndexMethod = (index) => {
  return (teacherQuery.page - 1) * teacherQuery.size + index + 1
}

const confirmAddTeachers = async () => {
  const courseId = route.params.courseId
  if (!courseId) {
    return
  }
  if (!selectedTeacherIds.value || selectedTeacherIds.value.length === 0) {
    ElMessage.warning('请选择教师')
    return
  }

  teacherSaving.value = true
  try {
    await request.post(`/courses/${courseId}/teachers`, { teacherIds: selectedTeacherIds.value })
    ElMessage.success('添加成功')
    teacherPickerVisible.value = false
    fetchDetail()
  } catch (e) {
    console.error('添加教师失败:', e)
  } finally {
    teacherSaving.value = false
  }
}

const handleRemoveTeacher = async (teacherId) => {
  const courseId = route.params.courseId
  if (!courseId || !teacherId) {
    return
  }
  try {
    await ElMessageBox.confirm('确定要移除该教师吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }

  teacherSaving.value = true
  try {
    await request.delete(`/courses/${courseId}/teachers`, { params: { teacherId } })
    ElMessage.success('移除成功')
    fetchDetail()
  } catch (e) {
    console.error('移除教师失败:', e)
  } finally {
    teacherSaving.value = false
  }
}

const handleRemoveStudent = async (studentId) => {
  const courseId = route.params.courseId
  if (!courseId || !studentId) {
    return
  }
  if (!canRemoveStudentUI.value) {
    ElMessage.error('没有权限操作')
    return
  }

  try {
    await ElMessageBox.confirm('确定要移除该学生吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }

  studentSaving.value = true
  try {
    await request.delete(`/courses/${courseId}/students`, { params: { studentId } })
    ElMessage.success('移除成功')
    fetchDetail()
  } catch (e) {
    console.error('移除学生失败:', e)
  } finally {
    studentSaving.value = false
  }
}

const goBack = () => {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push('/')
  }
}

onMounted(() => {
  fetchDetail()
})
</script>

<style scoped>
.course-detail-page {
  padding: 20px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: bold;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.section-title {
  font-weight: bold;
  margin: 8px 0;
}

.teacher-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.picker-toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
}

.pagination-container {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}
</style>
