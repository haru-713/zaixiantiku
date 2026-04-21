<template>
  <div class="home-container">
    <!-- 1. 欢迎卡片 (全宽) -->
    <el-card class="welcome-card" :body-style="{ padding: '24px 32px' }">
      <div class="welcome-content">
        <div class="welcome-left">
          <h2 class="welcome-title">{{ welcomeMessage }}，{{ userInfo.name || userInfo.username }}</h2>
          <div class="welcome-meta">
            <span class="date">{{ today }}</span>
            <span class="divider">|</span>
            <span class="quote">{{ motivationalQuote }}</span>
          </div>
        </div>
        <div class="welcome-right">
          <el-avatar :size="64" :src="userInfo.avatar">{{ userInfo.username?.charAt(0).toUpperCase() }}</el-avatar>
        </div>
      </div>
    </el-card>

    <!-- 2. 统计卡片 (一行四个) -->
    <el-row :gutter="24" class="stat-row">
      <el-col :xs="12" :sm="12" :md="6" v-for="item in roleStats" :key="item.label">
        <el-card class="stat-card" shadow="hover" @click="$router.push(item.path)">
          <div class="stat-main">
            <div class="stat-info">
              <div class="stat-value" :style="{ color: item.valueColor || '#1E88E5' }">
                {{ item.value }}
              </div>
              <div class="stat-label">{{ item.label }}</div>
            </div>
            <div class="stat-icon-wrapper" :style="{ backgroundColor: item.bgColor }">
              <el-icon :style="{ color: item.color }">
                <component :is="item.icon" />
              </el-icon>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 3. 快捷入口 (全宽卡片) -->
    <el-card class="quick-entry-card" :body-style="{ padding: '24px' }">
      <template #header>
        <div class="card-header">
          <span class="header-title">快捷入口</span>
        </div>
      </template>
      <el-row :gutter="20">
        <el-col :xs="12" :sm="8" :md="4" v-for="item in quickEntries" :key="item.label">
          <div class="quick-item" @click="$router.push(item.path)">
            <el-icon class="quick-icon">
              <component :is="item.icon" />
            </el-icon>
            <span class="quick-label">{{ item.label }}</span>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <!-- 4. 系统公告 (全宽卡片) -->
    <el-card class="announcement-card" shadow="hover" :body-style="{ padding: '0' }">
      <template #header>
        <div class="card-header-v4">
          <div class="header-left">
            <el-icon class="header-icon"><Notification /></el-icon>
            <span class="header-title">系统公告</span>
          </div>
          <el-button link type="primary" @click="handleViewMoreAnnouncements">
            更多 <el-icon><ArrowRight /></el-icon>
          </el-button>
        </div>
      </template>
      
      <div class="announcement-container-v4" v-if="announcementList.length > 0">
        <!-- 左侧焦点区域 (40%) -->
        <div class="focus-side" :class="{ 'full-width': announcementList.length === 1 }" @click="showAnnouncementDetail(focusAnnouncement)">
          <div class="focus-inner">
            <div class="focus-header">
              <div class="title-row">
                <el-tag v-if="focusAnnouncement.isTop == 1" type="warning" size="small" effect="dark" class="top-tag">置顶</el-tag>
                <h3 class="focus-title">{{ focusAnnouncement.title }}</h3>
              </div>
              <span class="focus-time">{{ formatDateTime(focusAnnouncement.createTime) }}</span>
            </div>
            <div class="focus-content">{{ focusAnnouncement.content }}</div>
          </div>
        </div>

        <!-- 分隔线 -->
        <div class="vertical-divider" v-if="announcementList.length > 1"></div>

        <!-- 右侧轮播区域 (60%) -->
        <div class="carousel-side" v-if="announcementList.length > 1">
          <el-carousel height="180px" arrow="hover" :interval="5000" pause-on-hover>
            <el-carousel-item v-for="item in carouselList" :key="item.id">
              <div class="carousel-item-v4" @click="showAnnouncementDetail(item)">
                <div class="item-header">
                  <h4 class="item-title">{{ item.title }}</h4>
                  <span class="item-time">{{ formatDateTime(item.createTime) }}</span>
                </div>
                <div class="item-content">{{ item.content }}</div>
              </div>
            </el-carousel-item>
          </el-carousel>
          <div class="carousel-footer">
            <span class="footer-count">共 {{ carouselList.length }} 条其他动态</span>
          </div>
        </div>

        <!-- 右侧为空时的占位符 (当总数为1时) -->
        <div class="empty-side" v-else-if="announcementList.length === 1">
          <div class="empty-placeholder">
            <p>暂无其他公告</p>
            <el-button v-if="isAdmin" type="primary" link size="small" icon="Plus" @click="handlePublish">发布公告</el-button>
          </div>
        </div>
      </div>

      <!-- 全局无公告状态 -->
      <div v-else class="no-announcement-v4">
        <el-empty description="暂无系统公告" :image-size="64">
          <el-button v-if="isAdmin" type="primary" plain size="small" icon="Plus" @click="handlePublish">发布新公告</el-button>
        </el-empty>
      </div>
    </el-card>

    <!-- 公告详情对话框 -->
    <el-dialog v-model="detailVisible" :title="currentAnnouncement.title" width="600px" center class="custom-dialog">
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
import { ref, onMounted, computed } from 'vue'
import { useUserStore } from '@/store/user'
import { useRouter } from 'vue-router'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'
import { ArrowRight, Calendar, Notification } from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()
const userInfo = computed(() => userStore.userInfo || {})
const roles = computed(() => userInfo.value.roles || [])
const isAdmin = computed(() => roles.value.includes('ADMIN'))
const isTeacher = computed(() => roles.value.includes('TEACHER'))
const isStudent = computed(() => roles.value.includes('STUDENT'))

const loading = ref(true)
const announcementList = ref([])
const detailVisible = ref(false)
const currentAnnouncement = ref({})

// 2.2 拆分规则
// 左侧焦点公告：取列表第 1 条
const focusAnnouncement = computed(() => {
  return announcementList.value.length > 0 ? announcementList.value[0] : null
})

// 右侧轮播列表：取列表第 2 条及以后
const carouselList = computed(() => {
  return announcementList.value.length >= 2 ? announcementList.value.slice(1) : []
})

const stats = ref({
  courseCount: 0,
  practiceCount: 0,
  examCount: 0,
  mistakeCount: 0,
  questionCount: 0,
  userCount: 0,
  pendingCount: 0,
  activeToday: 0,
  examSessions: 0
})

const welcomeMessage = computed(() => {
  const hour = new Date().getHours()
  if (hour < 6) return '凌晨好'
  if (hour < 9) return '早上好'
  if (hour < 12) return '上午好'
  if (hour < 14) return '中午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

const motivationalQuote = ref('祝您学习愉快！')
const quotes = [
  '书山有路勤为径，学海无涯苦作舟。',
  '学而不思则罔，思而不学则殆。',
  '知识就是力量。',
  '成功的秘诀在于坚持。',
  '志当存高远。'
]

const today = computed(() => {
  const date = new Date()
  const days = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']
  return `${date.getFullYear()}年${date.getMonth() + 1}月${date.getDate()}日 ${days[date.getDay()]}`
})

const roleStats = computed(() => {
  if (isAdmin.value) {
    return [
      { label: '用户总数', value: stats.value.userCount, icon: 'UserFilled', color: '#1E88E5', bgColor: '#E3F2FD', path: '/admin/users' },
      { label: '今日活跃', value: stats.value.activeToday, icon: 'TrendCharts', color: '#10B981', bgColor: '#D1FAE5', path: '/admin/logs', valueColor: '#10B981' },
      { label: '试题总量', value: stats.value.questionCount, icon: 'Memo', color: '#F97316', bgColor: '#FFEDD5', path: '/question/manage', valueColor: '#F97316' },
      { label: '考试场次', value: stats.value.examSessions, icon: 'Calendar', color: '#EF4444', bgColor: '#FEE2E2', path: '/exam/schedule', valueColor: '#EF4444' }
    ]
  }
  if (isTeacher.value) {
    return [
      { label: '管理课程', value: stats.value.courseCount, icon: 'Collection', color: '#1E88E5', bgColor: '#E3F2FD', path: '/course/list' },
      { label: '试题总量', value: stats.value.questionCount, icon: 'Memo', color: '#10B981', bgColor: '#D1FAE5', path: '/question/manage', valueColor: '#10B981' },
      { label: '待阅试卷', value: stats.value.pendingCount, icon: 'Checked', color: '#F97316', bgColor: '#FFEDD5', path: '/exam/marking', valueColor: '#F97316' },
      { label: '班级总数', value: 8, icon: 'School', color: '#3498db', bgColor: '#EBF5FB', path: '/teacher/analysis' }
    ]
  }
  return [
    { label: '参与课程', value: stats.value.courseCount, icon: 'Reading', color: '#1E88E5', bgColor: '#E3F2FD', path: '/course/query' },
    { label: '累计练习', value: stats.value.practiceCount, icon: 'EditPen', color: '#10B981', bgColor: '#D1FAE5', path: '/study/record', valueColor: '#10B981' },
    { label: '考试记录', value: stats.value.examCount, icon: 'Document', color: '#F97316', bgColor: '#FFEDD5', path: '/study/exam-record', valueColor: '#F97316' },
    { label: '错题数量', value: stats.value.mistakeCount, icon: 'Warning', color: '#EF4444', bgColor: '#FEE2E2', path: '/study/mistakes', valueColor: '#EF4444' }
  ]
})

const quickEntries = computed(() => {
  if (isAdmin.value) {
    return [
      { label: '用户管理', icon: 'User', path: '/admin/users' },
      { label: '试题管理', icon: 'Memo', path: '/question/manage' },
      { label: '试卷管理', icon: 'Files', path: '/exam/paper' },
      { label: '操作日志', icon: 'Operation', path: '/admin/logs' }
    ]
  }
  if (isTeacher.value) {
    return [
      { label: '课程管理', icon: 'Collection', path: '/course/list' },
      { label: '试题管理', icon: 'Memo', path: '/question/manage' },
      { label: '考试安排', icon: 'Calendar', path: '/exam/schedule' },
      { label: '阅卷管理', icon: 'Finished', path: '/exam/marking' }
    ]
  }
  return [
    { label: '课程查询', icon: 'Search', path: '/course/query' },
    { label: '在线练习', icon: 'Edit', path: '/study/practice' },
    { label: '我的考试', icon: 'List', path: '/study/exam' },
    { label: '错题本', icon: 'Notebook', path: '/study/mistakes' }
  ]
})

const fetchStats = async () => {
  try {
    if (isStudent.value) {
      const coursesRes = await request.get('/student/analysis/courses')
      stats.value.courseCount = coursesRes.data?.length || 0
      const mistakesRes = await request.get('/mistakes', { params: { size: 1 } })
      stats.value.mistakeCount = mistakesRes.data?.total || 0
      const recordsRes = await request.get('/study/records', { params: { size: 1 } })
      stats.value.practiceCount = recordsRes.data?.total || 0
      const examRecordsRes = await request.get('/student/exam-records', { params: { size: 1 } })
      stats.value.examCount = examRecordsRes.data?.total || 0
    }
    if (isTeacher.value) {
      const coursesRes = await request.get('/courses/managed')
      stats.value.courseCount = coursesRes.data?.length || 0
      const questionsRes = await request.get('/questions', { params: { size: 1 } })
      stats.value.questionCount = questionsRes.data?.total || 0
      const pendingRes = await request.get('/teacher/exam-records/pending', { params: { size: 1, status: 1 } })
      stats.value.pendingCount = pendingRes.data?.total || 0
    }
    if (isAdmin.value) {
      const dashboardRes = await request.get('/admin/analysis/dashboard')
      if (dashboardRes.code === 200 || dashboardRes.code === 1) {
        const data = dashboardRes.data
        stats.value.userCount = data.totalUsers || 0
        stats.value.activeToday = data.activeToday || 0
        stats.value.questionCount = data.totalQuestions || 0
        stats.value.examSessions = data.totalExams || 0
      } else {
        // 降级方案
        const usersRes = await request.get('/admin/users', { params: { size: 1 } })
        stats.value.userCount = usersRes.data?.total || 0
        const questionsRes = await request.get('/questions', { params: { size: 1 } })
        stats.value.questionCount = questionsRes.data?.total || 0
      }
    }
  } catch (e) {
    console.error('获取统计数据失败:', e)
  }
}

const fetchAnnouncements = async () => {
  try {
    const res = await request.get('/announcements', { params: { size: 10, publicView: true } })
    if (res.code === 1) {
      announcementList.value = res.data.list
    }
  } catch (error) {
    console.error('获取公告失败:', error)
  }
}

const handleViewMoreAnnouncements = () => {
  router.push('/announcements')
}

const handlePublish = () => {
  router.push('/system/announcements')
}

const showAnnouncementDetail = (item) => {
  currentAnnouncement.value = item
  detailVisible.value = true
}

const formatDateTime = (time) => {
  if (!time) return '-'
  if (typeof time !== 'string') {
    // 处理可能的数组格式 [yyyy, mm, dd, hh, mm, ss]
    if (Array.isArray(time)) {
      return `${time[0]}-${String(time[1]).padStart(2, '0')}-${String(time[2]).padStart(2, '0')}`
    }
    return '-'
  }
  return time.split('T')[0]
}

onMounted(() => {
  motivationalQuote.value = quotes[Math.floor(Math.random() * quotes.length)]
  fetchAnnouncements()
  fetchStats()
})
</script>

<style scoped>
.home-container {
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* 1. 欢迎卡片样式 */
.welcome-card {
  border: none;
  background: linear-gradient(135deg, #F0F7FF 0%, #FFFFFF 100%);
  border-radius: 16px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.03);
}

.welcome-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.welcome-title {
  margin: 0 0 12px 0;
  font-size: 24px;
  color: #1e293b;
  font-weight: 700;
}

.welcome-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #64748b;
  font-size: 14px;
}

.welcome-meta .divider {
  color: #e2e8f0;
}

/* 2. 统计卡片样式 */
.stat-row {
  margin: 0;
}

.stat-card {
  cursor: pointer;
  border: none;
  border-radius: 12px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  background: #ffffff;
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1);
}

.stat-main {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 4px;
}

.stat-value {
  font-size: 32px;
  font-weight: 700;
  line-height: 1;
  margin-bottom: 8px;
}

.stat-label {
  font-size: 14px;
  color: #94a3b8;
}

.stat-icon-wrapper {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-icon-wrapper .el-icon {
  font-size: 24px;
}

/* 3. 快捷入口样式 */
.quick-entry-card {
  border: none;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.02);
}

.quick-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 16px;
  cursor: pointer;
  border-radius: 12px;
  transition: all 0.2s;
  background: transparent;
}

.quick-item:hover {
  background-color: #f8fafc;
  transform: translateY(-2px);
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
}

.quick-icon {
  font-size: 24px;
  color: var(--el-color-primary);
  margin-bottom: 12px;
  background: var(--el-color-primary-light-9);
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.quick-label {
  font-size: 14px;
  color: #334155;
}

/* 4. 公告卡片样式 */
.announcement-card {
  border: none;
  border-radius: 12px;
}

.announcement-card :deep(.el-card__header) {
  padding: 16px 24px;
  border-bottom: 1px solid #f1f5f9;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-icon {
  font-size: 18px;
  color: var(--el-color-warning);
}

.header-title {
  font-weight: 700;
  color: #1e293b;
  font-size: 16px;
}

/* 4. 公告卡片新样式 - V4 (左右分栏) */
.card-header-v4 {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
}

.announcement-container-v4 {
  display: flex;
  min-height: 180px;
  background: #ffffff;
}

.focus-side {
  flex: 0 0 38%;
  padding: 24px;
  background: #F8FAFC;
  cursor: pointer;
  transition: background 0.2s;
  display: flex;
  flex-direction: column;
  min-width: 0; /* 防止内容撑开容器 */
}

.focus-side.full-width {
  flex: 0 0 100%;
}

.focus-side:hover {
  background: #F0F4F8;
}

.focus-inner {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.focus-header {
  margin-bottom: 12px;
}

.title-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.focus-title {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: #1e293b;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.focus-time {
  font-size: 12px;
  color: #94a3b8;
}

.focus-content {
  font-size: 14px;
  line-height: 1.6;
  color: #64748b;
  display: -webkit-box;
  -webkit-line-clamp: 4; /* 增加展示行数 */
  -webkit-box-orient: vertical;
  overflow: hidden;
  white-space: pre-wrap;
}

.vertical-divider {
  width: 1px;
  background-color: #f1f5f9;
  flex-shrink: 0; /* 防止分隔线被压缩 */
}

.carousel-side {
  flex: 1; /* 使用 flex: 1 自动填充剩余空间，解决 1px 溢出问题 */
  padding: 0;
  position: relative;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.carousel-item-v4 {
  height: 100%;
  padding: 24px;
  cursor: pointer;
  display: flex;
  flex-direction: column;
}

.carousel-item-v4:hover .item-title {
  color: var(--el-color-primary);
}

.item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.item-title {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: #1e293b;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}

.item-time {
  font-size: 12px;
  color: #94a3b8;
  margin-left: 16px;
}

.item-content {
  font-size: 13px;
  line-height: 1.6;
  color: #64748b;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.carousel-footer {
  position: absolute;
  bottom: 16px;
  left: 24px;
  font-size: 12px;
  color: #cbd5e1;
  pointer-events: none;
}

.empty-side {
  flex: 0 0 60%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #ffffff;
}

.empty-placeholder {
  text-align: center;
  color: #94a3b8;
}

.empty-placeholder p {
  margin-bottom: 8px;
  font-size: 14px;
}

.no-announcement-v4 {
  padding: 40px 0;
}

/* 公告详情弹窗 */
.announcement-detail .meta {
  display: flex;
  align-items: center;
  color: #94a3b8;
  font-size: 13px;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid #f1f5f9;
}

.announcement-detail .content {
  font-size: 15px;
  line-height: 1.8;
  color: #334155;
  white-space: pre-wrap;
}

@media (max-width: 768px) {
  .home-container {
    gap: 16px;
  }
  
  .welcome-card {
    padding: 16px 20px;
  }
  
  .welcome-meta .divider,
  .welcome-meta .quote {
    display: none;
  }

  .stat-value {
    font-size: 24px;
  }
  
  .announcement-container-v4 {
    flex-direction: column;
  }
  
  .focus-side, .carousel-side, .empty-side {
    flex: 0 0 auto;
  }
  
  .vertical-divider {
    display: none;
  }
}
</style>