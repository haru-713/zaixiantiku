<template>
  <div class="profile-container">
    <el-row :gutter="20">
      <!-- 左侧卡片：头像和基本信息 -->
      <el-col :xs="24" :md="8">
        <el-card class="profile-card">
          <div class="user-profile">
            <el-avatar :size="100" icon="UserFilled" class="profile-avatar" />
            <h2 class="user-name">{{ userInfo.name || '未设置姓名' }}</h2>
            <p class="user-role">
              <el-tag v-for="role in userInfo.roles" :key="role" size="small" effect="plain" class="role-tag">
                {{ role }}
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
            <div class="meta-item">
              <el-icon><Calendar /></el-icon>
              <span>注册于 {{ formatDate(userInfo.createTime) }}</span>
            </div>
            <div class="meta-item">
              <el-icon><Phone /></el-icon>
              <span>{{ userInfo.phone || '未绑定手机' }}</span>
            </div>
            <div class="meta-item">
              <el-icon><Message /></el-icon>
              <span>{{ userInfo.email || '未绑定邮箱' }}</span>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 右侧卡片：详细资料编辑 -->
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
            
            <el-divider content-position="left">账号安全</el-divider>
            <el-form-item label="密码管理">
              <el-button type="warning" plain @click="openPasswordDialog">修改登录密码</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>

    <!-- 修改密码对话框 -->
    <el-dialog v-model="passwordDialogVisible" title="修改密码" width="420px">
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
import { ref, reactive, computed, onMounted } from 'vue'
import { useUserStore } from '@/store/user'
import { useRouter } from 'vue-router'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()
const router = useRouter()
const userInfo = computed(() => userStore.userInfo || {})
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

const openPasswordDialog = () => {
  passwordForm.oldPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
  passwordDialogVisible.value = true
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
    userStore.logout()
    router.push('/login')
  } catch (e) {
    console.error('修改密码失败:', e)
  } finally {
    passwordLoading.value = false
  }
}

onMounted(() => {
  form.name = userInfo.value.name || ''
  form.phone = userInfo.value.phone || ''
  form.email = userInfo.value.email || ''
})
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
  box-shadow: 0 2px 12px rgba(0,0,0,0.1);
  margin-bottom: 16px;
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
  min-height: 500px;
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
</style>