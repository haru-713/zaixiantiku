<template>
  <div class="auth-container">
    <div class="auth-box register-box">
      <div class="auth-left">
        <div class="welcome-content">
          <h1>加入在线题库</h1>
          <p>开启您的专业学习与测评之旅</p>
          <div class="features">
            <div class="feature-item">
              <el-icon>
                <Check />
              </el-icon> 智能化题库管理
            </div>
            <div class="feature-item">
              <el-icon>
                <Check />
              </el-icon> 实时成绩反馈
            </div>
            <div class="feature-item">
              <el-icon>
                <Check />
              </el-icon> 教师互动交流
            </div>
          </div>
        </div>
      </div>
      <div class="auth-right">
        <el-card class="auth-card" :body-style="{ padding: '40px 50px' }">
          <template #header>
            <div class="card-header">
              <h3>创建账号</h3>
              <p class="subtitle">请填写以下信息完成注册</p>
            </div>
          </template>

          <el-form ref="registerFormRef" :model="registerForm" :rules="rules" label-width="0">
            <el-form-item prop="username">
              <el-input v-model="registerForm.username" placeholder="用户名" prefix-icon="User" size="large" />
            </el-form-item>
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item prop="password">
                  <el-input v-model="registerForm.password" type="password" placeholder="密码" prefix-icon="Lock"
                    show-password size="large" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item prop="confirmPassword">
                  <el-input v-model="registerForm.confirmPassword" type="password" placeholder="确认密码" prefix-icon="Lock"
                    show-password size="large" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item prop="name">
              <el-input v-model="registerForm.name" placeholder="真实姓名" prefix-icon="Postcard" size="large" />
            </el-form-item>
            <el-form-item prop="phone">
              <el-input v-model="registerForm.phone" placeholder="手机号" prefix-icon="Phone" size="large" />
            </el-form-item>
            <el-form-item prop="email">
              <el-input v-model="registerForm.email" placeholder="邮箱地址" prefix-icon="Message" size="large" />
            </el-form-item>

            <el-form-item label="身份选择" label-width="80px" class="role-item">
              <el-radio-group v-model="registerForm.roleCode" size="large">
                <el-radio-button label="STUDENT">我是学生</el-radio-button>
                <el-radio-button label="TEACHER">我是教师</el-radio-button>
              </el-radio-group>
            </el-form-item>

            <el-button type="primary" class="submit-btn" :loading="loading" size="large"
              @click="handleRegister">立即注册</el-button>

            <div class="footer-link">
              已有账号？<el-button link type="primary" @click="$router.push('/login')">立即登录</el-button>
            </div>
          </el-form>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import { useRouter } from 'vue-router'

const router = useRouter()
const registerFormRef = ref(null)
const loading = ref(false)

const registerForm = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  name: '',
  phone: '',
  email: '',
  avatar: '',
  roleCode: 'STUDENT'
})

const validatePass2 = (rule, value, callback) => {
  if (value === '') {
    callback(new Error('请再次输入密码'))
  } else if (value !== registerForm.password) {
    callback(new Error('两次输入密码不一致!'))
  } else {
    callback()
  }
}

const rules = reactive({
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '长度在 3 到 20 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少 6 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validatePass2, trigger: 'blur' }
  ],
  name: [
    { required: true, message: '请输入姓名', trigger: 'blur' }
  ],
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],
  email: [
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ]
})

const handleRegister = async () => {
  if (!registerFormRef.value) return

  await registerFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        const res = await request.post('/auth/register', registerForm)
        if (res.code === 1 || res.code === 200) {
          ElMessage.success('注册成功，请登录')
          router.push('/login')
        }
      } catch (error) {
        console.error(error)
      } finally {
        loading.value = false
      }
    }
  })
}
</script>

<style scoped>
.auth-container {
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: #f0f2f5 url('https://gw.alipayobjects.com/zos/rmsportal/TVirbqOXmUbiJpOIhfJk.svg') no-repeat center;
  background-size: 100%;
}

.auth-box {
  display: flex;
  width: 1100px;
  height: 720px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 24px 48px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.auth-left {
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

.auth-right {
  width: 580px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.auth-card {
  border: none !important;
  box-shadow: none !important;
}

.card-header {
  text-align: left;
  margin-bottom: 30px;
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

.role-item {
  margin-bottom: 30px;
}

.role-item :deep(.el-form-item__label) {
  font-weight: 500;
  color: #606266;
  font-size: 15px;
}

.submit-btn {
  width: 100%;
  height: 50px;
  font-size: 18px;
  border-radius: 10px !important;
  margin-bottom: 24px;
}

.footer-link {
  text-align: center;
  font-size: 15px;
  color: #606266;
}

/* 响应式适配 */
@media (max-width: 1100px) {
  .auth-box {
    width: 580px;
  }

  .auth-left {
    display: none;
  }
}
</style>
