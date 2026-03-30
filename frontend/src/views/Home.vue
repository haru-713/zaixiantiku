<template>
  <div class="home-container">
    <h1>欢迎来到在线题库系统</h1>
    <div v-if="loading">加载中...</div>
    <div v-else-if="userInfo.name">
      <el-card class="user-card">
        <template #header>
          <div class="card-header">
            <span>我的个人信息</span>
          </div>
        </template>
        <div class="user-info">
          <p><strong>用户名:</strong> {{ userInfo.username }}</p>
          <p><strong>姓名:</strong> {{ userInfo.name }}</p>
          <p><strong>手机号:</strong> {{ userInfo.phone || '未填写' }}</p>
          <p><strong>邮箱:</strong> {{ userInfo.email || '未填写' }}</p>
          <p><strong>角色:</strong> {{ userInfo.roles ? userInfo.roles.join(', ') : '' }}</p>
          <p><strong>审核状态:</strong>
            <el-tag :type="userInfo.auditStatus === 1 ? 'success' : 'warning'">
              {{ userInfo.auditStatus === 1 ? '已通过' : '待审核' }}
            </el-tag>
          </p>
        </div>
        <div class="action-btns" style="margin-top: 20px;">
          <el-button v-if="userInfo.roles && userInfo.roles.includes('ADMIN')" type="success" @click="router.push('/admin/users')">用户管理</el-button>
          <el-button v-if="userInfo.roles && (userInfo.roles.includes('ADMIN') || userInfo.roles.includes('TEACHER'))" type="success" @click="courseDialogVisible = true">创建课程</el-button>
          <el-button type="primary" @click="editDialogVisible = true">修改信息</el-button>
          <el-button type="warning" @click="passwordDialogVisible = true">修改密码</el-button>
          <el-button type="danger" @click="handleLogout">退出登录</el-button>
        </div>
      </el-card>
    </div>

    <el-dialog v-model="courseDialogVisible" title="创建课程" width="450px">
      <el-form :model="courseForm" label-width="90px">
        <el-form-item label="课程名称">
          <el-input v-model="courseForm.courseName" placeholder="请输入课程名称"></el-input>
        </el-form-item>
        <el-form-item label="课程描述">
          <el-input v-model="courseForm.description" type="textarea" :rows="3" placeholder="请输入课程描述"></el-input>
        </el-form-item>
        <el-form-item label="封面URL">
          <el-input v-model="courseForm.cover" placeholder="http://.../cover.jpg"></el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="courseDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleCreateCourse" :loading="courseLoading">创建</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 修改信息对话框 -->
    <el-dialog v-model="editDialogVisible" title="修改个人信息" width="400px">
      <el-form :model="editForm" label-width="80px">
        <el-form-item label="姓名">
          <el-input v-model="editForm.name"></el-input>
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="editForm.phone"></el-input>
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="editForm.email"></el-input>
        </el-form-item>
        <el-form-item label="头像URL">
          <el-input v-model="editForm.avatar"></el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="editDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleUpdate" :loading="updateLoading">保存</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 修改密码对话框 -->
    <el-dialog v-model="passwordDialogVisible" title="修改密码" width="400px">
      <el-form :model="passwordForm" label-width="100px">
        <el-form-item label="旧密码">
          <el-input v-model="passwordForm.oldPassword" type="password" show-password></el-input>
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="passwordForm.newPassword" type="password" show-password></el-input>
        </el-form-item>
        <el-form-item label="确认新密码">
          <el-input v-model="passwordForm.confirmPassword" type="password" show-password></el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="passwordDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handlePasswordUpdate" :loading="passwordLoading">修改</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, reactive } from 'vue'
import { useUserStore } from '@/store/user'
import { useRouter } from 'vue-router'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()
const router = useRouter()
const userInfo = ref({})
const loading = ref(true)

const editDialogVisible = ref(false)
const updateLoading = ref(false)
const editForm = reactive({
  name: '',
  phone: '',
  email: '',
  avatar: ''
})

const passwordDialogVisible = ref(false)
const passwordLoading = ref(false)
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const courseDialogVisible = ref(false)
const courseLoading = ref(false)
const courseForm = reactive({
  courseName: '',
  description: '',
  cover: ''
})

const fetchUserInfo = async () => {
  try {
    const response = await request.get('/user/me')
    userInfo.value = response.data
    // 初始化编辑表单
    editForm.name = userInfo.value.name
    editForm.phone = userInfo.value.phone
    editForm.email = userInfo.value.email
    editForm.avatar = userInfo.value.avatar
    // 同步更新 store 中的用户信息
    userStore.setUserInfo(response.data)
  } catch (error) {
    console.error('获取用户信息失败:', error)
    ElMessage.error('获取用户信息失败')
  } finally {
    loading.value = false
  }
}

const handleUpdate = async () => {
  updateLoading.value = true
  try {
    const response = await request.put('/user/me', editForm)
    userInfo.value = response.data
    userStore.setUserInfo(response.data)
    ElMessage.success('修改成功')
    editDialogVisible.value = false
  } catch (error) {
    console.error('修改用户信息失败:', error)
  } finally {
    updateLoading.value = false
  }
}

const handlePasswordUpdate = async () => {
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
    // 修改密码后退出登录
    handleLogout()
  } catch (error) {
    console.error('修改密码失败:', error)
  } finally {
    passwordLoading.value = false
  }
}

const handleCreateCourse = async () => {
  if (!courseForm.courseName) {
    ElMessage.warning('请输入课程名称')
    return
  }

  courseLoading.value = true
  try {
    const res = await request.post('/courses', {
      courseName: courseForm.courseName,
      description: courseForm.description,
      cover: courseForm.cover
    })
    ElMessage.success(`创建成功：ID=${res.data.id}`)
    courseDialogVisible.value = false
    courseForm.courseName = ''
    courseForm.description = ''
    courseForm.cover = ''
  } catch (e) {
    console.error('创建课程失败:', e)
  } finally {
    courseLoading.value = false
  }
}

onMounted(() => {
  fetchUserInfo()
})

const handleLogout = () => {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.home-container {
  padding: 40px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.user-card {
  width: 500px;
  margin-top: 20px;
}

.card-header {
  font-weight: bold;
}

.user-info p {
  text-align: left;
  margin: 10px 0;
  border-bottom: 1px solid #ebeef5;
  padding-bottom: 10px;
}
</style>
