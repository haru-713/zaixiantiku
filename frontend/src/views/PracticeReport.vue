<template>
  <div class="practice-report-page">
    <div class="page-header">
      <el-button @click="goBack" icon="ArrowLeft">返回</el-button>
      <span class="page-title">练习报告详情</span>
    </div>

    <el-card v-if="report" class="report-header">
      <el-descriptions title="练习概览" :column="3" border>
        <el-descriptions-item label="练习时间">
          <span v-if="report.record.submitTime">{{ formatDateTime(report.record.submitTime) }}</span>
          <el-tag v-else type="info" size="small">未提交</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="得分">
          <span v-if="report.record.submitTime !== null" class="score-text">{{ report.record.totalScore }}</span>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item label="总耗时">{{ formatDuration(report.record.totalDuration) }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <div v-if="report" class="details-list">
      <el-card v-for="(detail, index) in report.details" :key="index" class="question-card">
        <div class="question-title">
          <el-tag :type="detail.isCorrect ? 'success' : 'danger'" class="status-tag">
            {{ detail.isCorrect ? '正确' : '错误' }}
          </el-tag>
          <span>第 {{ index + 1 }} 题 ({{ getTypeName(detail.question.typeId) }})</span>
        </div>
        
        <div class="question-content" v-html="detail.question.content"></div>
        
        <div v-if="detail.question.options && detail.question.options.length" class="options-list">
          <div v-for="(opt, optIdx) in detail.question.options" :key="optIdx" class="option-item">
            {{ String.fromCharCode(65 + optIdx) }}. {{ opt }}
          </div>
        </div>

        <div class="answer-section">
          <p><strong>您的答案：</strong> <span :class="{ 'wrong-answer': !detail.isCorrect }">{{ detail.userAnswer || '未作答' }}</span></p>
          <p><strong>标准答案：</strong> <span class="correct-answer">{{ detail.question.answer }}</span></p>
          <p v-if="detail.question.analysis"><strong>解析：</strong> {{ detail.question.analysis }}</p>
        </div>
      </el-card>
    </div>

    <div v-if="!report && !loading" class="empty-state">
      <el-empty description="未找到报告数据" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import request from '@/utils/request'
import { ArrowLeft } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const report = ref(null)

const goBack = () => {
  router.back()
}

const fetchReport = async () => {
  loading.value = true
  try {
    const res = await request.get(`/student/practice-report/${route.params.id}`)
    if (res.code === 1) {
      report.value = res.data
    }
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const getTypeName = (typeId) => {
  const types = { 1: '单选题', 2: '多选题', 3: '判断题', 4: '填空题', 5: '简答题' }
  return types[typeId] || '未知题型'
}

const formatDateTime = (value) => {
  if (!value) return '-'
  return value.replace('T', ' ').substring(0, 19)
}

const formatDuration = (seconds) => {
  if (!seconds) return '0秒'
  const m = Math.floor(seconds / 60)
  const s = seconds % 60
  return m > 0 ? `${m}分${s}秒` : `${s}秒`
}

onMounted(() => {
  fetchReport()
})
</script>

<style scoped>
.practice-report-page {
  padding: 20px;
  max-width: 1000px;
  margin: 0 auto;
}
.page-header {
  margin-bottom: 20px;
  display: flex;
  align-items: center;
}
.page-title {
  margin-left: 20px;
  font-size: 20px;
  font-weight: bold;
}
.report-header {
  margin-bottom: 20px;
}
.question-card {
  margin-bottom: 20px;
}
.question-title {
  font-weight: bold;
  margin-bottom: 15px;
  display: flex;
  align-items: center;
}
.status-tag {
  margin-right: 10px;
}
.question-content {
  margin-bottom: 15px;
  line-height: 1.6;
}
.options-list {
  margin-bottom: 15px;
}
.option-item {
  margin-bottom: 8px;
}
.answer-section {
  background-color: #f8f9fa;
  padding: 15px;
  border-radius: 4px;
}
.score-text {
  font-size: 18px;
  font-weight: bold;
  color: #f56c6c;
}
.correct-answer {
  color: #67c23a;
  font-weight: bold;
}
.wrong-answer {
  color: #f56c6c;
  font-weight: bold;
}
</style>
