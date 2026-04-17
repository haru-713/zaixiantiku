<template>
  <div class="exam-detail-page">
    <el-card v-loading="loading">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-button @click="goBack" circle icon="ArrowLeft" />
            <span class="exam-title">{{ data.examName }} - 答卷详情</span>
          </div>
          <div class="total-score" v-if="data.totalScore !== null">
            得分：<span class="score-num">{{ data.totalScore }}</span>
            <span class="max-score" v-if="data.maxScore"> / {{ data.maxScore }}</span>
          </div>
        </div>
      </template>

      <div class="answer-list">
        <div v-for="(item, index) in data.answers" :key="index" class="question-item">
          <div class="q-header">
            <span class="q-index">{{ index + 1 }}.</span>
            <el-tag size="small" type="info">{{ item.typeName }}</el-tag>
            <el-tag v-if="item.isCorrect !== null" size="small" :type="item.isCorrect ? 'success' : 'danger'" class="res-tag">
              {{ item.isCorrect ? '正确' : '错误' }}
            </el-tag>
            <span class="q-score">({{ item.score }}分)</span>
          </div>
          
          <div class="q-content" v-html="item.content"></div>
          
          <div v-if="item.options && item.options.length" class="q-options">
            <div v-for="(opt, oIdx) in item.options" :key="oIdx" class="opt-item">
              {{ opt }}
            </div>
          </div>

          <div class="answer-area">
            <div class="user-ans">
              您的答案：<span :class="{ 'correct': item.isCorrect, 'wrong': item.isCorrect === false }">{{ item.userAnswer || '未作答' }}</span>
            </div>
            <div class="correct-ans">
              标准答案：<span class="correct">{{ item.correctAnswer }}</span>
            </div>
          </div>

          <div v-if="item.analysis" class="q-analysis">
            <div class="analysis-title">解析：</div>
            <div class="analysis-content">{{ item.analysis }}</div>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import request from '@/utils/request'
import { ArrowLeft } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const recordId = route.params.recordId

const loading = ref(false)
const data = ref({
  examName: '',
  totalScore: 0,
  answers: []
})

const fetchDetail = async () => {
  loading.value = true
  try {
    const res = await request.get(`/student/exam-records/${recordId}`)
    data.value = res.data
  } catch (e) {
    console.error('获取详情失败:', e)
  } finally {
    loading.value = false
  }
}

const goBack = () => {
  router.back()
}

onMounted(() => {
  fetchDetail()
})
</script>

<style scoped>
.exam-detail-page {
  padding: 20px;
  max-width: 1000px;
  margin: 0 auto;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.exam-title {
  font-size: 18px;
  font-weight: bold;
}
.total-score {
  font-size: 20px;
  font-weight: bold;
}
.score-num {
  font-size: 24px;
  font-weight: bold;
  color: #67c23a;
}
.max-score {
  font-size: 16px;
  color: #909399;
  margin-left: 2px;
}
.question-item {
  border-bottom: 1px solid #ebeef5;
  padding: 24px 0;
}
.question-item:last-child {
  border-bottom: none;
}
.q-header {
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
}
.q-index {
  font-weight: bold;
  font-size: 16px;
}
.res-tag {
  margin-left: 8px;
}
.q-score {
  color: #909399;
  font-size: 14px;
}
.q-content {
  margin-bottom: 16px;
  line-height: 1.6;
}
.q-options {
  margin-bottom: 16px;
}
.opt-item {
  margin-bottom: 8px;
}
.answer-area {
  background-color: #f8f9fa;
  padding: 12px;
  border-radius: 4px;
  margin-bottom: 12px;
  display: flex;
  gap: 24px;
}
.correct {
  color: #67c23a;
  font-weight: bold;
}
.wrong {
  color: #f56c6c;
  font-weight: bold;
}
.q-analysis {
  background-color: #fdf6ec;
  padding: 12px;
  border-radius: 4px;
  border-left: 4px solid #e6a23c;
}
.analysis-title {
  font-weight: bold;
  color: #e6a23c;
  margin-bottom: 4px;
}
</style>
