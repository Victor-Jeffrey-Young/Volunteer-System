<template>
  <div class="user-manage-container">
    <el-card shadow="never" class="box-card" :body-style="{ padding: isMobile ? '10px' : '20px' }">
      <template #header>
        <div class="card-header">
          <span style="font-size: 18px; font-weight: bold;">👥 用户权限与状态管理</span>
        </div>
      </template>

      <!-- 顶部多维筛选栏 (响应式折行) -->
      <div class="toolbar" :class="{ 'mobile-toolbar': isMobile }">
        <el-select v-model="searchRole" placeholder="全部角色" clearable class="filter-item" @change="fetchUserList(1)">
          <el-option label="普通志愿者" value="VOLUNTEER" />
          <el-option label="系统管理员" value="ADMIN" />
        </el-select>

        <el-select v-model="searchStatus" placeholder="账号状态" clearable class="filter-item" @change="fetchUserList(1)">
          <el-option label="✅ 正常" :value="1" />
          <el-option label="🚫 已封禁" :value="0" />
        </el-select>

        <el-input
            v-model="searchName"
            placeholder="搜姓名或账号..."
            clearable
            class="filter-item search-input"
            :prefix-icon="Search"
            @clear="fetchUserList(1)"
            @keyup.enter="fetchUserList(1)"
        />

        <div class="btn-group">
          <el-button type="primary" :icon="Search" @click="fetchUserList(1)">查询</el-button>
          <el-button :icon="Refresh" @click="resetSearch">重置</el-button>
        </div>
      </div>

      <!-- ========================================== -->
      <!-- 🖥️ PC 端视图：高级数据表格 -->
      <!-- ========================================== -->
      <el-table
          v-if="!isMobile"
          :data="userList"
          border
          stripe
          style="width: 100%; margin-top: 20px;"
          v-loading="loading"
      >
        <el-table-column label="头像" width="70" align="center">
          <template #default="scope">
            <el-avatar :size="36" :src="scope.row.avatar || getDefaultAvatar(scope.row.username)" />
          </template>
        </el-table-column>

        <el-table-column prop="username" label="登录账号" width="120" />
        <el-table-column prop="realName" label="真实姓名" width="120" />

        <el-table-column label="系统角色" width="100" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.role === 'ADMIN' ? 'danger' : 'primary'" effect="light">
              {{ scope.row.role === 'ADMIN' ? '管理员' : '志愿者' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="联系方式" min-width="150">
          <template #default="scope">
            <div style="font-size: 12px; line-height: 1.5;">
              <div v-if="scope.row.phone">📱 {{ scope.row.phone }}</div>
              <div v-if="scope.row.email">📧 {{ scope.row.email }}</div>
              <div v-if="!scope.row.phone && !scope.row.email" style="color:#ccc">未填写</div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="志愿等级 & 资产" width="180" align="center">
          <template #default="scope">
            <el-tag :color="getLevelInfo(scope.row.totalPoints).color" effect="dark" style="border:none; color: white;">
              {{ getLevelInfo(scope.row.totalPoints).name }}
            </el-tag>
            <div style="margin-top: 5px; font-size: 12px; color: #666;">
              <span>🏅累计: {{ scope.row.totalPoints || 0 }}</span> |
              <span>💰余额: {{ scope.row.currentPoints || 0 }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="80" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.status === 1 ? 'success' : 'info'" effect="dark" size="small">
              {{ scope.row.status === 1 ? '正常' : '已封禁' }}
            </el-tag>
          </template>
        </el-table-column>

        <!-- PC端操作列 -->
        <el-table-column label="操作管理" width="240" fixed="right" align="center">
          <template #default="scope">
            <div v-if="scope.row.userId == currentLoginId">
              <span style="color: #909399; font-size: 13px;">当前登录账号，受保护</span>
            </div>
            <div v-else style="display: flex; justify-content: center; gap: 5px; flex-wrap: wrap;">
              <!-- 🚨 新增：重置密码按钮 -->
              <el-button type="info" size="small" plain @click="resetUserPwd(scope.row)">重置密码</el-button>
              <el-button :type="scope.row.status === 1 ? 'warning' : 'success'" size="small" plain @click="toggleStatus(scope.row)">
                {{ scope.row.status === 1 ? '禁用' : '解封' }}
              </el-button>
              <el-button type="danger" size="small" plain @click="deleteUser(scope.row.userId)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- ========================================== -->
      <!-- 📱 移动端视图：自适应卡片列表 -->
      <!-- ========================================== -->
      <div v-else class="mobile-user-list" v-loading="loading">
        <div v-for="user in userList" :key="user.userId" class="m-user-card">
          <!-- 卡片头部：头像、姓名、角色、状态 -->
          <div class="m-card-header">
            <div class="m-user-title">
              <el-avatar :size="40" :src="user.avatar || getDefaultAvatar(user.username)" />
              <div class="m-name-box">
                <span class="m-name">{{ user.realName }}</span>
                <span class="m-username">账号: {{ user.username }}</span>
              </div>
            </div>
            <div class="m-tags">
              <el-tag :type="user.role === 'ADMIN' ? 'danger' : 'primary'" size="small">{{ user.role === 'ADMIN' ? '管理员' : '志愿者' }}</el-tag>
              <el-tag :type="user.status === 1 ? 'success' : 'info'" size="small" effect="dark" style="margin-left: 5px;">
                {{ user.status === 1 ? '正常' : '封禁' }}
              </el-tag>
            </div>
          </div>

          <!-- 卡片内容：等级、联系方式 -->
          <div class="m-card-body">
            <div class="m-info-row">
              <span class="m-label">等级资产:</span>
              <el-tag :color="getLevelInfo(user.totalPoints).color" effect="dark" size="small" style="border:none; color: white; margin-right: 5px;">
                {{ getLevelInfo(user.totalPoints).name }}
              </el-tag>
              <span style="font-size: 12px; color: #666;">(余 {{ user.currentPoints || 0 }} 分)</span>
            </div>
            <div class="m-info-row">
              <span class="m-label">联系方式:</span>
              <span v-if="user.phone || user.email" style="font-size: 12px; color: #666;">
                {{ user.phone || '' }} {{ user.email ? '| ' + user.email : '' }}
              </span>
              <span v-else style="font-size: 12px; color: #ccc;">未填写</span>
            </div>
          </div>

          <!-- 卡片底部：操作按钮 -->
          <div class="m-card-footer">
            <div v-if="user.userId == currentLoginId" style="color: #909399; font-size: 12px; text-align: center; width: 100%;">
              当前登录账号，受保护
            </div>
            <div v-else class="m-actions">
              <!-- 🚨 移动端重置密码按钮 -->
              <el-button type="info" size="small" plain @click="resetUserPwd(user)">重置密码</el-button>
              <el-button :type="user.status === 1 ? 'warning' : 'success'" size="small" plain @click="toggleStatus(user)">
                {{ user.status === 1 ? '禁用' : '解封' }}
              </el-button>
              <el-button type="danger" size="small" plain @click="deleteUser(user.userId)">删除</el-button>
            </div>
          </div>
        </div>

        <!-- 空状态 -->
        <el-empty v-if="userList.length === 0" description="未找到匹配的用户" />
      </div>

      <!-- 分页控件 -->
      <div class="pagination-box" :class="{ 'mobile-pagination': isMobile }">
        <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :total="total"
            background
            :layout="isMobile ? 'total, prev, pager, next' : 'total, sizes, prev, pager, next, jumper'"
            :page-sizes="[10, 20, 50]"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
            :small="isMobile"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue';
import { Search, Refresh } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import request from '../utils/request';
import { getLevelInfo, getDefaultAvatar } from '../utils/levelRules';

// --- 响应式判断 ---
const isMobile = ref(window.innerWidth <= 768);
const handleResize = () => { isMobile.value = window.innerWidth <= 768; };

// --- 基础变量 ---
const userList = ref([]);
const loading = ref(false);
const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(0);
const currentLoginId = ref(null);

// --- 筛选变量 ---
const searchName = ref('');
const searchRole = ref('');
const searchStatus = ref(null);

// --- 核心方法 ---

// 获取用户列表 (带多维筛选)
const fetchUserList = async (page = 1) => {
  loading.value = true;
  try {
    const res = await request.get('/api/user/page', {
      params: {
        current: page,
        size: pageSize.value,
        name: searchName.value,
        role: searchRole.value,
        status: searchStatus.value
      }
    });
    userList.value = res.data.records;
    total.value = res.data.total;
    currentPage.value = res.data.current;
  } catch (error) {
    console.error("加载用户列表失败", error);
  } finally {
    loading.value = false;
  }
};

// 重置搜索条件
const resetSearch = () => {
  searchName.value = '';
  searchRole.value = '';
  searchStatus.value = null;
  fetchUserList(1);
};

// 分页器事件
const handleSizeChange = (val) => {
  pageSize.value = val;
  fetchUserList(1);
};
const handleCurrentChange = (val) => {
  fetchUserList(val);
};

// 🚨 新增：重置用户密码
const resetUserPwd = (user) => {
  ElMessageBox.confirm(`确定要将用户【${user.realName}】的密码重置为 "123456" 吗？`, '重置密码确认', {
    confirmButtonText: '确定重置',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await request.put(`/api/user/reset-pwd/${user.userId}`);
      ElMessage.success('密码已成功重置');
    } catch (error) {}
  }).catch(() => {});
};

// 切换用户状态 (禁用/解封)
const toggleStatus = async (user) => {
  const actionText = user.status === 1 ? '禁用' : '解封';
  const confirmText = user.status === 1
      ? `确定要禁用用户【${user.realName}】吗？`
      : `确定要恢复用户【${user.realName}】的权限吗？`;

  ElMessageBox.confirm(confirmText, '状态变更提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      const newStatus = user.status === 1 ? 0 : 1;
      await request.put('/api/user/status', {
        userId: user.userId,
        status: newStatus
      });
      ElMessage.success(`已成功${actionText}`);
      fetchUserList(currentPage.value);
    } catch (error) {}
  }).catch(() => {});
};

// 永久删除用户
const deleteUser = (userId) => {
  ElMessageBox.confirm('此操作将永久删除该用户，是否继续？', '高风险操作警示', {
    confirmButtonText: '确定删除',
    cancelButtonText: '取消',
    type: 'error'
  }).then(async () => {
    try {
      await request.delete(`/api/user/${userId}`);
      ElMessage.success('用户已删除');
      // 如果当前页只有一条数据且被删除了，往前跳一页
      if (userList.value.length === 1 && currentPage.value > 1) {
        fetchUserList(currentPage.value - 1);
      } else {
        fetchUserList(currentPage.value);
      }
    } catch (error) {}
  }).catch(() => {});
};

// --- 生命周期钩子 ---
onMounted(() => {
  currentLoginId.value = localStorage.getItem('userId');
  fetchUserList(1);
  window.addEventListener('resize', handleResize);
});

onUnmounted(() => {
  window.removeEventListener('resize', handleResize);
});
</script>

<style scoped>
/* ====================================================
   🖥️ 默认样式 (PC 端)
   ==================================================== */
.user-manage-container { padding: 15px; }
.box-card { border-radius: 8px; border: none; }
.card-header { display: flex; justify-content: space-between; align-items: center; }

.toolbar {
  padding: 15px;
  background-color: #f8f9fa;
  border-radius: 6px;
  display: flex;
  align-items: center;
  flex-wrap: wrap; /* 允许换行 */
  gap: 10px;
}
.filter-item { width: 140px; }
.search-input { width: 220px; }

.pagination-box { margin-top: 25px; display: flex; justify-content: flex-end; }

/* ====================================================
   📱 移动端卡片列表样式
   ==================================================== */
.mobile-user-list {
  margin-top: 15px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.m-user-card {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
  padding: 12px;
}

.m-card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding-bottom: 10px;
  border-bottom: 1px solid #f0f0f0;
}

.m-user-title { display: flex; align-items: center; gap: 10px; }
.m-name-box { display: flex; flex-direction: column; }
.m-name { font-weight: bold; font-size: 15px; color: #303133; }
.m-username { font-size: 12px; color: #909399; }

.m-tags { display: flex; gap: 5px; flex-shrink: 0; }

.m-card-body {
  padding: 10px 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.m-info-row { display: flex; align-items: center; }
.m-label {
  font-size: 13px;
  color: #909399;
  width: 70px;
  flex-shrink: 0;
}

.m-card-footer {
  padding-top: 10px;
  border-top: 1px solid #f0f0f0;
}
.m-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

/* ====================================================
   📱 响应式媒体查询
   ==================================================== */
@media screen and (max-width: 768px) {
  .user-manage-container { padding: 5px; }
  .box-card { border-radius: 0; box-shadow: none !important; }

  /* 筛选栏适配：更紧凑 */
  .mobile-toolbar {
    flex-direction: column;
    align-items: stretch; /* 占满整行 */
  }
  .filter-item, .search-input { width: 100% !important; }
  .btn-group { display: flex; justify-content: space-between; gap: 10px; }
  .btn-group > .el-button { flex: 1; }

  /* 分页器适配：更小，居中 */
  .mobile-pagination {
    justify-content: center;
  }
}
</style>