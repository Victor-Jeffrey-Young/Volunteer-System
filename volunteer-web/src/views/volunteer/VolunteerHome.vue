<script setup>
import { ref, onMounted, watch } from 'vue';
import { RouterLink } from 'vue-router';
import { activityApi, announcementApi, dashboardApi, leaderboardApi } from '../../api/modules';
import { useUserStore } from '../../stores/user';
import { getFullAvatar } from '../../utils/file';
import {
  ArrowRight,
  Calendar,
  MapPin,
  Users,
  Star,
  Clock,
  X,
  CheckCircle,
} from 'lucide-vue-next';

const userStore = useUserStore();
const selectedAnnouncement = ref(null);
const confirmSignupActivity = ref(null);
const signupSuccessActivity = ref(null);
const activities = ref([]);
const news = ref([]);
const leaderboardPreview = ref([]);
const loading = ref(false);

const stats = ref({
  volCount: 0,
  activeCount: 0,
  totalHours: 0,
});

const fetchData = async () => {
  loading.value = true;

  // 1. 获取基础统计数据
  dashboardApi.getBaseData().then(res => {
    if (res.data) stats.value = res.data;
  }).catch(e => console.error("Stats error:", e));

  // 2. 获取推荐活动
  activityApi.getActivities({ page: 1, pageSize: 2 }).then(res => {
    activities.value = res.data?.records || [];
  }).catch(e => console.error("Activities error:", e));

  // 3. 获取最新公告
  announcementApi.getAnnouncements(1, 3).then(res => {
    news.value = res.data?.records || [];
  }).catch(e => console.error("Announcements error:", e));

  // 4. 获取荣誉榜单预览
  leaderboardApi.getLeaderboard('points').then(res => {
    leaderboardPreview.value = (res.data || []).slice(0, 3);
  }).catch(e => console.error("Leaderboard preview error:", e));

  loading.value = false;
};

onMounted(() => {
  fetchData();
});

const handleSignup = (activity) => {
  confirmSignupActivity.value = activity;
};

const confirmSignup = async () => {
  if (confirmSignupActivity.value) {
    try {
      const uid = userStore.userId || localStorage.getItem('userId');
      await activityApi.signup(uid, confirmSignupActivity.value.activityId);
      signupSuccessActivity.value = confirmSignupActivity.value;
      confirmSignupActivity.value = null;
      fetchData();
    } catch (error) {
      console.error("Signup error:", error);
    }
  }
};

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
  if (activity.cover) return getFullAvatar(activity.cover, 'cover');
  return categoryImageMap[activity.type] || '/Others.png';
};
</script>

<template>
  <div class="space-y-12 pb-12">
    <!-- Hero Section -->
    <section class="relative h-[400px] sm:h-[500px] overflow-hidden">
      <div class="absolute inset-0">
        <img src="/hero-banner.png" alt="企鹅志愿者" class="h-full w-full object-cover" />
        <div class="absolute inset-0 bg-gradient-to-r from-slate-900/70 via-slate-900/40 to-transparent"></div>
      </div>
      <div class="relative mx-auto flex h-full max-w-7xl items-center px-4 sm:px-6 lg:px-8">
        <div class="max-w-2xl text-white">
          <h1 class="text-4xl font-bold tracking-tight sm:text-5xl lg:text-6xl mb-6">
            汇聚微光<br />
            <span class="text-orange-500">照亮社区</span>
          </h1>
          <p class="text-lg sm:text-xl text-slate-200 mb-8 max-w-xl">
            加入我们，用你的爱心和行动，为社区带来温暖与改变。每一次付出，都值得被铭记。
          </p>
          <div class="flex flex-wrap gap-4">
            <RouterLink to="/volunteer/activities"
              class="inline-flex items-center justify-center rounded-full bg-orange-600 px-8 py-3.5 text-sm font-semibold text-white shadow-sm hover:bg-orange-500 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-orange-600 transition-colors">
              寻找活动
            </RouterLink>
            <RouterLink to="/volunteer/profile"
              class="inline-flex items-center justify-center rounded-full bg-white/10 px-8 py-3.5 text-sm font-semibold text-white backdrop-blur-sm hover:bg-white/20 transition-colors border border-white/20">
              我的志愿历程
            </RouterLink>
          </div>
        </div>
      </div>
    </section>

    <!-- Stats Section -->
    <section class="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8 -mt-16 relative z-10">
      <div
        class="rounded-2xl bg-white shadow-xl shadow-slate-200/50 p-8 grid grid-cols-2 md:grid-cols-4 gap-8 divide-x divide-slate-100">
        <div class="text-center">
          <div class="text-3xl font-bold text-slate-900 mb-1">{{ stats.volCount || 0 }}</div>
          <div class="text-sm font-medium text-slate-500">注册志愿者</div>
        </div>
        <div class="text-center">
          <div class="text-3xl font-bold text-slate-900 mb-1">{{ stats.activeCount || 0 }}</div>
          <div class="text-sm font-medium text-slate-500">活跃活动</div>
        </div>
        <div class="text-center">
          <div class="text-3xl font-bold text-slate-900 mb-1">{{ stats.totalHours || 0 }}</div>
          <div class="text-sm font-medium text-slate-500">服务时长 (小时)</div>
        </div>
        <div class="text-center">
          <div class="text-3xl font-bold text-slate-900 mb-1">99%</div>
          <div class="text-sm font-medium text-slate-500">好评率</div>
        </div>
      </div>
    </section>

    <div class="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8 grid grid-cols-1 lg:grid-cols-3 gap-12">
      <!-- Left Column: Activities -->
      <div class="lg:col-span-2 space-y-12">
        <!-- Recommended Activities -->
        <section>
          <div class="flex items-center justify-between mb-8">
            <h2 class="text-2xl font-bold text-slate-900 flex items-center gap-2">
              <Star class="h-6 w-6 text-orange-500 fill-current" />
              推荐活动
            </h2>
            <RouterLink to="/volunteer/activities"
              class="text-sm font-medium text-orange-600 hover:text-orange-700 flex items-center gap-1">
              查看更多
              <ArrowRight class="h-4 w-4" />
            </RouterLink>
          </div>

          <div v-if="activities.length > 0" class="grid sm:grid-cols-2 gap-6">
            <div v-for="activity in activities" :key="activity.activityId"
              class="group rounded-2xl bg-white border border-slate-200 overflow-hidden hover:shadow-xl hover:shadow-slate-200/50 transition-all duration-300">
              <div class="relative h-48 overflow-hidden bg-slate-100">
                <img :src="getCategoryCover(activity)" :alt="activity.title"
                  class="h-full w-full object-cover transition-transform duration-500 group-hover:scale-105"
                  referrerpolicy="no-referrer" />
                <div class="absolute top-4 left-4 flex gap-2">
                  <span
                    class="inline-flex items-center rounded-full bg-white/90 backdrop-blur-sm px-2.5 py-0.5 text-xs font-semibold text-slate-700 shadow-sm">
                    {{ activity.type || '社区服务' }}
                  </span>
                </div>
                <div
                  class="absolute top-4 right-4 inline-flex items-center rounded-full bg-orange-500 px-2.5 py-0.5 text-xs font-bold text-white shadow-sm">
                  {{ activity.rewardHours }} 工时
                </div>
              </div>
              <div class="p-6">
                <h3
                  class="text-lg font-bold text-slate-900 mb-4 group-hover:text-orange-600 transition-colors line-clamp-1">
                  {{ activity.title }}
                </h3>
                <div class="space-y-2.5 text-sm text-slate-600 mb-6">
                  <div class="flex items-center gap-2">
                    <Calendar class="h-4 w-4 text-slate-400" />
                    {{ activity.startTime ? activity.startTime.substring(0, 10) : '近期' }}
                  </div>
                  <div class="flex items-center gap-2">
                    <Clock class="h-4 w-4 text-slate-400" />
                    <span>{{ activity.startTime ? activity.startTime.substring(11, 16) : '' }} - {{ activity.endTime ?
                      activity.endTime.substring(11, 16) : '' }}</span>
                  </div>
                  <div class="flex items-center gap-2">
                    <MapPin class="h-4 w-4 text-slate-400" />
                    <span class="truncate">{{ activity.location }}</span>
                  </div>
                  <div class="flex items-center gap-2">
                    <Users class="h-4 w-4 text-slate-400" />
                    <span>{{ activity.currentNum }} / {{ activity.capacity }} 人报名</span>
                  </div>
                </div>
                <button @click="handleSignup(activity)"
                  class="w-full rounded-xl py-2.5 text-sm font-semibold transition-colors bg-orange-50 text-orange-600 hover:bg-orange-100">
                  立即报名
                </button>
              </div>
            </div>
          </div>
          <div v-else
            class="text-center py-12 bg-white rounded-2xl border border-dashed border-slate-300 text-slate-400">
            暂无推荐活动
          </div>
        </section>
      </div>

      <!-- Right Column: News & Announcements -->
      <div class="space-y-8">
        <!-- Announcements -->
        <section class="rounded-2xl bg-white border border-slate-200 p-6">
          <div class="flex items-center justify-between mb-6">
            <h2 class="text-lg font-bold text-slate-900">最新公告</h2>
            <RouterLink to="/volunteer/announcements" class="text-sm text-slate-500 hover:text-orange-600">更多
            </RouterLink>
          </div>
          <div v-if="news.length > 0" class="space-y-4">
            <button v-for="item in news" :key="item.noticeId" @click="selectedAnnouncement = item"
              class="group block w-full text-left">
              <div class="flex items-start gap-3">
                <div class="flex-1">
                  <h3 class="text-sm font-medium text-slate-700 group-hover:text-orange-600 line-clamp-2 leading-snug">
                    {{ item.title }}
                  </h3>
                  <p class="text-xs text-slate-400 mt-1.5">{{ item.createTime ? item.createTime.substring(0, 10) : '' }}
                  </p>
                </div>
              </div>
            </button>
          </div>
          <div v-else class="text-center py-6 text-sm text-slate-400">
            暂无公告
          </div>
        </section>

        <!-- Leaderboard Preview -->
        <section
          class="rounded-2xl bg-gradient-to-br from-orange-500 to-red-500 p-6 text-white relative overflow-hidden">
          <div class="absolute top-0 right-0 -mt-4 -mr-4 w-24 h-24 bg-white/10 rounded-full blur-2xl"></div>
          <div class="relative z-10">
            <h2 class="text-lg font-bold mb-2">荣誉榜单</h2>
            <p class="text-orange-100 text-sm mb-6">看看谁是本月的志愿之星</p>

            <div class="space-y-4 mb-6">
              <div v-for="(user, index) in leaderboardPreview" :key="index"
                class="flex items-center gap-3 bg-white/10 rounded-xl p-2 backdrop-blur-sm">
                <div class="w-6 text-center font-bold text-orange-200 text-sm">{{ index + 1 }}</div>
                <div class="h-8 w-8 rounded-full bg-white/20 overflow-hidden">
                  <img :src="getFullAvatar(user.avatar)" alt="avatar" class="w-full h-full object-cover" />
                </div>
                <div class="flex-1 text-sm font-medium truncate">{{ user.real_name }}</div>
                <div class="text-sm font-bold text-orange-200">{{ user.points || 0 }} 分</div>
              </div>
            </div>

            <RouterLink to="/volunteer/leaderboard"
              class="block w-full text-center bg-white text-orange-600 rounded-xl py-2.5 text-sm font-bold hover:bg-orange-50 transition-colors">
              查看完整榜单
            </RouterLink>
          </div>
        </section>
      </div>
    </div>

    <!-- Announcement Detail Modal -->
    <div v-if="selectedAnnouncement"
      class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/50 backdrop-blur-sm">
      <div
        class="bg-white rounded-3xl w-full max-w-2xl overflow-hidden shadow-2xl animate-in fade-in zoom-in duration-200 flex flex-col max-h-[90vh]">
        <div class="p-4 border-b border-slate-100 flex justify-between items-center bg-slate-50 shrink-0">
          <h3 class="font-bold text-slate-900">公告详情</h3>
          <button @click="selectedAnnouncement = null" class="text-slate-400 hover:text-slate-600 p-1">
            <X class="h-5 w-5" />
          </button>
        </div>
        <div class="p-6 sm:p-8 overflow-y-auto">
          <h2 class="text-2xl font-bold text-slate-900 mb-4 leading-snug">
            {{ selectedAnnouncement.title }}
          </h2>
          <div class="flex items-center gap-4 text-sm text-slate-500 mb-8 pb-6 border-b border-slate-100">
            <div class="flex items-center gap-1.5">
              <Calendar class="w-4 h-4" />
              {{ selectedAnnouncement.createTime ? selectedAnnouncement.createTime.substring(0, 10) : '' }}
            </div>
            <div>发布者：{{ selectedAnnouncement.publisherName || '系统管理员' }}</div>
          </div>
          <div class="prose prose-slate prose-orange max-w-none text-slate-700 leading-relaxed">
            <p>{{ selectedAnnouncement.content }}</p>
          </div>
        </div>
        <div class="p-4 border-t border-slate-100 bg-slate-50 shrink-0">
          <button @click="selectedAnnouncement = null"
            class="w-full sm:w-auto sm:px-8 py-2.5 bg-orange-600 text-white rounded-xl text-sm font-bold hover:bg-orange-700 transition-colors shadow-sm mx-auto block">
            我知道了
          </button>
        </div>
      </div>
    </div>

    <!-- Confirm Signup Modal -->
    <div v-if="confirmSignupActivity"
      class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/50 backdrop-blur-sm">
      <div
        class="bg-white rounded-3xl w-full max-w-md overflow-hidden shadow-2xl animate-in fade-in zoom-in duration-200 relative">
        <button @click="confirmSignupActivity = null"
          class="absolute top-4 right-4 text-slate-400 hover:text-slate-600 p-1 bg-slate-100 rounded-full z-10">
          <X class="h-5 w-5" />
        </button>

        <div class="p-6 sm:p-8">
          <h3 class="text-xl font-bold text-slate-900 mb-6 border-b border-slate-100 pb-4">确认报名信息</h3>

          <div class="flex gap-4 mb-6">
            <div class="w-24 h-24 rounded-xl overflow-hidden shrink-0">
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
              <MapPin class="h-5 w-5 text-slate-400 shrink-0 mt-0.5" />
              <div>
                <div class="text-sm font-medium text-slate-900">活动地点</div>
                <div class="text-sm text-slate-500">{{ confirmSignupActivity.location }}</div>
              </div>
            </div>
          </div>

          <div class="flex gap-3">
            <button @click="confirmSignupActivity = null"
              class="flex-1 bg-slate-100 text-slate-700 rounded-xl py-3 text-sm font-bold hover:bg-slate-200 transition-colors">
              取消
            </button>
            <button @click="confirmSignup"
              class="flex-1 bg-orange-600 text-white rounded-xl py-3 text-sm font-bold hover:bg-orange-700 transition-colors shadow-lg shadow-orange-600/30">
              确认报名
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
