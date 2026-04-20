<template>
  <div class="home-container">
    <!-- 欢迎卡片 -->
    <el-card class="welcome-card" :body-style="{ padding: '24px' }">
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

    <!-- 统计数据展示 -->
    <el-row :gutter="20" class="stat-row">
      <!-- 根据角色展示统计项 -->
      <el-col :xs="24" :sm="12" :md="6" v-for="item in roleStats" :key="item.label">
        <el-card class="stat-card" shadow="hover" @click="$router.push(item.path)">
          <div class="stat-main">
            <div class="stat-info">
              <div class="stat-value" :style="{ color: item.valueColor || 'var(--el-color-primary)' }">
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

    <!-- 快捷入口 -->
    <el-card class="quick-entry-card">
      <template #header>
        <div class="card-header">
          <span class="header-title">快捷入口</span>
        </div>
      </template>
      <el-row :gutter="20">
        <el-col :xs="12" :sm="8" :md="6" v-for="item in quickEntries" :key="item.label">
          <div class="quick-item" @click="$router.push(item.path)">
            <el-icon class="quick-icon">
              <component :is="item.icon" />
            </el-icon>
            <span class="quick-label">{{ item.label }}</span>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <!-- 系统公告 -->
    <el-card class="announcement-card" :body-style="{ padding: '16px 20px' }">
      <div class="announcement-content">
        <div class="announcement-left">
          <el-tag type="warning" size="small" effect="plain" class="announcement-tag">最新公告</el-tag>
          <template v-if="latestAnnouncement">
            <span class="announcement-title" @click="showAnnouncementDetail(latestAnnouncement)">
              {{ latestAnnouncement.title }}
            </span>
            <span class="announcement-time">{{ formatDateTime(latestAnnouncement.createTime) }}</span>
          </template>
          <span v-else class="no-announcement">暂无系统公告</span>
        </div>
        <el-button link type="primary" @click="handleViewMoreAnnouncements">
          查看更多 <el-icon class="el-icon--right">
            <ArrowRight />
          </el-icon>
        </el-button>
      </div>
    </el-card>

    <!-- 公告详情对话框 -->
    <el-dialog v-model="detailVisible" :title="currentAnnouncement.title" width="500px" center class="custom-dialog">
      <div class="announcement-detail">
        <div class="meta">发布时间：{{ formatDateTime(currentAnnouncement.createTime) }}</div>
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
const latestAnnouncement = computed(() => announcementList.value[0] || null)

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
    const res = await request.get('/announcements', { params: { size: 5 } })
    if (res.code === 1) {
      announcementList.value = res.data.list
    }
  } catch (error) {
    console.error('获取公告失败:', error)
  }
}

const handleViewMoreAnnouncements = () => {
  if (isAdmin.value) router.push('/system/announcements')
  else ElMessage.info('更多公告功能开发中...')
}

const showAnnouncementDetail = (item) => {
  currentAnnouncement.value = item
  detailVisible.value = true
}

const formatDateTime = (time) => {
  if (!time) return '-'
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
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.welcome-card {
  border: none;
}

.welcome-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.welcome-title {
  margin: 0 0 8px 0;
  font-size: 20px;
  color: var(--text-main);
}

.welcome-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  color: var(--text-secondary);
  font-size: 14px;
}

.welcome-meta .divider {
  color: var(--border-color);
}

.stat-row {
  margin: 0 -10px;
}

.stat-card {
  cursor: pointer;
  border: 1px solid var(--border-color);
}

.stat-main {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.stat-value {
  font-size: 32px;
  font-weight: bold;
  line-height: 1;
  margin-bottom: 8px;
}

.stat-label {
  font-size: 14px;
  color: var(--text-secondary);
}

.stat-icon-wrapper {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: 24px;
}

.quick-entry-card {
  border: 1px solid var(--border-color);
}

.header-title {
  font-size: 16px;
  font-weight: 600;
}

.quick-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 20px;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s;
}

.quick-item:hover {
  background-color: var(--bg-color);
  transform: translateY(-2px);
}

.quick-icon {
  font-size: 28px;
  color: var(--el-color-primary);
}

.quick-label {
  font-size: 14px;
  color: var(--text-regular);
}

.announcement-card {
  border: 1px solid var(--border-color);
}

.announcement-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.announcement-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  overflow: hidden;
}

.announcement-title {
  font-size: 14px;
  color: var(--text-main);
  cursor: pointer;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.announcement-title:hover {
  color: var(--el-color-primary);
}

.announcement-time {
  font-size: 13px;
  color: var(--text-secondary);
  flex-shrink: 0;
}

.no-announcement {
  color: var(--text-secondary);
  font-size: 14px;
}

.announcement-detail .meta {
  text-align: center;
  color: var(--text-secondary);
  font-size: 13px;
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--border-color);
}

.announcement-detail .content {
  font-size: 15px;
  line-height: 1.8;
  white-space: pre-wrap;
}

@media (max-width: 768px) {

  .welcome-meta .divider,
  .welcome-meta .quote {
    display: none;
  }

  .stat-card {
    margin-bottom: 16px;
  }
}
</style>