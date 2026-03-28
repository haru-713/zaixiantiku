<template>
  <div class="home-container">
    <h1>欢迎来到在线提库系统</h1>
    <div v-if="userStore.userInfo.name">
      <p>你好, {{ userStore.userInfo.name }} ({{ userStore.userInfo.username }})</p>
      <p>你的角色: {{ userStore.userInfo.roles.join(', ') }}</p>
      <el-button type="danger" @click="handleLogout">退出登录</el-button>
    </div>
  </div>
</template>

<script setup>
import { useUserStore } from '@/store/user'
import { useRouter } from 'vue-router'

const userStore = useUserStore()
const router = useRouter()

const handleLogout = () => {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.home-container {
  padding: 20px;
  text-align: center;
}
</style>
