<script setup>
import { ref, onMounted, computed } from 'vue';
import { 
  Search, 
  RefreshCcw, 
  UserPlus, 
  Shield, 
  UserCircle, 
  Mail, 
  Phone, 
  MoreHorizontal,
  Key,
  Ban,
  Trash2,
  Trophy,
  Coins
} from 'lucide-vue-next';
import { ElMessage, ElMessageBox } from 'element-plus';
import request from '../../utils/request';
import { getLevelInfo } from '../../utils/levelRules';
import { getFullAvatar } from '../../utils/file';

const loading = ref(false);
const userList = ref([]);
const total = ref(0);
const currentPage = ref(1);
const pageSize = ref(10);

const searchName = ref('');
const searchRole = ref('');
const searchStatus = ref(null);

const currentLoginId = localStorage.getItem('userId');

const fetchUserList = async (page = 1) => {
  if (typeof page === 'number') currentPage.value = page;
  loading.value = true;
  try {
    const res = await request.get('/api/user/page', {
      params: {
        current: currentPage.value,
        size: pageSize.value,
        name: searchName.value,
        role: searchRole.value,
        status: searchStatus.value
      }
    });
    userList.value = res.data?.records || [];
    total.value = res.data?.total || 0;
  } catch (error) {
    console.error("加载用户列表失败", error);
  } finally {
    loading.value = false;
  }
};

const resetSearch = () => {
  searchName.value = '';
  searchRole.value = '';
  searchStatus.value = null;
  fetchUserList(1);
};

const resetUserPwd = (user) => {
  ElMessageBox.confirm(`确定要重置【${user.realName}】的密码吗？`, '重置密码', {
    type: 'warning',
    confirmButtonClass: 'bg-orange-600 border-orange-600',
  }).then(async () => {
    try {
      await request.put(`/api/user/reset-pwd/${user.userId}`);
      ElMessage.success('密码已重置为 123456');
    } catch (e) {}
  });
};

const toggleStatus = (user) => {
  const isBlocking = user.status === 1;
  ElMessageBox.confirm(`确定要${isBlocking ? '封禁' : '解封'}该账号吗？`, '状态变更').then(async () => {
    try {
      await request.put('/api/user/status', { userId: user.userId, status: isBlocking ? 0 : 1 });
      ElMessage.success('操作成功');
      fetchUserList();
    } catch (e) {}
  });
};

const deleteUser = (userId) => {
  ElMessageBox.confirm('永久删除用户将不可恢复，确认继续？', '极高风险操作', { type: 'error' }).then(async () => {
    try {
      await request.delete(`/api/user/${userId}`);
      ElMessage.success('用户已删除');
      fetchUserList();
    } catch (e) {}
  });
};

onMounted(() => fetchUserList(1));
</script>

<template>
  <div class="space-y-6">
    <!-- Header -->
    <div class="flex flex-col md:flex-row md:items-center justify-between gap-4">
      <div>
        <h1 class="text-2xl font-bold text-slate-900">用户权限管理</h1>
        <p class="text-sm text-slate-500 mt-1">监管全站志愿者与管理员的账号状态及荣誉等级</p>
      </div>
    </div>

    <!-- Filters -->
    <div class="bg-white p-4 rounded-2xl border border-slate-200 shadow-sm flex flex-wrap gap-4 items-center">
      <div class="relative flex-1 min-w-[200px]">
        <Search class="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-400" />
        <input 
          v-model="searchName" 
          placeholder="搜姓名、账号或手机..." 
          class="w-full pl-10 pr-4 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:ring-2 focus:ring-orange-500/20 focus:border-orange-500 outline-none transition-all"
          @keyup.enter="fetchUserList(1)"
        />
      </div>
      
      <select v-model="searchRole" class="bg-slate-50 border border-slate-200 rounded-xl px-4 py-2 text-sm outline-none focus:border-orange-500">
        <option value="">全部角色</option>
        <option value="VOLUNTEER">志愿者</option>
        <option value="RESIDENT">居民</option>
        <option value="ADMIN">管理员</option>
      </select>

      <select v-model="searchStatus" class="bg-slate-50 border border-slate-200 rounded-xl px-4 py-2 text-sm outline-none focus:border-orange-500">
        <option :value="null">全部状态</option>
        <option :value="1">正常</option>
        <option :value="0">已封禁</option>
      </select>

      <div class="flex gap-2">
        <button @click="fetchUserList(1)" class="px-4 py-2 bg-orange-600 text-white rounded-xl text-sm font-bold hover:bg-orange-700 transition-colors">查询</button>
        <button @click="resetSearch" class="p-2 bg-slate-100 text-slate-600 rounded-xl hover:bg-slate-200 transition-colors">
          <RefreshCcw class="w-4 h-4" />
        </button>
      </div>
    </div>

    <!-- User Table -->
    <div class="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
      <el-table :data="userList" v-loading="loading" style="width: 100%" size="large">
        <el-table-column label="基本信息" min-width="240">
          <template #default="scope">
            <div class="flex items-center gap-3">
              <el-avatar :size="40" :src="getFullAvatar(scope.row.avatar)" class="border-2 border-orange-50" />
              <div class="flex flex-col">
                <span class="font-bold text-slate-900 leading-tight">{{ scope.row.realName }}</span>
                <span class="text-xs text-slate-400">@{{ scope.row.username }}</span>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="角色" width="120">
          <template #default="scope">
            <span 
              class="px-2.5 py-0.5 rounded-full text-xs font-bold"
              :class="scope.row.role === 'ADMIN' ? 'bg-red-50 text-red-600' : (scope.row.role === 'RESIDENT' ? 'bg-purple-50 text-purple-600' : 'bg-blue-50 text-blue-600')"
            >
              {{ scope.row.role === 'ADMIN' ? '管理员' : (scope.row.role === 'RESIDENT' ? '居民' : '志愿者') }}
            </span>
          </template>
        </el-table-column>

        <el-table-column label="联系方式" min-width="180">
          <template #default="scope">
            <div class="flex flex-col gap-1">
              <div class="flex items-center gap-1.5 text-xs text-slate-600">
                <Phone class="w-3 h-3 text-slate-400" /> {{ scope.row.phone || '--' }}
              </div>
              <div class="flex items-center gap-1.5 text-xs text-slate-600">
                <Mail class="w-3 h-3 text-slate-400" /> {{ scope.row.email || '--' }}
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="等级与积分" width="200">
          <template #default="scope">
            <div class="flex flex-col gap-1">
              <div class="flex items-center gap-1.5">
                <Trophy class="w-3.5 h-3.5 text-orange-500" />
                <span class="text-xs font-bold text-slate-700 flex items-center gap-1.5">
                  <span>{{ getLevelInfo(scope.row.totalPoints).icon }}</span>
                  {{ getLevelInfo(scope.row.totalPoints).name }}
                </span>
              </div>
              <div class="flex items-center gap-3 text-[10px] text-slate-400 font-bold uppercase tracking-wider">
                <span>累计: {{ scope.row.totalPoints }}</span>
                <span>可用: {{ scope.row.currentPoints }}</span>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="100">
          <template #default="scope">
            <div class="flex items-center gap-1.5">
              <div class="w-2 h-2 rounded-full" :class="scope.row.status === 1 ? 'bg-emerald-500 shadow-[0_0_8px_rgba(16,185,129,0.5)]' : 'bg-slate-300'"></div>
              <span class="text-xs font-medium" :class="scope.row.status === 1 ? 'text-emerald-600' : 'text-slate-400'">
                {{ scope.row.status === 1 ? '正常' : '封禁' }}
              </span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="管理操作" width="280" fixed="right" align="center">
          <template #default="scope">
            <div v-if="scope.row.role === 'ADMIN'" class="text-xs text-slate-400 italic py-2">
              {{ scope.row.userId == currentLoginId ? '当前账号 (受保护)' : '管理账号 (受保护)' }}
            </div>
            <div v-else-if="scope.row.role === 'RESIDENT'" class="text-xs text-slate-400 italic py-2">
              居民账号
            </div>
            <div v-else class="flex items-center justify-center gap-2">
              <el-button 
                size="small" 
                type="info" 
                plain 
                class="!rounded-lg"
                @click="resetUserPwd(scope.row)"
              >
                <template #icon><Key class="w-3.5 h-3.5" /></template>
                重置密码
              </el-button>

              <el-button 
                size="small" 
                :type="scope.row.status === 1 ? 'warning' : 'success'" 
                plain 
                class="!rounded-lg"
                @click="toggleStatus(scope.row)"
              >
                <template #icon>
                  <Ban v-if="scope.row.status === 1" class="w-3.5 h-3.5" />
                  <Shield v-else class="w-3.5 h-3.5" />
                </template>
                {{ scope.row.status === 1 ? '封禁' : '解封' }}
              </el-button>

              <el-button 
                size="small" 
                type="danger" 
                plain 
                class="!rounded-lg"
                @click="deleteUser(scope.row.userId)"
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
          :page-size="pageSize"
          :total="total"
          layout="prev, pager, next"
          background
          @current-change="fetchUserList"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
:deep(.el-table) {
  --el-table-border-color: #e2e8f0; /* 🚨 稍微加深边框，在圆角处更清晰 */
  --el-table-header-bg-color: #f8fafc;
  border-radius: 16px !important; /* 使表格内部也贴合圆角 */
  overflow: hidden !important;
}
:deep(.el-table__row) {
  transition: all 0.2s;
}
:deep(.el-table__row:hover) {
  background-color: #fff7ed !important; /* 更明显的橙色悬浮 */
}
/* 移除多余的外层边框冲突 */
:deep(.el-table--border) {
  border: none !important;
}
</style>