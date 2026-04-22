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
        <el-descriptions-item label="课程名称">{{ detail.courseName }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="detail.status === 1 ? 'success' : 'danger'">
            {{ detail.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatDateTime(detail.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ formatDateTime(detail.updateTime) }}</el-descriptions-item>
        <el-descriptions-item label="封面" :span="2">
          <el-image v-if="detail.cover" :src="detail.cover" style="width: 240px; height: 135px" fit="cover">
            <template #error>
              <div class="image-slot">
                <el-icon>
                  <Picture />
                </el-icon>
              </div>
            </template>
          </el-image>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item label="课程描述" :span="2">
          <span>{{ detail.description || '-' }}</span>
        </el-descriptions-item>
      </el-descriptions>

      <el-divider />

      <div class="section-title">教师列表</div>
      <div v-if="canAddTeacherUI" class="teacher-actions">
        <el-button type="primary" @click="openTeacherPicker">添加教师</el-button>
      </div>

      <el-table :data="detail.teachers || []" style="width: 100%">
        <el-table-column type="index" label="序号" width="80" />
        <el-table-column prop="name" label="姓名" />
        <el-table-column label="操作" width="120">
          <template #default="scope">
            <el-button v-if="canRemoveTeacherUI(scope.row)" link type="danger"
              @click="handleRemoveTeacher(scope.row)">移除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <template v-if="localCanManageStudents">
        <el-divider />
        <div class="student-manage-entry">
          <el-button type="success" size="large" @click="goToStudentManage">
            <el-icon style="margin-right: 8px">
              <User />
            </el-icon>
            管理课程学生
          </el-button>
          <p class="hint">点击进入专用的学生管理页面，进行按班级批量导入、筛选及移除操作。</p>
        </div>
      </template>
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
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import request from '@/utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Picture, User } from '@element-plus/icons-vue'
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
  canAddTeacher: false,
  canRemoveTeacher: false,
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

const canRemoveTeacherUI = (row) => {
  // 管理员和课程创建者有权移除教师
  if (localIsAdmin.value || detail.value.canRemoveTeacher === true) {
    return true
  }
  // 教师可以退出自己教授的课程
  return row.id === currentUserId.value
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

const handleRemoveTeacher = async (row) => {
  const courseId = route.params.courseId
  if (!courseId || !row.id) return

  const isSelf = row.id === currentUserId.value
  const confirmMsg = isSelf
    ? '确定要退出该课程的授课吗？'
    : `确定将教师 [${row.name}] 从本课程中移除吗？`

  try {
    await ElMessageBox.confirm(confirmMsg, '操作确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await request.delete(`/courses/${courseId}/teachers`, { params: { teacherId: row.id } })
    ElMessage.success(isSelf ? '已成功退出课程' : '移除成功')
    fetchDetail()
  } catch (e) {
    if (e !== 'cancel') {
      console.error('移除教师失败:', e)
    }
  }
}

const goToStudentManage = () => {
  router.push({
    path: '/course/students',
    query: { courseId: detail.value.id }
  })
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

.image-slot {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%;
  height: 100%;
  background: #f5f7fa;
  color: #909399;
  font-size: 30px;
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

.student-manage-entry {
  padding: 30px 0;
  text-align: center;
  background-color: #f8f9fa;
  border-radius: 8px;
}

.student-manage-entry .hint {
  margin-top: 12px;
  color: #909399;
  font-size: 14px;
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
