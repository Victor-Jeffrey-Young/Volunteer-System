<template>
  <div class="reg-manage-container">
    <el-card shadow="never" class="box-card" :body-style="{ padding: isMobile ? '10px' : '20px' }">
      <template #header>
        <div class="card-header">
          <span style="font-size: 18px; font-weight: bold;">📑 报名审核与工时结算</span>
        </div>
      </template>

      <!-- 顶部筛选与搜索栏 (响应式) -->
      <div class="toolbar" :class="{ 'mobile-toolbar': isMobile }">
        <el-select
            v-model="filterStatus"
            placeholder="按状态筛选"
            clearable
            class="filter-item"
            @change="handleFilter"
        >
          <el-option label="全部状态" :value="null" />
          <el-option label="待审核 (0)" :value="0" />
          <el-option label="审核通过 (1)" :value="1" />
          <el-option label="进行中 (5)" :value="5" />
          <el-option label="待结算/已签退 (6)" :value="6" />
          <el-option label="已完结 (3)" :value="3" />
          <el-option label="已拒绝 (2)" :value="2" />
          <el-option label="已取消 (4)" :value="4" />
        </el-select>

      </div>

      <!-- ========================================== -->
      <!-- 🖥️ PC 端视图：标准表格 -->
      <!-- ========================================== -->
      <el-table
          v-if="!isMobile"
          :data="regList"
          border
          stripe
          style="width: 100%; margin-top: 20px;"
          v-loading="loading"
      >
        <el-table-column prop="realName" label="志愿者" width="120" align="center" />
        <el-table-column prop="activityTitle" label="报名活动" min-width="180" show-overflow-tooltip />
        <el-table-column label="申请时间" width="160" align="center">
          <template #default="scope">{{ formatTime(scope.row.applyTime) }}</template>
        </el-table-column>

        <el-table-column label="当前状态" width="140" align="center">
          <template #default="scope">
            <el-tag :type="getStatusType(scope.row.status)" effect="light">
              {{ getStatusText(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="操作管理" width="220" fixed="right" align="center">
          <template #default="scope">
            <!-- 待审核 -->
            <div v-if="scope.row.status === 0">
              <el-button type="success" size="small" plain @click="handleAudit(scope.row.regId, 1)">通过</el-button>
              <el-button type="danger" size="small" plain @click="handleAudit(scope.row.regId, 2)">拒绝</el-button>
            </div>

            <!-- 允许发放工时的状态：1(通过), 5(签到), 6(签退) -->
            <div v-else-if="[1, 5, 6].includes(scope.row.status)">
              <el-button type="primary" size="small" @click="openGrantDialog(scope.row)">
                {{ scope.row.status === 6 ? '结算工时' : '补录工时' }}
              </el-button>
            </div>

            <span v-else style="color: #999; font-size: 13px;">流程已完结</span>
          </template>
        </el-table-column>
      </el-table>

      <!-- ========================================== -->
      <!-- 📱 移动端视图：业务卡片列表 -->
      <!-- ========================================== -->
      <div v-else class="mobile-list" v-loading="loading">
        <div v-for="item in regList" :key="item.regId" class="m-card">
          <!-- 卡片头：活动名 + 状态 -->
          <div class="m-card-header">
            <span class="m-activity-title">{{ item.activityTitle }}</span>
            <el-tag :type="getStatusType(item.status)" size="small" effect="dark">
              {{ getStatusText(item.status) }}
            </el-tag>
          </div>

          <!-- 卡片身：人员信息 + 时间 -->
          <div class="m-card-body">
            <div class="m-row">
              <span class="m-label">志愿者:</span>
              <span class="m-value bold">{{ item.realName }}</span>
            </div>
            <div class="m-row">
              <span class="m-label">申请时间:</span>
              <span class="m-value">{{ formatTime(item.applyTime) }}</span>
            </div>
            <!-- 如果有打卡时间，显示出来辅助决策 -->
            <div class="m-row" v-if="item.signInTime">
              <span class="m-label">签到/退:</span>
              <span class="m-value code-font">
                {{ formatTimeShort(item.signInTime) }} - {{ formatTimeShort(item.signOutTime) }}
              </span>
            </div>
          </div>

          <!-- 卡片底：操作按钮 -->
          <div class="m-card-footer">
            <div v-if="item.status === 0" class="m-btn-group">
              <el-button type="success" size="small" plain @click="handleAudit(item.regId, 1)">通过</el-button>
              <el-button type="danger" size="small" plain @click="handleAudit(item.regId, 2)">拒绝</el-button>
            </div>
            <div v-else-if="[1, 5, 6].includes(item.status)">
              <el-button type="primary" size="small" style="width: 100%;" @click="openGrantDialog(item)">
                <el-icon style="margin-right: 5px"><Stopwatch /></el-icon>
                {{ item.status === 6 ? '一键结算工时' : '手动补录工时' }}
              </el-button>
            </div>
            <div v-else class="m-status-text">
              <el-icon><CircleCheck /></el-icon> 流程已结束
            </div>
          </div>
        </div>

        <el-empty v-if="regList.length === 0" description="暂无相关记录" />
      </div>

      <!-- 分页控件 -->
      <div class="pagination-box" :class="{ 'mobile-pagination': isMobile }">
        <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :total="total"
            background
            :layout="isMobile ? 'total, prev, pager, next' : 'total, sizes, prev, pager, next, jumper'"
            @current-change="fetchList"
            :small="isMobile"
        />
      </div>
    </el-card>

    <!-- 发放工时弹窗 (响应式适配) -->
    <el-dialog
        v-model="dialogVisible"
        title="⏱ 发放志愿工时"
        :width="isMobile ? '90%' : '400px'"
        destroy-on-close
    >
      <el-form label-width="100px" label-position="top">
        <div class="grant-info">
          <p>正在为 <strong>{{ currentVolunteerName }}</strong> 结算工时</p>
          <p v-if="autoCalcMsg" class="calc-tip">{{ autoCalcMsg }}</p>
        </div>
        <el-form-item label="核发工时 (小时)">
          <el-input-number
              v-model="grantHours"
              :precision="1"
              :step="0.5"
              :min="0"
              style="width: 100%;"
          />
        </el-form-item>
        <p class="points-tip">
          <el-icon><Coin /></el-icon> 系统将自动发放 <strong>{{ grantHours * 10 }}</strong> 积分
        </p>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitGrant">确认发放</el-button>
        </span>
      </template>
    </el-dialog>

  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue';
import { Search, Stopwatch, Coin, CircleCheck } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import request from '../utils/request';

// --- 响应式判断 ---
const isMobile = ref(window.innerWidth <= 768);
const handleResize = () => { isMobile.value = window.innerWidth <= 768; };

const regList = ref([]);
const loading = ref(false);
const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(0);
const filterStatus = ref(null);

// 弹窗相关
const dialogVisible = ref(false);
const currentRegId = ref(null);
const currentVolunteerName = ref('');
const grantHours = ref(2.0);
const autoCalcMsg = ref('');

// 格式化时间
// RegistrationManage.vue 中的推荐写法：
const formatTime = (timeStr) => timeStr ? timeStr.replace('T', ' ').substring(0, 16) : '--';
const formatTimeShort = (timeStr) => timeStr ? timeStr.replace('T', ' ').substring(5, 16) : '--';

// 状态字典 (保持和之前一致)
const getStatusType = (s) => ({0:'warning', 1:'primary', 2:'danger', 3:'success', 4:'info', 5:'warning', 6:'success'})[s] || 'info';
const getStatusText = (s) => ({0:'待审核', 1:'审核通过', 2:'已拒绝', 3:'已完结', 4:'已取消', 5:'进行中', 6:'已签退/待结算'})[s] || '未知';

const fetchList = async (page = 1) => {
  if (typeof page === 'number') currentPage.value = page;
  loading.value = true;
  try {
    const res = await request.get('/api/reg/admin/page', {
      params: {
        current: currentPage.value,
        size: pageSize.value,
        status: filterStatus.value
      }
    });
    regList.value = res.data.records;
    total.value = res.data.total;
  } catch (e) { console.error(e); }
  finally { loading.value = false; }
};

const handleFilter = () => {
  currentPage.value = 1;
  fetchList();
};

const handleAudit = (regId, status) => {
  const actionText = status === 1 ? '通过' : '拒绝';
  ElMessageBox.prompt(`确认${actionText}该申请吗？(可选填备注)`, '审核', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
  }).then(async ({ value }) => {
    await request.put(`/api/reg/admin/audit?regId=${regId}&status=${status}&remarks=${value || ''}`);
    ElMessage.success(`已${actionText}`);
    fetchList();
  }).catch(() => {});
};

const openGrantDialog = (row) => {
  currentRegId.value = row.regId;
  currentVolunteerName.value = row.realName;
  autoCalcMsg.value = '';

  // 智能计算逻辑
  if (row.signInTime && row.signOutTime) {
    const start = new Date(row.signInTime).getTime();
    const end = new Date(row.signOutTime).getTime();
    const diff = (end - start) / (1000 * 60 * 60);
    grantHours.value = Math.max(0, Math.round(diff * 10) / 10);
    autoCalcMsg.value = `系统根据打卡记录自动计算：${grantHours.value} 小时`;
  } else {
    grantHours.value = 2.0;
  }

  dialogVisible.value = true;
};

const submitGrant = async () => {
  try {
    await request.post(`/api/reg/admin/grant?regId=${currentRegId.value}&actualHours=${grantHours.value}`);
    ElMessage.success('结算成功');
    dialogVisible.value = false;
    fetchList();
  } catch (error) {}
};

onMounted(() => {
  fetchList();
  window.addEventListener('resize', handleResize);
});
onUnmounted(() => {
  window.removeEventListener('resize', handleResize);
});
</script>

<style scoped>
/* PC端基础样式 */
.reg-manage-container { padding: 15px; }
.box-card { border-radius: 8px; border: none; }

.toolbar {
  padding: 15px;
  background-color: #f8f9fa;
  border-radius: 6px;
  display: flex;
  align-items: center;
  gap: 10px;
}
.filter-item { width: 200px; }
.pagination-box { margin-top: 25px; display: flex; justify-content: flex-end; }

/* 弹窗内部样式 */
.grant-info { background: #eef7fe; padding: 10px; border-radius: 4px; margin-bottom: 15px; color: #606266; font-size: 14px; }
.calc-tip { color: #409eff; font-weight: bold; margin-top: 5px; }
.points-tip { font-size: 12px; color: #909399; margin-top: 5px; }

/* ====================================================
   📱 移动端响应式适配 (小于 768px)
   ==================================================== */
@media screen and (max-width: 768px) {
  .reg-manage-container { padding: 5px; }
  .box-card { border-radius: 0; box-shadow: none !important; }

  /* 筛选栏 */
  .mobile-toolbar {
    flex-direction: column;
    align-items: stretch;
  }
  .filter-item { width: 100% !important; }
  .search-btn { width: 100%; }

  /* 移动端卡片列表 */
  .mobile-list {
    margin-top: 15px;
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
    background: #fcfcfc;
    padding: 10px 12px;
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    border-bottom: 1px solid #ebeef5;
  }
  .m-activity-title {
    font-weight: bold;
    font-size: 15px;
    color: #303133;
    flex: 1;
    margin-right: 10px;
    /* 最多显示两行 */
    display: -webkit-box;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 2;
    overflow: hidden;
  }

  .m-card-body {
    padding: 12px;
    font-size: 13px;
    color: #606266;
  }
  .m-row { margin-bottom: 6px; display: flex; }
  .m-label { width: 70px; color: #909399; flex-shrink: 0; }
  .m-value { flex: 1; }
  .m-value.bold { font-weight: bold; color: #303133; }
  .code-font { font-family: monospace; color: #409eff; }

  .m-card-footer {
    padding: 10px 12px;
    border-top: 1px solid #ebeef5;
    display: flex;
    justify-content: flex-end;
    background: #fff;
  }

  .m-btn-group { display: flex; gap: 10px; width: 100%; }
  .m-btn-group .el-button { flex: 1; }

  .m-status-text { color: #909399; font-size: 12px; display: flex; align-items: center; gap: 5px; }

  /* 分页居中 */
  .mobile-pagination { justify-content: center; }
}
</style>