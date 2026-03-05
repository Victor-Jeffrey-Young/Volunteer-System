<template>
  <div style="padding: 20px; background-color: #f0f2f5; min-height: 100vh;">
    <h2 style="margin-top: 0; color: #333;">📊 社区志愿服务数据大屏</h2>

    <!-- 第一排：饼图和柱状图 -->
    <el-row :gutter="20" style="margin-top: 20px;">
      <!-- 饼图：活动类型占比 -->
      <el-col :span="10">
        <el-card shadow="hover" style="border-radius: 8px;">
          <template #header><div class="card-title">活动类型分布占比</div></template>
          <div ref="pieChartRef" style="height: 350px; width: 100%;"></div>
        </el-card>
      </el-col>

      <!-- 柱状图：志愿标兵排行榜 -->
      <el-col :span="14">
        <el-card shadow="hover" style="border-radius: 8px;">
          <template #header><div class="card-title">🏆 志愿服务时长 TOP 5</div></template>
          <div ref="barChartRef" style="height: 350px; width: 100%;"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 第二排：折线图 -->
    <el-row style="margin-top: 20px;">
      <el-col :span="24">
        <el-card shadow="hover" style="border-radius: 8px;">
          <template #header><div class="card-title">📈 近期活动开展趋势 (月度)</div></template>
          <div ref="lineChartRef" style="height: 350px; width: 100%;"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue';
import * as echarts from 'echarts';
import request from '../utils/request';

// 获取 DOM 引用
const pieChartRef = ref(null);
const barChartRef = ref(null);
const lineChartRef = ref(null);

// ECharts 实例
let pieChart, barChart, lineChart;

// 1. 渲染饼状图
const renderPieChart = async () => {
  const res = await request.get('/api/dashboard/typePie');
  pieChart = echarts.init(pieChartRef.value);
  pieChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c}个 ({d}%)' },
    legend: { bottom: '0', left: 'center' },
    color:['#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de'],
    series:[{
      name: '活动类型',
      type: 'pie',
      radius: ['40%', '70%'], // 环形图设计
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 10, borderColor: '#fff', borderWidth: 2 },
      label: { show: false, position: 'center' },
      emphasis: { label: { show: true, fontSize: '20', fontWeight: 'bold' } },
      data: res.data // 直接使用后端返回的 {name, value} 数组
    }]
  });
};

// 2. 渲染柱状图
const renderBarChart = async () => {
  const res = await request.get('/api/dashboard/rank');
  const names = res.data.map(item => item.name);
  const hours = res.data.map(item => item.value);

  barChart = echarts.init(barChartRef.value);
  barChart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: names, axisTick: { alignWithLabel: true } },
    yAxis: { type: 'value', name: '小时' },
    series:[{
      name: '累计时长',
      type: 'bar',
      barWidth: '40%',
      itemStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1,[
          { offset: 0, color: '#83bff6' },
          { offset: 0.5, color: '#188df0' },
          { offset: 1, color: '#188df0' }
        ]),
        borderRadius: [4, 4, 0, 0] // 柱子顶部圆角
      },
      data: hours
    }]
  });
};

// 3. 渲染折线图
const renderLineChart = async () => {
  const res = await request.get('/api/dashboard/trend');
  const months = res.data.map(item => item.month);
  const counts = res.data.map(item => item.count);

  lineChart = echarts.init(lineChartRef.value);
  lineChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', boundaryGap: false, data: months },
    yAxis: { type: 'value', name: '活动场次' },
    series:[{
      name: '活动数量',
      type: 'line',
      smooth: true, // 平滑曲线
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
};

// 挂载时渲染图表，并监听窗口大小改变使图表自适应
onMounted(() => {
  renderPieChart();
  renderBarChart();
  renderLineChart();

  window.addEventListener('resize', () => {
    pieChart?.resize();
    barChart?.resize();
    lineChart?.resize();
  });
});

// 卸载时销毁监听器，防止内存泄漏
onUnmounted(() => {
  window.removeEventListener('resize', null);
});
</script>

<style scoped>
.card-title {
  font-weight: bold;
  font-size: 16px;
  color: #333;
}
</style>