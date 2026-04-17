<template>
  <div class="share-list-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>交流分享</span>
          <el-button type="primary" @click="handlePublish">发布分享</el-button>
        </div>
      </template>

      <div class="search-bar">
        <el-input
          v-model="query.keyword"
          placeholder="搜索标题或内容"
          clearable
          style="width: 300px; margin-right: 10px"
          @keyup.enter="handleQuery"
        />
        <el-button type="primary" @click="handleQuery">查询</el-button>
      </div>

      <div v-loading="loading" class="share-container">
        <el-empty v-if="list.length === 0" description="暂无分享内容" />
        <div v-for="item in list" :key="item.id" class="share-item" :class="{ 'is-rejected': item.status === 2, 'is-pending': item.status === 0 }" @click="viewDetail(item.id)">
          <div class="share-main">
            <h3 class="share-title">
              <el-tag v-if="item.isTop" size="small" type="danger" effect="dark" style="margin-right: 8px">置顶</el-tag>
              <template v-if="isAdmin || item.userId === currentUserId">
                <el-tag v-if="item.status === 0" size="small" type="warning" style="margin-right: 8px">待审核</el-tag>
                <el-tag v-if="item.status === 2" size="small" type="info" style="margin-right: 8px">已驳回</el-tag>
              </template>
              {{ item.title }}
            </h3>
            <div class="share-summary">{{ getSummary(item.content) }}</div>
            <div class="share-info">
              <span class="author">
                <el-icon><User /></el-icon> {{ item.username }}
              </span>
              <span class="time">
                <el-icon><Clock /></el-icon> {{ formatDateTime(item.createTime) }}
              </span>
              <span class="stats">
                <el-icon><View /></el-icon> {{ item.viewCount }}
                <el-icon style="margin-left: 10px"><Pointer /></el-icon> {{ item.likeCount }}
              </span>
              <span v-if="isAdmin" class="audit-btn" @click.stop>
                <el-button v-if="item.status === 0" type="success" size="small" @click="handleAudit(item.id, 1)">通过</el-button>
                <el-button v-if="item.status === 0" type="danger" size="small" @click="handleAudit(item.id, 2)">驳回</el-button>
                <el-button :type="item.isTop ? 'warning' : 'info'" size="small" @click="handleToggleTop(item)">
                  {{ item.isTop ? '取消置顶' : '置顶' }}
                </el-button>
              </span>
            </div>
          </div>
        </div>
      </div>

      <div class="pagination-container">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 发布/修改对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="form.id ? '修改分享' : '发布分享'"
      width="60%"
    >
      <el-form :model="form" label-width="80px">
        <el-form-item label="标题" required>
          <el-input v-model="form.title" placeholder="请输入标题" />
        </el-form-item>
        <el-form-item label="内容" required>
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="10"
            placeholder="请输入分享内容（支持富文本/Markdown文本）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import { User, Clock, View, Pointer } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()
const currentUserId = computed(() => userStore.userInfo?.id)
const isAdmin = computed(() => userStore.userInfo?.roles?.includes('ADMIN'))
const loading = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({
  keyword: '',
  page: 1,
  size: 10
})

const dialogVisible = ref(false)
const submitting = ref(false)
const form = reactive({
  id: null,
  title: '',
  content: ''
})

const fetchList = async () => {
  loading.value = true
  try {
    const params = { ...query }
    // 如果是管理员，可以查看到待审核的，但这取决于后端逻辑。
    // 当前后端逻辑 getPublicShares 只返回 status=1 的。
    // 如果需要管理员看到待审核的，可能需要另一个接口。
    // 但为了简化，我们先只显示已发布的，管理员在详情页或者列表页如果能看到，就方便操作。
    const res = await request.get('/shares', { params })
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

const handlePublish = () => {
  form.id = null
  form.title = ''
  form.content = ''
  dialogVisible.value = true
}

const submitForm = async () => {
  if (!form.title.trim() || !form.content.trim()) {
    return ElMessage.warning('请填写标题和内容')
  }
  
  submitting.value = true
  try {
    const url = form.id ? `/shares/${form.id}` : '/shares'
    const method = form.id ? 'put' : 'post'
    const res = await request[method](url, form)
    if (res.code === 1) {
      ElMessage.success(form.id ? '修改成功' : '发布成功，请等待审核')
      dialogVisible.value = false
      fetchList()
    }
  } catch (e) {
    console.error(e)
  } finally {
    submitting.value = false
  }
}

const viewDetail = (id) => {
  router.push(`/shares/${id}`)
}

const handleAudit = async (shareId, status) => {
  try {
    const res = await request.put(`/admin/shares/${shareId}/audit`, { status })
    if (res.code === 1) {
      ElMessage.success('审核成功')
      fetchList()
    }
  } catch (e) {
    console.error(e)
  }
}

const handleToggleTop = async (item) => {
  try {
    const newStatus = item.isTop ? 0 : 1
    const res = await request.put(`/admin/shares/${item.id}/top`, null, {
      params: { isTop: newStatus }
    })
    if (res.code === 1) {
      ElMessage.success(newStatus ? '置顶成功' : '已取消置顶')
      fetchList()
    }
  } catch (e) {
    console.error(e)
  }
}

const getSummary = (content) => {
  if (!content) return ''
  const plainText = content.replace(/<[^>]+>/g, '')
  return plainText.length > 150 ? plainText.substring(0, 150) + '...' : plainText
}

const formatDateTime = (timeStr) => {
  if (!timeStr) return ''
  return timeStr.replace('T', ' ')
}

onMounted(() => {
  fetchList()
})
</script>

<style scoped>
.share-list-page {
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
.share-item {
  padding: 20px;
  border-bottom: 1px solid #ebeef5;
  cursor: pointer;
  transition: background-color 0.3s;
}
.share-item:hover {
  background-color: #f9f9f9;
}
.share-item.is-rejected {
  background-color: #fcfcfc;
  opacity: 0.7;
}
.share-item.is-pending {
  border-left: 4px solid #e6a23c;
}
.share-title {
  margin: 0 0 10px 0;
  font-size: 18px;
  color: #303133;
}
.share-summary {
  font-size: 14px;
  color: #606266;
  margin-bottom: 15px;
  line-height: 1.6;
}
.share-info {
  font-size: 13px;
  color: #909399;
  display: flex;
  align-items: center;
  gap: 20px;
}
.share-info .el-icon {
  vertical-align: middle;
  margin-right: 4px;
}
.audit-btn {
  margin-left: auto;
  display: flex;
  gap: 10px;
}
.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
