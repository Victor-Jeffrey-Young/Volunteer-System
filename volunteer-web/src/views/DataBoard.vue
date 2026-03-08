<template>
  <div class="databoard-container">
    <div class="page-header">
      <h2>📊 社区志愿服务数据大屏</h2>
      <p>多维数据全景分析，助力社区智慧治理</p>
    </div>

    <!-- 第一排：饼图和柱状图 -->
    <el-row :gutter="20">
      <!-- 饼图：活动类型占比 -->
      <el-col :xs="24" :sm="24" :md="10" class="chart-col">
        <el-card shadow="hover" class="chart-card">
          <template #header><div class="card-title">🍩 活动类型分布占比</div></template>
          <div ref="pieChartRef" class="chart-box"></div>
        </el-card>
      </el-col>

      <!-- 柱状图：志愿标兵排行榜 -->
      <el-col :xs="24" :sm="24" :md="14" class="chart-col">
        <el-card shadow="hover" class="chart-card">
          <template #header><div class="card-title">🏆 志愿服务时长 TOP 5</div></template>
          <div ref="barChartRef" class="chart-box"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 第二排：折线图 -->
    <el-row :gutter="20">
      <el-col :xs="24" :sm="24" :md="24" class="chart-col">
        <el-card shadow="hover" class="chart-card">
          <template #header><div class="card-title">📈 近期活动开展趋势 (月度)</div></template>
          <div ref="lineChartRef" class="chart-box"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue';
import * as echarts from 'echarts';
import request from '../utils/request';

// DOM 引用
const pieChartRef = ref(null);
const barChartRef = ref(null);
const lineChartRef = ref(null);

// ECharts 实例
let pieChart = null;
let barChart = null;
let lineChart = null;

// 响应式状态判断
const isMobile = ref(window.innerWidth <= 768);

// 1. 渲染饼状图
const renderPieChart = async () => {
  try {
    const res = await request.get('/api/dashboard/typePie');
    if (!pieChartRef.value) return;
    pieChart = echarts.init(pieChartRef.value);
    pieChart.setOption({
      tooltip: { trigger: 'item', formatter: '{b}: {c}个 ({d}%)' },
      // 手机端图例放在底部，PC端靠左
      legend: {
        bottom: '0',
        left: 'center',
        itemWidth: 12,
        textStyle: { fontSize: isMobile.value ? 10 : 12 }
      },
      color:['#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de'],
      series:[{
        name: '活动类型',
        type: 'pie',
        // 手机端适当缩小环形图半径，留出空间给文字
        radius: isMobile.value ? ['35%', '60%'] : ['40%', '70%'],
        center:['50%', '45%'], // 整体稍微往上挪一点，给底部图例让位
        avoidLabelOverlap: true,
        itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
        label: { show: false, position: 'center' },
        emphasis: { label: { show: true, fontSize: isMobile.value ? '16' : '20', fontWeight: 'bold' } },
        data: res.data
      }]
    });
  } catch (e) { console.error("饼图加载失败", e); }
};

// 2. 渲染柱状图
const renderBarChart = async () => {
  try {
    const res = await request.get('/api/dashboard/rank');
    const names = res.data.map(item => item.name);
    const hours = res.data.map(item => item.value);

    if (!barChartRef.value) return;
    barChart = echarts.init(barChartRef.value);
    barChart.setOption({
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      // 手机端缩小边距，最大化图表区域
      grid: {
        left: isMobile.value ? '2%' : '3%',
        right: isMobile.value ? '5%' : '4%',
        bottom: '3%',
        top: '15%',
        containLabel: true
      },
      xAxis: {
        type: 'category',
        data: names,
        axisTick: { alignWithLabel: true },
        axisLabel: {
          // 手机端如果名字太长，倾斜显示防止重叠
          interval: 0,
          rotate: isMobile.value ? 30 : 0,
          fontSize: isMobile.value ? 10 : 12
        }
      },
      yAxis: { type: 'value', name: '小时' },
      series:[{
        name: '累计时长',
        type: 'bar',
        barWidth: isMobile.value ? '30%' : '40%', // 手机端柱子细一点
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1,[
            { offset: 0, color: '#83bff6' },
            { offset: 0.5, color: '#188df0' },
            { offset: 1, color: '#188df0' }
          ]),
          borderRadius: [4, 4, 0, 0]
        },
        data: hours
      }]
    });
  } catch (e) { console.error("柱状图加载失败", e); }
};

// 3. 渲染折线图
const renderLineChart = async () => {
  try {
    const res = await request.get('/api/dashboard/trend');
    const months = res.data.map(item => item.month);
    const counts = res.data.map(item => item.count);

    if (!lineChartRef.value) return;
    lineChart = echarts.init(lineChartRef.value);
    lineChart.setOption({
      tooltip: { trigger: 'axis' },
      grid: {
        left: isMobile.value ? '2%' : '3%',
        right: isMobile.value ? '5%' : '4%',
        bottom: '3%',
        top: '15%',
        containLabel: true
      },
      xAxis: {
        type: 'category',
        boundaryGap: false,
        data: months,
        axisLabel: { fontSize: isMobile.value ? 10 : 12 }
      },
      yAxis: { type: 'value', name: '场次' },
      series:[{
        name: '活动数量',
        type: 'line',
        smooth: true,
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1,[
            { offset: 0, color: 'rgba(84,112,198,0.5)' },
            { offset: 1, color: 'rgba(84,112,198,0.1)' }
          ])
        },
        itemStyle: { color: '#5470c6' },
        data: counts
      }]
    });
  } catch (e) { console.error("折线图加载失败", e); }
};

// 窗口尺寸变化监听
const handleResize = () => {
  isMobile.value = window.innerWidth <= 768;
  // 调用 ECharts 自带的 resize 方法重新计算图表尺寸
  if (pieChart) pieChart.resize();
  if (barChart) barChart.resize();
  if (lineChart) lineChart.resize();
};

onMounted(async () => {
  // 保证 DOM 渲染完成后再初始化图表
  await nextTick();
  renderPieChart();
  renderBarChart();
  renderLineChart();

  window.addEventListener('resize', handleResize);
});

onUnmounted(() => {
  window.removeEventListener('resize', handleResize);
  // 销毁实例释放内存
  if (pieChart) pieChart.dispose();
  if (barChart) barChart.dispose();
  if (lineChart) lineChart.dispose();
});
</script>

<style scoped>
/* ====================================================
   🖥️ 默认样式 (PC 端)
   ==================================================== */
.databoard-container {
  padding: 10px;
}

.page-header {
  margin-bottom: 20px;
}
.page-header h2 {
  margin: 0 0 5px 0;
  color: #303133;
  font-size: 24px;
}
.page-header p {
  margin: 0;
  color: #909399;
  font-size: 14px;
}

.chart-card {
  border-radius: 12px;
  border: none;
  box-shadow: 0 2px 12px rgba(0,0,0,0.05);
}

.card-title {
  font-weight: bold;
  font-size: 16px;
  color: #333;
}

.chart-col {
  margin-bottom: 20px;
}

/* PC 端图表高度 */
.chart-box {
  height: 350px;
  width: 100%;
}

/* ====================================================
   📱 移动端响应式适配 (小于 768px)
   ==================================================== */
@media screen and (max-width: 768px) {
  .databoard-container {
    padding: 5px;
  }

  .page-header h2 {
    font-size: 20px;
  }

  .chart-col {
    margin-bottom: 15px;
  }

  /* 减小卡片的内边距，留给图表更多空间 */
  .chart-card :deep(.el-card__body) {
    padding: 10px;
  }
  .chart-card :deep(.el-card__header) {
    padding: 12px 15px;
  }

  .card-title {
    font-size: 14px;
  }

  /* 移动端图表高度稍微压扁一点，防止占据整个屏幕需要频繁滑动 */
  .chart-box {
    height: 280px;
  }
}
</style>