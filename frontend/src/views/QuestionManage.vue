<template>
  <div class="question-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>题目管理</span>
          <div class="header-actions">
            <el-button type="primary" @click="openCreate">创建题目</el-button>
          </div>
        </div>
      </template>

      <div class="search-bar">
        <el-input v-model="query.keyword" placeholder="题目内容" clearable style="width: 240px; margin-right: 10px"
          @clear="handleQuery" @keyup.enter="handleQuery" />
        <el-select v-model="query.courseId" filterable remote clearable :remote-method="fetchCourseOptions"
          :loading="courseLoading" placeholder="课程" style="width: 220px; margin-right: 10px"
          @change="handleCourseQueryChange">
          <el-option v-for="c in courseOptions" :key="c.id" :label="c.courseName" :value="c.id" />
        </el-select>
        <el-select v-model="query.typeId" placeholder="题型" clearable style="width: 160px; margin-right: 10px">
          <el-option v-for="t in questionTypeOptions" :key="t.id" :label="t.typeName" :value="t.id" />
        </el-select>
        <el-select v-model="query.difficulty" placeholder="难度" clearable style="width: 120px; margin-right: 10px">
          <el-option label="简单" :value="1" />
          <el-option label="中等" :value="2" />
          <el-option label="困难" :value="3" />
        </el-select>
        <el-select v-model="query.knowledgeId" filterable clearable :disabled="!query.courseId"
          :loading="knowledgeLoading" placeholder="知识点" style="width: 220px; margin-right: 10px"
          @visible-change="handleKnowledgeDropdown">
          <el-option v-for="k in knowledgeOptions" :key="k.id" :label="k.name" :value="k.id" />
        </el-select>
        <el-select v-model="query.status" placeholder="状态" clearable style="width: 120px; margin-right: 10px">
          <el-option label="草稿" :value="0" />
          <el-option label="待审核" :value="1" />
          <el-option label="已发布" :value="2" />
          <el-option label="禁用" :value="3" />
        </el-select>
        <el-button type="primary" @click="handleQuery">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </div>

      <el-table :data="list" v-loading="loading" style="width: 100%; margin-top: 16px">
        <el-table-column type="index" label="序号" width="80" :index="indexMethod" />
        <el-table-column prop="content" label="题目内容" min-width="320" show-overflow-tooltip />
        <el-table-column prop="typeId" label="题型" width="140">
          <template #default="scope">
            {{ typeName(scope.row.typeId) }}
          </template>
        </el-table-column>
        <el-table-column prop="difficulty" label="难度" width="100">
          <template #default="scope">
            <el-tag :type="difficultyType(scope.row.difficulty)">{{ difficultyLabel(scope.row.difficulty) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="scope">
            <el-tag :type="statusType(scope.row.status)">{{ statusLabel(scope.row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180">
          <template #default="scope">
            {{ formatDateTime(scope.row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240">
          <template #default="scope">
            <el-button size="small" @click="openDetail(scope.row)">详情</el-button>
            <el-button type="primary" size="small" @click="openEdit(scope.row)">修改</el-button>
            <el-button type="danger" size="small" @click="handleDelete(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <el-pagination :current-page="query.page" :page-size="query.size" :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper" :total="total" @size-change="handleSizeChange"
          @current-change="handleCurrentChange" />
      </div>
    </el-card>

    <el-dialog v-model="formVisible" :title="editingId ? '修改题目' : '创建题目'" width="840px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="课程">
          <el-select v-model="form.courseId" filterable remote clearable :remote-method="fetchCourseOptions"
            :loading="courseLoading" placeholder="选择课程" style="width: 420px" @change="handleFormCourseChange">
            <el-option v-for="c in courseOptions" :key="c.id" :label="c.courseName" :value="c.id" />
          </el-select>
        </el-form-item>

        <el-form-item label="题型">
          <el-select v-model="form.typeId" placeholder="选择题型" style="width: 240px" @change="handleTypeChange">
            <el-option v-for="t in questionTypeOptions" :key="t.id" :label="t.typeName" :value="t.id" />
          </el-select>
        </el-form-item>

        <el-form-item label="难度">
          <el-select v-model="form.difficulty" style="width: 200px">
            <el-option label="简单" :value="1" />
            <el-option label="中等" :value="2" />
            <el-option label="困难" :value="3" />
          </el-select>
        </el-form-item>

        <el-form-item label="题目内容">
          <el-input v-model="form.content" type="textarea" :rows="4" placeholder="请输入题目内容" />
        </el-form-item>

        <el-form-item v-if="showOptions" label="选项">
          <div class="option-editor">
            <div v-for="(opt, idx) in optionTexts" :key="idx" class="option-row">
              <div class="option-label">{{ optionLabel(idx) }}.</div>
              <el-input v-model="optionTexts[idx]" placeholder="请输入选项内容" />
              <el-button v-if="canRemoveOption" type="danger" @click="removeOption(idx)">删除</el-button>
            </div>
            <div v-if="canAddOption" class="option-actions">
              <el-button type="primary" @click="addOption">添加选项</el-button>
            </div>
          </div>
        </el-form-item>

        <el-form-item label="答案">
          <div class="answer-box">
            <el-radio-group v-if="mode === 'single'" v-model="answerSingle">
              <el-radio v-for="(opt, idx) in optionTexts" :key="idx" :label="optionLabel(idx)">
                {{ optionLabel(idx) }}
              </el-radio>
            </el-radio-group>

            <el-checkbox-group v-else-if="mode === 'multi'" v-model="answerMulti">
              <el-checkbox v-for="(opt, idx) in optionTexts" :key="idx" :label="optionLabel(idx)">
                {{ optionLabel(idx) }}
              </el-checkbox>
            </el-checkbox-group>

            <el-radio-group v-else-if="mode === 'judge'" v-model="answerJudge">
              <el-radio label="对">对</el-radio>
              <el-radio label="错">错</el-radio>
            </el-radio-group>

            <el-input v-else v-model="answerBlank" placeholder="请输入答案" style="width: 420px" />
          </div>
        </el-form-item>

        <el-form-item label="知识点">
          <el-select v-model="form.knowledgeIds" multiple filterable clearable collapse-tags collapse-tags-tooltip
            :disabled="!form.courseId" :loading="knowledgeLoading" placeholder="选择知识点" style="width: 420px"
            @visible-change="handleKnowledgeDropdownForForm">
            <el-option v-for="k in knowledgeOptions" :key="k.id" :label="k.name" :value="k.id" />
          </el-select>
        </el-form-item>

        <el-form-item label="解析">
          <el-input v-model="form.analysis" type="textarea" :rows="3" placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="formVisible = false">取消</el-button>
          <el-button type="primary" :loading="saving" @click="submitForm">{{ editingId ? '保存' : '创建' }}</el-button>
        </span>
      </template>
    </el-dialog>

    <el-drawer v-model="detailVisible" title="题目详情" size="50%">
      <el-descriptions :column="2" border v-if="detail">
        <el-descriptions-item label="ID">{{ detail.id }}</el-descriptions-item>
        <el-descriptions-item label="课程ID">{{ detail.courseId }}</el-descriptions-item>
        <el-descriptions-item label="题型">{{ typeName(detail.typeId) }}</el-descriptions-item>
        <el-descriptions-item label="难度">{{ difficultyLabel(detail.difficulty) }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ statusLabel(detail.status) }}</el-descriptions-item>
        <el-descriptions-item label="创建人">{{ detail.createBy }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatDateTime(detail.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ formatDateTime(detail.updateTime) }}</el-descriptions-item>
        <el-descriptions-item label="题目内容" :span="2">{{ detail.content }}</el-descriptions-item>
        <el-descriptions-item label="选项" :span="2">
          <div v-if="detail.options && detail.options.length">
            <div v-for="(op, idx) in detail.options" :key="idx">{{ op }}</div>
          </div>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item label="答案" :span="2">{{ detail.answer }}</el-descriptions-item>
        <el-descriptions-item label="解析" :span="2">{{ detail.analysis || '-' }}</el-descriptions-item>
        <el-descriptions-item label="知识点ID" :span="2">
          {{ (detail.knowledgeIds || []).join(', ') || '-' }}
        </el-descriptions-item>
      </el-descriptions>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

const loading = ref(false)
const saving = ref(false)
const list = ref([])
const total = ref(0)

const query = reactive({
  page: 1,
  size: 10,
  courseId: null,
  typeId: null,
  difficulty: null,
  knowledgeId: null,
  keyword: '',
  status: null
})

const formVisible = ref(false)
const detailVisible = ref(false)
const detail = ref(null)
const editingId = ref(null)

const questionTypeOptions = ref([])
const courseOptions = ref([])
const knowledgeOptions = ref([])
const courseLoading = ref(false)
const knowledgeLoading = ref(false)

const form = reactive({
  courseId: null,
  typeId: null,
  content: '',
  difficulty: 1,
  analysis: '',
  knowledgeIds: []
})

const optionTexts = ref([])
const answerSingle = ref('')
const answerMulti = ref([])
const answerJudge = ref('对')
const answerBlank = ref('')

const mode = computed(() => {
  const t = questionTypeOptions.value.find((x) => x.id === form.typeId)
  const code = (t?.typeCode || '').toUpperCase()
  const name = String(t?.typeName || '')
  if (code.includes('SINGLE') || name.includes('单选')) return 'single'
  if (code.includes('MULTI') || name.includes('多选')) return 'multi'
  if (code.includes('JUDGE') || name.includes('判断')) return 'judge'
  if (code.includes('BLANK') || code.includes('FILL') || name.includes('填空')) return 'blank'
  return 'single'
})

const showOptions = computed(() => mode.value === 'single' || mode.value === 'multi')
const canAddOption = computed(() => mode.value === 'multi')
const canRemoveOption = computed(() => mode.value === 'multi' && optionTexts.value.length > 2)

const formatDateTime = (value) => {
  if (!value) {
    return '-'
  }
  const str = String(value)
  if (str.includes('T')) {
    const replaced = str.replace('T', ' ')
    return replaced.length >= 19 ? replaced.slice(0, 19) : replaced
  }
  return str
}

const difficultyLabel = (d) => {
  const map = { 1: '简单', 2: '中等', 3: '困难' }
  return map[d] || '-'
}

const difficultyType = (d) => {
  const map = { 1: 'success', 2: 'warning', 3: 'danger' }
  return map[d] || 'info'
}

const statusLabel = (s) => {
  const map = { 0: '草稿', 1: '待审核', 2: '已发布', 3: '禁用' }
  return map[s] || '-'
}

const statusType = (s) => {
  const map = { 0: 'info', 1: 'warning', 2: 'success', 3: 'danger' }
  return map[s] || 'info'
}

const typeName = (typeId) => {
  const t = questionTypeOptions.value.find((x) => x.id === typeId)
  return t ? t.typeName : String(typeId || '-')
}

const indexMethod = (index) => {
  return (query.page - 1) * query.size + index + 1
}

const optionLabel = (idx) => {
  return String.fromCharCode(65 + idx)
}

const addOption = () => {
  optionTexts.value.push('')
}

const removeOption = (idx) => {
  optionTexts.value.splice(idx, 1)
  if (mode.value === 'single') {
    if (answerSingle.value && !availableAnswerLabels().includes(answerSingle.value)) {
      answerSingle.value = ''
    }
  }
  if (mode.value === 'multi') {
    answerMulti.value = (answerMulti.value || []).filter((a) => availableAnswerLabels().includes(a))
  }
}

const availableAnswerLabels = () => {
  return optionTexts.value.map((_, idx) => optionLabel(idx))
}

const buildOptionsPayload = () => {
  if (!showOptions.value) return null
  const cleaned = optionTexts.value.map((t) => String(t || '').trim())
  if (cleaned.some((t) => !t)) {
    throw new Error('选项不能为空')
  }
  return cleaned.map((t, idx) => `${optionLabel(idx)}. ${t}`)
}

const buildAnswerPayload = () => {
  if (mode.value === 'single') {
    if (!answerSingle.value) {
      throw new Error('请选择答案')
    }
    return answerSingle.value
  }
  if (mode.value === 'multi') {
    const arr = (answerMulti.value || []).slice().sort()
    if (arr.length === 0) {
      throw new Error('请选择答案')
    }
    return arr.join(',')
  }
  if (mode.value === 'judge') {
    if (!answerJudge.value) {
      throw new Error('请选择答案')
    }
    return answerJudge.value
  }
  const v = String(answerBlank.value || '').trim()
  if (!v) {
    throw new Error('请输入答案')
  }
  return v
}

const fetchQuestionTypes = async () => {
  try {
    const res = await request.get('/question-types')
    questionTypeOptions.value = res.data || []
  } catch (e) {
    console.error('获取题型失败:', e)
  }
}

const fetchCourseOptions = async (keyword) => {
  courseLoading.value = true
  try {
    const params = { page: 1, size: 20, keyword: keyword || '', status: 1 }
    const res = await request.get('/courses', { params })
    courseOptions.value = res.data.list || []
  } catch (e) {
    console.error('获取课程失败:', e)
  } finally {
    courseLoading.value = false
  }
}

const fetchKnowledgeOptions = async (courseId, keyword) => {
  if (!courseId) {
    knowledgeOptions.value = []
    return
  }
  knowledgeLoading.value = true
  try {
    const res = await request.get('/knowledge-points', { params: { courseId, keyword: keyword || '' } })
    knowledgeOptions.value = res.data || []
  } catch (e) {
    console.error('获取知识点失败:', e)
  } finally {
    knowledgeLoading.value = false
  }
}

const fetchList = async () => {
  loading.value = true
  try {
    const res = await request.get('/questions', { params: query })
    list.value = res.data.list
    total.value = res.data.total
  } catch (e) {
    console.error('获取题目列表失败:', e)
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  query.page = 1
  fetchList()
}

const resetQuery = () => {
  query.keyword = ''
  query.courseId = null
  query.typeId = null
  query.difficulty = null
  query.knowledgeId = null
  query.status = null
  knowledgeOptions.value = []
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

const resetForm = () => {
  form.courseId = null
  form.typeId = questionTypeOptions.value.length ? questionTypeOptions.value[0].id : null
  form.content = ''
  form.difficulty = 1
  form.analysis = ''
  form.knowledgeIds = []
  optionTexts.value = ['', '', '', '']
  answerSingle.value = ''
  answerMulti.value = []
  answerJudge.value = '对'
  answerBlank.value = ''
}

const openCreate = async () => {
  editingId.value = null
  resetForm()
  formVisible.value = true
}

const handleFormCourseChange = () => {
  form.knowledgeIds = []
  fetchKnowledgeOptions(form.courseId, '')
}

const handleTypeChange = () => {
  if (mode.value === 'judge' || mode.value === 'blank') {
    optionTexts.value = []
  } else {
    if (optionTexts.value.length === 0) {
      optionTexts.value = ['', '', '', '']
    }
  }
  answerSingle.value = ''
  answerMulti.value = []
  answerJudge.value = '对'
  answerBlank.value = ''
}

const openEdit = async (row) => {
  if (!row || !row.id) return
  editingId.value = row.id
  saving.value = true
  try {
    const res = await request.get(`/questions/${row.id}`)
    const d = res.data
    form.courseId = d.courseId
    form.typeId = d.typeId
    form.content = d.content
    form.difficulty = d.difficulty
    form.analysis = d.analysis || ''
    form.knowledgeIds = d.knowledgeIds || []
    await fetchKnowledgeOptions(form.courseId, '')

    const opts = d.options || []
    const parsedOpts = opts.map((x) => String(x || '').replace(/^[A-Z]\.\s*/, ''))
    optionTexts.value = parsedOpts.length ? parsedOpts : ['', '', '', '']

    const ans = String(d.answer || '')
    if (mode.value === 'single') {
      answerSingle.value = ans
    } else if (mode.value === 'multi') {
      answerMulti.value = ans ? ans.split(',').map((s) => s.trim()).filter((s) => s) : []
    } else if (mode.value === 'judge') {
      answerJudge.value = ans || '对'
    } else {
      answerBlank.value = ans
    }

    formVisible.value = true
  } catch (e) {
    console.error('获取题目详情失败:', e)
  } finally {
    saving.value = false
  }
}

const submitForm = async () => {
  saving.value = true
  try {
    if (!form.courseId) throw new Error('请选择课程')
    if (!form.typeId) throw new Error('请选择题型')
    if (!form.content || !String(form.content).trim()) throw new Error('题目内容不能为空')
    const options = buildOptionsPayload()
    const answer = buildAnswerPayload()
    const payload = {
      courseId: form.courseId,
      typeId: form.typeId,
      content: form.content,
      difficulty: form.difficulty,
      options,
      answer,
      analysis: form.analysis,
      knowledgeIds: form.knowledgeIds
    }

    if (editingId.value) {
      await request.put(`/questions/${editingId.value}`, payload)
      ElMessage.success('保存成功')
    } else {
      await request.post('/questions', payload)
      ElMessage.success('创建成功')
    }

    formVisible.value = false
    fetchList()
  } catch (e) {
    if (e instanceof Error && e.message) {
      ElMessage.error(e.message)
    } else {
      console.error('提交失败:', e)
    }
  } finally {
    saving.value = false
  }
}

const handleDelete = async (row) => {
  if (!row || !row.id) return
  try {
    await ElMessageBox.confirm('确定删除该题目吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  saving.value = true
  try {
    await request.delete(`/questions/${row.id}`)
    ElMessage.success('删除成功')
    fetchList()
  } catch (e) {
    console.error('删除题目失败:', e)
  } finally {
    saving.value = false
  }
}

const openDetail = async (row) => {
  if (!row || !row.id) return
  saving.value = true
  try {
    const res = await request.get(`/questions/${row.id}`)
    detail.value = res.data
    detailVisible.value = true
  } catch (e) {
    console.error('获取题目详情失败:', e)
  } finally {
    saving.value = false
  }
}

const handleCourseQueryChange = () => {
  query.knowledgeId = null
  knowledgeOptions.value = []
}

const handleKnowledgeDropdown = (visible) => {
  if (!visible) return
  if (!query.courseId) return
  fetchKnowledgeOptions(query.courseId, '')
}

const handleKnowledgeDropdownForForm = (visible) => {
  if (!visible) return
  if (!form.courseId) return
  fetchKnowledgeOptions(form.courseId, '')
}

onMounted(() => {
  fetchQuestionTypes().then(() => {
    if (!form.typeId && questionTypeOptions.value.length) {
      form.typeId = questionTypeOptions.value[0].id
    }
  })
  fetchList()
})
</script>

<style scoped>
.question-page {
  padding: 20px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: bold;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.search-bar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.pagination-container {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.option-editor {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.option-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.option-label {
  width: 32px;
  text-align: right;
  font-weight: bold;
}

.option-actions {
  display: flex;
  justify-content: flex-start;
}

.answer-box {
  width: 100%;
}
</style>
