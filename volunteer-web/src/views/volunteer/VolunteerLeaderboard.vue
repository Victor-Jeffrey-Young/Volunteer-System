<template>
  <div class="mx-auto max-w-4xl px-4 sm:px-6 lg:px-8 py-8">
    <!-- Header -->
    <div class="text-center mb-12">
      <div class="inline-flex items-center justify-center h-16 w-16 rounded-full bg-orange-100 text-orange-600 mb-4">
        <Trophy class="h-8 w-8" />
      </div>
      <h1 class="text-3xl font-bold text-slate-900 mb-4">荣誉殿堂</h1>
      <p class="text-slate-600 max-w-2xl mx-auto">
        致敬每一位无私奉献的志愿者。您的每一次行动，都在让世界变得更美好。
      </p>
    </div>

    <!-- Current User Rank Card -->
    <div class="mb-8 bg-gradient-to-r from-slate-900 to-slate-800 text-white rounded-2xl p-6 sm:p-8 shadow-xl flex flex-col sm:flex-row items-center justify-between border border-slate-700 gap-6 relative overflow-hidden">
      <div class="absolute top-0 right-0 -mt-8 -mr-8 w-32 h-32 bg-white/5 rounded-full blur-2xl"></div>
      <div class="flex items-center gap-6 w-full sm:w-auto relative z-10">
        <div class="w-16 h-16 rounded-full flex items-center justify-center bg-slate-800 border-2 border-orange-500 font-bold text-2xl shadow-[0_0_15px_rgba(249,115,22,0.3)] text-orange-500">
          {{ myRank }}
        </div>
        <div class="flex items-center gap-4">
          <div class="w-14 h-14 rounded-full overflow-hidden border-2 border-slate-600 shrink-0 bg-slate-700">
            <img :src="userStore.user?.avatar || '/default-Avatar.png'" alt="Me" class="w-full h-full object-cover" />
          </div>
          <div>
            <div class="font-bold text-lg flex items-center gap-2">
              {{ userStore.user?.realName || userStore.user?.username || '志愿者' }}
              <span class="text-[10px] bg-orange-500 px-2 py-0.5 rounded-full font-bold">{{ getLevel(userStore.user?.totalPoints || 0) }}</span>
            </div>
            <div class="text-sm text-slate-400 mt-1">
              {{ category === 'points' ? '当前积分排名' : '当前时长排名' }}
            </div>
          </div>
        </div>
      </div>
      <div class="flex gap-8 w-full sm:w-auto justify-around sm:justify-end border-t sm:border-t-0 border-slate-700 pt-6 sm:pt-0 relative z-10">
        <div class="text-center sm:text-right">
          <div class="font-bold text-orange-400 text-2xl">{{ userStore.user?.totalPoints || 0 }}</div>
          <div class="text-xs text-slate-400 mt-1">累计积分</div>
        </div>
        <div class="w-px h-10 bg-slate-700 hidden sm:block self-center"></div>
        <div class="text-center sm:text-right">
          <div class="font-bold text-emerald-400 text-2xl">{{ userStore.user?.totalHours || 0 }}</div>
          <div class="text-xs text-slate-400 mt-1">志愿时长 (h)</div>
        </div>
      </div>
    </div>

    <!-- Controls -->
    <div class="flex flex-col sm:flex-row items-center justify-between gap-4 mb-8 bg-white p-4 rounded-2xl border border-slate-200 shadow-sm">
      <div class="flex items-center gap-2 w-full sm:w-auto">
        <span class="text-sm text-slate-500 font-medium whitespace-nowrap">排序依据：</span>
        <select
          v-model="category"
          class="w-full sm:w-auto bg-slate-50 border border-slate-200 text-slate-700 text-sm rounded-xl focus:ring-orange-500 focus:border-orange-500 block p-2.5"
        >
          <option value="points">累计积分榜</option>
          <option value="hours">服务时长榜</option>
          <option value="likes">居民赞赏榜(新)</option>
        </select>
      </div>
    </div>

    <div v-loading="loading">
      <!-- Top 3 Podium -->
      <div class="hidden md:flex items-end justify-center gap-6 mb-12 pt-8">
        <!-- 2nd Place -->
        <div v-if="leaderboardData.length > 1" class="flex flex-col items-center w-40">
          <div class="relative mb-4">
            <div class="w-20 h-20 rounded-full border-4 border-slate-200 overflow-hidden shadow-lg bg-slate-100">
              <img :src="getFullAvatar(leaderboardData[1].avatar)" :alt="leaderboardData[1].realName" class="w-full h-full object-cover" />
            </div>
            <div class="absolute -bottom-3 left-1/2 -translate-x-1/2 bg-slate-200 text-slate-600 text-xs font-bold px-3 py-1 rounded-full border-2 border-white">
              NO.2
            </div>
          </div>
          <div class="text-center bg-white rounded-t-2xl border-t border-x border-slate-200 w-full pt-6 pb-4 shadow-sm relative z-10">
            <h3 class="font-bold text-slate-900 truncate px-2">{{ leaderboardData[1].realName || leaderboardData[1].username }}</h3>
            <p class="text-sm font-medium text-slate-500 mt-1">{{ category === 'points' ? `${leaderboardData[1].totalPoints || 0} 分` : (category === 'hours' ? `${leaderboardData[1].totalHours || 0} 小时` : `👍 ${leaderboardData[1].likes || 0} 赞`) }}</p>
          </div>
          <div class="h-24 w-full bg-gradient-to-b from-slate-100 to-slate-50 border-x border-slate-200 rounded-b-xl flex flex-col items-center pt-3 shadow-[inset_0_4px_6px_-4px_rgba(0,0,0,0.1)] relative z-0">
            <div class="text-[10px] font-bold text-slate-500 bg-slate-200/70 px-2 py-0.5 rounded-full mb-2 border border-slate-300/50">
              {{ getLevel(leaderboardData[1].totalPoints || 0) }}
            </div>
            <div class="flex flex-wrap justify-center gap-x-3 gap-y-1 text-xs text-slate-500/80 font-medium w-full px-2">
              <span v-if="category !== 'points'" class="flex items-center gap-1" title="累计积分">🏆 {{ leaderboardData[1].totalPoints || 0 }}</span>
              <span v-if="category !== 'hours'" class="flex items-center gap-1" title="志愿时长">⏱️ {{ leaderboardData[1].totalHours || 0 }}h</span>
              <span v-if="category !== 'likes'" class="flex items-center gap-1" title="居民点赞">👍 {{ leaderboardData[1].likes || 0 }}</span>
            </div>
          </div>
        </div>

        <!-- 1st Place -->
        <div v-if="leaderboardData.length > 0" class="flex flex-col items-center w-48 -mt-8">
          <div class="relative mb-4">
            <div class="absolute -top-6 left-1/2 -translate-x-1/2 text-yellow-500">
              <Trophy class="w-10 h-10 fill-current drop-shadow-md" />
            </div>
            <div class="w-28 h-28 rounded-full border-4 border-yellow-400 overflow-hidden shadow-xl shadow-yellow-500/20 bg-slate-100">
              <img :src="getFullAvatar(leaderboardData[0].avatar)" :alt="leaderboardData[0].realName" class="w-full h-full object-cover" />
            </div>
            <div class="absolute -bottom-3 left-1/2 -translate-x-1/2 bg-gradient-to-r from-yellow-400 to-yellow-500 text-white text-sm font-bold px-4 py-1 rounded-full border-2 border-white shadow-sm">
              NO.1
            </div>
          </div>
          <div class="text-center bg-white rounded-t-2xl border-t border-x border-yellow-200 w-full pt-8 pb-4 shadow-[0_-4px_10px_rgba(0,0,0,0.02)] relative z-10">
            <h3 class="font-bold text-lg text-slate-900 truncate px-2">{{ leaderboardData[0].realName || leaderboardData[0].username }}</h3>
            <p class="text-base font-bold text-orange-600 mt-1">{{ category === 'points' ? `${leaderboardData[0].totalPoints || 0} 分` : (category === 'hours' ? `${leaderboardData[0].totalHours || 0} 小时` : `👍 ${leaderboardData[0].likes || 0} 赞`) }}</p>
          </div>
          <div class="h-32 w-full bg-gradient-to-b from-yellow-50 to-amber-50/30 border-x border-yellow-200/50 rounded-b-xl flex flex-col items-center pt-4 shadow-[inset_0_4px_6px_-2px_rgba(0,0,0,0.05)] relative z-0">
            <div class="text-xs font-bold text-yellow-700 bg-yellow-200/50 px-3 py-1 rounded-full mb-4 shadow-sm border border-yellow-300 text-center">
              🎖️ {{ getLevel(leaderboardData[0].totalPoints || 0) }}
            </div>
            <div class="grid grid-cols-2 gap-x-2 gap-y-2 text-xs text-amber-700 w-full px-4">
              <div v-if="category !== 'points'" class="flex flex-col items-center bg-white/60 rounded p-1 border border-yellow-100/50 shadow-sm"><span class="text-[10px] text-amber-600/70">总积分</span><span class="font-bold border-t border-yellow-200/30 w-full text-center mt-0.5 pt-0.5">{{ leaderboardData[0].totalPoints || 0 }}</span></div>
              <div v-if="category !== 'hours'" class="flex flex-col items-center bg-white/60 rounded p-1 border border-yellow-100/50 shadow-sm"><span class="text-[10px] text-amber-600/70">总时长</span><span class="font-bold border-t border-yellow-200/30 w-full text-center mt-0.5 pt-0.5">{{ leaderboardData[0].totalHours || 0 }}h</span></div>
              <div v-if="category !== 'likes'" class="flex flex-col items-center bg-white/60 rounded p-1 border border-yellow-100/50 shadow-sm"><span class="text-[10px] text-amber-600/70">点赞数</span><span class="font-bold border-t border-yellow-200/30 w-full text-center mt-0.5 pt-0.5">{{ leaderboardData[0].likes || 0 }}</span></div>
            </div>
          </div>
        </div>

        <!-- 3rd Place -->
        <div v-if="leaderboardData.length > 2" class="flex flex-col items-center w-40">
          <div class="relative mb-4">
            <div class="w-20 h-20 rounded-full border-4 border-orange-200 overflow-hidden shadow-lg shadow-orange-500/10 bg-slate-100">
              <img :src="getFullAvatar(leaderboardData[2].avatar)" :alt="leaderboardData[2].realName" class="w-full h-full object-cover" />
            </div>
            <div class="absolute -bottom-3 left-1/2 -translate-x-1/2 bg-orange-200 text-orange-700 text-xs font-bold px-3 py-1 rounded-full border-2 border-white">
              NO.3
            </div>
          </div>
          <div class="text-center bg-white rounded-t-2xl border-t border-x border-orange-100 w-full pt-6 pb-4 shadow-sm relative z-10">
            <h3 class="font-bold text-slate-900 truncate px-2">{{ leaderboardData[2].realName || leaderboardData[2].username }}</h3>
            <p class="text-sm font-medium text-slate-500 mt-1">{{ category === 'points' ? `${leaderboardData[2].totalPoints || 0} 分` : (category === 'hours' ? `${leaderboardData[2].totalHours || 0} 小时` : `👍 ${leaderboardData[2].likes || 0} 赞`) }}</p>
          </div>
          <div class="h-20 w-full bg-gradient-to-b from-orange-50 to-white border-x border-orange-100/50 rounded-b-xl flex flex-col items-center pt-2 shadow-[inset_0_4px_6px_-4px_rgba(0,0,0,0.05)] relative z-0">
            <div class="text-[10px] font-bold text-orange-700 bg-orange-200/40 px-2 py-0.5 rounded-full mb-1 border border-orange-200">
              {{ getLevel(leaderboardData[2].totalPoints || 0) }}
            </div>
            <div class="flex gap-2 text-[10px] text-orange-900/60 font-medium w-full justify-center mt-1">
              <span v-if="category !== 'points'" class="flex items-center gap-1 bg-white/50 px-1.5 rounded border border-orange-100">🏆 {{ leaderboardData[2].totalPoints || 0 }}</span>
              <span v-if="category !== 'hours'" class="flex items-center gap-1 bg-white/50 px-1.5 rounded border border-orange-100">⏱️ {{ leaderboardData[2].totalHours || 0 }}h</span>
              <span v-if="category !== 'likes'" class="flex items-center gap-1 bg-white/50 px-1.5 rounded border border-orange-100">👍 {{ leaderboardData[2].likes || 0 }}</span>
            </div>
          </div>
        </div>
        </div>

        <!-- List View -->
        <div class="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
        <div class="grid grid-cols-12 gap-2 sm:gap-4 p-4 bg-slate-50 border-b border-slate-200 text-xs font-bold text-slate-500 uppercase tracking-wider">
          <div class="col-span-2 sm:col-span-1 text-center">排名</div>
          <div class="col-span-4 sm:col-span-5">志愿者</div>
          <div class="col-span-2 text-right">积分</div>
          <div class="col-span-3 text-right">核心成就</div>
          <div class="col-span-1 text-center">趋势</div>
        </div>

        <div class="divide-y divide-slate-100">
          <div
            v-for="(user, index) in leaderboardData"
            :key="user.userId"
            class="grid grid-cols-12 gap-4 p-4 items-center hover:bg-slate-50 transition-colors"
          >
            <div class="col-span-2 sm:col-span-1 flex justify-center">
              <div class="w-10 h-10 rounded-full flex items-center justify-center border" :class="getRankStyle(index + 1)">
                <Trophy v-if="index === 0" class="w-6 h-6 text-yellow-500 fill-current" />
                <Medal v-else-if="index === 1" class="w-6 h-6 text-slate-400 fill-current" />
                <Medal v-else-if="index === 2" class="w-6 h-6 text-orange-500 fill-current" />
                <span v-else class="text-lg font-bold">{{ index + 1 }}</span>
              </div>
            </div>

            <div class="col-span-4 sm:col-span-5 flex items-center gap-3 min-w-0">
              <div class="w-10 h-10 sm:w-12 sm:h-12 rounded-full overflow-hidden border-2 border-white shadow-sm shrink-0 bg-slate-100">
                <img :src="getFullAvatar(user.avatar)" :alt="user.realName" class="w-full h-full object-cover" />
              </div>
              <div class="flex-1 min-w-0 border-transparent">
                <div class="font-bold text-slate-900 flex items-center gap-2 min-w-0">
                  <span class="truncate block">{{ user.realName || user.username || '未知用户' }}</span>
                  <span class="hidden sm:inline-flex items-center text-[10px] bg-orange-100 text-orange-700 px-2 py-0.5 rounded-full font-bold whitespace-nowrap shrink-0">
                    {{ getLevel(user.totalPoints || 0) }}
                  </span>
                </div>
                <div class="text-xs text-slate-500 mt-0.5 sm:hidden">{{ getLevel(user.totalPoints || 0) }}</div>
              </div>
            </div>

            <div class="col-span-2 text-right">
              <div class="font-bold text-orange-600 text-sm sm:text-lg">
                {{ user.totalPoints || 0 }} 分
              </div>
            </div>

            <div class="col-span-3 text-right flex flex-col justify-center items-end">
              <div v-if="category === 'points' || category === 'hours'" class="font-bold text-slate-700 text-sm sm:text-base">
                ⏱ {{ user.totalHours || 0 }}<span class="hidden sm:inline"> 小时</span>
              </div>
              <div v-else class="font-bold text-rose-500 text-sm sm:text-base">
                👍 {{ user.likes || 0 }}<span class="hidden sm:inline"> 个赞</span>
              </div>
            </div>

            <div class="col-span-1 flex justify-center">
              <div class="flex items-center text-xs font-bold" :class="getTrend(user, index + 1).color">
                <component :is="getTrend(user, index + 1).component" class="w-3 h-3 mr-0.5" />
                <span v-if="getTrend(user, index + 1).change > 0">{{ getTrend(user, index + 1).change }}</span>
              </div>
            </div>
          </div>
        </div>
        </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue';
import { dashboardApi } from '../../api/modules';
import { useUserStore } from '../../stores/user';
import { getFullAvatar } from '../../utils/file';
import { getLevelInfo } from '../../utils/levelRules';
import {
  Trophy,
  Medal,
  ArrowUp,
  ArrowDown,
  Minus,
} from 'lucide-vue-next';

const category = ref('points');
const leaderboardData = ref([]);
const userStore = useUserStore();
const myRank = ref('-');
const loading = ref(false);

const getLevel = (points) => {
  return getLevelInfo(points).name;
};

// 支持多轨制的真实趋势计算逻辑
const getTrend = (user, currentRank) => {
  // 根据当前所在的榜单分类，选择对应的历史名次字段
  // 积分榜用 lastRank，时长榜用 lastHoursRank，获赞榜暂无历史排名字段
  const lastRank = category.value === 'points'
      ? (user.lastRank || 0)
      : (category.value === 'hours' ? (user.lastHoursRank || 0) : 0);

  if (lastRank === 0) return { color: 'text-slate-400', component: Minus, change: 0 };

  const diff = lastRank - currentRank;

  if (diff > 0) return { color: 'text-emerald-500', component: ArrowUp, change: diff };
  if (diff < 0) return { color: 'text-red-500', component: ArrowDown, change: Math.abs(diff) };

  return { color: 'text-slate-400', component: Minus, change: 0 };
};

const fetchData = async () => {
  loading.value = true;
  try {
    const res = await dashboardApi.getVolunteerList();
    const allUsers = res.data || [];

    const effectiveUserId = userStore.userId;

    let sortedList = [];
    if (category.value === 'points') {
      sortedList = [...allUsers].sort((a, b) => (b.totalPoints || 0) - (a.totalPoints || 0));
    } else if (category.value === 'hours') {
      sortedList = [...allUsers].sort((a, b) => (b.totalHours || 0) - (a.totalHours || 0));
    } else {
      sortedList = [...allUsers].sort((a, b) => (b.likes || 0) - (a.likes || 0));
    }

    // 为每个用户注入“当前实时排名”属性，供模板计算趋势使用
    leaderboardData.value = sortedList.map((user, index) => ({
      ...user,
      currentRealRank: index + 1
    }));

    // 计算当前登录用户的名次
    if (effectiveUserId) {
      const myIndex = sortedList.findIndex(item => String(item.userId) === String(effectiveUserId));
      myRank.value = myIndex !== -1 ? myIndex + 1 : '500+';
    }

    leaderboardData.value = leaderboardData.value.slice(0, 50);
  } catch (error) {
    console.error("Fetch leaderboard error:", error);
  } finally {
    loading.value = false;
  }
};

onMounted(fetchData);

// 监听分类切换
watch(category, () => {
  fetchData();
});

const getRankStyle = (rank) => {
  switch (rank) {
    case 1: return 'bg-yellow-100 text-yellow-600 border-yellow-200';
    case 2: return 'bg-slate-100 text-slate-500 border-slate-200';
    case 3: return 'bg-orange-100 text-orange-600 border-orange-200';
    default: return 'bg-white text-slate-500 border-slate-100';
  }
};
</script>
