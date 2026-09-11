<template>
  <div class="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8 py-8">
    <!-- Header & Search -->
    <div class="flex flex-col md:flex-row md:items-center justify-between gap-4 mb-8">
      <div>
        <h1 class="text-2xl font-bold text-slate-900">志愿活动</h1>
        <p class="text-sm text-slate-500 mt-1">发现身边有意义的志愿服务</p>
      </div>

      <div class="flex items-center gap-3">
        <div class="relative flex-1 md:w-80">
          <Search class="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-slate-400" />
          <input v-model="searchTitle" type="text" placeholder="搜索活动名称..." @keyup.enter="handleSearch"
            class="w-full rounded-xl border-slate-200 bg-white pl-10 pr-4 py-2.5 text-sm focus:border-orange-500 focus:ring-orange-500 shadow-sm" />
        </div>
        <button @click="handleSearch"
          class="flex items-center gap-2 rounded-xl bg-orange-600 px-6 py-2.5 text-sm font-bold text-white hover:bg-orange-700 shadow-md transition-all">
          搜索
        </button>
      </div>
    </div>

    <!-- Categories -->
    <div class="flex overflow-x-auto pb-4 mb-2 -mx-4 px-4 sm:mx-0 sm:px-0 hide-scrollbar gap-2">
      <button v-for="category in categories" :key="category" @click="activeTab = category"
        class="whitespace-nowrap rounded-full px-4 py-2 text-sm font-medium transition-colors"
        :class="activeTab === category ? 'bg-slate-900 text-white' : 'bg-white text-slate-600 hover:bg-slate-100 border border-slate-200'">
        {{ category }}
      </button>
    </div>

    <!-- 状态筛选 -->
    <div class="flex overflow-x-auto pb-4 mb-6 -mx-4 px-4 sm:mx-0 sm:px-0 hide-scrollbar gap-2">
      <div class="flex items-center text-xs text-slate-400 mr-2 font-bold uppercase tracking-wider">状态:</div>
      <button v-for="opt in statusOptions" :key="opt.label" @click="statusFilter = opt.value"
        class="whitespace-nowrap rounded-full px-4 py-1.5 text-xs font-bold transition-all"
        :class="statusFilter === opt.value ? 'bg-orange-600 text-white shadow-md' : 'bg-slate-100 text-slate-500 hover:bg-slate-200'">
        {{ opt.label }}
      </button>
    </div>

    <!-- Filters Bar -->
    <div
      class="flex items-center gap-4 mb-6 text-sm text-slate-600 bg-white p-3 rounded-xl border border-slate-200 shadow-sm">
      <span class="font-medium text-slate-900">排序：</span>
      <span class="text-orange-600 font-bold">最新发布</span>
      <span class="text-slate-300">|</span>
      <span class="text-slate-400">当前共有 {{ total }} 个活动</span>
    </div>

    <!-- Activity List -->
    <div v-if="loading" class="flex justify-center py-20">
      <div class="animate-spin rounded-full h-12 w-12 border-4 border-orange-600 border-t-transparent"></div>
    </div>
    <div v-else-if="activities.length > 0" class="grid sm:grid-cols-2 lg:grid-cols-3 gap-6">
      <div v-for="activity in activities" :key="activity.activityId"
        class="group flex flex-col rounded-2xl bg-white border border-slate-200 overflow-hidden hover:shadow-xl hover:shadow-slate-200/50 transition-all duration-300">
        <div class="relative h-48 overflow-hidden bg-slate-100">
          <img :src="getCategoryCover(activity)" :alt="activity.title"
            class="h-full w-full object-cover transition-transform duration-500 group-hover:scale-105"
            referrerpolicy="no-referrer" />
          <div class="absolute top-4 left-4 flex gap-2">
            <span
              class="inline-flex items-center rounded-full bg-white/90 backdrop-blur-sm px-2.5 py-0.5 text-xs font-semibold text-slate-700 shadow-sm">
              {{ activity.type || '志愿服务' }}
            </span>
          </div>
          <div
            class="absolute top-4 right-4 inline-flex items-center rounded-full bg-orange-500 px-2.5 py-0.5 text-xs font-bold text-white shadow-sm">
            {{ activity.rewardHours }} 工时
          </div>
          <div v-if="activity.currentNum >= activity.capacity"
            class="absolute inset-0 bg-slate-900/40 flex items-center justify-center backdrop-blur-[2px]">
            <span
              class="rounded-full bg-slate-900/80 px-4 py-1.5 text-sm font-bold text-white tracking-widest">名额已满</span>
          </div>
        </div>

        <div class="flex flex-col flex-1 p-6">
          <h3 class="text-lg font-bold text-slate-900 mb-2 group-hover:text-orange-600 transition-colors line-clamp-1">
            {{ activity.title }}
          </h3>
          <p class="text-xs text-slate-500 mb-4 flex items-center gap-1">
            状态：<span :class="activity.status === 0 ? 'text-emerald-600' : 'text-slate-400'" class="font-bold">
              {{ activity.status === 0 ? '招募中' : (activity.status === 1 ? '进行中' : '已结束') }}
            </span>
          </p>

          <div class="space-y-2.5 text-sm text-slate-600 mb-6 flex-1">
            <!-- 专业技能需求展示 -->
            <div v-if="hasRequiredSkills(activity)" class="flex flex-wrap gap-1.5 mb-3">
              <span v-for="skill in parseSkills(activity)" :key="skill"
                class="px-2 py-0.5 rounded-md bg-blue-50 text-blue-600 text-[10px] font-bold border border-blue-100 flex items-center gap-1">
                <Sparkles class="w-2.5 h-2.5" />
                {{ skill }}
              </span>
            </div>

            <div class="flex items-center gap-2">
              <Calendar class="h-4 w-4 text-slate-400 shrink-0" />
              <span>{{ activity.startTime ? activity.startTime.substring(0, 10) : '近期' }}</span>
            </div>
            <div class="flex items-center gap-2">
              <Clock class="h-4 w-4 text-slate-400 shrink-0" />
              <span>{{ activity.startTime ? activity.startTime.substring(11, 16) : '' }} - {{ activity.endTime ?
                activity.endTime.substring(11, 16) : '' }}</span>
            </div>
            <div class="flex items-start gap-2">
              <MapPin class="h-4 w-4 text-slate-400 shrink-0 mt-0.5" />
              <span class="line-clamp-2">{{ activity.location }}</span>
            </div>
            <div class="flex items-center gap-2 pt-2">
              <Users class="h-4 w-4 text-slate-400 shrink-0" />
              <div class="flex-1">
                <div class="flex justify-between text-xs mb-1">
                  <span>已报名 {{ activity.currentNum }} 人</span>
                  <span class="text-slate-400">总名额 {{ activity.capacity }}</span>
                </div>
                <div class="h-1.5 bg-slate-100 rounded-full overflow-hidden">
                  <div class="h-full rounded-full transition-all duration-500"
                    :class="activity.currentNum >= activity.capacity ? 'bg-slate-400' : 'bg-orange-500'"
                    :style="{ width: `${Math.min((activity.currentNum / activity.capacity) * 100, 100)}%` }"></div>
                </div>
              </div>
            </div>
          </div>

          <button @click="handleSignup(activity)"
            class="w-full rounded-xl py-2.5 text-sm font-bold transition-colors mt-auto shadow-sm" :class="{
              'bg-orange-50 text-orange-600 hover:bg-orange-600 hover:text-white': activity.status === 0 && activity.currentNum < activity.capacity && !registeredIds.has(activity.activityId),
              'bg-slate-100 text-slate-400 cursor-not-allowed': activity.status !== 0 || activity.currentNum >= activity.capacity || registeredIds.has(activity.activityId)
            }"
            :disabled="activity.status !== 0 || activity.currentNum >= activity.capacity || registeredIds.has(activity.activityId)">
            {{
              registeredIds.has(activity.activityId) ? '您已报名' :
                activity.status === 1 ? '活动进行中' :
                  activity.status === 2 ? '活动已结束' :
                    activity.currentNum >= activity.capacity ? '名额已满' : '立即报名'
            }}
          </button>
        </div>
      </div>
    </div>
    <div v-else class="text-center py-20 bg-white rounded-3xl border-2 border-dashed border-slate-200">
      <div class="text-slate-400 mb-2 text-lg font-medium">暂无匹配的活动</div>
      <p class="text-slate-400 text-sm">换个关键词或分类试试吧</p>
    </div>

    <!-- Pagination -->
    <div v-if="total > pageSize" class="mt-12 flex justify-center">
      <el-pagination v-model:current-page="currentPage" :page-size="pageSize" :total="total" layout="prev, pager, next"
        background @current-change="handlePageChange" />
    </div>

    <!-- Confirm Signup Modal -->
    <div v-if="confirmSignupActivity"
      class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/50 backdrop-blur-sm">
      <div
        class="bg-white rounded-3xl w-full max-w-md overflow-hidden shadow-2xl animate-in fade-in zoom-in duration-200 relative flex flex-col max-h-[90vh]">
        <button @click="confirmSignupActivity = null"
          class="absolute top-4 right-4 text-slate-400 hover:text-slate-600 p-1 bg-slate-100 rounded-full z-10">
          <X class="h-5 w-5" />
        </button>

        <div class="p-6 sm:p-8 overflow-y-auto">
          <h3 class="text-xl font-bold text-slate-900 mb-6 border-b border-slate-100 pb-4">确认报名信息</h3>

          <div class="flex gap-4 mb-6">
            <div class="w-24 h-24 rounded-xl overflow-hidden shrink-0 bg-slate-100">
              <img :src="getCategoryCover(confirmSignupActivity)" :alt="confirmSignupActivity.title"
                class="w-full h-full object-cover" />
            </div>
            <div>
              <h4 class="font-bold text-slate-900 mb-2 line-clamp-2">{{ confirmSignupActivity.title }}</h4>
              <div
                class="inline-flex items-center rounded-full bg-orange-100 px-2.5 py-0.5 text-xs font-bold text-orange-600">
                +{{ confirmSignupActivity.rewardHours }} 工时
              </div>
            </div>
          </div>

          <!-- 技能需求与匹配评估展示 -->
          <div v-if="hasRequiredSkills(confirmSignupActivity)" class="mb-6">
            <div class="flex items-center justify-between mb-2">
              <p class="text-xs font-bold text-slate-400 uppercase tracking-wider">专业技能评估</p>
              <span :class="getMatchResult(confirmSignupActivity).color"
                class="text-xs font-bold flex items-center gap-1">
                <CheckCircle v-if="getMatchResult(confirmSignupActivity).status === 'perfect'" class="w-3 h-3" />
                {{ getMatchResult(confirmSignupActivity).label }}
              </span>
            </div>
            <div class="flex flex-wrap gap-2">
              <span v-for="s in parseSkills(confirmSignupActivity)" :key="s"
                class="px-2.5 py-1 rounded-lg text-xs font-bold border flex items-center gap-1"
                :class="userSkills.includes(s) ? 'bg-emerald-50 text-emerald-700 border-emerald-200' : 'bg-slate-50 text-slate-400 border-slate-200'">
                <Sparkles v-if="userSkills.includes(s)" class="w-3 h-3" />
                {{ s }}
              </span>
            </div>
            <p v-if="getMatchResult(confirmSignupActivity).status === 'missing'"
              class="text-[10px] text-slate-400 mt-2">
              温馨提示：该岗位需要特定专业技能。（技能描述仅供参考）
            </p>
          </div>

          <div class="space-y-4 bg-slate-50 rounded-2xl p-4 border border-slate-100 mb-8">
            <div class="flex items-start gap-3">
              <Calendar class="w-5 h-5 text-slate-400 shrink-0 mt-0.5" />
              <div>
                <div class="text-sm font-medium text-slate-900">活动时间</div>
                <div class="text-sm text-slate-500">{{ confirmSignupActivity.startTime }} 至 {{
                  confirmSignupActivity.endTime }}</div>
              </div>
            </div>
            <div class="flex items-start gap-3">
              <MapPin class="w-5 h-5 text-slate-400 shrink-0 mt-0.5" />
              <div>
                <div class="text-sm font-medium text-slate-900">活动地点</div>
                <div class="text-sm text-slate-500">{{ confirmSignupActivity.location }}</div>
              </div>
            </div>
          </div>

          <div class="flex gap-3">
            <button @click="confirmSignupActivity = null" :disabled="submittingSignup"
              class="flex-1 bg-slate-100 text-slate-700 rounded-xl py-3 text-sm font-bold hover:bg-slate-50 transition-colors disabled:opacity-60 disabled:cursor-not-allowed">
              取消
            </button>
            <button @click="confirmSignup" :disabled="submittingSignup"
              class="flex-1 bg-orange-600 text-white rounded-xl py-3 text-sm font-bold hover:bg-orange-700 transition-colors shadow-lg shadow-orange-600/30 disabled:opacity-60 disabled:cursor-not-allowed">
              {{ submittingSignup ? '提交中...' : '确认报名' }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Signup Success Modal -->
    <div v-if="signupSuccessActivity"
      class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/50 backdrop-blur-sm">
      <div
        class="bg-white rounded-3xl w-full max-w-sm overflow-hidden shadow-2xl animate-in fade-in zoom-in duration-200 text-center relative">
        <button @click="signupSuccessActivity = null"
          class="absolute top-4 right-4 text-slate-400 hover:text-slate-600 p-1 bg-slate-100 rounded-full">
          <X class="h-5 w-5" />
        </button>

        <div class="p-8 pt-10">
          <div class="w-20 h-20 bg-emerald-100 rounded-full flex items-center justify-center mx-auto mb-6">
            <CheckCircle class="w-10 h-10 text-emerald-500" />
          </div>
          <h3 class="text-2xl font-bold text-slate-900 mb-2">报名成功！</h3>
          <p class="text-sm text-slate-500 mb-6">
            您已成功报名参加<br />
            <span class="font-bold text-slate-900">{{ signupSuccessActivity.title }}</span>
          </p>

          <div class="bg-slate-50 rounded-2xl p-4 border border-slate-100 mb-6 text-left space-y-3">
            <div class="flex items-start gap-2 text-sm">
              <Calendar class="w-4 h-4 text-slate-400 shrink-0 mt-0.5" />
              <span class="text-slate-700">{{ signupSuccessActivity.startTime }}</span>
            </div>
            <div class="flex items-start gap-2 text-sm">
              <MapPin class="w-4 h-4 text-slate-400 shrink-0 mt-0.5" />
              <span class="text-slate-700">{{ signupSuccessActivity.location }}</span>
            </div>
          </div>

          <button @click="signupSuccessActivity = null"
            class="w-full bg-orange-600 text-white rounded-xl py-3 text-sm font-bold hover:bg-orange-700 transition-colors shadow-lg shadow-orange-600/30">
            我知道了
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue';
import { activityApi, userApi } from '../../api/modules';
import { ElMessage } from 'element-plus';
import {
  Search,
  Filter,
  MapPin,
  Calendar,
  Clock,
  Users,
  CheckCircle,
  X,
  Sparkles,
} from 'lucide-vue-next';

import { useUserStore } from '../../stores/user';
import request from '../../utils/request';

const userStore = useUserStore();
const activeTab = ref('全部');
const statusFilter = ref('全部');
const confirmSignupActivity = ref(null);
const signupSuccessActivity = ref(null);
const submittingSignup = ref(false);
const activities = ref([]);
const registeredIds = ref(new Set());
const userSkills = ref([]);
const loading = ref(false);

// 分页与搜索状态
const searchTitle = ref('');
const currentPage = ref(1);
const pageSize = ref(6);
const total = ref(0);

// 辅助：解析 JSON 技能 (增加对下划线命名的兼容)
const parseSkills = (activityOrStr) => {
  if (!activityOrStr) return [];

  let rawData = '';
  if (typeof activityOrStr === 'object') {
    rawData = activityOrStr.requiredSkills || activityOrStr.required_skills;
  } else {
    rawData = activityOrStr;
  }

  try {
    const parsed = JSON.parse(rawData || '[]');
    return Array.isArray(parsed) ? parsed : [];
  } catch (e) {
    return rawData ? [rawData] : [];
  }
};

const hasRequiredSkills = (activity) => {
  return parseSkills(activity).length > 0;
};

// 辅助：计算技能匹配度
const getMatchResult = (activity) => {
  const required = parseSkills(activity);
  if (required.length === 0) return { status: 'general', label: '通用岗位', color: 'text-slate-500' };

  const matched = required.filter(s => userSkills.value.includes(s));
  const percent = Math.round((matched.length / required.length) * 100);

  if (percent === 100) return { status: 'perfect', label: '专业对口 (100%)', color: 'text-emerald-600' };
  if (percent > 0) return { status: 'partial', label: `技能匹配 (${percent}%)`, color: 'text-orange-600' };
  return { status: 'missing', label: '技能暂不匹配', color: 'text-rose-500' };
};

const fetchData = async () => {
  loading.value = true;
  try {
    const params = {
      current: currentPage.value,
      size: pageSize.value,
      title: searchTitle.value,
      type: activeTab.value === '全部' ? undefined : activeTab.value,
      status: statusFilter.value === '全部' ? undefined : statusFilter.value
    };
    const res = await activityApi.getActivities(params);
    activities.value = res.data?.records || [];
    total.value = res.data?.total || 0;

    const uid = userStore.userId;
    if (uid) {
      // 1. 获取已报名 ID（身份由后端从 JWT 解析）
      const regRes = await activityApi.getMySignups();
      const allRegs = regRes.data || [];

      // 只有处于 待审(0)、通过(1)、完结(3)、签到(5)、签退(6) 状态才视为“已占用”
      // 拒绝(2) 和 取消(4) 的记录不应阻止用户重新报名
      const activeRegs = allRegs.filter(r => ![2, 4].includes(r.status));
      registeredIds.value = new Set(activeRegs.map(r => r.activityId));

      // 2. 关键优化：先确保 userStore 里的数据是最新的
      if (!userStore.user) {
        await userStore.fetchCurrentUser();
      }

      // 3. 从最新的 user 对象中获取技能
      const uData = userStore.user || {};
      try {
        userSkills.value = JSON.parse(uData.skills || '[]');
      } catch (e) {
        userSkills.value = uData.skills ? [uData.skills] : [];
      }
    }
  } catch (error) {
    console.error("Fetch activities error:", error);
  } finally {
    loading.value = false;
  }
};

const handlePageChange = (page) => {
  currentPage.value = page;
  fetchData();
};

const handleSearch = () => {
  currentPage.value = 1;
  fetchData();
};

onMounted(fetchData);

const categories = ['全部', '社区服务', '环境保护', '教育助学', '助老服务', '医疗支援', '文化艺术', '技术支持', '赛事服务', '其他'];

// 活动分类 → 本地配图映射
const categoryImageMap = {
  '社区服务': '/Community Services.png',
  '环境保护': '/Environmental Protection.png',
  '教育助学': '/Educational Assistance.png',
  '助老服务': '/Elderly Care Services.png',
  '医疗支援': '/Medical Support.png',
  '文化艺术': '/Culture and Arts.png',
  '技术支持': '/Technical Support.png',
  '赛事服务': '/Event Services.png',
  '其他': '/Others.png',
};

const getCategoryCover = (activity) => {
  // 优先使用活动自带封面
  if (activity.cover) return activity.cover;
  // 根据分类匹配本地配图
  return categoryImageMap[activity.type] || '/Others.png';
};
const statusOptions = [
  { label: '全部', value: '全部' },
  { label: '招募中', value: 0 },
  { label: '进行中', value: 1 },
  { label: '已结束', value: 2 }
];

watch(activeTab, () => {
  currentPage.value = 1;
  fetchData();
});

watch(statusFilter, () => {
  currentPage.value = 1;
  fetchData();
});

const handleSignup = (activity) => {
  if (registeredIds.value.has(activity.activityId)) {
    return ElMessage.warning('您已经报名参加过该活动了');
  }
  if (activity.status !== 0) {
    return ElMessage.warning('该活动目前不在招募阶段');
  }
  if (activity.currentNum >= activity.capacity) {
    return ElMessage.warning('抱歉，该活动名额已满');
  }
  confirmSignupActivity.value = activity;
};

const confirmSignup = async () => {
  // 请求期间禁用按钮，防止双击重复提交 (后端另有锁+唯一索引兜底)
  if (submittingSignup.value || !confirmSignupActivity.value) return;
  submittingSignup.value = true;
  try {
    await activityApi.signup(confirmSignupActivity.value.activityId);
    signupSuccessActivity.value = confirmSignupActivity.value;
    confirmSignupActivity.value = null;
    fetchData();
  } catch (error) {
    console.error("Signup error:", error);
  } finally {
    submittingSignup.value = false;
  }
};
</script>

<style scoped>
.hide-scrollbar::-webkit-scrollbar {
  display: none;
}

.hide-scrollbar {
  -ms-overflow-style: none;
  scrollbar-width: none;
}
</style>