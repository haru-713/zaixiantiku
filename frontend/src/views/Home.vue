<template>
  <div class="home-container">
    <h1>欢迎来到在线题库系统</h1>
    
    <!-- 公告展示区 -->
    <div class="announcement-container" v-if="topAnnouncement || otherAnnouncements.length > 0">
      <!-- 置顶公告（固定展示） -->
      <div v-if="topAnnouncement" class="announcement-top" @click="showAnnouncementDetail(topAnnouncement)">
        <div class="announcement-item top-item">
          <el-tag type="danger" size="small" effect="dark">置顶</el-tag>
          <span class="announcement-title">{{ topAnnouncement.title }}</span>
          <span class="announcement-time">{{ formatDateTime(topAnnouncement.createTime) }}</span>
        </div>
      </div>
      
      <!-- 普通公告（下方轮播） -->
      <div v-if="otherAnnouncements.length > 0" class="announcement-carousel">
        <el-carousel height="50px" direction="vertical" :autoplay="true" indicator-position="none" :interval="4000">
          <el-carousel-item v-for="item in otherAnnouncements" :key="item.id">
            <div class="announcement-item normal-item" @click="showAnnouncementDetail(item)">
              <span class="announcement-title">{{ item.title }}</span>
              <span class="announcement-time">{{ formatDateTime(item.createTime) }}</span>
            </div>
          </el-carousel-item>
        </el-carousel>
      </div>
    </div>

    <div v-if="loading">加载中...</div>
    <div v-else-if="userInfo.name">
      <el-card class="user-card">
        <template #header>
          <div class="card-header">
            <span>我的个人信息</span>
          </div>
        </template>
        <div class="user-info">
          <p><strong>用户名:</strong> {{ userInfo.username }}</p>
          <p><strong>姓名:</strong> {{ userInfo.name }}</p>
          <p><strong>手机号:</strong> {{ userInfo.phone || '未填写' }}</p>
          <p><strong>邮箱:</strong> {{ userInfo.email || '未填写' }}</p>
          <p><strong>角色:</strong> {{ userInfo.roles ? userInfo.roles.join(', ') : '' }}</p>
          <p><strong>审核状态:</strong>
            <el-tag :type="userInfo.auditStatus === 1 ? 'success' : 'warning'">
              {{ userInfo.auditStatus === 1 ? '已通过' : '待审核' }}
            </el-tag>
          </p>
        </div>
      </el-card>
    </div>

    <!-- 公告详情对话框 -->
    <el-dialog
      v-model="detailVisible"
      :title="currentAnnouncement.title"
      width="50%"
    >
      <div class="announcement-detail-content">
        <p class="meta-info">发布时间：{{ formatDateTime(currentAnnouncement.createTime) }}</p>
        <div class="content-body">{{ currentAnnouncement.content }}</div>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useUserStore } from '@/store/user'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()
const userInfo = ref({})
const loading = ref(true)
const topAnnouncement = ref(null)
const otherAnnouncements = ref([])
const detailVisible = ref(false)
const currentAnnouncement = ref({})

const fetchUserInfo = async () => {
  try {
    const response = await request.get('/user/me')
    userInfo.value = response.data
    // 同步更新 store 中的用户信息
    userStore.setUserInfo(response.data)
  } catch (error) {
    console.error('获取用户信息失败:', error)
    ElMessage.error('获取用户信息失败')
  } finally {
    loading.value = false
  }
}

const fetchAnnouncements = async () => {
  try {
    const res = await request.get('/announcements', { params: { size: 20 } })
    if (res.code === 1) {
      const all = res.data.list
      topAnnouncement.value = all.find(a => a.isTop === 1) || null
      otherAnnouncements.value = all.filter(a => a.isTop !== 1)
    }
  } catch (error) {
    console.error('获取公告失败:', error)
  }
}

const showAnnouncementDetail = (item) => {
  currentAnnouncement.value = item
  detailVisible.value = true
}

const formatDateTime = (time) => {
  if (!time) return '-'
  return time.replace('T', ' ')
}

onMounted(() => {
  fetchUserInfo()
  fetchAnnouncements()
})
</script>

<style scoped>
.home-container {
  padding: 40px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.announcement-container {
  width: 700px;
  margin-bottom: 30px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0,0,0,0.1);
  overflow: hidden;
}

.announcement-top {
  border-bottom: 1px dashed #ebeef5;
  background-color: #fff9f9;
}

.announcement-carousel {
  background-color: #ffffff;
}

.announcement-item {
  height: 50px;
  display: flex;
  align-items: center;
  cursor: pointer;
  padding: 0 20px;
  transition: background 0.3s;
}

.top-item {
  height: 60px;
}

.announcement-item:hover {
  background: #f5f7fa;
}

.announcement-title {
  flex: 1;
  font-size: 16px;
  font-weight: bold;
  color: #303133;
  margin: 0 15px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.top-item .announcement-title {
  color: #f56c6c;
}

.announcement-time {
  font-size: 13px;
  color: #909399;
}

.announcement-detail-content {
  line-height: 1.8;
}

.meta-info {
  color: #909399;
  font-size: 13px;
  margin-bottom: 20px;
  border-bottom: 1px solid #ebeef5;
  padding-bottom: 10px;
}

.content-body {
  white-space: pre-wrap;
  font-size: 15px;
  color: #606266;
}

.user-card {
  width: 500px;
  margin-top: 20px;
}

.card-header {
  font-weight: bold;
}

.user-info p {
  text-align: left;
  margin: 10px 0;
  border-bottom: 1px solid #ebeef5;
  padding-bottom: 10px;
}
</style>
