<template>
  <div class="exam-room">
    <div v-if="loading" class="loading-state">
      <el-skeleton :rows="10" animated />
    </div>
    <div v-else-if="examData" class="exam-container">
      <el-header class="exam-header">
        <div class="exam-title">{{ examData.examName }}</div>
        <div class="exam-timer">
          <el-icon>
            <Timer />
          </el-icon>
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
              <div v-for="(q, index) in examData.paper.questions" :key="q.id" class="q-dot"
                :class="{ 'q-done': answers[q.id] }" @click="scrollTo(q.id)">
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
              <!-- 单选题 -->
              <el-radio-group v-if="q.typeId === 1" v-model="answers[q.id]">
                <el-radio v-for="opt in q.options" :key="opt" :value="opt.substring(0, 1)" class="opt-item">
                  {{ opt }}
                </el-radio>
              </el-radio-group>

              <!-- 多选题 -->
              <el-checkbox-group v-else-if="q.typeId === 2" v-model="answers[q.id]">
                <el-checkbox v-for="opt in q.options" :key="opt" :label="opt.substring(0, 1)" class="opt-item">
                  {{ opt }}
                </el-checkbox>
              </el-checkbox-group>

              <!-- 填空题 -->
              <div v-else-if="q.typeId === 3" class="fill-blank-options">
                <el-input v-model="answers[q.id]" type="textarea" :rows="3" placeholder="请输入答案" />
              </div>

              <!-- 主观题 -->
              <div v-else-if="q.typeId === 4" class="subjective-options">
                <el-input v-model="answers[q.id]" type="textarea" :rows="5" placeholder="请输入答案" />
              </div>
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
const cheatCount = ref(0)
const maxCheatCount = 3
let timer = null

const handleVisibilityChange = () => {
  if (document.visibilityState === 'hidden') {
    cheatCount.value++
    recordCheat()

    if (cheatCount.value >= maxCheatCount) {
      ElMessageBox.alert('您已切屏超过 3 次，考试被强制提交！', '作弊警告', {
        confirmButtonText: '确定',
        type: 'error',
        callback: () => {
          submit(true)
        }
      })
    } else {
      ElMessage.warning(`警告：切屏记录一次！切屏超过 3 次将自动交卷。当前切屏次数：${cheatCount.value}`)
    }
  }
}

const recordCheat = async () => {
  try {
    await request.post('/student/exam/cheat', {
      examId: Number(examId),
      cheatCount: cheatCount.value
    })
  } catch (e) {
    console.error('记录切屏失败', e)
  }
}

const preventCopy = (e) => {
  e.preventDefault()
  ElMessage.warning('考试期间禁止复制内容')
}

const preventPaste = (e) => {
  e.preventDefault()
  ElMessage.warning('考试期间禁止粘贴内容')
}

const preventContextMenu = (e) => {
  e.preventDefault()
}

const enterFullscreen = () => {
  const element = document.documentElement
  if (element.requestFullscreen) {
    element.requestFullscreen()
  } else if (element.webkitRequestFullscreen) {
    element.webkitRequestFullscreen()
  } else if (element.msRequestFullscreen) {
    element.msRequestFullscreen()
  }
}

const fetchExam = async () => {
  try {
    const res = await request.get(`/exams/${examId}/enter`)
    examData.value = res.data
    remainingSeconds.value = res.data.remainingSeconds

    // 初始化答案
    examData.value.paper.questions.forEach(q => {
      if (q.typeId === 2) { // 多选题
        answers[q.id] = []
      } else { // 单选、填空、主观题
        answers[q.id] = ''
      }
    })
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
  submit(true)
}

const submit = async (isForce = false) => {
  try {
    const submitData = {
      answers: examData.value.paper.questions.map(q => {
        let userAnswer = answers[q.id]
        if (q.typeId === 2) { // 多选题答案需要转换为字符串
          userAnswer = userAnswer ? userAnswer.sort().join('') : ''
        }
        return {
          questionId: q.id,
          answer: userAnswer || '',
          timeSpent: 0 // 暂不统计单题耗时
        }
      }),
      cheatCount: cheatCount.value,
      forceSubmit: isForce
    }
    await request.post(`/student/exam/submit`, submitData)
    ElMessage.success('提交成功！')

    // 退出全屏
    if (document.fullscreenElement) {
      document.exitFullscreen()
    }

    router.push('/my-exams') // 跳转到我的考试列表
  } catch (e) {
    console.error(e)
  }
}

onMounted(() => {
  fetchExam()

  // 防作弊监听
  document.addEventListener('visibilitychange', handleVisibilityChange)
  document.addEventListener('copy', preventCopy)
  document.addEventListener('paste', preventPaste)
  document.addEventListener('contextmenu', preventContextMenu)

  // 提醒全屏
  ElMessageBox.confirm('为了考试公平，建议进入全屏模式。考试期间切屏将被记录。', '考试须知', {
    confirmButtonText: '进入全屏',
    cancelButtonText: '取消',
    type: 'info'
  }).then(() => {
    enterFullscreen()
  }).catch(() => { })
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
  document.removeEventListener('visibilitychange', handleVisibilityChange)
  document.removeEventListener('copy', preventCopy)
  document.removeEventListener('paste', preventPaste)
  document.removeEventListener('contextmenu', preventContextMenu)
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

.fill-blank-options,
.subjective-options {
  padding-left: 30px;
  width: 80%;
}
</style>
