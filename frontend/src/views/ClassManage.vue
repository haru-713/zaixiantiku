<template>
  <div class="class-manage-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>班级管理</span>
          <el-button type="primary" @click="openAddDialog">新建班级</el-button>
        </div>
      </template>

      <!-- 搜索栏 -->
      <div class="search-bar">
        <el-input v-model="queryParams.keyword" placeholder="搜索班级/年级" style="width: 300px; margin-right: 15px"
          clearable @clear="handleQuery" @keyup.enter="handleQuery" />
        <el-button type="primary" @click="handleQuery">查询</el-button>
      </div>

      <!-- 数据表格 -->
      <el-table :data="classList" v-loading="loading" style="width: 100%; margin-top: 20px">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="className" label="班级名称" min-width="150" />
        <el-table-column prop="grade" label="年级" width="120" />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="scope">
            <el-button size="small" @click="openEditDialog(scope.row)">编辑</el-button>
            <el-button size="small" type="success" @click="manageStudents(scope.row)">管理学生</el-button>
            <el-button size="small" type="danger" @click="handleDelete(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination :current-page="queryParams.page" :page-size="queryParams.size"
          :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next, jumper" :total="total"
          @size-change="handleSizeChange" @current-change="handleCurrentChange" />
      </div>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '修改班级' : '新建班级'" width="500px">
      <el-form :model="classForm" label-width="80px" :rules="rules" ref="classFormRef">
        <el-form-item label="班级名称" prop="className">
          <el-input v-model="classForm.className" placeholder="请输入班级名称" />
        </el-form-item>
        <el-form-item label="年级" prop="grade">
          <el-input v-model="classForm.grade" placeholder="例如：2023级" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>

    <!-- 管理学生对话框 -->
    <el-dialog v-model="studentDialogVisible" :title="'管理班级学生：' + currentClass.className" width="800px">
      <div class="student-dialog-content">
        <div class="student-toolbar">
          <el-button type="primary" @click="openStudentPicker">添加学生</el-button>
        </div>
        <el-table :data="currentClassStudents" v-loading="studentLoading" style="width: 100%; margin-top: 15px">
          <el-table-column prop="username" label="学号/用户名" width="120" />
          <el-table-column prop="name" label="姓名" width="120" />
          <el-table-column label="操作" width="100" align="center">
            <template #default="scope">
              <el-button type="danger" link @click="removeStudent(scope.row)">移除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-dialog>

    <!-- 学生选择器对话框 -->
    <el-dialog v-model="pickerVisible" title="选择学生" width="600px">
      <div class="picker-container">
        <el-input v-model="pickerQuery.keyword" placeholder="搜索学号/姓名" style="margin-bottom: 15px"
          @keyup.enter="fetchPickerStudents">
          <template #append>
            <el-button icon="Search" @click="fetchPickerStudents" />
          </template>
        </el-input>
        <el-table :data="pickerList" v-loading="pickerLoading" @selection-change="handleSelectionChange"
          height="400px">
          <el-table-column type="selection" width="55" />
          <el-table-column prop="username" label="学号" />
          <el-table-column prop="name" label="姓名" />
        </el-table>
      </div>
      <template #footer>
        <el-button @click="pickerVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmAddStudents" :disabled="selectedStudents.length === 0">添加</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

const loading = ref(false)
const classList = ref([])
const total = ref(0)
const queryParams = reactive({
  page: 1,
  size: 10,
  keyword: ''
})

const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const classFormRef = ref(null)
const classForm = reactive({
  id: null,
  className: '',
  grade: ''
})

const rules = {
  className: [{ required: true, message: '请输入班级名称', trigger: 'blur' }],
  grade: [{ required: true, message: '请输入年级', trigger: 'blur' }]
}

const studentDialogVisible = ref(false)
const currentClass = ref({})
const currentClassStudents = ref([])
const studentLoading = ref(false)

const pickerVisible = ref(false)
const pickerLoading = ref(false)
const pickerList = ref([])
const pickerQuery = reactive({
  keyword: ''
})
const selectedStudents = ref([])

const fetchList = async () => {
  loading.value = true
  try {
    const res = await request.get('/classes', { params: queryParams })
    classList.value = res.data.list
    total.value = res.data.total
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  queryParams.page = 1
  fetchList()
}

const handleSizeChange = (val) => {
  queryParams.size = val
  fetchList()
}

const handleCurrentChange = (val) => {
  queryParams.page = val
  fetchList()
}

const openAddDialog = () => {
  isEdit.value = false
  classForm.id = null
  classForm.className = ''
  classForm.grade = ''
  dialogVisible.value = true
}

const openEditDialog = (row) => {
  isEdit.value = true
  classForm.id = row.id
  classForm.className = row.className
  classForm.grade = row.grade
  dialogVisible.value = true
}

const submitForm = async () => {
  await classFormRef.value.validate()
  submitting.value = true
  try {
    if (isEdit.value) {
      await request.put(`/classes/${classForm.id}`, classForm)
      ElMessage.success('修改成功')
    } else {
      await request.post('/classes', classForm)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchList()
  } catch (e) {
    console.error(e)
  } finally {
    submitting.value = false
  }
}

const handleDelete = (row) => {
  ElMessageBox.confirm('确定要删除该班级吗？删除后班级下的学生将失去关联。', '提示', {
    type: 'warning'
  }).then(async () => {
    await request.delete(`/classes/${row.id}`)
    ElMessage.success('删除成功')
    fetchList()
  })
}

const manageStudents = (row) => {
  currentClass.value = row
  fetchClassStudents()
  studentDialogVisible.value = true
}

const fetchClassStudents = async () => {
  studentLoading.value = true
  try {
    const res = await request.get(`/classes/${currentClass.value.id}/students`)
    currentClassStudents.value = res.data
  } catch (e) {
    console.error(e)
  } finally {
    studentLoading.value = false
  }
}

const removeStudent = (row) => {
  ElMessageBox.confirm(`确定从班级移除学生 ${row.name} 吗？`, '提示', {
    type: 'warning'
  }).then(async () => {
    await request.delete(`/classes/${currentClass.value.id}/students/${row.id}`)
    ElMessage.success('移除成功')
    fetchClassStudents()
  })
}

const openStudentPicker = () => {
  pickerQuery.keyword = ''
  fetchPickerStudents()
  pickerVisible.value = true
}

const fetchPickerStudents = async () => {
  pickerLoading.value = true
  try {
    // 搜索全校学生
    const res = await request.get('/admin/users', {
      params: {
        roleCode: 'STUDENT',
        keyword: pickerQuery.keyword,
        page: 1,
        size: 100 // 简化处理，直接加载前100个
      }
    })
    // 过滤掉已经在该班级的学生
    const existingIds = currentClassStudents.value.map(s => s.id)
    pickerList.value = res.data.list.filter(s => !existingIds.includes(s.id))
  } catch (e) {
    console.error(e)
  } finally {
    pickerLoading.value = false
  }
}

const handleSelectionChange = (val) => {
  selectedStudents.value = val
}

const confirmAddStudents = async () => {
  try {
    const studentIds = selectedStudents.value.map(s => s.id)
    await request.post(`/classes/${currentClass.value.id}/students`, studentIds)
    ElMessage.success('添加成功')
    pickerVisible.value = false
    fetchClassStudents()
  } catch (e) {
    console.error(e)
  }
}

onMounted(() => {
  fetchList()
})
</script>

<style scoped>
.class-manage-container {
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

.student-dialog-content {
  padding: 10px 0;
}

.student-toolbar {
  margin-bottom: 15px;
}

.picker-container {
  padding: 10px 0;
}
</style>
