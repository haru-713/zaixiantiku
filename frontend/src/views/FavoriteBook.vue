<template>
  <div class="favorite-book">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>我的收藏夹</span>
        </div>
      </template>

      <div v-loading="loading" class="favorite-list">
        <el-empty v-if="list.length === 0" description="暂无收藏题目" />
        <div v-else v-for="item in list" :key="item.id" class="fav-item">
          <div class="fav-info">
            <div class="fav-meta">
              <el-tag size="small" type="info">{{ getTypeName(item.typeId) }}</el-tag>
              <el-tag size="small" :type="getDifficultyType(item.difficulty)">
                {{ getDifficultyLabel(item.difficulty) }}
              </el-tag>
              <span class="fav-time">收藏于: {{ formatTime(item.createTime) }}</span>
            </div>
            <div class="fav-question" v-html="item.content"></div>
          </div>
          <div class="fav-actions">
            <el-button type="primary" size="small" plain @click="viewDetail(item)">查看详情</el-button>
            <el-button type="danger" size="small" plain @click="removeFavorite(item)">取消收藏</el-button>
          </div>
        </div>
      </div>

      <div class="pagination-container">
        <el-pagination :current-page="query.page" :page-size="query.size" :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper" :total="total" @size-change="handleSizeChange"
          @current-change="handleCurrentChange" />
      </div>
    </el-card>

    <!-- 详情对话框 -->
    <el-dialog v-model="detailVisible" title="题目详情" width="600px">
      <div v-if="detailData" class="detail-content">
        <div class="q-content" v-html="detailData.content"></div>
        <div v-if="detailData.options && detailData.options.length" class="q-options">
          <div v-for="opt in detailData.options" :key="opt" class="opt-item">{{ opt }}</div>
        </div>
        <div class="detail-ans">
          <div class="ans-label">正确答案：</div>
          <div class="ans-val">{{ detailData.answer }}</div>
        </div>
        <div v-if="detailData.analysis" class="detail-analysis">
          <div class="ans-label">解析：</div>
          <div class="ans-val">{{ detailData.analysis }}</div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import request from '@/utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const questionTypes = ref([])

const query = reactive({
  page: 1,
  size: 10
})

const detailVisible = ref(false)
const detailData = ref(null)

const fetchList = async () => {
  loading.value = true
  try {
    const res = await request.get('/student/favorites', { params: query })
    list.value = res.data.list
    total.value = res.data.total
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const fetchTypes = async () => {
  try {
    const res = await request.get('/question-types')
    questionTypes.value = res.data
  } catch (e) {
    console.error(e)
  }
}

const getTypeName = (id) => {
  const t = questionTypes.value.find(x => x.id === id)
  return t ? t.typeName : '未知题型'
}

const getDifficultyLabel = (val) => {
  if (val === 1) return '简单'
  if (val === 2) return '中等'
  if (val === 3) return '困难'
  return '未知'
}

const getDifficultyType = (val) => {
  if (val === 1) return 'success'
  if (val === 2) return 'warning'
  if (val === 3) return 'danger'
  return 'info'
}

const formatTime = (time) => {
  if (!time) return '-'
  return time.replace('T', ' ').substring(0, 19)
}

const handleSizeChange = (val) => {
  query.size = val
  fetchList()
}

const handleCurrentChange = (val) => {
  query.page = val
  fetchList()
}

const viewDetail = (item) => {
  detailData.value = item
  detailVisible.value = true
}

const removeFavorite = (item) => {
  ElMessageBox.confirm('确定取消收藏该题目吗？', '提示', {
    type: 'warning'
  }).then(async () => {
    try {
      await request.delete(`/student/favorites/${item.id}`)
      ElMessage.success('已取消收藏')
      fetchList()
    } catch (e) {
      console.error(e)
    }
  })
}

onMounted(() => {
  fetchTypes()
  fetchList()
})
</script>

<style scoped>
.favorite-book {
  padding: 20px;
}
.fav-item {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 20px;
  border-bottom: 1px solid #ebeef5;
}
.fav-item:last-child {
  border-bottom: none;
}
.fav-info {
  flex: 1;
}
.fav-meta {
  margin-bottom: 10px;
  display: flex;
  align-items: center;
  gap: 12px;
}
.fav-time {
  font-size: 13px;
  color: #909399;
}
.fav-question {
  font-size: 16px;
  line-height: 1.6;
}
.fav-actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-left: 20px;
}
.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
.detail-content {
  padding: 10px;
}
.opt-item {
  margin-bottom: 10px;
}
.detail-ans, .detail-analysis {
  margin-top: 20px;
  padding: 15px;
  background: #f8f9fa;
  border-radius: 4px;
}
.ans-label {
  font-weight: bold;
  color: #606266;
  margin-bottom: 5px;
}
.ans-val {
  color: #67c23a;
  font-weight: bold;
}
.detail-analysis {
  background: #fdf6ec;
}
.detail-analysis .ans-val {
  color: #606266;
  font-weight: normal;
}
</style>
