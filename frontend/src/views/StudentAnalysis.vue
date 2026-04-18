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
                <div class="value">{{ report.totalExamCount }}</div>
              </div>
              <div class="stat-item">
                <div class="label">平均得分率</div>
                <div class="value rate">{{ (report.avgExamScoreRate * 100).toFixed(1) }}%</div>
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
const selectedCourse = ref(null)
const courses = ref([])
const report = ref({
  totalPracticeCount: 0,
  totalPracticeQuestions: 0,
  avgPracticeAccuracy: 0,
  totalExamCount: 0,
  avgExamScoreRate: 0,
  maxExamScore: 0,
  maxExamTotalScore: 0,
  mistakeCount: 0,
  practiceTrend: [],
  examTypeStats: [],
  knowledgeRadar: []
})

const fetchCourses = async () => {
  try {
    const res = await request.get('/student/analysis/courses')
    if (res.code === 1) {
      courses.value = res.data
      if (courses.value.length > 0) {
        selectedCourse.value = courses.value[0].id
        fetchReport()
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

const handleCourseChange = () => {
  fetchReport()
}

const goToRecords = (path) => {
  router.push({
    path,
    query: { courseId: selectedCourse.value }
  })
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
    data: report.value.practiceTrend.map(t => (t.accuracy * 100).toFixed(1)),
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
