<template>
  <div style="padding: 20px;">
    <h2>📑 报名审核与工时结算</h2>

    <!-- 顶部筛选与搜索栏 -->
    <div style="margin-top: 20px; margin-bottom: 20px; display: flex; gap: 15px;">
      <el-select
          v-model="filterStatus"
          placeholder="全部审核状态"
          clearable
          style="width: 200px"
          @change="handleFilter"
      >
        <el-option label="待审核 (0)" :value="0" />
        <el-option label="待签到 (1)" :value="1" />
        <el-option label="已拒绝 (2)" :value="2" />
        <el-option label="流程完结 (3)" :value="3" />
        <el-option label="已取消 (4)" :value="4" />
        <el-option label="进行中/已签到 (5)" :value="5" />
        <el-option label="待发工时/已签退 (6)" :value="6" />
      </el-select>

      <el-button type="primary" icon="Search" @click="handleFilter">筛选</el-button>
    </div>

    <el-table :data="regList" border stripe style="margin-top: 20px;">
      <el-table-column prop="realName" label="志愿者姓名" width="120" />
      <el-table-column prop="activityTitle" label="报名的活动" min-width="140" />
      <el-table-column prop="applyTime" label="申请时间" width="160">
        <template #default="scope">{{ scope.row.applyTime.replace('T', ' ') }}</template>
      </el-table-column>

      <el-table-column label="当前状态" width="160">
        <template #default="scope">
          <el-tag :type="getStatusType(scope.row.status)">
            {{ getStatusText(scope.row.status) }}
          </el-tag>
        </template>
      </el-table-column>

      <!-- 修改操作列，状态5和6都能发工时 -->
      <el-table-column label="操作管理" width="220" fixed="right">
        <template #default="scope">
          <div v-if="scope.row.status === 0">
            <el-button type="success" size="small" @click="handleAudit(scope.row.regId, 1)">通过</el-button>
            <el-button type="danger" size="small" @click="handleAudit(scope.row.regId, 2)">拒绝</el-button>
          </div>

          <!-- 🚨 修复：只要过了审核（1, 5, 6），都可以强行结算工时 -->
          <div v-else-if="[1, 5, 6].includes(scope.row.status)">
            <el-button type="primary" size="small" @click="openGrantDialog(scope.row)">发放工时</el-button>
          </div>

          <span v-else style="color: #999; font-size: 13px;">流程已结束</span>
        </template>
      </el-table-column>
    </el-table>

    <!--分页器-->
    <div style="margin-top: 20px; display: flex; justify-content: flex-end;">
      <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next, jumper"
          @current-change="handleCurrentChange"
      />
    </div>

    <!-- 发放工时弹窗 -->
    <el-dialog v-model="dialogVisible" title="发放志愿工时与积分" width="400px">
      <el-form label-width="100px">
        <el-form-item label="实际工时(h)">
          <el-input-number v-model="grantHours" :precision="1" :step="0.5" :min="0" />
        </el-form-item>
        <p style="font-size: 12px; color: #999; text-align: center;">发放后，系统将自动同步增加该志愿者的累计时长与对应积分（1h=10分）。</p>
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
import { ref, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import request from '../utils/request';

const regList = ref([]);
const dialogVisible = ref(false);
const currentRegId = ref(null);
const grantHours = ref(2.0); // 默认发放2小时
const filterStatus = ref(null);


// 处理审核 (1-通过, 2-拒绝)
const handleAudit = (regId, status) => {
  const actionText = status === 1 ? '通过' : '拒绝';
  ElMessageBox.prompt(`确认${actionText}该报名申请吗？(可选填备注)`, '审核提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    inputPattern: /.*/,
  }).then(async ({ value }) => {
    await request.put(`/api/reg/admin/audit?regId=${regId}&status=${status}&remarks=${value || ''}`);
    ElMessage.success(`已${actionText}`);
    fetchList();
  }).catch(() => {});
};

// 提交工时发放
const submitGrant = async () => {
  try {
    await request.post(`/api/reg/admin/grant?regId=${currentRegId.value}&actualHours=${grantHours.value}`);
    ElMessage.success('工时及积分发放成功！');
    dialogVisible.value = false;
    fetchList();
  } catch (error) {
    ElMessage.error('发放失败');
  }
};

const openGrantDialog = (row) => {
  currentRegId.value = row.regId;

  // 智能计算工时：如果既有签到又有签退时间，自动算出相差几小时
  if (row.signInTime && row.signOutTime) {
    const start = new Date(row.signInTime).getTime();
    const end = new Date(row.signOutTime).getTime();
    const diffHours = (end - start) / (1000 * 60 * 60);
    // 保留一位小数，例如 2.5 小时
    grantHours.value = Math.max(0, Math.round(diffHours * 10) / 10);
    ElMessage.info(`系统根据打卡记录自动计算推荐工时：${grantHours.value} 小时`);
  } else {
    // 否则默认给 2 小时
    grantHours.value = 2.0;
  }

  dialogVisible.value = true;
};

// 同步更新管理员端的状态字典，加入 4(取消), 5(签到), 6(签退) 的解析
const getStatusType = (s) => {
  const map = {
    0: 'warning',   // 待审核 (黄)
    1: 'primary',   // 审核通过 (蓝)
    2: 'danger',    // 已拒绝 (红)
    3: 'success',   // 流程完结 (绿)
    4: 'info',      // 已取消 (灰)
    5: 'warning',   // 已签到/进行中 (黄)
    6: 'success'    // 已签退/待结算 (绿)
  };
  return map[s] || 'info';
};

const getStatusText = (s) => {
  const map = {
    0: '待审核',
    1: '审核通过(待签到)',
    2: '已拒绝',
    3: '流程完结(已发工时)',
    4: '已取消',
    5: '已签到(进行中)',
    6: '已签退(待发工时)'
  };
  return map[s] || '未知';
};

// 新增分页状态变量
const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(0);

// 修改拉取列表的方法
const fetchList = async () => {
  const res = await request.get('/api/reg/admin/page', {
    params: {
      current: currentPage.value,
      size: pageSize.value,
      status: filterStatus.value // 将下拉框的值传给后端
    }
  });
  regList.value = res.data.records;
  total.value = res.data.total;
};

// 🚨 新增：触发筛选时的处理方法
const handleFilter = () => {
  // 重点：每次重新筛选时，必须把页码重置为第 1 页，防止查不到数据
  currentPage.value = 1;
  fetchList();
};

// 监听页码改变
const handleCurrentChange = (val) => {
  currentPage.value = val;
  fetchList();
};

onMounted(fetchList);
</script>