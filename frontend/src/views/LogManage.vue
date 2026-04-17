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
          style="width: 250px; margin-right: 10px"
          @keyup.enter="handleQuery"
        />
        <el-button type="primary" @click="handleQuery">查询</el-button>
      </div>

      <el-table :data="list" v-loading="loading" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="操作人" width="120" />
        <el-table-column prop="module" label="模块" width="120" />
        <el-table-column prop="operation" label="操作描述" width="180" />
        <el-table-column prop="ip" label="IP地址" width="140" />
        <el-table-column prop="createTime" label="操作时间" width="180">
          <template #default="scope">{{ formatDateTime(scope.row.createTime) }}</template>
        </el-table-column>
        <el-table-column prop="params" label="请求参数" min-width="200" show-overflow-tooltip />
        <el-table-column label="详情" width="80" fixed="right">
          <template #default="scope">
            <el-button type="primary" link @click="showDetail(scope.row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
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

const loading = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({
  module: '',
  page: 1,
  size: 10
})

const detailVisible = ref(false)
const currentLog = ref({})

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
