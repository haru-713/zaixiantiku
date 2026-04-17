<template>
  <div class="share-detail-page">
    <el-card v-loading="loading">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-button @click="goBack" circle icon="ArrowLeft" />
            <span class="share-title">
              <el-tag v-if="share.isTop" size="small" type="danger" effect="dark" style="margin-right: 8px">置顶</el-tag>
              <template v-if="isAdmin">
                <el-tag v-if="share.status === 0" size="small" type="warning" style="margin-right: 8px">待审核</el-tag>
                <el-tag v-if="share.status === 2" size="small" type="info" style="margin-right: 8px">已驳回</el-tag>
              </template>
              {{ share.title }}
            </span>
          </div>
          <div class="header-right" v-if="isAuthor || isAdmin">
            <el-button type="primary" link @click="handleEdit" v-if="isAuthor">修改</el-button>
            <el-button type="danger" link @click="handleDelete">删除</el-button>
          </div>
        </div>
      </template>

      <div class="share-meta">
        <span class="author">发布者：{{ share.username }}</span>
        <span class="time">时间：{{ formatDateTime(share.createTime) }}</span>
        <span class="stats">浏览：{{ share.viewCount }} | 点赞：{{ share.likeCount }}</span>
      </div>

      <div class="share-content" v-html="share.content"></div>

      <div class="comment-section">
        <h3 class="section-title">评论 ({{ comments.length }})</h3>
        <div class="comment-form">
          <el-input
            v-model="newComment"
            type="textarea"
            :rows="3"
            placeholder="写下你的评论..."
          />
          <div class="form-footer">
            <el-button type="primary" @click="submitComment" :loading="commenting">发表评论</el-button>
          </div>
        </div>

        <div class="comment-list">
          <div v-for="c in comments" :key="c.id" class="comment-item">
            <div class="comment-header">
              <span class="c-user">
                <el-tag v-if="c.isTop" size="small" type="danger" effect="dark" style="margin-right: 5px">置顶</el-tag>
                {{ c.username }}
              </span>
              <span class="c-time">{{ formatDateTime(c.createTime) }}</span>
              <div class="comment-ops">
                <el-button
                  v-if="isAdmin"
                  :type="c.isTop ? 'warning' : 'primary'"
                  link
                  size="small"
                  @click="toggleCommentTop(c)"
                >{{ c.isTop ? '取消置顶' : '置顶' }}</el-button>
                <el-button
                  v-if="isAdmin || c.userId === currentUserId"
                  type="danger"
                  link
                  size="small"
                  @click="deleteComment(c.id)"
                >删除</el-button>
              </div>
            </div>
            <div class="comment-content">{{ c.content }}</div>
          </div>
          <el-empty v-if="comments.length === 0" description="暂无评论" :image-size="60" />
        </div>
      </div>
    </el-card>

    <!-- 修改对话框 -->
    <el-dialog v-model="editVisible" title="修改分享" width="60%">
      <el-form :model="editForm" label-width="80px">
        <el-form-item label="标题" required>
          <el-input v-model="editForm.title" />
        </el-form-item>
        <el-form-item label="内容" required>
          <el-input v-model="editForm.content" type="textarea" :rows="10" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="submitEdit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import request from '@/utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const shareId = route.params.id

const loading = ref(false)
const share = ref({})
const comments = ref([])
const newComment = ref('')
const commenting = ref(false)

const currentUserId = computed(() => userStore.userInfo?.id)
const isAdmin = computed(() => userStore.userInfo?.roles?.includes('ADMIN'))
const isAuthor = computed(() => share.value.userId === currentUserId.value)

const editVisible = ref(false)
const editForm = ref({
  title: '',
  content: ''
})

const fetchDetail = async () => {
  loading.value = true
  try {
    const res = await request.get(`/shares/${shareId}`)
    if (res.code === 1) {
      share.value = res.data
    }
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const fetchComments = async () => {
  try {
    const res = await request.get(`/shares/${shareId}/comments`)
    if (res.code === 1) {
      comments.value = res.data
    }
  } catch (e) {
    console.error(e)
  }
}

const submitComment = async () => {
  if (!newComment.value.trim()) return ElMessage.warning('请输入评论内容')
  
  commenting.value = true
  try {
    const res = await request.post(`/shares/${shareId}/comments`, { content: newComment.value })
    if (res.code === 1) {
      ElMessage.success('评论成功')
      newComment.value = ''
      fetchComments()
    }
  } catch (e) {
    console.error(e)
  } finally {
    commenting.value = false
  }
}

const deleteComment = (commentId) => {
  ElMessageBox.confirm('确定要删除这条评论吗？', '提示', { type: 'warning' }).then(async () => {
    const res = await request.delete(`/shares/comments/${commentId}`)
    if (res.code === 1) {
      ElMessage.success('删除成功')
      fetchComments()
    }
  })
}

const toggleCommentTop = async (comment) => {
  try {
    const newStatus = comment.isTop ? 0 : 1
    const res = await request.put(`/admin/shares/comments/${comment.id}/top`, null, {
      params: { isTop: newStatus }
    })
    if (res.code === 1) {
      ElMessage.success(newStatus ? '评论置顶成功' : '已取消评论置顶')
      fetchComments()
    }
  } catch (e) {
    console.error(e)
  }
}

const handleEdit = () => {
  editForm.value = { title: share.value.title, content: share.value.content }
  editVisible.value = true
}

const submitEdit = async () => {
  try {
    const res = await request.put(`/shares/${shareId}`, editForm.value)
    if (res.code === 1) {
      ElMessage.success('修改成功')
      editVisible.value = false
      fetchDetail()
    }
  } catch (e) {
    console.error(e)
  }
}

const handleDelete = () => {
  ElMessageBox.confirm('确定要删除这篇分享吗？', '提示', { type: 'warning' }).then(async () => {
    const res = await request.delete(`/shares/${shareId}`)
    if (res.code === 1) {
      ElMessage.success('删除成功')
      router.back()
    }
  })
}

const goBack = () => {
  router.back()
}

const formatDateTime = (timeStr) => {
  if (!timeStr) return ''
  return timeStr.replace('T', ' ')
}

onMounted(() => {
  fetchDetail()
  fetchComments()
})
</script>

<style scoped>
.share-detail-page {
  padding: 20px;
  max-width: 1000px;
  margin: 0 auto;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}
.share-title {
  font-size: 20px;
  font-weight: bold;
}
.share-meta {
  margin-bottom: 20px;
  padding-bottom: 10px;
  border-bottom: 1px solid #f0f0f0;
  color: #909399;
  font-size: 14px;
  display: flex;
  gap: 20px;
}
.share-content {
  line-height: 1.8;
  font-size: 16px;
  color: #303133;
  margin-bottom: 40px;
  min-height: 200px;
  white-space: pre-wrap;
}
.comment-section {
  border-top: 1px solid #ebeef5;
  padding-top: 20px;
}
.section-title {
  margin-bottom: 20px;
}
.comment-form {
  margin-bottom: 30px;
}
.form-footer {
  margin-top: 10px;
  display: flex;
  justify-content: flex-end;
}
.comment-item {
  padding: 15px 0;
  border-bottom: 1px solid #f5f7fa;
}
.comment-header {
  display: flex;
  align-items: center;
  gap: 15px;
  margin-bottom: 8px;
}
.comment-ops {
  margin-left: auto;
  display: flex;
  gap: 10px;
}
.c-user {
  font-weight: bold;
  color: #409eff;
  font-size: 14px;
}
.c-time {
  color: #909399;
  font-size: 12px;
}
.comment-content {
  font-size: 14px;
  color: #606266;
}
</style>
