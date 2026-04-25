<template>
  <div class="space-y-6">
    <!-- Header -->
    <div class="flex flex-col md:flex-row md:items-center justify-between gap-4">
      <div>
        <h1 class="text-2xl font-bold text-slate-900">公告与资讯管理</h1>
        <p class="text-sm text-slate-500 mt-1">发布社区动态与志愿招募通知</p>
      </div>
      <button 
        @click="openAdd"
        class="flex items-center gap-2 bg-orange-600 text-white px-6 py-2.5 rounded-xl font-bold text-sm hover:bg-orange-700 shadow-lg shadow-orange-600/20 transition-all active:scale-95"
      >
        <Plus class="w-4 h-4" /> 发布新公告
      </button>
    </div>

    <!-- Filters -->
    <div class="bg-white p-4 rounded-2xl border border-slate-200 shadow-sm flex gap-4">
      <div class="relative flex-1">
        <Search class="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-400" />
        <input 
          v-model="searchTitle" 
          placeholder="搜索公告标题关键词..." 
          class="w-full pl-10 pr-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm outline-none focus:border-orange-500 transition-all"
          @keyup.enter="fetchNotices(1)"
        />
      </div>
      <button @click="fetchNotices(1)" class="px-6 bg-slate-900 text-white rounded-xl text-sm font-bold hover:bg-slate-800 transition-colors">查询</button>
    </div>

    <!-- Notice List -->
    <div class="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
      <el-table :data="noticeList" v-loading="loading" style="width: 100%" size="large">
        <el-table-column label="公告详情" min-width="300">
          <template #default="scope">
            <div class="flex flex-col gap-1 py-1">
              <div class="flex items-center gap-2">
                <span 
                  class="px-1.5 py-0.5 rounded text-[10px] font-black uppercase tracking-tighter"
                  :class="scope.row.type === 1 ? 'bg-red-100 text-red-600' : 'bg-emerald-100 text-emerald-600'"
                >
                  {{ scope.row.type === 1 ? '重要通知' : '志愿新闻' }}
                </span>
                <span class="font-bold text-slate-800 line-clamp-1">{{ scope.row.title }}</span>
              </div>
              <p class="text-xs text-slate-400 line-clamp-1">{{ scope.row.content }}</p>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="publisherName" label="发布人" width="150" align="center">
          <template #default="scope">
            <div class="flex items-center justify-center gap-1.5 text-slate-600 font-medium">
              <User class="w-3.5 h-3.5 text-slate-400" />
              {{ scope.row.publisherName || '系统管理员' }}
            </div>
          </template>
        </el-table-column>

        <el-table-column label="发布时间" width="180" align="center">
          <template #default="scope">
            <div class="text-xs text-slate-500 font-mono">
              {{ formatTime(scope.row.createTime) }}
            </div>
          </template>
        </el-table-column>

        <el-table-column label="管理操作" width="180" fixed="right" align="center">
          <template #default="scope">
            <div class="flex items-center justify-center gap-2">
              <el-button 
                size="small" 
                type="primary" 
                plain 
                class="!rounded-lg"
                @click="openEdit(scope.row)"
              >
                <template #icon><Edit class="w-3.5 h-3.5" /></template>
                编辑
              </el-button>

              <el-button 
                size="small" 
                type="danger" 
                plain 
                class="!rounded-lg"
                @click="handleDelete(scope.row.noticeId)"
              >
                <template #icon><Trash2 class="w-3.5 h-3.5" /></template>
                删除
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="p-4 border-t border-slate-100 flex justify-center">
        <el-pagination
          v-model:current-page="currentPage"
          :total="total"
          :page-size="pageSize"
          layout="prev, pager, next"
          background
          @current-change="fetchNotices"
        />
      </div>
    </div>

    <!-- Edit Dialog -->
    <div v-if="dialogVisible" class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/50 backdrop-blur-sm">
      <div class="bg-white rounded-3xl w-full max-w-2xl overflow-hidden shadow-2xl animate-in fade-in zoom-in duration-200 flex flex-col max-h-[90vh]">
        <div class="p-4 border-b border-slate-100 flex justify-between items-center bg-slate-50">
          <div class="flex items-center gap-2 font-bold text-slate-900">
            <Megaphone class="w-5 h-5 text-orange-600" />
            {{ form.noticeId ? '编辑内容' : '拟定新公告' }}
          </div>
          <button @click="dialogVisible = false" class="p-1 hover:bg-slate-200 rounded-full transition-colors">
            <X class="w-5 h-5" />
          </button>
        </div>

        <div class="p-6 space-y-6 overflow-y-auto">
          <div class="space-y-1.5">
            <label class="text-xs font-bold text-slate-500 uppercase">公告标题</label>
            <input 
              v-model="form.title" 
              placeholder="请输入具有吸引力的标题..." 
              class="w-full px-4 py-3 bg-slate-50 border border-slate-200 rounded-xl focus:ring-2 focus:ring-orange-500/20 focus:border-orange-500 outline-none transition-all font-bold"
            />
          </div>

          <div class="space-y-1.5">
            <label class="text-xs font-bold text-slate-500 uppercase">内容分类</label>
            <div class="flex gap-3">
              <button 
                v-for="t in [{v:1, l:'重要通知'}, {v:2, l:'志愿新闻'}]" 
                :key="t.v"
                @click="form.type = t.v"
                class="flex-1 py-2.5 rounded-xl border text-sm font-bold transition-all"
                :class="form.type === t.v ? 'bg-orange-600 border-orange-600 text-white shadow-md' : 'bg-white border-slate-200 text-slate-600 hover:bg-slate-50'"
              >
                {{ t.l }}
              </button>
            </div>
          </div>

          <div class="space-y-1.5">
            <label class="text-xs font-bold text-slate-500 uppercase">公告正文</label>
            <textarea 
              v-model="form.content" 
              rows="8"
              placeholder="请详细描述公告内容，支持换行..." 
              class="w-full px-4 py-3 bg-slate-50 border border-slate-200 rounded-xl focus:ring-2 focus:ring-orange-500/20 focus:border-orange-500 outline-none transition-all resize-none text-sm leading-relaxed"
            ></textarea>
          </div>
        </div>

        <div class="p-4 border-t border-slate-100 bg-slate-50 flex gap-3">
          <button @click="dialogVisible = false" class="flex-1 py-2.5 bg-white border border-slate-200 rounded-xl text-sm font-bold hover:bg-slate-50 transition-colors">取消</button>
          <button 
            @click="submitForm" 
            :disabled="submitting"
            class="flex-1 py-2.5 bg-orange-600 text-white rounded-xl text-sm font-bold hover:bg-orange-700 shadow-lg shadow-orange-600/30 flex justify-center items-center gap-2 transition-all active:scale-95"
          >
            <span v-if="submitting" class="animate-spin border-2 border-white border-t-transparent rounded-full w-3 h-3"></span>
            {{ submitting ? '发布中...' : '确认发布' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import {
  Plus,
  Search,
  RefreshCcw,
  FileText,
  User,
  Calendar,
  MoreHorizontal,
  Edit,
  Trash2,
  AlertCircle,
  Megaphone,
  X
} from 'lucide-vue-next';
import { ElMessage, ElMessageBox } from 'element-plus';
import request from '../../utils/request';

const loading = ref(false);
const noticeList = ref([]);
const total = ref(0);
const currentPage = ref(1);
const pageSize = ref(10);
const searchTitle = ref('');

const dialogVisible = ref(false);
const submitting = ref(false);
const form = ref({ noticeId: null, title: '', type: 1, content: '', publisherId: null });

const formatTime = (str) => str ? str.replace('T', ' ').substring(0, 16) : '--';

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
    noticeList.value = res.data?.records || [];
    total.value = res.data?.total || 0;
  } catch (e) {
    console.error(e);
  } finally {
    loading.value = false;
  }
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
  if (!form.value.title || !form.value.content) return ElMessage.warning('请填写完整信息');
  submitting.value = true;
  try {
    if (form.value.noticeId) {
      await request.put('/api/notice/update', form.value);
    } else {
      await request.post('/api/notice/add', form.value);
    }
    ElMessage.success('发布成功');
    dialogVisible.value = false;
    fetchNotices();
  } catch (e) {
  } finally {
    submitting.value = false;
  }
};

const handleDelete = (id) => {
  ElMessageBox.confirm('确定要删除这条公告吗？', '删除确认', { type: 'warning' }).then(async () => {
    try {
      await request.delete(`/api/notice/${id}`);
      ElMessage.success('已删除');
      fetchNotices();
    } catch (e) {}
  });
};

onMounted(() => fetchNotices(1));
</script>

<style scoped>
:deep(.el-table) {
  --el-table-border-color: #e2e8f0;
  --el-table-header-bg-color: #f8fafc;
  border-radius: 16px !important;
  overflow: hidden !important;
}
:deep(.el-table__row) {
  transition: all 0.2s;
}
:deep(.el-table__row:hover) {
  background-color: #fff7ed !important;
}
/* 移除多余的外层边框冲突 */
:deep(.el-table--border) {
  border: none !important;
}
</style>