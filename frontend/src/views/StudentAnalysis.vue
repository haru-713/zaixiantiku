<template>
  <div class="student-analysis-page">
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <template #header>
            <div class="card-header">
              <span>总练习次数</span>
            </div>
          </template>
          <div class="stat-value">{{ report.totalPracticeCount }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <template #header>
            <div class="card-header">
              <span>累计答题数</span>
            </div>
          </template>
          <div class="stat-value">{{ report.totalQuestionCount }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <template #header>
            <div class="card-header">
              <span>待掌握错题</span>
            </div>
          </template>
          <div class="stat-value mistake-count">{{ report.mistakeCount }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <template #header>
            <div class="card-header">
              <span>平均正确率</span>
            </div>
          </template>
          <div class="stat-value">
            <el-progress 
              type="circle" 
              :percentage="Math.round(report.avgAccuracy * 100)" 
              :color="customColors"
              :width="80"
            />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>答题趋势</span>
            </div>
          </template>
          <el-table :data="report.trend" style="width: 100%" height="300">
            <el-table-column prop="date" label="日期" />
            <el-table-column label="正确率">
              <template #default="scope">
                <el-progress :percentage="Math.round(scope.row.accuracy * 100)" />
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>知识点掌握度</span>
            </div>
          </template>
          <el-table :data="report.knowledgeMastery" style="width: 100%" height="300">
            <el-table-column prop="knowledgeName" label="知识点" />
            <el-table-column label="正确率">
              <template #default="scope">
                <div class="accuracy-wrapper">
                  <el-progress :percentage="Math.round(scope.row.accuracy * 100)" status="success" />
                  <span class="accuracy-text">{{ Math.round(scope.row.accuracy * 100) }}%</span>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'

const report = ref({
  totalPracticeCount: 0,
  totalQuestionCount: 0,
  avgAccuracy: 0,
  trend: [],
  knowledgeMastery: []
})

const customColors = [
  { color: '#f56c6c', percentage: 20 },
  { color: '#e6a23c', percentage: 40 },
  { color: '#5cb87a', percentage: 60 },
  { color: '#1989fa', percentage: 80 },
  { color: '#6f7ad3', percentage: 100 },
]

const fetchReport = async () => {
  try {
    const res = await request.get('/student/analysis/report')
    if (res.code === 1) {
      report.value = res.data
    }
  } catch (e) {
    console.error(e)
    ElMessage.error('获取学习报告失败')
  }
}

onMounted(() => {
  fetchReport()
})
</script>

<style scoped>
.student-analysis-page {
  padding: 20px;
}
.stat-card {
  text-align: center;
}
.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: #409eff;
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100px;
}
.mistake-count {
  color: #f56c6c;
}
.card-header {
  font-weight: bold;
}
.accuracy-wrapper {
  display: flex;
  align-items: center;
  gap: 10px;
}
.accuracy-wrapper .el-progress {
  flex: 1;
}
.accuracy-text {
  font-size: 14px;
  color: #67c23a;
  min-width: 45px;
  text-align: right;
}
</style>
