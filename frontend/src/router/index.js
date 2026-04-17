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
import PaperManage from '../views/PaperManage.vue'
import ExamSchedule from '../views/ExamSchedule.vue'
import Profile from '../views/Profile.vue'
import MyExams from '../views/MyExams.vue'
import ExamRoom from '../views/ExamRoom.vue'
import StudyRecord from '../views/StudyRecord.vue'
import ExamRecord from '../views/ExamRecord.vue'
import ExamRecordDetail from '../views/ExamRecordDetail.vue'
import ExamMarkingList from '../views/ExamMarkingList.vue'
import PracticeView from '../views/PracticeView.vue'
import MistakeBook from '../views/MistakeBook.vue'
import FavoriteBook from '../views/FavoriteBook.vue'
import StudentAnalysis from '../views/StudentAnalysis.vue'
import TeacherAnalysis from '../views/TeacherAnalysis.vue'
import AdminAnalysis from '../views/AdminAnalysis.vue'
import PracticeReport from '../views/PracticeReport.vue'
import ShareList from '../views/ShareList.vue'
import ShareDetail from '../views/ShareDetail.vue'
import AnnouncementManage from '../views/AnnouncementManage.vue'

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
    meta: { requiresAuth: true, title: '试题管理', roles: ['ADMIN', 'TEACHER'] }
  },
  {
    path: '/question/category',
    name: 'QuestionCategory',
    component: KnowledgePointManage,
    meta: { requiresAuth: true, title: '知识点管理', roles: ['ADMIN', 'TEACHER'] }
  },
  {
    path: '/exam/paper',
    name: 'ExamPaper',
    component: PaperManage,
    meta: { requiresAuth: true, title: '试卷管理', roles: ['ADMIN', 'TEACHER'] }
  },
  {
    path: '/exam/schedule',
    name: 'ExamSchedule',
    component: ExamSchedule,
    meta: { requiresAuth: true, title: '考试安排', roles: ['ADMIN', 'TEACHER'] }
  },
  {
    path: '/exam/marking',
    name: 'ExamMarking',
    component: ExamMarkingList,
    meta: { requiresAuth: true, title: '阅卷管理', roles: ['ADMIN', 'TEACHER'] }
  },
  {
    path: '/teacher/analysis',
    name: 'TeacherAnalysis',
    component: TeacherAnalysis,
    meta: { requiresAuth: true, title: '班级分析', roles: ['ADMIN', 'TEACHER'] }
  },
  {
    path: '/exam/statistics',
    name: 'ExamStatistics',
    component: AdminAnalysis,
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
    component: PracticeView,
    meta: { requiresAuth: true, title: '在线练习', roles: ['STUDENT'] }
  },
  {
    path: '/study/mistakes',
    name: 'MistakeBook',
    component: MistakeBook,
    meta: { requiresAuth: true, title: '错题本', roles: ['STUDENT'] }
  },
  {
    path: '/study/favorites',
    name: 'FavoriteBook',
    component: FavoriteBook,
    meta: { requiresAuth: true, title: '收藏夹', roles: ['STUDENT'] }
  },
  {
    path: '/study/exam',
    name: 'MyExam',
    component: MyExams,
    meta: { requiresAuth: true, title: '我的考试', roles: ['STUDENT'] }
  },
  {
    path: '/study/exam/:examId',
    name: 'ExamRoom',
    component: ExamRoom,
    meta: { requiresAuth: true, title: '正在考试', roles: ['STUDENT'] }
  },
  {
    path: '/study/record',
    name: 'StudyRecord',
    component: StudyRecord,
    meta: { requiresAuth: true, title: '练习记录', roles: ['STUDENT'] }
  },
  {
    path: '/study/practice-report/:id',
    name: 'PracticeReport',
    component: PracticeReport,
    meta: { requiresAuth: true, title: '练习报告', roles: ['STUDENT'] }
  },
  {
    path: '/study/analysis',
    name: 'StudentAnalysis',
    component: StudentAnalysis,
    meta: { requiresAuth: true, title: '学习报告', roles: ['STUDENT'] }
  },
  {
    path: '/study/exam-record',
    name: 'ExamRecord',
    component: ExamRecord,
    meta: { requiresAuth: true, title: '考试记录', roles: ['STUDENT'] }
  },
  {
    path: '/study/exam-record/:recordId',
    name: 'ExamRecordDetail',
    component: ExamRecordDetail,
    meta: { requiresAuth: true, title: '考试详情', roles: ['ADMIN', 'TEACHER', 'STUDENT'] }
  },
  {
    path: '/shares',
    name: 'ShareList',
    component: ShareList,
    meta: { requiresAuth: true, title: '交流分享' }
  },
  {
    path: '/shares/:id',
    name: 'ShareDetail',
    component: ShareDetail,
    meta: { requiresAuth: true, title: '分享详情' }
  },
  {
    path: '/system/announcements',
    name: 'AnnouncementManage',
    component: AnnouncementManage,
    meta: { requiresAuth: true, title: '公告管理', roles: ['ADMIN'] }
  },
  {
    path: '/admin/logs',
    name: 'LogManage',
    component: () => import('../views/LogManage.vue'),
    meta: { requiresAuth: true, title: '操作日志', roles: ['ADMIN'] }
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

  // 定义根据角色跳转的默认路径
  const getHomePath = (roles) => {
    return '/'
  }

  // 如果是前往需要认证的路由
  if (to.meta.requiresAuth) {
    if (authed) {
      // 允许所有人访问根路径 / (Home 页面)
      if (to.path === '/') {
        next()
        return
      }

      const allowRoles = to.meta.roles
      if (allowRoles && allowRoles.length > 0) {
        const ok = userRoles.some((r) => allowRoles.includes(r))
        if (!ok) {
          ElMessage.error('没有权限访问该页面')
          next(getHomePath(userRoles))
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
    next(getHomePath(userRoles))
  } else {
    next()
  }
})

export default router
