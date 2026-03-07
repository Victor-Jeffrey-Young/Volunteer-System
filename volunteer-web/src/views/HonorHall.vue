<template>
  <div class="honor-container">
    <div class="page-header">
      <h2>🏆 志愿风采荣誉殿堂</h2>
      <p>感谢每一位为社区无私奉献的志愿者，你们是社区最闪亮的星！</p>
    </div>

    <!-- gutter 调整为响应式，手机端间距小一点 -->
    <el-row :gutter="isMobile ? 10 : 20">
      <!-- 左侧：年度志愿之星 (按时长) -->
      <el-col :xs="24" :sm="24" :md="8" class="col-margin">
        <el-card class="rank-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span class="title-gold">🌟 年度志愿之星 (时长榜)</span>
            </div>
          </template>
          <div v-for="(user, index) in hoursRank" :key="user.user_id" class="rank-item">
            <div class="rank-index">
              <img v-if="index === 0" src="https://cdn-icons-png.flaticon.com/512/2583/2583344.png" width="28" />
              <img v-else-if="index === 1" src="https://cdn-icons-png.flaticon.com/512/2583/2583319.png" width="28" />
              <img v-else-if="index === 2" src="https://cdn-icons-png.flaticon.com/512/2583/2583434.png" width="28" />
              <span v-else class="rank-num">{{ index + 1 }}</span>
            </div>
            <el-avatar :size="isMobile ? 35 : 45" :src="user.avatar || getDefaultAvatar(user.username)" class="rank-avatar" />
            <div class="rank-info">
              <div class="name">{{ user.real_name }}</div>
              <div class="score">
                服务 <span style="color:#e6a23c; font-weight: bold;">{{ user.total_hours || 0 }}</span> 小时
              </div>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 中间：月度活跃达人 (按积分) -->
      <el-col :xs="24" :sm="24" :md="8" class="col-margin">
        <el-card class="rank-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span class="title-blue">🔥 月度活跃达人 (积分榜)</span>
            </div>
          </template>
          <div v-for="(user, index) in pointsRank" :key="user.user_id" class="rank-item">
            <div class="rank-index">
              <el-icon v-if="index === 0" color="#FFD700" size="26"><Trophy /></el-icon>
              <el-icon v-else-if="index === 1" color="#C0C0C0" size="26"><Trophy /></el-icon>
              <el-icon v-else-if="index === 2" color="#CD7F32" size="26"><Trophy /></el-icon>
              <span v-else class="rank-num">{{ index + 1 }}</span>
            </div>
            <el-avatar :size="isMobile ? 35 : 45" :src="user.avatar || getDefaultAvatar(user.username)" class="rank-avatar" />
            <div class="rank-info">
              <div class="name">{{ user.real_name }}</div>
              <div class="score">
                积分 <span class="points-highlight">{{ user.points || 0 }}</span> 分
              </div>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 右侧：数据可视化分析 (能力雷达图) -->
      <el-col :xs="24" :sm="24" :md="8" class="col-margin">
        <el-card class="rank-card radar-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span>📊 社区志愿力量分布</span>
            </div>
          </template>
          <div ref="radarChartRef" class="radar-container"></div>
          <div class="quote-box">“赠人玫瑰，手有余香。”</div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'; // 增加 onUnmounted
import request from '../utils/request';
import { getDefaultAvatar } from '../utils/levelRules';
import * as echarts from 'echarts';
import {Trophy} from '@element-plus/icons-vue';

const hoursRank = ref([]);
const pointsRank = ref([]);
const radarChartRef = ref(null);
let myChart = null; // 定义图表实例

// 响应式判断
const isMobile = ref(window.innerWidth <= 768);
const handleResize = () => {
  isMobile.value = window.innerWidth <= 768;
  if (myChart) myChart.resize(); // 🚨 核心：重置图表大小
};

// 获取排行榜数据
const fetchRanks = async () => {
  // 并发请求两个榜单
  const [resHours, resPoints] = await Promise.all([
    request.get('/api/dashboard/volunteer/rank', { params: { type: 'hours' } }),
    request.get('/api/dashboard/volunteer/rank', { params: { type: 'points' } })
  ]);

  hoursRank.value = resHours.data;
  pointsRank.value = resPoints.data;
};

// 渲染雷达图 (模拟数据，增加视觉效果)
const renderRadar = () => {
  if (!radarChartRef.value) return;
  myChart = echarts.init(radarChartRef.value);
  myChart.setOption({
    radar: {
      indicator: [
        { name: '社区服务', max: 100 },
        { name: '环境保护', max: 100 },
        { name: '教育助学', max: 100 },
        { name: '文明创建', max: 100 },
        { name: '活跃度', max: 100 },
        { name: '专业技能', max: 100 }
      ],
      // 🚨 移动端缩小雷达图半径，防止文字溢出
      radius: isMobile.value ? '50%' : '65%',
      name: {
        textStyle: {
          fontSize: isMobile.value ? 10 : 12
        }
      }
    },
    series: [{
      name: '能力分布',
      type: 'radar',
      data: [
        {
          value: [85, 90, 70, 80, 95, 60],
          name: '平均水平',
          itemStyle: { color: '#409EFF' },
          areaStyle: { opacity: 0.3 }
        },
        {
          value: [95, 80, 95, 70, 85, 90],
          name: '标兵水平',
          itemStyle: { color: '#E6A23C' },
          areaStyle: { opacity: 0.3 }
        }
      ]
    }]
  });
};

onMounted(async () => {
  await fetchRanks();
  renderRadar();
  window.addEventListener('resize', handleResize);
});

onUnmounted(() => {
  window.removeEventListener('resize', handleResize);
  if (myChart) myChart.dispose(); // 销毁实例
});
</script>

<style scoped>
.honor-container { padding: 20px; background: linear-gradient(to bottom, #fdfbfb, #ebedee); min-height: 90vh; }
.page-header { text-align: center; margin-bottom: 30px; }
.page-header h2 { font-size: 28px; color: #333; margin-bottom: 10px; }

/* 关键：PC端固定高度，手机端自适应 */
.rank-card {
  height: 550px;
  border-radius: 12px;
  border: none;
  transition: all 0.3s;
}

.radar-container { height: 400px; width: 100%; }

.card-header { font-weight: bold; font-size: 16px; text-align: center; }
.title-gold { color: #d4af37; } /* 金色 */
.title-blue { color: #409eff; } /* 蓝色 */

.rank-item { display: flex; align-items: center; padding: 12px 0; border-bottom: 1px dashed #eee; transition: all 0.3s; }
.rank-item:hover { background: #f9f9f9; transform: translateX(5px); }
.rank-item:last-child { border-bottom: none; }

.rank-index { width: 40px; text-align: center; font-weight: bold; }
.rank-num { color: #999; font-size: 16px; font-style: italic; }
.rank-avatar { border: 2px solid #fff; box-shadow: 0 2px 5px rgba(0,0,0,0.1); margin: 0 10px; }

.rank-info .name { font-size: 15px; font-weight: bold; color: #333; }
.rank-info .score { font-size: 12px; color: #666; margin-top: 3px; }

.badge { padding: 2px 6px; border-radius: 4px; color: white; font-size: 10px; }
.badge-1 { background: #f56c6c; }
.badge-2 { background: #e6a23c; }
.badge-3 { background: #409eff; }

.quote-box { margin-top: 20px; text-align: center; font-style: italic; color: #999; font-family: "Georgia", serif; }

.rank-item {
  display: flex;
  align-items: center;
  padding: 12px 5px; /* 稍微减少左右内边距 */
  border-bottom: 1px dashed #eee;
  transition: all 0.3s;
}
.rank-item:hover {
  background: #f9f9f9;
  transform: translateX(5px);
}
.rank-item:last-child { border-bottom: none; }

/* 排名容器：这是对齐的关键 */
.rank-index {
  width: 50px; /* 固定宽度 */
  flex-shrink: 0; /* 防止被挤压 */
  display: flex;
  justify-content: center;
  align-items: center;
  font-weight: bold;
}

/* 排名数字样式 */
.rank-num {
  color: #909399;
  font-size: 18px;
  font-style: italic;
  font-family: "Georgia", serif;
}

/* 头像样式 */
.rank-avatar {
  border: 2px solid #fff;
  box-shadow: 0 2px 5px rgba(0,0,0,0.1);
  margin: 0 12px;
}

/* 名字和分数 */
.rank-info .name {
  font-size: 15px;
  font-weight: bold;
  color: #333;
}
.rank-info .score {
  font-size: 12px;
  color: #666;
  margin-top: 3px;
}
.points-highlight {
  color: #e6a23c;
  font-weight: bold;
  font-size: 14px;
}

/* ====================================================
   📱 移动端响应式精修
   ==================================================== */
@media screen and (max-width: 768px) {
  .honor-container { padding: 10px; }

  .page-header { margin-bottom: 15px; }
  .page-header h2 { font-size: 20px; }
  .page-header p { font-size: 12px; }

  /* 1. 手机端卡片高度设为 auto，防止内容溢出或留白过多 */
  .rank-card {
    height: auto !important;
    margin-bottom: 15px;
    padding-bottom: 10px;
  }

  .col-margin {
    margin-bottom: 15px;
  }

  /* 2. 缩小排行榜间距 */
  .rank-item {
    padding: 8px 0;
  }

  .rank-index {
    width: 35px;
  }

  .rank-info .name {
    font-size: 14px;
  }

  /* 3. 雷达图高度调小，防止手机上一屏显示不完 */
  .radar-container {
    height: 300px;
  }

  .quote-box {
    margin-top: 10px;
    font-size: 12px;
  }
}

</style>