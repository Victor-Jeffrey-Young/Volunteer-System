<template>
  <div class="activity-page">
    <!-- ========================================== -->
    <!-- 📢 管理员操作区：发布新活动 -->
    <!-- ========================================== -->
    <el-card class="box-card" v-if="userRole === 'ADMIN'">
      <template #header>
        <div class="card-header">
          <span>📢 发布新的志愿活动</span>
          <!-- 移动端交互优化：增加表单的收起/展开按钮，防止占用过多屏幕空间 -->
          <el-button v-if="isMobile" size="small" text type="primary" @click="showAddForm = !showAddForm">
            {{ showAddForm ? '收起表单' : '展开填写' }}
          </el-button>
        </div>
      </template>

      <!-- 响应式表单布局：PC端固定显示，移动端受 showAddForm 控制 -->
      <el-form
          v-show="!isMobile || showAddForm"
          :model="newActivity"
          :label-width="isMobile ? '80px' : '100px'"
          :label-position="isMobile ? 'top' : 'left'"
      >
        <!-- 响应式栅格系统：PC端(sm以上)双列，移动端(xs)单列 -->
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
          <!-- 奖励与人数在移动端依然保持单行双列并排，节省垂直高度 -->
          <el-col :xs="12" :sm="6">
            <el-form-item label="奖励时长">
              <el-input-number v-model="newActivity.rewardHours" :precision="1" :step="0.5" :min="0" style="width: 100%" :controls="false" />
            </el-form-item>
          </el-col>
          <el-col :xs="12" :sm="6">
            <el-form-item label="招募人数">
              <el-input-number v-model="newActivity.capacity" :min="1" :max="500" style="width: 100%" :controls="false" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :xs="24" :sm="12">
            <el-form-item label="活动时间" required>
              <!-- :teleported="false" 是解决移动端日历弹窗错位/撑破布局的核心属性 -->
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
          <el-input v-model="newActivity.content" type="textarea" :rows="3" placeholder="请详细描述活动流程及志愿者要求..." />
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
    <!-- 🌟 公共展示区：活动大厅瀑布流 -->
    <!-- ========================================== -->
    <h3 class="section-title">🌟 当前活动大厅</h3>
    <el-row :gutter="20">
      <!-- 采用响应式排版：手机1列，平板2列，PC端3列 -->
      <el-col
          :xs="24" :sm="12" :md="8"
          v-for="item in activityList"
          :key="item.activityId"
          style="margin-bottom: 20px"
      >
        <el-card :body-style="{ padding: '15px' }" shadow="hover" class="activity-card">
          <!-- 卡片头部信息 -->
          <div class="card-top">
            <strong class="activity-title">{{ item.title }}</strong>
            <!-- 采用独立的 Activity 状态解析器，避免与报名状态冲突 -->
            <el-tag :type="getActivityStatusTag(item.status)" size="small" effect="light">
              {{ getActivityStatusText(item.status) }}
            </el-tag>
          </div>

          <!-- 卡片核心指标 -->
          <div class="card-info">
            <p><el-icon><Location /></el-icon> {{ item.location }}</p>
            <p><el-icon><Clock /></el-icon> {{ item.startTime.substring(5, 16) }}</p>
            <p>
              <el-icon><User /></el-icon> 招募:
              <!-- 人数满载时高亮显示告警色 -->
              <span :class="{'full-load': item.currentNum >= item.capacity}">
                {{ item.currentNum }} / {{ item.capacity }}
              </span>
            </p>
          </div>

          <!-- 角色分化操作区 -->
          <div class="card-actions">
            <!-- 1. 管理员操作按钮组 -->
            <div v-if="userRole === 'ADMIN'" class="admin-btns">
              <el-button size="small" color="#722ed1" plain @click="openQrCode(item)">签到码</el-button>
              <el-button size="small" type="primary" plain @click="openEdit(item)">修改</el-button>
              <el-button size="small" type="info" plain @click="viewApplicants(item)">名单</el-button>
              <el-button size="small" type="danger" plain @click="handleDelete(item.activityId)">删除</el-button>
            </div>

            <!-- 2. 志愿者操作按钮组 -->
            <div v-else class="volunteer-btns">
              <el-button size="small" @click="openDetail(item)">详情</el-button>
              <!-- 业务校验：基于 appliedSet 判断是否已在报名流程中，动态置灰按钮 -->
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

    <!-- ========================================== -->
    <!-- 📄 志愿者弹窗：活动详情浏览 -->
    <!-- ========================================== -->
    <el-dialog v-model="detailVisible" title="志愿活动详情" :width="isMobile ? '95%' : '500px'" destroy-on-close>
      <div v-if="currentActivity" class="activity-detail-box">
        <h3 style="color: #409eff; margin-top: 0; line-height: 1.4;">{{ currentActivity.title }}</h3>
        <p><strong>📍 活动地点：</strong>{{ currentActivity.location }}</p>
        <p><strong>🏷️ 活动类型：</strong>{{ currentActivity.type }}</p>
        <p><strong>⏰ 活动时间：</strong>{{ currentActivity.startTime }} 至 {{ currentActivity.endTime }}</p>
        <p><strong>🎁 奖励工时：</strong>{{ currentActivity.rewardHours }} 小时</p>
        <p><strong>👥 招募进度：</strong>{{ currentActivity.currentNum }} / {{ currentActivity.capacity }} 人</p>
        <el-divider border-style="dashed" />
        <p><strong>📝 活动内容与要求：</strong></p>
        <!-- pre-wrap 保证后台录入的回车换行能原样渲染 -->
        <div class="detail-content">{{ currentActivity.content || '暂无详细描述' }}</div>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="detailVisible = false">关闭</el-button>
          <el-button type="primary" @click="handleApplyAndClose(currentActivity.activityId)">我要报名</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- ========================================== -->
    <!-- 📝 管理员弹窗：编辑/修改活动信息 -->
    <!-- ========================================== -->
    <el-dialog
        v-model="editVisible"
        title="📝 修改活动信息"
        :width="isMobile ? '95%' : '700px'"
        destroy-on-close
        top="5vh"
        class="edit-dialog"
    >
      <el-form :model="editForm" :label-width="isMobile ? '80px' : '100px'" :label-position="isMobile ? 'top' : 'right'" class="edit-form">
        <el-row :gutter="isMobile ? 10 : 20">
          <el-col :xs="24" :sm="12">
            <el-form-item label="活动标题" required><el-input v-model="editForm.title" /></el-form-item>
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
            <el-form-item label="活动地点" required><el-input v-model="editForm.location" /></el-form-item>
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
              <!-- 修复移动端日历弹窗被截断/错位的问题 -->
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
            <el-form-item label="工时奖励"><el-input-number v-model="editForm.rewardHours" :precision="1" :step="0.5" :min="0" :controls="false" style="width: 100%" /></el-form-item>
          </el-col>
          <el-col :xs="12">
            <el-form-item label="招募人数"><el-input-number v-model="editForm.capacity" :min="1" :controls="false" style="width: 100%" /></el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="详细内容"><el-input v-model="editForm.content" type="textarea" :rows="4" /></el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="editVisible = false">取 消</el-button>
          <el-button type="primary" @click="submitEdit">保 存</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- ========================================== -->
    <!-- 👥 管理员弹窗：指定活动报名名单审计 -->
    <!-- ========================================== -->
    <el-dialog v-model="applicantVisible" :title="'名单审计 - ' + selectedActivityTitle" :width="isMobile ? '95%' : '800px'" destroy-on-close>
      <div style="margin-bottom: 15px; display: flex; align-items: center; justify-content: space-between;">
        <span style="font-size: 13px; color: #606266;">
          <el-icon color="#e6a23c"><Warning /></el-icon> 提示：被拒绝或已取消的记录不占用活动有效名额。
        </span>
      </div>
      <!-- 名单表格视图 -->
      <el-table :data="applicantList" border stripe height="400">
        <el-table-column prop="realName" label="姓名" width="80" align="center" />
        <el-table-column label="状态" width="85" align="center">
          <template #default="scope">
            <!-- 采用独立的 Reg 状态解析器 -->
            <el-tag :type="getRegStatusType(scope.row.status)" size="small" effect="dark">{{ getRegStatusText(scope.row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="applyTime" label="申请时间" min-width="150" align="center" />
        <el-table-column label="操作" width="120" fixed="right" align="center">
          <template #default="scope">
            <div v-if="scope.row.status === 0" style="display:flex; gap:10px; justify-content: center;">
              <el-button type="success" size="small" circle :icon="Check" @click="handleAuditInList(scope.row, 1)" title="通过"></el-button>
              <el-button type="danger" size="small" circle :icon="Close" @click="handleAuditInList(scope.row, 2)" title="拒绝"></el-button>
            </div>
            <span v-else style="color:#ccc; font-size:12px">已流转</span>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- ========================================== -->
    <!-- 📱 管理员弹窗：动态生成 O2O 签到二维码 -->
    <!-- ========================================== -->
    <el-dialog v-model="qrVisible" title="📱 现场活动打卡码" :width="isMobile ? '85%' : '350px'" center destroy-on-close>
      <div style="display: flex; flex-direction: column; align-items: center; padding: 10px 0;">
        <img :src="qrCodeUrl" style="width: 220px; height: 220px; border-radius: 8px; box-shadow: 0 4px 12px rgba(0,0,0,0.15);" />
        <p style="color: #409eff; font-weight: bold; margin-top: 20px; font-size: 16px; text-align: center; line-height: 1.4;">
          {{ currentActivityTitle }}
        </p>
        <p style="margin-top: 10px; color: #909399; font-size: 13px; text-align: center; line-height: 1.6;">
          请志愿者使用系统底部的<br/>【扫码签到】功能进行打卡
        </p>
      </div>
    </el-dialog>

  </div>
</template>

<script setup>
/**
 * 活动大厅与管理模块 (ActivityManage.vue)
 * 职责：实现志愿活动的发布、编辑、名单审计，以及供志愿者的在线报名服务。
 */
import { ref, onMounted, onUnmounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import request from '../utils/request';
import { Location, Clock, User, Check, Close, Warning } from '@element-plus/icons-vue';
import QRCode from 'qrcode'; // JS 纯前端二维码生成器

// --- 全局响应式状态 ---
const isMobile = ref(window.innerWidth <= 768);
const showAddForm = ref(false); // 控制移动端发布表单的折叠状态
const handleResize = () => { isMobile.value = window.innerWidth <= 768; };

const userRole = localStorage.getItem('role');
const activityList = ref([]);

// --- 发布新活动相关 ---
const newActivity = ref({ title: '', type: '', location: '', rewardHours: 2.0, capacity: 10, content: '', startTime: '', endTime: '' });
const activityTimeRange = ref([]);

// --- 修改活动相关 ---
const editVisible = ref(false);
const editForm = ref({});
const editTimeRange = ref([]);

// --- 业务交互控制 ---
const detailVisible = ref(false);
const currentActivity = ref(null);
const appliedSet = ref(new Set()); // 存放当前志愿者已报名的 Activity ID 集合（防呆设计）

// 1. 获取活动列表
const fetchActivities = async () => {
  try {
    const res = await request.get('/api/activity/page');
    activityList.value = res.data.records;

    // 针对志愿者：提前拉取历史报名记录，用于视图层报名按钮的置灰判断
    if (userRole === 'VOLUNTEER') {
      const userId = localStorage.getItem('userId');
      const myRes = await request.get(`/api/reg/my?userId=${userId}`);
      // 提取正处于有效生命周期的状态码
      const activeIds = myRes.data
          .filter(reg => [0, 1, 3, 5, 6].includes(reg.status))
          .map(reg => reg.activityId);
      appliedSet.value = new Set(activeIds);
    }
  } catch (error) { console.error("加载活动大厅失败", error); }
};

// 2. 志愿者：发起报名请求
const handleApply = async (activityId) => {
  const userId = localStorage.getItem('userId');
  if (!userId) return ElMessage.error('登录状态失效，请重新登录');

  try {
    const res = await request.post(`/api/reg/apply?userId=${userId}&activityId=${activityId}`);
    ElMessage.success(res.msg || '报名申请已提交');
    fetchActivities(); // 刷新大厅，更新按钮状态及招募人数
  } catch (e) { /* 异常交由 Axios 全局拦截处理 */ }
};

// 3. 管理员：新增发布活动
const handleAdd = async () => {
  if (!newActivity.value.title || !newActivity.value.startTime) {
    return ElMessage.error('请填写活动标题与起止时间！');
  }
  try {
    await request.post('/api/activity/add', newActivity.value);
    ElMessage.success('活动发布成功！');
    resetForm();
    fetchActivities();
  } catch (e) {}
};

// 4. 管理员：保存活动修改 (基于深拷贝对象提交)
const submitEdit = async () => {
  if (!editForm.value.title || !editForm.value.startTime) return ElMessage.warning('信息不完整');
  try {
    const res = await request.put('/api/activity/update', editForm.value);
    if (res.code === 200) {
      ElMessage.success('活动信息已更新');
      editVisible.value = false;
      fetchActivities();
    }
  } catch (error) {}
};

// 5. 管理员：物理删除活动
const handleDelete = (id) => {
  ElMessageBox.confirm('删除活动将导致关联报名记录断裂，是否继续？', '风险操作提示', { type: 'error' }).then(async () => {
    await request.delete(`/api/activity/${id}`);
    ElMessage.success('活动已删除');
    fetchActivities();
  }).catch(() => {});
};

// --- 表单时间处理辅助函数 ---
const handleTimeChange = (val) => {
  if (val) { newActivity.value.startTime = val[0]; newActivity.value.endTime = val[1]; }
};
const handleEditTimeChange = (val) => {
  if (val) { editForm.value.startTime = val[0]; editForm.value.endTime = val[1]; }
};
const resetForm = () => {
  newActivity.value = { title: '', type: '', location: '', rewardHours: 2.0, capacity: 10, content: '', startTime: '', endTime: '' };
  activityTimeRange.value =[];
};

// --- 志愿者：打开详情预览 ---
const openDetail = (item) => { currentActivity.value = item; detailVisible.value = true; };
const handleApplyAndClose = async (activityId) => { await handleApply(activityId); detailVisible.value = false; };

// --- 管理员：打开修改弹窗 (利用 JSON 深拷贝解耦视图层) ---
const openEdit = (item) => {
  editForm.value = JSON.parse(JSON.stringify(item));
  editTimeRange.value = (item.startTime && item.endTime) ? [item.startTime, item.endTime] :[];
  editVisible.value = true;
};


// ================== 名单审计子模块 ==================
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

// ================== 扫码签到 O2O 模块 ==================
const qrVisible = ref(false);
const qrCodeUrl = ref('');
const currentActivityTitle = ref('');

// 管理员生成携带 ActivityID 的 O2O 签到核销码
const openQrCode = async (item) => {
  currentActivityTitle.value = item.title;
  try {
    qrCodeUrl.value = await QRCode.toDataURL(item.activityId.toString(), {
      width: 300, margin: 2, color: { dark: '#333333', light: '#ffffff' }
    });
    qrVisible.value = true;
  } catch (err) { ElMessage.error('生成打卡二维码失败'); }
};

// ================== 状态枚举字典 (命名空间解耦) ==================

// 1. 活动主状态 (Activity 表)
const getActivityStatusTag = (s) => ({ 0: 'success', 1: 'warning', 2: 'info', 3: 'danger' }[s] || 'info');
const getActivityStatusText = (s) => ({ 0: '招募中', 1: '进行中', 2: '已结束', 3: '已取消' }[s] || '未知');

// 2. 报名流转状态 (Registration 表)
const getRegStatusType = (s) => ({ 0: 'warning', 1: 'primary', 2: 'danger', 3: 'success', 4: 'info', 5: 'warning', 6: 'success' }[s] || 'info');
const getRegStatusText = (s) => ({ 0: '待审', 1: '通过', 2: '拒绝', 3: '完结', 4: '取消', 5: '签到', 6: '签退' }[s] || '未知');

// --- 生命周期 ---
onMounted(() => { fetchActivities(); window.addEventListener('resize', handleResize); });
onUnmounted(() => { window.removeEventListener('resize', handleResize); });
</script>

<style scoped>
/* 🖥️ PC 端基础样式 */
.activity-page { padding: 15px; }
.card-header { display: flex; justify-content: space-between; align-items: center; font-weight: bold; }
.section-title { margin: 25px 0 15px; font-size: 18px; color: #303133; border-left: 4px solid #409eff; padding-left: 10px; }

/* 瀑布流活动卡片 */
.activity-card { border-radius: 8px; border: none; box-shadow: 0 2px 12px rgba(0,0,0,0.05); }
.card-top { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 10px; }
.activity-title { font-size: 16px; color: #303133; line-height: 1.4; }

.card-info p { margin: 6px 0; color: #606266; font-size: 13px; display: flex; align-items: center; gap: 6px; }
.full-load { color: #f56c6c; font-weight: bold; }

.card-actions { margin-top: 15px; padding-top: 15px; border-top: 1px dashed #eee; }
.admin-btns, .volunteer-btns { display: flex; justify-content: flex-end; gap: 8px; }

.detail-content { background: #f8f9fa; padding: 12px; border-radius: 6px; white-space: pre-wrap; line-height: 1.6; font-size: 14px; color: #555; border-left: 3px solid #dcdfe6; }

/* 📱 移动端响应式核心适配 (<= 768px) */
@media screen and (max-width: 768px) {
  .activity-page { padding: 5px; }

  /* 1. 表单全宽适配 */
  .form-btn-group { display: flex; gap: 10px; }
  .action-btn { flex: 1; }

  /* 2. 列表卡片空间压缩 */
  .activity-card :deep(.el-card__body) { padding: 12px; }
  .card-top { margin-bottom: 8px; }
  .activity-title { font-size: 15px; }
  .card-info p { font-size: 12px; }

  /* 3. 管理员多按钮换行处理，防止撑破容器 */
  .admin-btns {
    flex-wrap: wrap;
    gap: 6px; /* 利用 gap 替代 margin */
  }
  .admin-btns .el-button {
    margin: 0 !important;
  }

  /* 4. 详情与弹窗的精细化 */
  .detail-content { font-size: 13px; }
  .edit-dialog :deep(.el-dialog__body) { padding: 10px 15px; }
  .edit-form .el-form-item { margin-bottom: 18px; }
}
</style>