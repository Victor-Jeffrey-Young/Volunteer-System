<template>
  <div class="wish-pool-container">
    <el-tabs v-model="activeTab" class="custom-tabs">
      <!-- 微心愿池大厅 -->
      <el-tab-pane label="微心愿大厅" name="pool">
        <div class="wish-grid" v-loading="loading">
          <div v-for="item in wishPool" :key="item.wishId" class="wish-card">
            <div class="wish-content">
              <div class="wish-header">
                <el-tag size="small" type="warning" effect="dark" round>{{ item.category }}</el-tag>
                <span class="wish-time">{{ formatTime(item.createTime) }}</span>
              </div>
              <h3 class="wish-title">{{ item.title }}</h3>
              <p class="wish-desc">{{ item.content }}</p>
              
              <div class="requester-box">
                <el-icon><User /></el-icon>
                <span>发起人: <strong>{{ item.requesterName }}</strong></span>
              </div>
              
              <div class="address-box">
                <el-icon><Location /></el-icon>
                <span>服务地点: {{ item.address || '联系确认'}}</span>
              </div>
            </div>
            
            <div class="wish-footer">
              <el-button type="primary" round class="claim-btn" @click="handleClaim(item.wishId)">
                立即揭榜
              </el-button>
            </div>
          </div>
          
          <el-empty v-if="wishPool.length === 0" description="暂无心愿，快去喝杯茶吧" class="empty-state" />
        </div>
      </el-tab-pane>

      <!-- 我的任务 -->
      <el-tab-pane label="我的任务" name="my">
        <div class="wish-grid" v-loading="loading">
          <div v-for="item in myWishes" :key="item.wishId" class="wish-card my-wish-card">
            <div class="wish-content">
              <div class="wish-header">
                <el-tag size="small" :type="getStatusType(item.status)" effect="dark" round>
                  {{ getStatusText(item.status) }}
                </el-tag>
                <span class="wish-time">{{ formatTime(item.createTime) }}</span>
              </div>
              <h3 class="wish-title">{{ item.title }}</h3>
              
              <!-- 认领后展示完整信息 -->
              <div class="contact-info">
                <p><strong>联系电话:</strong> {{ item.requesterPhone }}</p>
                <p><strong>服务地址:</strong> {{ item.address }}</p>
              </div>

              <div v-if="item.remarks" class="remarks-box">
                反馈: {{ item.remarks }}
              </div>
              
              <div style="margin-top: 10px;">
                <el-button type="info" link size="small" @click="showProgress(item)">查看心愿流转轨迹</el-button>
              </div>
            </div>
            
            <div v-if="item.status === 2" class="wish-footer">
              <el-button type="success" round class="claim-btn" @click="handleFinish(item.wishId)">
                我已提供服务，申请完成
              </el-button>
            </div>
            <div v-else-if="item.status === 5 || item.status === 6" class="wish-footer">
              <el-button type="info" plain size="small" disabled class="claim-btn">
                {{ item.status === 5 ? '等待居民确认中...' : '居民已确认，待结算' }}
              </el-button>
            </div>
          </div>
          
          <el-empty v-if="myWishes.length === 0" description="目前还没有领过心愿哦" class="empty-state" />
        </div>
      </el-tab-pane>
    </el-tabs>

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
            <p style="font-size: 12px; color: #16a34a; font-weight: bold;" v-if="[2,3,5,6].includes(selectedWish.status)">就是您接单啦！辛苦！</p>
            <p style="font-size: 12px; color: #94a3b8;" v-else>等待认领...</p>
          </el-timeline-item>

          <el-timeline-item timestamp="您标记完成" placement="top" :color="[3,5,6].includes(selectedWish.status) ? '#22c55e' : '#cbd5e1'">
            <p style="font-size: 12px; color: #94a3b8;">{{ [3,5,6].includes(selectedWish.status) ? '您已上报完工' : '等待服务完成并上报' }}</p>
          </el-timeline-item>

          <el-timeline-item timestamp="居民确认评价" placement="top" :color="[3,6].includes(selectedWish.status) ? '#f97316' : '#cbd5e1'">
            <p style="font-size: 12px; color: #94a3b8;">{{ [3,6].includes(selectedWish.status) ? '居民已点下确认按钮' : '等待居民校验' }}</p>
            <p style="font-size: 12px; font-weight:bold; color: #ea580c; margin-top:2px;" v-if="selectedWish.isLiked===1">居民为您送上了一个真诚的点赞 👍！</p>
          </el-timeline-item>

          <el-timeline-item timestamp="后台核发积分" placement="top" :color="selectedWish.status === 3 ? '#22c55e' : '#cbd5e1'">
            <p style="font-size: 12px; color: #94a3b8;">{{ selectedWish.status === 3 ? '报酬已入账，功德+1' : '等待管理员最后结算' }}</p>
          </el-timeline-item>
        </el-timeline>
      </div>
    </el-drawer>

  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue';
import { User, Location } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import request from '../../utils/request';

const activeTab = ref('pool');
const loading = ref(false);
const wishPool = ref([]);
const myWishes = ref([]);
const userId = localStorage.getItem('userId');

// 路由
const fetchPool = async () => {
  loading.value = true;
  try {
    const res = await request.get('/api/wish/pool');
    wishPool.value = res.data;
  } finally {
    loading.value = false;
  }
};

const fetchMyWishes = async () => {
  loading.value = true;
  try {
    const res = await request.get('/api/wish/my', {
      params: { userId, role: 'VOLUNTEER' }
    });
    myWishes.value = res.data;
  } finally {
    loading.value = false;
  }
};

// 监听 Tab 切换
watch(activeTab, (val) => {
  if (val === 'pool') fetchPool();
  else fetchMyWishes();
});

// 认领心愿
const handleClaim = (wishId) => {
  ElMessageBox.confirm(
    '认领后请务必尽快与居民沟通并落实服务，确定揭榜吗？',
    '认领确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'success',
    }
  ).then(async () => {
    await request.put(`/api/wish/claim?wishId=${wishId}&userId=${userId}`);
    ElMessage.success('揭榜成功！您已进入办理流程。');
    fetchPool();
  });
};

const handleFinish = (wishId) => {
  ElMessageBox.confirm(
    '确定已完成该心愿服务吗？提交后将通知居民进行确认。',
    '提示',
    { type: 'warning' }
  ).then(async () => {
    await request.put(`/api/wish/finish?wishId=${wishId}`);
    ElMessage.success('状态已更新，由于流程优化，需居民确认后方可进行奖励结算。');
    fetchMyWishes();
  });
};

const drawerVisible = ref(false);
const selectedWish = ref(null);
const showProgress = (row) => {
  selectedWish.value = row;
  drawerVisible.value = true;
};

const formatTime = (t) => t ? t.substring(0, 10) : '--';

const getStatusType = (s) => ({2: 'warning', 3: 'success', 5: 'success', 6: 'primary'})[s] || 'info';
const getStatusText = (s) => ({2: '办理中', 3: '已达成', 5: '待确认', 6: '待结算'})[s] || '未知';

onMounted(() => {
  fetchPool();
});
</script>

<style scoped>
.wish-pool-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

/* 选项卡个性化 */
:deep(.el-tabs__header) {
  margin-bottom: 30px;
}

.wish-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 24px;
}

.wish-card {
  background: white;
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 4px 15px rgba(0,0,0,0.05);
  display: flex;
  flex-direction: column;
  transition: all 0.3s;
  border: 1px solid #f1f5f9;
}

.wish-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 10px 25px rgba(0,0,0,0.1);
}

.wish-content {
  padding: 20px;
  flex: 1;
}

.wish-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.wish-time {
  font-size: 11px;
  color: #94a3b8;
}

.wish-title {
  font-size: 18px;
  font-weight: 800;
  color: #1e293b;
  margin-bottom: 10px;
}

.wish-desc {
  font-size: 14px;
  color: #64748b;
  line-height: 1.6;
  margin-bottom: 20px;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.requester-box, .address-box {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #475569;
  margin-bottom: 6px;
}

.wish-footer {
  padding: 16px 20px;
  background: #fcfcfc;
  border-top: 1px solid #f8fafc;
}

.claim-btn {
  width: 100%;
  padding: 12px 0;
  font-size: 14px;
  font-weight: bold;
}

.contact-info {
  background: #f0f9ff;
  padding: 12px;
  border-radius: 12px;
  margin-top: 10px;
  font-size: 12px;
}

.contact-info p {
  margin-bottom: 4px;
}

.remarks-box {
  margin-top: 12px;
  padding: 8px;
  background: #fff7ed;
  color: #ea580c;
  font-size: 12px;
  border-radius: 8px;
}

.empty-state {
  grid-column: 1 / -1;
  width: 100%;
}

/* 移动端适配 */
@media (max-width: 640px) {
  .wish-grid {
    grid-template-columns: 1fr;
    gap: 16px;
  }
}
</style>
