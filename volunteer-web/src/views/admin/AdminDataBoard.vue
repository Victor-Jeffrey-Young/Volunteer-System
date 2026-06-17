<template>
  <div class="databoard-container">
    <div class="page-header">
      <h2>📊 社区志愿服务数据大屏</h2>
      <p>多维数据全景分析，助力社区智慧治理</p>
    </div>

    <!--
      使用 Element Plus 栅格系统
      - PC 端 (md): 饼图占 10 份，柱状图占 14 份
      - 移动端 (xs, sm): 强制占满 24 份，实现上下堆叠布局
    -->
    <el-row :gutter="20">
      <!-- 饼图：活动类型占比 -->
      <el-col :xs="24" :sm="24" :md="10" class="chart-col">
        <el-card shadow="hover" class="chart-card">
          <template #header><div class="card-title">🍩 活动类型分布占比</div></template>
          <!-- 图表挂载点，必须设置宽高 -->
          <div ref="pieChartRef" class="chart-box"></div>
        </el-card>
      </el-col>

      <!-- 柱状/条形图：志愿标兵排行榜 -->
      <el-col :xs="24" :sm="24" :md="14" class="chart-col">
        <el-card shadow="hover" class="chart-card">
          <template #header><div class="card-title">🏆 志愿服务时长 TOP 10</div></template>
          <div ref="barChartRef" class="chart-box"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区：折线图 -->
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
/**
 * 社区数据看板模块
 * 核心功能：通过 ECharts 渲染后端聚合数据，支持跨终端的响应式自适应布局。
 */
import { ref, onMounted, onUnmounted, nextTick } from 'vue';
import * as echarts from 'echarts';
import request from '../../utils/request';

// --- DOM 元素引用 ---
const pieChartRef = ref(null);
const barChartRef = ref(null);
const lineChartRef = ref(null);

// --- ECharts 实例对象 (用于后续的 resize 和销毁) ---
let pieChart = null;
let barChart = null;
let lineChart = null;

// --- 响应式状态判断 ---
// 初始化判断当前设备是否为移动端 (屏幕宽度 <= 768px)
const isMobile = ref(window.innerWidth <= 768);

/**
 * 渲染活动类型饼图
 * 业务逻辑：拉取各类型活动的总数，绘制空心环形图
 */
const renderPieChart = async () => {
  try {
    const res = await request.get('/api/dashboard/typePie');
    if (!pieChartRef.value) return;

    pieChart = echarts.init(pieChartRef.value);
    pieChart.setOption({
      tooltip: { trigger: 'item', formatter: '{b}: {c}个 ({d}%)' },
      // 响应式优化：移动端图例放在底部居中，PC 端靠左
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
        // 响应式优化：移动端缩小外半径防止文字被截断
        radius: isMobile.value ? ['35%', '60%'] :['40%', '70%'],
        center: ['50%', '45%'], // 整体中心点上移，给底部的图例让出空间
        avoidLabelOverlap: true,
        itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
        label: { show: false, position: 'center' },
        emphasis: {
          label: { show: true, fontSize: isMobile.value ? '16' : '20', fontWeight: 'bold' }
        },
        data: res.data
      }]
    });
  } catch (e) {
    console.error("饼图加载失败", e);
  }
};

/**
 * 渲染志愿服务时长排行榜
 * 业务逻辑：获取 Top 10 用户时长，绘制横向条形图以解决长姓名重叠问题
 */
const renderBarChart = async () => {
  try {
    const res = await request.get('/api/dashboard/rank');
    // 横向条形图的数据排序是从下往上画的，所以需要将后端降序的数据 reverse 反转，保证第1名在最顶端
    const names = res.data.map(item => item.name).reverse();
    const hours = res.data.map(item => item.value).reverse();

    if (!barChartRef.value) return;

    barChart = echarts.init(barChartRef.value);
    barChart.setOption({
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      // 动态间距：预留左侧空间显示人名
      grid: {
        left: '2%', right: '6%', bottom: '3%', top: '5%', containLabel: true
      },
      xAxis: {
        type: 'value',
        name: '小时',
        splitLine: { show: false } // 保持图表清爽，隐藏背景网格线
      },
      yAxis: {
        type: 'category',
        data: names,
        axisTick: { show: false }, // 隐藏 Y 轴刻度线
        axisLabel: {
          fontWeight: 'bold',
          color: '#555'
        }
      },
      series:[{
        name: '累计时长',
        type: 'bar',
        barWidth: '50%', // 柱子宽度比例
        itemStyle: {
          // 渐变色配置：自左向右渐变
          color: new echarts.graphic.LinearGradient(1, 0, 0, 0,[
            { offset: 0, color: '#188df0' },
            { offset: 1, color: '#83bff6' }
          ]),
          borderRadius: [0, 4, 4, 0] // 仅右侧圆角
        },
        // 在柱子末尾直接显示数值，增强可读性
        label: {
          show: true,
          position: 'right',
          color: '#188df0',
          fontWeight: 'bold'
        },
        data: hours
      }]
    });
  } catch (e) {
    console.error("条形图加载失败", e);
  }
};

/**
 * 3. 渲染近期活动开展趋势图
 * 业务逻辑：获取近半年每月的活动发布数量，绘制平滑的面积折线图
 */
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
        boundaryGap: false, // 让折线从 Y 轴起点开始，无两端留白
        data: months,
        axisLabel: { fontSize: isMobile.value ? 10 : 12 }
      },
      yAxis: {
        type: 'value',
        name: '场次'
      },
      series:[{
        name: '活动数量',
        type: 'line',
        smooth: true, // 开启平滑曲线
        areaStyle: {
          // 曲线下方填充半透明渐变色，提升视觉冲击力
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1,[
            { offset: 0, color: 'rgba(84,112,198,0.5)' },
            { offset: 1, color: 'rgba(84,112,198,0.1)' }
          ])
        },
        itemStyle: { color: '#5470c6' },
        data: counts
      }]
    });
  } catch (e) {
    console.error("折线图加载失败", e);
  }
};

/**
 * 窗口尺寸变化监听函数
 * 作用：当用户改变浏览器窗口大小，或手机横竖屏切换时，触发图表重绘
 */
const handleResize = () => {
  isMobile.value = window.innerWidth <= 768;
  // 必须调用 ECharts 实例的 resize() 方法才能实现自适应
  if (pieChart) pieChart.resize();
  if (barChart) barChart.resize();
  if (lineChart) lineChart.resize();
};

// --- 生命周期管理 ---

onMounted(async () => {
  // 使用 nextTick 确保包含了 ECharts 容器的 DOM 已完全渲染完毕
  await nextTick();

  // 异步并发或依次渲染图表
  renderPieChart();
  renderBarChart();
  renderLineChart();

  // 挂载窗口缩放监听器
  window.addEventListener('resize', handleResize);
});

onUnmounted(() => {
  // 组件卸载时，务必移除全局监听器，防止内存泄漏
  window.removeEventListener('resize', handleResize);

  // 销毁 ECharts 实例，释放 WebGL/Canvas 资源
  if (pieChart) pieChart.dispose();
  if (barChart) barChart.dispose();
  if (lineChart) lineChart.dispose();
});
</script>

<style scoped>
/* ====================================================
   默认样式 (PC 端)
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

/* PC 端图表固定高度 */
.chart-box {
  height: 350px;
  width: 100%;
}

/* ====================================================
   移动端响应式适配 (屏幕宽度 <= 768px 时生效)
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

  /* 减小卡片的内边距，将更多的屏幕空间留给数据图表 */
  .chart-card :deep(.el-card__body) {
    padding: 10px;
  }
  .chart-card :deep(.el-card__header) {
    padding: 12px 15px;
  }

  .card-title {
    font-size: 14px;
  }

  /* 移动端图表高度稍微压扁一点，防止图表占据整个屏幕，减少用户的滑动疲劳 */
  .chart-box {
    height: 280px;
  }
}
</style>