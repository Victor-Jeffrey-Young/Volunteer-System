<template>
  <div class="honor-container">
    <div class="page-header">
      <h2>🏆 志愿风采荣誉殿堂</h2>
      <p>感谢每一位为社区无私奉献的志愿者，你们是社区最闪亮的星！</p>
    </div>

    <el-row :gutter="20">
      <!-- 左侧：年度志愿之星 (按时长) -->
      <el-col :span="8">
        <el-card class="rank-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span class="title-gold">🌟 年度志愿之星 (时长榜)</span>
            </div>
          </template>
          <div v-for="(user, index) in hoursRank" :key="user.user_id" class="rank-item">
            <div class="rank-index">
              <img v-if="index === 0" src="https://cdn-icons-png.flaticon.com/512/2583/2583344.png" width="30" />
              <img v-else-if="index === 1" src="https://cdn-icons-png.flaticon.com/512/2583/2583319.png" width="30" />
              <img v-else-if="index === 2" src="https://cdn-icons-png.flaticon.com/512/2583/2583434.png" width="30" />
              <span v-else class="rank-num">{{ index + 1 }}</span>
            </div>
            <el-avatar :size="45" :src="user.avatar || getDefaultAvatar(user.username)" class="rank-avatar" />
            <div class="rank-info">
              <div class="name">{{ user.real_name }}</div>
              <div class="score">累计服务 {{ user.total_hours }} 小时</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 中间：月度活跃达人 (按积分) -->
      <el-col :span="8">
        <el-card class="rank-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span class="title-blue">🔥 月度活跃达人 (积分榜)</span>
            </div>
          </template>
          <div v-for="(user, index) in pointsRank" :key="user.user_id" class="rank-item">
            <div class="rank-index">
              <span :class="'badge badge-' + (index + 1)" v-if="index < 3">TOP {{ index + 1 }}</span>
              <span class="rank-num" v-else>{{ index + 1 }}</span>
            </div>
            <el-avatar :size="45" :src="user.avatar || getDefaultAvatar(user.username)" class="rank-avatar" />
            <div class="rank-info">
              <div class="name">{{ user.real_name }}</div>
              <div class="score">活跃积分 {{ user.points }} 分</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 右侧：数据可视化分析 (能力雷达图) -->
      <el-col :span="8">
        <el-card class="rank-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span>📊 社区志愿力量分布</span>
            </div>
          </template>
          <!-- 雷达图容器 -->
          <div ref="radarChartRef" style="height: 400px;"></div>
          <div class="quote-box">
            “赠人玫瑰，手有余香。”
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import request from '../utils/request';
import { getDefaultAvatar } from '../utils/levelRules';
import * as echarts from 'echarts';

const hoursRank = ref([]);
const pointsRank = ref([]);
const radarChartRef = ref(null);

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
  const chart = echarts.init(radarChartRef.value);
  chart.setOption({
    radar: {
      indicator: [
        { name: '社区服务', max: 100 },
        { name: '环境保护', max: 100 },
        { name: '教育助学', max: 100 },
        { name: '文明创建', max: 100 },
        { name: '活跃度', max: 100 },
        { name: '专业技能', max: 100 }
      ],
      radius: '65%'
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
});
</script>

<style scoped>
.honor-container { padding: 20px; background: linear-gradient(to bottom, #fdfbfb, #ebedee); min-height: 90vh; }
.page-header { text-align: center; margin-bottom: 30px; }
.page-header h2 { font-size: 28px; color: #333; margin-bottom: 10px; letter-spacing: 2px; }
.page-header p { color: #666; font-size: 14px; }

.rank-card { height: 550px; border-radius: 12px; border: none; }
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
</style>