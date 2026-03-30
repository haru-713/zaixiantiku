<template>
  <div class="admin-user-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>用户管理</span>
        </div>
      </template>

      <!-- 搜索栏 -->
      <div class="search-bar">
        <el-input v-model="queryParams.keyword" placeholder="用户名/姓名/手机号" style="width: 200px; margin-right: 10px"
          clearable @clear="handleQuery" @keyup.enter="handleQuery" />
        <el-select v-model="queryParams.roleCode" placeholder="角色" clearable style="width: 120px; margin-right: 10px">
          <el-option label="管理员" value="ADMIN" />
          <el-option label="教师" value="TEACHER" />
          <el-option label="学生" value="STUDENT" />
        </el-select>
        <el-select v-model="queryParams.status" placeholder="状态" clearable style="width: 100px; margin-right: 10px">
          <el-option label="正常" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>
        <el-select v-model="queryParams.auditStatus" placeholder="审核状态" clearable
          style="width: 120px; margin-right: 10px">
          <el-option label="待审核" :value="0" />
          <el-option label="审核通过" :value="1" />
          <el-option label="审核拒绝" :value="2" />
        </el-select>
        <el-button type="primary" @click="handleQuery">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </div>

      <!-- 数据表格 -->
      <el-table :data="userList" v-loading="loading" style="width: 100%; margin-top: 20px">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="头像" width="80">
          <template #default="scope">
            <el-avatar :size="40" :src="scope.row.avatar" />
          </template>
        </el-table-column>
        <el-table-column prop="username" label="用户名" />
        <el-table-column prop="name" label="姓名" />
        <el-table-column prop="phone" label="手机号" />
        <el-table-column label="角色">
          <template #default="scope">
            <el-tag v-for="role in scope.row.roleCodes" :key="role" style="margin-right: 5px">
              {{ role === 'ADMIN' ? '管理员' : (role === 'TEACHER' ? '教师' : '学生') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.status === 1 ? 'success' : 'danger'">
              {{ scope.row.status === 1 ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="审核状态" width="120">
          <template #default="scope">
            <el-tag v-if="scope.row.roleCodes.includes('STUDENT')" :type="auditStatusType(scope.row.auditStatus)">
              {{ auditStatusLabel(scope.row.auditStatus) }}
            </el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template #default="scope">
            <el-button v-if="isAdmin" :type="scope.row.status === 1 ? 'danger' : 'success'" size="small"
              @click="handleToggleStatus(scope.row.id, scope.row.status)">
              {{ scope.row.status === 1 ? '禁用' : '启用' }}
            </el-button>

            <template v-if="isAdmin && scope.row.roleCodes.includes('STUDENT') && scope.row.auditStatus === 0">
              <el-button type="success" size="small" @click="handleAudit(scope.row.id, 1)">通过</el-button>
              <el-button type="warning" size="small" @click="handleAudit(scope.row.id, 2)">拒绝</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination :current-page="queryParams.page" :page-size="queryParams.size" :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper" :total="total" @size-change="handleSizeChange"
          @current-change="handleCurrentChange" />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import request from '@/utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const isAdmin = computed(() => (userStore.userInfo?.roles || []).includes('ADMIN'))

const loading = ref(false)
const userList = ref([])
const total = ref(0)

const queryParams = reactive({
  page: 1,
  size: 10,
  keyword: '',
  roleCode: '',
  status: undefined,
  auditStatus: undefined
})

const fetchUserList = async () => {
  loading.value = true
  try {
    const response = await request.get('/admin/users', { params: queryParams })
    userList.value = response.data.list
    total.value = response.data.total
  } catch (error) {
    console.error('获取用户列表失败:', error)
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  queryParams.page = 1
  fetchUserList()
}

const resetQuery = () => {
  queryParams.keyword = ''
  queryParams.roleCode = ''
  queryParams.status = undefined
  queryParams.auditStatus = undefined
  handleQuery()
}

const handleSizeChange = (val) => {
  queryParams.size = val
  fetchUserList()
}

const handleCurrentChange = (val) => {
  queryParams.page = val
  fetchUserList()
}

const auditStatusLabel = (status) => {
  const labels = { 0: '待审核', 1: '通过', 2: '拒绝' }
  return labels[status] || '未知'
}

const auditStatusType = (status) => {
  const types = { 0: 'warning', 1: 'success', 2: 'danger' }
  return types[status] || 'info'
}

const handleAudit = async (userId, auditStatus) => {
  if (!isAdmin.value) {
    return
  }
  let reason = ''
  if (auditStatus === 2) {
    try {
      const res = await ElMessageBox.prompt('请输入拒绝原因', '审核拒绝', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputPlaceholder: '例如：信息不完整'
      })
      reason = res.value || ''
    } catch {
      return
    }
  } else {
    reason = '审核通过'
  }

  loading.value = true
  try {
    await request.put(`/admin/users/${userId}/audit`, { auditStatus, reason })
    ElMessage.success('审核完成')
    fetchUserList()
  } catch (e) {
    console.error('审核失败:', e)
  } finally {
    loading.value = false
  }
}

const handleToggleStatus = async (userId, currentStatus) => {
  if (!isAdmin.value) {
    return
  }
  const nextStatus = currentStatus === 1 ? 0 : 1
  const actionText = nextStatus === 1 ? '启用' : '禁用'

  try {
    await ElMessageBox.confirm(`确定要${actionText}该用户吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }

  loading.value = true
  try {
    await request.put(`/admin/users/${userId}/status`, { status: nextStatus })
    ElMessage.success('状态已更新')
    fetchUserList()
  } catch (e) {
    console.error('状态更新失败:', e)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchUserList()
})
</script>

<style scoped>
.admin-user-container {
  padding: 20px;
}

.search-bar {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
