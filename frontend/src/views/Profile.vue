<template>
  <div class="profile-container">
    <el-row :gutter="20">
      <!-- 左侧卡片：头像和基本信息 -->
      <el-col :xs="24" :md="8">
        <el-card class="profile-card">
          <div class="user-profile">
            <el-upload
              class="avatar-uploader"
              action="#"
              :show-file-list="false"
              :http-request="handleAvatarUpload"
              :before-upload="beforeAvatarUpload"
            >
              <el-avatar :size="100" :src="userInfo.avatar" icon="UserFilled" class="profile-avatar" />
              <div class="avatar-mask">
                <el-icon><Camera /></el-icon>
                <span>更换头像</span>
              </div>
            </el-upload>
            <h2 class="user-name">{{ userInfo.name || '未设置姓名' }}</h2>
            <p class="user-role">
              <el-tag v-for="role in userInfo.roles" :key="role" size="small" effect="plain" class="role-tag">
                {{ getRoleName(role) }}
              </el-tag>
            </p>
            <div class="user-status">
              <el-tag :type="userInfo.status === 1 ? 'success' : 'danger'" size="small">
                {{ userInfo.status === 1 ? '账号正常' : '已禁用' }}
              </el-tag>
              <el-tag :type="auditStatusType" size="small" class="ml-2">
                {{ auditStatusLabel }}
              </el-tag>
            </div>
          </div>

          <div class="profile-meta">
            <div class="meta-item" v-if="userInfo.classes && userInfo.classes.length > 0">
              <el-icon>
                <Collection />
              </el-icon>
              <span>{{ isTeacher ? '管辖班级' : '所属班级' }}：{{ userInfo.classes.map(c => c.className).join(', ') }}</span>
            </div>
            <div class="meta-item">
              <el-icon>
                <Calendar />
              </el-icon>
              <span>注册于 {{ formatDate(userInfo.createTime) }}</span>
            </div>
            <div class="meta-item">
              <el-icon>
                <Phone />
              </el-icon>
              <span>{{ userInfo.phone || '未绑定手机' }}</span>
            </div>
            <div class="meta-item">
              <el-icon>
                <Message />
              </el-icon>
              <span>{{ userInfo.email || '未绑定邮箱' }}</span>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 右侧卡片：详细资料编辑 & 密码管理 -->
      <el-col :xs="24" :md="16">
        <el-card class="detail-card">
          <template #header>
            <div class="card-header">
              <span>详细资料</span>
              <el-button type="primary" :loading="saving" @click="handleUpdate">保存修改</el-button>
            </div>
          </template>

          <el-form :model="form" label-width="100px" label-position="left">
            <el-form-item label="登录账号">
              <el-input v-model="userInfo.username" disabled />
              <div class="form-tip">登录账号不可修改</div>
            </el-form-item>
            <el-form-item label="真实姓名">
              <el-input v-model="form.name" placeholder="请输入姓名" />
            </el-form-item>
            <el-form-item label="手机号码">
              <el-input v-model="form.phone" placeholder="请输入手机号" />
            </el-form-item>
            <el-form-item label="电子邮箱">
              <el-input v-model="form.email" placeholder="请输入邮箱" />
            </el-form-item>
          </el-form>
        </el-card>

        <el-card class="password-card" style="margin-top: 20px;">
          <template #header>
            <div class="card-header">
              <span>密码管理</span>
            </div>
          </template>
          <div class="password-content">
            <p class="password-tip">建议定期更换密码，以提高账号安全性。</p>
            <el-button type="warning" @click="openPasswordDialog">修改登录密码</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 修改密码对话框 -->
    <el-dialog v-model="passwordDialogVisible" title="修改密码" width="500px" destroy-on-close center :lock-scroll="false">
      <el-form :model="passwordForm" label-width="100px" label-position="top">
        <el-form-item label="当前密码">
          <el-input v-model="passwordForm.oldPassword" type="password" show-password placeholder="请输入当前登录密码"
            prefix-icon="Lock" />
        </el-form-item>

        <el-form-item label="新密码">
          <el-input v-model="passwordForm.newPassword" type="password" show-password placeholder="请输入新密码 (至少6位)"
            prefix-icon="Lock" />
          <!-- 密码强度条 -->
          <div class="password-strength" v-if="passwordForm.newPassword">
            <div class="strength-bar" :class="strengthClass"></div>
            <span class="strength-text" :class="strengthClass">强度：{{ strengthText }}</span>
          </div>
        </el-form-item>

        <el-form-item label="确认新密码">
          <el-input v-model="passwordForm.confirmPassword" type="password" show-password placeholder="请再次输入新密码"
            prefix-icon="Lock" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="passwordDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="passwordLoading" @click="handleUpdatePassword">确定修改</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useUserStore } from '@/store/user'
import { useRouter } from 'vue-router'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'
import { Camera, Collection, Calendar, Phone, Message, Lock, UserFilled, ArrowUp, ArrowDown } from '@element-plus/icons-vue'

const userStore = useUserStore()
const router = useRouter()
const userInfo = computed(() => userStore.userInfo || {})
const roles = computed(() => userInfo.value.roles || [])
const isAdmin = computed(() => roles.value.includes('ADMIN'))
const isTeacher = computed(() => roles.value.includes('TEACHER'))
const isStudent = computed(() => roles.value.includes('STUDENT'))
const saving = ref(false)

const form = reactive({
  name: '',
  phone: '',
  email: ''
})

const passwordDialogVisible = ref(false)
const passwordLoading = ref(false)
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const strengthClass = computed(() => {
  const pwd = passwordForm.newPassword
  if (!pwd) return ''
  let score = 0
  if (pwd.length >= 6) score++
  if (/[A-Z]/.test(pwd)) score++
  if (/[0-9]/.test(pwd)) score++
  if (/[^A-Za-z0-9]/.test(pwd)) score++

  if (score <= 1) return 'weak'
  if (score <= 3) return 'medium'
  return 'strong'
})

const strengthText = computed(() => {
  const cls = strengthClass.value
  if (cls === 'weak') return '弱'
  if (cls === 'medium') return '中'
  if (cls === 'strong') return '强'
  return ''
})

const getRoleName = (role) => {
  const map = {
    'ADMIN': '管理员',
    'TEACHER': '教师',
    'STUDENT': '学生',
    'ROLE_ADMIN': '管理员',
    'ROLE_TEACHER': '教师',
    'ROLE_STUDENT': '学生'
  }
  return map[role] || role
}

const auditStatusLabel = computed(() => {
  const labels = { 0: '待审核', 1: '审核通过', 2: '审核拒绝' }
  return labels[userInfo.value.auditStatus] || '未知状态'
})

const auditStatusType = computed(() => {
  const types = { 0: 'warning', 1: 'success', 2: 'danger' }
  return types[userInfo.value.auditStatus] || 'info'
})

const formatDate = (time) => {
  if (!time) return '-'
  return time.split('T')[0]
}

const handleUpdate = async () => {
  if (!form.name.trim()) {
    return ElMessage.warning('姓名不能为空')
  }

  saving.value = true
  try {
    const res = await request.put('/user/me', form)
    ElMessage.success('资料更新成功')
    userStore.setUserInfo(res.data)
  } catch (e) {
    console.error('更新资料失败:', e)
  } finally {
    saving.value = false
  }
}

const beforeAvatarUpload = (file) => {
  const isJPGorPNG = file.type === 'image/jpeg' || file.type === 'image/png'
  const isLt2M = file.size / 1024 / 1024 < 2

  if (!isJPGorPNG) {
    ElMessage.error('上传头像图片只能是 JPG 或 PNG 格式!')
  }
  if (!isLt2M) {
    ElMessage.error('上传头像图片大小不能超过 2MB!')
  }
  return isJPGorPNG && isLt2M
}

const handleAvatarUpload = async (options) => {
  const { file } = options
  const formData = new FormData()
  formData.append('file', file)

  try {
    const res = await request.post('/upload/avatar', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    if (res.code === 1) {
      // 上传成功后，更新用户信息中的头像字段
      const updatedInfo = { ...userInfo.value, avatar: res.data }
      await request.put('/user/me', { ...form, avatar: res.data })
      userStore.setUserInfo(updatedInfo)
      ElMessage.success('头像修改成功')
    }
  } catch (e) {
    console.error('头像上传失败:', e)
    ElMessage.error('头像上传失败')
  }
}

const openPasswordDialog = () => {
  passwordForm.oldPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
  passwordDialogVisible.value = true
}

const handleUpdatePassword = async () => {
  // 1. 前端基础校验
  if (!passwordForm.oldPassword) {
    return ElMessage.warning('请输入当前密码')
  }
  if (!passwordForm.newPassword) {
    return ElMessage.warning('请输入新密码')
  }
  if (passwordForm.newPassword.length < 6) {
    return ElMessage.warning('新密码长度不能少于6位')
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    return ElMessage.warning('两次输入的新密码不一致')
  }
  if (passwordForm.oldPassword === passwordForm.newPassword) {
    return ElMessage.warning('新密码不能与旧密码相同')
  }

  passwordLoading.value = true
  try {
    // 2. 调用后端接口 (后端会按顺序校验：旧密码 -> 新密码查重)
    await request.put('/user/me/password', {
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })

    // 3. 修改成功处理
    ElMessage.success('密码修改成功，为了您的账号安全，请重新登录')
    passwordDialogVisible.value = false

    // 4. 自动退出登录
    userStore.logout()
    router.push('/login')
  } catch (e) {
    // 后端抛出的异常（如“旧密码错误”）会被拦截器捕获并显示
    console.error('修改密码失败:', e)
  } finally {
    passwordLoading.value = false
  }
}

const fetchUserInfo = async () => {
  try {
    const res = await request.get('/user/me');
    userStore.setUserInfo(res.data);
    form.name = res.data.name || '';
    form.phone = res.data.phone || '';
    form.email = res.data.email || '';
  } catch (error) {
    console.error('获取用户最新信息失败:', error);
    ElMessage.error('获取用户最新信息失败');
  }
};

onMounted(() => {
  fetchUserInfo();
});
</script>

<style scoped>
.profile-container {
  width: 100%;
}

.profile-card {
  text-align: center;
  padding: 20px 0;
}

.profile-avatar {
  border: 4px solid #fff;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.avatar-uploader {
  position: relative;
  display: inline-block;
  cursor: pointer;
  border-radius: 50%;
  overflow: hidden;
  margin-bottom: 16px;
}

.avatar-mask {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.4);
  color: #fff;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s;
  border-radius: 50%;
  font-size: 12px;
  gap: 4px;
}

.avatar-uploader:hover .avatar-mask {
  opacity: 1;
}

.avatar-mask .el-icon {
  font-size: 20px;
}

.user-name {
  margin: 0 0 12px 0;
  font-size: 24px;
  color: #303133;
}

.user-role {
  margin-bottom: 16px;
}

.role-tag {
  margin: 0 4px;
}

.user-status {
  margin-bottom: 24px;
}

.profile-meta {
  border-top: 1px solid #f0f0f0;
  padding-top: 20px;
  text-align: left;
  margin: 0 20px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  color: #606266;
  font-size: 14px;
}

.meta-item .el-icon {
  font-size: 16px;
  color: #909399;
}

.detail-card {
  min-height: 400px;
}

.password-card .password-content {
  padding: 10px 0;
}

.password-tip {
  font-size: 14px;
  color: #909399;
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: bold;
}

.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
  line-height: 1;
}

.ml-2 {
  margin-left: 8px;
}

/* 密码强度相关样式 */
.password-strength {
  margin-top: 12px;
  display: flex;
  align-items: center;
  gap: 15px;
}

.strength-bar {
  flex: 1;
  height: 8px;
  background-color: #f0f2f5;
  border-radius: 4px;
  position: relative;
  overflow: hidden;
}

.strength-bar::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  height: 100%;
  border-radius: 4px;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  width: 0;
}

.strength-bar.weak::before {
  width: 33.33%;
  background-color: var(--el-color-danger);
}

.strength-bar.medium::before {
  width: 66.66%;
  background-color: var(--el-color-warning);
}

.strength-bar.strong::before {
  width: 100%;
  background-color: var(--el-color-success);
}

.strength-text {
  font-size: 13px;
  min-width: 50px;
  font-weight: 500;
}

.strength-text.weak {
  color: var(--el-color-danger);
}

.strength-text.medium {
  color: var(--el-color-warning);
}

.strength-text.strong {
  color: var(--el-color-success);
}
</style>