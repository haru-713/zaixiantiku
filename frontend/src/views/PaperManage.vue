<template>
  <div class="paper-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>试卷管理</span>
          <div class="header-actions">
            <el-button type="primary" @click="openManualCreate">手动组卷</el-button>
            <el-button type="success" @click="openAutoCreate">自动组卷</el-button>
          </div>
        </div>
      </template>

      <div class="search-bar">
        <el-select v-model="query.courseId" filterable remote clearable :remote-method="fetchCourseOptions"
          :loading="courseLoading" placeholder="请选择课程" style="width: 240px; margin-right: 10px"
          @visible-change="handleCourseDropdown">
          <el-option v-for="c in courseOptions" :key="c.id" :label="c.courseName" :value="c.id" />
        </el-select>
        <el-input v-model="query.keyword" placeholder="试卷名称关键字" clearable style="width: 240px; margin-right: 10px"
          @keyup.enter="handleQuery" />
        <el-select v-model="query.status" placeholder="状态" clearable style="width: 120px; margin-right: 10px">
          <el-option label="已启用" :value="1" />
          <el-option label="已禁用" :value="0" />
        </el-select>
        <el-button type="primary" @click="handleQuery">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </div>

      <el-table v-loading="loading" :data="list" style="width: 100%; margin-top: 16px">
        <el-table-column type="index" label="序号" width="60" />
        <el-table-column prop="paperName" label="试卷名称" min-width="200" show-overflow-tooltip />
        <el-table-column v-if="isAdmin" prop="courseName" label="所属课程" width="150" />
        <el-table-column v-if="isAdmin" prop="creatorName" label="创建教师" width="120" />
        <el-table-column prop="totalScore" label="总分" width="80" align="center" />
        <el-table-column label="状态" width="100" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.status === 1 ? 'success' : 'info'">
              {{ scope.row.status === 1 ? '已启用' : '已禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="160">
          <template #default="scope">
            {{ formatDateTime(scope.row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="scope">
            <el-button size="small" @click="viewDetail(scope.row)">详情</el-button>
            <el-button type="primary" size="small" @click="openEdit(scope.row)">修改</el-button>
            <el-button type="danger" size="small" @click="handleDelete(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <el-pagination :current-page="query.page" :page-size="query.size" :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper" :total="total" @size-change="handleSizeChange"
          @current-change="handleCurrentChange" />
      </div>
    </el-card>

    <!-- 手动组卷/修改试卷对话框 -->
    <el-dialog v-model="manualVisible" :title="editingId ? '修改试卷' : '手动组卷'" width="900px">
      <el-form :model="manualForm" label-width="90px">
        <el-form-item label="试卷名称" required>
          <el-input v-model="manualForm.paperName" placeholder="请输入试卷名称" />
        </el-form-item>
        <el-form-item label="所属课程" required>
          <el-select v-model="manualForm.courseId" filterable remote clearable :remote-method="fetchCourseOptions"
            :loading="courseLoading" placeholder="选择课程" style="width: 100%" @change="handleManualCourseChange"
            @visible-change="handleCourseDropdown">
            <el-option v-for="c in courseOptions" :key="c.id" :label="c.courseName" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="总分">
          <el-input-number v-model="manualForm.totalScore" :min="1" />
          <span v-if="manualForm.questions.length > 0" class="score-tip"
            :class="{ 'error': currentSum !== manualForm.totalScore || manualForm.totalScore < manualForm.questions.length }">
            当前题目分值总和: {{ currentSum }}
            <span v-if="currentSum !== manualForm.totalScore"> (与总分不符)</span>
            <span v-if="manualForm.totalScore < manualForm.questions.length"> (总分过小，至少应为 {{ manualForm.questions.length
              }})</span>
          </span>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="manualForm.remark" type="textarea" :rows="2" />
        </el-form-item>

        <div class="question-selection">
          <div class="section-title">试题选择 (已选 {{ manualForm.questions.length }} 题)</div>
          <div class="selection-actions">
            <el-button type="primary" size="small" :disabled="!manualForm.courseId"
              @click="openQuestionPicker">选择题目</el-button>
          </div>
          <el-table :data="manualForm.questions" size="small" border style="margin-top: 10px">
            <el-table-column prop="questionId" label="ID" width="80" />
            <el-table-column prop="content" label="题目内容" show-overflow-tooltip />
            <el-table-column label="分值" width="100">
              <template #default="scope">
                <el-input-number v-model="scope.row.score" :min="1" size="small" style="width: 80px" />
              </template>
            </el-table-column>
            <el-table-column label="排序" width="80">
              <template #default="scope">
                <el-input-number v-model="scope.row.sortOrder" :min="1" size="small" :controls="false"
                  style="width: 60px" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="80">
              <template #default="scope">
                <el-button type="danger" size="small" link @click="removeQuestion(scope.$index)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="manualVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitManual">保存试卷</el-button>
      </template>
    </el-dialog>

    <!-- 自动组卷对话框 -->
    <el-dialog v-model="autoVisible" title="自动组卷" width="600px">
      <el-form :model="autoForm" label-width="100px">
        <el-form-item label="试卷名称" required>
          <el-input v-model="autoForm.paperName" placeholder="请输入试卷名称" />
        </el-form-item>
        <el-form-item label="所属课程" required>
          <el-select v-model="autoForm.courseId" filterable remote clearable :remote-method="fetchCourseOptions"
            :loading="courseLoading" placeholder="选择课程" style="width: 100%" @change="handleAutoCourseChange"
            @visible-change="handleCourseDropdown">
            <el-option v-for="c in courseOptions" :key="c.id" :label="c.courseName" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="总分" required>
          <el-input-number v-model="autoForm.totalScore" :min="1" />
        </el-form-item>

        <div class="rule-section">
          <div class="section-title">组卷规则</div>
          <el-form-item label="知识点">
            <el-tree-select v-model="autoForm.rule.knowledgeIds" :data="knowledgeTree" multiple collapse-tags
              collapse-tags-tooltip :props="{ label: 'name', children: 'children' }" node-key="id"
              placeholder="请选择知识点范围" style="width: 100%" :disabled="!autoForm.courseId" />
          </el-form-item>

          <div v-for="(dist, index) in autoForm.rule.typeDistribution" :key="index" class="type-row">
            <el-form-item :label="getTypeName(dist.typeId)" label-width="80px">
              <div class="dist-inputs">
                <span>数量</span>
                <el-input-number v-model="dist.count" :min="0" size="small" />
                <span>单题分值</span>
                <el-input-number v-model="dist.scorePerQuestion" :min="1" size="small" />
              </div>
            </el-form-item>
          </div>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="autoVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitAuto">开始自动组卷</el-button>
      </template>
    </el-dialog>

    <!-- 题目选择器 (子对话框) -->
    <el-dialog v-model="pickerVisible" title="选择题目" width="800px" append-to-body>
      <div class="picker-search">
        <el-input v-model="pickerQuery.keyword" placeholder="题目内容关键字" style="width: 200px; margin-right: 10px"
          @keyup.enter="fetchPickerList" />
        <el-button type="primary" @click="fetchPickerList">查询</el-button>
      </div>
      <el-table :data="pickerList" v-loading="pickerLoading" style="margin-top: 10px"
        @selection-change="handlePickerSelection">
        <el-table-column type="selection" width="55" />
        <el-table-column type="index" label="序号" width="60" />
        <el-table-column prop="content" label="题目内容" show-overflow-tooltip />
        <el-table-column label="题型" width="100">
          <template #default="scope">{{ getTypeName(scope.row.typeId) }}</template>
        </el-table-column>
      </el-table>
      <div class="pagination-container">
        <el-pagination small layout="prev, pager, next" :total="pickerTotal" @current-change="handlePickerPageChange" />
      </div>
      <template #footer>
        <el-button @click="pickerVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmPicker">确定已选 ({{ pickerSelected.length }})</el-button>
      </template>
    </el-dialog>

    <!-- 试卷详情抽屉 -->
    <el-drawer v-model="detailVisible" title="试卷详情" size="60%">
      <div v-if="paperDetail" class="paper-detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="试卷名称">{{ paperDetail.paperName }}</el-descriptions-item>
          <el-descriptions-item label="总分">{{ paperDetail.totalScore }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ paperDetail.remark || '-' }}</el-descriptions-item>
        </el-descriptions>

        <div class="detail-questions">
          <div class="section-title">题目列表</div>
          <el-table :data="paperDetail.questions" size="small" border>
            <el-table-column prop="sortOrder" label="序号" width="60" />
            <el-table-column prop="content" label="题目内容" show-overflow-tooltip />
            <el-table-column prop="score" label="分值" width="80" />
          </el-table>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const isAdmin = computed(() => userStore.userInfo?.roleCodes?.includes('ADMIN'))

const loading = ref(false)
const saving = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({
  page: 1,
  size: 10,
  courseId: null,
  keyword: '',
  status: null
})

const courseOptions = ref([])
const courseLoading = ref(false)
const questionTypeOptions = ref([])
const knowledgeTree = ref([])

// 详情相关
const detailVisible = ref(false)
const paperDetail = ref(null)

// 手动组卷/修改相关
const manualVisible = ref(false)
const editingId = ref(null)
const manualForm = reactive({
  paperName: '',
  courseId: null,
  totalScore: 100,
  remark: '',
  questions: []
})

// 自动组卷相关
const autoVisible = ref(false)
const autoForm = reactive({
  paperName: '',
  courseId: null,
  totalScore: 100,
  rule: {
    typeDistribution: [],
    difficultyRatio: { "1": 0.3, "2": 0.5, "3": 0.2 },
    knowledgeIds: []
  }
})

// 题目选择器相关
const pickerVisible = ref(false)
const pickerLoading = ref(false)
const pickerList = ref([])
const pickerTotal = ref(0)
const pickerQuery = reactive({
  page: 1,
  size: 10,
  keyword: ''
})
const pickerSelected = ref([])

const currentSum = computed(() => {
  return manualForm.questions.reduce((sum, q) => sum + (q.score || 0), 0)
})

const fetchList = async () => {
  loading.value = true
  try {
    const res = await request.get('/papers', { params: query })
    list.value = res.data.list
    total.value = res.data.total
  } catch (e) {
    console.error('获取试卷列表失败:', e)
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  query.page = 1
  fetchList()
}

const resetQuery = () => {
  query.courseId = null
  query.keyword = ''
  handleQuery()
}

const handleSizeChange = (val) => {
  query.size = val
  fetchList()
}

const handleCurrentChange = (val) => {
  query.page = val
  fetchList()
}

const fetchCourseOptions = async () => {
  courseLoading.value = true
  try {
    // 改为调用 managed 接口，根据角色自动过滤
    const res = await request.get('/courses/managed')
    courseOptions.value = res.data || []
  } catch (e) {
    console.error('获取课程选项失败:', e)
  } finally {
    courseLoading.value = false
  }
}

const handleCourseDropdown = (visible) => {
  if (visible && courseOptions.value.length === 0) {
    fetchCourseOptions()
  }
}

const fetchQuestionTypes = async () => {
  try {
    const res = await request.get('/question-types')
    questionTypeOptions.value = res.data
  } catch (e) {
    console.error('获取题型失败:', e)
  }
}

const getTypeName = (id) => {
  const t = questionTypeOptions.value.find(x => x.id === id)
  return t ? t.typeName : id
}

const openManualCreate = () => {
  editingId.value = null
  Object.assign(manualForm, {
    paperName: '',
    courseId: null,
    totalScore: 0,
    remark: '',
    questions: []
  })
  manualVisible.value = true
}

const openEdit = async (row) => {
  try {
    const res = await request.get(`/papers/${row.id}`)
    editingId.value = row.id
    Object.assign(manualForm, {
      paperName: res.data.paperName,
      courseId: res.data.courseId,
      totalScore: res.data.totalScore,
      remark: res.data.remark,
      questions: res.data.questions.map(q => ({
        questionId: q.questionId,
        content: q.content,
        score: q.score,
        sortOrder: q.sortOrder
      }))
    })
    manualVisible.value = true
  } catch (e) {
    console.error('获取详情失败:', e)
  }
}

const handleManualCourseChange = () => {
  if (!editingId.value) {
    manualForm.questions = []
    manualForm.totalScore = 0
  }
}

const openQuestionPicker = () => {
  pickerQuery.page = 1
  pickerQuery.keyword = ''
  pickerSelected.value = []
  pickerVisible.value = true
  fetchPickerList()
}

const fetchPickerList = async () => {
  if (!manualForm.courseId) return
  pickerLoading.value = true
  try {
    const res = await request.get('/questions', {
      params: {
        courseId: manualForm.courseId,
        keyword: pickerQuery.keyword,
        page: pickerQuery.page,
        size: 50, // 增加单次查询量，尽量覆盖更多待选题
        status: 2
      }
    })
    // 过滤掉已经在 manualForm.questions 中的题目
    const existingIds = manualForm.questions.map(q => String(q.questionId))
    pickerList.value = res.data.list.filter(q => !existingIds.includes(String(q.id)))
    pickerTotal.value = res.data.total
  } catch (e) {
    console.error('获取待选题目失败:', e)
  } finally {
    pickerLoading.value = false
  }
}

const handlePickerSelection = (selection) => {
  pickerSelected.value = selection
}

const handlePickerPageChange = (val) => {
  pickerQuery.page = val
  fetchPickerList()
}

const confirmPicker = () => {
  const existingIds = manualForm.questions.map(q => q.questionId)
  const newQuestions = pickerSelected.value
    .filter(q => !existingIds.includes(q.id))
    .map(q => ({
      questionId: q.id,
      content: q.content,
      score: 5,
      sortOrder: manualForm.questions.length + 1
    }))

  manualForm.questions.push(...newQuestions)
  pickerVisible.value = false
}

const removeQuestion = (index) => {
  manualForm.questions.splice(index, 1)
}

const autoAdjustScores = () => {
  const T = manualForm.totalScore
  const questions = manualForm.questions
  const n = questions.length
  if (n === 0) return

  if (T < n) {
    ElMessage.error(`总分无效：当前有 ${n} 道题，总分至少应为 ${n}`)
    return false
  }

  const S = questions.reduce((sum, q) => sum + (q.score || 0), 0)
  if (S === 0) {
    // 如果当前总和为0，则平摊
    const base = Math.floor(T / n)
    let remain = T % n
    questions.forEach((q, i) => {
      q.score = base + (i < remain ? 1 : 0)
    })
    return true
  }

  // 1. 计算理论分值并取整
  // 确保每个 floor 至少为 1
  let floorScores = questions.map(q => {
    const base_i = (q.score || 0) / S * T
    return {
      q,
      floor: Math.max(1, Math.floor(base_i)),
      decimal: base_i - Math.floor(base_i)
    }
  })

  // 2. 计算剩余分数
  let currentFloorSum = floorScores.reduce((sum, item) => sum + item.floor, 0)
  let remain = T - currentFloorSum

  if (remain < 0) {
    // 如果初始分配就超过了总分（由于 Math.max(1, ...)），需要减少一些分值
    const sortedToReduce = [...floorScores]
      .filter(item => item.floor > 1)
      .sort((a, b) => a.decimal - b.decimal)

    for (let i = 0; i < Math.abs(remain) && i < sortedToReduce.length; i++) {
      sortedToReduce[i].floor -= 1
    }
  } else if (remain > 0) {
    // 3. 按小数部分从大到小排序
    const sorted = [...floorScores].sort((a, b) => b.decimal - a.decimal)

    // 4. 分配剩余分数
    for (let i = 0; i < remain; i++) {
      sorted[i].floor += 1
    }
  }

  // 5. 更新原数据
  floorScores.forEach(item => {
    item.q.score = item.floor
  })
  return true
}

const submitManual = async () => {
  if (!manualForm.paperName) return ElMessage.warning('请输入试卷名称')
  if (!manualForm.courseId) return ElMessage.warning('请选择课程')
  if (manualForm.questions.length === 0) return ElMessage.warning('请至少选择一道题目')

  if (manualForm.totalScore <= 0) {
    return ElMessage.warning('试卷总分必须大于0')
  }

  const n = manualForm.questions.length
  if (manualForm.totalScore < n) {
    return ElMessage.warning(`试卷总分过小：当前有 ${n} 道题，每题至少1分，总分至少应为 ${n}`)
  }

  if (currentSum.value !== manualForm.totalScore) {
    try {
      await ElMessageBox.confirm(
        `当前题目分值总和为 ${currentSum.value}，试卷总分设置为 ${manualForm.totalScore}，两者不匹配。`,
        '分值不匹配',
        {
          confirmButtonText: '自动调整分值',
          cancelButtonText: '手动修改',
          distinguishCancelAndClose: true,
          type: 'warning'
        }
      )
      if (autoAdjustScores()) {
        ElMessage.success('已自动按比例调整分值，请确认后再次点击保存')
      }
      return
    } catch (action) {
      return
    }
  }

  saving.value = true
  try {
    if (editingId.value) {
      await request.put(`/papers/${editingId.value}`, manualForm)
      ElMessage.success('修改试卷成功')
    } else {
      await request.post('/papers', manualForm)
      ElMessage.success('手动组卷成功')
    }
    manualVisible.value = false
    fetchList()
  } catch (e) {
    console.error('保存试卷失败:', e)
  } finally {
    saving.value = false
  }
}

const openAutoCreate = () => {
  editingId.value = null
  Object.assign(autoForm, {
    paperName: '',
    courseId: null,
    totalScore: 100,
    rule: {
      typeDistribution: questionTypeOptions.value.map(t => ({
        typeId: t.id,
        count: 0,
        scorePerQuestion: 5
      })),
      difficultyRatio: { "1": 0.3, "2": 0.5, "3": 0.2 },
      knowledgeIds: []
    }
  })
  autoVisible.value = true
}

const handleAutoCourseChange = async (val) => {
  if (!val) {
    knowledgeTree.value = []
    return
  }
  try {
    const res = await request.get(`/courses/${val}/knowledge-points`)
    knowledgeTree.value = res.data
  } catch (e) {
    console.error('获取知识点树失败:', e)
  }
}

const submitAuto = async () => {
  if (!autoForm.paperName) return ElMessage.warning('请输入试卷名称')
  if (!autoForm.courseId) return ElMessage.warning('请选择课程')

  const activeDists = autoForm.rule.typeDistribution.filter(d => d.count > 0)
  if (activeDists.length === 0) return ElMessage.warning('请设置至少一种题型的数量')

  saving.value = true
  try {
    const payload = {
      ...autoForm,
      rule: {
        ...autoForm.rule,
        typeDistribution: activeDists
      }
    }
    await request.post('/papers/auto-generate', payload)
    ElMessage.success('自动组卷成功')
    autoVisible.value = false
    fetchList()
  } catch (e) {
    console.error('自动组卷失败:', e)
  } finally {
    saving.value = false
  }
}

const formatDateTime = (val) => {
  if (!val) return '-'
  return val.replace('T', ' ').substring(0, 19)
}

const viewDetail = async (row) => {
  try {
    const res = await request.get(`/papers/${row.id}`)
    paperDetail.value = res.data
    detailVisible.value = true
  } catch (e) {
    console.error('获取详情失败:', e)
  }
}

const handleDelete = (row) => {
  ElMessageBox.confirm('确定要删除该试卷吗？', '提示', { type: 'warning' }).then(async () => {
    try {
      await request.delete(`/papers/${row.id}`)
      ElMessage.success('删除成功')
      fetchList()
    } catch (e) {
      console.error('删除试卷失败:', e)
    }
  })
}

onMounted(() => {
  fetchQuestionTypes()
  fetchCourseOptions()
  fetchList()
})
</script>

<style scoped>
.paper-page {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.search-bar {
  margin-bottom: 20px;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.section-title {
  font-weight: bold;
  margin: 20px 0 10px;
  padding-bottom: 5px;
  border-bottom: 1px solid #eee;
}

.selection-actions {
  margin-bottom: 10px;
}

.dist-inputs {
  display: flex;
  align-items: center;
  gap: 10px;
}

.type-row {
  margin-bottom: 10px;
}

.rule-section {
  background: #f9f9f9;
  padding: 15px;
  border-radius: 4px;
}

.score-tip {
  margin-left: 15px;
  font-size: 13px;
  color: #67c23a;
}

.score-tip.error {
  color: #f56c6c;
  font-weight: bold;
}
</style>
