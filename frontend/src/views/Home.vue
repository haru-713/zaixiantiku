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
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useUserStore } from '@/store/user'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()
const userInfo = ref({})
const loading = ref(true)

const fetchUserInfo = async () => {
  try {
    const response = await request.get('/user/me')
    userInfo.value = response.data
    // 同步更新 store 中的用户信息
    userStore.setUserInfo(response.data)
  } catch (error) {
    console.error('获取用户信息失败:', error)
    ElMessage.error('获取用户信息失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchUserInfo()
})
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
