<template>
  <div class="log-manage">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>操作日志</span>
        </div>
      </template>

      <div class="search-bar">
        <el-input
          v-model="query.module"
          placeholder="搜索模块名称"
          clearable
          style="width: 200px; margin-right: 10px"
          @keyup.enter="handleQuery"
        />
        <el-input
          v-model="query.username"
          placeholder="搜索操作人"
          clearable
          style="width: 200px; margin-right: 10px"
          @keyup.enter="handleQuery"
        />
        <el-button type="primary" @click="handleQuery">查询</el-button>
        <el-button
          type="danger"
          plain
          :disabled="selectedIds.length === 0"
          @click="handleBatchDelete"
          >批量删除</el-button
        >
        <el-button type="danger" @click="handleClear">清空日志</el-button>
      </div>

      <el-table
        :data="list"
        v-loading="loading"
        style="width: 100%"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column type="index" label="序号" width="80" :index="indexMethod" />
        <el-table-column prop="username" label="操作人" width="120" />
        <el-table-column prop="module" label="模块" width="120" />
        <el-table-column prop="operation" label="操作描述" width="180" />
        <el-table-column prop="ip" label="IP地址" width="140" />
        <el-table-column prop="createTime" label="操作时间" width="180">
          <template #default="scope">{{ formatDateTime(scope.row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="scope">
            <el-button type="primary" link @click="showDetail(scope.row)">详情</el-button>
            <el-button type="danger" link @click="handleDelete(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <el-pagination
          :current-page="query.page"
          :page-size="query.size"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
          @update:current-page="query.page = $event"
          @update:page-size="query.size = $event"
        />
      </div>
    </el-card>

    <!-- 日志详情对话框 -->
    <el-dialog v-model="detailVisible" title="日志详情" width="50%">
      <el-form label-width="80px">
        <el-form-item label="操作人">{{ currentLog.username }} (ID: {{ currentLog.userId }})</el-form-item>
        <el-form-item label="模块">{{ currentLog.module }}</el-form-item>
        <el-form-item label="操作描述">{{ currentLog.operation }}</el-form-item>
        <el-form-item label="IP地址">{{ currentLog.ip }}</el-form-item>
        <el-form-item label="操作时间">{{ formatDateTime(currentLog.createTime) }}</el-form-item>
        <el-form-item label="请求参数">
          <pre class="json-content">{{ currentLog.params }}</pre>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
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
  module: '',
  username: '',
  page: 1,
  size: 10
})

const detailVisible = ref(false)
const currentLog = ref({})
const selectedIds = ref([])

const fetchList = async () => {
  loading.value = true
  try {
    const res = await request.get('/admin/logs', { params: query })
    if (res.code === 1 || res.code === 200) {
      list.value = res.data.list
      total.value = res.data.total
    }
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  query.page = 1
  fetchList()
}

const handleSizeChange = () => {
  query.page = 1
  fetchList()
}

const handleCurrentChange = () => {
  fetchList()
}

const showDetail = (row) => {
  currentLog.value = row
  detailVisible.value = true
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除这条日志吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    const res = await request.delete(`/admin/logs/${row.id}`)
    if (res.code === 1 || res.code === 200) {
      ElMessage.success('删除成功')
      fetchList()
    }
  } catch (e) {
    if (e !== 'cancel') {
      console.error(e)
    }
  }
}

const handleBatchDelete = async () => {
  if (selectedIds.value.length === 0) return
  try {
    await ElMessageBox.confirm(`确定要删除选中的 ${selectedIds.value.length} 条日志吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    const res = await request.delete('/admin/logs/batch', { data: selectedIds.value })
    if (res.code === 1 || res.code === 200) {
      ElMessage.success('批量删除成功')
      fetchList()
    }
  } catch (e) {
    if (e !== 'cancel') {
      console.error(e)
    }
  }
}

const handleClear = async () => {
  try {
    await ElMessageBox.confirm('确定要清空所有日志吗？此操作不可恢复！', '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'error'
    })
    const res = await request.delete('/admin/logs/clear')
    if (res.code === 1 || res.code === 200) {
      ElMessage.success('日志已清空')
      fetchList()
    }
  } catch (e) {
    if (e !== 'cancel') {
      console.error(e)
    }
  }
}

const handleSelectionChange = (selection) => {
  selectedIds.value = selection.map(item => item.id)
}

const indexMethod = (index) => {
  return (query.page - 1) * query.size + index + 1
}

const formatDateTime = (time) => {
  if (!time) return '-'
  return time.replace('T', ' ')
}

onMounted(fetchList)
</script>

<style scoped>
.log-manage {
  padding: 20px;
}
.search-bar {
  margin-bottom: 20px;
}
.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
.json-content {
  background: #f5f7fa;
  padding: 10px;
  border-radius: 4px;
  font-family: monospace;
  white-space: pre-wrap;
  word-break: break-all;
  max-height: 300px;
  overflow-y: auto;
}
</style>
