<template>
  <div class="course-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>课程列表</span>
          <div class="header-actions">
            <el-button type="primary" @click="openCreate">创建课程</el-button>
          </div>
        </div>
      </template>

      <div class="search-bar">
        <el-input v-model="query.keyword" placeholder="课程名称" clearable style="width: 220px; margin-right: 10px"
          @clear="handleQuery" @keyup.enter="handleQuery" />
        <el-select v-model="query.status" placeholder="状态" clearable style="width: 120px; margin-right: 10px">
          <el-option label="启用" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>
        <el-button type="primary" @click="handleQuery">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </div>

      <el-table :data="list" v-loading="loading" style="width: 100%; margin-top: 16px">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="courseName" label="课程名称" min-width="160" />
        <el-table-column prop="description" label="课程描述" min-width="200" show-overflow-tooltip />
        <el-table-column label="教师" min-width="160" show-overflow-tooltip>
          <template #default="scope">
            {{ formatTeachers(scope.row.teachers) }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.status === 1 ? 'success' : 'danger'">
              {{ scope.row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column prop="updateTime" label="更新时间" width="180" />
        <el-table-column label="操作" width="180">
          <template #default="scope">
            <el-button type="primary" size="small" @click="openEdit(scope.row)">修改</el-button>
            <el-button type="danger" size="small" @click="handleDelete(scope.row)">删除</el-button>
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
          <el-input v-model="createForm.cover" placeholder="http://.../cover.jpg" />
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
          <el-input v-model="editForm.cover" placeholder="http://.../cover.jpg" />
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
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

const loading = ref(false)

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

const formatTeachers = (teachers) => {
  if (!Array.isArray(teachers) || teachers.length === 0) {
    return '-'
  }
  const names = teachers.map((t) => t && t.name).filter((n) => n)
  return names.length ? names.join(', ') : '-'
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
  try {
    await ElMessageBox.confirm(`确定要删除课程【${row.courseName}】吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
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

onMounted(() => {
  fetchList()
})
</script>

<style scoped>
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

.search-bar {
  display: flex;
  align-items: center;
}

.pagination-container {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
