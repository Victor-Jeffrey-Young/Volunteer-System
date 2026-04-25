<template>
  <div class="resident-wish-container">
    <!-- 顶部状态概览 -->
    <div class="header-section">
      <div class="stat-card">
        <h2 class="section-title">✨ 我的微心愿</h2>
        <p class="section-subtitle">提交您的困难或需求，社区志愿者将竭诚为您服务</p>
      </div>
      <el-button color="#f97316" size="large" round class="add-wish-btn" @click="openDialog">
        <el-icon style="margin-right: 6px;"><Plus /></el-icon>发布新求助
      </el-button>
    </div>

    <!-- 心愿列表 (进度轨迹式) -->
    <div class="wish-list-wrapper" v-loading="loading">
      <div v-for="item in myWishes" :key="item.wishId" class="wish-item-card">
        <div class="card-left">
          <div class="category-badge">{{ item.category }}</div>
          <h3 class="wish-title">{{ item.title }}</h3>
          <p class="wish-content">{{ item.content }}</p>
          <div class="wish-meta">
            <span>📅 发布于: {{ formatTime(item.createTime) }}</span>
            <span v-if="item.address">📍 地点: {{ item.address }}</span>
            <span v-if="item.isLiked === 1" class="liked-badge">已点赞 👍</span>
          </div>
        </div>

        <div class="card-right">
          <div class="status-indicator">
            <el-tag :color="item.status === 1 ? '#fff7ed' : ''" :style="{ color: item.status === 1 ? '#ea580c' : '', borderColor: item.status === 1 ? '#fed7aa' : '' }" :type="getStatusType(item.status)" effect="light" round>
              {{ getStatusText(item.status) }}
            </el-tag>
          </div>
          
          <!-- 认领后展示志愿者信息 -->
          <div v-if="[2, 3, 5, 6].includes(item.status)" class="volunteer-info">
            <el-avatar size="small" :src="getFullAvatar(item.volunteerAvatar)" />
            <div class="volunteer-details">
              <span class="volunteer-name">{{ item.volunteerName || '爱心志愿者' }} 已接单</span>
              <span v-if="item.volunteerPhone" class="volunteer-phone">
                📞 {{ item.volunteerPhone }}
              </span>
            </div>
          </div>

          <div v-if="item.remarks" class="feedback-msg">
            <el-icon><ChatDotRound /></el-icon> {{ item.remarks }}
          </div>

          <div class="card-actions">
            <!-- 现有的状态5确认按钮 -->
            <el-button 
              v-if="item.status === 5"
              color="#16a34a"
              size="small"
              round
              @click="handleConfirm(item.wishId)"
              style="margin-bottom: 8px; color: white !important;"
            >
              确认服务已完成
            </el-button>

            <!-- 新增：对于被拒绝的记录，提供自肃删除功能 -->
            <el-button 
              v-if="item.status === 4"
              color="#ef4444"
              size="small"
              round
              @click="handleDeleteWish(item.wishId)"
              style="margin-bottom: 8px; color: white !important;"
            >
              <el-icon style="margin-right: 4px;"><Delete /></el-icon> 删除记录
            </el-button>

            <!-- 状态已确认/已达成，且没点过赞，即可单独补充点赞 -->
            <el-button 
              v-if="(item.status === 6 || item.status === 3) && item.isLiked !== 1"
              color="#f59e0b"
              size="small"
              round
              @click="handleStandaloneLike(item.wishId)"
              style="margin-bottom: 8px; color: white !important;"
            >
              为 TA 点赞 👍
            </el-button>

            <el-button 
              color="#f97316"
              link 
              @click="showProgress(item)"
              class="progress-btn"
            >
              查看进度轨迹
            </el-button>
          </div>
        </div>
      </div>

      <el-empty v-if="myWishes.length === 0" description="您还没有发布过心愿哦" />
    </div>

    <!-- 发布弹窗 -->
    <el-dialog 
      v-model="dialogVisible" 
      title="🏮 发布互助微心愿" 
      :width="dialogWidth"
      class="wish-dialog"
    >
      <el-form :model="form" label-position="top">
        <el-form-item label="心愿概要 (简单一句话)" required>
          <el-input v-model="form.title" placeholder="例如：急需帮取大件快递 / 水管漏水求修" maxlength="30" show-word-limit />
        </el-form-item>
        
        <el-form-item label="心愿类别" required>
          <el-select v-model="form.category" placeholder="请选择类别" style="width: 100%;">
            <el-option label="跑腿代办" value="跑腿代办" />
            <el-option label="日常维修" value="日常维修" />
            <el-option label="心理疏导" value="心理疏导" />
            <el-option label="环境清洁" value="环境清洁" />
            <el-option label="助老/医疗" value="助老/医疗" />
            <el-option label="其他求助" value="其他" />
          </el-select>
        </el-form-item>

        <el-form-item label="详细说明您的困难 (志愿者会根据此内容决定是否认领)">
          <el-input v-model="form.content" type="textarea" :rows="3" placeholder="请详细描述具体的困难、时间要求等..." />
        </el-form-item>

        <el-form-item label="服务地点">
          <el-input v-model="form.address" placeholder="例如：幸福里小区3号楼2单元501" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button color="#f97316" :loading="submitting" @click="handleSubmit">确认发布</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 进度详情抽屉 -->
    <el-drawer
      v-model="drawerVisible"
      title="心愿进度轨迹"
      size="400px"
      class="progress-drawer"
    >
      <div v-if="selectedWish" class="drawer-content">
        <div class="wish-summary" style="margin-bottom: 30px;">
          <div class="category-badge">{{ selectedWish.category }}</div>
          <h3 style="font-size: 20px; font-weight: bold; color: #1e293b; margin-bottom: 8px;">{{ selectedWish.title }}</h3>
          <p style="font-size: 14px; color: #64748b;">{{ selectedWish.content }}</p>
        </div>

        <el-timeline v-if="selectedWish.status !== 4">
          <!-- 1. 发布 -->
          <el-timeline-item timestamp="发布成功" placement="top" color="#f97316">
            <p style="font-size: 12px; color: #94a3b8;">{{ formatTimeFull(selectedWish.createTime) }}</p>
            <p style="font-size: 12px; font-weight: 500; margin-top: 4px;">您已成功提交心愿，正在等待管理员审核。</p>
          </el-timeline-item>

          <!-- 2. 管理员审核通过 -->
          <el-timeline-item
            timestamp="管理员审核通过 (心愿公示中)"
            placement="top"
            :color="selectedWish.status >= 1 ? '#3b82f6' : '#cbd5e1'"
          >
            <p v-if="selectedWish.status >= 1" style="font-size: 12px; font-weight: 500;">您的心愿已审核通过，正式进入心愿池，等待志愿者认领。</p>
            <p v-else style="font-size: 12px; color: #94a3b8;">等待管理员审核上架...</p>
          </el-timeline-item>

          <!-- 3. 志愿者认领 -->
          <el-timeline-item
            timestamp="志愿者已揭榜"
            placement="top"
            :color="[2,3,5,6].includes(selectedWish.status) ? '#eab308' : '#cbd5e1'"
          >
            <div v-if="[2,3,5,6].includes(selectedWish.status)">
              <div style="display: flex; align-items: center; gap: 8px; margin-top: 4px;">
                <el-avatar :size="20" :src="getFullAvatar(selectedWish.volunteerAvatar)" />
                <p style="font-size: 12px; font-weight: bold; color: #f97316;">志愿者 {{ selectedWish.volunteerName }} 正在赶来</p>
              </div>
              <div v-if="selectedWish.volunteerPhone" style="margin-top: 6px; padding: 6px 10px; background: #ecfdf5; border-radius: 6px; border: 1px solid #a7f3d0;">
                <p style="font-size: 11px; color: #059669; font-weight: 500;">📞 联系电话：{{ selectedWish.volunteerPhone }}</p>
              </div>
            </div>
            <p v-else style="font-size: 12px; color: #94a3b8;">等待热心志愿者接单...</p>
          </el-timeline-item>

          <!-- 4. 志愿者服务完成 -->
          <el-timeline-item
            timestamp="志愿者标记完成"
            placement="top"
            :color="[3,5,6].includes(selectedWish.status) ? '#22c55e' : '#cbd5e1'"
          >
            <p v-if="[3,5,6].includes(selectedWish.status)" style="font-size: 12px; font-weight: 500; color: #16a34a;">志愿者已提交服务完成登记，邀请您核实确认。</p>
            <p v-else style="font-size: 12px; color: #94a3b8;">等待志愿者上门服务并完工...</p>
          </el-timeline-item>

          <!-- 5. 居民确认 -->
          <el-timeline-item
            timestamp="居民确认评价"
            placement="top"
            :color="[3,6].includes(selectedWish.status) ? '#f97316' : '#cbd5e1'"
          >
            <div v-if="[3,6].includes(selectedWish.status)">
              <p style="font-size: 12px; font-weight: 500;">您已在此环节确认了该服务圆满完成。{{ selectedWish.isLiked === 1 ? '您的点赞已送达！👍' : '' }}</p>
              <div v-if="selectedWish.remarks && selectedWish.remarks.startsWith('居民评价')" style="background:#fff7ed; padding:10px; border-radius:8px; border:1px solid #fed7aa; margin-top:8px;">
                <p style="font-size:12px; color:#c2410c; font-style:italic;">💬 "{{ selectedWish.remarks.replace('居民评价: ', '') }}"</p>
              </div>
            </div>
            <p v-else style="font-size: 12px; color: #94a3b8;">服务完成后，需要您亲自点击确认...</p>
          </el-timeline-item>

          <!-- 6. 管理员结算 -->
          <el-timeline-item
            timestamp="管理员核实与奖励发放"
            placement="top"
            :color="selectedWish.status === 3 ? '#22c55e' : '#cbd5e1'"
          >
            <div v-if="selectedWish.status === 3">
              <p style="font-size: 12px; color: #94a3b8;">{{ formatTimeFull(selectedWish.finishTime) }}</p>
              <div style="background: #f0fdf4; padding: 8px; border-radius: 8px; margin-top: 8px; border: 1px solid #dcfce7;">
                <p style="font-size: 12px; font-weight: bold; color: #15803d;">管理员已发放奖励，任务圆满功德结项。</p>
              </div>
            </div>
            <p v-else style="font-size: 12px; color: #94a3b8;">等待所有确认完毕后，系统将正式发放奖励...</p>
          </el-timeline-item>
        </el-timeline>

        <!-- 针对驳回状态的单独显示 -->
        <el-timeline v-else>
          <el-timeline-item timestamp="发布成功" placement="top" color="#f97316">
            <p style="font-size: 12px; color: #94a3b8;">{{ formatTimeFull(selectedWish.createTime) }}</p>
            <p style="font-size: 12px; font-weight: 500; margin-top: 4px;">您已成功提交心愿。</p>
          </el-timeline-item>
          <el-timeline-item timestamp="管理员审核驳回" placement="top" color="#ef4444">
            <p style="font-size: 12px; font-weight: bold; color: #ef4444;">抱歉，您的求助未通过审核。</p>
            <div v-if="selectedWish.remarks" style="background: #fef2f2; padding: 8px; border-radius: 8px; margin-top: 8px; border: 1px solid #fee2e2;">
              <p style="font-size: 12px; color: #991b1b;">驳回原因：{{ selectedWish.remarks }}</p>
            </div>
          </el-timeline-item>
        </el-timeline>

      </div>
    </el-drawer>

    <!-- 满意度核实验收弹窗 -->
    <el-dialog
      v-model="reviewDialogVisible"
      title="💝 心愿服务验收与评价"
      width="400px"
      style="border-radius: 16px;"
    >
      <div style="margin-bottom: 20px; font-size: 14px; color: #475569; line-height: 1.6;">
        志愿者已提交完工申请，请确认服务是否符合预期，并为 TA 留下宝贵的评价鼓励吧！
      </div>

      <el-form label-position="top">
        <el-form-item label="是否为志愿者点赞 👍">
          <el-switch v-model="reviewForm.liked" active-color="#f59e0b" inactive-color="#e2e8f0" active-text="送上点赞" inactive-text="不需要" />
        </el-form-item>
        <el-form-item label="写几句感谢留言 (选填)">
          <el-input v-model="reviewForm.rateMsg" type="textarea" :rows="3" placeholder="例如：师傅上门很快，非常热心解决问题，手动点赞。..." />
        </el-form-item>
      </el-form>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="reviewDialogVisible = false">暂不处理</el-button>
          <el-button color="#16a34a" style="color: white;" @click="submitConfirm">正式完成验收</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, onUnmounted } from 'vue';
import { useRoute } from 'vue-router';
import { Plus, ChatDotRound, Delete } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import request from '../../utils/request';
import { getFullAvatar } from '../../utils/file';

const route = useRoute();

const loading = ref(false);
const submitting = ref(false);
const dialogVisible = ref(false);
const drawerVisible = ref(false);
const selectedWish = ref(null);
const windowWidth = ref(window.innerWidth);

// 响应式对话框宽度
const dialogWidth = computed(() => {
  return windowWidth.value < 768 ? '90%' : '500px';
});

const myWishes = ref([]);
const userId = localStorage.getItem('userId');

const form = ref({
  requesterId: userId,
  title: '',
  category: '其他',
  content: '',
  address: ''
});

const fetchMyWishes = async () => {
  loading.value = true;
  try {
    const res = await request.get('/api/wish/my', {
      params: { userId, role: 'RESIDENT' }
    });
    myWishes.value = res.data;
  } finally {
    loading.value = false;
  }
};

const openDialog = () => {
  form.value = {
    requesterId: userId,
    title: '',
    category: '其他',
    content: '',
    address: ''
  };
  dialogVisible.value = true;
};

const handleSubmit = async () => {
  if (!form.value.title || !form.value.content) {
    return ElMessage.warning('请填写完整的标题和描述');
  }
  submitting.value = true;
  try {
    await request.post('/api/wish/apply', form.value);
    ElMessage.success('心愿发布成功！请等待管理员审核。');
    dialogVisible.value = false;
    fetchMyWishes();
  } finally {
    submitting.value = false;
  }
};

const reviewDialogVisible = ref(false);
const reviewForm = ref({ wishId: null, liked: true, rateMsg: '' });

const handleConfirm = (wishId) => {
  reviewForm.value = { wishId, liked: true, rateMsg: '' };
  reviewDialogVisible.value = true;
};

const submitConfirm = async () => {
  try {
    const { wishId, liked, rateMsg } = reviewForm.value;
    await request.put(`/api/wish/confirm?wishId=${wishId}&liked=${liked}&rateMsg=${encodeURIComponent(rateMsg)}`);
    ElMessage.success('验收通过，您的评价记录已发布！社区感谢您的热情参与。');
    reviewDialogVisible.value = false;
    fetchMyWishes();
  } catch (error) {
    // 错误统一由 request.js 处理
  }
};

const handleStandaloneLike = async (wishId) => {
  try {
    await request.put(`/api/wish/like?wishId=${wishId}`);
    ElMessage.success('点赞成功！心意已传达给志愿者。👍');
    fetchMyWishes();
  } catch (error) {
    // 错误处理由 request.js 统一接管
  }
};

const handleDeleteWish = (wishId) => {
  ElMessageBox.confirm(
    '确定要删除这条被拒绝的求助记录吗？删除后列表将不再显示该心愿。',
    '删除确认',
    {
      confirmButtonText: '确定删除',
      cancelButtonText: '暂不删除',
      type: 'warning',
      confirmButtonClass: 'el-button--danger'
    }
  ).then(async () => {
    try {
      await request.delete(`/api/wish/${wishId}`);
      ElMessage.success('清理成功！该记录已从您的列表中移除。');
      fetchMyWishes();
    } catch (error) {
      // 错误统一由 request.js 处理
    }
  }).catch(() => {});
};

const showProgress = (wish) => {
  selectedWish.value = wish;
  drawerVisible.value = true;
};

const formatTime = (t) => t ? t.substring(0, 10) : '--';
const formatTimeFull = (t) => t ? t.replace('T', ' ').substring(0, 16) : '--';

const getStatusType = (s) => ({
  0: 'info',
  1: 'primary',
  2: 'warning',
  3: 'success',
  4: 'danger',
  5: 'success',
  6: 'warning'
})[s] || 'info';

const getStatusText = (s) => ({
  0: '待审核',
  1: '待认领',
  2: '认领中',
  3: '已达成',
  4: '已驳回',
  5: '志愿者已完成',
  6: '待管理员结算'
})[s] || '未知';

const handleResize = () => {
  windowWidth.value = window.innerWidth;
};

onMounted(() => {
  fetchMyWishes();
  window.addEventListener('resize', handleResize);
  
  // 检查是否需要自动打开弹窗
  if (route.query.action === 'create') {
    openDialog();
  }
});

onUnmounted(() => {
  window.removeEventListener('resize', handleResize);
});
</script>

<style scoped>
.resident-wish-container {
  max-width: 900px;
  margin: 0 auto;
  padding: 30px 20px;
}

.header-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 40px;
}

.section-title {
  font-size: 26px;
  font-weight: 800;
  color: #1e293b;
  margin-bottom: 8px;
}

.section-subtitle {
  color: #64748b;
  font-size: 14px;
}

.add-wish-btn {
  padding: 12px 24px;
  font-weight: bold;
  color: white !important;
  box-shadow: 0 4px 12px rgba(249, 115, 22, 0.3);
}

.wish-item-card {
  background: white;
  border-radius: 20px;
  padding: 24px;
  margin-bottom: 20px;
  display: flex;
  justify-content: space-between;
  border: 1px solid #f1f5f9;
  box-shadow: 0 2px 8px rgba(0,0,0,0.02);
  transition: all 0.3s;
}

.wish-item-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(0,0,0,0.06);
}

.category-badge {
  background: #fff7ed;
  color: #ea580c;
  padding: 2px 10px;
  border-radius: 6px;
  font-size: 11px;
  font-weight: bold;
  display: inline-block;
  margin-bottom: 10px;
  border: 1px solid #ffedd5;
}

.wish-title {
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
  margin-bottom: 8px;
}

.wish-content {
  color: #475569;
  font-size: 14px;
  line-height: 1.6;
  margin-bottom: 16px;
}

.wish-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  font-size: 12px;
  color: #94a3b8;
}

.liked-badge {
  color: #f59e0b;
  font-weight: bold;
  background: #fffbeb;
  padding: 0 8px;
  border-radius: 4px;
}

.card-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  justify-content: center;
  min-width: 140px;
}

.volunteer-info {
  margin-top: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
  background: #f0fdf4;
  padding: 6px 12px;
  border-radius: 999px;
}

.volunteer-details {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.volunteer-name {
  font-size: 12px;
  color: #16a34a;
  font-weight: bold;
}

.volunteer-phone {
  font-size: 11px;
  color: #15803d;
  font-weight: 500;
}

.feedback-msg {
  margin-top: 12px;
  font-size: 12px;
  color: #ef4444;
  background: #fef2f2;
  padding: 6px 12px;
  border-radius: 8px;
  width: 100%;
}

.card-actions {
  margin-top: 20px;
  width: 100%;
  display: flex;
  justify-content: flex-end;
}

.progress-btn {
  font-weight: 600;
  font-size: 13px;
}

.drawer-content {
  padding: 0 10px;
}

.timeline-item :deep(.el-timeline-item__content) {
  margin-top: -6px;
}

/* 弹窗样式优化 */
:deep(.wish-dialog) {
  border-radius: 20px;
}
:deep(.el-form-item__label) {
  font-weight: 600;
  padding-bottom: 4px !important;
}

@media (max-width: 640px) {
  .header-section {
    flex-direction: column;
    align-items: flex-start;
    gap: 20px;
    margin-bottom: 24px;
  }
  
  .wish-item-card {
    flex-direction: column;
    padding: 16px;
  }

  .card-right {
    align-items: flex-start;
    margin-top: 16px;
    padding-top: 16px;
    border-top: 1px dashed #e2e8f0;
    width: 100%;
  }

  .section-title {
    font-size: 20px;
  }

  .wish-meta {
    flex-direction: column;
    gap: 4px;
  }
}
</style>
