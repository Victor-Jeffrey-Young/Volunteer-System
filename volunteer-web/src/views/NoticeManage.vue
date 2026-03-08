<template>
  <div class="notice-manage-container">
    <el-card shadow="never" class="box-card" :body-style="{ padding: isMobile ? '10px' : '20px' }">
      <template #header>
        <div class="card-header">
          <span style="font-size: 18px; font-weight: bold;">📣 新闻与公告管理</span>
        </div>
      </template>

      <!-- 顶部操作栏 -->
      <div class="toolbar" :class="{ 'mobile-toolbar': isMobile }">
        <el-button type="primary" :icon="Plus" @click="openAdd" class="action-btn">发布新公告</el-button>

        <div class="search-group">
          <el-input
              v-model="searchTitle"
              placeholder="搜标题..."
              clearable
              class="search-input"
              @clear="fetchNotices(1)"
              @keyup.enter="fetchNotices(1)"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-button :icon="Refresh" circle @click="resetSearch" />
        </div>
      </div>

      <!-- ========================================== -->
      <!-- 🖥️ PC 端视图：标准表格 -->
      <!-- ========================================== -->
      <el-table
          v-if="!isMobile"
          :data="noticeList"
          border
          stripe
          style="width: 100%; margin-top: 20px;"
          v-loading="loading"
      >
        <el-table-column prop="title" label="公告标题" min-width="200" show-overflow-tooltip />

        <el-table-column label="类型" width="120" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.type === 1 ? 'danger' : 'success'" effect="light">
              {{ scope.row.type === 1 ? '重要通知' : '志愿新闻' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="publisherName" label="发布人" width="120" align="center" />

        <el-table-column label="发布时间" width="170" align="center">
          <template #default="scope">{{ formatTime(scope.row.createTime) }}</template>
        </el-table-column>

        <el-table-column label="操作" width="180" fixed="right" align="center">
          <template #default="scope">
            <el-button size="small" type="primary" plain @click="openEdit(scope.row)">编辑</el-button>
            <el-button size="small" type="danger" plain @click="handleDelete(scope.row.noticeId)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- ========================================== -->
      <!-- 📱 移动端视图：图文卡片列表 -->
      <!-- ========================================== -->
      <div v-else class="mobile-list" v-loading="loading">
        <div v-for="item in noticeList" :key="item.noticeId" class="m-card">
          <!-- 卡片头部：类型 + 标题 -->
          <div class="m-card-header">
            <el-tag :type="item.type === 1 ? 'danger' : 'success'" size="small" effect="dark" class="m-tag">
              {{ item.type === 1 ? '通知' : '新闻' }}
            </el-tag>
            <span class="m-title">{{ item.title }}</span>
          </div>

          <!-- 卡片内容摘要 -->
          <div class="m-card-body">
            <div class="m-content-preview">{{ item.content }}</div>
            <div class="m-meta-info">
              <span><el-icon><User /></el-icon> {{ item.publisherName }}</span>
              <span><el-icon><Clock /></el-icon> {{ formatTimeShort(item.createTime) }}</span>
            </div>
          </div>

          <!-- 卡片底部操作 -->
          <div class="m-card-footer">
            <el-button type="primary" size="small" plain :icon="Edit" @click="openEdit(item)">编辑</el-button>
            <el-button type="danger" size="small" plain :icon="Delete" @click="handleDelete(item.noticeId)">删除</el-button>
          </div>
        </div>

        <el-empty v-if="noticeList.length === 0" description="暂无公告" />
      </div>

      <!-- 分页控件 -->
      <div class="pagination-box" :class="{ 'mobile-pagination': isMobile }">
        <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :total="total"
            background
            :layout="isMobile ? 'total, prev, next' : 'total, prev, pager, next, jumper'"
            @current-change="fetchNotices"
            :small="isMobile"
        />
      </div>
    </el-card>

    <!-- 发布/编辑弹窗 (响应式) -->
    <el-dialog
        v-model="dialogVisible"
        :title="form.noticeId ? '✏️ 编辑公告' : '📣 发布新公告'"
        :width="isMobile ? '90%' : '600px'"
        destroy-on-close
    >
      <el-form label-width="70px" label-position="top">
        <el-form-item label="公告标题" required>
          <el-input v-model="form.title" placeholder="请输入醒目的标题" />
        </el-form-item>

        <el-form-item label="公告类型" required>
          <el-radio-group v-model="form.type" class="type-radio">
            <el-radio :label="1" border>🔥 重要通知</el-radio>
            <el-radio :label="2" border>📰 志愿新闻</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="正文内容" required>
          <el-input
              v-model="form.content"
              type="textarea"
              :rows="isMobile ? 8 : 10"
              placeholder="在此输入公告正文，支持换行..."
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定发布</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Plus, Search, Refresh, User, Clock, Edit, Delete } from '@element-plus/icons-vue';
import request from '../utils/request';

// --- 响应式判断 ---
const isMobile = ref(window.innerWidth <= 768);
const handleResize = () => { isMobile.value = window.innerWidth <= 768; };

const noticeList = ref([]);
const loading = ref(false);
const dialogVisible = ref(false);
const searchTitle = ref('');
const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(0);

const form = ref({ noticeId: null, title: '', type: 1, content: '', publisherId: null });

// 时间格式化
const formatTime = (str) => str ? str.replace('T', ' ') : '';
const formatTimeShort = (str) => str ? str.split('T')[0] : '';

const fetchNotices = async (page = 1) => {
  if (typeof page === 'number') currentPage.value = page;
  loading.value = true;
  try {
    const res = await request.get('/api/notice/page', {
      params: {
        current: currentPage.value,
        size: pageSize.value,
        title: searchTitle.value
      }
    });
    noticeList.value = res.data.records;
    total.value = res.data.total;
  } catch (e) { console.error(e); }
  finally { loading.value = false; }
};

const resetSearch = () => {
  searchTitle.value = '';
  fetchNotices(1);
};

const openAdd = () => {
  form.value = { noticeId: null, title: '', type: 1, content: '', publisherId: localStorage.getItem('userId') };
  dialogVisible.value = true;
};

const openEdit = (row) => {
  form.value = { ...row };
  dialogVisible.value = true;
};

const submitForm = async () => {
  if (!form.value.title || !form.value.content) return ElMessage.warning('标题和正文不能为空');
  try {
    if (form.value.noticeId) {
      await request.put('/api/notice/update', form.value);
    } else {
      await request.post('/api/notice/add', form.value);
    }
    ElMessage.success('操作成功');
    dialogVisible.value = false;
    fetchNotices();
  } catch (e) {}
};

const handleDelete = (id) => {
  ElMessageBox.confirm('确定删除该公告吗？', '删除确认', {
    type: 'warning',
    confirmButtonText: '确定删除',
    cancelButtonText: '取消'
  }).then(async () => {
    await request.delete(`/api/notice/${id}`);
    ElMessage.success('删除成功');
    // 如果当前页只有一条且被删，向前跳页
    if (noticeList.value.length === 1 && currentPage.value > 1) {
      fetchNotices(currentPage.value - 1);
    } else {
      fetchNotices(currentPage.value);
    }
  }).catch(() => {});
};

onMounted(() => {
  fetchNotices();
  window.addEventListener('resize', handleResize);
});
onUnmounted(() => {
  window.removeEventListener('resize', handleResize);
});
</script>

<style scoped>
/* PC端基础样式 */
.notice-manage-container { padding: 15px; }
.box-card { border-radius: 8px; border: none; }

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.search-group { display: flex; gap: 10px; }
.search-input { width: 250px; }
.pagination-box { margin-top: 25px; display: flex; justify-content: flex-end; }

/* ====================================================
   📱 移动端响应式适配 (小于 768px)
   ==================================================== */
@media screen and (max-width: 768px) {
  .notice-manage-container { padding: 5px; }
  .box-card { border-radius: 0; box-shadow: none !important; }

  /* 顶部工具栏垂直排列 */
  .mobile-toolbar {
    flex-direction: column-reverse; /* 搜索框在上面，发布按钮在下面 */
    gap: 15px;
  }
  .action-btn { width: 100%; }
  .search-group { width: 100%; display: flex; gap: 10px; }
  .search-input { flex: 1; width: auto; }

  /* 卡片列表 */
  .mobile-list {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  .m-card {
    background: #fff;
    border: 1px solid #ebeef5;
    border-radius: 8px;
    box-shadow: 0 2px 8px rgba(0,0,0,0.05);
    overflow: hidden;
  }

  .m-card-header {
    padding: 10px 12px;
    background: #fdfdfd;
    border-bottom: 1px solid #f0f0f0;
    display: flex;
    align-items: center;
  }
  .m-tag { margin-right: 8px; flex-shrink: 0; }
  .m-title {
    font-weight: bold;
    font-size: 15px;
    color: #303133;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis; /* 标题过长省略 */
  }

  .m-card-body {
    padding: 12px;
  }

  /* 内容摘要：限制显示两行，多余显示省略号 */
  .m-content-preview {
    font-size: 13px;
    color: #606266;
    line-height: 1.6;
    margin-bottom: 10px;
    display: -webkit-box;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 2; /* 限制2行 */
    overflow: hidden;
  }

  .m-meta-info {
    display: flex;
    justify-content: space-between;
    font-size: 12px;
    color: #909399;
  }
  .m-meta-info span { display: flex; align-items: center; gap: 4px; }

  .m-card-footer {
    padding: 10px 12px;
    border-top: 1px solid #f0f0f0;
    display: flex;
    justify-content: flex-end;
    gap: 10px;
  }

  /* 弹窗内的单选框组适配 */
  .type-radio {
    display: flex;
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }

  .mobile-pagination { justify-content: center; }
}
</style>