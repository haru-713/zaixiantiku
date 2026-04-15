import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import Login from '../views/Login.vue'
import Register from '../views/Register.vue'
import Home from '../views/Home.vue'
import AdminUserList from '../views/AdminUserList.vue'
import ComingSoon from '../views/ComingSoon.vue'
import CourseCreate from '../views/CourseCreate.vue'
import CourseDetail from '../views/CourseDetail.vue'
import QuestionManage from '../views/QuestionManage.vue'
import KnowledgePointManage from '../views/KnowledgePointManage.vue'
import Profile from '../views/Profile.vue'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: Login
  },
  {
    path: '/register',
    name: 'Register',
    component: Register
  },
  {
    path: '/',
    name: 'Home',
    component: Home,
    meta: { requiresAuth: true, title: '首页' }
  },
  {
    path: '/system/users',
    name: 'AdminUserList',
    component: AdminUserList,
    meta: { requiresAuth: true, title: '用户管理', roles: ['ADMIN', 'TEACHER'] }
  },
  {
    path: '/admin/users',
    redirect: '/system/users'
  },
  {
    path: '/question/manage',
    name: 'QuestionManage',
    component: QuestionManage,
    meta: { requiresAuth: true, title: '题目管理', roles: ['ADMIN', 'TEACHER'] }
  },
  {
    path: '/question/category',
    name: 'QuestionCategory',
    component: KnowledgePointManage,
    meta: { requiresAuth: true, title: '题库分类', roles: ['ADMIN', 'TEACHER'] }
  },
  {
    path: '/exam/paper',
    name: 'ExamPaper',
    component: ComingSoon,
    meta: { requiresAuth: true, title: '试卷管理', roles: ['ADMIN', 'TEACHER'] }
  },
  {
    path: '/exam/schedule',
    name: 'ExamSchedule',
    component: ComingSoon,
    meta: { requiresAuth: true, title: '考试安排', roles: ['ADMIN', 'TEACHER'] }
  },
  {
    path: '/exam/statistics',
    name: 'ExamStatistics',
    component: ComingSoon,
    meta: { requiresAuth: true, title: '成绩统计', roles: ['ADMIN', 'TEACHER'] }
  },
  {
    path: '/course/list',
    name: 'CourseList',
    component: CourseCreate,
    meta: { requiresAuth: true, title: '课程管理', roles: ['ADMIN', 'TEACHER'] }
  },
  {
    path: '/course/query',
    name: 'CourseQuery',
    component: CourseCreate,
    meta: { requiresAuth: true, title: '课程查询', roles: ['STUDENT'] }
  },
  {
    path: '/course/detail/:courseId',
    name: 'CourseDetail',
    component: CourseDetail,
    meta: { requiresAuth: true, title: '课程详情', roles: ['ADMIN', 'TEACHER', 'STUDENT'] }
  },
  {
    path: '/profile',
    name: 'Profile',
    component: Profile,
    meta: { requiresAuth: true, title: '个人信息' }
  },
  {
    path: '/study/practice',
    name: 'MyPractice',
    component: ComingSoon,
    meta: { requiresAuth: true, title: '我的练习', roles: ['STUDENT'] }
  },
  {
    path: '/study/exam',
    name: 'MyExam',
    component: ComingSoon,
    meta: { requiresAuth: true, title: '我的考试', roles: ['STUDENT'] }
  },
  {
    path: '/study/record',
    name: 'StudyRecord',
    component: ComingSoon,
    meta: { requiresAuth: true, title: '学习记录', roles: ['STUDENT'] }
  },
  {
    path: '/system/roles',
    name: 'RolePermission',
    component: ComingSoon,
    meta: { requiresAuth: true, title: '角色权限', roles: ['ADMIN'] }
  },
  {
    path: '/system/settings',
    name: 'SystemSettings',
    component: ComingSoon,
    meta: { requiresAuth: true, title: '系统设置', roles: ['ADMIN'] }
  },
  // 捕获所有未定义的路由并重定向到首页
  {
    path: '/:pathMatch(.*)*',
    redirect: '/'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const rawToken = (localStorage.getItem('token') || '').trim()
  const token = rawToken && rawToken !== 'null' && rawToken !== 'undefined' ? rawToken : ''
  const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
  const userRoles = userInfo.roles || []

  const isTokenExpired = (jwt) => {
    try {
      const parts = String(jwt).split('.')
      if (parts.length !== 3) return false
      const payload = JSON.parse(decodeURIComponent(escape(atob(parts[1].replace(/-/g, '+').replace(/_/g, '/')))))
      const exp = payload?.exp
      if (!exp) return false
      return Date.now() >= Number(exp) * 1000
    } catch {
      return false
    }
  }

  const clearAuth = () => {
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
  }

  const authed = token && !isTokenExpired(token)
  if (!authed && token) {
    clearAuth()
  }
  
  // 如果是前往需要认证的路由
  if (to.meta.requiresAuth) {
    if (authed) {
      const allowRoles = to.meta.roles
      if (allowRoles && allowRoles.length > 0) {
        const ok = userRoles.some((r) => allowRoles.includes(r))
        if (!ok) {
          ElMessage.error('没有权限访问该页面')
          next('/')
          return
        }
      }
      next()
    } else {
      // 没有 token，重定向到登录页
      next('/login')
    }
  } else if ((to.path === '/login' || to.path === '/register') && authed) {
    // 已经登录了还去登录/注册页，直接跳回首页
    next('/')
  } else {
    next()
  }
})

export default router
