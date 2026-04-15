<template>
  <div class="kp-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>知识点管理</span>
          <div class="header-actions">
            <el-button @click="fetchList">刷新</el-button>
            <el-button type="primary" @click="openCreate">新增知识点</el-button>
          </div>
        </div>
      </template>

      <div class="toolbar">
        <el-input v-model="query.keyword" placeholder="知识点名称" clearable style="width: 240px" @clear="handleQuery"
          @keyup.enter="handleQuery" />
        <el-select v-model="query.courseId" filterable remote clearable :remote-method="fetchCourseOptions"
          :loading="courseLoading" placeholder="课程（可选）" style="width: 320px" @change="handleQuery">
          <el-option v-for="c in courseOptions" :key="c.id" :label="c.courseName" :value="c.id" />
        </el-select>
        <el-button type="primary" @click="handleQuery">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </div>

      <el-table :data="list" v-loading="loading" style="width: 100%; margin-top: 16px">
        <el-table-column type="index" label="序号" width="80" :index="indexMethod" />
        <el-table-column prop="courseName" label="课程" min-width="160" show-overflow-tooltip />
        <el-table-column prop="name" label="知识点名称" min-width="220" show-overflow-tooltip />
        <el-table-column prop="createTime" label="创建时间" width="180">
          <template #default="scope">
            {{ formatDateTime(scope.row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260">
          <template #default="scope">
            <el-button size="small" @click="openDetail(scope.row)">详情</el-button>
            <el-button size="small" type="primary" @click="openEdit(scope.row.id)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(scope.row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <el-pagination :current-page="query.page" :page-size="query.size" :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper" :total="total" @size-change="handleSizeChange"
          @current-change="handleCurrentChange" />
      </div>
    </el-card>

    <el-dialog v-model="formVisible" :title="editingId ? '编辑知识点' : '新增知识点'" width="560px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="课程">
          <el-select v-model="form.courseId" filterable remote clearable :remote-method="fetchCourseOptions"
            :loading="courseLoading" placeholder="选择课程" style="width: 360px" :disabled="!!editingId"
            @change="handleFormCourseChange">
            <el-option v-for="c in courseOptions" :key="c.id" :label="c.courseName" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="名称">
          <el-input v-model="form.name" placeholder="请输入知识点名称" />
        </el-form-item>
        <el-form-item label="父节点">
          <el-select v-model="form.parentId" clearable filterable placeholder="可选" style="width: 360px"
            :disabled="!form.courseId">
            <el-option label="无（根节点）" :value="null" />
            <el-option v-for="item in parentOptions" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="!editingId" label="位置">
          <el-radio-group v-model="form.position">
            <el-radio label="TOP">置顶</el-radio>
            <el-radio label="BOTTOM">置底</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="formVisible = false">取消</el-button>
          <el-button type="primary" :loading="saving" @click="submitForm">保存</el-button>
        </span>
      </template>
    </el-dialog>

    <el-drawer v-model="detailVisible" title="知识点详情" size="55%">
      <div v-loading="detailLoading">
        <el-descriptions v-if="detail" :column="2" border>
          <el-descriptions-item label="ID">{{ detail.id }}</el-descriptions-item>
          <el-descriptions-item label="课程">{{ detailCourseName }}</el-descriptions-item>
          <el-descriptions-item label="名称">{{ detail.name }}</el-descriptions-item>
          <el-descriptions-item label="父节点ID">{{ detail.parentId ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="排序值">{{ detail.sortOrder ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatDateTime(detail.createTime) }}</el-descriptions-item>
        </el-descriptions>

        <el-divider />

        <div class="detail-actions">
          <el-button :disabled="!detail?.id" @click="moveUp">上移</el-button>
          <el-button :disabled="!detail?.id" @click="moveDown">下移</el-button>
          <el-button v-if="detailCourseId" @click="fetchDetailTree">刷新树</el-button>
        </div>

        <div v-loading="treeLoading" class="tree-container">
          <el-empty v-if="!detailCourseId" description="无课程信息" />
          <el-empty v-else-if="treeData.length === 0" description="暂无知识点" />
          <el-tree v-else class="readonly-tree" :data="treeData" node-key="id" default-expand-all
            :expand-on-click-node="false" :highlight-current="true" :current-node-key="detail?.id">
            <template #default="{ data }">
              <span>{{ (data.orderNo || '') + ' ' + data.name }}</span>
            </template>
          </el-tree>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

const courseOptions = ref([])
const courseLoading = ref(false)

const loading = ref(false)
const saving = ref(false)
const list = ref([])
const total = ref(0)

const query = reactive({
  page: 1,
  size: 10,
  courseId: null,
  keyword: ''
})

const formVisible = ref(false)
const editingId = ref(null)
const form = reactive({
  courseId: null,
  name: '',
  parentId: null,
  position: 'BOTTOM'
})

const parentOptionsRaw = ref([])
const parentOptions = computed(() => {
  const arr = parentOptionsRaw.value || []
  if (!editingId.value) {
    return arr
  }
  return arr.filter((x) => x.id !== editingId.value)
})

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

const fetchList = async () => {
  loading.value = true
  try {
    const res = await request.get('/knowledge-points/page', { params: query })
    list.value = res.data.list || []
    total.value = res.data.total || 0
  } catch (e) {
    console.error('获取知识点列表失败:', e)
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

const indexMethod = (index) => {
  return (query.page - 1) * query.size + index + 1
}

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

const fetchParentOptions = async () => {
  if (!form.courseId) {
    parentOptionsRaw.value = []
    return
  }
  try {
    const res = await request.get('/knowledge-points', { params: { courseId: form.courseId } })
    parentOptionsRaw.value = res.data || []
  } catch (e) {
    console.error('获取父节点列表失败:', e)
    parentOptionsRaw.value = []
  }
}

const handleFormCourseChange = () => {
  form.parentId = null
  fetchParentOptions()
}

const openCreate = () => {
  editingId.value = null
  form.courseId = query.courseId || null
  form.name = ''
  form.parentId = null
  form.position = 'BOTTOM'
  fetchParentOptions()
  formVisible.value = true
}

const openEdit = async (kpId) => {
  if (!kpId) return
  saving.value = true
  try {
    const res = await request.get(`/knowledge-points/${kpId}`)
    const d = res.data
    editingId.value = d.id
    form.courseId = d.courseId
    form.name = d.name || ''
    form.parentId = d.parentId ?? null
    await fetchParentOptions()
    formVisible.value = true
  } catch (e) {
    console.error('获取知识点详情失败:', e)
  } finally {
    saving.value = false
  }
}

const submitForm = async () => {
  if (!form.courseId) {
    ElMessage.error('请选择课程')
    return
  }
  if (!form.name || !String(form.name).trim()) {
    ElMessage.error('请输入名称')
    return
  }

  saving.value = true
  try {
    const payload = {
      name: String(form.name).trim(),
      parentId: form.parentId,
      sortOrder: editingId.value ? null : form.position === 'TOP' ? 0 : null
    }

    if (editingId.value) {
      await request.put(`/knowledge-points/${editingId.value}`, payload)
      ElMessage.success('保存成功')
    } else {
      await request.post(`/courses/${form.courseId}/knowledge-points`, payload)
      ElMessage.success('创建成功')
    }

    formVisible.value = false
    fetchList()
  } catch (e) {
    console.error('保存知识点失败:', e)
  } finally {
    saving.value = false
  }
}

const handleDelete = async (kpId) => {
  if (!kpId) return

  try {
    await ElMessageBox.confirm('确定删除该知识点吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }

  saving.value = true
  try {
    await request.delete(`/knowledge-points/${kpId}`)
    ElMessage.success('删除成功')
    fetchList()
  } catch (e) {
    console.error('删除知识点失败:', e)
  } finally {
    saving.value = false
  }
}

const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref(null)
const detailCourseId = ref(null)
const detailCourseName = ref('')
const treeLoading = ref(false)
const treeData = ref([])

const annotateOrder = (nodes) => {
  const walk = (arr, prefix = '') => {
    ; (arr || []).forEach((n, idx) => {
      const no = `${prefix}${idx + 1}.`
      n.orderNo = no
      if (n.children && n.children.length) {
        walk(n.children, no)
      }
    })
  }
  const cloned = JSON.parse(JSON.stringify(nodes || []))
  walk(cloned)
  return cloned
}

const fetchDetailTree = async () => {
  if (!detailCourseId.value) {
    treeData.value = []
    return
  }
  treeLoading.value = true
  try {
    const res = await request.get(`/courses/${detailCourseId.value}/knowledge-points`)
    treeData.value = annotateOrder(res.data || [])
  } catch (e) {
    console.error('获取知识点树失败:', e)
    treeData.value = []
  } finally {
    treeLoading.value = false
  }
}

const openDetail = async (row) => {
  if (!row?.id) return
  detailVisible.value = true
  detailLoading.value = true
  try {
    detailCourseId.value = row.courseId
    detailCourseName.value = row.courseName || ''
    const res = await request.get(`/knowledge-points/${row.id}`)
    detail.value = res.data
    await fetchDetailTree()
  } catch (e) {
    console.error('获取知识点详情失败:', e)
    detail.value = null
  } finally {
    detailLoading.value = false
  }
}

const moveUp = async () => {
  if (!detail.value?.id) return
  saving.value = true
  try {
    const res = await request.post(`/knowledge-points/${detail.value.id}/move`, null, { params: { direction: 'UP' } })
    const moved = !!res.data
    if (moved) {
      ElMessage.success(res.msg || '已上移')
      fetchList()
      fetchDetailTree()
    } else {
      ElMessage.warning(res.msg || '已是最顶部，无法上移')
    }
  } catch (e) {
    console.error('上移失败:', e)
  } finally {
    saving.value = false
  }
}

const moveDown = async () => {
  if (!detail.value?.id) return
  saving.value = true
  try {
    const res = await request.post(`/knowledge-points/${detail.value.id}/move`, null, { params: { direction: 'DOWN' } })
    const moved = !!res.data
    if (moved) {
      ElMessage.success(res.msg || '已下移')
      fetchList()
      fetchDetailTree()
    } else {
      ElMessage.warning(res.msg || '已是最底部，无法下移')
    }
  } catch (e) {
    console.error('下移失败:', e)
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  fetchCourseOptions('')
  fetchList()
})
</script>

<style scoped>
.kp-page {
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

.toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.detail-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.tree-container {
  margin-top: 16px;
  min-height: 240px;
}

.pagination-container {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.readonly-tree :deep(.el-tree-node__content) {
  pointer-events: none;
}
</style>
