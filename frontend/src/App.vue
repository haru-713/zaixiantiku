<template>
  <div class="app-root">
    <router-view v-if="isAuthPage" />
    <el-container v-else class="layout">
      <el-header class="layout-header">
        <div class="header-left">在线题库系统</div>
        <div class="header-right">
          <el-dropdown trigger="click" @command="handleUserCommand">
            <span class="user-trigger">【{{ currentUserName }}】</span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人信息</el-dropdown-item>
                <el-dropdown-item command="password">修改密码</el-dropdown-item>
                <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-container>
        <el-aside width="220px" class="layout-aside">
          <el-menu :default-active="activeMenu" router class="layout-menu">
            <el-menu-item index="/">
              首页
            </el-menu-item>

            <el-sub-menu v-if="isAdmin || isTeacher" index="qbank">
              <template #title>题库管理</template>
              <el-menu-item index="/question/manage">试题管理</el-menu-item>
              <el-menu-item index="/question/category">知识点管理</el-menu-item>
            </el-sub-menu>

            <el-sub-menu v-if="isAdmin || isTeacher" index="academic">
              <template #title>教务管理</template>
              <el-menu-item index="/course/list">课程管理</el-menu-item>
              <el-menu-item index="/exam/paper">试卷管理</el-menu-item>
              <el-menu-item index="/exam/schedule">考试安排</el-menu-item>
            </el-sub-menu>

            <el-sub-menu v-if="isAdmin || isTeacher" index="analysis">
              <template #title>成绩分析</template>
              <el-menu-item index="/exam/marking">阅卷管理</el-menu-item>
              <el-menu-item index="/teacher/analysis">班级分析</el-menu-item>
              <el-menu-item index="/exam/statistics">成绩统计</el-menu-item>
            </el-sub-menu>

            <el-sub-menu v-if="isStudent" index="study">
              <template #title>学习中心</template>
              <el-menu-item index="/course/query">课程查询</el-menu-item>
              <el-menu-item index="/study/practice">在线练习</el-menu-item>
              <el-menu-item index="/study/exam">我的考试</el-menu-item>
            </el-sub-menu>

            <el-sub-menu v-if="isStudent" index="personal">
              <template #title>个人题库</template>
              <el-menu-item index="/study/mistakes">错题本</el-menu-item>
              <el-menu-item index="/study/favorites">收藏夹</el-menu-item>
            </el-sub-menu>

            <el-sub-menu v-if="isStudent" index="records">
              <template #title>数据与记录</template>
              <el-menu-item index="/study/analysis">学习报告</el-menu-item>
              <el-menu-item index="/study/record">练习记录</el-menu-item>
              <el-menu-item index="/study/exam-record">考试记录</el-menu-item>
            </el-sub-menu>

            <el-sub-menu v-if="isAdmin || isTeacher" index="system">
              <template #title>系统管理</template>
              <el-menu-item index="/system/users">用户管理</el-menu-item>
              <el-menu-item v-if="isAdmin" index="/system/roles">角色权限</el-menu-item>
              <el-menu-item v-if="isAdmin" index="/system/settings">系统设置</el-menu-item>
            </el-sub-menu>
          </el-menu>
        </el-aside>
        <el-main class="layout-main">
          <div class="breadcrumb">
            <el-breadcrumb separator="/">
              <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
              <el-breadcrumb-item v-if="route.path !== '/'">
                {{ route.meta.title || '页面' }}
              </el-breadcrumb-item>
            </el-breadcrumb>
          </div>
          <div class="main-content">
            <router-view />
          </div>
        </el-main>
      </el-container>
    </el-container>

    <el-dialog v-if="!isAuthPage" v-model="passwordDialogVisible" title="修改密码" width="420px">
      <el-form :model="passwordForm" label-width="100px">
        <el-form-item label="旧密码">
          <el-input v-model="passwordForm.oldPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="passwordForm.newPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="确认新密码">
          <el-input v-model="passwordForm.confirmPassword" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="passwordDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="passwordLoading" @click="handleUpdatePassword">修改</el-button>
        </span>
      </template>
    </el-dialog>
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

const isAuthPage = computed(() => route.path === '/login' || route.path === '/register' || route.name === 'ExamRoom')
const roles = computed(() => userStore.userInfo?.roles || [])
const isAdmin = computed(() => roles.value.includes('ADMIN'))
const isTeacher = computed(() => roles.value.includes('TEACHER'))
const isStudent = computed(() => roles.value.includes('STUDENT'))

const currentUserName = computed(() => userStore.userInfo?.name || userStore.userInfo?.username || '')
const activeMenu = computed(() => route.meta.activeMenu || route.path)

const passwordDialogVisible = ref(false)
const passwordLoading = ref(false)
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const refreshUserInfo = async () => {
  try {
    const res = await request.get('/user/me')
    userStore.setUserInfo(res.data)
  } catch (e) {
    console.error('获取用户信息失败:', e)
  }
}

const openPasswordDialog = () => {
  passwordForm.oldPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
  passwordDialogVisible.value = true
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
  if (command === 'password') {
    openPasswordDialog()
    return
  }
  if (command === 'logout') {
    handleLogout()
  }
}

const handleUpdatePassword = async () => {
  if (!passwordForm.oldPassword || !passwordForm.newPassword) {
    ElMessage.warning('请输入密码')
    return
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    ElMessage.warning('两次输入的新密码不一致')
    return
  }
  if (passwordForm.oldPassword === passwordForm.newPassword) {
    ElMessage.warning('新密码不能与旧密码相同')
    return
  }
  if (passwordForm.newPassword.length < 6) {
    ElMessage.warning('新密码长度不能少于6位')
    return
  }

  passwordLoading.value = true
  try {
    await request.put('/user/me/password', {
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })
    ElMessage.success('密码修改成功，请重新登录')
    passwordDialogVisible.value = false
    handleLogout()
  } catch (e) {
    console.error('修改密码失败:', e)
  } finally {
    passwordLoading.value = false
  }
}

onMounted(() => {
  if (userStore.token) {
    refreshUserInfo()
  }
})
</script>

<style>
body {
  margin: 0;
  padding: 0;
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', '微软雅黑', Arial, sans-serif;
}

.layout {
  height: 100vh;
}

.layout-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #409eff;
  color: #fff;
}

.header-left {
  font-family: KaiTi, '楷体', serif;
  font-size: 30px;
  text-align: left;
}

.header-right {
  cursor: pointer;
  user-select: none;
}

.user-trigger {
  color: #fff;
}

.layout-aside {
  background: #fff;
  border-right: 1px solid #ebeef5;
}

.layout-menu {
  border-right: none;
}

.layout-main {
  padding: 0;
  background: #f5f7fa;
}

.breadcrumb {
  padding: 12px 16px;
  background: #fff;
  border-bottom: 1px solid #ebeef5;
}

.main-content {
  padding: 16px;
}
</style>
