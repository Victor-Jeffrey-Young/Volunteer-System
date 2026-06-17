<template>
  <div class="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8 space-y-8">
    <!-- Welcome Section -->
    <div class="relative overflow-hidden rounded-3xl bg-gradient-to-br from-orange-500 to-amber-600 p-8 text-white shadow-xl">
      <div class="absolute right-0 top-0 -mr-20 -mt-20 h-64 w-64 rounded-full bg-white/10 blur-3xl"></div>
      <div class="relative z-10 flex flex-col md:flex-row md:items-center justify-between gap-6">
        <div class="space-y-2">
          <h1 class="text-3xl font-bold">欢迎回来，{{ userStore.user?.realName || '邻居' }}</h1>
          <p class="text-orange-50 max-w-md">
            远亲不如近邻，邻里互助平台致力于为您提供一个温馨、便利的社区生活环境。
          </p>
        </div>
        <div class="flex gap-4">
          <div class="bg-white/20 backdrop-blur-md rounded-2xl p-4 text-center min-w-[100px] border border-white/30">
            <div class="text-2xl font-bold">{{ stats.totalWishes }}</div>
            <div class="text-[10px] text-orange-100 uppercase tracking-wider">累计发布</div>
          </div>
          <div class="bg-white/20 backdrop-blur-md rounded-2xl p-4 text-center min-w-[100px] border border-white/30">
            <div class="text-2xl font-bold">{{ stats.completedWishes }}</div>
            <div class="text-[10px] text-orange-100 uppercase tracking-wider">已达成</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 快捷操作 -->
    <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
      <div 
        v-for="link in quickLinks" 
        :key="link.title"
        @click="handleLinkClick(link)"
        class="group cursor-pointer bg-white rounded-2xl p-6 shadow-sm border border-slate-100 hover:shadow-md hover:border-orange-200 transition-all"
      >
        <div class="flex items-center gap-4">
          <div :class="['w-12 h-12 rounded-xl flex items-center justify-center transition-transform group-hover:scale-110', link.color]">
            <component :is="link.icon" class="w-6 h-6" />
          </div>
          <div class="flex-1">
            <h3 class="font-bold text-slate-900 group-hover:text-orange-600 transition-colors">{{ link.title }}</h3>
            <p class="text-xs text-slate-500 mt-1">{{ link.desc }}</p>
          </div>
          <ArrowRight class="w-4 h-4 text-slate-300 group-hover:text-orange-400 group-hover:translate-x-1 transition-all" />
        </div>
      </div>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-3 gap-8">
      <!-- Activity Feed / Service Highlights -->
      <div class="lg:col-span-2 space-y-6">
        <div class="flex items-center justify-between">
          <h2 class="text-xl font-bold text-slate-900 flex items-center gap-2">
            <Sparkles class="w-5 h-5 text-orange-500" />
            社区互助动态
          </h2>
          <button @click="router.push('/resident/wishes')" class="text-xs text-orange-600 font-medium hover:underline">查看更多</button>
        </div>
        
        <div v-if="wishFeeds.length > 0" class="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div 
            v-for="feed in wishFeeds" 
            :key="feed.wishId"
            class="bg-white rounded-2xl p-5 border border-slate-100 shadow-sm relative overflow-hidden group hover:border-orange-200 transition-all"
          >
            <div class="absolute top-0 right-0 p-2 opacity-10 group-hover:opacity-20 transition-opacity">
              <Heart class="w-12 h-12 text-orange-600 fill-current" />
            </div>
            
            <div class="space-y-3 relative z-10">
              <div class="flex items-center gap-2">
                <span class="text-[10px] px-2 py-0.5 rounded-full bg-emerald-100 text-emerald-700 font-bold uppercase">功德圆满</span>
                <span class="text-xs text-slate-400">{{ feed.finishTime?.substring(5, 10) }}</span>
              </div>
              <h4 class="font-bold text-slate-900 truncate">{{ feed.title }}</h4>
              <div class="flex items-center justify-between pt-2 border-t border-slate-50">
                <div class="flex items-center gap-2 overflow-hidden">
                  <el-avatar :size="20" :src="getFullAvatar(feed.requesterAvatar)" style="flex-shrink: 0;" />
                  <span class="text-[10px] text-slate-500 truncate">{{ feed.requesterName }} 的求助</span>
                </div>
                <div class="flex items-center gap-2 text-xs text-orange-600 font-medium">
                  <el-avatar :size="20" :src="getFullAvatar(feed.volunteerAvatar)" />
                  由 {{ feed.volunteerName }} 完成
                </div>
              </div>
            </div>
          </div>
        </div>

        <div v-else class="bg-white rounded-2xl p-8 border border-slate-100 text-center space-y-4 shadow-sm">
          <div class="w-16 h-16 bg-orange-50 text-orange-500 rounded-full flex items-center justify-center mx-auto">
            <Heart class="w-8 h-8 fill-current" />
          </div>
          <div class="max-max-w-xs mx-auto space-y-2">
            <h3 class="font-bold text-slate-900">让爱心在邻里间流动</h3>
            <p class="text-sm text-slate-500">
              如果您在生活中遇到修水龙头、搬运重物或简单的咨询需求，不妨发布一个“微心愿”，热心的志愿者们随时准备着为您效劳。
            </p>
          </div>
          <button 
            @click="router.push('/resident/wishes?action=create')"
            class="bg-orange-600 text-white px-8 py-3 rounded-xl font-bold hover:bg-orange-700 transition-colors shadow-lg shadow-orange-600/20"
          >
            发布我的第一条心愿
          </button>
        </div>
      </div>

      <!-- Announcements -->
      <div class="space-y-6">
        <h2 class="text-xl font-bold text-slate-900 flex items-center gap-2">
          <Bell class="w-5 h-5 text-orange-500" />
          社区通知
        </h2>
        
        <div class="space-y-4">
          <div 
            v-for="notice in announcements" 
            :key="notice.noticeId"
            class="bg-white rounded-2xl p-4 border border-slate-100 hover:shadow-sm transition-shadow"
          >
            <div class="flex gap-4">
              <div class="w-10 h-10 rounded-full bg-slate-50 flex items-center justify-center shrink-0">
                <Bell class="w-5 h-5 text-slate-400" />
              </div>
              <div class="space-y-1 overflow-hidden">
                <h4 class="font-bold text-sm text-slate-900 truncate">{{ notice.title }}</h4>
                <p class="text-xs text-slate-500 line-clamp-2 leading-relaxed">
                  {{ notice.content?.replace(/<[^>]+>/g, '') }}
                </p>
                <div class="text-[10px] text-slate-400 pt-2">{{ notice.createTime?.substring(0, 10) }}</div>
              </div>
            </div>
          </div>
          <div v-if="announcements.length === 0" class="text-center py-10 bg-white rounded-2xl border border-slate-100 text-slate-400 text-sm">
            暂无社区公告
          </div>
        </div>

        <!-- Welfare Card -->
        <div class="bg-gradient-to-br from-indigo-500 to-blue-600 rounded-2xl p-6 text-white overflow-hidden relative shadow-lg shadow-indigo-500/10">
          <Gift class="absolute -right-4 -bottom-4 w-24 h-24 text-white/10 rotate-12" />
          <h4 class="font-bold mb-1">公益积分换礼遇</h4>
          <p class="text-[11px] text-blue-100 leading-relaxed mb-4">
            虽然居民端不直接赚取积分，但您的评价是给志愿者最好的礼物。
          </p>
          <div class="h-2 bg-white/20 rounded-full overflow-hidden">
            <div class="h-full bg-white w-2/3"></div>
          </div>
        </div>
      </div>
    </div>

    <!-- Help Dialog -->
    <el-dialog
      v-model="showHelpDialog"
      title="微心愿发布与处理全流程指引"
      width="90%"
      style="max-width: 550px; border-radius: 20px;"
      :show-close="false"
    >
      <div class="space-y-6">
        <div class="p-4 bg-emerald-50 rounded-2xl border border-emerald-100 flex gap-4">
          <ShieldCheck class="w-8 h-8 text-emerald-600 shrink-0" />
          <p class="text-sm text-emerald-800 leading-relaxed font-medium">
            为了让志愿服务更透明、更有序，微心愿采用“多方确认”的严谨流程：
          </p>
        </div>

        <div class="grid grid-cols-1 gap-4 relative before:absolute before:inset-0 before:ml-[31px] before:-translate-x-px md:before:mx-auto md:before:translate-x-0 before:h-full before:w-0.5 before:bg-gradient-to-b before:from-transparent before:via-slate-200 before:to-transparent">
          
          <div class="relative flex items-center justify-between md:justify-normal md:odd:flex-row-reverse group is-active">
            <div class="flex items-center justify-center w-10 h-10 rounded-full border-4 border-white bg-rose-100 text-rose-600 font-bold shrink-0 md:order-1 md:group-odd:-translate-x-1/2 md:group-even:translate-x-1/2 shadow-sm relative z-10">1</div>
            <div class="w-[calc(100%-4rem)] md:w-[calc(50%-2.5rem)] p-4 rounded-xl border border-slate-100 bg-white hover:bg-slate-50 transition-colors shadow-sm">
              <h5 class="font-bold text-slate-900 text-sm mb-1">发布与审核</h5>
              <p class="text-xs text-slate-500">明确标题和详细需求，留下联系方式。提交后由管理员审核并公示到心愿墙。</p>
            </div>
          </div>

          <div class="relative flex items-center justify-between md:justify-normal md:odd:flex-row-reverse group is-active">
            <div class="flex items-center justify-center w-10 h-10 rounded-full border-4 border-white bg-blue-100 text-blue-600 font-bold shrink-0 md:order-1 md:group-odd:-translate-x-1/2 md:group-even:translate-x-1/2 shadow-sm relative z-10">2</div>
            <div class="w-[calc(100%-4rem)] md:w-[calc(50%-2.5rem)] p-4 rounded-xl border border-slate-100 bg-white hover:bg-slate-50 transition-colors shadow-sm">
              <h5 class="font-bold text-slate-900 text-sm mb-1">志愿者揭榜与服务</h5>
              <p class="text-xs text-slate-500">热心志愿者认领心愿并与您联系，提供线下的帮扶服务，事后在系统中标记完成。</p>
            </div>
          </div>

          <div class="relative flex items-center justify-between md:justify-normal md:odd:flex-row-reverse group is-active">
            <div class="flex items-center justify-center w-10 h-10 rounded-full border-4 border-white bg-orange-100 text-orange-600 font-bold shrink-0 md:order-1 md:group-odd:-translate-x-1/2 md:group-even:translate-x-1/2 shadow-sm relative z-10">3</div>
            <div class="w-[calc(100%-4rem)] md:w-[calc(50%-2.5rem)] p-4 rounded-xl border border-orange-200 bg-orange-50 hover:bg-orange-100 transition-colors shadow-sm">
              <h5 class="font-bold text-orange-700 text-sm mb-1">居民核实与点赞 (核心)</h5>
              <p class="text-xs text-orange-600/80">志愿者标记完成后，需要<strong class="text-orange-700">您亲自在系统点击确认</strong>。您还可以在此环节给志愿者送花点赞 👍！</p>
            </div>
          </div>

          <div class="relative flex items-center justify-between md:justify-normal md:odd:flex-row-reverse group is-active">
            <div class="flex items-center justify-center w-10 h-10 rounded-full border-4 border-white bg-emerald-100 text-emerald-600 font-bold shrink-0 md:order-1 md:group-odd:-translate-x-1/2 md:group-even:translate-x-1/2 shadow-sm relative z-10">4</div>
            <div class="w-[calc(100%-4rem)] md:w-[calc(50%-2.5rem)] p-4 rounded-xl border border-slate-100 bg-white hover:bg-slate-50 transition-colors shadow-sm">
              <h5 class="font-bold text-slate-900 text-sm mb-1">系统结算奖励</h5>
              <p class="text-xs text-slate-500">在获得您的确认后，管理员将介入核发，最终为志愿者发放工时和荣誉积分，流程结束。</p>
            </div>
          </div>

        </div>

      </div>
      <template #footer>
        <div class="flex gap-3">
          <button @click="showHelpDialog = false" class="flex-1 py-3 text-slate-600 font-medium bg-slate-100 rounded-xl hover:bg-slate-200 transition-colors">我知道了</button>
          <button @click="router.push('/resident/wishes'); showHelpDialog = false" class="flex-1 py-3 bg-rose-600 text-white font-bold rounded-xl hover:bg-rose-700 transition-all shadow-lg shadow-rose-600/20">查看我的心愿</button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useUserStore } from '../../stores/user';
import { announcementApi, wishApi } from '../../api/modules';
import {
  Heart,
  Send,
  History,
  Bell,
  ArrowRight,
  Sparkles,
  ShieldCheck,
  Gift
} from 'lucide-vue-next';
import { getFullAvatar } from '../../utils/file';

const router = useRouter();
const userStore = useUserStore();
const announcements = ref([]);
const wishFeeds = ref([]);
const stats = ref({
  totalWishes: 0,
  completedWishes: 0,
  pendingWishes: 0
});

const fetchHomeData = async () => {
  if (!userStore.userId) return;
  try {
    // 1. 获取公告
    const noticeRes = await announcementApi.getAnnouncements(1, 4);
    announcements.value = noticeRes.data?.records || [];

    // 2. 获取我的心愿统计
    const wishRes = await wishApi.getMyWishes(userStore.userId, 'RESIDENT');
    const wishes = wishRes.data || [];
    stats.value.totalWishes = wishes.length;
    stats.value.completedWishes = wishes.filter(w => w.status === 3).length;
    stats.value.pendingWishes = wishes.filter(w => w.status === 1 || w.status === 2).length;

    // 3. 获取全平台动态流
    const feedRes = await wishApi.getPublicFeeds();
    wishFeeds.value = feedRes.data || [];
  } catch (err) {
    console.error('Fetch home data error:', err);
  }
};

onMounted(fetchHomeData);

const showHelpDialog = ref(false);

const quickLinks = [
  { title: '发布心愿', desc: '生活琐事求助，志愿者来帮您', icon: Send, path: '/resident/wishes?action=create', color: 'bg-orange-50 text-orange-600' },
  { title: '进度追踪', desc: '实时查看心愿办理进度', icon: History, path: '/resident/wishes', color: 'bg-orange-50 text-orange-600' },
  { title: '查看帮助', desc: '了解如何提交有效请求', icon: ShieldCheck, path: null, color: 'bg-orange-50 text-orange-600', action: () => showHelpDialog.value = true },
];

const handleLinkClick = (link) => {
  if (link.action) {
    link.action();
  } else if (link.path) {
    router.push(link.path);
  }
};

</script>
