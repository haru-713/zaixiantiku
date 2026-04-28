<template>
  <div class="course-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>课程列表</span>
          <div class="header-actions">
            <el-button v-if="canManage" type="primary" @click="openCreate">创建课程</el-button>
          </div>
        </div>
      </template>

      <el-form :inline="true" :model="query" class="search-form">
        <el-form-item>
          <el-input v-model="query.keyword" placeholder="课程名称" clearable style="width: 200px" @clear="handleQuery"
            @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="list" v-loading="loading" stripe border style="width: 100%; margin-top: 10px">
        <el-table-column type="index" label="序号" width="70" :index="indexMethod" align="center" />
        <el-table-column label="封面" width="120" align="center">
          <template #default="scope">
            <el-image v-if="scope.row.cover" :src="scope.row.cover"
              style="width: 80px; height: 45px; border-radius: 4px" fit="cover" :preview-src-list="[scope.row.cover]"
              preview-teleported>
              <template #error>
                <div class="image-slot">
                  <el-icon>
                    <Picture />
                  </el-icon>
                </div>
              </template>
            </el-image>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="courseName" label="课程名称" min-width="160" />
        <el-table-column prop="description" label="课程描述" min-width="200" show-overflow-tooltip />
        <el-table-column label="教师" min-width="150" show-overflow-tooltip>
          <template #default="scope">
            {{ formatTeachers(scope.row.teachers) }}
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="160">
          <template #default="scope">
            {{ formatDateTime(scope.row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="scope">
            <el-button link type="primary" @click="openDetail(scope.row)">详情</el-button>
            <el-button v-if="canManage" link type="success" @click="manageStudents(scope.row)">学生</el-button>
            <el-button v-if="canManage" link type="primary" @click="openEdit(scope.row)">修改</el-button>
            <el-button v-if="isAdmin" link type="danger" @click="handleDelete(scope.row)">删除</el-button>
            <el-button v-else-if="isTeacher" link type="warning" @click="handleExit(scope.row)">退出</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <el-pagination :current-page="query.page" :page-size="query.size" :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper" :total="total" @size-change="handleSizeChange"
          @current-change="handleCurrentChange" />
      </div>
    </el-card>

    <el-dialog v-model="createVisible" title="创建课程" width="520px">
      <el-form :model="createForm" label-width="90px">
        <el-form-item label="课程名称">
          <el-input v-model="createForm.courseName" placeholder="请输入课程名称" />
        </el-form-item>
        <el-form-item label="课程描述">
          <el-input v-model="createForm.description" type="textarea" :rows="4" placeholder="请输入课程描述" />
        </el-form-item>
        <el-form-item label="封面URL">
          <div class="cover-row">
            <el-input v-model="createForm.cover" disabled />
            <el-upload :show-file-list="false" :http-request="uploadCreateCover" :before-upload="beforeCoverUpload">
              <el-button :loading="createCoverUploading" type="primary">上传封面</el-button>
            </el-upload>
          </div>
          <div v-if="createForm.cover" class="cover-preview">
            <el-image :src="createForm.cover" style="width: 160px; height: 90px" fit="cover" />
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="createVisible = false">取消</el-button>
          <el-button type="primary" :loading="createLoading" @click="handleCreate">创建</el-button>
        </span>
      </template>
    </el-dialog>

    <el-dialog v-model="editVisible" title="修改课程" width="520px">
      <el-form :model="editForm" label-width="90px">
        <el-form-item label="课程名称">
          <el-input v-model="editForm.courseName" placeholder="请输入课程名称" />
        </el-form-item>
        <el-form-item label="课程描述">
          <el-input v-model="editForm.description" type="textarea" :rows="4" placeholder="请输入课程描述" />
        </el-form-item>
        <el-form-item label="封面URL">
          <div class="cover-row">
            <el-input v-model="editForm.cover" disabled />
            <el-upload :show-file-list="false" :http-request="uploadEditCover" :before-upload="beforeCoverUpload">
              <el-button :loading="editCoverUploading" type="primary">上传封面</el-button>
            </el-upload>
          </div>
          <div v-if="editForm.cover" class="cover-preview">
            <el-image :src="editForm.cover" style="width: 160px; height: 90px" fit="cover" />
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="editVisible = false">取消</el-button>
          <el-button type="primary" :loading="editLoading" @click="handleEdit">保存</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'
import { useUserStore } from '@/store/user'
import { useRouter } from 'vue-router'
import { Picture } from '@element-plus/icons-vue'

const loading = ref(false)

const userStore = useUserStore()
const router = useRouter()
const isAdmin = computed(() => (userStore.userInfo?.roles || []).includes('ADMIN'))
const isTeacher = computed(() => (userStore.userInfo?.roles || []).includes('TEACHER'))
const canManage = computed(() => isAdmin.value || isTeacher.value)

const createCoverUploading = ref(false)
const editCoverUploading = ref(false)

const list = ref([])
const total = ref(0)

const query = reactive({
  page: 1,
  size: 10,
  keyword: '',
  status: undefined
})

const fetchList = async () => {
  loading.value = true
  try {
    const res = await request.get('/courses', { params: query })
    list.value = res.data.list
    total.value = res.data.total
  } catch (e) {
    console.error('获取课程列表失败:', e)
  } finally {
    loading.value = false
  }
}

const indexMethod = (index) => {
  return (query.page - 1) * query.size + index + 1
}

const formatTeachers = (teachers) => {
  if (!Array.isArray(teachers) || teachers.length === 0) {
    return '-'
  }
  const names = teachers.map((t) => t && t.name).filter((n) => n)
  return names.length ? names.join(', ') : '-'
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

const handleQuery = () => {
  query.page = 1
  fetchList()
}

const resetQuery = () => {
  query.keyword = ''
  query.status = undefined
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

const createVisible = ref(false)
const createLoading = ref(false)
const createForm = reactive({
  courseName: '',
  description: '',
  cover: ''
})

const openCreate = () => {
  createForm.courseName = ''
  createForm.description = ''
  createForm.cover = ''
  createVisible.value = true
}

const handleCreate = async () => {
  if (!createForm.courseName) {
    ElMessage.warning('请输入课程名称')
    return
  }

  createLoading.value = true
  try {
    const res = await request.post('/courses', {
      courseName: createForm.courseName,
      description: createForm.description,
      cover: createForm.cover
    })
    ElMessage.success(`创建成功：ID=${res.data.id}`)
    createVisible.value = false
    fetchList()
  } catch (e) {
    console.error('创建课程失败:', e)
  } finally {
    createLoading.value = false
  }
}

const editVisible = ref(false)
const editLoading = ref(false)
const editCourseId = ref(null)
const editForm = reactive({
  courseName: '',
  description: '',
  cover: ''
})

const openEdit = (row) => {
  editCourseId.value = row.id
  editForm.courseName = row.courseName || ''
  editForm.description = row.description || ''
  editForm.cover = row.cover || ''
  editVisible.value = true
}

const openDetail = (row) => {
  if (!row || !row.id) {
    return
  }
  router.push(`/course/detail/${row.id}`)
}

const manageStudents = (row) => {
  if (!row || !row.id) {
    return
  }
  router.push({
    path: '/course/students',
    query: { courseId: row.id }
  })
}

const handleEdit = async () => {
  if (!editCourseId.value) {
    return
  }
  if (!editForm.courseName) {
    ElMessage.warning('请输入课程名称')
    return
  }

  editLoading.value = true
  try {
    await request.put(`/courses/${editCourseId.value}`, {
      courseName: editForm.courseName,
      description: editForm.description,
      cover: editForm.cover
    })
    ElMessage.success('修改成功')
    editVisible.value = false
    fetchList()
  } catch (e) {
    console.error('修改课程失败:', e)
  } finally {
    editLoading.value = false
  }
}

const handleDelete = async (row) => {
  const confirmMsg = isAdmin.value
    ? `确定要强制删除课程【${row.courseName}】吗？这将级联删除该课程下的所有教师关联、选课学生、题目、试卷及考试记录，且不可恢复！`
    : `确定要删除课程【${row.courseName}】吗？删除前请确保该课程下已无任何教师、学生、题目和知识点。`

  try {
    await ElMessageBox.confirm(confirmMsg, '警告', {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'error'
    })
  } catch {
    return
  }

  loading.value = true
  try {
    await request.delete(`/courses/${row.id}`)
    ElMessage.success('删除成功')
    fetchList()
  } catch (e) {
    console.error('删除课程失败:', e)
  } finally {
    loading.value = false
  }
}

const handleExit = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要退出课程【${row.courseName}】的任课教师团队吗？`, '退出提示', {
      confirmButtonText: '确定退出',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }

  loading.value = true
  try {
    // 使用通用的移除教师接口，传入当前教师自己的 ID 实现退出
    await request.delete(`/courses/${row.id}/teachers`, { params: { teacherId: userStore.userInfo.id } })
    ElMessage.success('已退出该课程')
    fetchList()
  } catch (e) {
    console.error('退出课程失败:', e)
  } finally {
    loading.value = false
  }
}

const beforeCoverUpload = (file) => {
  const isImage = file.type && file.type.startsWith('image/')
  if (!isImage) {
    ElMessage.error('只能上传图片')
    return false
  }
  const isLt5M = file.size / 1024 / 1024 < 5
  if (!isLt5M) {
    ElMessage.error('图片大小不能超过 5MB')
    return false
  }
  return true
}

const uploadCreateCover = async (options) => {
  const file = options.file
  const formData = new FormData()
  formData.append('file', file)
  createCoverUploading.value = true
  try {
    const res = await request.post('/upload/course-cover', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    createForm.cover = res.data
    options.onSuccess && options.onSuccess(res, file)
    ElMessage.success('上传成功')
  } catch (e) {
    options.onError && options.onError(e)
  } finally {
    createCoverUploading.value = false
  }
}

const uploadEditCover = async (options) => {
  const file = options.file
  const formData = new FormData()
  formData.append('file', file)
  editCoverUploading.value = true
  try {
    const res = await request.post('/upload/course-cover', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    editForm.cover = res.data
    options.onSuccess && options.onSuccess(res, file)
    ElMessage.success('上传成功')
  } catch (e) {
    options.onError && options.onError(e)
  } finally {
    editCoverUploading.value = false
  }
}

onMounted(() => {
  fetchList()
})
</script>

<style scoped>
.course-page {
  width: 100%;
}

.search-form {
  background-color: #F8FAFC;
  padding: 16px 16px 0;
  border-radius: 8px;
  margin-bottom: 20px;
  border: 1px solid var(--border-color);
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.cover-row {
  display: flex;
  gap: 10px;
  width: 100%;
}

.cover-preview {
  margin-top: 10px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  overflow: hidden;
  width: 160px;
  height: 90px;
}

.image-slot {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%;
  height: 100%;
  background: #f5f7fa;
  color: #909399;
}
</style>
