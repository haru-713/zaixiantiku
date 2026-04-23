<template>
  <div class="exam-detail-page">
    <el-card v-loading="loading">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-button @click="goBack" circle icon="ArrowLeft" />
            <span class="exam-title">{{ data.examName }} - 答卷详情</span>
          </div>
          <div class="total-score" v-if="data.status === 2">
            得分：<span class="score-num">{{ data.totalScore }}</span>
            <span class="max-score" v-if="data.maxScore"> / {{ data.maxScore }}</span>
            <el-tag :type="getScoreTagType(data.totalScore / data.maxScore)" class="rate-tag">
              {{ Math.round(data.totalScore / data.maxScore * 100) }}%
            </el-tag>
          </div>
          <div class="total-score" v-else-if="data.status === 1">
            <el-tag type="warning">教师阅卷中</el-tag>
          </div>
        </div>
      </template>

      <!-- 阅卷中状态显示 -->
      <div v-if="data.status === 1" class="pending-marking">
        <el-result icon="warning" title="考试正在批阅中" sub-title="包含主观题的试卷需要教师人工阅卷，请耐心等待最终成绩。">
          <template #extra>
            <el-button type="primary" @click="goBack">返回记录列表</el-button>
          </template>
        </el-result>
      </div>

      <!-- 个人分析部分 -->
      <el-row :gutter="20" class="analysis-section"
        v-if="data.status === 2 && data.knowledgeMastery && data.knowledgeMastery.length">
        <el-col :span="10">
          <el-card shadow="never" class="info-card">
            <div class="info-grid">
              <div class="info-item">
                <div class="info-label">班级排名</div>
                <div class="info-value">第 <span class="rank-num">{{ data.rank }}</span> 名</div>
                <div class="info-total">共 {{ data.totalStudents }} 人参加</div>
              </div>
              <div class="info-item">
                <div class="info-label">本次考试表现</div>
                <el-progress type="circle" :percentage="Math.round(data.totalScore / data.maxScore * 100)"
                  :color="customColors" :width="120" />
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="14">
          <el-card shadow="never" class="radar-card">
            <template #header><span class="analysis-title-text">知识点掌握度分析</span></template>
            <div class="radar-container">
              <v-chart class="radar-chart" :option="radarOption" autoresize />
            </div>
          </el-card>
        </el-col>
      </el-row>

      <div class="answer-list" v-if="data.status === 2">
        <div v-for="(item, index) in data.answers" :key="index" class="question-item">
          <div class="q-header">
            <span class="q-index">{{ index + 1 }}.</span>
            <el-tag size="small" type="info">{{ item.typeName }}</el-tag>
            <el-tag v-if="item.isCorrect !== null" size="small" :type="item.isCorrect ? 'success' : 'danger'"
              class="res-tag">
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
              您的答案：<span :class="{ 'correct': item.isCorrect, 'wrong': item.isCorrect === false }">{{ item.userAnswer ||
                '未作答'
                }}</span>
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
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import request from '@/utils/request'
import { ArrowLeft } from '@element-plus/icons-vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { RadarChart } from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  LegendComponent
} from 'echarts/components'
import VChart from 'vue-echarts'

use([
  CanvasRenderer,
  RadarChart,
  TitleComponent,
  TooltipComponent,
  LegendComponent
])

const route = useRoute()
const router = useRouter()
const recordId = route.params.recordId

const loading = ref(false)
const data = ref({
  examName: '',
  totalScore: 0,
  maxScore: 100,
  rank: 0,
  totalStudents: 0,
  knowledgeMastery: [],
  answers: []
})

const customColors = [
  { color: '#f56c6c', percentage: 60 },
  { color: '#e6a23c', percentage: 80 },
  { color: '#67c23a', percentage: 100 }
]

const getScoreTagType = (rate) => {
  if (rate >= 0.85) return 'success'
  if (rate >= 0.6) return 'warning'
  return 'danger'
}

const radarOption = computed(() => {
  if (!data.value.knowledgeMastery || data.value.knowledgeMastery.length === 0) return {}

  const indicators = data.value.knowledgeMastery.map(k => ({
    name: k.name,
    max: 1
  }))

  return {
    tooltip: {
      trigger: 'item'
    },
    radar: {
      indicator: indicators,
      radius: '65%',
      axisName: {
        color: '#606266',
        fontSize: 12
      }
    },
    series: [
      {
        name: '知识点掌握度',
        type: 'radar',
        data: [
          {
            value: data.value.knowledgeMastery.map(k => k.accuracy),
            name: '正确率',
            areaStyle: {
              color: 'rgba(64, 158, 255, 0.3)'
            },
            lineStyle: {
              color: '#409eff'
            },
            itemStyle: {
              color: '#409eff'
            }
          }
        ]
      }
    ]
  }
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

.rate-tag {
  margin-left: 10px;
  vertical-align: middle;
}

.analysis-section {
  margin-top: 20px;
  margin-bottom: 20px;
}

.info-card {
  height: 300px;
}

.info-grid {
  display: flex;
  flex-direction: column;
  justify-content: space-around;
  height: 240px;
  align-items: center;
  text-align: center;
}

.info-label {
  font-size: 14px;
  color: #909399;
  margin-bottom: 8px;
}

.info-value {
  font-size: 20px;
  color: #303133;
  margin-bottom: 4px;
}

.rank-num {
  font-size: 32px;
  font-weight: bold;
  color: #409eff;
}

.info-total {
  font-size: 12px;
  color: #c0c4cc;
}

.radar-card {
  height: 300px;
}

.analysis-title-text {
  font-weight: bold;
  font-size: 15px;
}

.radar-container {
  height: 230px;
}

.radar-chart {
  height: 100%;
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
