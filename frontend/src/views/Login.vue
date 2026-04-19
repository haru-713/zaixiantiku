<template>
  <div class="login-container">
    <div class="login-box">
      <div class="login-left">
        <div class="welcome-content">
          <h1>在线题库系统</h1>
          <p>专业的在线学习与考试平台</p>
          <div class="features">
            <div class="feature-item">
              <el-icon>
                <Check />
              </el-icon> 海量题库资源
            </div>
            <div class="feature-item">
              <el-icon>
                <Check />
              </el-icon> 智能考试评分
            </div>
            <div class="feature-item">
              <el-icon>
                <Check />
              </el-icon> 详尽数据分析
            </div>
          </div>
        </div>
      </div>
      <div class="login-right">
        <el-card class="login-card" :body-style="{ padding: '40px' }">
          <template #header>
            <div class="card-header">
              <h3>用户登录</h3>
              <p class="subtitle">请登录您的账号以开始使用</p>
            </div>
          </template>

          <el-form ref="loginFormRef" :model="loginForm" :rules="rules" label-width="0">
            <el-form-item prop="username">
              <el-input v-model="loginForm.username" placeholder="用户名" prefix-icon="User" size="large" />
            </el-form-item>
            <el-form-item prop="password">
              <el-input v-model="loginForm.password" type="password" placeholder="密码" prefix-icon="Lock" show-password
                size="large" @keyup.enter="handleLogin" />
            </el-form-item>

            <div class="form-actions">
              <el-button link type="primary" @click="handleForgotPassword">忘记密码？</el-button>
            </div>

            <el-button type="primary" class="login-btn" :loading="loading" size="large"
              @click="handleLogin">立即登录</el-button>

            <div class="register-link">
              还没有账号？<el-button link type="primary" @click="$router.push('/register')">立即注册</el-button>
            </div>
          </el-form>
        </el-card>
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
  ElMessageBox.confirm(
    '请联系管理员重置您的密码。重置后初始密码为 123456，登录后请及时修改。\n\n' +
    '管理员联系方式：\n' +
    '📧 邮箱：admin@example.com\n' +
    '📞 电话：138-0000-0001',
    '找回密码提示',
    {
      confirmButtonText: '我知道了',
      cancelButtonText: '取消',
      type: 'info',
      distinguishCancelAndClose: true,
      message: '请联系管理员重置您的密码。重置后初始密码为 123456，登录后请及时修改。<br/><br/>' +
        '<b>管理员联系方式：</b><br/>' +
        '📧 邮箱：admin@example.com<br/>' +
        '📞 电话：138-0000-0001',
      dangerouslyUseHTMLString: true
    }
  )
}

const resetForm = () => {
  if (loginFormRef.value) {
    loginFormRef.value.resetFields()
  }
}
</script>

<style scoped>
.login-container {
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: #f0f2f5 url('https://gw.alipayobjects.com/zos/rmsportal/TVirbqOXmUbiJpOIhfJk.svg') no-repeat center;
  background-size: 100%;
}

.login-box {
  display: flex;
  width: 1100px;
  height: 720px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 24px 48px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.login-left {
  flex: 1;
  background: linear-gradient(135deg, #2c3e50 0%, #34495e 100%);
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 60px;
  color: #fff;
}

.welcome-content h1 {
  font-size: 36px;
  margin-bottom: 20px;
  font-family: KaiTi, '楷体', serif;
}

.welcome-content p {
  font-size: 18px;
  opacity: 0.85;
  margin-bottom: 50px;
}

.features {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 16px;
  font-size: 16px;
}

.feature-item .el-icon {
  background: rgba(255, 255, 255, 0.2);
  padding: 6px;
  border-radius: 50%;
  font-size: 14px;
}

.login-right {
  width: 580px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.login-card {
  border: none !important;
  box-shadow: none !important;
}

.card-header {
  text-align: left;
  margin-bottom: 40px;
}

.card-header h3 {
  font-size: 28px;
  margin: 0 0 10px 0;
  color: #303133;
}

.card-header .subtitle {
  font-size: 15px;
  color: #909399;
  margin: 0;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  margin-bottom: 30px;
}

.login-btn {
  width: 100%;
  height: 50px;
  font-size: 18px;
  border-radius: 10px !important;
  margin-bottom: 24px;
}

.register-link {
  text-align: center;
  font-size: 15px;
  color: #606266;
}

/* 响应式适配 */
@media (max-width: 1100px) {
  .login-box {
    width: 580px;
  }

  .login-left {
    display: none;
  }
}
</style>
