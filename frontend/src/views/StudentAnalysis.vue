<template>
  <div class="student-analysis-page">
    <!-- 一、整体概览 (仪表盘) -->
    <div class="filter-bar">
      <el-radio-group v-model="timeRange" @change="fetchReport" size="small">
        <el-radio-button value="all">全部</el-radio-button>
        <el-radio-button value="month">本月</el-radio-button>
        <el-radio-button value="week">本周</el-radio-button>
      </el-radio-group>
    </div>
    <el-row :gutter="20">
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card practice-card">
          <template #header>
            <div class="card-header">
              <span>练习概览</span>
              <el-button link type="primary" @click="router.push('/study/record')">查看详情</el-button>
            </div>
          </template>
          <div class="stat-grid">
            <div class="stat-item">
              <div class="label">总练习次数</div>
              <div class="value">{{ report.totalPracticeCount }}</div>
            </div>
            <div class="stat-item">
              <div class="label">累计答题数</div>
              <div class="value">{{ report.totalPracticeQuestions }}</div>
            </div>
            <div class="stat-item">
              <div class="label">平均正确率</div>
              <div class="value accuracy">{{ (report.avgPracticeAccuracy * 100).toFixed(1) }}%</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="10">
        <el-card shadow="hover" class="stat-card exam-card">
          <template #header>
            <div class="card-header">
              <span>考试概览</span>
              <el-button link type="primary" @click="router.push('/study/exam-record')">查看详情</el-button>
            </div>
          </template>
          <div class="stat-grid exam-grid">
            <div class="stat-item">
              <div class="label">参加考试次数</div>
              <div class="value">{{ report.totalExamCount }}</div>
            </div>
            <div class="stat-item">
              <div class="label">平均得分率</div>
              <div class="value rate">{{ (report.avgExamScoreRate * 100).toFixed(1) }}%</div>
            </div>
            <div class="stat-item">
              <div class="label">平均分</div>
              <div class="value">{{ report.avgExamScore.toFixed(1) }}</div>
            </div>
            <div class="stat-item">
              <div class="label">最高分</div>
              <div class="value high-score">
                {{ report.maxExamScore }}
                <span class="total-score-label" v-if="report.maxExamTotalScore"> / {{ report.maxExamTotalScore }}</span>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card mistake-card">
          <template #header>
            <div class="card-header">
              <span>错题概览</span>
            </div>
          </template>
          <div class="stat-grid">
            <div class="stat-item single">
              <div class="label">当前错题总数</div>
              <div class="value mistake-count">{{ report.mistakeCount }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 二、分项详细分析 -->
    <el-tabs v-model="activeTab" class="analysis-tabs" type="border-card">
      <el-tab-pane label="考试分析报告" name="exam">
        <el-row :gutter="20">
          <el-col :span="24">
            <el-card shadow="never" class="chart-card">
              <template #header><span>班级排名趋势</span></template>
              <v-chart class="chart full-width-chart" :option="rankingTrendOption" autoresize />
            </el-card>
          </el-col>
        </el-row>

        <el-row :gutter="20" style="margin-top: 20px;">
          <el-col :span="24">
            <el-card shadow="never">
              <template #header>
                <div class="card-header">
                  <span>近期考试记录</span>
                  <el-button link type="primary" @click="router.push('/study/exam-record')">更多记录</el-button>
                </div>
              </template>
              <el-table :data="report.recentExams" stripe size="small" style="width: 100%">
                <el-table-column prop="examName" label="考试名称" min-width="180" />
                <el-table-column label="得分 / 满分" width="120" align="center">
                  <template #default="scope">
                    {{ scope.row.score }} / {{ scope.row.maxScore }}
                  </template>
                </el-table-column>
                <el-table-column label="得分率" width="100" align="center">
                  <template #default="scope">
                    <el-tag :type="getScoreTagType(scope.row.scoreRate)" size="small">
                      {{ (scope.row.scoreRate * 100).toFixed(1) }}%
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="submitTime" label="提交时间" width="160" align="center" />
              </el-table>
            </el-card>
          </el-col>
        </el-row>

        <el-row :gutter="20" style="margin-top: 20px;">
          <el-col :span="24">
            <el-card shadow="never" class="chart-card">
              <template #header><span>考试高频错题 Top5 (按频次降序)</span></template>
              <el-table :data="report.highFreqMistakes" size="small">
                <el-table-column type="index" label="排名" width="60" />
                <el-table-column prop="content" label="题目内容">
                  <template #default="scope">
                    <div class="text-ellipsis" v-html="scope.row.content"></div>
                  </template>
                </el-table-column>
                <el-table-column prop="wrongCount" label="错误频次" width="120" sortable />
              </el-table>
            </el-card>
          </el-col>
        </el-row>
      </el-tab-pane>

      <el-tab-pane label="练习分析报告" name="practice">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-card shadow="never" class="chart-card">
              <template #header><span>练习正确率趋势 (近7次)</span></template>
              <v-chart class="chart" :option="practiceTrendOption" autoresize />
            </el-card>
          </el-col>
          <el-col :span="12">
            <el-card shadow="never" class="chart-card">
              <template #header><span>知识点掌握度 (雷达图)</span></template>
              <v-chart class="chart" :option="knowledgeRadarOption" autoresize />
            </el-card>
          </el-col>
        </el-row>
        <el-row :gutter="20" style="margin-top: 20px;">
          <el-col :span="12">
            <el-card shadow="never" class="chart-card">
              <template #header><span>题型练习统计</span></template>
              <v-chart class="chart" :option="practiceTypeOption" autoresize />
            </el-card>
          </el-col>
          <el-col :span="12">
            <el-card shadow="never" class="chart-card">
              <template #header><span>错题集中知识点 Top5</span></template>
              <el-table :data="report.topMistakePoints" size="small">
                <el-table-column type="index" label="排名" width="60" />
                <el-table-column prop="name" label="知识点" />
                <el-table-column prop="count" label="错题数" width="100" />
              </el-table>
            </el-card>
          </el-col>
        </el-row>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, BarChart, PieChart, RadarChart } from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent,
  VisualMapComponent
} from 'echarts/components'
import VChart from 'vue-echarts'

use([
  CanvasRenderer,
  LineChart,
  BarChart,
  PieChart,
  RadarChart,
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent,
  VisualMapComponent
])

const router = useRouter()
const activeTab = ref('practice')
const timeRange = ref('all')
const report = ref({
  totalPracticeCount: 0,
  totalPracticeQuestions: 0,
  avgPracticeAccuracy: 0,
  totalExamCount: 0,
  avgExamScore: 0,
  avgExamScoreRate: 0,
  maxExamScore: 0,
  mistakeCount: 0,
  recentExams: [],
  practiceTrend: [],
  knowledgeRadar: [],
  practiceTypeStats: [],
  topMistakePoints: [],
  rankingTrend: [],
  examTypeStats: [],
  highFreqMistakes: []
})

const fetchReport = async () => {
  try {
    const res = await request.get('/student/analysis/report', {
      params: { timeRange: timeRange.value }
    })
    if (res.code === 1) {
      report.value = res.data
    }
  } catch (e) {
    console.error(e)
    ElMessage.error('获取学习报告失败')
  }
}

// 图表配置
const practiceTrendOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  xAxis: { type: 'category', data: report.value.practiceTrend.map(t => t.date) },
  yAxis: { type: 'value', max: 100 },
  series: [{
    data: report.value.practiceTrend.map(t => (t.accuracy * 100).toFixed(1)),
    type: 'line',
    smooth: true,
    areaStyle: { opacity: 0.2 },
    color: '#409eff'
  }]
}))

const knowledgeRadarOption = computed(() => {
  const indicators = report.value.knowledgeRadar.map(k => ({ name: k.name, max: 1 }))
  return {
    tooltip: {},
    radar: { indicator: indicators },
    series: [{
      type: 'radar',
      data: [{
        value: report.value.knowledgeRadar.map(k => k.value),
        name: '掌握度',
        areaStyle: { opacity: 0.3 }
      }]
    }]
  }
})

const practiceTypeOption = computed(() => ({
  tooltip: { trigger: 'item' },
  legend: { bottom: '5%', left: 'center' },
  series: [{
    type: 'pie',
    radius: ['40%', '70%'],
    avoidLabelOverlap: false,
    itemStyle: { borderRadius: 10, borderColor: '#fff', borderWidth: 2 },
    label: { show: false, position: 'center' },
    emphasis: { label: { show: true, fontSize: 16, fontWeight: 'bold' } },
    data: report.value.practiceTypeStats.map(t => ({ value: t.count, name: t.typeName }))
  }]
}))

const rankingTrendOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  xAxis: { type: 'category', data: report.value.rankingTrend.map(r => r.examName) },
  yAxis: { type: 'value', inverse: true, min: 1 },
  series: [{
    data: report.value.rankingTrend.map(r => r.rank),
    type: 'line',
    label: { show: true, position: 'top' },
    color: '#e6a23c'
  }]
}))

const getScoreTagType = (rate) => {
  if (rate >= 0.9) return 'success'
  if (rate >= 0.7) return 'primary'
  if (rate >= 0.6) return 'warning'
  return 'danger'
}

onMounted(() => {
  fetchReport()
})
</script>

<style scoped>
.student-analysis-page {
  padding: 20px;
}

.filter-bar {
  margin-bottom: 15px;
  display: flex;
  justify-content: flex-end;
}

.stat-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: bold;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
  padding: 10px 0;
}

.exam-grid {
  grid-template-columns: repeat(4, 1fr);
}

.stat-item {
  text-align: center;
}

.stat-item.single {
  grid-column: span 3;
}

.stat-item .label {
  font-size: 13px;
  color: #909399;
  margin-bottom: 8px;
}

.stat-item .value {
  font-size: 22px;
  font-weight: bold;
  color: #303133;
}

.value.accuracy, .value.rate { color: #409eff; }
.value.high-score { color: #67c23a; }
.total-score-label {
  font-size: 14px;
  color: #909399;
  font-weight: normal;
  margin-left: 2px;
}
.value.mistake-count { color: #f56c6c; font-size: 32px; }

.analysis-tabs {
  margin-top: 10px;
}

.chart-card {
  height: 400px;
}

.chart {
  height: 320px;
}

.full-width-chart {
  width: 100%;
}

.text-ellipsis {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

:deep(.el-card__header) {
  padding: 10px 15px;
  font-size: 14px;
}
</style>
