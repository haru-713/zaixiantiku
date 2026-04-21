<template>
  <div class="exam-report-detail-page">
    <div class="page-header">
      <div class="header-left">
        <el-button type="primary" :icon="ArrowLeft" @click="goBack">返回</el-button>
        <h2 class="title">{{ detail.examName }} - 考试报告</h2>
      </div>
    </div>

    <div v-if="!detail.recordId" class="empty-state">
      <el-empty description="未找到考试报告详情" />
    </div>

    <template v-else>
      <!-- 考试概览卡片 -->
      <el-card shadow="hover" class="overview-card">
        <template #header>
          <div class="card-header">
            <span>考试概览</span>
          </div>
        </template>
        <el-row :gutter="20">
          <el-col :span="6">
            <div class="overview-item">
              <div class="label">课程名称</div>
              <div class="value">{{ detail.courseName }}</div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="overview-item">
              <div class="label">提交时间</div>
              <div class="value">{{ formatDate(detail.submitTime) }}</div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="overview-item">
              <div class="label">学生得分</div>
              <div class="value score">{{ detail.totalScore }} / {{ detail.maxScore }}</div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="overview-item">
              <div class="label">得分率</div>
              <div class="value rate">{{ (detail.scoreRate * 100).toFixed(1) }}%</div>
            </div>
          </el-col>
        </el-row>
      </el-card>

      <!-- 知识点掌握情况雷达图 -->
      <el-card shadow="hover" class="chart-card" style="margin-top: 20px;">
        <template #header><span>本次考试知识点掌握情况</span></template>
        <v-chart class="chart" :option="knowledgeRadarOption" autoresize />
      </el-card>

      <!-- 题目详情 -->
      <el-card shadow="hover" class="question-detail-card" style="margin-top: 20px;">
        <template #header>
          <div class="card-header">
            <span>题目详情</span>
            <el-tag type="info">共 {{ detail.questionDetails.length }} 题</el-tag>
          </div>
        </template>
        <div v-for="(q, index) in detail.questionDetails" :key="q.questionId" class="question-item">
          <div class="question-header">
            <span class="question-num">{{ index + 1 }}.</span>
            <span class="question-content" v-html="q.questionContent"></span>
            <el-tag :type="q.isCorrect ? 'success' : 'danger'" size="small" class="result-tag">
              {{ q.isCorrect ? '正确' : '错误' }}
            </el-tag>
            <span class="question-score">({{ q.userScore }} / {{ q.score }} 分)</span>
          </div>
          <div class="question-options" v-if="q.options && q.options.length > 0">
            <div v-for="(option, oIndex) in q.options" :key="oIndex" class="option-item">
              {{ String.fromCharCode(65 + oIndex) }}. <span v-html="option"></span>
            </div>
          </div>
          <div class="answer-info">
            <p><strong>你的答案:</strong> <span :class="{'wrong-answer': !q.isCorrect}">{{ q.userAnswer || '未作答' }}</span></p>
            <p><strong>正确答案:</strong> <span class="correct-answer">{{ q.correctAnswer }}</span></p>
            <p v-if="q.analysis"><strong>解析:</strong> <span v-html="q.analysis"></span></p>
          </div>
        </div>
      </el-card>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { RadarChart } from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent
} from 'echarts/components'
import VChart from 'vue-echarts'

use([
  CanvasRenderer,
  RadarChart,
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent
])

const route = useRoute()
const router = useRouter()
const recordId = ref(null)
const detail = ref({
  recordId: null,
  examId: null,
  examName: '',
  courseId: null,
  courseName: '',
  submitTime: '',
  totalScore: 0,
  maxScore: 0,
  scoreRate: 0,
  status: 0,
  questionDetails: [],
  knowledgeRadar: []
})

const fetchExamDetail = async () => {
  try {
    const res = await request.get(`/student/analysis/exam-records/${recordId.value}/detail`)
    if (res.code === 1) {
      detail.value = res.data
    } else {
      ElMessage.error(res.msg || '获取考试详情失败')
    }
  } catch (e) {
    console.error(e)
    ElMessage.error('获取考试详情失败')
  }
}

const goBack = () => {
  router.back()
}

const formatDate = (dateString) => {
  if (!dateString) return ''
  const date = new Date(dateString)
  return date.toLocaleString()
}

const knowledgeRadarOption = computed(() => {
  const indicators = detail.value.knowledgeRadar.map(k => ({ name: k.name, max: 100 }))
  return {
    tooltip: { trigger: 'item' },
    radar: {
      indicator: indicators,
      radius: '65%',
      splitArea: { areaStyle: { color: ['#fff', '#f8f9fa'] } }
    },
    series: [{
      type: 'radar',
      data: [{
        value: detail.value.knowledgeRadar.map(k => (k.value * 100).toFixed(1)),
        name: '正确率',
        areaStyle: { color: 'rgba(103, 194, 58, 0.3)' },
        lineStyle: { color: '#67c23a' },
        itemStyle: { color: '#67c23a' }
      }]
    }]
  }
})

onMounted(() => {
  recordId.value = route.params.recordId
  if (recordId.value) {
    fetchExamDetail()
  }
})
</script>

<style scoped>
.exam-report-detail-page {
  padding: 20px;
  background-color: #f5f7fa;
  min-height: calc(100vh - 40px);
}

.page-header {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
  background-color: #fff;
  padding: 15px 20px;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0,0,0,0.05);
}

.page-header .title {
  margin: 0 0 0 10px;
  font-size: 20px;
  color: #303133;
}

.empty-state {
  margin-top: 100px;
}

.overview-card, .chart-card, .question-detail-card {
  border-radius: 8px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: bold;
}

.overview-item {
  text-align: center;
  padding: 10px 0;
}

.overview-item .label {
  font-size: 14px;
  color: #909399;
  margin-bottom: 8px;
}

.overview-item .value {
  font-size: 22px;
  font-weight: bold;
  color: #303133;
}

.overview-item .value.score { color: #409eff; }
.overview-item .value.rate { color: #e6a23c; }

.chart {
  height: 350px;
  width: 100%;
}

.question-item {
  margin-bottom: 20px;
  padding-bottom: 20px;
  border-bottom: 1px dashed #ebeef5;
}

.question-item:last-child {
  border-bottom: none;
  margin-bottom: 0;
  padding-bottom: 0;
}

.question-header {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
}

.question-num {
  font-size: 16px;
  font-weight: bold;
  margin-right: 8px;
  color: #303133;
}

.question-content {
  font-size: 16px;
  color: #303133;
  flex-grow: 1;
}

.result-tag {
  margin-left: 10px;
}

.question-score {
  margin-left: 10px;
  font-size: 14px;
  color: #909399;
}

.question-options {
  margin-left: 25px;
  margin-bottom: 10px;
}

.option-item {
  margin-bottom: 5px;
  color: #606266;
}

.answer-info p {
  margin: 5px 0;
  font-size: 14px;
  color: #606266;
}

.answer-info strong {
  color: #303133;
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
