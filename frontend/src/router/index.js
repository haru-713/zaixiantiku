import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import Login from '../views/Login.vue'
import Register from '../views/Register.vue'
import Home from '../views/Home.vue'
import AdminUserList from '../views/AdminUserList.vue'

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
    meta: { requiresAuth: true }
  },
  {
    path: '/admin/users',
    name: 'AdminUserList',
    component: AdminUserList,
    meta: { requiresAuth: true, requiresAdmin: true }
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
  const token = localStorage.getItem('token')
  
  // 如果是前往需要认证的路由
  if (to.meta.requiresAuth) {
    if (token) {
      // 检查管理员权限
      if (to.meta.requiresAdmin) {
        const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
        if (userInfo.roles && userInfo.roles.includes('ADMIN')) {
          next()
        } else {
          ElMessage.error('没有权限访问该页面')
          next('/')
        }
      } else {
        next()
      }
    } else {
      // 没有 token，重定向到登录页
      next('/login')
    }
  } else if ((to.path === '/login' || to.path === '/register') && token) {
    // 已经登录了还去登录/注册页，直接跳回首页
    next('/')
  } else {
    next()
  }
})

export default router
