<template>
  <div class="exam-marking-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>阅卷管理</span>
        </div>
      </template>

      <div class="search-bar">
        <el-select v-model="query.courseId" placeholder="按课程筛选" clearable style="width: 200px; margin-right: 10px" @change="handleQuery">
          <el-option v-for="c in courseOptions" :key="c.id" :label="c.courseName" :value="c.id" />
        </el-select>
        <el-select v-model="query.status" placeholder="状态" clearable style="width: 150px; margin-right: 10px" @change="handleQuery">
          <el-option label="待批阅" :value="1" />
          <el-option label="已批阅" :value="2" />
        </el-select>
        <el-button type="primary" @click="handleQuery">查询</el-button>
      </div>

      <el-table v-loading="loading" :data="list" style="width: 100%; margin-top: 16px">
        <el-table-column prop="examName" label="考试名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="paperName" label="试卷名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="studentName" label="学生" width="120" />
        <el-table-column label="提交时间" width="180">
          <template #default="scope">
            {{ formatDateTime(scope.row.submitTime) }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.status === 1 ? 'warning' : 'success'">
              {{ scope.row.status === 1 ? '待批阅' : '已批阅' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="scope">
            <el-button type="primary" size="small" @click="openMarking(scope.row)">
              {{ scope.row.status === 1 ? '批阅' : '查看' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <el-pagination :current-page="query.page" :page-size="query.size" :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper" :total="total" @size-change="handleSizeChange"
          @current-change="handleCurrentChange" />
      </div>
    </el-card>

    <!-- 批阅/详情对话框 -->
    <el-dialog v-model="markingVisible" title="考卷批阅" width="1000px" fullscreen>
      <div v-if="markingData" class="marking-container">
        <div class="marking-header">
          <span class="m-title">{{ markingData.examName }} - {{ currentRecord.studentName }}</span>
          <div class="m-score">当前得分：<span class="score-num">{{ markingData.totalScore }}</span></div>
        </div>

        <div class="marking-body">
          <div v-for="(item, index) in markingData.answers" :key="index" class="marking-item">
            <div class="q-header">
              <span class="q-index">{{ index + 1 }}.</span>
              <el-tag size="small" type="info">{{ item.typeName }}</el-tag>
              <span class="q-score-limit">(分值：{{ item.score }}分)</span>
            </div>
            
            <div class="q-content" v-html="item.content"></div>
            
            <div v-if="item.options && item.options.length" class="q-options">
              <div v-for="(opt, oIdx) in item.options" :key="oIdx" class="opt-item">
                {{ opt }}
              </div>
            </div>

            <div class="answer-row">
              <div class="ans-col">
                <div class="ans-label">学生答案：</div>
                <div class="ans-val">{{ item.userAnswer || '未作答' }}</div>
              </div>
              <div class="ans-col">
                <div class="ans-label">标准答案：</div>
                <div class="ans-val correct">{{ item.correctAnswer }}</div>
              </div>
            </div>

            <div class="marking-action" v-if="currentRecord.status === 1">
              <span class="mark-label">评分：</span>
              <el-input-number v-model="markScores[item.questionId]" :min="0" :max="item.score" size="small" />
              <el-input v-model="markComments[item.questionId]" placeholder="评语" size="small" style="width: 300px; margin-left: 10px" />
            </div>
            <div class="marked-info" v-else>
              <span class="mark-label">得分：</span>
              <span class="score-num">{{ item.score }}</span>
            </div>
          </div>
        </div>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="markingVisible = false">取消</el-button>
          <el-button v-if="currentRecord.status === 1" type="primary" :loading="submitting" @click="submitMark">提交批阅</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({
  page: 1,
  size: 10,
  courseId: null,
  status: 1
})

const courseOptions = ref([])

const markingVisible = ref(false)
const markingData = ref(null)
const currentRecord = ref(null)
const submitting = ref(false)

// 存储评分和评语
const markScores = reactive({})
const markComments = reactive({})

const fetchList = async () => {
  loading.value = true
  try {
    const res = await request.get('/teacher/exam-records/pending', { params: query })
    list.value = res.data.list
    total.value = res.data.total
  } catch (e) {
    console.error('获取列表失败:', e)
  } finally {
    loading.value = false
  }
}

const fetchCourseOptions = async () => {
  try {
    const res = await request.get('/courses', { params: { page: 1, size: 100 } })
    courseOptions.value = res.data.list
  } catch (e) {
    console.error(e)
  }
}

const handleQuery = () => {
  query.page = 1
  fetchList()
}

const handleSizeChange = (val) => {
  query.size = val
  fetchList()
}

const handleCurrentChange = (val) => {
  query.page = val
  fetchList()
}

const openMarking = async (row) => {
  currentRecord.value = row
  try {
    // 复用学生详情接口获取考卷内容
    const res = await request.get(`/student/exam-records/${row.id}`)
    markingData.value = res.data
    
    // 初始化评分和评语
    markingData.value.answers.forEach(ans => {
      markScores[ans.questionId] = ans.score || 0
      markComments[ans.questionId] = ''
    })
    
    markingVisible.value = true
  } catch (e) {
    console.error('获取详情失败:', e)
  }
}

const submitMark = async () => {
  submitting.value = true
  try {
    const scores = Object.keys(markScores).map(qId => ({
      questionId: Number(qId),
      score: markScores[qId],
      comment: markComments[qId]
    }))
    
    await request.put(`/teacher/exam-records/${currentRecord.value.id}/mark`, { scores })
    ElMessage.success('批阅成功')
    markingVisible.value = false
    fetchList()
  } catch (e) {
    console.error('批阅失败:', e)
  } finally {
    submitting.value = false
  }
}

const formatDateTime = (value) => {
  if (!value) return '-'
  const str = String(value)
  if (str.includes('T')) {
    const replaced = str.replace('T', ' ')
    return replaced.length >= 19 ? replaced.slice(0, 19) : replaced
  }
  return str
}

onMounted(() => {
  fetchList()
  fetchCourseOptions()
})
</script>

<style scoped>
.exam-marking-page {
  padding: 20px;
}
.card-header {
  font-weight: bold;
}
.search-bar {
  margin-bottom: 20px;
}
.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
.marking-container {
  padding: 0 40px;
  max-width: 1200px;
  margin: 0 auto;
}
.marking-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 0;
  border-bottom: 2px solid #ebeef5;
  position: sticky;
  top: 0;
  background: white;
  z-index: 10;
}
.m-title {
  font-size: 20px;
  font-weight: bold;
}
.score-num {
  font-size: 24px;
  color: #f56c6c;
  font-weight: bold;
}
.marking-item {
  padding: 30px 0;
  border-bottom: 1px dashed #dcdfe6;
}
.q-header {
  margin-bottom: 15px;
  display: flex;
  align-items: center;
  gap: 10px;
}
.q-index {
  font-weight: bold;
  font-size: 18px;
}
.q-score-limit {
  color: #909399;
  font-size: 14px;
}
.q-content {
  margin-bottom: 20px;
  font-size: 16px;
  line-height: 1.6;
}
.opt-item {
  margin-bottom: 10px;
}
.answer-row {
  display: flex;
  background: #f8f9fa;
  padding: 15px;
  border-radius: 4px;
  gap: 40px;
  margin-bottom: 20px;
}
.ans-label {
  font-weight: bold;
  margin-bottom: 5px;
  color: #606266;
}
.ans-val {
  font-size: 16px;
}
.correct {
  color: #67c23a;
  font-weight: bold;
}
.marking-action {
  background: #f0f9eb;
  padding: 15px;
  border-radius: 4px;
  display: flex;
  align-items: center;
}
.mark-label {
  font-weight: bold;
  color: #67c23a;
  margin-right: 10px;
}
.marked-info {
  padding: 10px 0;
  font-size: 18px;
}
</style>
