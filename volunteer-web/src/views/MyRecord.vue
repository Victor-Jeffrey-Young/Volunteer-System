<template>
  <div class="my-record-container">
    <el-card shadow="never" class="box-card" :body-style="{ padding: isMobile ? '10px' : '20px' }">
      <template #header>
        <div class="card-header">
          <span style="font-size: 18px; font-weight: bold;">📅 我的志愿活动记录</span>
          <!-- 手机端扫码按钮 (置于头部) -->
          <el-button v-if="isMobile" type="primary" @click="startScan" size="small" round>
            扫码签到
          </el-button>
        </div>
      </template>

      <!-- ========================================== -->
      <!-- 🖥️ PC 端视图：标准表格 -->
      <!-- ========================================== -->
      <div v-if="!isMobile">
        <el-table :data="myRecords" border stripe style="width: 100%">
          <el-table-column prop="activityTitle" label="活动名称" min-width="180" show-overflow-tooltip />
          <el-table-column prop="activityLocation" label="活动地点" min-width="150" show-overflow-tooltip />

          <el-table-column label="报名时间" width="160" align="center">
            <template #default="scope">{{ formatTime(scope.row.applyTime) }}</template>
          </el-table-column>

          <el-table-column label="当前状态" width="120" align="center">
            <template #default="scope">
              <el-tag :type="getStatusType(scope.row.status)">{{ getStatusText(scope.row.status) }}</el-tag>
            </template>
          </el-table-column>

          <el-table-column label="获得积分" width="100" align="center">
            <template #default="scope">
              <span v-if="scope.row.status === 3" style="color: #e6a23c; font-weight: bold;">+{{ scope.row.rewardPoints }}</span>
              <span v-else style="color: #ccc;">--</span>
            </template>
          </el-table-column>

          <el-table-column label="审核反馈" min-width="150">
            <template #default="scope">
              <span v-if="scope.row.status === 2" style="color: #f56c6c; font-size: 12px;">
                原因: {{ scope.row.remarks || '未填写' }}
              </span>
              <span v-else style="color: #ccc;">--</span>
            </template>
          </el-table-column>

          <el-table-column label="操作/打卡" width="200" fixed="right" align="center">
            <template #default="scope">
              <!-- 待审核 -->
              <el-button v-if="scope.row.status === 0" type="danger" size="small" plain @click="handleCancel(scope.row.regId)">取消报名</el-button>

              <!-- 审核通过：判断活动状态 -->
              <div v-else-if="scope.row.status === 1">
                <el-tag v-if="scope.row.activityStatus === 0" type="info" size="small">未开始</el-tag>
                <el-button v-else-if="scope.row.activityStatus === 1" type="success" size="small" @click="handleSign(scope.row.regId, 'in')">现场签到</el-button>
                <el-tag v-else type="danger" size="small">已过期</el-tag>
              </div>

              <!-- 签到中 -->
              <div v-else-if="scope.row.status === 5">
                <div style="font-size: 12px; color: #909399; margin-bottom: 2px;">已签: {{ formatTimeOnly(scope.row.signInTime) }}</div>
                <el-button type="warning" size="small" @click="handleSign(scope.row.regId, 'out')">结束签退</el-button>
              </div>

              <!-- 已签退 -->
              <span v-else-if="scope.row.status === 6" style="color: #67c23a; font-size: 12px;">待结算</span>
              <span v-else style="color: #ccc; font-size: 12px;">无需操作</span>
            </template>
          </el-table-column>
        </el-table>

        <!-- PC端扫码按钮放在底部 -->
        <div style="margin-top: 20px; text-align: center;">
          <el-button type="primary" @click="startScan" size="large" round>📷 电脑摄像头扫码</el-button>
        </div>
      </div>

      <!-- ========================================== -->
      <!-- 📱 移动端视图：行程卡片列表 -->
      <!-- ========================================== -->
      <div v-else class="mobile-list" v-loading="loading">
        <div v-for="item in myRecords" :key="item.regId" class="m-record-card">
          <!-- 头部：活动名 + 状态 -->
          <div class="m-head">
            <span class="m-title">{{ item.activityTitle }}</span>
            <el-tag :type="getStatusType(item.status)" size="small" effect="dark">
              {{ getStatusText(item.status) }}
            </el-tag>
          </div>

          <!-- 内容区 -->
          <div class="m-body">
            <div class="m-row">
              <el-icon><Location /></el-icon> {{ item.activityLocation }}
            </div>
            <div class="m-row">
              <el-icon><Time /></el-icon> 报名: {{ formatTime(item.applyTime) }}
            </div>
            <!-- 如果被拒绝，显示原因 -->
            <div class="m-row reject-box" v-if="item.status === 2">
              <el-icon color="#f56c6c"><Warning /></el-icon>
              <span>{{ item.remarks || '管理员未填写拒绝原因' }}</span>
            </div>
            <!-- 如果已完结，显示积分 -->
            <div class="m-row points-box" v-if="item.status === 3">
              <el-icon color="#e6a23c"><Coin /></el-icon>
              <span>获得积分: {{ item.rewardPoints }}</span>
            </div>
          </div>

          <!-- 底部操作区 (核心交互) -->
          <div class="m-foot">
            <!-- 情况1: 待审核 -->
            <el-button v-if="item.status === 0" type="danger" plain size="small" style="width: 100%" @click="handleCancel(item.regId)">取消报名</el-button>

            <!-- 情况2: 审核通过 (判断活动是否开始) -->
            <div v-else-if="item.status === 1" style="width: 100%">
              <el-button v-if="item.activityStatus === 1" type="success" size="small" style="width: 100%" @click="handleSign(item.regId, 'in')">
                📍 到达现场，立即签到
              </el-button>
              <el-button v-else-if="item.activityStatus === 0" type="info" disabled size="small" style="width: 100%">
                活动尚未开始
              </el-button>
              <el-button v-else type="info" disabled size="small" style="width: 100%">已过期</el-button>
            </div>

            <!-- 情况3: 签到中 (显示签到时间 + 签退按钮) -->
            <div v-else-if="item.status === 5" style="width: 100%; display: flex; align-items: center; justify-content: space-between;">
              <span class="m-time-tip">已签到 {{ formatTimeOnly(item.signInTime) }}</span>
              <el-button type="warning" size="small" @click="handleSign(item.regId, 'out')">结束签退</el-button>
            </div>

            <!-- 其他状态 -->
            <div v-else class="m-done-text">
              <el-icon><CircleCheckFilled /></el-icon> 流程结束
            </div>
          </div>
        </div>
        <el-empty v-if="myRecords.length === 0" description="暂无报名记录" />
      </div>
    </el-card>

    <!-- 扫码弹窗 (保持不变) -->
    <el-dialog v-model="scanVisible" title="扫描活动二维码" width="90%" @close="stopScan" destroy-on-close>
      <div id="reader" style="width: 100%; min-height: 250px; background: #000;"></div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue';
import request from '../utils/request';
import { Html5Qrcode } from "html5-qrcode";
import { ElMessage, ElMessageBox } from 'element-plus';
import { Location, Timer as Time, Coin, Warning, CircleCheckFilled, Clock } from '@element-plus/icons-vue';

// --- 响应式判断 ---
const isMobile = ref(window.innerWidth <= 768);
const handleResize = () => { isMobile.value = window.innerWidth <= 768; };

const myRecords = ref([]);
const loading = ref(false);
const scanVisible = ref(false);
let html5QrCode = null;
// 🚨 新增：防止重复扫码的“防抖锁”
let isProcessing = false;

// 状态字典
const getStatusType = (s) => ({0:'warning', 1:'primary', 2:'danger', 3:'success', 4:'info', 5:'warning', 6:'success'})[s] || 'info';
const getStatusText = (s) => ({0:'待审核', 1:'待签到', 2:'已拒绝', 3:'已完结', 4:'已取消', 5:'服务中', 6:'已签退(待结算)'})[s] || '未知';

// --- 🚨 修复版：安全的时间格式化函数 ---
const formatTime = (str) => {
  if (!str) return '--';
  // 无论是带 T 还是带空格，统一替换为空格，并只截取到分钟 (前16位)
  return str.replace('T', ' ').substring(0, 16);
};

const formatTimeOnly = (str) => {
  if (!str) return '--';
  // 兼容处理：判断字符串里包含 T 还是空格，按对应字符分割
  const timePart = str.includes('T') ? str.split('T')[1] : str.split(' ')[1];
  // 安全提取前 5 位 (HH:mm)
  return timePart ? timePart.substring(0, 5) : '--';
};

const fetchMyRecords = async () => {
  loading.value = true;
  try {
    const userId = localStorage.getItem('userId');
    const res = await request.get(`/api/reg/my?userId=${userId}`);
    myRecords.value = res.data;
  } catch (e) { console.error(e); }
  finally { loading.value = false; }
};

const handleSign = async (regId, type) => {
  const userId = localStorage.getItem('userId');
  const url = type === 'in' ? '/api/reg/sign' : '/api/reg/sign-out';

  if (type === 'out') {
    try {
      await ElMessageBox.confirm('确认结束本次志愿服务并签退吗？', '提示', { confirmButtonText: '确认签退', type: 'warning' });
    } catch { return; }
  }

  try {
    await request.put(`${url}?regId=${regId}&userId=${userId}`);
    ElMessage.success(type === 'in' ? '签到成功！' : '签退成功！');
    fetchMyRecords();
  } catch (e) {}
};

const handleCancel = (regId) => {
  ElMessageBox.confirm('确定取消报名吗？', '提示', { type: 'warning' }).then(async () => {
    const userId = localStorage.getItem('userId');
    await request.put(`/api/reg/cancel?regId=${regId}&userId=${userId}`);
    ElMessage.success('已取消');
    fetchMyRecords();
  }).catch(() => {});
};

// 启动摄像头扫码 (修复内存泄漏与卡死版)
const startScan = async () => {
  scanVisible.value = true;
  isProcessing = false; // 每次打开弹窗时，重置锁
  await nextTick();

  html5QrCode = new Html5Qrcode("reader");

  html5QrCode.start(
      { facingMode: "environment" },
      { fps: 10, qrbox: { width: 250, height: 250 } },

      // 扫码成功的回调
      async (decodedText) => {
        // 🚨 1. 防抖拦截：如果正在处理，直接丢弃同一秒内多余的扫描结果
        if (isProcessing) return;
        isProcessing = true; // 上锁！

        // 🚨 2. 安全关闭：必须使用 await 等待摄像头完全关闭，再进行后续操作
        await stopScan();

        // 3. 业务逻辑处理
        const scannedActivityId = parseInt(decodedText);
        if (isNaN(scannedActivityId)) {
          ElMessage.error('无效的二维码格式！');
          return;
        }

        const record = myRecords.value.find(r => r.activityId === scannedActivityId);
        if (!record) {
          ElMessage.error('扫码失败：您尚未报名该活动，或申请未通过！');
          return;
        }

        // 状态机判断
        if (record.status === 1) {
          if (record.activityStatus !== 1) {
            ElMessage.warning('活动尚未开始或已结束，当前无法签到！');
          } else {
            handleSign(record.regId, 'in');
          }
        }
        else if (record.status === 5) {
          handleSign(record.regId, 'out');
        }
        else if (record.status === 6 || record.status === 3) {
          ElMessage.success('您已经完成了该活动的全部打卡，辛苦了！');
        }
        else {
          ElMessage.error('当前审核状态无法进行打卡操作！');
        }
      },
      (errorMessage) => { /* 扫描进行中，忽略报错 */ }
  ).catch(err => {
    console.error("摄像头调用失败", err);
    ElMessage.error('无法调用摄像头，请检查浏览器权限或是否为 HTTPS 环境！');
    scanVisible.value = false;
  });
};

// 🚨 修复版：安全关闭摄像头
const stopScan = async () => {
  if (html5QrCode) {
    try {
      // 检查摄像头是否正在运行
      if (html5QrCode.isScanning) {
        // 必须 await，等待底层硬件彻底切断视频流
        await html5QrCode.stop();
      }
      html5QrCode.clear();
    } catch (err) {
      console.error("关闭摄像头时发生异常:", err);
    }
  }
  // 必须等摄像头关了，才能让 Vue 销毁 DOM 弹窗
  scanVisible.value = false;
};

onMounted(() => {
  fetchMyRecords();
  window.addEventListener('resize', handleResize);
});
onUnmounted(() => {
  window.removeEventListener('resize', handleResize);
});
</script>

<style scoped>
/* PC端基础样式 */
.my-record-container { padding: 15px; }
.box-card { border-radius: 8px; border: none; }
.card-header { display: flex; justify-content: space-between; align-items: center; }

/* ====================================================
   📱 移动端响应式适配 (小于 768px)
   ==================================================== */
@media screen and (max-width: 768px) {
  .my-record-container { padding: 5px; }
  .box-card { border-radius: 0; box-shadow: none !important; }

  /* 列表容器 */
  .mobile-list { display: flex; flex-direction: column; gap: 15px; }

  /* 卡片样式 */
  .m-record-card {
    background: #fff;
    border: 1px solid #ebeef5;
    border-radius: 10px;
    box-shadow: 0 2px 8px rgba(0,0,0,0.05);
    overflow: hidden;
  }

  /* 卡片头部 */
  .m-head {
    padding: 12px 15px;
    background: #fdfdfd;
    border-bottom: 1px solid #f0f0f0;
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
  .m-title { font-weight: bold; font-size: 15px; color: #303133; }

  /* 卡片内容 */
  .m-body { padding: 15px; font-size: 13px; color: #606266; }
  .m-row { display: flex; align-items: center; gap: 6px; margin-bottom: 8px; }
  .m-row:last-child { margin-bottom: 0; }

  /* 特殊状态提示框 */
  .reject-box { background: #fef0f0; color: #f56c6c; padding: 8px; border-radius: 4px; font-size: 12px; }
  .points-box { background: #fdf6ec; color: #e6a23c; padding: 8px; border-radius: 4px; font-weight: bold; }

  /* 底部操作区 */
  .m-foot {
    padding: 10px 15px;
    border-top: 1px solid #f0f0f0;
    background: #fff;
    display: flex;
    justify-content: flex-end;
    align-items: center;
  }

  .m-time-tip { font-size: 12px; color: #409eff; margin-right: auto; background: #ecf5ff; padding: 2px 6px; border-radius: 4px; }
  .m-done-text { font-size: 12px; color: #909399; display: flex; align-items: center; gap: 5px; }
}
</style>