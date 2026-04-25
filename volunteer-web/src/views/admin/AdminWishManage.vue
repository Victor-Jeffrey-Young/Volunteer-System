<template>
  <div class="wish-admin-container">
    <el-card shadow="never" class="box-card">
      <template #header>
        <div class="card-header">
          <span style="font-size: 18px; font-weight: bold;">💗 邻里微心愿审计中台</span>
          <el-radio-group v-model="filterStatus" @change="handleFilter" size="small">
            <el-radio-button :label="null">全部</el-radio-button>
            <el-radio-button :label="0">待审核</el-radio-button>
            <el-radio-button :label="1">待认领</el-radio-button>
            <el-radio-button :label="2">办理中</el-radio-button>
            <el-radio-button :label="5">待验收</el-radio-button>
            <el-radio-button :label="6">待结算</el-radio-button>
            <el-radio-button :label="3">已达成</el-radio-button>
            <el-radio-button :label="4">已驳回</el-radio-button>
          </el-radio-group>
        </div>
      </template>

      <!-- PC 表格视图 -->
      <el-table :data="wishList" stripe style="width: 100%; margin-top: 10px;" v-loading="loading">
        <el-table-column label="心愿概要" min-width="220">
          <template #default="scope">
            <div style="font-weight: bold; color: #303133;">{{ scope.row.title }}</div>
            <div style="font-size: 12px; color: #909399; margin-top: 4px;">{{ scope.row.content }}</div>
          </template>
        </el-table-column>

        <el-table-column label="分类" width="100">
          <template #default="scope">
            <el-tag size="small" effect="plain">{{ scope.row.category }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="发起人" width="140">
          <template #default="scope">
            <div style="display: flex; flex-direction: column;">
              <span>{{ scope.row.requesterName || '【未知用户】' }}</span>
              <span style="font-size: 11px; color: #409eff;">{{ scope.row.requesterPhone }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="认领志愿者" width="140">
          <template #default="scope">
            <div v-if="scope.row.volunteerId" style="display: flex; flex-direction: column;">
              <span>{{ scope.row.volunteerName || '【正在办理】' }}</span>
            </div>
            <span v-else style="color: #999; font-size: 12px;">-- 待认领 --</span>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="120" align="center">
          <template #default="scope">
            <el-tag :type="getStatusType(scope.row.status)" effect="dark">
              {{ getStatusText(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="180" fixed="right" align="center">
          <template #default="scope">
            <!-- 情况1：待审核 -->
            <div v-if="scope.row.status === 0">
              <el-button type="success" size="small" plain @click="handleAudit(scope.row.wishId, 1)">通过</el-button>
              <el-button type="danger" size="small" plain @click="handleAudit(scope.row.wishId, 4)">驳回</el-button>
            </div>

            <!-- 情况2：办理中 / 待结算 (支持强行结算或按流程结算) -->
            <div v-if="scope.row.status === 2 || scope.row.status === 5 || scope.row.status === 6">
              <el-button 
                :type="scope.row.status === 6 ? 'primary' : 'warning'" 
                size="small" 
                @click="openSettleDialog(scope.row)"
              >
                {{ scope.row.status === 6 ? '核发结算' : '强制结算' }}
              </el-button>
            </div>

            <span v-if="scope.row.status === 3" style="color: #67c23a; font-size: 12px; margin-right: 8px;">
              ✨ 已发放 {{ scope.row.rewardPoints }} 积分
            </span>
            <el-button type="info" link size="small" @click="showProgress(scope.row)">轨迹</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页布局 -->
      <div style="margin-top: 20px; display: flex; justify-content: flex-end;">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          background
          layout="total, prev, pager, next"
          @current-change="fetchList"
        />
      </div>
    </el-card>

    <!-- 结算对话框 -->
    <el-dialog v-model="settleVisible" title="⏱ 心愿结算与奖励发放" width="400px">
      <el-form label-position="top">
        <el-form-item label="核发积分">
          <el-input-number v-model="settleForm.rewardPoints" :min="0" :max="500" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="核发志愿时长 (小时)">
          <el-input-number v-model="settleForm.rewardHours" :precision="1" :step="0.5" style="width: 100%;" />
        </el-form-item>
        <p style="font-size: 12px; color: #909399;">
          提示：确认发放后，积分和时长将自动累计到志愿者的个人账户中。
        </p>
      </el-form>
      <template #footer>
        <el-button @click="settleVisible = false">取消</el-button>
        <el-button type="primary" @click="submitSettle">确认核发</el-button>
      </template>
    </el-dialog>

    <!-- 进度详情抽屉 -->
    <el-drawer v-model="drawerVisible" title="心愿流转轨迹" size="400px" class="progress-drawer">
      <div v-if="selectedWish" class="drawer-content">
        <div style="margin-bottom: 20px;">
          <h3 style="font-size: 18px; font-weight: bold; color: #1e293b;">{{ selectedWish.title }}</h3>
          <p style="font-size: 13px; color: #64748b; margin-top: 4px;">{{ selectedWish.content }}</p>
        </div>

        <el-timeline v-if="selectedWish.status !== 4">
          <el-timeline-item timestamp="发布成功" placement="top" color="#f97316">
            <p style="font-size: 12px; color: #94a3b8;">居民成功提交心愿</p>
          </el-timeline-item>

          <el-timeline-item timestamp="管理员审核通过" placement="top" :color="selectedWish.status >= 1 ? '#3b82f6' : '#cbd5e1'">
            <p style="font-size: 12px; color: #94a3b8;">{{ selectedWish.status >= 1 ? '已上架到心愿墙' : '等待审核' }}</p>
          </el-timeline-item>

          <el-timeline-item timestamp="志愿者揭榜接单" placement="top" :color="[2,3,5,6].includes(selectedWish.status) ? '#eab308' : '#cbd5e1'">
            <p style="font-size: 12px; color: #94a3b8;">{{ [2,3,5,6].includes(selectedWish.status) ? `志愿者 ${selectedWish.volunteerName || '已接单'}` : '等待认领' }}</p>
          </el-timeline-item>

          <el-timeline-item timestamp="志愿者标记完成" placement="top" :color="[3,5,6].includes(selectedWish.status) ? '#22c55e' : '#cbd5e1'">
            <p style="font-size: 12px; color: #94a3b8;">{{ [3,5,6].includes(selectedWish.status) ? '志愿者已上报完工' : '等待服务完成' }}</p>
          </el-timeline-item>

          <el-timeline-item timestamp="居民确认核实" placement="top" :color="[3,6].includes(selectedWish.status) ? '#f97316' : '#cbd5e1'">
            <p style="font-size: 12px; color: #94a3b8;">{{ [3,6].includes(selectedWish.status) ? '居民已点击确认' : '等待居民校验' }}</p>
          </el-timeline-item>

          <el-timeline-item timestamp="管理员核算奖励" placement="top" :color="selectedWish.status === 3 ? '#22c55e' : '#cbd5e1'">
            <p style="font-size: 12px; color: #94a3b8;">{{ selectedWish.status === 3 ? '积分/工时已划拨发放' : '等待结项算薪' }}</p>
          </el-timeline-item>
        </el-timeline>

        <!-- 针对驳回状态的单独显示 -->
        <el-timeline v-else>
          <el-timeline-item timestamp="发布成功" placement="top" color="#f97316">
            <p style="font-size: 12px; color: #94a3b8;">居民已提交心愿</p>
          </el-timeline-item>
          <el-timeline-item timestamp="管理员审核驳回" placement="top" color="#ef4444">
            <p style="font-size: 12px; font-weight: bold; color: #ef4444;">已被拒绝上架</p>
            <p style="font-size: 12px; color: #991b1b; margin-top:4px;">原因：{{ selectedWish.remarks }}</p>
          </el-timeline-item>
        </el-timeline>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import request from '../../utils/request';

const wishList = ref([]);
const loading = ref(false);
const filterStatus = ref(null);
const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(0);

// 获取列表数据
const fetchList = async () => {
  loading.value = true;
  try {
    const res = await request.get('/api/wish/admin/page', {
      params: {
        current: currentPage.value,
        size: pageSize.value,
        status: filterStatus.value
      }
    });
    wishList.value = res.data.records;
    total.value = res.data.total;
  } catch (error) {
    console.error(error);
  } finally {
    loading.value = false;
  }
};

const handleFilter = () => {
  currentPage.value = 1;
  fetchList();
}

// 审核心愿发布
const handleAudit = (wishId, status) => {
  const title = status === 1 ? '确认通过审核吗？' : '请填写驳回理由';
  if (status === 4) {
    ElMessageBox.prompt(title, '审核驳回', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
    }).then(async ({ value }) => {
      await request.put(`/api/wish/admin/audit?wishId=${wishId}&status=${status}&remarks=${value || ''}`);
      ElMessage.success('审核已拒绝');
      fetchList();
    }).catch(() => {});
  } else {
    ElMessageBox.confirm(title, '心愿通过', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'success',
    }).then(async () => {
      await request.put(`/api/wish/admin/audit?wishId=${wishId}&status=${status}&remarks=''`);
      ElMessage.success('心愿已并在许愿墙发布！');
      fetchList();
    }).catch(() => {});
  }
};

// 结算逻辑相关
const settleVisible = ref(false);
const settleForm = ref({
  wishId: null,
  rewardPoints: 20,
  rewardHours: 1.0
});

const openSettleDialog = (row) => {
  settleForm.value.wishId = row.wishId;
  
  // 💡 智能积分建议逻辑
  const categoryPoints = {
    '跑腿代办': 15,
    '日常维修': 25,
    '助老/医疗': 40,
    '心理疏导': 30,
    '环境清洁': 10
  };
  
  settleForm.value.rewardPoints = row.rewardPoints || categoryPoints[row.category] || 20;
  settleForm.value.rewardHours = row.rewardHours || 1.0;
  settleVisible.value = true;
};

const submitSettle = async () => {
  try {
    await request.post('/api/wish/admin/settle', settleForm.value);
    ElMessage.success('结算奖励发放成功！');
    settleVisible.value = false;
    fetchList();
  } catch (error) {}
};

const drawerVisible = ref(false);
const selectedWish = ref(null);
const showProgress = (row) => {
  selectedWish.value = row;
  drawerVisible.value = true;
};

// 状态字典辅助函数
const getStatusType = (s) => ({
  0: 'warning',
  1: 'primary',
  2: 'info',
  3: 'success',
  4: 'danger',
  5: 'warning',
  6: 'primary'
})[s] || 'info';

const getStatusText = (s) => ({
  0: '待审核',
  1: '待认领',
  2: '办理中',
  3: '已达成',
  4: '已驳回',
  5: '待验收',
  6: '待结算'
})[s] || '未知';

onMounted(() => {
  fetchList();
});
</script>

<style scoped>
.wish-admin-container {
  padding: 20px;
}
.box-card {
  border-radius: 12px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
:deep(.el-table) {
  border-radius: 8px;
  overflow: hidden;
}
</style>
