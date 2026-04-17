<template>
  <div class="admin-analysis-page">
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
          <template #header>覆盖学生人数</template>
          <div class="stat-value">{{ globalStats.totalStudents }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <template #header>全校平均分</template>
          <div class="stat-value">{{ (globalStats.averageScore || 0).toFixed(2) }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <template #header>全校及格率</template>
          <div class="stat-value">
            <el-progress type="circle" :percentage="Math.round((globalStats.passRate || 0) * 100)" :width="80" />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 班级成绩对比 -->
    <el-card shadow="hover" class="chart-card">
      <template #header>
        <div class="card-header">
          <span>班级成绩对比</span>
          <el-button type="primary" link @click="fetchGlobalData">刷新数据</el-button>
        </div>
      </template>
      <el-table :data="globalStats.classPerformance" style="width: 100%" border stripe>
        <el-table-column prop="className" label="班级名称" min-width="150" />
        <el-table-column label="平均分" width="120" align="center">
          <template #default="scope">
            <span class="score-text">{{ (scope.row.averageScore || 0).toFixed(1) }}</span>
            <span class="max-score" v-if="scope.row.maxScore"> / {{ scope.row.maxScore }}</span>
          </template>
        </el-table-column>
        <el-table-column label="及格率" width="250">
          <template #default="scope">
            <div class="progress-cell">
              <el-progress :percentage="Math.round((scope.row.passRate || 0) * 100)" />
              <span class="percentage-text">{{ Math.round((scope.row.passRate || 0) * 100) }}%</span>
            </div>
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
          <span>考试数据概览 (包含历史数据)</span>
        </div>
      </template>
      <el-table :data="globalStats.recentExams" style="width: 100%" border stripe>
        <el-table-column prop="id" label="考试ID" width="100" align="center" />
        <el-table-column prop="examName" label="考试名称" min-width="200" />
        <el-table-column prop="participantCount" label="参与人数" width="120" align="center" />
        <el-table-column label="平均分" width="150" align="center">
          <template #default="scope">
            <span class="score-text">{{ (scope.row.averageScore || 0).toFixed(2) }}</span>
            <span class="max-score" v-if="scope.row.maxScore"> / {{ scope.row.maxScore }}</span>
          </template>
        </el-table-column>
        <el-table-column label="及格率" width="250">
          <template #default="scope">
            <div class="progress-cell">
              <el-progress :percentage="Math.round((scope.row.passRate || 0) * 100)" />
              <span class="percentage-text">{{ Math.round((scope.row.passRate || 0) * 100) }}%</span>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 详细分析抽屉 -->
    <el-drawer
      v-model="detailVisible"
      :title="currentClassName + ' - 详细分析'"
      size="70%"
      destroy-on-close
    >
      <teacher-analysis :class-id="currentClassId" is-embedded />
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'
import TeacherAnalysis from './TeacherAnalysis.vue'

const globalStats = ref({
  totalExams: 0,
  totalStudents: 0,
  averageScore: 0,
  passRate: 0,
  classPerformance: [],
  recentExams: []
})

const detailVisible = ref(false)
const currentClassId = ref(null)
const currentClassName = ref('')

const fetchGlobalData = async () => {
  try {
    const res = await request.get('/admin/analysis/global')
    if (res.code === 1) {
      globalStats.value = res.data
    }
  } catch (e) {
    console.error(e)
    ElMessage.error('获取全局分析数据失败')
  }
}

const viewClassDetail = (classId) => {
  const cls = globalStats.value.classPerformance.find(c => c.classId === classId)
  currentClassId.value = classId
  currentClassName.value = cls?.className || ''
  detailVisible.value = true
}

onMounted(() => {
  fetchGlobalData()
})
</script>

<style scoped>
.admin-analysis-page {
  padding: 20px;
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
