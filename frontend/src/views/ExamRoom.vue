<template>
  <div class="exam-room">
    <div v-if="loading" class="loading-state">
      <el-skeleton :rows="10" animated />
    </div>
    <div v-else-if="examData" class="exam-container">
      <el-header class="exam-header">
        <div class="exam-title">{{ examData.examName }}</div>
        <div class="exam-timer">
          <el-icon><Timer /></el-icon>
          <span :class="{ 'timer-urgent': remainingSeconds < 300 }">
            剩余时间: {{ formatTime(remainingSeconds) }}
          </span>
        </div>
        <div class="exam-actions">
          <el-button type="success" @click="confirmSubmit">提交试卷</el-button>
        </div>
      </el-header>

      <el-container class="exam-body">
        <el-aside width="280px" class="exam-nav">
          <el-card shadow="never">
            <template #header>答题卡</template>
            <div class="answer-card">
              <div v-for="(q, index) in examData.paper.questions" :key="q.id" 
                class="q-dot" :class="{ 'q-done': answers[q.id] }" @click="scrollTo(q.id)">
                {{ index + 1 }}
              </div>
            </div>
          </el-card>
        </el-aside>

        <el-main class="exam-content">
          <div v-for="(q, index) in examData.paper.questions" :key="q.id" :id="'q-' + q.id" class="question-item">
            <div class="q-header">
              <span class="q-index">{{ index + 1 }}.</span>
              <span class="q-score">({{ q.score }}分)</span>
              <div class="q-text" v-html="q.content"></div>
            </div>
            <div class="q-options">
              <el-radio-group v-model="answers[q.id]">
                <el-radio v-for="opt in q.options" :key="opt" :label="opt.substring(0, 1)" class="opt-item">
                  {{ opt }}
                </el-radio>
              </el-radio-group>
            </div>
          </div>
        </el-main>
      </el-container>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import request from '@/utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Timer } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const examId = route.params.examId
const loading = ref(true)
const examData = ref(null)
const remainingSeconds = ref(0)
const answers = reactive({})
let timer = null

const fetchExam = async () => {
  try {
    const res = await request.get(`/exams/${examId}/enter`)
    examData.value = res.data
    remainingSeconds.value = res.data.remainingSeconds
    startTimer()
  } catch (e) {
    console.error(e)
    router.back()
  } finally {
    loading.value = false
  }
}

const startTimer = () => {
  timer = setInterval(() => {
    if (remainingSeconds.value > 0) {
      remainingSeconds.value--
    } else {
      clearInterval(timer)
      autoSubmit()
    }
  }, 1000)
}

const formatTime = (seconds) => {
  const h = Math.floor(seconds / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  const s = seconds % 60
  return `${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`
}

const scrollTo = (id) => {
  const el = document.getElementById('q-' + id)
  if (el) el.scrollIntoView({ behavior: 'smooth' })
}

const confirmSubmit = () => {
  const doneCount = Object.keys(answers).length
  const totalCount = examData.value.paper.questions.length
  
  ElMessageBox.confirm(
    `您已完成 ${doneCount}/${totalCount} 题，确定提交试卷吗？`,
    '提示',
    { type: 'warning' }
  ).then(() => {
    submit()
  })
}

const autoSubmit = () => {
  ElMessage.warning('考试时间已到，系统正在为您自动交卷...')
  submit()
}

const submit = async () => {
  try {
    const submitData = {
      answers: examData.value.paper.questions.map(q => ({
        questionId: q.id,
        answer: answers[q.id] || '',
        timeSpent: 0 // 暂不统计单题耗时
      }))
    }
    await request.post(`/exams/${examId}/submit`, submitData)
    ElMessage.success('提交成功！')
    router.push('/study/record')
  } catch (e) {
    console.error(e)
  }
}

onMounted(() => {
  fetchExam()
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.exam-room {
  height: 100vh;
  background: #f5f7fa;
  display: flex;
  flex-direction: column;
}
.exam-header {
  background: #fff;
  border-bottom: 1px solid #dcdfe6;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 40px;
  height: 60px;
}
.exam-title {
  font-size: 20px;
  font-weight: bold;
}
.exam-timer {
  font-size: 18px;
  display: flex;
  align-items: center;
  gap: 8px;
}
.timer-urgent {
  color: #f56c6c;
  font-weight: bold;
}
.exam-body {
  flex: 1;
  overflow: hidden;
  padding: 20px 40px;
  gap: 20px;
}
.exam-nav {
  height: fit-content;
}
.answer-card {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
.q-dot {
  width: 32px;
  height: 32px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-size: 14px;
}
.q-dot:hover {
  border-color: #409eff;
  color: #409eff;
}
.q-done {
  background: #409eff;
  color: #fff;
  border-color: #409eff;
}
.exam-content {
  background: #fff;
  border-radius: 4px;
  padding: 30px;
  overflow-y: auto;
}
.question-item {
  margin-bottom: 40px;
  padding-bottom: 20px;
  border-bottom: 1px dashed #ebeef5;
}
.q-header {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin-bottom: 20px;
}
.q-index {
  font-weight: bold;
}
.q-score {
  color: #909399;
  font-size: 14px;
}
.q-text {
  font-size: 16px;
  line-height: 1.6;
}
.q-options {
  padding-left: 30px;
}
.opt-item {
  display: block;
  margin-bottom: 15px;
  white-space: normal;
  height: auto;
}
</style>
