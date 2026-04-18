<template>
  <div class="mistake-book">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>我的错题本</span>
          <div class="header-filters">
            <el-select v-model="filter.courseId" placeholder="课程筛选" clearable style="width: 180px; margin-right: 10px" @change="fetchMistakes">
              <el-option v-for="c in courses" :key="c.id" :label="c.courseName" :value="c.id" />
            </el-select>
            <el-select v-model="filter.typeId" placeholder="题型筛选" clearable style="width: 150px" @change="fetchMistakes">
              <el-option v-for="t in questionTypes" :key="t.id" :label="t.typeName" :value="t.id" />
            </el-select>
          </div>
        </div>
      </template>

      <div v-loading="loading" class="mistake-list">
        <el-empty v-if="mistakes.length === 0" description="暂无错题记录" />
        <div v-else v-for="item in mistakes" :key="item.id" class="mistake-item">
          <div class="m-info">
            <div class="m-meta">
              <el-tag size="small" type="danger">错误次数: {{ item.wrongCount }}</el-tag>
              <span class="m-time">最近错误: {{ formatTime(item.lastWrongTime) }}</span>
            </div>
            <div class="m-question" v-html="item.question.content"></div>
            <div v-if="item.note" class="m-note">
              <strong>我的笔记:</strong> {{ item.note }}
            </div>
          </div>
          <div class="m-actions">
            <el-button type="warning" size="small" plain @click="toggleFavorite(item.question.id)">收藏</el-button>
            <el-button type="primary" size="small" plain @click="redoMistake(item)">重做</el-button>
            <el-button type="info" size="small" plain @click="editNote(item)">笔记</el-button>
            <el-button type="danger" size="small" plain @click="removeMistake(item)">移除</el-button>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 重做对话框 -->
    <el-dialog v-model="redoVisible" title="重做题目" width="700px">
      <div v-if="redoData" class="redo-content">
        <div class="q-content" v-html="redoData.content"></div>
        
        <div class="q-options">
          <!-- 单选题/判断题 -->
          <el-radio-group v-if="redoMode === 'single'" v-model="redoAnswer">
            <el-radio v-for="opt in redoData.options" :key="opt" :label="opt.substring(0, 1)" class="opt-item">
              {{ opt }}
            </el-radio>
          </el-radio-group>
          
          <!-- 多选题 -->
          <el-checkbox-group v-else-if="redoMode === 'multiple'" v-model="redoMultiAnswer" @change="handleRedoMultiChange">
            <el-checkbox v-for="opt in redoData.options" :key="opt" :value="opt.substring(0, 1)" class="opt-item">
              {{ opt }}
            </el-checkbox>
          </el-checkbox-group>
          
          <!-- 填空题 -->
          <el-input v-else v-model="redoAnswer" placeholder="请输入您的答案" style="width: 100%" />
        </div>

        <div v-if="showRedoResult" class="redo-result" :class="{ 'res-correct': isRedoCorrect, 'res-wrong': !isRedoCorrect }">
          <div class="res-title">{{ isRedoCorrect ? '恭喜，回答正确！' : '回答错误' }}</div>
          <div class="res-ans">正确答案：{{ redoData.answer }}</div>
          <div v-if="redoData.analysis" class="res-analysis">解析：{{ redoData.analysis }}</div>
        </div>
      </div>
      <template #footer>
        <el-button v-if="!showRedoResult" type="primary" @click="checkRedo">提交答案</el-button>
        <el-button v-else @click="redoVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 笔记对话框 -->
    <el-dialog v-model="noteVisible" title="错题笔记" width="500px">
      <el-input v-model="noteContent" type="textarea" :rows="4" placeholder="输入您的笔记内容..." />
      <template #footer>
        <el-button @click="noteVisible = false">取消</el-button>
        <el-button type="primary" @click="saveNote">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import request from '@/utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute()
const loading = ref(false)
const mistakes = ref([])
const courses = ref([])
const questionTypes = ref([])
const filter = reactive({
  courseId: route.query.courseId ? Number(route.query.courseId) : null,
  typeId: null
})

const redoVisible = ref(false)
const redoData = ref(null)
const redoAnswer = ref('')
const redoMultiAnswer = ref([]) // 用于多选题
const showRedoResult = ref(false)
const isRedoCorrect = ref(false)

const handleRedoMultiChange = (val) => {
  redoAnswer.value = val.sort().join(',')
}

const redoMode = computed(() => {
  if (!redoData.value) return 'single'
  const t = questionTypes.value.find(x => x.id === redoData.value.typeId)
  const name = String(t?.typeName || '')
  if (name.includes('填空')) return 'blank'
  if (name.includes('多选')) return 'multiple'
  return 'single'
})

const noteVisible = ref(false)
const noteContent = ref('')
const currentMistake = ref(null)

const fetchMistakes = async () => {
  loading.value = true
  try {
    const res = await request.get('/student/mistake-book', { params: filter })
    mistakes.value = res.data
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

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

const formatTime = (time) => {
  if (!time) return '-'
  return time.replace('T', ' ').substring(0, 19)
}

const redoMistake = async (item) => {
  currentMistake.value = item
  try {
    const res = await request.post(`/student/mistake-book/${item.id}/redo`)
    redoData.value = res.data.questions[0]
    redoAnswer.value = ''
    redoMultiAnswer.value = []
    showRedoResult.value = false
    redoVisible.value = true
  } catch (e) {
    console.error(e)
  }
}

const checkRedo = async () => {
  if (!redoAnswer.value) return ElMessage.warning('请填写或选择答案')
  
  try {
    const res = await request.post(`/student/mistake-book/${currentMistake.value.id}/submit`, {
      answer: redoAnswer.value
    })
    
    isRedoCorrect.value = res.data
    showRedoResult.value = true
    
    if (isRedoCorrect.value) {
      ElMessage.success('回答正确，已自动移出错题本')
      fetchMistakes()
    } else {
      ElMessage.error('回答错误，错误次数已累加')
      fetchMistakes() // 刷新列表以更新错误次数
    }
  } catch (e) {
    console.error('提交重做失败:', e)
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

const editNote = (item) => {
  currentMistake.value = item
  noteContent.value = item.note || ''
  noteVisible.value = true
}

const saveNote = async () => {
  try {
    await request.put(`/student/mistake-book/${currentMistake.value.id}/note`, { note: noteContent.value })
    ElMessage.success('笔记保存成功')
    noteVisible.value = false
    fetchMistakes()
  } catch (e) {
    console.error(e)
  }
}

const removeMistake = (item) => {
  ElMessageBox.confirm('确定将该题目从错题本中移除吗？', '提示', {
    type: 'warning'
  }).then(async () => {
    try {
      await request.delete(`/student/mistake-book/${item.id}`)
      ElMessage.success('移除成功')
      fetchMistakes()
    } catch (e) {
      console.error(e)
    }
  })
}

onMounted(() => {
  fetchInitData()
  fetchMistakes()
})
</script>

<style scoped>
.mistake-book {
  padding: 20px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.mistake-item {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 20px;
  border-bottom: 1px solid #ebeef5;
}
.mistake-item:last-child {
  border-bottom: none;
}
.m-info {
  flex: 1;
}
.m-meta {
  margin-bottom: 10px;
  display: flex;
  align-items: center;
  gap: 15px;
}
.m-time {
  font-size: 13px;
  color: #909399;
}
.m-question {
  font-size: 16px;
  line-height: 1.6;
  margin-bottom: 15px;
}
.m-note {
  background: #fdf6ec;
  padding: 10px;
  border-left: 4px solid #e6a23c;
  font-size: 14px;
  color: #606266;
}
.m-actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-left: 20px;
}
.redo-content {
  padding: 10px;
}
.q-options {
  margin: 20px 0;
}
.opt-item {
  display: block;
  margin-bottom: 15px;
  white-space: normal;
  height: auto;
}
.redo-result {
  margin-top: 20px;
  padding: 15px;
  border-radius: 4px;
}
.res-correct {
  background: #f0f9eb;
  border: 1px solid #c2e7b0;
}
.res-wrong {
  background: #fef0f0;
  border: 1px solid #fbc4c4;
}
.res-title {
  font-weight: bold;
  margin-bottom: 10px;
}
.res-ans {
  margin-bottom: 10px;
  color: #67c23a;
  font-weight: bold;
}
.res-analysis {
  font-size: 14px;
  color: #606266;
  border-top: 1px dashed #dcdfe6;
  padding-top: 10px;
}
</style>
