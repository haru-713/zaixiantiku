<template>
  <el-card>
    <template #header>
      <div class="card-header">
        <span>创建课程</span>
      </div>
    </template>

    <el-form :model="form" label-width="90px" style="max-width: 520px;">
      <el-form-item label="课程名称">
        <el-input v-model="form.courseName" placeholder="请输入课程名称" />
      </el-form-item>
      <el-form-item label="课程描述">
        <el-input v-model="form.description" type="textarea" :rows="4" placeholder="请输入课程描述" />
      </el-form-item>
      <el-form-item label="封面URL">
        <el-input v-model="form.cover" placeholder="http://.../cover.jpg" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="handleSubmit">创建</el-button>
        <el-button :disabled="loading" @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>
  </el-card>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const loading = ref(false)
const form = reactive({
  courseName: '',
  description: '',
  cover: ''
})

const handleReset = () => {
  form.courseName = ''
  form.description = ''
  form.cover = ''
}

const handleSubmit = async () => {
  if (!form.courseName) {
    ElMessage.warning('请输入课程名称')
    return
  }

  loading.value = true
  try {
    const res = await request.post('/courses', {
      courseName: form.courseName,
      description: form.description,
      cover: form.cover
    })
    ElMessage.success(`创建成功：ID=${res.data.id}`)
    handleReset()
  } catch (e) {
    console.error('创建课程失败:', e)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.card-header {
  font-weight: bold;
}
</style>
