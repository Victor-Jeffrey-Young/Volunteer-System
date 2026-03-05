<template>
  <div class="user-manage-container">
    <el-card shadow="never" class="box-card">
      <template #header>
        <div class="card-header">
          <span style="font-size: 18px; font-weight: bold;">👥 用户权限与状态管理</span>
        </div>
      </template>

      <!-- 顶部搜索栏 -->
      <div class="toolbar">
        <el-input
            v-model="searchName"
            placeholder="请输入志愿者姓名进行检索"
            clearable
            style="width: 300px; margin-right: 15px;"
            :prefix-icon="Search"
            @clear="fetchUserList(1)"
            @keyup.enter="fetchUserList(1)"
        />
        <el-button type="primary" icon="Search" @click="fetchUserList(1)">查 询</el-button>
        <el-button icon="Refresh" @click="resetSearch">重 置</el-button>
      </div>

      <!-- 高级用户列表 -->
      <el-table
          :data="userList"
          border
          stripe
          style="width: 100%; margin-top: 20px;"
          v-loading="loading"
      >
        <el-table-column prop="userId" label="用户ID" width="80" align="center" />
        <el-table-column prop="username" label="登录账号" width="150" />
        <el-table-column prop="realName" label="真实姓名" width="150" />

        <!-- 角色列使用 Tag 区分颜色 -->
        <el-table-column label="系统角色" width="120" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.role === 'ADMIN' ? 'danger' : 'primary'" effect="light">
              {{ scope.row.role === 'ADMIN' ? '管理员' : '志愿者' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="累计志愿时长" width="150" align="center">
          <template #default="scope">
            <span style="color: #67c23a; font-weight: bold;">{{ scope.row.totalHours }} h</span>
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

// 响应式变量
const userList = ref([]);
const loading = ref(false); // 表格加载状态
const searchName = ref('');
const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(0);
const currentLoginId = ref(null); // 当前登录人的ID，用于防误删

// 获取用户列表 (核心逻辑)
const fetchUserList = async (page = 1) => {
  loading.value = true; // 开启加载动画
  try {
    const res = await request.get('/api/user/page', {
      params: {
        current: page,
        size: pageSize.value,
        name: searchName.value
      }
    });
    userList.value = res.data.records;
    total.value = res.data.total;
    currentPage.value = res.data.current;
  } catch (error) {
    console.error("加载用户列表失败", error);
  } finally {
    loading.value = false; // 关闭加载动画
  }
};

// 重置搜索
const resetSearch = () => {
  searchName.value = '';
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