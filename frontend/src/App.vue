<template>
  <div class="app-root">
    <router-view v-if="isAuthPage" />
    <el-container v-else class="layout">
      <el-header class="layout-header">
        <div class="header-left">
          <el-icon class="collapse-btn" @click="isCollapse = !isCollapse">
            <Expand v-if="isCollapse" />
            <Fold v-else />
          </el-icon>
          <span class="logo-text">在线题库系统</span>
        </div>
        <div class="header-right">
          <el-dropdown trigger="click" @command="handleUserCommand">
            <div class="user-trigger">
              <el-avatar :size="32" icon="UserFilled" />
              <span class="username">{{ currentUserName }}</span>
              <el-icon><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <el-icon><User /></el-icon>个人中心
                </el-dropdown-item>
                <el-dropdown-item divided command="logout">
                  <el-icon><SwitchButton /></el-icon>退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-container>
        <el-aside :width="isCollapse ? '64px' : '220px'" class="layout-aside">
          <el-menu
            :default-active="activeMenu"
            router
            class="layout-menu"
            :collapse="isCollapse"
            :collapse-transition="false"
          >
            <el-menu-item index="/home">
              <el-icon><HomeFilled /></el-icon>
              <template #title>首页</template>
            </el-menu-item>

            <el-sub-menu v-if="isAdmin || isTeacher" index="qbank">
              <template #title>
                <el-icon><Reading /></el-icon>
                <span>题库资源</span>
              </template>
              <el-menu-item index="/question/manage">试题管理</el-menu-item>
              <el-menu-item index="/question/category">知识点管理</el-menu-item>
              <el-menu-item index="/exam/paper">试卷管理</el-menu-item>
            </el-sub-menu>

            <el-sub-menu v-if="isAdmin || isTeacher" index="teaching">
              <template #title>
                <el-icon><Management /></el-icon>
                <span>教学管理</span>
              </template>
              <el-menu-item index="/course/list">课程管理</el-menu-item>
              <el-menu-item index="/exam/schedule">考试安排</el-menu-item>
              <el-menu-item index="/exam/marking">阅卷管理</el-menu-item>
            </el-sub-menu>

            <el-sub-menu v-if="isAdmin || isTeacher" index="analysis">
              <template #title>
                <el-icon><DataLine /></el-icon>
                <span>成绩分析</span>
              </template>
              <el-menu-item index="/teacher/analysis">班级分析</el-menu-item>
              <el-menu-item index="/exam/statistics">成绩统计</el-menu-item>
            </el-sub-menu>

            <el-sub-menu v-if="isStudent" index="study">
              <template #title>
                <el-icon><Monitor /></el-icon>
                <span>学习中心</span>
              </template>
              <el-menu-item index="/course/query">课程查询</el-menu-item>
              <el-menu-item index="/study/practice">在线练习</el-menu-item>
              <el-menu-item index="/study/exam">我的考试</el-menu-item>
              <el-menu-item index="/shares">交流分享</el-menu-item>
            </el-sub-menu>

            <el-sub-menu v-if="isStudent" index="personal">
              <template #title>
                <el-icon><Collection /></el-icon>
                <span>个人题库</span>
              </template>
              <el-menu-item index="/study/mistakes">错题本</el-menu-item>
              <el-menu-item index="/study/favorites">收藏夹</el-menu-item>
            </el-sub-menu>

            <el-sub-menu v-if="isStudent" index="records">
              <template #title>
                <el-icon><PieChart /></el-icon>
                <span>数据统计</span>
              </template>
              <el-menu-item index="/study/analysis">学习报告</el-menu-item>
              <el-menu-item index="/study/record">练习记录</el-menu-item>
              <el-menu-item index="/study/exam-record">考试记录</el-menu-item>
            </el-sub-menu>

            <el-sub-menu v-if="isAdmin || isTeacher" index="system">
              <template #title>
                <el-icon><Setting /></el-icon>
                <span>系统管理</span>
              </template>
              <el-menu-item index="/system/classes">班级管理</el-menu-item>
              <el-menu-item index="/admin/users">用户管理</el-menu-item>
              <el-menu-item index="/admin/logs" v-if="isAdmin">操作日志</el-menu-item>
            </el-sub-menu>
          </el-menu>
        </el-aside>
        <el-main class="layout-main">
          <div class="breadcrumb">
            <div class="breadcrumb-left">
              <el-button
                v-if="route.path !== '/home'"
                link
                icon="Back"
                @click="router.back()"
                class="back-btn"
              >返回</el-button>
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
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const isCollapse = ref(false)
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

onMounted(() => {
  if (userStore.token) {
    refreshUserInfo()
  }
})
</script>

<style>
html {
  overflow-y: scroll;
}

body {
  margin: 0;
  padding: 0;
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', '微软雅黑', Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

/* 统一滚动条样式 */
::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 4px;
}

::-webkit-scrollbar-thumb {
  background: #ccc;
  border-radius: 4px;
}

::-webkit-scrollbar-thumb:hover {
  background: #999;
}

.layout {
  height: 100vh;
}

.layout-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #2c3e50;
  color: #fff;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.12);
  z-index: 1001;
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
  transition: background-color 0.3s;
}

.collapse-btn:hover {
  background-color: rgba(255, 255, 255, 0.1);
}

.logo-text {
  font-family: KaiTi, '楷体', serif;
  font-size: 24px;
  font-weight: bold;
}

.header-right {
  display: flex;
  align-items: center;
}

.user-trigger {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #fff;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
  transition: background-color 0.3s;
}

.user-trigger:hover {
  background-color: rgba(255, 255, 255, 0.1);
}

.user-trigger .username {
  font-size: 14px;
}

.layout-aside {
  background: #fff;
  border-right: 1px solid #ebeef5;
  transition: width 0.3s;
  overflow: hidden;
}

.layout-menu {
  border-right: none;
}

.layout-main {
  padding: 0;
  background: #f5f7fa;
  display: flex;
  flex-direction: column;
  height: calc(100vh - 60px); /* 减去 header 高度 */
  overflow: hidden;
}

.breadcrumb {
  padding: 12px 24px;
  background: #fff;
  border-bottom: 1px solid #ebeef5;
  flex-shrink: 0;
}

.breadcrumb-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.back-btn {
  font-size: 14px;
  padding: 0;
  height: auto;
}

.main-content {
  padding: 20px 24px;
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  display: flex;
  flex-direction: column;
}

.main-content > div {
  width: 100%;
  flex: 1;
}

/* 全局卡片样式 */
.el-card {
  border-radius: 12px !important;
  box-shadow: 0 8px 24px rgba(149, 157, 165, 0.1) !important;
  border: 1px solid #ebeef5 !important;
  transition: all 0.3s ease;
}

.el-card:hover {
  box-shadow: 0 12px 32px rgba(149, 157, 165, 0.2) !important;
}

.el-card__header {
  padding: 16px 20px !important;
  border-bottom: 1px solid #f0f0f0 !important;
  background-color: #fafafa;
}

/* 按钮圆角统一 */
.el-button {
  border-radius: 6px !important;
}

/* 输入框圆角统一 */
.el-input__wrapper {
  border-radius: 6px !important;
}

/* 表格样式优化 */
.el-table {
  border-radius: 8px;
  overflow: hidden;
}

.el-table th.el-table__cell {
  background-color: #f8f9fb !important;
  color: #606266;
  font-weight: 600;
}

/* 进度条颜色 */
#nprogress .bar {
  background: #409eff !important;
  height: 3px !important;
}

/* 响应式适配 */
@media (max-width: 768px) {
  .logo-text {
    display: none;
  }
}
</style>
