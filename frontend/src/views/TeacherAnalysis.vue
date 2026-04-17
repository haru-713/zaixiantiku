<template>
  <div class="teacher-analysis-page" :class="{ 'embedded': isEmbedded }">
    <el-form :inline="true" :model="query" class="query-form" v-if="!isEmbedded">
      <el-form-item label="选择班级">
        <el-select v-model="query.classId" placeholder="请选择班级" @change="handleClassChange" style="width: 200px">
          <el-option
            v-for="item in classes"
            :key="item.id"
            :label="item.className"
            :value="item.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="选择考试">
        <el-select v-model="query.examId" placeholder="全部考试" clearable @change="fetchAnalysis" style="width: 250px" :disabled="!query.classId">
          <el-option label="全部考试" :value="null" />
          <el-option
            v-for="item in exams"
            :key="item.id"
            :label="item.examName"
            :value="item.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="fetchAnalysis" :disabled="!query.classId">刷新分析</el-button>
      </el-form-item>
    </el-form>

    <div v-else class="embedded-header">
      <el-form :inline="true" :model="query">
        <el-form-item label="筛选考试">
          <el-select v-model="query.examId" placeholder="全部考试" clearable @change="fetchAnalysis" style="width: 250px">
            <el-option label="全部考试" :value="null" />
            <el-option
              v-for="item in exams"
              :key="item.id"
              :label="item.examName"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
    </div>

    <div v-if="!query.classId" class="empty-tip">
      <el-empty description="请先选择一个班级进行分析" />
    </div>

    <template v-else-if="analysis">
      <el-row :gutter="20">
        <el-col :span="8">
          <el-card shadow="hover">
            <template #header>
              <div class="card-header">
                <span>班级平均分</span>
              </div>
            </template>
            <div class="stat-value" :class="{ 'zero-value': analysis.averageScore === 0 }">
              {{ analysis.averageScore.toFixed(2) }}
            </div>
          </el-card>
        </el-col>
        <el-col :span="16">
          <el-card shadow="hover">
            <template #header>
              <div class="card-header">
                <span>成绩分布</span>
              </div>
            </template>
            <div class="dist-container" v-if="hasData">
              <div v-for="item in analysis.scoreDistribution" :key="item.range" class="dist-item">
                <div class="dist-label">{{ item.range }}</div>
                <el-progress :percentage="getDistPercentage(item.count)" :format="() => item.count + '人'" />
              </div>
            </div>
            <div v-else class="no-data-mini">暂无成绩数据</div>
          </el-card>
        </el-col>
      </el-row>

      <el-card shadow="hover" style="margin-top: 20px;">
        <template #header>
          <div class="card-header">
            <span>题目正确率分析</span>
          </div>
        </template>
        <el-table :data="analysis.questionAccuracies" style="width: 100%" border stripe v-if="analysis.questionAccuracies.length > 0">
          <el-table-column prop="questionId" label="题目ID" width="100" align="center" />
          <el-table-column prop="content" label="题目内容" show-overflow-tooltip>
            <template #default="scope">
              <div v-html="scope.row.content"></div>
            </template>
          </el-table-column>
          <el-table-column label="班级正确率" width="200">
            <template #default="scope">
              <div class="accuracy-cell">
                <el-progress 
                  :percentage="Math.round(scope.row.accuracy * 100)" 
                  :status="scope.row.accuracy < 0.6 ? 'exception' : 'success'"
                />
                <span class="percentage-text">{{ Math.round(scope.row.accuracy * 100) }}%</span>
              </div>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-else description="暂无题目分析数据" :image-size="100" />
      </el-card>
    </template>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, defineProps, watch } from 'vue'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'

const props = defineProps({
  classId: {
    type: Number,
    default: null
  },
  isEmbedded: {
    type: Boolean,
    default: false
  }
})

const query = reactive({
  classId: props.classId,
  examId: null
})

// 监听外部传入的 classId 变化
watch(() => props.classId, (newId) => {
  if (props.isEmbedded && newId) {
    query.classId = newId
    handleClassChange()
  }
})

const classes = ref([])
const exams = ref([])
const analysis = ref(null)

const hasData = computed(() => {
  if (!analysis.value) return false
  return analysis.value.scoreDistribution.some(item => item.count > 0)
})

const fetchClasses = async () => {
  // 如果是嵌入模式，不需要获取班级列表
  if (props.isEmbedded) {
    if (props.classId) {
      handleClassChange()
    }
    return
  }

  try {
    const res = await request.get('/teacher/analysis/classes')
    if (res.code === 1) {
      classes.value = res.data
      if (classes.value.length > 0 && !query.classId) {
        query.classId = classes.value[0].id
        handleClassChange()
      }
    }
  } catch (e) {
    console.error(e)
    ElMessage.error('获取班级列表失败')
  }
}

const handleClassChange = async () => {
  query.examId = null
  exams.value = []
  analysis.value = null
  
  if (!query.classId) return

  try {
    const res = await request.get(`/teacher/analysis/class/${query.classId}/exams`)
    if (res.code === 1) {
      exams.value = res.data
      // 默认不自动选考试，或者如果想自动选第一个：
      // if (exams.value.length > 0) {
      //   query.examId = exams.value[0].id
      // }
    }
    fetchAnalysis()
  } catch (e) {
    console.error(e)
  }
}

const fetchAnalysis = async () => {
  if (!query.classId) return
  
  try {
    const res = await request.get(`/teacher/analysis/class/${query.classId}`, {
      params: { examId: query.examId }
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
  fetchClasses()
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
.stat-value.zero-value {
  color: #909399;
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
