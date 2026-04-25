<template>
  <div class="activity-page">
    <el-card class="box-card" v-if="userRole === 'ADMIN'">
      <template #header>
        <div class="card-header">
          <span>📢 发布新的志愿活动</span>
          <el-button v-if="isMobile" size="small" text type="primary" @click="showAddForm = !showAddForm">
            {{ showAddForm ? '收起表单' : '展开填写' }}
          </el-button>
        </div>
      </template>

      <el-form
          v-show="!isMobile || showAddForm"
          :model="newActivity"
          label-width="100px"
          :label-position="isMobile ? 'top' : 'left'"
      >
        <el-row :gutter="20">
          <el-col :xs="24" :sm="12">
            <el-form-item label="活动标题" required>
              <el-input v-model="newActivity.title" placeholder="例如：社区孤寡老人慰问" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="活动类型" required>
              <el-select v-model="newActivity.type" placeholder="请选择类别" style="width: 100%">
                <el-option label="社区服务" value="社区服务" />
                <el-option label="环境保护" value="环境保护" />
                <el-option label="教育助学" value="教育助学" />
                <el-option label="助老服务" value="助老服务" />
                <el-option label="医疗支援" value="医疗支援" />
                <el-option label="文化艺术" value="文化艺术" />
                <el-option label="技术支持" value="技术支持" />
                <el-option label="赛事服务" value="赛事服务" />
                <el-option label="其他" value="其他" />
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
          <el-col :xs="12" :sm="6">
            <el-form-item label="奖励时长">
              <el-input-number v-model="newActivity.rewardHours" :precision="1" :step="0.5" :min="0.5" style="width: 100%" :controls="true" />
            </el-form-item>
          </el-col>
          <el-col :xs="12" :sm="6">
            <el-form-item label="招募人数">
              <el-input-number v-model="newActivity.capacity" :min="1" :max="500" style="width: 100%" :controls="true" />
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
                  :disabled-date="disabledDate"
              />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="技能需求">
              <el-select
                v-model="newActivity.requiredSkills"
                multiple
                filterable
                allow-create
                default-first-option
                collapse-tags
                placeholder="请选择或输入专业技能需求"
                style="width: 100%"
              >
                <el-option v-for="item in skillOptions" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="活动内容">
          <el-input v-model="newActivity.content" type="textarea" :rows="3" placeholder="描述活动流程及要求..." />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleAdd">立即发布</el-button>
          <el-button @click="resetForm">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <div class="lobby-header">
      <h3 class="section-title">🌟 当前活动大厅</h3>
      <div class="search-bar">
        <el-input v-model="queryParams.title" placeholder="搜索标题" style="width: 180px" clearable @change="handleSearch" />
        <el-select v-model="queryParams.status" placeholder="状态" style="width: 110px" clearable @change="handleSearch">
          <el-option label="招募中" :value="0" /><el-option label="进行中" :value="1" /><el-option label="已结束" :value="2" />
        </el-select>
        <el-button type="primary" @click="handleSearch" :icon="Search">查询</el-button>
      </div>
    </div>

    <div v-loading="loading">
      <el-row :gutter="20">
        <el-col :xs="24" :sm="12" :md="8" v-for="item in filteredActivityList" :key="item.activityId" style="margin-bottom: 20px">
          <el-card shadow="hover" class="activity-card">
            <div class="card-top">
              <strong class="activity-title">{{ item.title }}</strong>
              <el-tag :type="getActivityStatusTag(item.status)" size="small">{{ getActivityStatusText(item.status) }}</el-tag>
            </div>
            <div class="card-info">
              <p><el-icon><Location /></el-icon> {{ item.location }}</p>
              <p><el-icon><Clock /></el-icon> {{ item.startTime ? item.startTime.substring(5, 16) : '待定' }}</p>
              <p><el-icon><User /></el-icon> 招募: {{ item.currentNum || 0 }} / {{ item.capacity || 0 }}</p>
              <div v-if="item.requiredSkills" class="skill-tags">
                <el-tag v-for="s in safeParseSkills(item.requiredSkills)" :key="s" size="small" type="info" effect="plain">{{ s }}</el-tag>
              </div>
            </div>
            <div class="card-actions">
              <div v-if="userRole === 'ADMIN'" class="admin-btns">
                <el-button size="small" color="#722ed1" plain @click="openQrCode(item)">签到码</el-button>
                <el-button size="small" type="primary" plain @click="openEdit(item)">修改</el-button>
                <el-button size="small" type="info" plain @click="viewApplicants(item)">名单</el-button>
                <el-button size="small" type="danger" plain @click="handleDelete(item.activityId)">删除</el-button>
              </div>
              <div v-else class="volunteer-btns">
                <el-button size="small" @click="openDetail(item)">详情</el-button>
                <el-button size="small" :type="appliedSet.has(item.activityId) ? 'info' : 'primary'" :disabled="appliedSet.has(item.activityId)" @click="handleApply(item.activityId)">
                  {{ appliedSet.has(item.activityId) ? '已报名' : '立即报名' }}
                </el-button>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
      <div class="pagination-center">
        <el-pagination v-model:current-page="queryParams.page" :page-size="queryParams.pageSize" :total="total" layout="total, prev, pager, next" @current-change="handlePageChange" />
      </div>
    </div>

    <!-- 修改弹窗 -->
    <el-dialog v-model="editVisible" title="📝 修改活动" width="600px" align-center destroy-on-close>
      <el-form :model="editForm" label-width="80px">
        <el-form-item label="标题" required><el-input v-model="editForm.title" /></el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="类型" required>
              <el-select v-model="editForm.type" style="width: 100%">
                <el-option label="社区服务" value="社区服务" />
                <el-option label="环境保护" value="环境保护" />
                <el-option label="教育助学" value="教育助学" />
                <el-option label="助老服务" value="助老服务" />
                <el-option label="医疗支援" value="医疗支援" />
                <el-option label="文化艺术" value="文化艺术" />
                <el-option label="技术支持" value="技术支持" />
                <el-option label="赛事服务" value="赛事服务" />
                <el-option label="其他" value="其他" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" required>
              <el-select v-model="editForm.status" style="width: 100%">
                <el-option label="招募中" :value="0" /><el-option label="进行中" :value="1" /><el-option label="已结束" :value="2" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="技能需求">
          <el-select
            v-model="editForm.requiredSkills"
            multiple
            filterable
            allow-create
            default-first-option
            collapse-tags
            style="width: 100%"
            placeholder="选择或输入专业技能"
          >
            <el-option v-for="item in skillOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="地点" required><el-input v-model="editForm.location" /></el-form-item>
        <el-form-item label="内容"><el-input v-model="editForm.content" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="submitEdit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="applicantVisible" :title="'名单 - ' + selectedActivityTitle" width="800px" align-center destroy-on-close>
      <el-table :data="applicantList" stripe height="350">
        <el-table-column prop="realName" label="姓名" width="100" align="center" />
        <el-table-column label="状态" align="center">
          <template #default="scope"><el-tag :type="getRegStatusType(scope.row.status)">{{ getRegStatusText(scope.row.status) }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="applyTime" label="时间" align="center" />
      </el-table>
    </el-dialog>

    <el-dialog v-model="qrVisible" title="打卡码" width="360px" align-center destroy-on-close>
      <div class="flex flex-col items-center justify-center py-4">
        <div class="bg-white p-2 rounded-xl border border-slate-100 shadow-sm">
          <img :src="qrCodeUrl" class="w-[240px] h-[240px] block" />
        </div>
        <p class="mt-6 font-bold text-slate-800 text-lg">{{ currentActivityTitle }}</p>
        <p class="mt-2 text-xs text-slate-400">请志愿者使用手机端扫码签到</p>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
/**
 * 活动大厅与管理模块 (ActivityManage.vue)
 * 职责：实现志愿活动的发布、编辑、名单审计，以及供志愿者的在线报名服务。
 */
import { ref, onMounted, onUnmounted, computed } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import request from '../../utils/request';
import { activityApi } from '../../api/modules';
import { Location, Clock, User, Check, Close, Warning, Search } from '@element-plus/icons-vue';
import QRCode from 'qrcode';

// 全局响应式状态
const isMobile = ref(window.innerWidth <= 768);
const loading = ref(false);
const showAddForm = ref(false);
const handleResize = () => { isMobile.value = window.innerWidth <= 768; };
const userRole = localStorage.getItem('role');
const activityList = ref([]);
const total = ref(0);

// 技能库定义
const skillOptions = ['医疗急救', '心理疏导', '家电维修', '文艺演出', '法律咨询', '计算机IT', '外语翻译', '手工制作'];
const queryParams = ref({
  page: 1,
  pageSize: 6,
  title: '',
  status: null
});

// 发布新活动相关
const newActivity = ref({
  title: '', type: '', location: '', rewardHours: 2.0, capacity: 10,
  content: '', startTime: '', endTime: '', requiredSkills: []
});
const activityTimeRange = ref([]);

// 修改活动相关
const editVisible = ref(false);
const editForm = ref({ requiredSkills: [] });
const editTimeRange = ref([]);

// 业务交互控制
const detailVisible = ref(false);
const currentActivity = ref(null);
const appliedSet = ref(new Set());

// 状态筛选：前端联动过滤
const filteredActivityList = computed(() => {
  if (queryParams.value.status === null || queryParams.value.status === undefined || queryParams.value.status === '') {
    return activityList.value;
  }
  return activityList.value.filter(act => act.status === queryParams.value.status);
});

// 1. 获取活动列表
const fetchActivities = async () => {
  loading.value = true;
  try {
    const res = await activityApi.getActivities(queryParams.value);
    activityList.value = res.data?.records || [];
    total.value = res.data?.total || 0;

    if (userRole === 'VOLUNTEER') {
      const userId = localStorage.getItem('userId');
      const myRes = await request.get(`/api/reg/my?userId=${userId}`);
      if (myRes.data && Array.isArray(myRes.data)) {
        const activeIds = myRes.data
            .filter(reg => [0, 1, 3, 5, 6].includes(reg.status))
            .map(reg => reg.activityId);
        appliedSet.value = new Set(activeIds);
      }
    }
  } catch (error) {
    console.error("加载活动大厅失败", error);
  } finally {
    loading.value = false;
  }
};

const handleSearch = () => {
  queryParams.value.page = 1;
  fetchActivities();
};

const handlePageChange = (page) => {
  queryParams.value.page = page;
  fetchActivities();
};

const handleApply = async (activityId) => {
  const userId = localStorage.getItem('userId');
  if (!userId) return ElMessage.error('登录状态失效，请重新登录');
  try {
    const res = await request.post(`/api/reg/apply?userId=${userId}&activityId=${activityId}`);
    ElMessage.success(res.msg || '报名申请已提交');
    fetchActivities();
  } catch (e) {}
};

// 限制不能选择过去的时间
const disabledDate = (time) => {
  return time.getTime() < Date.now() - 8.64e7; // 8.64e7 毫秒是一天，允许选择今天
};

const handleAdd = async () => {
  if (!newActivity.value.title || !newActivity.value.startTime) {
    return ElMessage.error('请填写活动标题与起止时间！');
  }

  // 校验：奖励时长和招募人数
  if (newActivity.value.rewardHours <= 0) {
    return ElMessage.error('奖励时长必须大于0！');
  }
  if (newActivity.value.capacity <= 0) {
    return ElMessage.error('招募人数必须大于0！');
  }

  // 校验：禁止发布过去的时间
  const now = new Date();
  const selectedStart = new Date(newActivity.value.startTime);
  if (selectedStart < now) {
    return ElMessage.error('活动开始时间不能早于当前时间！');
  }

  try {
    const submitData = JSON.parse(JSON.stringify(newActivity.value));
    const skillsJson = JSON.stringify(submitData.requiredSkills || []);
    // 兼容性修复：同时发送驼峰和下划线字段，确保数据库一定能存入
    submitData.requiredSkills = skillsJson;
    submitData.required_skills = skillsJson;

    await request.post('/api/activity/add', submitData);
    ElMessage.success('活动发布成功！');
    resetForm();
    fetchActivities();
  } catch (e) {}
};

const submitEdit = async () => {
  if (!editForm.value.title || !editForm.value.startTime) return ElMessage.warning('信息不完整');

  if (editForm.value.rewardHours <= 0) return ElMessage.error('奖励时长必须大于0');
  if (editForm.value.capacity <= 0) return ElMessage.error('招募人数必须大于0');

  try {
    const submitData = JSON.parse(JSON.stringify(editForm.value));
    const skillsJson = JSON.stringify(submitData.requiredSkills || []);

    submitData.requiredSkills = skillsJson;
    submitData.required_skills = skillsJson;

    const res = await request.put('/api/activity/update', submitData);
    if (res.code === 200) {
      ElMessage.success('活动信息已更新');
      editVisible.value = false;
      fetchActivities();
    }
  } catch (error) {}
};

const handleDelete = (id) => {
  ElMessageBox.confirm('删除活动将导致关联报名记录断裂，是否继续？', '风险操作提示', { type: 'error' }).then(async () => {
    await request.delete(`/api/activity/${id}`);
    ElMessage.success('活动已删除');
    fetchActivities();
  }).catch(() => {});
};

const handleTimeChange = (val) => {
  if (val) { newActivity.value.startTime = val[0]; newActivity.value.endTime = val[1]; }
};
const handleEditTimeChange = (val) => {
  if (val) { editForm.value.startTime = val[0]; editForm.value.endTime = val[1]; }
};
const resetForm = () => {
  newActivity.value = { title: '', type: '', location: '', rewardHours: 2.0, capacity: 10, content: '', startTime: '', endTime: '', requiredSkills: [] };
  activityTimeRange.value =[];
};

const openDetail = (item) => { currentActivity.value = item; detailVisible.value = true; };
const handleApplyAndClose = async (activityId) => { await handleApply(activityId); detailVisible.value = false; };

const openEdit = (item) => {
  const data = JSON.parse(JSON.stringify(item));
  try {
    data.requiredSkills = JSON.parse(data.requiredSkills || '[]');
  } catch (e) {
    data.requiredSkills = [];
  }
  editForm.value = data;
  editTimeRange.value = (item.startTime && item.endTime) ? [item.startTime, item.endTime] :[];
  editVisible.value = true;
};

const applicantVisible = ref(false);
const applicantList = ref([]);
const selectedActivityTitle = ref('');
const currentActivityId = ref(null);

const viewApplicants = async (activity) => {
  currentActivityId.value = activity.activityId;
  selectedActivityTitle.value = activity.title;
  const res = await request.get(`/api/reg/admin/activity/${activity.activityId}`);
  applicantList.value = res.data;
  applicantVisible.value = true;
};

const handleAuditInList = async (row, status) => {
  try {
    await request.put(`/api/reg/admin/audit?regId=${row.regId}&status=${status}`);
    ElMessage.success('审核操作成功');
    viewApplicants({ activityId: currentActivityId.value, title: selectedActivityTitle.value });
    fetchActivities();
  } catch (e) {}
};

const qrVisible = ref(false);
const qrCodeUrl = ref('');
const currentActivityTitle = ref('');

const openQrCode = async (item) => {
  currentActivityTitle.value = item.title;
  try {
    qrCodeUrl.value = await QRCode.toDataURL(item.activityId.toString(), {
      width: 300, margin: 2, color: { dark: '#333333', light: '#ffffff' }
    });
    qrVisible.value = true;
  } catch (err) { ElMessage.error('生成打卡二维码失败'); }
};

const getActivityStatusTag = (s) => ({ 0: 'success', 1: 'warning', 2: 'info', 3: 'danger' }[s] || 'info');
const getActivityStatusText = (s) => ({ 0: '招募中', 1: '进行中', 2: '已结束', 3: '已取消' }[s] || '未知');
const getRegStatusType = (s) => ({ 0: 'warning', 1: 'primary', 2: 'danger', 3: 'success', 4: 'info', 5: 'warning', 6: 'success' }[s] || 'info');
const getRegStatusText = (s) => ({ 0: '待审', 1: '通过', 2: '拒绝', 3: '完结', 4: '取消', 5: '签到', 6: '签退' }[s] || '未知');

const safeParseSkills = (jsonStr) => {
  try { return JSON.parse(jsonStr || '[]'); } catch (e) { return []; }
};

onMounted(() => { fetchActivities(); window.addEventListener('resize', handleResize); });
onUnmounted(() => { window.removeEventListener('resize', handleResize); });
</script>

<style scoped>
.activity-page { padding: 20px; }
.lobby-header { 
  display: flex; 
  justify-content: space-between; 
  align-items: center; 
  margin: 30px 0 15px; 
  flex-wrap: wrap; /* 允许换行 */
  gap: 15px;
}
.search-bar { display: flex; gap: 10px; flex-wrap: wrap; }
.section-title { 
  font-size: 18px; 
  border-left: 4px solid #409eff; 
  padding-left: 10px; 
  margin: 0; 
  white-space: nowrap; /* 禁止标题文字内部换行 */
}
.activity-card { margin-bottom: 20px; }
.card-top { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 12px; }
.activity-title { font-size: 15px; flex: 1; margin-right: 10px; }
.card-info p { margin: 5px 0; font-size: 13px; color: #666; display: flex; align-items: center; gap: 5px; }
.skill-tags { margin-top: 8px; display: flex; flex-wrap: wrap; gap: 5px; }
.card-actions { margin-top: 15px; padding-top: 12px; border-top: 1px dashed #eee; display: flex; justify-content: flex-end; }
.admin-btns, .volunteer-btns { display: flex; gap: 8px; }
.pagination-center { display: flex; justify-content: center; margin-top: 25px; }

/* 📱 移动端适配 */
@media screen and (max-width: 768px) {
  .activity-page { padding: 10px; }
  .lobby-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
  .search-bar {
    width: 100%;
  }
  .search-bar :deep(.el-input),
  .search-bar :deep(.el-select) {
    width: 100% !important;
    margin-bottom: 5px;
  }
}
</style>