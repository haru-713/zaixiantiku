<template>
  <div class="home-container">
    <div class="welcome-card">
      <div class="welcome-text">
        <h2>欢迎回来，{{ userInfo.name || userInfo.username }}</h2>
        <p class="subtitle">今天是 {{ today }}, 祝您学习愉快！</p>
      </div>
      <el-button type="primary" plain @click="$router.push('/profile')">查看个人档案</el-button>
    </div>

    <!-- 统计数据展示 -->
    <el-row :gutter="20" class="stat-row">
      <!-- 学生统计项 -->
      <template v-if="isStudent">
        <el-col :xs="24" :sm="12" :md="6">
          <el-card class="stat-card is-link" shadow="hover" @click="$router.push('/course/query')">
            <div class="stat-content">
              <el-icon class="stat-icon color-blue"><Reading /></el-icon>
              <div class="stat-info">
                <div class="stat-label">参与课程</div>
                <div class="stat-value">{{ stats.courseCount || 0 }}</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="12" :md="6">
          <el-card class="stat-card is-link" shadow="hover" @click="$router.push('/study/record')">
            <div class="stat-content">
              <el-icon class="stat-icon color-green"><EditPen /></el-icon>
              <div class="stat-info">
                <div class="stat-label">累计练习</div>
                <div class="stat-value">{{ stats.practiceCount || 0 }}</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="12" :md="6">
          <el-card class="stat-card is-link" shadow="hover" @click="$router.push('/study/exam-record')">
            <div class="stat-content">
              <el-icon class="stat-icon color-orange"><Document /></el-icon>
              <div class="stat-info">
                <div class="stat-label">考试记录</div>
                <div class="stat-value">{{ stats.examCount || 0 }}</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="12" :md="6">
          <el-card class="stat-card is-link" shadow="hover" @click="$router.push('/study/mistakes')">
            <div class="stat-content">
              <el-icon class="stat-icon color-red"><Warning /></el-icon>
              <div class="stat-info">
                <div class="stat-label">错题统计</div>
                <div class="stat-value">{{ stats.mistakeCount || 0 }}</div>
              </div>
            </div>
          </el-card>
        </el-col>
      </template>

      <!-- 教师统计项 -->
      <template v-else-if="isTeacher">
        <el-col :xs="24" :sm="12" :md="6">
          <el-card class="stat-card is-link" shadow="hover" @click="$router.push('/course/list')">
            <div class="stat-content">
              <el-icon class="stat-icon color-blue"><Management /></el-icon>
              <div class="stat-info">
                <div class="stat-label">管理课程</div>
                <div class="stat-value">{{ stats.courseCount || 0 }}</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="12" :md="6">
          <el-card class="stat-card is-link" shadow="hover" @click="$router.push('/question/manage')">
            <div class="stat-content">
              <el-icon class="stat-icon color-green"><QuestionFilled /></el-icon>
              <div class="stat-info">
                <div class="stat-label">试题总量</div>
                <div class="stat-value">{{ stats.questionCount || 0 }}</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="12" :md="6">
          <el-card class="stat-card is-link" shadow="hover" @click="$router.push('/system/classes')">
            <div class="stat-content">
              <el-icon class="stat-icon color-orange"><UserFilled /></el-icon>
              <div class="stat-info">
                <div class="stat-label">班级总数</div>
                <div class="stat-value">{{ stats.classCount || 0 }}</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="12" :md="6">
          <el-card class="stat-card is-link" shadow="hover" @click="$router.push('/exam/marking')">
            <div class="stat-content">
              <el-icon class="stat-icon color-red"><Checked /></el-icon>
              <div class="stat-info">
                <div class="stat-label">待阅试卷</div>
                <div class="stat-value">{{ stats.pendingCount || 0 }}</div>
              </div>
            </div>
          </el-card>
        </el-col>
      </template>

      <!-- 管理员统计项 -->
      <template v-else-if="isAdmin">
        <el-col :xs="24" :sm="12" :md="6">
          <el-card class="stat-card is-link" shadow="hover" @click="$router.push('/course/list')">
            <div class="stat-content">
              <el-icon class="stat-icon color-blue"><Management /></el-icon>
              <div class="stat-info">
                <div class="stat-label">管理课程</div>
                <div class="stat-value">{{ stats.courseCount || 0 }}</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="12" :md="6">
          <el-card class="stat-card is-link" shadow="hover" @click="$router.push('/question/manage')">
            <div class="stat-content">
              <el-icon class="stat-icon color-green"><QuestionFilled /></el-icon>
              <div class="stat-info">
                <div class="stat-label">试题总量</div>
                <div class="stat-value">{{ stats.questionCount || 0 }}</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="12" :md="6">
          <el-card class="stat-card is-link" shadow="hover" @click="$router.push('/admin/users')">
            <div class="stat-content">
              <el-icon class="stat-icon color-orange"><UserFilled /></el-icon>
              <div class="stat-info">
                <div class="stat-label">用户总数</div>
                <div class="stat-value">{{ stats.userCount || 0 }}</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="12" :md="6">
          <el-card class="stat-card is-link" shadow="hover" @click="$router.push('/admin/logs')">
            <div class="stat-content">
              <el-icon class="stat-icon color-red"><Operation /></el-icon>
              <div class="stat-info">
                <div class="stat-label">操作日志</div>
                <div class="stat-value">查看</div>
              </div>
            </div>
          </el-card>
        </el-col>
      </template>
    </el-row>

    <el-row :gutter="20" class="main-row">
      <!-- 左侧公告 -->
      <el-col :xs="24" :md="16">
        <el-card class="announcement-card">
          <template #header>
            <div class="card-header">
              <div class="header-left">
                <el-icon><Bell /></el-icon>
                <span>系统公告</span>
              </div>
              <el-button link type="primary" v-if="isAdmin" @click="$router.push('/system/announcements')">管理公告</el-button>
            </div>
          </template>
          
          <div class="announcement-list" v-if="topAnnouncement || otherAnnouncements.length > 0">
            <div v-if="topAnnouncement" class="announcement-item is-top" @click="showAnnouncementDetail(topAnnouncement)">
              <el-tag type="danger" size="small" effect="dark">置顶</el-tag>
              <span class="title">{{ topAnnouncement.title }}</span>
              <span class="time">{{ formatDateTime(topAnnouncement.createTime) }}</span>
            </div>
            <div v-for="item in otherAnnouncements.slice(0, 5)" :key="item.id" class="announcement-item" @click="showAnnouncementDetail(item)">
              <span class="dot"></span>
              <span class="title">{{ item.title }}</span>
              <span class="time">{{ formatDateTime(item.createTime) }}</span>
            </div>
          </div>
          <el-empty v-else description="暂无公告" :image-size="100" />
        </el-card>
      </el-col>

      <!-- 右侧快捷入口 -->
      <el-col :xs="24" :md="8">
        <el-card class="quick-access-card">
          <template #header>
            <div class="card-header">
              <div class="header-left">
                <el-icon><Compass /></el-icon>
                <span>快捷入口</span>
              </div>
            </div>
          </template>
          
          <!-- 学生快捷入口 -->
          <div class="quick-links" v-if="isStudent">
            <div class="quick-item" @click="$router.push('/course/query')">
              <el-icon class="bg-blue"><Search /></el-icon>
              <span>课程查询</span>
            </div>
            <div class="quick-item" @click="$router.push('/study/practice')">
              <el-icon class="bg-green"><Edit /></el-icon>
              <span>在线练习</span>
            </div>
            <div class="quick-item" @click="$router.push('/study/exam')">
              <el-icon class="bg-orange"><List /></el-icon>
              <span>我的考试</span>
            </div>
            <div class="quick-item" @click="$router.push('/study/mistakes')">
              <el-icon class="bg-red"><Notebook /></el-icon>
              <span>错题本</span>
            </div>
          </div>

          <!-- 管理员快捷入口 -->
          <div class="quick-links" v-else-if="isAdmin">
            <div class="quick-item" @click="$router.push('/admin/users')">
              <el-icon class="bg-blue"><User /></el-icon>
              <span>用户管理</span>
            </div>
            <div class="quick-item" @click="$router.push('/question/manage')">
              <el-icon class="bg-green"><Memo /></el-icon>
              <span>试题管理</span>
            </div>
            <div class="quick-item" @click="$router.push('/exam/paper')">
              <el-icon class="bg-orange"><Files /></el-icon>
              <span>试卷管理</span>
            </div>
            <div class="quick-item" @click="$router.push('/admin/logs')">
              <el-icon class="bg-red"><Operation /></el-icon>
              <span>操作日志</span>
            </div>
          </div>

          <!-- 教师快捷入口 -->
          <div class="quick-links" v-else-if="isTeacher">
            <div class="quick-item" @click="$router.push('/course/list')">
              <el-icon class="bg-blue"><Collection /></el-icon>
              <span>课程管理</span>
            </div>
            <div class="quick-item" @click="$router.push('/exam/marking')">
              <el-icon class="bg-green"><Finished /></el-icon>
              <span>阅卷管理</span>
            </div>
            <div class="quick-item" @click="$router.push('/exam/schedule')">
              <el-icon class="bg-orange"><Calendar /></el-icon>
              <span>考试安排</span>
            </div>
            <div class="quick-item" @click="$router.push('/teacher/analysis')">
              <el-icon class="bg-red"><DataAnalysis /></el-icon>
              <span>成绩分析</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

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
import { ref, onMounted, computed } from 'vue'
import { useUserStore } from '@/store/user'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()
const userInfo = computed(() => userStore.userInfo || {})
const roles = computed(() => userInfo.value.roles || [])
const isAdmin = computed(() => roles.value.includes('ADMIN'))
const isTeacher = computed(() => roles.value.includes('TEACHER'))
const isStudent = computed(() => roles.value.includes('STUDENT'))

const loading = ref(true)
const topAnnouncement = ref(null)
const otherAnnouncements = ref([])
const detailVisible = ref(false)
const currentAnnouncement = ref({})

const stats = ref({
  courseCount: 0,
  practiceCount: 0,
  examCount: 0,
  mistakeCount: 0,
  questionCount: 0,
  userCount: 0,
  pendingCount: 0,
  classCount: 0
})

const today = computed(() => {
  const date = new Date()
  return `${date.getFullYear()}年${date.getMonth() + 1}月${date.getDate()}日`
})

const fetchStats = async () => {
  try {
    // 1. 学生统计
    if (isStudent.value) {
      // 课程数量
      const coursesRes = await request.get('/student/analysis/courses')
      stats.value.courseCount = coursesRes.data?.length || 0
      
      const mistakesRes = await request.get('/mistakes', { params: { size: 1 } })
      stats.value.mistakeCount = mistakesRes.data?.total || 0
      
      const recordsRes = await request.get('/study/records', { params: { size: 1 } })
      stats.value.practiceCount = recordsRes.data?.total || 0

      const examRecordsRes = await request.get('/student/exam-records', { params: { size: 1 } })
      stats.value.examCount = examRecordsRes.data?.total || 0
    }
    
    // 2. 教师统计
    if (isTeacher.value) {
      // 教师管理课程
      const coursesRes = await request.get('/courses/managed')
      stats.value.courseCount = coursesRes.data?.length || 0

      // 试题总量 (分页接口获取总数)
      const questionsRes = await request.get('/questions', { params: { size: 1 } })
      stats.value.questionCount = questionsRes.data?.total || 0

      // 待阅卷数量
      const pendingRes = await request.get('/teacher/exam-records/pending', { params: { size: 1, status: 1 } })
      stats.value.pendingCount = pendingRes.data?.total || 0

      // 班级总数
      const classesRes = await request.get('/teacher/analysis/classes')
      stats.value.classCount = classesRes.data?.length || 0
    }

    // 3. 管理员统计
    if (isAdmin.value) {
      const coursesRes = await request.get('/courses/managed')
      stats.value.courseCount = coursesRes.data?.length || 0

      const usersRes = await request.get('/admin/users', { params: { size: 1 } })
      stats.value.userCount = usersRes.data?.total || 0

      const questionsRes = await request.get('/questions', { params: { size: 1 } })
      stats.value.questionCount = questionsRes.data?.total || 0
    }
  } catch (e) {
    console.error('获取统计数据失败:', e)
  }
}

const fetchUserInfo = async () => {
  try {
    const response = await request.get('/user/me')
    userStore.setUserInfo(response.data)
  } catch (error) {
    console.error('获取用户信息失败:', error)
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

onMounted(async () => {
  await fetchUserInfo()
  fetchAnnouncements()
  fetchStats()
})
</script>

<style scoped>
.home-container {
  width: 100%;
  padding-bottom: 24px;
  animation: fadeIn 0.5s ease-in-out;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.stat-card.is-link {
  cursor: pointer;
  transition: all 0.3s ease;
}

.stat-card.is-link:hover {
  transform: translateY(-5px);
  border-color: #409eff;
  box-shadow: 0 8px 16px rgba(64, 158, 255, 0.2) !important;
}

.welcome-card {
  background: linear-gradient(135deg, #2c3e50 0%, #34495e 100%);
  padding: 32px 40px;
  border-radius: 12px;
  color: #fff;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}

.welcome-text h2 {
  margin: 0 0 8px 0;
  font-size: 26px;
  letter-spacing: 1px;
}

.welcome-text .subtitle {
  margin: 0;
  opacity: 0.85;
  font-size: 15px;
}

.stat-row {
  margin-bottom: 8px; /* 补偿 el-col 的 bottom margin */
}

.stat-row .el-col {
  margin-bottom: 24px;
}

.stat-card {
  border-radius: 12px;
  height: 100%;
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 20px;
}

.stat-icon {
  font-size: 30px;
  padding: 14px;
  border-radius: 12px;
}

.color-blue { background: #eef2ff; color: #4f46e5; }
.color-green { background: #ecfdf5; color: #10b981; }
.color-orange { background: #fff7ed; color: #f59e0b; }
.color-red { background: #fef2f2; color: #ef4444; }

.stat-info {
  flex: 1;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-bottom: 6px;
}

.stat-value {
  font-size: 26px;
  font-weight: 700;
  color: #303133;
}

.main-row {
  margin-top: 0;
}

.announcement-card, .quick-access-card {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.announcement-card :deep(.el-card__body), 
.quick-access-card :deep(.el-card__body) {
  flex: 1;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
  font-weight: 600;
  font-size: 16px;
  color: #303133;
}

.announcement-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.announcement-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.announcement-item:hover {
  background-color: #f8f9fb;
  transform: translateX(4px);
}

.announcement-item.is-top {
  background-color: #fff5f5;
}

.announcement-item.is-top:hover {
  background-color: #ffebeb;
}

.announcement-item .dot {
  width: 6px;
  height: 6px;
  background-color: #409eff;
  border-radius: 50%;
  margin-right: 14px;
}

.announcement-item .title {
  flex: 1;
  font-size: 14.5px;
  color: #3c4043;
  margin-right: 15px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.announcement-item .time {
  font-size: 13px;
  color: #9aa0a6;
}

.quick-links {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
  padding: 4px;
}

.quick-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14px;
  padding: 24px 10px;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border: 1px solid #f0f0f0;
  background-color: #fff;
}

.quick-item:hover {
  border-color: #409eff;
  background-color: #f0f7ff;
  transform: translateY(-4px);
  box-shadow: 0 6px 16px rgba(64, 158, 255, 0.15);
}

.quick-item .el-icon {
  font-size: 26px;
  padding: 14px;
  border-radius: 14px;
  color: #fff;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.1);
}

.bg-blue { background-color: #409eff; }
.bg-green { background-color: #67c23a; }
.bg-orange { background-color: #e6a23c; }
.bg-red { background-color: #f56c6c; }

.quick-item span {
  font-size: 14px;
  color: #444746;
  font-weight: 500;
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

/* 移动端适配 */
@media (max-width: 768px) {
  .welcome-card {
    flex-direction: column;
    text-align: center;
    gap: 20px;
    padding: 24px;
  }
  
  .quick-links {
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
  }
}
</style>
