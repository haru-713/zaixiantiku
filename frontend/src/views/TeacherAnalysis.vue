<template>
  <div class="teacher-analysis-page" :class="{ 'embedded': isEmbedded }">
    <el-form :inline="true" :model="query" class="query-form" v-if="!isEmbedded">
      <el-form-item label="选择课程">
        <el-select v-model="query.courseId" placeholder="请选择课程" @change="handleCourseChange" style="width: 200px">
          <el-option v-for="item in courses" :key="item.id" :label="item.courseName" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="选择班级">
        <el-select v-model="query.classId" placeholder="请选择班级" @change="handleClassChange" style="width: 200px"
          :disabled="!query.courseId">
          <el-option v-for="item in classes" :key="item.id" :label="item.className" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="选择考试">
        <el-select v-model="query.examId" placeholder="全部考试" clearable @change="handleExamChange" style="width: 250px"
          :disabled="!query.classId">
          <el-option label="全部考试" :value="null" />
          <el-option v-for="item in exams" :key="item.id" :label="item.examName" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleExamChange" :disabled="!query.classId">刷新分析</el-button>
      </el-form-item>
    </el-form>

    <div v-else class="embedded-header">
      <el-form :inline="true" :model="query">
        <el-form-item label="筛选考试">
          <el-select v-model="query.examId" placeholder="全部考试" clearable @change="handleExamChange"
            style="width: 250px">
            <el-option label="全部考试" :value="null" />
            <el-option v-for="item in exams" :key="item.id" :label="item.examName" :value="item.id" />
          </el-select>
        </el-form-item>
      </el-form>
    </div>

    <div v-if="!query.courseId && !isEmbedded" class="empty-tip">
      <el-empty description="请先选择课程以进行分析" />
    </div>

    <div v-else-if="!query.classId" class="empty-tip">
      <el-empty description="请先选择一个班级进行分析" />
    </div>

    <div v-else-if="!query.examId && !isEmbedded && !globalViewRequested" class="empty-tip">
      <el-result icon="info" title="请选择具体考试" sub-title="为了更精准地分析教学效果，请在上方选择一场具体考试以查看详细分析。">
        <template #extra>
          <el-button type="primary" @click="handleViewGlobal">查看全课综合统计</el-button>
        </template>
      </el-result>
    </div>

    <template v-else-if="analysis">
      <el-row :gutter="20">
        <el-col :span="8">
          <el-card shadow="hover">
            <template #header>
              <div class="card-header">
                <span>班级平均分</span>
                <el-button type="primary" size="small" link @click="scoreDetailVisible = true">查看全班分数明细</el-button>
              </div>
            </template>
            <div class="stat-value" :class="{ 'zero-value': analysis.averageScore === 0 }">
              {{ analysis.averageScore.toFixed(2) }}
              <span v-if="analysis.maxScore" class="total-score-suffix">/ {{ analysis.maxScore }}</span>
            </div>
          </el-card>
        </el-col>
        <el-col :span="16">
          <el-card shadow="hover">
            <template #header>
              <div class="card-header">
                <span>成绩分布 (得分率占比)</span>
              </div>
            </template>
            <div class="chart-container" v-if="hasData">
              <v-chart class="analysis-chart" :option="scoreDistOption" autoresize />
              <div class="chart-hint">注：分数占比 = 学生得分 / 试卷总分 × 100%</div>
            </div>
            <div v-else class="no-data-mini">暂无成绩数据</div>
          </el-card>
        </el-col>
      </el-row>

      <el-card shadow="hover" style="margin-top: 20px;" v-if="!query.examId">
        <template #header>
          <div class="card-header">
            <span>班级历次考试趋势</span>
          </div>
        </template>
        <div class="chart-container" style="height: 300px;">
          <v-chart class="analysis-chart" :option="classTrendOption" autoresize />
          <div class="chart-hint trend-hint">注：仅显示有学生参加的考试。无人参加的考试未计入趋势。</div>
        </div>
      </el-card>

      <el-card shadow="hover" style="margin-top: 20px;">
        <template #header>
          <div class="card-header">
            <span>题目正确率分析</span>
            <span class="header-tip" v-if="!query.examId">（请选择具体考试以查看题目分析）</span>
          </div>
        </template>
        <div v-if="query.examId">
          <el-table :data="analysis.questionAccuracies" style="width: 100%" border stripe
            v-if="analysis.questionAccuracies.length > 0">
            <el-table-column prop="questionId" label="题目ID" width="100" align="center" />
            <el-table-column prop="content" label="题目内容" show-overflow-tooltip>
              <template #default="scope">
                <div v-html="scope.row.content"></div>
              </template>
            </el-table-column>
            <el-table-column label="班级正确率" width="200">
              <template #default="scope">
                <div class="accuracy-cell">
                  <el-progress :percentage="Math.round(scope.row.accuracy * 100)"
                    :status="scope.row.accuracy < 0.6 ? 'exception' : 'success'" />
                  <span class="percentage-text">{{ Math.round(scope.row.accuracy * 100) }}%</span>
                </div>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-else description="暂无题目分析数据" :image-size="100" />
        </div>
        <div v-else class="all-exams-tip">
          <el-result icon="info" title="模式说明" sub-title="在“全部考试”模式下，系统仅统计班级整体分数表现。如需查看具体题目的正确率分析，请在上方选择一场具体的考试。">
          </el-result>
        </div>
      </el-card>
    </template>

    <!-- 分数明细弹窗 -->
    <el-dialog v-model="scoreDetailVisible" title="班级成绩明细" width="800px" destroy-on-close>
      <div class="score-detail-header"
        style="margin-bottom: 15px; display: flex; justify-content: space-between; align-items: center;">
        <span style="font-weight: bold; color: #409EFF;">
          {{query.examId ? '考试：' + exams.find(e => e.id === query.examId)?.examName : '统计范围：全课考试综合'}}
        </span>
        <el-button type="success" size="small" icon="Download" @click="exportScores">导出Excel</el-button>
      </div>
      <el-table :data="analysis.studentScores" border stripe height="450px" v-if="analysis && analysis.studentScores">
        <el-table-column prop="username" label="学号" width="150" align="center" />
        <el-table-column prop="name" label="姓名" width="150" align="center" />
        <el-table-column prop="className" label="班级" min-width="150" />
        <el-table-column label="得分" width="150" align="center">
          <template #default="scope">
            <span style="font-weight: bold; color: #67C23A">{{ scope.row.score }}</span>
            <span v-if="scope.row.totalScore" style="color: #909399"> / {{ scope.row.totalScore }}</span>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, defineProps, watch } from 'vue'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { BarChart, LineChart } from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent
} from 'echarts/components'
import VChart from 'vue-echarts'

use([
  CanvasRenderer,
  BarChart,
  LineChart,
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent
])

const props = defineProps({
  classId: {
    type: Number,
    default: null
  },
  courseId: {
    type: Number,
    default: null
  },
  examId: {
    type: Number,
    default: null
  },
  isEmbedded: {
    type: Boolean,
    default: false
  }
})

const query = reactive({
  courseId: props.courseId,
  classId: props.classId,
  examId: props.examId
})

// 监听外部传入的 classId, courseId 或 examId 变化
watch(() => [props.classId, props.courseId, props.examId], ([newClassId, newCourseId, newExamId]) => {
  if (props.isEmbedded) {
    if (newClassId) query.classId = newClassId
    if (newCourseId) query.courseId = newCourseId
    query.examId = newExamId
    fetchAnalysis()
  }
})

const courses = ref([])
const classes = ref([])
const exams = ref([])
const analysis = ref(null)
const scoreDetailVisible = ref(false)
const globalViewRequested = ref(false)

const scoreDistOption = computed(() => {
  if (!analysis.value) return {}
  const data = [...analysis.value.scoreDistribution].reverse() // 0-59% 在左，90-100% 在右

  // 统一显示“人数”，因为后端逻辑已统一为按学生得分率分布
  const modeLabel = '人数'

  return {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params) => {
        const item = params[0]
        // 增加更清晰的提示样式
        return `<div style="margin-bottom:4px;font-weight:bold;">${item.name}</div>
                <div style="display:flex;align-items:center;">
                  <span style="display:inline-block;margin-right:8px;border-radius:50%;width:10px;height:10px;background-color:#409EFF;"></span>
                  <span style="flex:1;margin-right:15px;">${modeLabel}</span>
                  <b style="color:#303133">${item.value}</b>
                </div>`
      }
    },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: data.map(i => i.range) },
    yAxis: { type: 'value', name: modeLabel, minInterval: 1 },
    series: [
      {
        name: modeLabel,
        type: 'bar',
        barWidth: '60%',
        data: data.map(i => i.count),
        itemStyle: {
          color: '#409EFF',
          borderRadius: [4, 4, 0, 0]
        }
      }
    ]
  }
})

const hasData = computed(() => {
  if (!analysis.value) return false
  return analysis.value.scoreDistribution.some(item => item.count > 0)
})

const classTrendOption = computed(() => {
  if (!analysis.value || !analysis.value.classTrend) return {}
  const trend = analysis.value.classTrend
  return {
    tooltip: { trigger: 'axis' },
    legend: { data: ['平均分', '及格率 (%)'], bottom: 0 },
    grid: { left: '3%', right: '4%', bottom: '15%', containLabel: true },
    xAxis: {
      type: 'category',
      data: trend.map(t => t.examName),
      axisLabel: { interval: 0, rotate: 20 }
    },
    yAxis: [
      { type: 'value', name: '平均分' },
      { type: 'value', name: '及格率', max: 100, axisLabel: { formatter: '{value}%' } }
    ],
    series: [
      {
        name: '平均分',
        type: 'line',
        smooth: true,
        data: trend.map(t => t.averageScore.toFixed(1)),
        itemStyle: { color: '#409EFF' }
      },
      {
        name: '及格率 (%)',
        type: 'line',
        smooth: true,
        yAxisIndex: 1,
        data: trend.map(t => t.passRate.toFixed(1)),
        itemStyle: { color: '#67C23A' }
      }
    ]
  }
})

const exportScores = () => {
  if (!analysis.value || !analysis.value.studentScores) return

  // 简单的 CSV 导出实现
  const headers = ['学号', '姓名', '班级', '得分']
  const rows = analysis.value.studentScores.map(s => [
    s.username,
    s.name,
    s.className,
    s.score + (s.totalScore ? ' / ' + s.totalScore : '')
  ])

  let csvContent = "data:text/csv;charset=utf-8,\uFEFF"
    + headers.join(",") + "\n"
    + rows.map(e => e.join(",")).join("\n")

  const encodedUri = encodeURI(csvContent)
  const link = document.createElement("a")
  link.setAttribute("href", encodedUri)
  const fileName = `成绩单_${query.examId ? exams.value.find(e => e.id === query.examId)?.examName : '全课综合'}.csv`
  link.setAttribute("download", fileName)
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}

const fetchCourses = async () => {
  if (props.isEmbedded) return
  try {
    const res = await request.get('/courses/managed')
    if (res.code === 1) {
      courses.value = res.data
      if (courses.value.length > 0 && !query.courseId) {
        query.courseId = courses.value[0].id
        handleCourseChange()
      }
    }
  } catch (e) {
    console.error(e)
    ElMessage.error('获取课程列表失败')
  }
}

const handleCourseChange = async () => {
  query.classId = null
  query.examId = null
  analysis.value = null
  globalViewRequested.value = false
  classes.value = []
  exams.value = []

  if (!query.courseId) return

  try {
    // 同时加载班级和考试列表
    const [classRes, examRes] = await Promise.all([
      request.get('/teacher/analysis/classes', { params: { courseId: query.courseId } }),
      request.get('/teacher/analysis/exams', { params: { courseId: query.courseId } })
    ])

    if (classRes.code === 1) {
      classes.value = classRes.data
    }
    if (examRes.code === 1) {
      exams.value = examRes.data
    }
  } catch (e) {
    console.error(e)
    ElMessage.error('获取列表数据失败')
  }
}

const handleClassChange = async () => {
  query.examId = null
  analysis.value = null
  globalViewRequested.value = false

  if (!query.classId) return

  try {
    // 重新加载该班级下的考试列表（更精确）
    const res = await request.get('/teacher/analysis/exams', {
      params: {
        classId: query.classId,
        courseId: query.courseId
      }
    })
    if (res.code === 1) {
      exams.value = res.data
    }
    fetchAnalysis()
  } catch (e) {
    console.error(e)
  }
}

const handleViewGlobal = () => {
  globalViewRequested.value = true
  fetchAnalysis()
}

const handleExamChange = () => {
  if (query.examId === null) {
    globalViewRequested.value = true
  }
  fetchAnalysis()
}

const fetchAnalysis = async () => {
  if (!query.classId) return

  try {
    const res = await request.get(`/teacher/analysis/class/${query.classId}`, {
      params: {
        examId: query.examId,
        courseId: query.courseId
      }
    })
    if (res.code === 1) {
      analysis.value = res.data
    }
  } catch (e) {
    console.error(e)
    ElMessage.error('获取分析数据失败')
  }
}

const getDistPercentage = (count) => {
  if (!analysis.value) return 0
  const total = analysis.value.scoreDistribution.reduce((acc, cur) => acc + cur.count, 0)
  return total === 0 ? 0 : Math.round((count / total) * 100)
}

onMounted(() => {
  if (props.isEmbedded) {
    if (props.classId) {
      handleClassChange()
    }
  } else {
    fetchCourses()
  }
})
</script>

<style scoped>
.teacher-analysis-page {
  padding: 20px;
}

.query-form {
  background-color: #f5f7fa;
  padding: 20px;
  border-radius: 4px;
  margin-bottom: 20px;
}

.stat-value {
  font-size: 36px;
  font-weight: bold;
  color: #67c23a;
  text-align: center;
  height: 100px;
  display: flex;
  justify-content: center;
  align-items: center;
}

.total-score-suffix {
  font-size: 18px;
  color: #909399;
  margin-left: 5px;
  margin-top: 10px;
}

.stat-value.zero-value {
  color: #909399;
}

.chart-container {
  height: 250px;
  display: flex;
  flex-direction: column;
}

.analysis-chart {
  flex: 1;
}

.chart-hint {
  font-size: 12px;
  color: #909399;
  text-align: center;
  margin-top: 10px;
}

.dist-container {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.dist-item {
  display: flex;
  align-items: center;
}

.dist-label {
  width: 80px;
  font-size: 14px;
}

.el-progress {
  flex: 1;
}

.card-header {
  font-weight: bold;
}

.header-tip {
  font-size: 12px;
  font-weight: normal;
  color: #909399;
  margin-left: 8px;
}

.all-exams-tip {
  padding: 20px 0;
}

.empty-tip {
  margin-top: 50px;
}

.accuracy-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.percentage-text {
  font-size: 12px;
  min-width: 35px;
}

.no-data-mini {
  text-align: center;
  color: #909399;
  padding: 20px 0;
}
</style>
