<template>
  <el-card>
    <template #header>
      <div class="card-header">
        <span>个人信息</span>
        <div class="header-actions">
          <el-button v-if="!editing" type="primary" @click="startEdit">编辑</el-button>
          <template v-else>
            <el-button :disabled="loading" @click="cancelEdit">取消</el-button>
            <el-button type="primary" :loading="loading" @click="saveEdit">保存</el-button>
          </template>
        </div>
      </div>
    </template>

    <el-form :model="form" label-width="90px" style="max-width: 520px;">
      <el-form-item label="用户名">
        <el-input v-model="form.username" disabled />
      </el-form-item>
      <el-form-item label="姓名">
        <el-input v-model="form.name" :disabled="!editing" />
      </el-form-item>
      <el-form-item label="手机号">
        <el-input v-model="form.phone" :disabled="!editing" />
      </el-form-item>
      <el-form-item label="邮箱">
        <el-input v-model="form.email" :disabled="!editing" />
      </el-form-item>
      <el-form-item label="头像URL">
        <el-input v-model="form.avatar" :disabled="!editing" />
      </el-form-item>
    </el-form>
  </el-card>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const loading = ref(false)
const editing = ref(false)

const form = reactive({
  username: '',
  name: '',
  phone: '',
  email: '',
  avatar: ''
})

const original = reactive({
  username: '',
  name: '',
  phone: '',
  email: '',
  avatar: ''
})

const syncForm = (data) => {
  form.username = data?.username || ''
  form.name = data?.name || ''
  form.phone = data?.phone || ''
  form.email = data?.email || ''
  form.avatar = data?.avatar || ''

  original.username = form.username
  original.name = form.name
  original.phone = form.phone
  original.email = form.email
  original.avatar = form.avatar
}

const fetchProfile = async () => {
  loading.value = true
  try {
    const res = await request.get('/user/me')
    userStore.setUserInfo(res.data)
    syncForm(res.data)
  } catch (e) {
    console.error('获取个人信息失败:', e)
    ElMessage.error('获取个人信息失败')
  } finally {
    loading.value = false
  }
}

const startEdit = () => {
  editing.value = true
}

const cancelEdit = () => {
  form.username = original.username
  form.name = original.name
  form.phone = original.phone
  form.email = original.email
  form.avatar = original.avatar
  editing.value = false
}

const saveEdit = async () => {
  loading.value = true
  try {
    const res = await request.put('/user/me', {
      name: form.name,
      phone: form.phone,
      email: form.email,
      avatar: form.avatar
    })
    userStore.setUserInfo(res.data)
    syncForm(res.data)
    ElMessage.success('保存成功')
    editing.value = false
  } catch (e) {
    console.error('保存失败:', e)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchProfile()
})
</script>

<style scoped>
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.header-actions {
  display: flex;
  gap: 8px;
}
</style>
