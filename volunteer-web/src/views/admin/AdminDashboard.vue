<template>
  <div class="dashboard-compact">
    <!-- 系统广播条 -->
    <div class="info-ticker bg-slate-900 text-white shadow-lg rounded-xl px-6 py-2.5 mb-4 flex items-center justify-between">
      <div class="flex items-center gap-3">
        <Megaphone class="w-4 h-4 text-orange-500 animate-pulse" />
        <span class="text-xs font-bold tracking-wide">系统广播：欢迎回来，管理员！当前系统有 {{ statCards[2].value }} 项挂起申请待处理。</span>
      </div>
      <span class="text-[10px] opacity-60 font-mono">{{ new Date().toLocaleDateString() }} {{ new Date().toLocaleTimeString() }}</span>
    </div>

    <!-- 核心指标 (横向排列) -->
    <div class="stats-row-grid">
      <div v-for="stat in statCards" :key="stat.title" class="compact-stat-card shadow-sm">
        <div class="stat-icon-mini" :style="{ backgroundColor: stat.bgColor }">
          <component :is="stat.icon" class="w-5 h-5" :style="{ color: stat.color }" />
        </div>
        <div class="stat-content">
          <p class="stat-label-mini">{{ stat.title }}</p>
          <div class="flex items-baseline gap-1">
            <span class="stat-value-mini">{{ stat.value }}</span>
            <span class="stat-unit-mini">{{ stat.unit }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 图表与审批 (并排) -->
    <div class="main-content-grid">
      
      <!-- 核心成就数据仪表 -->
      <div class="chart-column bg-slate-900 shadow-xl rounded-2xl border border-slate-800 relative ">
        
        <!-- 背景装饰光晕 -->
        <div class="absolute -top-[100px] -right-[100px] w-64 h-64 bg-orange-500/20 rounded-full blur-3xl pointer-events-none"></div>
        <div class="absolute bottom-0 left-0 w-full h-1/2 bg-gradient-to-t from-orange-600/10 to-transparent pointer-events-none"></div>

        <div class="section-header-compact border-b border-slate-800/50">
          <h3 class="section-title-mini flex items-center gap-2 !text-white drop-shadow-md">
            <Globe2 class="w-4 h-4 text-orange-400" />
            社区志愿成就仪表盘
          </h3>
          <span class="text-[10px] text-emerald-400 bg-emerald-400/10 px-2 py-1 rounded-md border border-emerald-400/20 font-bold shadow-[0_0_10px_rgba(52,211,153,0.2)] animate-pulse">实时运行中</span>
        </div>

        <div class="chart-area-compact flex flex-col justify-center h-full z-10 !pt-4 !pb-2">
          
          <div class="mb-2 mt-2">
            <p class="text-xs text-slate-400 font-bold mb-2 ml-1 flex items-center gap-1"><Activity class="w-3 h-3 text-emerald-400"/> 累计向社会贡献服务时长</p>
            <div class="flex items-baseline gap-2">
              <!-- 大数字发光效果 -->
              <span class="text-6xl font-black text-transparent bg-clip-text bg-gradient-to-br from-white to-slate-400 tracking-tighter filter drop-shadow-[0_0_15px_rgba(255,255,255,0.2)]" style="font-family: monospace;">
                {{ totalHours }}
              </span>
              <span class="text-lg text-orange-400 font-bold drop-shadow-[0_0_8px_rgba(249,115,22,0.5)]">小时</span>
            </div>
            <p class="text-[11px] text-slate-500 mt-2 ml-1">里程碑：每一小时的奉献，都在让世界变得更美好。</p>
          </div>

          <!-- 底部微型趋势图 -->
          <div class="mt-auto h-12 w-full flex items-end gap-1.5 opacity-50 hover:opacity-100 transition-opacity">
            <template v-if="trendData.length > 0">
              <div v-for="item in trendData" :key="item.month" class="flex-1 bg-slate-800/50 rounded-t-sm relative group cursor-pointer h-full flex flex-col justify-end">
                <div class="w-full bg-gradient-to-t from-orange-600 to-orange-400 rounded-t-sm transition-all" :style="{ height: (item.count / maxTrendValue * 100) + '%' }"></div>
                <!-- 气泡 -->
                <div class="absolute -top-7 left-1/2 -translate-x-1/2 bg-black text-white text-[10px] px-2 py-0.5 rounded opacity-0 group-hover:opacity-100 transition-opacity pointer-events-none whitespace-nowrap z-20 border border-slate-700 shadow-xl">
                  {{ item.month }}: {{ item.count }}场
                </div>
              </div>
            </template>
          </div>

        </div>
      </div>

      <!-- 实时审批流 -->
      <div class="todo-column bg-white shadow-sm rounded-2xl border border-slate-200">
        <div class="section-header-compact bg-slate-50/50">
          <h3 class="section-title-mini flex items-center gap-2 text-rose-600">
            <ShieldCheck class="w-4 h-4" />
            快速审批
          </h3>
          <button @click="router.push('/admin/registrations')" class="text-[10px] font-bold text-orange-600 uppercase hover:underline">
            进入大厅
          </button>
        </div>
        
        <div class="todo-scroll-area">
          <div v-if="todoList.length === 0" class="empty-tip-mini">
            ☕ 暂无待审任务
          </div>
          <div v-for="item in todoList" :key="item.regId" class="todo-item-compact">
            <div class="todo-item-top">
              <el-avatar :size="24" :src="getFullAvatar(item.avatar)" />
              <span class="todo-user-name">{{ item.realName }}</span>
              <span class="todo-time-mini">{{ item.applyTime?.substring(5, 10) }}</span>
            </div>
            <div class="todo-item-body">
              <p class="todo-act-title">{{ item.activityTitle }}</p>
              <div class="todo-actions-mini">
                <button @click="quickAudit(item, 1)" class="btn-mini btn-pass">通过</button>
                <button @click="quickAudit(item, 2)" class="btn-mini btn-reject">拒绝</button>
              </div>
            </div>
          </div>
        </div>
      </div>

    </div>

    <!-- 数据可视化 ECharts 面板 -->
    <div class="echarts-row-grid mt-4 grid grid-cols-1 lg:grid-cols-2 gap-4">
      <div class="bg-white rounded-2xl p-4 shadow-sm border border-slate-200 hover:shadow-md transition-shadow">
        <h3 class="text-sm font-bold text-slate-800 mb-2 flex items-center gap-2">
          微心愿分类占比
        </h3>
        <div ref="pieChartRef" class="w-full h-64"></div>
      </div>
      <div class="bg-white rounded-2xl p-4 shadow-sm border border-slate-200 hover:shadow-md transition-shadow">
        <h3 class="text-sm font-bold text-slate-800 mb-2 flex items-center gap-2">
          居民点赞光荣榜 TOP 10
        </h3>
        <div ref="barChartRef" class="w-full h-64"></div>
      </div>
    </div>

  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { 
  Users, 
  CalendarCheck, 
  TrendingUp, 
  ShieldCheck,
  Megaphone,
  Globe2,
  Activity
} from 'lucide-vue-next';
import { ElMessage } from 'element-plus';
import request from '../../utils/request';
import { getFullAvatar } from '../../utils/file';
import * as echarts from 'echarts';

const router = useRouter();
const timeRange = ref('7d');
const todoList = ref([]);
const trendData = ref([]);
const maxTrendValue = ref(1);
const totalHours = ref(0);

// ECharts 引用与实例
const pieChartRef = ref(null);
const barChartRef = ref(null);
let pieChartInstance = null;
let barChartInstance = null;

const statCards = ref([
  { title: '注册总量', value: '0', unit: '人', icon: Users, color: '#f97316', bgColor: '#fff7ed' },
  { title: '正在招募', value: '0', unit: '项', icon: CalendarCheck, color: '#3b82f6', bgColor: '#eff6ff' },
  { title: '待审申请', value: '0', unit: '单', icon: ShieldCheck, color: '#ef4444', bgColor: '#fef2f2' },
]);

const fetchDashboardData = async () => {
  try {
    const res = await request.get('/api/dashboard/base');
    if (res.data) {
      statCards.value[0].value = res.data.volCount || 0;
      statCards.value[1].value = res.data.activeCount || 0;
      totalHours.value = res.data.totalHours || 0;
    }
    const todoRes = await request.get('/api/reg/admin/page', { params: { current: 1, size: 10, status: 0 } });
    todoList.value = todoRes.data?.records || [];
    // 使用分页接口返回的 total 总数，后端 /dashboard/base 接口并没提供 pendingRegCount
    statCards.value[2].value = todoRes.data?.total || 0;
    
    // 获取真实趋势数据
    const trendRes = await request.get('/api/dashboard/trend');
    trendData.value = trendRes.data || [];
    if (trendData.value.length > 0) {
      maxTrendValue.value = Math.max(...trendData.value.map(item => item.count)) || 1;
    }
  } catch (error) { console.error(error); }
};

const quickAudit = async (record, status) => {
  try {
    await request.put(`/api/reg/admin/audit?regId=${record.regId}&status=${status}`);
    ElMessage.success('审核已提交');
    fetchDashboardData();
  } catch (e) {}
};

const renderPieChart = async () => {
  try {
    const res = await request.get('/api/dashboard/wishPie');
    if (!pieChartRef.value) return;
    pieChartInstance = echarts.init(pieChartRef.value);
    pieChartInstance.setOption({
      tooltip: { trigger: 'item', formatter: '{b}: {c}个 ({d}%)' },
      color: ['#f59e0b', '#3b82f6', '#10b981', '#f43f5e', '#8b5cf6', '#6366f1'],
      legend: { bottom: 0, textStyle: { fontSize: 10 } },
      series:[{
        name: '微心愿',
        type: 'pie',
        radius: ['45%', '70%'],
        center: ['50%', '40%'],
        avoidLabelOverlap: false,
        itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 2 },
        label: { show: false, position: 'center' },
        emphasis: { label: { show: true, fontSize: 16, fontWeight: 'bold' } },
        data: res.data
      }]
    });
  } catch (e) {}
};

const renderBarChart = async () => {
  try {
    const res = await request.get('/api/dashboard/likesRank');
    if (!barChartRef.value) return;
    barChartInstance = echarts.init(barChartRef.value);
    
    const names = res.data.map(item => item.name).reverse();
    const likes = res.data.map(item => item.value).reverse();

    barChartInstance.setOption({
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      grid: { left: '2%', right: '8%', bottom: '0', top: '5%', containLabel: true },
      xAxis: { type: 'value', show: false },
      yAxis: { type: 'category', data: names, axisTick: { show: false }, axisLine: { show: false }, axisLabel: { fontWeight: 'bold', color: '#64748b' } },
      series: [{
        name: '点赞数',
        type: 'bar',
        barWidth: '60%',
        itemStyle: {
          color: new echarts.graphic.LinearGradient(1, 0, 0, 0,[
            { offset: 0, color: '#f97316' },
            { offset: 1, color: '#fbd38d' }
          ]),
          borderRadius: [0, 4, 4, 0]
        },
        label: { show: true, position: 'right', color: '#f97316', fontWeight: 'bold' },
        data: likes
      }]
    });
  } catch (e) {}
};

const handleResize = () => {
  if (pieChartInstance) pieChartInstance.resize();
  if (barChartInstance) barChartInstance.resize();
};

onMounted(() => {
  fetchDashboardData();
  setTimeout(() => {
    renderPieChart();
    renderBarChart();
  }, 300);
  window.addEventListener('resize', handleResize);
});
</script>

<style scoped>
.dashboard-compact {
  max-width: 1400px;
  margin: 0 auto;
}

/* 第一行：指标卡片 */
.stats-row-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 16px;
}

.compact-stat-card {
  background: white;
  padding: 12px 16px;
  border-radius: 16px;
  border: 1px solid #e2e8f0;
  display: flex;
  align-items: center;
  gap: 16px;
  transition: all 0.3s;
}

.compact-stat-card:hover {
  border-color: #f97316;
  transform: translateY(-2px);
}

.stat-icon-mini {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-label-mini {
  font-size: 13px;
  color: #64748b;
  font-weight: bold;
  margin-bottom: 2px;
}

.stat-value-mini {
  font-size: 20px;
  font-weight: 800;
  color: #0f172a;
}

.stat-unit-mini {
  font-size: 12px;
  color: #94a3b8;
}

/* 第二行：主内容区 */
.main-content-grid {
  display: grid;
  grid-template-columns: 1fr 280px;
  gap: 12px;
  height: 320px; /* 进一步压缩高度 */
}

.chart-column {
  display: flex;
  flex-direction: column;
  overflow: hidden; /* 强制拦截内容溢出撑大外部 */
}

.section-header-compact {
  padding: 12px 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #f1f5f9;
}

.section-title-mini {
  font-size: 14px;
  font-weight: 800;
  color: #1e293b;
}

.chart-area-compact {
  flex: 1;
  padding: 16px;
  display: flex;
  flex-direction: column;
  justify-content: flex-start; /* 居上排列，不再拉伸 */
  gap: 16px;
}

.bar-chart-mini {
  display: flex;
  align-items: flex-end;
  gap: 10px;
  height: 100px;
}

.bar-mini-wrapper {
  flex: 1;
  background: #f8fafc;
  border-radius: 6px;
  height: 100%;
  display: flex;
  align-items: flex-end;
  overflow: hidden;
}

.bar-orange {
  width: 100%;
  background: linear-gradient(to top, #f97316, #fb923c);
  border-radius: 6px;
  transition: height 0.6s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.chart-footer-mini {
  font-size: 12px;
  color: #94a3b8;
  text-align: center;
  margin-top: 4px;
  opacity: 0.8;
}

.todo-column {
  display: flex;
  flex-direction: column;
  overflow: hidden; /* 强制拦截内容溢出 */
}

.todo-scroll-area {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.todo-item-compact {
  background: #fcfcfd;
  border: 1px solid #f1f5f9;
  padding: 8px 12px;
  border-radius: 10px;
  transition: all 0.2s;
}

.todo-item-compact:hover {
  border-color: #f97316;
  background: white;
  box-shadow: 0 4px 12px rgba(0,0,0,0.05);
}

.todo-item-top {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.todo-user-name {
  font-size: 13px;
  font-weight: bold;
  color: #334155;
  flex: 1;
}

.todo-time-mini {
  font-size: 10px;
  color: #94a3b8;
  font-family: monospace;
}

.todo-act-title {
  font-size: 12px;
  color: #64748b;
  margin-bottom: 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.todo-actions-mini {
  display: flex;
  gap: 8px;
}

.btn-mini {
  flex: 1;
  padding: 6px 0;
  border-radius: 8px;
  font-size: 11px;
  font-weight: bold;
  transition: all 0.2s;
  border: none;
  cursor: pointer;
}

.btn-pass { background: #f0fdf4; color: #16a34a; }
.btn-pass:hover { background: #16a34a; color: white; }

.btn-reject { background: #fef2f2; color: #dc2626; }
.btn-reject:hover { background: #dc2626; color: white; }

.empty-tip-mini {
  text-align: center;
  padding: 60px 0;
  color: #94a3b8;
  font-size: 12px;
}

@media (max-width: 1200px) {
  .main-content-grid {
    grid-template-columns: 1fr;
    height: auto;
  }
}

@media (max-width: 768px) {
  .stats-row-grid {
    grid-template-columns: 1fr;
  }
}
</style>