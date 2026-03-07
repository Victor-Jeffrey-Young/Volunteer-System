<template>
  <div class="activity-page">
    <el-card class="box-card" v-if="userRole === 'ADMIN'">
      <template #header>
        <div class="card-header">
          <span>📢 发布新的志愿活动</span>
        </div>
      </template>

      <!-- 明确的表单设计 -->
      <el-form :model="newActivity" label-width="100px" label-position="left">
        <el-row :gutter="20">
          <!-- 活动标题 -->
          <el-col :span="12">
            <el-form-item label="活动标题" required>
              <el-input v-model="newActivity.title" placeholder="例如：社区孤寡老人慰问" />
            </el-form-item>
          </el-col>

          <!-- 活动类型 -->
          <el-col :span="12">
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
          <!-- 活动地点 -->
          <el-col :span="12">
            <el-form-item label="活动地点" required>
              <el-input v-model="newActivity.location" placeholder="具体街道或社区名称" />
            </el-form-item>
          </el-col>

          <!-- 奖励时长 -->
          <el-col :span="6">
            <el-form-item label="奖励时长">
              <el-input-number v-model="newActivity.rewardHours" :precision="1" :step="0.5" :min="0" />
              <span style="margin-left: 5px">小时</span>
            </el-form-item>
          </el-col>

          <!-- 招募人数 -->
          <el-col :span="6">
            <el-form-item label="招募人数">
              <el-input-number v-model="newActivity.capacity" :min="1" :max="500" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <!-- 开始与结束时间 -->
          <el-col :span="12">
            <el-form-item label="活动时间" required>
              <el-date-picker
                  v-model="activityTimeRange"
                  type="datetimerange"
                  range-separator="至"
                  start-placeholder="开始日期"
                  end-placeholder="结束日期"
                  value-format="YYYY-MM-DD HH:mm:ss"
                  @change="handleTimeChange"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 活动内容 -->
        <el-form-item label="活动内容">
          <el-input
              v-model="newActivity.content"
              type="textarea"
              :rows="3"
              placeholder="请详细描述活动流程及志愿者要求..."
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleAdd">立即发布活动</el-button>
          <el-button @click="resetForm">重置表单</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 活动列表展示部分 -->
    <h3 style="margin-top: 30px">🌟 当前活动大厅</h3>
    <el-row :gutter="20">
      <el-col
          :xs="24"
          :sm="12"
          :md="8"
          v-for="item in activityList"
          :key="item.activityId"
          style="margin-bottom: 20px"
      >
        <el-card :body-style="{ padding: '15px' }" shadow="hover">
          <div style="display: flex; justify-content: space-between; align-items: center">
            <strong style="font-size: 16px; color: #409eff">{{ item.title }}</strong>
            <el-tag :type="getActivityStatusTag(item.status)">{{ getActivityStatusText(item.status) }}</el-tag>
          </div>
          <p style="font-size: 13px; color: #666">📍 地点：{{ item.location }}</p>
          <p style="font-size: 13px; color: #666">⏰ 时间：{{ item.startTime.substring(0, 16) }}</p>
          <div style="display: flex; justify-content: space-between; align-items: center">
            <span style="font-size: 12px; color: #999">已招募：{{ item.currentNum }}/{{ item.capacity }}</span>
            <!-- 活动列表中管理员操作区 -->
            <div v-if="userRole === 'ADMIN'" style="margin-top: 10px;">
              <el-button size="small" type="primary" plain @click="openEdit(item)">修改</el-button>
              <el-button size="small" type="info" plain @click="viewApplicants(item)">报名名单</el-button>
              <el-button size="small" type="danger" plain @click="handleDelete(item.activityId)">删除</el-button>
            </div>
            <div v-else>
              <el-button size="small" @click="openDetail(item)">详情</el-button>
              <!-- 如果 appliedSet 里面有这个活动 ID，就禁用按钮并改字 -->
              <el-button size="small" :type="appliedSet.has(item.activityId) ? 'info' : 'primary'"
                         :disabled="appliedSet.has(item.activityId)"
                         @click="handleApply(item.activityId)">
                {{ appliedSet.has(item.activityId) ? '已报名' : '立即报名' }}
              </el-button>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>


    <!-- 活动详情弹窗 -->
    <el-dialog v-model="detailVisible" title="志愿活动详情" width="500px">
      <div v-if="currentActivity" class="activity-detail-box">
        <h3 style="color: #409eff; margin-top: 0;">{{ currentActivity.title }}</h3>
        <p><strong>📍 活动地点：</strong>{{ currentActivity.location }}</p>
        <p><strong>🏷️ 活动类型：</strong>{{ currentActivity.type }}</p>
        <p><strong>⏰ 活动时间：</strong>{{ currentActivity.startTime }} 至 {{ currentActivity.endTime }}</p>
        <p><strong>🎁 奖励工时：</strong>{{ currentActivity.rewardHours }} 小时</p>
        <p><strong>👥 招募进度：</strong>{{ currentActivity.currentNum }} / {{ currentActivity.capacity }} 人</p>

        <el-divider border-style="dashed" />

        <p><strong>📝 活动内容与要求：</strong></p>
        <!-- 使用 pre-wrap 保证后端存的换行符能正常显示 -->
        <div style="background: #f4f4f5; padding: 10px; border-radius: 4px; white-space: pre-wrap; font-size: 14px; color: #606266; line-height: 1.6;">
          {{ currentActivity.content || '暂无详细描述' }}
        </div>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="detailVisible = false">关 闭</el-button>
          <el-button type="primary" @click="handleApplyAndClose(currentActivity.activityId)">我要报名</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 编辑活动弹窗 -->
    <el-dialog
        v-model="editVisible"
        title="📝 修改活动信息"
        width="650px"
        destroy-on-close
        class="custom-dialog"
    >
      <el-form
          :model="editForm"
          label-width="100px"
          label-position="right"
          style="padding: 10px 20px 0 10px"
      >
        <!-- 第一行：标题 -->
        <el-form-item label="活动标题" required>
          <el-input v-model="editForm.title" placeholder="请输入清晰的活动名称" />
        </el-form-item>

        <!-- 第二行：类型与状态 -->
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="活动类型" required>
              <el-select v-model="editForm.type" placeholder="请选择" style="width: 100%">
                <el-option label="社区服务" value="社区服务" />
                <el-option label="环境保护" value="环境保护" />
                <el-option label="教育助学" value="教育助学" />
                <el-option label="文明创建" value="文明创建" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="活动状态" required>
              <el-select v-model="editForm.status" placeholder="请选择" style="width: 100%">
                <el-option label="招募中" :value="0" />
                <el-option label="进行中" :value="1" />
                <el-option label="已结束" :value="2" />
                <el-option label="已取消" :value="3" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 第三行：地点 -->
        <el-form-item label="活动地点" required>
          <el-input v-model="editForm.location" placeholder="请输入详细活动地址">
            <template #prefix>
              <el-icon><Location /></el-icon>
            </template>
          </el-input>
        </el-form-item>

        <!-- 第四行：时间范围 -->
        <el-form-item label="活动时间" required>
          <el-date-picker
              v-model="editTimeRange"
              type="datetimerange"
              range-separator="至"
              start-placeholder="开始时间"
              end-placeholder="结束时间"
              value-format="YYYY-MM-DD HH:mm:ss"
              style="width: 100%"
              @change="handleEditTimeChange"
          />
        </el-form-item>

        <!-- 第五行：奖励与人数 -->
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="工时奖励">
              <el-input-number
                  v-model="editForm.rewardHours"
                  :precision="1"
                  :step="0.5"
                  :min="0"
                  controls-position="right"
                  style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="招募人数">
              <el-input-number
                  v-model="editForm.capacity"
                  :min="1"
                  controls-position="right"
                  style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 第六行：详细内容 -->
        <el-form-item label="详细内容">
          <el-input
              v-model="editForm.content"
              type="textarea"
              :rows="4"
              placeholder="请输入活动具体要求、注意事项等信息..."
              resize="none"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="editVisible = false" round>取 消</el-button>
          <el-button type="primary" @click="submitEdit" round>保 存 修 改</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 报名名单弹窗 -->
    <el-dialog v-model="applicantVisible" :title="'名单 - ' + selectedActivityTitle" width="800px">
      <!-- 🚨 新增：在表格上方加一个温馨提示，解释数据口径 -->
      <div style="margin-bottom: 15px; display: flex; align-items: center; justify-content: space-between;">
        <span style="font-size: 13px; color: #606266;">
          💡 提示：本列表包含所有历史申请记录。被拒绝或已取消的记录不占用活动的“已招募”名额。
        </span>
        <el-tag type="info" effect="plain">
          总记录数：{{ applicantList.length }} 条
        </el-tag>
      </div>
      <el-table :data="applicantList" border stripe height="400">
        <el-table-column prop="realName" label="志愿者姓名" width="120" />
        <el-table-column prop="applyTime" label="报名时间" width="160" />
        <el-table-column label="状态" width="120">
          <template #default="scope">
            <el-tag :type="getRegStatusType(scope.row.status)">{{ getRegStatusText(scope.row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="180">
          <template #default="scope">
            <!-- 复用审核逻辑 -->
            <div v-if="scope.row.status === 0">
              <el-button type="success" size="small" @click="handleAuditInList(scope.row, 1)">通过</el-button>
              <el-button type="danger" size="small" @click="handleAuditInList(scope.row, 2)">拒绝</el-button>
            </div>
            <!-- 签退成功后可以发放工时 -->
            <div v-else-if="[1, 5, 6].includes(scope.row.status)">
              <el-button type="primary" size="small" @click="handleGrantInList(scope.row)">发放工时</el-button>
            </div>
            <span v-else style="color: #999; font-size: 12px;">流程已完结</span>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import request from '../utils/request';
import {Location} from "@element-plus/icons-vue";


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

const getStatusTag = (s) => {
  const map = { 0: 'success', 1: 'warning', 2: 'info', 3: 'danger' };
  return map[s] || 'info';
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

// 发放工时：你可以选择跳转到专门的审核页面，或者弹窗
const handleGrantInList = (row) => {
  // 这里逻辑可以参考之前写的发放工时弹窗逻辑
  // 为了简单起见，提示用户去“报名审核”菜单操作，或在此复用弹窗
  ElMessage.info('正在为您准备工时发放...');
  // 也可以通过 router.push({ path: '/registrations' }) 跳转过去
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

onMounted(fetchActivities);
</script>