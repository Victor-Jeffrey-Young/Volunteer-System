<template>
  <div class="home-container">
    <!-- 顶部：核心数据看板 -->
    <div class="stat-cards">
      <div class="card card-1">
        <div class="card-content">
          <h4>总志愿者人数</h4>
          <p class="num">{{ stats.volCount }} <span class="unit">人</span></p>
        </div>
      </div>
      <div class="card card-2">
        <div class="card-content">
          <h4>全社区累计志愿时长</h4>
          <p class="num">{{ stats.totalHours }} <span class="unit">小时</span></p>
        </div>
      </div>
      <div class="card card-3">
        <div class="card-content">
          <h4>招募/进行中的活动</h4>
          <p class="num">{{ stats.activeCount }} <span class="unit">个</span></p>
        </div>
      </div>
    </div>

    <!-- 底部：系统新闻与公告区 -->
    <el-card class="notice-card" shadow="hover">
      <template #header>
        <div class="notice-header">
          <el-icon color="#f56c6c"><Bell /></el-icon>
          <span>社区新闻与官方通知</span>
        </div>
      </template>

      <el-collapse v-model="activeNames" accordion v-if="noticeList.length > 0">
        <el-collapse-item v-for="(item, index) in noticeList" :key="item.noticeId" :name="index">
          <template #title>
            <div class="collapse-title-box">
              <el-tag :type="item.type === 1 ? 'danger' : 'success'" size="small" class="type-tag" effect="dark">
                {{ item.type === 1 ? '通知' : '新闻' }}
              </el-tag>
              <span class="title-text">{{ item.title }}</span>
              <span class="time-text">{{ item.createTime.split(' ')[0].substring(5) }}</span> <!-- 手机端只显示月-日 -->
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
  } catch (error) { console.error(error); }
};

const fetchNotices = async () => {
  try {
    const res = await request.get('/api/notice/page', { params: { current: 1, size: 5 } });
    noticeList.value = res.data.records;
  } catch (error) { console.error(error); }
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
.home-container { padding: 15px; }

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

.card-1 { background: linear-gradient(135deg, #ff9a9e, #fecfef); } /* 更柔和的粉红 */
.card-2 { background: linear-gradient(135deg, #84fab0, #8fd3f4); } /* 更清新的青蓝 */
.card-3 { background: linear-gradient(135deg, #a18cd1, #fbc2eb); } /* 更优雅的浅紫 */

.card-content { display: flex; flex-direction: column; }
h4 { margin: 0; font-size: 16px; font-weight: normal; opacity: 0.9; text-shadow: 0 1px 2px rgba(0,0,0,0.1); }
.num { font-size: 36px; font-weight: bold; margin-top: 15px; margin-bottom: 0; text-shadow: 0 2px 4px rgba(0,0,0,0.1); }
.unit { font-size: 14px; font-weight: normal; margin-left: 4px; }

/* 公告模块 */
.notice-card { margin-top: 20px; border-radius: 12px; border: none; box-shadow: 0 2px 12px rgba(0,0,0,0.05); }
.notice-header { font-weight: bold; font-size: 16px; display: flex; align-items: center; gap: 8px; }

.collapse-title-box { display: flex; align-items: center; width: 100%; padding-right: 10px; }
.type-tag { margin-right: 10px; flex-shrink: 0; border: none; }
.title-text { font-size: 15px; flex: 1; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.time-text { color: #999; font-size: 13px; margin-left: 10px; flex-shrink: 0; }

.notice-content { white-space: pre-wrap; color: #606266; line-height: 1.8; padding: 15px; background: #f8f9fa; border-radius: 8px; font-size: 14px; }
.notice-footer { text-align: right; font-size: 12px; color: #999; margin-top: 10px; }

/* ====================================================
   📱 移动端响应式适配 (小于 768px)
   ==================================================== */
@media screen and (max-width: 768px) {
  .home-container { padding: 10px; }

  /* 🚨 核心修复 1：强制卡片单列排布，且改为左右结构 */
  .stat-cards {
    display: flex;
    flex-direction: column; /* 垂直堆叠 */
    gap: 12px;
  }

  .card {
    padding: 15px 20px;
    border-radius: 10px;
  }

  /* 移动端时，标题在左，数字在右 */
  .card-content {
    flex-direction: row;
    justify-content: space-between;
    align-items: center;
  }

  h4 { font-size: 15px; font-weight: bold; }
  .num { font-size: 24px; margin-top: 0; } /* 移除 margin-top */
  .unit { font-size: 12px; }

  /* 🚨 核心修复 2：处理公告标题溢出 */
  .collapse-title-box {
    max-width: calc(100vw - 70px); /* 减去折叠箭头的宽度，防止撑破屏幕 */
  }
  .title-text { font-size: 14px; }
  .time-text { font-size: 12px; }
}
</style>