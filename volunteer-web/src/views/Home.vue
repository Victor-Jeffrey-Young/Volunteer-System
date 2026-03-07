<template>
  <div class="home-container">
    <!-- 顶部：核心数据看板 -->
    <div class="stat-cards">
      <div class="card card-1">
        <h4>总志愿者人数</h4>
        <p class="num">{{ stats.volCount }} <span class="unit">人</span></p>
      </div>
      <div class="card card-2">
        <h4>全社区累计志愿时长</h4>
        <p class="num">{{ stats.totalHours }} <span class="unit">小时</span></p>
      </div>
      <div class="card card-3">
        <h4>招募/进行中的活动</h4>
        <p class="num">{{ stats.activeCount }} <span class="unit">个</span></p>
      </div>
    </div>

    <!-- 底部：系统新闻与公告区 -->
    <el-card class="notice-card" shadow="hover">
      <template #header>
        <div class="notice-header">
          <el-icon><Bell /></el-icon>
          <span>社区新闻与官方通知</span>
        </div>
      </template>

      <el-collapse v-model="activeNames" accordion v-if="noticeList.length > 0">
        <el-collapse-item v-for="(item, index) in noticeList" :key="item.noticeId" :name="index">
          <template #title>
            <div class="collapse-title-box">
              <el-tag :type="item.type === 1 ? 'danger' : 'success'" size="small" class="type-tag">
                {{ item.type === 1 ? '通知' : '新闻' }}
              </el-tag>
              <!-- 这里的 title-text 是核心 -->
              <span class="title-text">{{ item.title }}</span>
              <span class="time-text">{{ item.createTime.split(' ')[0] }}</span>
            </div>
          </template>

          <div class="notice-content">
            {{ item.content }}
          </div>
          <div class="notice-footer" v-if="item.publisherName">
            —— 发布人：{{ item.publisherName }}
          </div>
        </el-collapse-item>
      </el-collapse>

      <el-empty v-else description="近期暂无公告信息" :image-size="60" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { Bell } from '@element-plus/icons-vue';
import request from '../utils/request';

const stats = ref({ volCount: 0, totalHours: 0, activeCount: 0 });
const noticeList = ref([]);
const activeNames = ref([0]);

const fetchStats = async () => {
  try {
    const res = await request.get('/api/dashboard/base');
    stats.value = res.data;
  } catch (error) {
    console.error("加载统计数据失败", error);
  }
};

const fetchNotices = async () => {
  try {
    const res = await request.get('/api/notice/page', {
      params: { current: 1, size: 5 }
    });
    noticeList.value = res.data.records;
  } catch (error) {
    console.error("加载公告数据失败", error);
  }
};

onMounted(() => {
  fetchStats();
  fetchNotices();
});
</script>

<style scoped>
/* ====================================================
   🖥️ 默认样式 (PC 端)
   ==================================================== */
.home-container { padding: 10px; }

.stat-cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.card {
  padding: 25px 20px;
  border-radius: 12px;
  color: white;
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
  transition: transform 0.3s;
}
.card:hover { transform: translateY(-5px); }

.card-1 { background: linear-gradient(135deg, #ff6b6b, #ff8787); }
.card-2 { background: linear-gradient(135deg, #4facfe, #00f2fe); }
.card-3 { background: linear-gradient(135deg, #667eea, #764ba2); }

h4 { margin: 0; font-size: 16px; font-weight: normal; opacity: 0.9; }
.num { font-size: 32px; font-weight: bold; margin-top: 12px; margin-bottom: 0; }
.unit { font-size: 14px; font-weight: normal; margin-left: 4px; }

.notice-card { margin-top: 25px; border-radius: 12px; }
.notice-header { font-weight: bold; font-size: 16px; display: flex; align-items: center; gap: 8px; }

.collapse-title-box {
  display: flex;
  align-items: center;
  width: 100%;
  padding-right: 15px;
}
.type-tag { margin-right: 10px; flex-shrink: 0; }
.title-text {
  font-size: 15px;
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.time-text { color: #999; font-size: 13px; margin-left: 10px; flex-shrink: 0; }

.notice-content {
  white-space: pre-wrap;
  color: #606266;
  line-height: 1.8;
  padding: 12px;
  background: #f8f9fa;
  border-radius: 6px;
  font-size: 14px;
}
.notice-footer {
  text-align: right;
  font-size: 12px;
  color: #999;
  margin-top: 8px;
}

/* ====================================================
   📱 移动端响应式精修
   ==================================================== */
@media screen and (max-width: 768px) {
  /* 1. 强制隐藏主内容区可能出现的滚动条 */
  .home-container {
    padding: 10px 5px;
    overflow-x: hidden;
  }

  /* 2. 优化公告卡片标题 */
  .collapse-title-box {
    display: flex;
    align-items: center;
    width: 100%;
    /* 重点：限制盒子最大宽度，防止撑破 */
    max-width: calc(100vw - 60px);
    padding-right: 5px;
  }

  .title-text {
    font-size: 14px;
    color: #333;
    flex: 1; /* 占据剩余所有空间 */
    /* 🚨 核心三行代码：多出部分显示省略号 */
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    margin-right: 8px;
  }

  .time-text {
    font-size: 11px;
    color: #999;
    flex-shrink: 0; /* 禁止时间被压缩 */
  }

  .type-tag {
    flex-shrink: 0; /* 禁止标签被压缩 */
    transform: scale(0.9); /* 手机端稍微缩小一点标签 */
  }

  /* 3. 统计卡片字号微调，防止大数字溢出 */
  .num {
    font-size: 22px;
    letter-spacing: -0.5px;
  }
}
</style>