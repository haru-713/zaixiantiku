<template>
  <div class="exam-marking-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>阅卷管理</span>
        </div>
      </template>

      <div class="search-bar">
        <el-select v-model="query.courseId" placeholder="按课程筛选" clearable style="width: 200px; margin-right: 10px"
          @change="handleQuery">
          <el-option v-for="c in courseOptions" :key="c.id" :label="c.courseName" :value="c.id" />
        </el-select>
        <el-select v-model="query.status" placeholder="状态" clearable style="width: 150px; margin-right: 10px"
          @change="handleQuery">
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
                    <el-progress :percentage="Math.round((exam.markedExamCount / exam.totalExamCount) * 100)"
                      :format="() => `${exam.markedExamCount}/${exam.totalExamCount}`" style="width: 150px"
                      :stroke-width="12" :status="exam.markedExamCount === exam.totalExamCount ? 'success' : ''" />
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
              <el-table-column label="防作弊异常" width="220">
                <template #default="scope">
                  <div v-if="scope.row.cheatCount > 0 || scope.row.forceSubmit" class="cheat-info">
                    <el-tooltip v-if="scope.row.cheatCount >= 3" content="该生切屏次数过多，已被强制交卷" placement="top">
                      <el-tag type="danger" size="small" class="cheat-tag" effect="dark">
                        ⚠️ 可疑：切屏 {{ scope.row.cheatCount }} 次
                      </el-tag>
                    </el-tooltip>
                    <el-tag v-else type="warning" size="small" class="cheat-tag">
                      切屏: {{ scope.row.cheatCount }} 次
                    </el-tag>
                    <el-tag v-if="scope.row.forceSubmit" type="danger" size="small" class="cheat-tag">
                      强制交卷
                    </el-tag>
                  </div>
                  <span v-else class="normal-status">无异常</span>
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
          <div class="header-left">
            <span class="m-title">{{ markingData.examName }} - {{ currentRecord.studentName }}</span>
            <div class="marking-nav" v-if="pendingRecordIds.length > 0">
              <el-button-group>
                <el-button type="primary" plain icon="ArrowLeft" :disabled="currentNavIndex <= 0"
                  @click="handlePrevMarking">上一份</el-button>
                <el-button type="primary" plain :disabled="currentNavIndex >= pendingRecordIds.length - 1"
                  @click="handleNextMarking">
                  下一份 <el-icon class="el-icon--right">
                    <ArrowRight />
                  </el-icon>
                </el-button>
              </el-button-group>
              <span class="nav-progress">
                进度：第 {{ currentNavIndex + 1 }} 份 / 共 {{ pendingRecordIds.length }} 份
              </span>
            </div>
          </div>
          <div class="m-score">当前得分：<span class="score-num">{{ markingData.totalScore }}</span></div>
        </div>

        <div class="marking-body">
          <div v-if="subjectiveAnswers.length === 0" class="no-subjective">
            <el-empty description="本试卷无主观题，已由系统自动完成全部批阅" />
          </div>
          <div v-else v-for="(item, index) in subjectiveAnswers" :key="item.questionId" class="marking-item">
            <div class="q-header">
              <span class="q-index">{{ index + 1 }}.</span>
              <el-tag size="small" type="info">{{ item.typeName }}</el-tag>
              <span class="q-score-limit">(分值：{{ item.maxScore }}分)</span>
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
              <el-input-number v-model="markScores[item.questionId]" :min="0" :max="item.maxScore" size="small" />
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
          <el-button v-if="currentRecord.status === 1" type="primary" :loading="submitting"
            @click="submitMark">提交阅卷</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed, watch } from 'vue'
import request from '@/utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, ArrowRight } from '@element-plus/icons-vue'

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

// 过滤出主观题（供教师批阅），排除客观题
const subjectiveAnswers = computed(() => {
  if (!markingData.value || !markingData.value.answers) return []
  const objectiveKeywords = ['单选', '多选', '判断']
  return markingData.value.answers.filter(item => {
    const typeName = item.typeName || ''
    return !objectiveKeywords.some(kw => typeName.includes(kw))
  })
})

// 导航逻辑
const pendingRecordIds = ref([])
const currentNavIndex = computed(() => {
  if (!currentRecord.value) return -1
  return pendingRecordIds.value.indexOf(currentRecord.value.id)
})

// 存储评分
const markScores = reactive({})

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
    // 获取同场考试的所有待阅ID列表用于导航
    const idsRes = await request.get(`/teacher/exams/${row.examId}/pending-ids`)
    pendingRecordIds.value = idsRes.data

    await loadMarkingData(row.id)
    markingVisible.value = true
  } catch (e) {
    console.error('获取详情失败:', e)
  }
}

const loadMarkingData = async (recordId) => {
  loading.value = true
  try {
    const res = await request.get(`/student/exam-records/${recordId}`)
    markingData.value = res.data

    // 如果 currentRecord 信息不全，补全它
    if (res.data.studentName) {
      currentRecord.value = { ...currentRecord.value, studentName: res.data.studentName }
    }

    // 清空旧数据
    Object.keys(markScores).forEach(key => delete markScores[key])

    // 仅初始化主观题的评分数据（客观题由系统自动评分，无需教师操作）
    const objectiveKeywords = ['单选', '多选', '判断']
    markingData.value.answers.forEach(ans => {
      const typeName = ans.typeName || ''
      const isSubjective = !objectiveKeywords.some(kw => typeName.includes(kw))
      if (isSubjective) {
        markScores[ans.questionId] = ans.score || 0
      }
    })
  } finally {
    loading.value = false
  }
}

const handlePrevMarking = async () => {
  if (currentNavIndex.value > 0) {
    const prevId = pendingRecordIds.value[currentNavIndex.value - 1]
    await loadMarkingData(prevId)
    // 尝试更新基本信息
    const record = list.value.find(r => r.id === prevId)
    if (record) currentRecord.value = record
  }
}

const handleNextMarking = async () => {
  if (currentNavIndex.value < pendingRecordIds.value.length - 1) {
    const nextId = pendingRecordIds.value[currentNavIndex.value + 1]
    await loadMarkingData(nextId)
    // 尝试更新基本信息
    const record = list.value.find(r => r.id === nextId)
    if (record) currentRecord.value = record
  }
}

const submitMark = async () => {
  submitting.value = true
  try {
    // 仅提交主观题的评分数据（客观题已由系统自动评分）
    const objectiveKeywords = ['单选', '多选', '判断']
    const scores = markingData.value.answers
      .filter(ans => {
        const typeName = ans.typeName || ''
        return !objectiveKeywords.some(kw => typeName.includes(kw))
      })
      .map(ans => ({
        questionId: Number(ans.questionId),
        score: markScores[ans.questionId] || 0
      }))

    await request.put(`/teacher/exam-records/${currentRecord.value.id}/mark`, { scores })
    ElMessage.success('批阅保存成功')

    // 重新获取该考试的待阅 ID 列表，以获取最新状态
    const idsRes = await request.get(`/teacher/exams/${currentRecord.value.examId}/pending-ids`)
    pendingRecordIds.value = idsRes.data

    if (pendingRecordIds.value.length > 0) {
      // 还有待阅试卷，自动跳转到下一个（即当前列表的第一个）
      const nextId = pendingRecordIds.value[0]
      // 尝试在背景列表中寻找该记录对象，以便获取学生姓名等基本信息
      const nextRecord = list.value.find(r => r.id === nextId)
      if (nextRecord) {
        currentRecord.value = nextRecord
        await loadMarkingData(nextId)
      } else {
        // 如果背景列表（当前页）没找到，说明可能在其他页，
        // 这里为了简单，先关闭对话框并提示用户刷新列表，或者我们可以直接根据 ID 加载
        // 但由于 currentRecord 需要 VO 对象，最稳妥的是刷新列表后让用户点
        // 不过通常 50 条够用了。为了体验，我们尝试直接加载详情。
        await loadMarkingData(nextId)
        // 注意：此时 currentRecord 还是旧的，姓名等信息可能不对。
        // 我们在 loadMarkingData 里尝试补全一些信息
        currentRecord.value = { ...currentRecord.value, id: nextId, studentName: '下一位考生' }
      }
    } else {
      ElMessage.info('该考试所有试卷已批阅完成')
      markingVisible.value = false
    }
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

.cheat-info {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.cheat-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.cheat-tag {
  width: fit-content;
}

.normal-status {
  color: #909399;
  font-size: 12px;
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

.header-left {
  display: flex;
  align-items: center;
  gap: 30px;
}

.marking-nav {
  display: flex;
  align-items: center;
  gap: 15px;
}

.nav-progress {
  font-size: 14px;
  color: #909399;
  font-weight: normal;
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
