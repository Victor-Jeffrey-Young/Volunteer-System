<template>
  <div class="home-container">
    <!-- 顶部：核心数据看板 -->
    <div class="stat-cards">
      <div class="card card-1">
        <h4>总志愿者人数</h4>
        <p class="num">{{ stats.volCount }} <span style="font-size: 14px; font-weight: normal;">人</span></p>
      </div>
      <div class="card card-2">
        <h4>全社区累计志愿时长</h4>
        <p class="num">{{ stats.totalHours }} <span style="font-size: 14px; font-weight: normal;">小时</span></p>
      </div>
      <div class="card card-3">
        <h4>招募/进行中的活动</h4>
        <p class="num">{{ stats.activeCount }} <span style="font-size: 14px; font-weight: normal;">个</span></p>
      </div>
    </div>

    <!-- 底部：系统新闻与公告区 -->
    <el-card style="margin-top: 30px; border-radius: 8px;" shadow="hover">
      <template #header>
        <div style="font-weight: bold; font-size: 16px; display: flex; align-items: center;">
          <el-icon style="margin-right: 8px; color: #f56c6c;"><Bell /></el-icon>
          社区新闻与官方通知
        </div>
      </template>

      <!-- 使用 Element Plus 的折叠面板展示公告 -->
      <el-collapse v-model="activeNames" accordion v-if="noticeList.length > 0">
        <el-collapse-item v-for="(item, index) in noticeList" :key="item.noticeId" :name="index">
          <template #title>
            <!-- 标签区分：通知标红，新闻标绿 -->
            <el-tag :type="item.type === 1 ? 'danger' : 'success'" size="small" style="margin-right: 10px;">
              {{ item.type === 1 ? '重要通知' : '志愿新闻' }}
            </el-tag>
            <span style="font-size: 15px; font-weight: 500;">{{ item.title }}</span>
            <!-- 靠右对齐的发布时间与发布人 -->
            <span style="color: #999; font-size: 13px; margin-left: auto; margin-right: 15px;">
              {{ item.publisherName }} 发布于 {{ item.createTime.split(' ')[0] }}
            </span>
          </template>

          <!-- 公告正文内容 -->
          <div class="notice-content">
            {{ item.content }}
          </div>
        </el-collapse-item>
      </el-collapse>

      <!-- 如果没有数据时显示的空状态 -->
      <el-empty v-else description="近期暂无公告信息" :image-size="80" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { Bell } from '@element-plus/icons-vue'; // 引入小铃铛图标
import request from '../utils/request';

const stats = ref({ volCount: 0, totalHours: 0, activeCount: 0 });
const noticeList = ref([]);
const activeNames = ref([0]); // 默认展开第一条公告

// 1. 获取基础统计数据
const fetchStats = async () => {
  try {
    const res = await request.get('/api/dashboard/base');
    stats.value = res.data;
  } catch (error) {
    console.error("加载统计数据失败", error);
  }
};

// 2. 获取公告列表 (首页展示最新发布的 5 条即可)
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
.home-container {
  padding: 10px;
}

/* 顶部卡片网格布局 */
.stat-cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

/* 卡片基础样式与悬浮动效 */
.card {
  padding: 25px 20px;
  border-radius: 10px;
  color: white;
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
  transition: transform 0.3s;
}
.card:hover {
  transform: translateY(-5px);
}

/* 各卡片渐变背景色 */
.card-1 { background: linear-gradient(135deg, #52c41a, #73d13d); }
.card-2 { background: linear-gradient(135deg, #1890ff, #40a9ff); }
.card-3 { background: linear-gradient(135deg, #722ed1, #9254de); }

h4 { margin: 0; font-size: 16px; font-weight: normal; opacity: 0.9; }
.num { font-size: 32px; font-weight: bold; margin-top: 15px; margin-bottom: 0; }

/* 公告正文样式：保留换行符并优化阅读体验 */
.notice-content {
  white-space: pre-wrap;
  color: #606266;
  line-height: 1.8;
  padding: 15px;
  background: #f8f9fa;
  border-radius: 6px;
  font-size: 14px;
}
</style>