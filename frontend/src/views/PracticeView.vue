<template>
  <div class="practice-view">
    <!-- 练习配置界面 -->
    <el-card v-if="!isStarted" class="config-card">
      <template #header>
        <div class="card-header">
          <span>在线练习配置</span>
        </div>
      </template>
      <el-form :model="config" label-width="100px">
        <el-form-item label="选择课程" required>
          <el-select v-model="config.courseId" placeholder="请选择课程" style="width: 100%" @change="handleCourseChange">
            <el-option v-for="item in courses" :key="item.id" :label="item.courseName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="知识点">
          <el-select v-model="config.config.knowledgeIds" placeholder="请选择知识点（可选）" style="width: 100%" multiple collapse-tags clearable>
            <el-option v-for="kp in knowledgePoints" :key="kp.id" :label="kp.name" :value="kp.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="题目类型">
          <el-checkbox-group v-model="config.config.typeIds">
            <el-checkbox v-for="t in questionTypes" :key="t.id" :value="t.id">{{ t.typeName }}</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="难度等级">
          <el-checkbox-group v-model="config.config.difficulties">
            <el-checkbox :value="1">简单</el-checkbox>
            <el-checkbox :value="2">中等</el-checkbox>
            <el-checkbox :value="3">困难</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="题目数量">
          <el-input-number v-model="config.config.questionCount" :min="1" :max="50" @change="handleCountChange" />
          <div v-if="config.config.questionCount >= 50" class="limit-tip">单次练习最多支持 50 道题目</div>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" @click="startPractice" :loading="starting">开始练习</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 练习答题界面 -->
    <div v-else class="practice-container">
      <el-header class="practice-header">
        <div class="header-left">
          <el-button icon="ArrowLeft" circle @click="exitConfirm" />
          <span class="practice-title">在线练习</span>
        </div>
        <div class="header-right">
          <el-button type="success" @click="submitConfirm">提交练习</el-button>
        </div>
      </el-header>

      <el-container class="practice-body">
        <el-aside width="280px" class="practice-nav">
          <el-card shadow="never">
            <template #header>题目导航</template>
            <div class="nav-grid">
              <div v-for="(q, index) in questions" :key="q.id" 
                class="nav-dot" 
                :class="{ 
                  'dot-done': answers[q.id], 
                  'dot-active': currentIndex === index 
                }" 
                @click="handleNav(index)">
                {{ index + 1 }}
              </div>
            </div>
          </el-card>
        </el-aside>

        <el-main class="practice-content">
          <div v-if="currentQuestion" class="question-card">
            <div class="q-header">
              <div class="q-header-left">
                <span class="q-index">第 {{ currentIndex + 1 }} 题 / 共 {{ questions.length }} 题</span>
                <el-tag size="small" type="info">{{ getTypeName(currentQuestion.typeId) }}</el-tag>
              </div>
              <el-button type="warning" icon="Star" circle plain @click="toggleFavorite(currentQuestion.id)" />
            </div>
            <div class="q-text" v-html="currentQuestion.content"></div>
            
            <div class="q-options">
              <!-- 单选题/判断题 -->
              <el-radio-group v-if="currentMode === 'single'" v-model="answers[currentQuestion.id]">
                <template v-if="currentQuestion.options && currentQuestion.options.length">
                  <el-radio v-for="opt in currentQuestion.options" :key="opt" :label="opt.substring(0, 1)" class="opt-item">
                    {{ opt }}
                  </el-radio>
                </template>
                <template v-else-if="getTypeName(currentQuestion.typeId).includes('判断')">
                  <el-radio label="正确" class="opt-item">正确</el-radio>
                  <el-radio label="错误" class="opt-item">错误</el-radio>
                </template>
              </el-radio-group>
              
              <!-- 多选题 -->
              <el-checkbox-group v-else-if="currentMode === 'multiple'" v-model="multiAnswers[currentQuestion.id]" @change="handleMultiChange">
                <el-checkbox v-for="opt in currentQuestion.options" :key="opt" :value="opt.substring(0, 1)" class="opt-item">
                  {{ opt }}
                </el-checkbox>
              </el-checkbox-group>
              
              <!-- 简答题 -->
              <el-input v-else-if="currentMode === 'short'" v-model="answers[currentQuestion.id]" type="textarea" :rows="5" placeholder="请输入您的答案" />
              
              <!-- 填空题 -->
              <el-input v-else v-model="answers[currentQuestion.id]" placeholder="请输入您的答案" />
            </div>

            <div class="q-footer">
              <el-button v-if="currentIndex > 0" @click="handleNav(currentIndex - 1)">上一题</el-button>
              <el-button v-if="currentIndex < questions.length - 1" type="primary" @click="handleNav(currentIndex + 1)">下一题</el-button>
              <el-button v-else type="success" @click="submitConfirm">完成提交</el-button>
            </div>
          </div>
        </el-main>
      </el-container>
    </div>

    <!-- 结果展示对话框 -->
    <el-dialog v-model="resultVisible" title="练习结果" width="600px" :close-on-click-modal="false" :show-close="false">
      <div class="result-body" v-if="resultData">
        <el-result icon="success" title="练习提交成功" :sub-title="'本次得分：' + resultData.totalScore">
          <template #extra>
            <el-table :data="resultData.details" style="width: 100%">
              <el-table-column label="题号" width="60">
                <template #default="scope">{{ scope.$index + 1 }}</template>
              </el-table-column>
              <el-table-column label="结果" width="80">
                <template #default="scope">
                  <el-tag :type="scope.row.isCorrect ? 'success' : 'danger'">
                    {{ scope.row.isCorrect ? '正确' : '错误' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="correctAnswer" label="正确答案" width="100" />
              <el-table-column prop="analysis" label="解析" show-overflow-tooltip />
            </el-table>
          </template>
        </el-result>
      </div>
      <template #footer>
        <el-button @click="resetPractice">返回首页</el-button>
        <el-button type="primary" @click="goToReport">查看详细报告</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, onBeforeRouteLeave } from 'vue-router'
import request from '@/utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Star } from '@element-plus/icons-vue'

const router = useRouter()
const isStarted = ref(false)
const starting = ref(false)
const courses = ref([])
const questionTypes = ref([])
const knowledgePoints = ref([])
const questions = ref([])
const currentIndex = ref(0)
const answers = reactive({})
const multiAnswers = reactive({}) // 用于多选题的数组存储
const practiceId = ref(null)
const startTimeStamp = ref(0)
const questionStartTime = ref(0)
const questionDurations = reactive({})

const goToReport = () => {
  if (practiceId.value) {
    resultVisible.value = false
    router.push(`/study/practice-report/${practiceId.value}`)
    resetPractice()
  }
}

// 状态持久化：保存进度到 sessionStorage
const saveProgress = () => {
  if (isStarted.value) {
    const state = {
      isStarted: isStarted.value,
      questions: questions.value,
      currentIndex: currentIndex.value,
      answers: { ...answers },
      multiAnswers: { ...multiAnswers },
      practiceId: practiceId.value
    }
    sessionStorage.setItem('current_practice', JSON.stringify(state))
  } else {
    sessionStorage.removeItem('current_practice')
  }
}

// 状态恢复
const restoreProgress = () => {
  const saved = sessionStorage.getItem('current_practice')
  if (saved) {
    ElMessageBox.confirm('检测到您有未完成的练习，是否继续？', '提示', {
      confirmButtonText: '继续练习',
      cancelButtonText: '重新开始',
      type: 'info',
      distinguishCancelAndClose: true
    }).then(() => {
      const state = JSON.parse(saved)
      questions.value = state.questions
      currentIndex.value = state.currentIndex
      Object.assign(answers, state.answers)
      Object.assign(multiAnswers, state.multiAnswers)
      practiceId.value = state.practiceId
      isStarted.value = state.isStarted
    }).catch((action) => {
      if (action === 'cancel') {
        sessionStorage.removeItem('current_practice')
      }
    })
  }
}

// 路由守卫：离开确认
onBeforeRouteLeave((to, from, next) => {
  if (isStarted.value && !resultVisible.value) {
    ElMessageBox.confirm('练习正在进行中，离开将丢失当前未提交的进度（虽然我们会为您暂时保存），确定要离开吗？', '提示', {
      type: 'warning'
    }).then(() => {
      saveProgress() // 离开前保存一下
      next()
    }).catch(() => {
      next(false)
    })
  } else {
    next()
  }
})

// 监听题目切换，保存进度
const handleNav = (index) => {
  // 记录当前题目耗时
  if (currentQuestion.value) {
    const now = Date.now()
    const duration = Math.floor((now - questionStartTime.value) / 1000)
    const qId = currentQuestion.value.id
    questionDurations[qId] = (questionDurations[qId] || 0) + duration
    questionStartTime.value = now
  }

  currentIndex.value = index
  const q = questions.value[index]
  if (q && getTypeName(q.typeId).includes('多选') && answers[q.id]) {
    multiAnswers[q.id] = answers[q.id].split(',')
  } else if (q && getTypeName(q.typeId).includes('多选') && !multiAnswers[q.id]) {
    multiAnswers[q.id] = []
  }
  saveProgress()
}

const handleMultiChange = (val) => {
  const qId = currentQuestion.value.id
  answers[qId] = val.sort().join(',')
  saveProgress()
}

const handleCountChange = (val) => {
  if (val >= 50) {
    ElMessage.warning('已达到单次练习题目数量上限（50题）')
  }
}

const config = reactive({
  courseId: null,
  config: {
    typeIds: [],
    difficulties: [],
    knowledgeIds: [],
    questionCount: 10
  }
})

const resultVisible = ref(false)
const resultData = ref(null)

const currentQuestion = computed(() => questions.value[currentIndex.value])

const currentMode = computed(() => {
  if (!currentQuestion.value) return 'single'
  const t = questionTypes.value.find(x => x.id === currentQuestion.value.typeId)
  const name = String(t?.typeName || '')
  if (name.includes('填空')) return 'blank'
  if (name.includes('多选')) return 'multiple'
  if (name.includes('简答') || name.includes('论述') || name.includes('主观')) return 'short'
  return 'single'
})

const fetchInitData = async () => {
  try {
    const [courseRes, typeRes] = await Promise.all([
      request.get('/courses', { params: { page: 1, size: 100 } }),
      request.get('/question-types')
    ])
    courses.value = courseRes.data.list
    questionTypes.value = typeRes.data
  } catch (e) {
    console.error(e)
  }
}

const handleCourseChange = async (courseId) => {
  config.config.knowledgeIds = []
  if (!courseId) {
    knowledgePoints.value = []
    return
  }
  try {
    const res = await request.get('/knowledge-points', { params: { courseId } })
    knowledgePoints.value = res.data
  } catch (e) {
    console.error('获取知识点失败:', e)
  }
}

const getTypeName = (id) => {
  const t = questionTypes.value.find(x => x.id === id)
  return t ? t.typeName : '未知题型'
}

const startPractice = async () => {
  if (!config.courseId) {
    return ElMessage.warning('请先选择课程')
  }
  if (config.config.questionCount > 50) {
    return ElMessage.warning('单次练习题目数量不能超过 50 题')
  }
  if (config.config.questionCount < 1) {
    return ElMessage.warning('题目数量至少为 1 题')
  }
  
  starting.value = true
  try {
    const res = await request.post('/practice/start', config)
    if (!res.data.questions || res.data.questions.length === 0) {
      return ElMessage.warning('未找到符合条件的题目，请修改筛选条件')
    }
    questions.value = res.data.questions
    practiceId.value = res.data.practiceId
    isStarted.value = true
    startTimeStamp.value = Date.now()
    questionStartTime.value = Date.now()
    saveProgress() // 开始后保存初始状态
  } catch (e) {
    console.error(e)
  } finally {
    starting.value = false
  }
}

const toggleFavorite = async (questionId) => {
  try {
    await request.post('/student/favorites', { questionId })
    ElMessage.success('收藏成功')
  } catch (e) {
    console.error(e)
  }
}

const exitConfirm = () => {
  ElMessageBox.confirm('确定要退出本次练习吗？进度将不会被保存。', '提示', {
    type: 'warning'
  }).then(() => {
    isStarted.value = false
    questions.value = []
    Object.keys(answers).forEach(key => delete answers[key])
    Object.keys(multiAnswers).forEach(key => delete multiAnswers[key])
    sessionStorage.removeItem('current_practice')
  })
}

const submitConfirm = () => {
  const doneCount = Object.keys(answers).length
  const totalCount = questions.value.length
  ElMessageBox.confirm(`您已完成 ${doneCount}/${totalCount} 题，确定提交吗？`, '提示', {
    type: 'success'
  }).then(() => {
    submit()
  })
}

const submit = async () => {
  try {
    // 提交前更新最后一道题的耗时
    if (currentQuestion.value) {
      const now = Date.now()
      const duration = Math.floor((now - questionStartTime.value) / 1000)
      const qId = currentQuestion.value.id
      questionDurations[qId] = (questionDurations[qId] || 0) + duration
    }

    const totalDuration = Math.floor((Date.now() - startTimeStamp.value) / 1000)

    const submitData = {
      totalDuration: totalDuration,
      answers: questions.value.map(q => ({
        questionId: q.id,
        answer: answers[q.id] || '',
        timeSpent: questionDurations[q.id] || 0
      }))
    }
    const res = await request.post(`/practice/${practiceId.value}/submit`, submitData)
    resultData.value = res.data
    resultVisible.value = true
    sessionStorage.removeItem('current_practice') // 提交成功后清除
  } catch (e) {
    console.error(e)
  }
}

const resetPractice = () => {
  resultVisible.value = false
  isStarted.value = false
  questions.value = []
  Object.keys(answers).forEach(key => delete answers[key])
  Object.keys(multiAnswers).forEach(key => delete multiAnswers[key])
  currentIndex.value = 0
  sessionStorage.removeItem('current_practice')
}

// 外部刷新/关闭浏览器确认
const handleBeforeUnload = (e) => {
  if (isStarted.value && !resultVisible.value) {
    saveProgress()
    e.preventDefault()
    e.returnValue = ''
  }
}

onMounted(() => {
  fetchInitData()
  restoreProgress()
  window.addEventListener('beforeunload', handleBeforeUnload)
})

onUnmounted(() => {
  window.removeEventListener('beforeunload', handleBeforeUnload)
})
</script>

<style scoped>
.practice-view {
  padding: 20px;
  height: calc(100vh - 120px);
}
.config-card {
  max-width: 600px;
  margin: 40px auto;
}
.limit-tip {
  color: #e6a23c;
  font-size: 12px;
  margin-top: 4px;
  line-height: 1;
}
.practice-container {
  height: 100%;
  display: flex;
  flex-direction: column;
}
.practice-header {
  background: #fff;
  border-bottom: 1px solid #dcdfe6;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  margin-bottom: 20px;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 15px;
}
.practice-title {
  font-size: 18px;
  font-weight: bold;
}
.practice-body {
  flex: 1;
  overflow: hidden;
  gap: 20px;
}
.practice-nav {
  height: 100%;
}
.nav-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
.nav-dot {
  width: 32px;
  height: 32px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}
.nav-dot:hover {
  border-color: #409eff;
  color: #409eff;
}
.dot-done {
  background: #f0f9eb;
  color: #67c23a;
  border-color: #67c23a;
}
.dot-active {
  border-color: #409eff;
  color: #409eff;
  font-weight: bold;
  border-width: 2px;
}
.practice-content {
  background: #fff;
  border-radius: 4px;
  padding: 30px;
  overflow-y: auto;
}
.q-header {
  margin-bottom: 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.q-header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}
.q-index {
  font-weight: bold;
  color: #606266;
}
.q-text {
  font-size: 16px;
  margin-bottom: 30px;
}
.q-options {
  margin-bottom: 40px;
}
.opt-item {
  display: block;
  margin-bottom: 15px;
  white-space: normal;
  height: auto;
}
.q-footer {
  border-top: 1px solid #ebeef5;
  padding-top: 20px;
  display: flex;
  justify-content: center;
  gap: 20px;
}
</style>
