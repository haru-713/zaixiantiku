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
          <el-option label="待阅卷" :value="1" />
          <el-option label="已阅卷" :value="2" />
        </el-select>
        <el-button type="primary" @click="handleQuery">查询</el-button>
      </div>

      <div v-loading="loading" class="marking-list-container">
        <div v-if="groupedExams.length === 0" class="empty-tip">
          <el-empty :description="query.status === 1 ? '暂无待阅卷任务' : '暂无已阅卷记录'" />
        </div>
        
        <el-collapse v-else v-model="activeExams">
          <el-collapse-item v-for="exam in groupedExams" :key="exam.examId" :name="exam.examId">
            <template #title>
              <div class="exam-header">
                <span class="exam-name">{{ exam.examName }}</span>
                <el-tag size="small" type="info" class="paper-tag">{{ exam.paperName }}</el-tag>
                
                <div class="exam-info">
                  <span class="teacher-info" v-if="exam.courseTeacherNames">
                    阅卷人：{{ exam.courseTeacherNames }}
                  </span>
                  <div class="progress-info">
                    <span class="progress-label">阅卷进度：</span>
                    <el-progress 
                      :percentage="Math.round((exam.markedExamCount / exam.totalExamCount) * 100)" 
                      :format="() => `${exam.markedExamCount}/${exam.totalExamCount}`"
                      style="width: 150px"
                      :stroke-width="12"
                      :status="exam.markedExamCount === exam.totalExamCount ? 'success' : ''"
                    />
                  </div>
                </div>

                <div class="exam-stats">
                  <el-tag v-if="query.status === 1" type="danger" effect="dark" round size="small">
                    待阅：{{ exam.records.length }} 份
                  </el-tag>
                  <el-tag v-else type="success" effect="plain" round size="small">
                    已阅：{{ exam.records.length }} 份
                  </el-tag>
                </div>
              </div>
            </template>

            <el-table :data="exam.records" style="width: 100%">
              <el-table-column prop="studentName" label="学生姓名" width="150" />
              <el-table-column label="提交时间" width="200">
                <template #default="scope">
                  {{ formatDateTime(scope.row.submitTime) }}
                </template>
              </el-table-column>
              <el-table-column label="状态" width="120">
                <template #default="scope">
                  <el-tag :type="scope.row.status === 1 ? 'warning' : 'success'" size="small">
                    {{ scope.row.status === 1 ? '待阅卷' : '已阅卷' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" min-width="120">
                <template #default="scope">
                  <el-button type="primary" size="small" @click="openMarking(scope.row)">
                    {{ scope.row.status === 1 ? '开始阅卷' : '查看详情' }}
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-collapse-item>
        </el-collapse>
      </div>

      <div class="pagination-container">
        <el-pagination :current-page="query.page" :page-size="query.size" :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper" :total="total" @size-change="handleSizeChange"
          @current-change="handleCurrentChange" />
      </div>
    </el-card>

    <!-- 阅卷/详情对话框 -->
    <el-dialog v-model="markingVisible" title="考卷阅卷" width="1000px" fullscreen>
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
          <el-button v-if="currentRecord.status === 1" type="primary" :loading="submitting" @click="submitMark">提交阅卷</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({
  page: 1,
  size: 50, // 增加单页数量，方便分组展示更多数据
  courseId: null,
  status: 1
})

const activeExams = ref([])
const courseOptions = ref([])

// 按考试 ID 分组数据
const groupedExams = computed(() => {
  const groups = {}
  list.value.forEach(record => {
    if (!groups[record.examId]) {
      groups[record.examId] = {
        examId: record.examId,
        examName: record.examName,
        paperName: record.paperName,
        courseTeacherNames: record.courseTeacherNames,
        totalExamCount: record.totalExamCount,
        markedExamCount: record.markedExamCount,
        records: []
      }
    }
    groups[record.examId].records.push(record)
  })
  return Object.values(groups)
})

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
    const res = await request.get('/courses/managed')
    courseOptions.value = res.data
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
    ElMessage.success('阅卷成功')
    markingVisible.value = false
    fetchList()
  } catch (e) {
    console.error('阅卷失败:', e)
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
.exam-header {
  display: flex;
  align-items: center;
  width: 100%;
  padding-right: 20px;
}
.exam-name {
  font-weight: bold;
  font-size: 16px;
  margin-right: 15px;
  color: #303133;
}
.paper-tag {
  margin-right: 20px;
}
.exam-info {
  display: flex;
  align-items: center;
  gap: 30px;
  margin-right: auto;
  font-size: 14px;
  color: #606266;
}
.teacher-info {
  color: #409eff;
  background-color: #ecf5ff;
  padding: 2px 10px;
  border-radius: 4px;
}
.progress-info {
  display: flex;
  align-items: center;
}
.progress-label {
  margin-right: 8px;
}
.exam-stats {
  display: flex;
  gap: 10px;
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
