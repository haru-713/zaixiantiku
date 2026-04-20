<template>
  <div class="app-root">
    <router-view v-if="isAuthPage" />
    <el-container v-else class="layout">
      <!-- 侧边栏：品牌标识 + 导航 -->
      <el-aside :width="isCollapse ? '64px' : '240px'" class="layout-aside">
        <div class="logo-area">
          <el-icon size="24" class="logo-icon">
            <Notebook />
          </el-icon>
          <span v-if="!isCollapse" class="logo-text">在线题库系统</span>
        </div>
        <el-menu :default-active="activeMenu" router class="layout-menu" :collapse="isCollapse"
          :collapse-transition="false">
          <el-menu-item index="/home">
            <el-icon>
              <HomeFilled />
            </el-icon>
            <template #title>首页</template>
          </el-menu-item>

          <!-- 题库资源分组 -->
          <el-sub-menu v-if="isAdmin || isTeacher" index="qbank">
            <template #title>
              <el-icon>
                <Reading />
              </el-icon>
              <span>题库资源</span>
            </template>
            <el-menu-item index="/question/manage">
              <el-icon>
                <Document />
              </el-icon>
              <template #title>试题管理</template>
            </el-menu-item>
            <el-menu-item index="/question/category">
              <el-icon>
                <CollectionTag />
              </el-icon>
              <template #title>知识点管理</template>
            </el-menu-item>
            <el-menu-item index="/exam/paper">
              <el-icon>
                <Notebook />
              </el-icon>
              <template #title>试卷管理</template>
            </el-menu-item>
          </el-sub-menu>

          <!-- 教学管理分组 -->
          <el-sub-menu v-if="isAdmin || isTeacher" index="teaching">
            <template #title>
              <el-icon>
                <Management />
              </el-icon>
              <span>教学管理</span>
            </template>
            <el-menu-item index="/course/list">
              <el-icon>
                <School />
              </el-icon>
              <template #title>课程管理</template>
            </el-menu-item>
            <el-menu-item index="/exam/schedule">
              <el-icon>
                <Calendar />
              </el-icon>
              <template #title>考试安排</template>
            </el-menu-item>
            <el-menu-item index="/exam/marking">
              <el-icon>
                <EditPen />
              </el-icon>
              <template #title>阅卷管理</template>
            </el-menu-item>
          </el-sub-menu>

          <!-- 成绩分析分组 -->
          <el-sub-menu v-if="isAdmin || isTeacher" index="analysis">
            <template #title>
              <el-icon>
                <DataLine />
              </el-icon>
              <span>成绩分析</span>
            </template>
            <el-menu-item index="/teacher/analysis">
              <el-icon>
                <Box />
              </el-icon>
              <template #title>班级分析</template>
            </el-menu-item>
            <el-menu-item index="/exam/statistics">
              <el-icon>
                <TrendCharts />
              </el-icon>
              <template #title>成绩统计</template>
            </el-menu-item>
          </el-sub-menu>

          <!-- 学习中心分组 (学生) -->
          <el-sub-menu v-if="isStudent" index="study">
            <template #title>
              <el-icon>
                <Monitor />
              </el-icon>
              <span>学习中心</span>
            </template>
            <el-menu-item index="/course/query">
              <el-icon>
                <Search />
              </el-icon>
              <template #title>课程查询</template>
            </el-menu-item>
            <el-menu-item index="/study/practice">
              <el-icon>
                <Edit />
              </el-icon>
              <template #title>在线练习</template>
            </el-menu-item>
            <el-menu-item index="/study/exam">
              <el-icon>
                <List />
              </el-icon>
              <template #title>我的考试</template>
            </el-menu-item>
            <el-menu-item index="/shares">
              <el-icon>
                <ChatDotRound />
              </el-icon>
              <template #title>交流分享</template>
            </el-menu-item>
          </el-sub-menu>

          <!-- 个人题库分组 (学生) -->
          <el-sub-menu v-if="isStudent" index="personal">
            <template #title>
              <el-icon>
                <Collection />
              </el-icon>
              <span>个人题库</span>
            </template>
            <el-menu-item index="/study/mistakes">
              <el-icon>
                <Notebook />
              </el-icon>
              <template #title>错题本</template>
            </el-menu-item>
            <el-menu-item index="/study/favorites">
              <el-icon>
                <Star />
              </el-icon>
              <template #title>收藏夹</template>
            </el-menu-item>
          </el-sub-menu>

          <!-- 数据统计分组 (学生) -->
          <el-sub-menu v-if="isStudent" index="records">
            <template #title>
              <el-icon>
                <PieChart />
              </el-icon>
              <span>数据统计</span>
            </template>
            <el-menu-item index="/study/analysis">
              <el-icon>
                <DataAnalysis />
              </el-icon>
              <template #title>学习报告</template>
            </el-menu-item>
            <el-menu-item index="/study/record">
              <el-icon>
                <Clock />
              </el-icon>
              <template #title>练习记录</template>
            </el-menu-item>
            <el-menu-item index="/study/exam-record">
              <el-icon>
                <Files />
              </el-icon>
              <template #title>考试记录</template>
            </el-menu-item>
          </el-sub-menu>

          <!-- 系统管理分组 -->
          <el-sub-menu v-if="isAdmin || isTeacher" index="system">
            <template #title>
              <el-icon>
                <Setting />
              </el-icon>
              <span>系统管理</span>
            </template>
            <el-menu-item index="/system/classes">
              <el-icon>
                <School />
              </el-icon>
              <template #title>班级管理</template>
            </el-menu-item>
            <el-menu-item index="/admin/users">
              <el-icon>
                <User />
              </el-icon>
              <template #title>用户管理</template>
            </el-menu-item>
            <el-menu-item index="/admin/logs" v-if="isAdmin">
              <el-icon>
                <Operation />
              </el-icon>
              <template #title>操作日志</template>
            </el-menu-item>
          </el-sub-menu>
        </el-menu>
      </el-aside>

      <el-container class="layout-right">
        <!-- 顶部栏：功能 + 导航 -->
        <el-header class="layout-header">
          <div class="header-left">
            <el-icon class="collapse-btn" @click="isCollapse = !isCollapse">
              <Expand v-if="isCollapse" />
              <Fold v-else />
            </el-icon>
          </div>
          <div class="header-right">
            <el-dropdown trigger="click" @command="handleUserCommand">
              <div class="user-trigger">
                <el-avatar :size="32" icon="UserFilled" />
                <span class="username">{{ currentUserName }}</span>
                <el-icon>
                  <ArrowDown />
                </el-icon>
              </div>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="profile">
                    <el-icon>
                      <User />
                    </el-icon>个人中心
                  </el-dropdown-item>
                  <el-dropdown-item divided command="logout">
                    <el-icon>
                      <SwitchButton />
                    </el-icon>退出登录
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </el-header>

        <el-main class="layout-main">
          <div class="breadcrumb">
            <div class="breadcrumb-left">
              <el-button v-if="route.path !== '/home'" link @click="router.back()" class="back-btn">
                <el-icon>
                  <Back />
                </el-icon>
              </el-button>
              <el-breadcrumb separator="/">
                <el-breadcrumb-item :to="{ path: '/home' }">首页</el-breadcrumb-item>
                <el-breadcrumb-item v-if="route.path !== '/home'">
                  {{ route.meta.title || '页面' }}
                </el-breadcrumb-item>
              </el-breadcrumb>
            </div>
          </div>
          <div class="main-content">
            <router-view />
          </div>
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const isCollapse = ref(false)
const isMobile = ref(false)

const isAuthPage = computed(() => route.path === '/login' || route.path === '/register' || route.name === 'ExamRoom')
const roles = computed(() => userStore.userInfo?.roles || [])
const isAdmin = computed(() => roles.value.includes('ADMIN'))
const isTeacher = computed(() => roles.value.includes('TEACHER'))
const isStudent = computed(() => roles.value.includes('STUDENT'))

const currentUserName = computed(() => userStore.userInfo?.name || userStore.userInfo?.username || '')
const activeMenu = computed(() => route.meta.activeMenu || route.path)

const refreshUserInfo = async () => {
  try {
    const res = await request.get('/user/me')
    userStore.setUserInfo(res.data)
  } catch (e) {
    console.error('获取用户信息失败:', e)
  }
}

const handleLogout = () => {
  userStore.logout()
  router.push('/login')
}

const handleUserCommand = (command) => {
  if (command === 'profile') {
    router.push('/profile')
    return
  }
  if (command === 'logout') {
    handleLogout()
  }
}

// 监听窗口大小变化
const handleResize = () => {
  isMobile.value = window.innerWidth < 768
  if (isMobile.value) {
    isCollapse.value = true
  } else {
    isCollapse.value = false
  }
}

watch(() => route.path, () => {
  if (isMobile.value) {
    isCollapse.value = true
  }
})

onMounted(() => {
  handleResize()
  window.addEventListener('resize', handleResize)
  if (userStore.token) {
    refreshUserInfo()
  }
})
</script>

<style>
/* 科技蓝侧边栏风格 */
:root {
  --sidebar-bg: #1E293B;
  --sidebar-logo-bg: #0F172A;
  --sidebar-text: #E2E8F0;
  --sidebar-active-text: #FFFFFF;
  --sidebar-active-bg: #334155;
  --sidebar-hover-bg: #334155;
}

.layout {
  height: 100vh;
}

.layout-right {
  display: flex;
  flex-direction: column;
  height: 100vh;
  overflow: hidden;
}

.layout-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #ffffff;
  color: var(--text-main);
  border-bottom: 1px solid var(--border-color);
  z-index: 10;
  /* 降低 z-index，因为侧边栏现在在同级 */
  padding: 0 20px;
  height: 60px;
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.collapse-btn {
  font-size: 20px;
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  transition: all 0.3s;
  color: var(--text-secondary);
}

.collapse-btn:hover {
  background-color: #F1F5F9;
  color: var(--el-color-primary);
}

.logo-area {
  height: 60px;
  display: flex;
  align-items: center;
  padding: 0 16px;
  background-color: var(--sidebar-logo-bg);
  color: #FFFFFF;
  overflow: hidden;
  white-space: nowrap;
  transition: all 0.3s;
}

.logo-icon {
  flex-shrink: 0;
  margin-right: 12px;
  color: var(--el-color-primary-light-3);
}

.logo-text {
  font-size: 20px;
  font-weight: 600;
  letter-spacing: 1px;
  opacity: 1;
  transition: opacity 0.3s;
}

.header-right {
  display: flex;
  align-items: center;
}

.user-trigger {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--text-regular);
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
  transition: background-color 0.3s;
}

.user-trigger:hover {
  background-color: #F1F5F9;
}

.layout-aside {
  background: var(--sidebar-bg);
  transition: width 0.3s;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.layout-menu {
  border-right: none;
  background-color: transparent !important;
  flex: 1;
}

.layout-menu:not(.el-menu--collapse) {
  width: 240px;
}

.layout-menu.el-menu {
  border-right: none;
}

/* 侧边栏字号与颜色优化 */
.layout-menu .el-menu-item,
.layout-menu .el-sub-menu__title {
  color: var(--sidebar-text) !important;
  font-size: 15px !important;
  height: 48px !important;
  line-height: 48px !important;
  margin: 4px 12px !important;
  width: calc(100% - 24px) !important;
  border-radius: 8px !important;
  padding: 0 16px !important;
  transition: all 0.2s ease !important;
}

/* 针对子菜单内的项单独设置宽度和边距，防止溢出 */
.layout-menu .el-sub-menu .el-menu-item {
  width: calc(100% - 24px) !important;
  margin: 2px 12px !important;
  padding-left: 48px !important;
  /* 缩进子菜单内容 */
}

/* 分组标题样式 (如果仍有需要展示的静态标题) */
.menu-group-title {
  padding: 16px 24px 8px;
  font-size: 13px;
  font-weight: 700;
  color: #94A3B8;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

/* 修复 el-sub-menu 展开后的背景和箭头颜色 */
:deep(.el-sub-menu.is-opened > .el-sub-menu__title) {
  color: var(--sidebar-active-text) !important;
}

:deep(.el-sub-menu__icon-arrow) {
  color: var(--sidebar-text) !important;
  font-size: 12px !important;
  right: 16px !important;
}

/* 图标尺寸优化 */
.layout-menu .el-icon {
  font-size: 20px !important;
  margin-right: 12px !important;
  color: inherit !important;
  transition: transform 0.2s ease !important;
}

/* 折叠状态图标优化 */
.layout-menu.el-menu--collapse .el-menu-item .el-icon,
.layout-menu.el-menu--collapse .el-sub-menu__title .el-icon {
  margin: 0 !important;
  font-size: 22px !important;
}

.layout-menu.el-menu--collapse .el-menu-item,
.layout-menu.el-menu--collapse .el-sub-menu__title {
  padding: 0 !important;
  display: flex !important;
  justify-content: center !important;
  margin: 4px 8px !important;
  width: calc(100% - 16px) !important;
}

/* 子菜单背景修复 */
.layout-menu .el-menu--inline {
  background-color: rgba(0, 0, 0, 0.15) !important;
  padding: 4px 0 !important;
  margin: 0 !important;
}

.layout-menu .el-menu-item:hover,
.layout-menu .el-sub-menu__title:hover {
  background-color: rgba(255, 255, 255, 0.1) !important;
  color: var(--sidebar-active-text) !important;
}

.layout-menu .el-menu-item.is-active {
  background-color: var(--el-color-primary) !important;
  color: #FFFFFF !important;
  font-weight: 600;
  box-shadow: 0 4px 12px rgba(30, 136, 229, 0.3);
}

.layout-menu .el-menu-item.is-active .el-icon {
  color: #FFFFFF !important;
}

.layout-main {
  padding: 0;
  background: var(--bg-color);
  display: flex;
  flex-direction: column;
  flex: 1;
  overflow: hidden;
}

.breadcrumb {
  padding: 0 20px;
  height: 50px;
  background: transparent;
  display: flex;
  align-items: center;
  flex-shrink: 0;
}

.breadcrumb-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.back-btn {
  font-size: 16px;
  padding: 4px;
  margin-right: 8px;
  color: var(--text-secondary);
}

.main-content {
  padding: 0 20px 20px 20px;
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
}

.main-content>div {
  width: 100%;
}

/* 响应式适配 */
@media (max-width: 768px) {
  .logo-text {
    display: none;
  }

  .layout-aside {
    position: absolute;
    z-index: 2000;
    height: 100%;
  }

  .layout-aside:not(.el-menu--collapse) {
    width: 240px;
  }

  .layout-aside.el-aside--collapse .logo-area {
    padding: 0;
    justify-content: center;
  }

  .layout-aside.el-aside--collapse .logo-icon {
    margin-right: 0;
  }
}
</style>
