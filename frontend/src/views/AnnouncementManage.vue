<template>
  <div class="announcement-manage">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>公告管理</span>
          <el-button type="primary" @click="handleAdd">发布公告</el-button>
        </div>
      </template>

      <el-table :data="list" v-loading="loading" style="width: 100%">
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="isTop" label="置顶" width="100" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.isTop ? 'danger' : 'info'">
              {{ scope.row.isTop ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.status ? 'success' : 'info'">
              {{ scope.row.status ? '显示' : '隐藏' }}
            </el-tag>
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
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="fetchList"
        />
      </div>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="form.id ? '编辑公告' : '发布公告'"
      width="50%"
    >
      <el-form :model="form" label-width="100px" ref="formRef">
        <el-form-item label="标题" required>
          <el-input v-model="form.title" placeholder="请输入公告标题" />
        </el-form-item>
        <el-form-item label="内容" required>
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="6"
            placeholder="请输入公告内容"
          />
        </el-form-item>
        <el-form-item label="展示时间">
          <el-date-picker
            v-model="timeRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            @change="handleTimeChange"
          />
        </el-form-item>
        <el-form-item label="置顶">
          <el-switch v-model="form.isTop" :active-value="1" :inactive-value="0" />
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

const handleAdd = () => {
  Object.assign(form, {
    id: null,
    title: '',
    content: '',
    startTime: '',
    endTime: '',
    isTop: 0,
    status: 1
  })
  timeRange.value = []
  dialogVisible.value = true
}

const handleEdit = (row) => {
  Object.assign(form, row)
  timeRange.value = [row.startTime, row.endTime]
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

const handleTimeChange = (val) => {
  if (val) {
    form.startTime = val[0]
    form.endTime = val[1]
  } else {
    form.startTime = ''
    form.endTime = ''
  }
}

const submitForm = async () => {
  if (!form.title || !form.content) {
    return ElMessage.warning('请填写完整信息')
  }
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
  } catch (e) {
    console.error(e)
  } finally {
    submitting.value = false
  }
}

const formatDateTime = (time) => {
  if (!time) return '-'
  return time.replace('T', ' ')
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
</style>
