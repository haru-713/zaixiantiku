<template>
  <div class="announcement-manage">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>公告管理</span>
          <div class="header-actions">
            <el-button type="danger" plain @click="handleDeleteExpired">清除过期公告</el-button>
            <el-button type="primary" @click="handleAdd">发布公告</el-button>
          </div>
        </div>
      </template>

      <el-table :data="list" v-loading="loading" style="width: 100%" :row-class-name="tableRowClassName">
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="isTop" label="置顶" width="100" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.isTop ? 'danger' : 'info'">
              {{ scope.row.isTop ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="120" align="center">
          <template #default="scope">
            <el-space direction="vertical" :size="4">
              <el-tag :type="scope.row.status ? 'success' : 'info'" size="small">
                {{ scope.row.status ? '显示' : '隐藏' }}
              </el-tag>
              <el-tag v-if="isExpired(scope.row.endTime)" type="danger" size="small">
                已过期
              </el-tag>
            </el-space>
          </template>
        </el-table-column>
        <el-table-column prop="startTime" label="开始时间" width="180">
          <template #default="scope">{{ formatDateTime(scope.row.startTime) }}</template>
        </el-table-column>
        <el-table-column prop="endTime" label="结束时间" width="180">
          <template #default="scope">{{ formatDateTime(scope.row.endTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="scope">
            <el-button type="primary" link @click="handleEdit(scope.row)">编辑</el-button>
            <el-button type="danger" link @click="handleDelete(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <el-pagination
          :current-page="query.page"
          :page-size="query.size"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="val => { query.page = val; fetchList(); }"
          @size-change="val => { query.size = val; fetchList(); }"
        />
      </div>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="form.id ? '编辑公告' : '发布公告'"
      width="50%"
      @close="resetForm"
    >
      <el-alert
        v-if="form.id && isExpired(form.endTime)"
        title="该公告已过期，如需在首页重新显示，请延长“展示时间”。"
        type="warning"
        show-icon
        :closable="false"
        style="margin-bottom: 20px"
      />
      <el-form :model="form" :rules="rules" label-width="100px" ref="formRef">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入公告标题" />
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="6"
            placeholder="请输入公告内容"
          />
        </el-form-item>
        <el-form-item label="展示时间" prop="startTime">
          <el-date-picker
            v-model="timeRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            :default-time="[new Date(2000, 1, 1, 0, 0, 0), new Date(2000, 1, 1, 23, 59, 59)]"
            @change="handleTimeChange"
          />
        </el-form-item>
        <el-form-item label="置顶">
          <el-switch v-model="form.isTop" :active-value="1" :inactive-value="0" />
          <span style="margin-left: 10px; color: #909399; font-size: 12px;">注：首页将固定展示最新的一条置顶公告</span>
        </el-form-item>
        <el-form-item label="显示状态">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">显示</el-radio>
            <el-radio :label="0">隐藏</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm" :loading="submitting">确定</el-button>
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
  size: 10
})

const dialogVisible = ref(false)
const submitting = ref(false)
const formRef = ref(null)
const timeRange = ref([])
const form = reactive({
  id: null,
  title: '',
  content: '',
  startTime: '',
  endTime: '',
  isTop: 0,
  status: 1
})

const rules = {
  title: [{ required: true, message: '请输入公告标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入公告内容', trigger: 'blur' }],
  startTime: [{ required: true, message: '请选择展示时间', trigger: 'change' }]
}

const fetchList = async () => {
  loading.value = true
  try {
    const res = await request.get('/announcements', { params: query })
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

const resetForm = () => {
  if (formRef.value) {
    formRef.value.resetFields()
  }
  form.id = null
  form.title = ''
  form.content = ''
  form.startTime = ''
  form.endTime = ''
  form.isTop = 0
  form.status = 1
  timeRange.value = []
}

const handleAdd = () => {
  resetForm()
  dialogVisible.value = true
}

const handleEdit = (row) => {
  resetForm()
  Object.assign(form, row)
  if (form.startTime && form.endTime) {
    timeRange.value = [form.startTime, form.endTime]
  }
  dialogVisible.value = true
}

const handleDelete = (row) => {
  ElMessageBox.confirm('确定要删除这条公告吗？', '提示', { type: 'warning' }).then(async () => {
    const res = await request.delete(`/announcements/${row.id}`)
    if (res.code === 1) {
      ElMessage.success('删除成功')
      fetchList()
    }
  })
}

const handleDeleteExpired = () => {
  ElMessageBox.confirm('确定要清空所有已过期的公告吗？此操作不可撤销。', '警告', {
    type: 'warning',
    confirmButtonText: '确定清空',
    confirmButtonClass: 'el-button--danger'
  }).then(async () => {
    const res = await request.delete('/announcements/expired')
    if (res.code === 1) {
      ElMessage.success('清理成功')
      fetchList()
    }
  })
}

const handleTimeChange = (val) => {
  if (val) {
    form.startTime = val[0]
    form.endTime = val[1]
  } else {
    form.startTime = ''
    form.endTime = ''
  }
}

const tableRowClassName = ({ row }) => {
  if (isExpired(row.endTime)) {
    return 'expired-row'
  }
  return ''
}

const submitForm = async () => {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitting.value = true
      try {
        const url = form.id ? `/announcements/${form.id}` : '/announcements'
        const method = form.id ? 'put' : 'post'
        const res = await request[method](url, form)
        if (res.code === 1) {
          ElMessage.success(form.id ? '修改成功' : '发布成功')
          dialogVisible.value = false
          fetchList()
        }
      } catch (error) {
        console.error('保存公告失败:', error)
      } finally {
        submitting.value = false
      }
    }
  })
}

const formatDateTime = (time) => {
  if (!time) return '-'
  if (typeof time !== 'string') {
    if (Array.isArray(time)) {
      const [y, m, d, h, min, s] = time
      return `${y}-${String(m).padStart(2, '0')}-${String(d).padStart(2, '0')} ${String(h).padStart(2, '0')}:${String(min).padStart(2, '0')}:${String(s).padStart(2, '0')}`
    }
    return '-'
  }
  return time.replace('T', ' ')
}

const isExpired = (endTime) => {
  if (!endTime) return false
  return new Date(endTime) < new Date()
}

onMounted(fetchList)
</script>

<style scoped>
.announcement-manage {
  padding: 20px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

:deep(.expired-row) {
  background-color: #f5f7fa !important;
  color: #c0c4cc;
}

:deep(.expired-row .el-tag--success) {
  filter: grayscale(1);
  opacity: 0.6;
}
</style>
