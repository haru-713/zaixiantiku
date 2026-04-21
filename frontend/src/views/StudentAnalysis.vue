<template>
  <div class="student-analysis-page">
    <div class="page-header">
      <div class="header-left">
        <h2 class="title">学习报告</h2>
      </div>
      <div class="header-right">
        <el-select v-model="selectedCourse" placeholder="请选择课程" @change="handleCourseChange" size="default" style="width: 240px">
          <el-option v-for="item in courses" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
      </div>
    </div>

    <div v-if="!selectedCourse" class="empty-state">
      <el-empty description="请先选择一门课程以查看详细学习报告" />
    </div>

    <template v-else>
      <el-tabs v-model="activeTab" class="analysis-tabs">
        <el-tab-pane label="练习报告" name="practice">
          <!-- 一、概览卡片 -->
          <el-row :gutter="20" class="overview-section">
            <el-col :span="8">
              <el-card shadow="hover" class="stat-card">
                <template #header>
                  <div class="card-header">
                    <span>【练习概况】</span>
                    <el-button link type="primary" @click="goToRecords('/study/record')">查看详情</el-button>
                  </div>
                </template>
                <div class="stat-content">
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
            <el-col :span="6">
              <el-card shadow="hover" class="stat-card">
                <template #header>
                  <div class="card-header">
                    <span>【错题统计】</span>
                    <el-button link type="primary" @click="goToRecords('/study/mistakes')">查看错题本</el-button>
                  </div>
                </template>
                <div class="stat-content single">
                  <div class="stat-item">
                    <div class="label">当前错题总数</div>
                    <div class="value mistake-count">{{ report.mistakeCount }}</div>
                  </div>
                </div>
              </el-card>
            </el-col>
          </el-row>

          <!-- 二、图表分析 -->
          <el-row :gutter="20" class="chart-section">
            <el-col :span="24">
              <el-card shadow="hover" class="chart-card">
                <template #header><span>【学习正确率趋势】（近7天）</span></template>
                <v-chart class="chart full-width" :option="practiceTrendOption" autoresize />
              </el-card>
            </el-col>
          </el-row>

          <el-row :gutter="20" class="chart-section" style="margin-top: 20px;">
            <el-col :span="12">
              <el-card shadow="hover" class="chart-card">
                <template #header><span>【题型分布统计】（按正确率）</span></template>
                <v-chart class="chart" :option="typeDistOption" autoresize />
              </el-card>
            </el-col>
            <el-col :span="12">
              <el-card shadow="hover" class="chart-card">
                <template #header><span>【知识点掌握情况】（按正确率）</span></template>
                <v-chart class="chart" :option="knowledgeRadarOption" autoresize />
              </el-card>
            </el-col>
          </el-row>
        </el-tab-pane>

        <el-tab-pane label="考试报告" name="exam">
          <el-row :gutter="20" class="overview-section">
            <el-col :span="10">
              <el-card shadow="hover" class="stat-card">
                <template #header>
                  <div class="card-header">
                    <span>【考试概况】</span>
                    <el-button link type="primary" @click="goToRecords('/study/exam-record')">查看详情</el-button>
                  </div>
                </template>
                <div class="stat-content">
                  <div class="stat-item">
                    <div class="label">参加考试次数</div>
                    <div class="value">{{ examSummary.totalExamCount }}</div>
                  </div>
                  <div class="stat-item">
                    <div class="label">平均得分率</div>
                    <div class="value rate">{{ (examSummary.avgExamScoreRate * 100).toFixed(1) }}%</div>
                  </div>
                  <div class="stat-item">
                    <div class="label">最高分</div>
                    <div class="value high-score">
                      {{ examSummary.maxExamScore }}
                      <span class="total-score-label" v-if="examSummary.maxExamTotalScore"> / {{ examSummary.maxExamTotalScore }}</span>
                    </div>
                  </div>
                </div>
              </el-card>
            </el-col>
          </el-row>

          <el-row :gutter="20" class="chart-section" style="margin-top: 20px;">
            <el-col :span="24">
              <el-card shadow="hover" class="chart-card">
                <template #header><span>【历次考试得分率趋势】</span></template>
                <v-chart class="chart full-width" :option="examTrendOption" autoresize />
              </el-card>
            </el-col>
          </el-row>

          <el-row :gutter="20" class="table-section" style="margin-top: 20px;">
            <el-col :span="24">
              <el-card shadow="hover" class="table-card">
                <template #header><span>【考试记录】</span></template>
                <el-table :data="examRecords.list" v-loading="examRecordsLoading" style="width: 100%">
                  <el-table-column prop="examName" label="考试名称" width="200" />
                  <el-table-column prop="submitTime" label="提交时间" width="180">
                    <template #default="{ row }">
                      {{ formatDate(row.submitTime) }}
                    </template>
                  </el-table-column>
                  <el-table-column label="得分" width="100">
                    <template #default="{ row }">
                      {{ row.totalScore }} / {{ row.maxScore }}
                    </template>
                  </el-table-column>
                  <el-table-column label="得分率" width="100">
                    <template #default="{ row }">
                      {{ (row.scoreRate * 100).toFixed(1) }}%
                    </template>
                  </el-table-column>
                  <el-table-column label="状态" width="100">
                    <template #default="{ row }">
                      <el-tag :type="row.status === 2 ? 'success' : 'warning'">
                        {{ row.status === 2 ? '已批阅' : '已提交' }}
                      </el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column label="操作">
                    <template #default="{ row }">
                      <el-button link type="primary" @click="goToExamDetail(row.id)">查看详情</el-button>
                    </template>
                  </el-table-column>
                </el-table>
                <div class="pagination-container">
                  <el-pagination
                    background
                    layout="total, prev, pager, next"
                    :total="examRecords.total"
                    :page-size="examRecords.pageSize"
                    :current-page="examRecords.pageNum"
                    @current-change="handleExamRecordsPageChange"
                  />
                </div>
              </el-card>
            </el-col>
          </el-row>
        </el-tab-pane>
      </el-tabs>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, BarChart, RadarChart } from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent
} from 'echarts/components'
import VChart from 'vue-echarts'

use([
  CanvasRenderer,
  LineChart,
  BarChart,
  RadarChart,
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent
])

const router = useRouter()
const activeTab = ref('practice') // 默认显示练习报告
const selectedCourse = ref(null)
const courses = ref([])
const report = ref({
  totalPracticeCount: 0,
  totalPracticeQuestions: 0,
  avgPracticeAccuracy: 0,
  mistakeCount: 0,
  practiceTrend: [],
  examTypeStats: [],
  knowledgeRadar: []
})

const examSummary = ref({
  totalExamCount: 0,
  avgExamScoreRate: 0,
  maxExamScore: 0,
  maxExamTotalScore: 0,
})

const examRecords = ref({
  list: [],
  total: 0,
  pageNum: 1,
  pageSize: 10
})
const examRecordsLoading = ref(false)
const examScoreTrend = ref([])

const fetchCourses = async () => {
  try {
    const res = await request.get('/student/analysis/courses')
    if (res.code === 1) {
      courses.value = res.data
      if (courses.value.length > 0) {
        selectedCourse.value = courses.value[0].id
        fetchReport()
        fetchExamSummary()
        fetchExamRecords()
        fetchExamScoreTrend()
      }
    }
  } catch (e) {
    console.error(e)
  }
}

const fetchReport = async () => {
  if (!selectedCourse.value) return
  try {
    const res = await request.get('/student/analysis/report', {
      params: { courseId: selectedCourse.value }
    })
    if (res.code === 1) {
      report.value = res.data
    }
  } catch (e) {
    console.error(e)
    ElMessage.error('获取学习报告失败')
  }
}

const fetchExamSummary = async () => {
  if (!selectedCourse.value) return
  try {
    const res = await request.get('/student/analysis/exam-summary', {
      params: { courseId: selectedCourse.value }
    })
    if (res.code === 1) {
      examSummary.value = res.data
    }
  } catch (e) {
    console.error(e)
    ElMessage.error('获取考试概况失败')
  }
}

const fetchExamRecords = async () => {
  if (!selectedCourse.value) return
  examRecordsLoading.value = true
  try {
    const res = await request.get('/student/analysis/exam-records', {
      params: {
        courseId: selectedCourse.value,
        page: examRecords.value.pageNum,
        size: examRecords.value.pageSize
      }
    })
    if (res.code === 1) {
      examRecords.value.list = res.data.list
      examRecords.value.total = res.data.total
    }
  } catch (e) {
    console.error(e)
    ElMessage.error('获取考试记录失败')
  } finally {
    examRecordsLoading.value = false
  }
}

const fetchExamScoreTrend = async () => {
  if (!selectedCourse.value) return
  try {
    const res = await request.get('/student/analysis/exam-score-trend', {
      params: { courseId: selectedCourse.value }
    })
    if (res.code === 1) {
      examScoreTrend.value = res.data
    }
  } catch (e) {
    console.error(e)
    ElMessage.error('获取考试得分趋势失败')
  }
}

const handleCourseChange = () => {
  fetchReport()
  fetchExamSummary()
  fetchExamRecords()
  fetchExamScoreTrend()
}

const handleExamRecordsPageChange = (newPage) => {
  examRecords.value.pageNum = newPage
  fetchExamRecords()
}

const goToRecords = (path) => {
  router.push({
    path,
    query: { courseId: selectedCourse.value }
  })
}

const goToExamDetail = (recordId) => {
  router.push({
    path: `/study/exam-report-detail/${recordId}`
  })
}

const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}`
}

// 图表配置
const practiceTrendOption = computed(() => ({
  tooltip: { trigger: 'axis', formatter: '{b}<br/>正确率: {c}%' },
  xAxis: { 
    type: 'category', 
    data: report.value.practiceTrend.map(t => t.date),
    axisLabel: { color: '#909399' }
  },
  yAxis: { 
    type: 'value', 
    max: 100,
    axisLabel: { formatter: '{value}%', color: '#909399' },
    splitLine: { lineStyle: { type: 'dashed' } }
  },
  grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
  series: [{
    data: report.value.practiceTrend.map(t => (t.value * 100).toFixed(1)),
    type: 'line',
    smooth: true,
    areaStyle: { 
      color: {
        type: 'linear',
        x: 0, y: 0, x2: 0, y2: 1,
        colorStops: [{ offset: 0, color: '#409eff' }, { offset: 1, color: '#ecf5ff' }]
      },
      opacity: 0.3 
    },
    lineStyle: { width: 3, color: '#409eff' },
    itemStyle: { color: '#409eff' }
  }]
}))

const examTrendOption = computed(() => ({
  tooltip: { trigger: 'axis', formatter: '{b}<br/>得分率: {c}%' },
  xAxis: {
    type: 'category',
    data: examScoreTrend.value.map(t => t.date), // 假设后端返回的TrendVO中date是考试名称
    axisLabel: { color: '#909399', rotate: 30 }
  },
  yAxis: {
    type: 'value',
    max: 100,
    axisLabel: { formatter: '{value}%', color: '#909399' },
    splitLine: { lineStyle: { type: 'dashed' } }
  },
  grid: { left: '3%', right: '4%', bottom: '15%', containLabel: true },
  series: [{
    data: examScoreTrend.value.map(t => (t.value * 100).toFixed(1)),
    type: 'line',
    smooth: true,
    areaStyle: {
      color: {
        type: 'linear',
        x: 0, y: 0, x2: 0, y2: 1,
        colorStops: [{ offset: 0, color: '#e6a23c' }, { offset: 1, color: '#fdf6ec' }]
      },
      opacity: 0.3
    },
    lineStyle: { width: 3, color: '#e6a23c' },
    itemStyle: { color: '#e6a23c' }
  }]
}))

const typeDistOption = computed(() => {
  const data = report.value.examTypeStats || []
  return {
    tooltip: { trigger: 'axis', formatter: '{b}<br/>正确率: {c}%' },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { 
      type: 'category', 
      data: data.map(t => t.typeName),
      axisLabel: { color: '#909399', interval: 0 }
    },
    yAxis: { 
      type: 'value', 
      max: 100,
      axisLabel: { formatter: '{value}%', color: '#909399' }
    },
    series: [{
      data: data.map(t => (t.accuracy * 100).toFixed(1)),
      type: 'bar',
      barWidth: '40%',
      itemStyle: {
        color: '#409eff',
        borderRadius: [4, 4, 0, 0]
      },
      label: {
        show: true,
        position: 'top',
        formatter: '{c}%'
      }
    }]
  }
})

const knowledgeRadarOption = computed(() => {
  const indicators = report.value.knowledgeRadar.map(k => ({ name: k.name, max: 100 }))
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
        value: report.value.knowledgeRadar.map(k => (k.value * 100).toFixed(1)),
        name: '正确率',
        areaStyle: { color: 'rgba(103, 194, 58, 0.3)' },
        lineStyle: { color: '#67c23a' },
        itemStyle: { color: '#67c23a' }
      }]
    }]
  }
})

onMounted(() => {
  fetchCourses()
})
</script>

<style scoped>
.student-analysis-page {
  padding: 20px;
  background-color: #f5f7fa;
  min-height: calc(100vh - 40px);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  background-color: #fff;
  padding: 15px 20px;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0,0,0,0.05);
}

.title {
  margin: 0;
  font-size: 20px;
  color: #303133;
}

.empty-state {
  margin-top: 100px;
}

.stat-card {
  height: 180px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: bold;
}

.stat-content {
  display: flex;
  justify-content: space-around;
  align-items: center;
  height: 80px;
}

.stat-content.single {
  justify-content: center;
}

.stat-item {
  text-align: center;
}

.stat-item .label {
  font-size: 13px;
  color: #909399;
  margin-bottom: 10px;
}

.stat-item .value {
  font-size: 24px;
  font-weight: bold;
  color: #303133;
}

.value.accuracy { color: #409eff; }
.value.rate { color: #e6a23c; }
.value.high-score { color: #67c23a; }
.value.mistake-count { color: #f56c6c; font-size: 36px; }

.total-score-label {
  font-size: 14px;
  color: #909399;
  font-weight: normal;
}

.chart-card {
  border-radius: 8px;
}

.chart {
  height: 350px;
  width: 100%;
}

:deep(.el-card__header) {
  padding: 12px 20px;
  border-bottom: 1px solid #ebeef5;
}
</style>
