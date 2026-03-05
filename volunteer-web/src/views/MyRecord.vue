<template>
  <div style="padding: 20px;">
    <h2>📅 我的志愿活动记录</h2>
    <el-table :data="myRecords" style="width: 100%; margin-top: 20px;" border stripe>
      <el-table-column prop="activityTitle" label="活动名称" min-width="150" />
      <el-table-column prop="activityLocation" label="活动地点" min-width="150" />
      <el-table-column prop="applyTime" label="报名时间" width="180">
        <template #default="scope">
          {{ scope.row.applyTime.replace('T', ' ') }}
        </template>
      </el-table-column>
      <el-table-column label="当前状态" width="120">
        <template #default="scope">
          <el-tag :type="getStatusType(scope.row.status)">
            {{ getStatusText(scope.row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <!-- 🚨 审核反馈/拒绝理由列 -->
      <el-table-column prop="remarks" label="审核反馈" min-width="150">
        <template #default="scope">
          <!-- 如果有拒绝理由，标红显示；如果没有，显示暂无 -->
          <span v-if="scope.row.status === 2" style="color: red; font-size: 13px;">
            原因: {{ scope.row.remarks || '管理员未填写原因' }}
          </span>
          <span v-else-if="scope.row.remarks" style="color: #666; font-size: 13px;">
            {{ scope.row.remarks }}
          </span>
          <span v-else style="color: #ccc; font-size: 12px;">--</span>
        </template>
      </el-table-column>
      <!-- 表格中的操作列 -->
      <el-table-column label="操作/打卡" width="220" fixed="right">
        <template #default="scope">
          <el-button v-if="scope.row.status === 0" type="danger" size="small" plain @click="handleCancel(scope.row.regId)">取消报名</el-button>

          <!-- 状态1：审核通过，等待签到 -->
          <div v-else-if="scope.row.status === 1">
            <!-- 🚨 分支 1：活动还没开始 (招募中 0) -->
            <el-tag v-if="scope.row.activityStatus === 0" type="info" size="small">
              活动未开始
            </el-tag>

            <!-- 🚨 分支 2：活动进行中 (1) -> 允许签到 -->
            <el-button v-else-if="scope.row.activityStatus === 1" type="success" size="small" @click="handleSign(scope.row.regId, 'in')">
              现场签到
            </el-button>

            <!-- 🚨 分支 3：活动已结束 (2) -> 错过 -->
            <el-tag v-else type="danger" size="small">已过期/缺席</el-tag>
          </div>

          <!-- 状态5：已签到，显示【签退】 -->
          <div v-else-if="scope.row.status === 5">
            <span style="font-size:12px; color:#999; display:block; margin-bottom:5px;">
              已签到: {{ scope.row.signInTime ? scope.row.signInTime.split(' ')[1] : '--' }}
            </span>
            <el-button type="warning" size="small" @click="handleSign(scope.row.regId, 'out')">结束签退</el-button>
          </div>

          <!-- 状态6：已签退 -->
          <div v-else-if="scope.row.status === 6">
            <span style="font-size:12px; color:#67c23a;">打卡完成，待结算</span>
          </div>

          <span v-else style="color:#ccc; font-size:12px;">--</span>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import request from '../utils/request';
import {Clock} from "@element-plus/icons-vue";

const myRecords = ref([]);
// 修改状态字典
const getStatusType = (s) => ({0:'warning', 1:'primary', 2:'danger', 3:'success', 4:'info', 5:'warning', 6:'success'})[s] || 'info';
const getStatusText = (s) => ({0:'待审核', 1:'待签到', 2:'已拒绝', 3:'流程完结', 4:'已取消', 5:'进行中', 6:'已签退(待结算)'})[s] || '未知';

// 合并签到与签退请求
const handleSign = async (regId, type) => {
  const userId = localStorage.getItem('userId');
  const url = type === 'in' ? '/api/reg/sign' : '/api/reg/sign-out';

  if (type === 'out' && !confirm('确认结束本次志愿服务并签退吗？')) return;

  await request.put(`${url}?regId=${regId}&userId=${userId}`);
  alert(type === 'in' ? '签到成功，服务开始！' : '签退成功！');
  fetchMyRecords();
};

// 报名取消
const handleCancel = async (regId) => {
  if(confirm('确定要取消此次报名吗？名额将释放。')) {
    const userId = localStorage.getItem('userId');
    await request.put(`/api/reg/cancel?regId=${regId}&userId=${userId}`);
    fetchMyRecords(); // 刷新
  }
};

// 拉取志愿活动记录
const fetchMyRecords = async () => {
  const userId = localStorage.getItem('userId');
  const res = await request.get(`/api/reg/my?userId=${userId}`);
  myRecords.value = res.data;
};

onMounted(() => {
  fetchMyRecords();
});
</script>

<style scoped>
:deep(.el-table__cell .cell) {
  display: flex;
  justify-content: center;
  align-items: center;
}

/* 调整标签的内边距和高度，使其更精致 */
.el-tag {
  padding: 0 10px;
  height: 26px;
  line-height: 24px;
}
</style>