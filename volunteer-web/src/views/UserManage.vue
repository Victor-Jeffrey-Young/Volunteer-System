<template>
  <div class="user-manage-container">
    <el-card shadow="never" class="box-card">
      <template #header>
        <div class="card-header">
          <span style="font-size: 18px; font-weight: bold;">👥 用户权限与状态管理</span>
        </div>
      </template>

      <!-- 顶部多维筛选栏 -->
      <div class="toolbar">
        <!-- 1. 角色筛选 -->
        <el-select
            v-model="searchRole"
            placeholder="全部角色"
            clearable
            style="width: 140px; margin-right: 10px;"
            @change="fetchUserList(1)"
        >
          <el-option label="普通志愿者" value="VOLUNTEER" />
          <el-option label="系统管理员" value="ADMIN" />
        </el-select>

        <!-- 2. 状态筛选 -->
        <el-select
            v-model="searchStatus"
            placeholder="账号状态"
            clearable
            style="width: 140px; margin-right: 10px;"
            @change="fetchUserList(1)"
        >
          <el-option label="✅ 正常" :value="1" />
          <el-option label="🚫 已封禁" :value="0" />
        </el-select>

        <!-- 3. 关键词搜索 -->
        <el-input
            v-model="searchName"
            placeholder="搜姓名或账号..."
            clearable
            style="width: 200px; margin-right: 10px;"
            :prefix-icon="Search"
            @clear="fetchUserList(1)"
            @keyup.enter="fetchUserList(1)"
        />

        <!-- 按钮组 -->
        <el-button type="primary" :icon="Search" @click="fetchUserList(1)">查询</el-button>
        <el-button :icon="Refresh" @click="resetSearch">重置</el-button>
      </div>

      <!-- 高级用户列表 -->
      <el-table
          :data="userList"
          border
          stripe
          style="width: 100%; margin-top: 20px;"
          v-loading="loading"
      >
        <!-- 1. 新增：头像列 -->
        <el-table-column label="头像" width="80" align="center">
          <template #default="scope">
            <el-avatar :size="40" :src="scope.row.avatar || getDefaultAvatar(scope.row.username)" />
          </template>
        </el-table-column>
        <el-table-column prop="userId" label="用户ID" width="80" align="center" />
        <el-table-column prop="username" label="登录账号" width="150" />
        <el-table-column prop="realName" label="真实姓名" width="150" />
        <!-- 3. 新增：联系方式 (手机/邮箱) -->
        <el-table-column label="联系方式" width="180">
          <template #default="scope">
            <div style="font-size: 12px;">
              <div v-if="scope.row.phone">📱 {{ scope.row.phone }}</div>
              <div v-if="scope.row.email">📧 {{ scope.row.email }}</div>
              <div v-if="!scope.row.phone && !scope.row.email" style="color:#ccc">未填写</div>
            </div>
          </template>
        </el-table-column>

        <!-- 角色列使用 Tag 区分颜色 -->
        <el-table-column label="系统角色" width="120" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.role === 'ADMIN' ? 'danger' : 'primary'" effect="light">
              {{ scope.row.role === 'ADMIN' ? '管理员' : '志愿者' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="志愿等级 & 积分资产" width="220" align="center">
          <template #default="scope">
            <!-- 🚨 必须使用 scope.row.totalPoints 来计算段位 -->
            <el-tag :color="getLevelInfo(scope.row.totalPoints).color" effect="dark" style="border:none; color: white;">
              {{ getLevelInfo(scope.row.totalPoints).name }}
            </el-tag>
            <div style="margin-top: 5px; font-size: 12px; color: #666; display: flex; justify-content: center; gap: 10px;">
              <span title="决定段位">🏅累计: {{ scope.row.totalPoints || 0 }}</span>
              <span title="可用于购物">💰余额: {{ scope.row.currentPoints || 0 }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="当前状态" width="100" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.status === 1 ? 'success' : 'info'" effect="dark" size="small">
              {{ scope.row.status === 1 ? '正常' : '已封禁' }}
            </el-tag>
          </template>
        </el-table-column>

        <!-- 操作列 -->
        <el-table-column label="操作管理" min-width="180" fixed="right">
          <template #default="scope">
            <!-- 防误触设计：如果是本人，不显示禁用和删除按钮 -->
            <div v-if="scope.row.userId == currentLoginId">
              <span style="color: #909399; font-size: 13px;">当前登录账号</span>
            </div>
            <div v-else>
              <el-button
                  :type="scope.row.status === 1 ? 'warning' : 'success'"
                  size="small"
                  plain
                  @click="toggleStatus(scope.row)"
              >
                {{ scope.row.status === 1 ? '禁用账号' : '解除封禁' }}
              </el-button>

              <el-button
                  type="danger"
                  size="small"
                  plain
                  @click="deleteUser(scope.row.userId)"
              >
                永久删除
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- 现代化分页控件 -->
      <div class="pagination-box">
        <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :total="total"
            background
            layout="total, sizes, prev, pager, next, jumper"
            :page-sizes="[10, 20, 50]"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { Search, Refresh } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import request from '../utils/request';
import { getLevelInfo, getDefaultAvatar } from '../utils/levelRules';

// 响应式变量
const searchRole = ref('');   // 新增：角色筛选
const searchStatus = ref(null); // 新增：状态筛选 (注意初始值为 null)
const userList = ref([]);
const loading = ref(false); // 表格加载状态
const searchName = ref('');
const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(0);
const currentLoginId = ref(null); // 当前登录人的ID，用于防误删

// 获取用户列表 (核心逻辑)
const fetchUserList = async (page = 1) => {
  loading.value = true;
  try {
    const res = await request.get('/api/user/page', {
      params: {
        current: page,
        size: pageSize.value,
        name: searchName.value,
        role: searchRole.value,     // 传参
        status: searchStatus.value  // 传参
      }
    });
    userList.value = res.data.records;
    total.value = res.data.total;
    currentPage.value = res.data.current;
  } catch (error) {
    console.error("加载失败", error);
  } finally {
    loading.value = false;
  }
};

// 重置搜索
const resetSearch = () => {
  searchName.value = '';
  searchRole.value = '';
  searchStatus.value = null;
  fetchUserList(1);
};


// 切换每页显示数量
const handleSizeChange = (val) => {
  pageSize.value = val;
  fetchUserList(1); // 重置到第一页
};

// 翻页
const handleCurrentChange = (val) => {
  fetchUserList(val);
};

// 切换用户状态 (禁用/启用)
const toggleStatus = async (user) => {
  const actionText = user.status === 1 ? '禁用' : '解封';
  const confirmText = user.status === 1
      ? `确定要禁用用户【${user.realName}】吗？禁用后该用户无法登录。`
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
      ElMessage.success(`已成功${actionText}该用户`);
      fetchUserList(currentPage.value); // 刷新列表
    } catch (error) {
      // 错误已拦截
    }
  }).catch(() => {});
};

// 删除用户
const deleteUser = (userId) => {
  ElMessageBox.confirm('此操作将永久删除该用户及其关联数据，是否继续？', '高风险操作警示', {
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
    } catch (error) {
      // 错误已拦截
    }
  }).catch(() => {});
};

onMounted(() => {
  // 获取当前登录用户的ID，用于前端判断“是否是自己”
  currentLoginId.value = localStorage.getItem('userId');
  fetchUserList(1);
});
</script>

<style scoped>
.user-manage-container {
  padding: 20px;
}

.box-card {
  border-radius: 8px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.toolbar {
  margin-bottom: 20px;
  padding: 15px;
  background-color: #f8f9fa;
  border-radius: 6px;
  display: flex;
  align-items: center;
}

.pagination-box {
  margin-top: 25px;
  display: flex;
  justify-content: flex-end;
}
</style>