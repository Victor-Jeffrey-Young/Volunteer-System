<template>
  <div class="activity-page">
    <!-- ========================================== -->
    <!-- 📢 发布区域：仅管理员可见 -->
    <!-- ========================================== -->
    <el-card class="box-card" v-if="userRole === 'ADMIN'">
      <template #header>
        <div class="card-header">
          <span>📢 发布新的志愿活动</span>
          <!-- 手机端收起/展开表单的按钮 (优化体验) -->
          <el-button v-if="isMobile" size="small" text type="primary" @click="showAddForm = !showAddForm">
            {{ showAddForm ? '收起' : '展开填写' }}
          </el-button>
        </div>
      </template>

      <!-- 这里的 v-show 用于手机端折叠，PC端始终显示 -->
      <el-form
          v-show="!isMobile || showAddForm"
          :model="newActivity"
          :label-width="isMobile ? '80px' : '100px'"
          :label-position="isMobile ? 'top' : 'left'"
      >
        <el-row :gutter="20">
          <!-- 标题：手机占满，PC占一半 -->
          <el-col :xs="24" :sm="12">
            <el-form-item label="活动标题" required>
              <el-input v-model="newActivity.title" placeholder="例如：社区孤寡老人慰问" />
            </el-form-item>
          </el-col>

          <!-- 类型：手机占满，PC占一半 -->
          <el-col :xs="24" :sm="12">
            <el-form-item label="活动类型" required>
              <el-select v-model="newActivity.type" placeholder="请选择类型" style="width: 100%">
                <el-option label="社区服务" value="社区服务" />
                <el-option label="环境保护" value="环境保护" />
                <el-option label="教育助学" value="教育助学" />
                <el-option label="文明创建" value="文明创建" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :xs="24" :sm="12">
            <el-form-item label="活动地点" required>
              <el-input v-model="newActivity.location" placeholder="具体街道或社区名称">
                <template #prefix><el-icon><Location /></el-icon></template>
              </el-input>
            </el-form-item>
          </el-col>

          <!-- 手机端：奖励和人数并排显示，各占一半，节省空间 -->
          <el-col :xs="12" :sm="6">
            <el-form-item label="奖励时长">
              <el-input-number
                  v-model="newActivity.rewardHours"
                  :precision="1" :step="0.5" :min="0"
                  style="width: 100%"
                  :controls="false"
              />
            </el-form-item>
          </el-col>

          <el-col :xs="12" :sm="6">
            <el-form-item label="招募人数">
              <el-input-number
                  v-model="newActivity.capacity"
                  :min="1" :max="500"
                  style="width: 100%"
                  :controls="false"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :xs="24" :sm="12">
            <el-form-item label="活动时间" required>
              <el-date-picker
                  v-model="activityTimeRange"
                  type="datetimerange"
                  range-separator="至"
                  start-placeholder="开始"
                  end-placeholder="结束"
                  value-format="YYYY-MM-DD HH:mm:ss"
                  @change="handleTimeChange"
                  style="width: 100%"
                  :teleported="false"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="活动内容">
          <el-input
              v-model="newActivity.content"
              type="textarea"
              :rows="3"
              placeholder="请详细描述活动流程及志愿者要求..."
          />
        </el-form-item>

        <el-form-item>
          <div class="form-btn-group">
            <el-button type="primary" @click="handleAdd" class="action-btn">立即发布</el-button>
            <el-button @click="resetForm" class="action-btn">重置</el-button>
          </div>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- ========================================== -->
    <!-- 🌟 活动列表展示部分 (保持之前的响应式 Grid) -->
    <!-- ========================================== -->
    <h3 class="section-title">🌟 当前活动大厅</h3>
    <el-row :gutter="20">
      <el-col
          :xs="24" :sm="12" :md="8"
          v-for="item in activityList"
          :key="item.activityId"
          style="margin-bottom: 20px"
      >
        <el-card :body-style="{ padding: '15px' }" shadow="hover" class="activity-card">
          <div class="card-top">
            <strong class="activity-title">{{ item.title }}</strong>
            <el-tag :type="getActivityStatusTag(item.status)" size="small">
              {{ getActivityStatusText(item.status) }}
            </el-tag>
          </div>

          <div class="card-info">
            <p><el-icon><Location /></el-icon> {{ item.location }}</p>
            <p><el-icon><Clock /></el-icon> {{ item.startTime.substring(5, 16) }}</p>
            <p>
              <el-icon><User /></el-icon> 招募:
              <span :class="{'full-load': item.currentNum >= item.capacity}">
                {{ item.currentNum }} / {{ item.capacity }}
              </span>
            </p>
          </div>

          <div class="card-actions">
            <!-- 管理员操作 -->
            <div v-if="userRole === 'ADMIN'" class="admin-btns">
              <!-- 🚨 新增：签到码按钮 -->
              <el-button size="small" color="#722ed1" plain @click="openQrCode(item)">签到码</el-button>
              <el-button size="small" type="primary" plain @click="openEdit(item)">修改</el-button>
              <el-button size="small" type="info" plain @click="viewApplicants(item)">名单</el-button>
              <el-button size="small" type="danger" plain @click="handleDelete(item.activityId)">删除</el-button>
            </div>
            <!-- 志愿者操作 -->
            <div v-else class="volunteer-btns">
              <el-button size="small" @click="openDetail(item)">详情</el-button>
              <el-button
                  size="small"
                  :type="appliedSet.has(item.activityId) ? 'info' : 'primary'"
                  :disabled="appliedSet.has(item.activityId)"
                  @click="handleApply(item.activityId)"
              >
                {{ appliedSet.has(item.activityId) ? '已报名' : '立即报名' }}
              </el-button>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 活动详情弹窗 (宽度适配) -->
    <el-dialog v-model="detailVisible" title="志愿活动详情" :width="isMobile ? '95%' : '500px'">
      <!-- ... 内容保持不变 ... -->
      <div v-if="currentActivity" class="activity-detail-box">
        <h3 style="color: #409eff; margin-top: 0;">{{ currentActivity.title }}</h3>
        <p><strong>📍 活动地点：</strong>{{ currentActivity.location }}</p>
        <p><strong>🏷️ 活动类型：</strong>{{ currentActivity.type }}</p>
        <p><strong>⏰ 活动时间：</strong>{{ currentActivity.startTime }} 至 {{ currentActivity.endTime }}</p>
        <p><strong>🎁 奖励工时：</strong>{{ currentActivity.rewardHours }} 小时</p>
        <p><strong>👥 招募进度：</strong>{{ currentActivity.currentNum }} / {{ currentActivity.capacity }} 人</p>
        <el-divider border-style="dashed" />
        <p><strong>📝 活动内容与要求：</strong></p>
        <div class="detail-content">
          {{ currentActivity.content || '暂无详细描述' }}
        </div>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="detailVisible = false">关闭</el-button>
          <el-button type="primary" @click="handleApplyAndClose(currentActivity.activityId)">我要报名</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 📝 修改活动弹窗 (响应式适配版) -->
    <el-dialog
        v-model="editVisible"
        title="📝 修改活动信息"
        :width="isMobile ? '95%' : '700px'"
        destroy-on-close
        top="5vh"
        class="edit-dialog"
    >
      <el-form
          :model="editForm"
          :label-width="isMobile ? '80px' : '100px'"
          :label-position="isMobile ? 'top' : 'right'"
          class="edit-form"
      >
        <el-row :gutter="isMobile ? 10 : 20">
          <!-- 🚨 手机端全部单列显示，不再并排 -->
          <el-col :xs="24" :sm="12">
            <el-form-item label="活动标题" required>
              <el-input v-model="editForm.title" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="活动类型" required>
              <el-select v-model="editForm.type" style="width: 100%">
                <el-option label="社区服务" value="社区服务" />
                <el-option label="环境保护" value="环境保护" />
                <el-option label="教育助学" value="教育助学" />
                <el-option label="文明创建" value="文明创建" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="isMobile ? 10 : 20">
          <el-col :xs="24" :sm="12">
            <el-form-item label="活动地点" required>
              <el-input v-model="editForm.location" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="活动状态" required>
              <el-select v-model="editForm.status" style="width: 100%">
                <el-option label="招募中" :value="0" />
                <el-option label="进行中" :value="1" />
                <el-option label="已结束" :value="2" />
                <el-option label="已取消" :value="3" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="isMobile ? 10 : 20">
          <el-col :xs="24">
            <el-form-item label="活动时间" required>
              <el-date-picker
                  v-model="editTimeRange"
                  type="datetimerange"
                  range-separator="至"
                  start-placeholder="开始"
                  end-placeholder="结束"
                  value-format="YYYY-MM-DD HH:mm:ss"
                  @change="handleEditTimeChange"
                  style="width: 100%"
                  :teleported="false"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="isMobile ? 10 : 20">
          <el-col :xs="12">
            <el-form-item label="工时奖励">
              <el-input-number v-model="editForm.rewardHours" :precision="1" :step="0.5" :min="0" :controls="false" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :xs="12">
            <el-form-item label="招募人数">
              <el-input-number v-model="editForm.capacity" :min="1" :controls="false" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="详细内容">
          <el-input v-model="editForm.content" type="textarea" :rows="4" />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="editVisible = false">取 消</el-button>
          <el-button type="primary" @click="submitEdit">保 存</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 报名名单弹窗 (保持原样，宽度已适配) -->
    <el-dialog v-model="applicantVisible" :title="'名单 - ' + selectedActivityTitle" :width="isMobile ? '95%' : '800px'">
      <!-- ... 内容保持不变 ... -->
      <div style="margin-bottom: 15px; display: flex; align-items: center; justify-content: space-between;">
        <span style="font-size: 13px; color: #606266;">
          💡 提示：被拒绝或已取消的记录不占用活动的“已招募”名额。
        </span>
      </div>
      <el-table :data="applicantList" border stripe height="400">
        <el-table-column prop="realName" label="姓名" width="80" />
        <el-table-column label="状态" width="80">
          <template #default="scope">
            <el-tag :type="getRegStatusType(scope.row.status)" size="small">{{ getRegStatusText(scope.row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="applyTime" label="时间" min-width="140" />
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="scope">
            <div v-if="scope.row.status === 0" style="display:flex; gap:5px;">
              <el-button type="success" size="small" circle :icon="Check" @click="handleAuditInList(scope.row, 1)"></el-button>
              <el-button type="danger" size="small" circle :icon="Close" @click="handleAuditInList(scope.row, 2)"></el-button>
            </div>
            <span v-else style="color:#ccc; font-size:12px">已处理</span>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- 🚨 新增：活动签到二维码弹窗 -->
    <el-dialog v-model="qrVisible" title="📱 活动现场打卡码" :width="isMobile ? '80%' : '350px'" center destroy-on-close>
      <div style="display: flex; flex-direction: column; align-items: center; padding: 10px 0;">
        <!-- 显示生成的二维码 Base64 图片 -->
        <img :src="qrCodeUrl" style="width: 220px; height: 220px; border-radius: 8px; box-shadow: 0 4px 12px rgba(0,0,0,0.1);" />

        <p style="color: #409eff; font-weight: bold; margin-top: 20px; font-size: 16px; text-align: center;">
          {{ currentActivityTitle }}
        </p>
        <p style="margin-top: 10px; color: #909399; font-size: 13px; text-align: center; line-height: 1.5;">
          请志愿者使用系统底部的<br/>【扫码签到】功能扫描此码
        </p>
      </div>
    </el-dialog>

  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import request from '../utils/request';
import { Location, Clock, User, Check, Close } from '@element-plus/icons-vue'; // 补全图标引入 🚨
import QRCode from 'qrcode'; //引入纯 JS 二维码生成库


// --- 响应式判断 ---
const isMobile = ref(window.innerWidth <= 768);
const showAddForm = ref(false); // 手机端默认收起发布表单
const handleResize = () => { isMobile.value = window.innerWidth <= 768; };

const userRole = localStorage.getItem('role');
const activityList = ref([]);
const activityTimeRange = ref([]); // 用于绑定日期范围选择器
const detailVisible = ref(false);
const currentActivity = ref(null);
const newActivity = ref({
  title: '',
  type: '',
  location: '',
  rewardHours: 2.0,
  capacity: 10,
  content: '',
  startTime: '',
  endTime: ''
});

const editVisible = ref(false);
const editForm = ref({});
const editTimeRange = ref([]); // 专门用于修改弹窗的时间选择器
const appliedSet = ref(new Set()); // 存放已报名的活动ID

const fetchActivities = async () => {
  const res = await request.get('/api/activity/page');
  activityList.value = res.data.records;

  // 如果是志愿者，额外拉取一下他报名了哪些，用来变灰按钮
  if (userRole === 'VOLUNTEER') {
    const userId = localStorage.getItem('userId');
    const myRes = await request.get(`/api/reg/my?userId=${userId}`);
    // 把 待审核(0), 通过(1), 完结(3), 签到(5) 的提取出来，变灰按钮
    const activeIds = myRes.data
        .filter(reg => [0, 1, 3, 5].includes(reg.status))
        .map(reg => reg.activityId);
    appliedSet.value = new Set(activeIds);
  }
};

// 打开修改弹窗
const openEdit = (item) => {
  // 1. 使用深拷贝，防止直接修改列表数据
  editForm.value = JSON.parse(JSON.stringify(item));

  // 2. 初始化时间选择器回显
  if (editForm.value.startTime && editForm.value.endTime) {
    editTimeRange.value = [editForm.value.startTime, editForm.value.endTime];
  } else {
    editTimeRange.value = [];
  }

  editVisible.value = true;
};

// 处理修改弹窗的时间变化
const handleEditTimeChange = (val) => {
  if (val) {
    editForm.value.startTime = val[0];
    editForm.value.endTime = val[1];
  } else {
    editForm.value.startTime = '';
    editForm.value.endTime = '';
  }
};


// 提交修改到后端
const submitEdit = async () => {
  // 基础校验
  if (!editForm.value.title || !editForm.value.startTime) {
    ElMessage.warning('请填写完整的活动信息');
    return;
  }

  try {
    // 调用之前写的 PUT /api/activity/update 接口
    const res = await request.put('/api/activity/update', editForm.value);
    if (res.code === 200) {
      ElMessage.success('活动信息已更新');
      editVisible.value = false;
      fetchActivities(); // 刷新列表数据
    }
  } catch (error) {
    console.error("修改失败", error);
  }
};

// 处理时间范围选择
const handleTimeChange = (val) => {
  if (val) {
    newActivity.value.startTime = val[0];
    newActivity.value.endTime = val[1];
  }
};

const handleAdd = async () => {
  if (!newActivity.value.title || !newActivity.value.startTime) {
    ElMessage.error('请填写完整的活动信息！');
    return;
  }
  try {
    await request.post('/api/activity/add', newActivity.value);
    ElMessage.success('活动发布成功！');
    resetForm();
    fetchActivities();
  } catch (e) {
    ElMessage.error('发布失败');
  }
};

const resetForm = () => {
  newActivity.value = { title: '', type: '', location: '', rewardHours: 2.0, capacity: 10, content: '', startTime: '', endTime: '' };
  activityTimeRange.value = [];
};

const handleDelete = (id) => {
  ElMessageBox.confirm('确定要删除这个活动吗？', '提示', { type: 'warning' }).then(async () => {
    await request.delete(`/api/activity/${id}`);
    ElMessage.success('删除成功');
    fetchActivities();
  });
};



const handleApply = async (activityId) => {
  const userId = localStorage.getItem('userId');
  if (!userId) {
    ElMessage.error('用户未登录或登录已失效');
    return;
  }

  try {
    // 调用报名接口，传递 userId 和 activityId
    const res = await request.post(`/api/reg/apply?userId=${userId}&activityId=${activityId}`);
    ElMessage.success(res.msg || '报名成功');
    fetchActivities(); // 刷新列表，你能看到页面上的已招募人数 +1
  } catch (e) {
    // 错误在 request.js 中已拦截弹窗，这里无需额外处理
  }
};

// 新增打开详情弹窗的方法
const openDetail = (item) => {
  currentActivity.value = item;
  detailVisible.value = true;
};

// 弹窗里的报名按钮逻辑（报完名自动关弹窗）
const handleApplyAndClose = async (activityId) => {
  await handleApply(activityId);
  detailVisible.value = false; // 关闭弹窗
};

// 响应式变量
const applicantVisible = ref(false);
const applicantList = ref([]);
const selectedActivityTitle = ref('');
const currentActivityId = ref(null);

// 查看名单方法
const viewApplicants = async (activity) => {
  currentActivityId.value = activity.activityId;
  selectedActivityTitle.value = activity.title;
  await fetchApplicants();
  applicantVisible.value = true;
};

// 获取名单数据
const fetchApplicants = async () => {
  const res = await request.get(`/api/reg/admin/activity/${currentActivityId.value}`);
  applicantList.value = res.data;
};

// 审核操作 (在名单弹窗中)
const handleAuditInList = async (row, status) => {
  try {
    await request.put(`/api/reg/admin/audit?regId=${row.regId}&status=${status}`);
    ElMessage.success('审核成功');
    fetchApplicants(); // 刷新名单
    fetchActivities(); // 刷新活动大厅（报名人数可能变化）
  } catch (e) {}
};

// --- 1. 活动状态解析 (用于大厅卡片) ---
const getActivityStatusTag = (s) => {
  const map = { 0: 'success', 1: 'warning', 2: 'info', 3: 'danger' };
  return map[s] || 'info';
};

const getActivityStatusText = (s) => {
  const map = { 0: '招募中', 1: '进行中', 2: '已结束', 3: '已取消' };
  return map[s] || '未知';
};

// --- 2. 报名记录状态解析 (用于名单弹窗表格) ---
const getRegStatusType = (s) => {
  const map = { 0: 'warning', 1: 'primary', 2: 'danger', 3: 'success', 4: 'info', 5: 'warning', 6: 'success' };
  return map[s] || 'info';
};

const getRegStatusText = (s) => {
  const map = { 0: '待审核', 1: '已通过', 2: '已拒绝', 3: '已完结', 4: '已取消', 5: '进行中', 6: '已签退' };
  return map[s] || '未知';
};


// 🚨 新增：二维码相关变量
const qrVisible = ref(false);
const qrCodeUrl = ref('');
const currentActivityTitle = ref('');

// 生成并打开二维码
const openQrCode = async (item) => {
  currentActivityTitle.value = item.title;
  try {
    // 将 activityId 转换为二维码图片
    // margin: 2 控制白边，color 控制前景色和背景色
    qrCodeUrl.value = await QRCode.toDataURL(item.activityId.toString(), {
      width: 300,
      margin: 2,
      color: { dark: '#333333', light: '#ffffff' }
    });
    qrVisible.value = true;
  } catch (err) {
    console.error(err);
    ElMessage.error('生成二维码失败');
  }
};


onMounted(() => {
  fetchActivities();
  window.addEventListener('resize', handleResize);
});
onUnmounted(() => {
  window.removeEventListener('resize', handleResize);
});
</script>

<style scoped>
/* 基础样式 */
.activity-page { padding: 15px; }
.card-header { display: flex; justify-content: space-between; align-items: center; font-weight: bold; }
.section-title { margin: 25px 0 15px; font-size: 18px; color: #303133; border-left: 4px solid #409eff; padding-left: 10px; }

/* 活动卡片样式 */
.activity-card { border-radius: 8px; border: none; box-shadow: 0 2px 12px rgba(0,0,0,0.05); }
.card-top { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 10px; }
.activity-title { font-size: 16px; color: #303133; line-height: 1.4; }

.card-info p { margin: 6px 0; color: #606266; font-size: 13px; display: flex; align-items: center; gap: 6px; }
.full-load { color: #f56c6c; font-weight: bold; }

.card-actions { margin-top: 15px; padding-top: 15px; border-top: 1px solid #f0f0f0; }
.admin-btns, .volunteer-btns { display: flex; justify-content: flex-end; gap: 8px; }

.detail-content { background: #f8f9fa; padding: 10px; border-radius: 4px; white-space: pre-wrap; line-height: 1.6; font-size: 14px; color: #555; }

/* ====================================================
   📱 移动端响应式适配
   ==================================================== */
@media screen and (max-width: 768px) {
  .activity-page { padding: 5px; }

  /* 1. 发布表单：按钮撑满 */
  .form-btn-group { display: flex; gap: 10px; }
  .action-btn { flex: 1; }

  /* 2. 列表卡片：紧凑模式 */
  .activity-card :deep(.el-card__body) { padding: 12px; }
  .card-top { margin-bottom: 8px; }
  .activity-title { font-size: 15px; }
  .card-info p { font-size: 12px; }

  /* 3. 按钮组：手机上允许换行，或者缩小 */
  .admin-btns {
    flex-wrap: wrap;
  }
  .admin-btns .el-button {
    margin-left: 0 !important;
    margin-right: 5px;
    margin-bottom: 5px;
  }

  /* 4. 详情内容 */
  .detail-content { font-size: 13px; }

  /* 🚨 针对编辑弹窗的内部样式 */
  .edit-dialog :deep(.el-dialog__body) {
    padding: 10px 15px; /* 减小内边距 */
  }

  .edit-form .el-form-item {
    margin-bottom: 18px; /* 减小表单项之间的垂直间距 */
  }
}
</style>