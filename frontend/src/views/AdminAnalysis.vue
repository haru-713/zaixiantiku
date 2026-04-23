<template>
  <div class="admin-analysis-page">
    <!-- 课程选择器 -->
    <el-card shadow="hover" class="filter-card">
      <el-form :inline="true">
        <el-form-item label="选择课程">
          <el-select v-model="courseId" placeholder="请选择课程" @change="fetchGlobalData" style="width: 250px">
            <el-option v-for="item in courses" :key="item.id" :label="item.courseName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchGlobalData">刷新数据</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 概览卡片 -->
    <el-row :gutter="20" class="overview-cards">
      <el-col :span="6">
        <el-card shadow="hover">
          <template #header>累计考试场次</template>
          <div class="stat-value">{{ globalStats.totalExams }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <template #header>课程选课人数</template>
          <div class="stat-value">{{ globalStats.totalStudents }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <template #header>课程平均分</template>
          <div class="stat-value">{{ (globalStats.averageScore || 0).toFixed(2) }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <template #header>课程及格率</template>
          <div class="stat-value">
            <el-progress type="circle" :percentage="Math.round((globalStats.passRate || 0) * 100)" :width="80" />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 考试趋势分析 -->
    <el-row :gutter="20" class="chart-card">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>平均分趋势</span>
            </div>
          </template>
          <div class="chart-container">
            <v-chart class="trend-chart" :option="scoreTrendOption" autoresize />
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>及格率趋势</span>
            </div>
          </template>
          <div class="chart-container">
            <v-chart class="trend-chart" :option="passRateTrendOption" autoresize />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 班级成绩对比 -->
    <el-card shadow="hover" class="chart-card">
      <template #header>
        <div class="card-header">
          <span>班级成绩对比</span>
          <div class="header-action">
            <span class="filter-label">筛选考试：</span>
            <el-select v-model="selectedExamId" placeholder="全部考试" clearable @change="fetchClassAnalysis"
              style="width: 200px">
              <el-option label="全部考试" :value="null" />
              <el-option v-for="item in globalStats.recentExams" :key="item.id" :label="item.examName"
                :value="item.id" />
            </el-select>
          </div>
        </div>
      </template>
      <el-table :data="classPerformanceData" style="width: 100%" border stripe v-loading="classLoading">
        <el-table-column prop="className" label="班级名称" min-width="150" />
        <el-table-column label="平均分" width="120" align="center">
          <template #default="scope">
            <span class="score-text">{{ (scope.row.averageScore || 0).toFixed(1) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="及格率" width="250">
          <template #default="scope">
            <div class="progress-cell">
              <el-progress :percentage="Math.round((scope.row.passRate || 0) * 100)" :show-text="false" />
              <span class="percentage-text">{{ Math.round((scope.row.passRate || 0) * 100) }}%</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="最高分" width="120" align="center">
          <template #default="scope">
            <span>{{ scope.row.maxScore || 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" align="center">
          <template #default="scope">
            <el-button type="primary" link @click="viewClassDetail(scope.row.classId)">查看详情分析</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 考试数据概览 -->
    <el-card shadow="hover" class="chart-card">
      <template #header>
        <div class="card-header">
          <span>考试数据概览 (各场考试独立数据)</span>
        </div>
      </template>
      <el-table :data="globalStats.recentExams" style="width: 100%" border stripe>
        <el-table-column prop="examName" label="考试名称" min-width="200" />
        <el-table-column label="参与人数" width="120" align="center">
          <template #default="scope">
            <el-tag v-if="scope.row.status === 'empty'" type="info" size="small">无人参加</el-tag>
            <span v-else>{{ scope.row.participantCount }}</span>
          </template>
        </el-table-column>
        <el-table-column label="平均分 (得分/满分)" width="180" align="center">
          <template #default="scope">
            <span class="score-text">{{ (scope.row.averageScore || 0).toFixed(1) }}</span>
            <span class="max-score" v-if="scope.row.paperTotalScore"> / {{ scope.row.paperTotalScore }}</span>
          </template>
        </el-table-column>
        <el-table-column label="及格率" width="200">
          <template #default="scope">
            <div class="progress-cell">
              <el-progress :percentage="Math.round((scope.row.passRate || 0) * 100)" :show-text="false" />
              <span class="percentage-text">{{ Math.round((scope.row.passRate || 0) * 100) }}%</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="优秀率 (≥90%)" width="200">
          <template #default="scope">
            <div class="progress-cell">
              <el-progress :percentage="Math.round((scope.row.excellentRate || 0) * 100)" :show-text="false" color="#67C23A" />
              <span class="percentage-text">{{ Math.round((scope.row.excellentRate || 0) * 100) }}%</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="最高分/最低分" width="150" align="center">
          <template #default="scope">
            <template v-if="scope.row.status === 'empty'">
              <span style="color: #909399">-</span>
            </template>
            <template v-else>
              <span style="color: #67C23A">{{ scope.row.maxScore }}</span>
              <span style="margin: 0 4px">/</span>
              <span style="color: #F56C6C">{{ scope.row.minScore }}</span>
            </template>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 详细分析抽屉 -->
    <el-drawer v-model="detailVisible" :title="currentClassName + ' - 详细分析'" size="70%" destroy-on-close>
      <teacher-analysis :class-id="currentClassId" :course-id="courseId" :exam-id="selectedExamId" is-embedded />
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'
import TeacherAnalysis from './TeacherAnalysis.vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart } from 'echarts/charts'
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
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent
])

const courseId = ref(null)
const selectedExamId = ref(null)
const courses = ref([])
const globalStats = ref({
  totalExams: 0,
  totalStudents: 0,
  averageScore: 0,
  passRate: 0,
  classPerformance: [],
  recentExams: [],
  scoreTrend: [],
  passRateTrend: []
})

const classPerformanceData = ref([])
const classLoading = ref(false)

const scoreTrendOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  xAxis: { type: 'category', data: globalStats.value.scoreTrend.map(t => t.examName) },
  yAxis: { type: 'value', name: '平均分' },
  series: [{
    data: globalStats.value.scoreTrend.map(t => t.value.toFixed(1)),
    type: 'line',
    smooth: true,
    color: '#409EFF'
  }]
}))

const passRateTrendOption = computed(() => ({
  tooltip: { trigger: 'axis', formatter: '{b}<br/>及格率: {c}%' },
  xAxis: { type: 'category', data: globalStats.value.passRateTrend.map(t => t.examName) },
  yAxis: { type: 'value', name: '及格率', max: 100 },
  series: [{
    data: globalStats.value.passRateTrend.map(t => t.value.toFixed(1)),
    type: 'line',
    smooth: true,
    color: '#67C23A'
  }]
}))

const detailVisible = ref(false)
const currentClassId = ref(null)
const currentClassName = ref('')

const fetchCourses = async () => {
  try {
    const res = await request.get('/courses/managed')
    if (res.code === 1) {
      courses.value = res.data
      if (courses.value.length > 0) {
        courseId.value = courses.value[0].id
        fetchGlobalData()
      }
    }
  } catch (e) {
    console.error(e)
  }
}

const fetchGlobalData = async () => {
  if (!courseId.value) return
  selectedExamId.value = null // 重置考试筛选
  try {
    const res = await request.get('/admin/analysis/global', {
      params: { courseId: courseId.value }
    })
    if (res.code === 1) {
      globalStats.value = res.data
      classPerformanceData.value = res.data.classPerformance
    }
  } catch (e) {
    console.error(e)
    ElMessage.error('获取全局分析数据失败')
  }
}

const fetchClassAnalysis = async () => {
  if (!courseId.value) return
  if (!selectedExamId.value) {
    classPerformanceData.value = globalStats.value.classPerformance
    return
  }

  classLoading.value = true
  try {
    // 借用 getClassAnalysis 的逻辑，由于它返回的是单个班级的
    // 我们需要一个能按考试获取所有班级对比的接口。
    // 如果没有专门接口，我们可以遍历班级调用接口（虽然效率低但可行），
    // 或者我们直接在后端 getGlobalAnalysis 中支持 examId 参数。
    const res = await request.get('/admin/analysis/global', {
      params: {
        courseId: courseId.value,
        examId: selectedExamId.value // 假设后端支持此参数，我们稍后去后端加上
      }
    })
    if (res.code === 1) {
      classPerformanceData.value = res.data.classPerformance
    }
  } catch (e) {
    console.error(e)
    ElMessage.error('获取班级对比数据失败')
  } finally {
    classLoading.value = false
  }
}

const viewClassDetail = (classId) => {
  const cls = globalStats.value.classPerformance.find(c => c.classId === classId)
  currentClassId.value = classId
  currentClassName.value = cls?.className || ''
  detailVisible.value = true
}

onMounted(() => {
  fetchCourses()
})
</script>

<style scoped>
.admin-analysis-page {
  padding: 20px;
}

.filter-card {
  margin-bottom: 20px;
}

.overview-cards {
  margin-bottom: 20px;
}

.stat-value {
  font-size: 32px;
  font-weight: bold;
  color: #409eff;
  text-align: center;
  height: 80px;
  display: flex;
  justify-content: center;
  align-items: center;
}

.chart-card {
  margin-top: 20px;
}

.score-text {
  font-weight: bold;
  color: #67c23a;
}

.max-score {
  font-size: 12px;
  color: #909399;
  margin-left: 2px;
}

.progress-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.progress-cell .el-progress {
  flex: 1;
}

.percentage-text {
  font-size: 12px;
  min-width: 35px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
