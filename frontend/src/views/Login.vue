<template>
  <div class="login-container">
    <div class="login-box">
      <div class="login-header">
        <h1 class="system-title">在线题库系统</h1>
        <p class="system-subtitle">专业、智能、高效的在线学习平台</p>
      </div>

      <el-card class="login-card">
        <div class="card-title">用户登录</div>
        <el-form ref="loginFormRef" :model="loginForm" :rules="rules" label-width="0">
          <el-form-item prop="username">
            <el-input v-model="loginForm.username" placeholder="用户名" prefix-icon="User" size="large" />
          </el-form-item>
          <el-form-item prop="password">
            <el-input v-model="loginForm.password" type="password" placeholder="密码" prefix-icon="Lock" show-password
              size="large" @keyup.enter="handleLogin" />
          </el-form-item>

          <div class="form-options">
            <el-button link type="primary" @click="handleForgotPassword">忘记密码？</el-button>
          </div>

          <el-button type="primary" class="login-btn" :loading="loading" size="large" @click="handleLogin">
            登 录
          </el-button>

          <div class="register-link">
            还没有账号？<el-button link type="primary" @click="$router.push('/register')">立即注册</el-button>
          </div>
        </el-form>
      </el-card>

      <div class="login-footer">
        <div class="feature-tag"><el-icon>
            <CircleCheckFilled />
          </el-icon>海量题库</div>
        <div class="feature-tag"><el-icon>
            <CircleCheckFilled />
          </el-icon>智能评分</div>
        <div class="feature-tag"><el-icon>
            <CircleCheckFilled />
          </el-icon>数据分析</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import request from '@/utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

const loginFormRef = ref(null)
const loading = ref(false)

const loginForm = reactive({
  username: '',
  password: ''
})

const rules = reactive({
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' }
  ]
})

const handleLogin = async () => {
  if (!loginFormRef.value) return

  await loginFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        const response = await request.post('/auth/login', loginForm)
        ElMessage.success('登录成功')

        const { token, userInfo } = response.data
        userStore.setToken(token)
        userStore.setUserInfo(userInfo)

        router.push('/home')
      } catch (error) {
        console.error(error)
      } finally {
        loading.value = false
      }
    }
  })
}

const handleForgotPassword = () => {
  ElMessageBox.alert(
    '请联系系统管理员重置您的密码。<br/><br/>' +
    '<b>管理员邮箱：</b>admin@example.com<br/>' +
    '<b>联系电话：</b>138-0000-0001',
    '找回密码',
    {
      confirmButtonText: '确定',
      dangerouslyUseHTMLString: true
    }
  )
}
</script>

<style scoped>
.login-container {
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: #F1F5F9;
  position: relative;
  overflow: hidden;
}

.login-container::before {
  content: '';
  position: absolute;
  width: 100%;
  height: 100%;
  background: radial-gradient(circle at 0% 0%, #E3F2FD 0%, transparent 50%),
    radial-gradient(circle at 100% 100%, #F1F8FE 0%, transparent 50%);
}

.login-box {
  width: 400px;
  z-index: 1;
}

.login-header {
  text-align: center;
  margin-bottom: 30px;
}

.system-title {
  font-size: 28px;
  font-weight: bold;
  color: #1E293B;
  margin-bottom: 8px;
  letter-spacing: 1px;
}

.system-subtitle {
  font-size: 14px;
  color: #64748B;
}

.login-card {
  background: #FFFFFF !important;
  border-radius: 12px !important;
  box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.1), 0 8px 10px -6px rgba(0, 0, 0, 0.1) !important;
  padding: 10px;
  border: 1px solid #E2E8F0 !important;
}

.card-title {
  font-size: 20px;
  font-weight: 600;
  color: #1E293B;
  text-align: center;
  margin-bottom: 30px;
}

.form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.login-btn {
  width: 100%;
  height: 44px;
  font-size: 15px;
  font-weight: 600;
  margin-top: 10px;
  border-radius: 8px !important;
}

.register-link {
  text-align: center;
  margin-top: 24px;
  font-size: 14px;
  color: #64748B;
}

.login-footer {
  margin-top: 40px;
  display: flex;
  justify-content: center;
  gap: 20px;
}

.feature-tag {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #64748B;
  font-size: 13px;
  background: #FFFFFF;
  padding: 6px 12px;
  border-radius: 20px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
  border: 1px solid #E2E8F0;
}

:deep(.el-input__wrapper) {
  background-color: #F8FAFC !important;
  box-shadow: 0 0 0 1px #E2E8F0 inset !important;
  transition: all 0.2s;
}

:deep(.el-input__wrapper.is-focus) {
  background-color: #FFFFFF !important;
  box-shadow: 0 0 0 1px #1E88E5 inset !important;
}

@media (max-width: 480px) {
  .login-box {
    width: 90%;
  }
}
</style>
