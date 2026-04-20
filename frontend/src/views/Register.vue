<template>
  <div class="register-container">
    <div class="register-box">
      <div class="register-header">
        <h1 class="system-title">创建账号</h1>
        <p class="system-subtitle">加入我们，开启您的专业学习之旅</p>
      </div>

      <el-card class="register-card">
        <el-form ref="registerFormRef" :model="registerForm" :rules="rules" label-width="0">
          <el-form-item prop="username">
            <el-input v-model="registerForm.username" placeholder="用户名" prefix-icon="User" size="large" />
          </el-form-item>

          <el-row :gutter="12">
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

          <div class="role-selection">
            <span class="role-label">身份选择:</span>
            <el-radio-group v-model="registerForm.roleCode" size="large">
              <el-radio label="STUDENT">学生</el-radio>
              <el-radio label="TEACHER">教师</el-radio>
            </el-radio-group>
          </div>

          <el-button type="primary" class="register-btn" :loading="loading" size="large" @click="handleRegister">注
            册</el-button>

          <div class="login-link">
            已有账号？<el-button link type="primary" @click="$router.push('/login')">立即登录</el-button>
          </div>
        </el-form>
      </el-card>
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
.register-container {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: #F1F5F9;
  padding: 40px 20px;
  position: relative;
  overflow: hidden;
}

.register-container::before {
  content: '';
  position: absolute;
  width: 100%;
  height: 100%;
  background: radial-gradient(circle at 0% 0%, #E3F2FD 0%, transparent 50%),
              radial-gradient(circle at 100% 100%, #F1F8FE 0%, transparent 50%);
}

.register-box {
  width: 500px;
  z-index: 1;
}

.register-header {
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

.register-card {
  background: #FFFFFF !important;
  border-radius: 12px !important;
  box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.1), 0 8px 10px -6px rgba(0, 0, 0, 0.1) !important;
  padding: 10px;
  border: 1px solid #E2E8F0 !important;
}

.role-selection {
  display: flex;
  align-items: center;
  gap: 15px;
  margin-bottom: 24px;
  padding-left: 4px;
}

.role-label {
  font-size: 14px;
  color: #64748B;
}

.register-btn {
  width: 100%;
  height: 44px;
  font-size: 15px;
  font-weight: 600;
  border-radius: 8px !important;
}

.login-link {
  text-align: center;
  margin-top: 20px;
  font-size: 14px;
  color: #64748B;
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

@media (max-width: 540px) {
  .register-box {
    width: 100%;
  }
}
</style>
