<template>
  <div class="announcement-list-container">
    <el-card class="list-card">
      <template #header>
        <div class="card-header">
          <div class="header-title">
            <el-icon><Notification /></el-icon>
            <span>系统公告</span>
          </div>
          <div class="header-search">
            <el-input
              v-model="queryParams.keyword"
              placeholder="搜索公告标题..."
              clearable
              style="width: 250px"
              @keyup.enter="handleQuery"
            >
              <template #append>
                <el-button @click="handleQuery">
                  <el-icon><Search /></el-icon>
                </el-button>
              </template>
            </el-input>
          </div>
        </div>
      </template>

      <div class="announcement-list" v-loading="loading">
        <template v-if="list.length > 0">
          <div 
            v-for="item in list" 
            :key="item.id" 
            class="announcement-row"
            @click="showDetail(item)"
          >
            <div class="row-main">
              <div class="row-title-area">
                <el-tag v-if="item.isTop" type="danger" size="small" effect="dark" class="top-tag">置顶</el-tag>
                <span class="row-title">{{ item.title }}</span>
              </div>
              <div class="row-content">{{ truncateContent(item.content) }}</div>
            </div>
            <div class="row-meta">
              <span class="time">
                <el-icon><Calendar /></el-icon>
                {{ formatDateTime(item.createTime) }}
              </span>
              <el-icon class="arrow-icon"><ArrowRight /></el-icon>
            </div>
          </div>
        </template>
        <el-empty v-else description="暂无公告" />
      </div>

      <div class="pagination-container" v-if="total > 0">
        <el-pagination
          :current-page="queryParams.page"
          :page-size="queryParams.size"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <!-- 详情对话框 -->
    <el-dialog
      v-model="detailVisible"
      :title="currentAnnouncement.title"
      width="700px"
      center
      destroy-on-close
    >
      <div class="announcement-detail">
        <div class="meta">
          <span><el-icon><Calendar /></el-icon> 发布时间：{{ formatDateTime(currentAnnouncement.createTime) }}</span>
          <el-tag v-if="currentAnnouncement.isTop" type="danger" size="small" style="margin-left: 10px">置顶</el-tag>
        </div>
        <div class="content">{{ currentAnnouncement.content }}</div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import request from '@/utils/request'
import { Notification, Search, Calendar, ArrowRight } from '@element-plus/icons-vue'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const queryParams = reactive({
  page: 1,
  size: 10,
  keyword: '',
  publicView: true
})

const detailVisible = ref(false)
const currentAnnouncement = ref({})

const fetchList = async () => {
  loading.value = true
  try {
    const res = await request.get('/announcements', { params: queryParams })
    if (res.code === 1) {
      list.value = res.data.list
      total.value = res.data.total
    }
  } catch (error) {
    console.error('获取公告列表失败:', error)
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  queryParams.page = 1
  fetchList()
}

const handlePageChange = (val) => {
  queryParams.page = val
  fetchList()
  window.scrollTo(0, 0)
}

const showDetail = (item) => {
  currentAnnouncement.value = item
  detailVisible.value = true
}

const truncateContent = (content) => {
  if (!content) return ''
  return content.length > 150 ? content.substring(0, 150) + '...' : content
}

const formatDateTime = (time) => {
  if (!time) return '-'
  if (typeof time !== 'string') {
    if (Array.isArray(time)) {
      return `${time[0]}-${String(time[1]).padStart(2, '0')}-${String(time[2]).padStart(2, '0')}`
    }
    return '-'
  }
  return time.split('T')[0]
}

onMounted(fetchList)
</script>

<style scoped>
.announcement-list-container {
  padding: 0;
}

.list-card {
  border: none;
  min-height: calc(100vh - 120px);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 18px;
  font-weight: 700;
  color: #1e293b;
}

.header-title .el-icon {
  color: var(--el-color-warning);
}

.announcement-list {
  padding: 10px 0;
}

.announcement-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24px 20px;
  border-bottom: 1px solid #f1f5f9;
  cursor: pointer;
  transition: all 0.2s;
}

.announcement-row:hover {
  background-color: #f8fafc;
}

.row-main {
  flex: 1;
  padding-right: 40px;
}

.row-title-area {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.row-title {
  font-size: 17px;
  font-weight: 700;
  color: #1e293b;
}

.row-content {
  font-size: 14px;
  color: #64748b;
  line-height: 1.6;
}

.row-meta {
  display: flex;
  align-items: center;
  gap: 20px;
  color: #94a3b8;
  font-size: 14px;
}

.row-meta .time {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 120px;
}

.arrow-icon {
  font-size: 16px;
  transition: transform 0.2s;
}

.announcement-row:hover .arrow-icon {
  transform: translateX(4px);
  color: var(--el-color-primary);
}

.pagination-container {
  margin-top: 30px;
  display: flex;
  justify-content: center;
}

.announcement-detail .meta {
  display: flex;
  align-items: center;
  color: #94a3b8;
  font-size: 14px;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f1f5f9;
}

.announcement-detail .content {
  font-size: 16px;
  line-height: 1.8;
  color: #334155;
  white-space: pre-wrap;
}
</style>
