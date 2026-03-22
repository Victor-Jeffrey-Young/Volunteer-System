<script setup>
import { ref, onMounted, computed } from 'vue';
import { useRoute, useRouter, RouterLink } from 'vue-router';
import { useUserStore } from '../stores/user';
import { getFullAvatar } from '../utils/file';
import { announcementApi } from '../api/modules'; // 🚨 顶部引入 API
import { ElMessage } from 'element-plus';
import {
  Heart,
  Search,
  Bell,
  Menu,
  X,
  Globe,
  Users,
  Share2,
  Phone,
  Mail,
} from 'lucide-vue-next';

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();

const isMobileMenuOpen = ref(false);
const isNotifOpen = ref(false);
const notifications = ref([]);

const userAvatar = computed(() => getFullAvatar(userStore.user?.avatar || localStorage.getItem('avatar')));
const userName = computed(() => userStore.user?.realName || localStorage.getItem('realName') || '志愿者');

// 🚨 企业级：从后端拉取真实的通知数据
const fetchLiveNotifications = async () => {
  try {
    const res = await announcementApi.getAnnouncements(1, 5);
    // 🚨 增强版逻辑：除非后端明确返回 isRead 为 true 或 1，否则一律视为未读
    notifications.value = (res.data?.records || []).map(n => {
      // 这里的判定逻辑要非常宽松：
      // 如果 isRead 是 1、true，说明已读；
      // 如果是 0、null、undefined，说明没读过，显示红点
      const isActuallyRead = n.isRead === 1 || n.isRead === true;
      
      return {
        id: n.noticeId,
        title: n.title,
        desc: n.content,
        time: n.createTime?.substring(5, 16),
        unread: !isActuallyRead 
      };
    });
  } catch (e) {
    console.error("加载通知失败", e);
  }
};

const markAllAsRead = async () => {
  const uid = userStore.userId || localStorage.getItem('userId');
  if (!uid) return;
  
  try {
    await announcementApi.markAllAsRead(uid);
    notifications.value.forEach(n => n.unread = false);
    ElMessage.success('已全部标记为已读');
  } catch (e) {
    console.error(e);
  }
};

const markAsRead = async (notif) => {
  if (!notif.unread) return;
  try {
    await announcementApi.markAsRead(notif.id);
    notif.unread = false;
  } catch (e) {
    console.error(e);
  }
};

const goToAllNotifications = () => {
  isNotifOpen.value = false;
  router.push('/volunteer/announcements');
};

onMounted(() => {
  userStore.fetchCurrentUser();
  fetchLiveNotifications();
});

const navLinks = [
  { name: '首页', path: '/volunteer/home' },
  { name: '志愿活动', path: '/volunteer/activities' },
  { name: '积分商城', path: '/volunteer/mall' },
  { name: '微心愿', path: '/volunteer/wishes' },
  { name: '荣誉殿堂', path: '/volunteer/leaderboard' },
  { name: '公告通知', path: '/volunteer/announcements' },
  { name: '个人中心', path: '/volunteer/profile' },
];

const isActive = (path) => {
  return route.path === path;
};

const hasUnread = computed(() => notifications.value.some(n => n.unread));

// 🚨 帮助中心弹窗控制
const isHelpOpen = ref(false);
const helpTab = ref('guide'); 

const openHelp = (tab = 'guide') => {
  helpTab.value = tab;
  isHelpOpen.value = true;
};

// 🚨 关于我们弹窗控制
const isAboutOpen = ref(false);
const aboutTab = ref('mission'); // 'mission', 'policy', 'join'

const openAbout = (tab = 'mission') => {
  aboutTab.value = tab;
  isAboutOpen.value = true;
};
</script>

<template>
  <div class="min-h-screen flex flex-col bg-slate-50 text-slate-900 font-sans">
    <!-- Header -->
    <header class="sticky top-0 z-50 w-full border-b border-slate-200 bg-white/80 backdrop-blur-md">
      <div class="mx-auto flex max-w-7xl items-center justify-between px-4 py-3 sm:px-6 lg:px-8">
        <div class="flex items-center gap-2 sm:gap-4">
          <RouterLink to="/volunteer/home" class="flex items-center gap-2 text-orange-600">
            <Heart class="h-8 w-8 fill-current" />
            <h2 class="hidden text-xl font-bold tracking-tight text-slate-900 sm:block">志愿服务平台</h2>
          </RouterLink>
        </div>

        <!-- Desktop Navigation -->
        <nav class="hidden lg:flex items-center gap-8">
          <RouterLink
            v-for="link in navLinks"
            :key="link.path"
            :to="link.path"
            class="text-sm font-medium transition-colors hover:text-orange-600"
            :class="isActive(link.path) ? 'text-orange-600 border-b-2 border-orange-600 pb-1' : 'text-slate-600'"
          >
            {{ link.name }}
          </RouterLink>
        </nav>

        <div class="flex items-center gap-2 sm:gap-4">
          <!-- 已移除：无功能搜索框 -->
          <div class="relative">
            <button
              @click="isNotifOpen = !isNotifOpen"
              class="relative flex h-10 w-10 items-center justify-center rounded-xl bg-slate-100 text-slate-600 hover:bg-slate-200"
            >
              <Bell class="h-5 w-5" />
              <span v-if="hasUnread" class="absolute top-2.5 right-2.5 h-2 w-2 rounded-full bg-orange-600 border-2 border-white"></span>
            </button>

            <!-- Notifications Popover (优化移动端定位与宽度) -->
            <div
              v-if="isNotifOpen"
              class="absolute -right-16 sm:right-0 mt-2 w-[calc(100vw-2rem)] sm:w-80 max-w-[360px] bg-white rounded-2xl shadow-2xl border border-slate-200 overflow-hidden z-50 animate-in fade-in slide-in-from-top-2 duration-300"
            >
              <div class="p-4 border-b border-slate-100 flex justify-between items-center bg-slate-50">
                <h3 class="font-bold text-slate-900 text-sm">消息通知</h3>
                <button @click="markAllAsRead" class="text-xs text-orange-600 hover:text-orange-700 font-medium">全部已读</button>
              </div>
              <div class="max-h-80 overflow-y-auto">
                <div
                  v-for="notif in notifications"
                  :key="notif.id"
                  @click="markAsRead(notif)"
                  class="p-4 border-b border-slate-50 hover:bg-slate-50 transition-colors cursor-pointer"
                  :class="{ 'bg-orange-50/30': notif.unread }"
                >
                  <div class="flex justify-between items-start mb-1">
                    <h4 class="text-sm font-medium" :class="notif.unread ? 'text-slate-900' : 'text-slate-700'">
                      <span v-if="notif.unread" class="inline-block w-1.5 h-1.5 bg-orange-500 rounded-full mr-2 align-middle"></span>
                      {{ notif.title }}
                    </h4>
                    <span class="text-xs text-slate-400 shrink-0 ml-2">{{ notif.time }}</span>
                  </div>
                  <p class="text-xs text-slate-500 line-clamp-2 pl-3.5 mt-1">{{ notif.desc }}</p>
                </div>
              </div>
              <div @click="goToAllNotifications" class="p-3 text-center border-t border-slate-100 bg-slate-50 hover:bg-slate-100 cursor-pointer transition-colors">
                <button class="text-xs text-slate-600 font-medium">查看全部消息</button>
              </div>
            </div>
          </div>
          <button
            @click="isMobileMenuOpen = !isMobileMenuOpen"
            class="lg:hidden flex h-10 w-10 items-center justify-center rounded-xl bg-slate-100 text-slate-600 hover:bg-slate-200"
          >
            <Menu v-if="!isMobileMenuOpen" class="h-5 w-5" />
            <X v-else class="h-5 w-5" />
          </button>
          <div class="flex items-center gap-3">
            <span class="hidden sm:block text-sm font-medium text-slate-700">{{ userName }}</span>
            <RouterLink to="/volunteer/profile" class="h-10 w-10 overflow-hidden rounded-full border-2 border-orange-600/20 bg-slate-200">
              <img
                :src="userAvatar"
                alt="User profile"
                class="h-full w-full object-cover"
              />
            </RouterLink>
          </div>
        </div>
      </div>

      <!-- Mobile Navigation Menu -->
      <div
        v-if="isMobileMenuOpen"
        class="lg:hidden absolute top-full left-0 w-full bg-white border-b border-slate-200 shadow-lg py-4 px-4 flex flex-col gap-2"
      >
        <RouterLink
          v-for="link in navLinks"
          :key="link.path"
          :to="link.path"
          @click="isMobileMenuOpen = false"
          class="text-base font-medium px-4 py-3 rounded-xl transition-colors"
          :class="isActive(link.path) ? 'bg-orange-50 text-orange-600' : 'text-slate-600 hover:bg-slate-50'"
        >
          {{ link.name }}
        </RouterLink>
      </div>
    </header>

    <!-- Main Content -->
    <main class="flex-1">
      <RouterView />
    </main>

    <!-- Footer -->
    <footer class="mt-12 border-t border-slate-200 bg-white px-4 py-12">
      <div class="mx-auto max-w-7xl">
        <div class="grid grid-cols-2 gap-8 md:grid-cols-4 lg:grid-cols-5">
          <div class="col-span-2 lg:col-span-2">
            <div class="flex items-center gap-2 mb-4">
              <div class="flex h-8 w-8 items-center justify-center rounded-lg bg-orange-600 text-white">
                <Heart class="h-5 w-5 fill-current" />
              </div>
              <h2 class="text-lg font-bold tracking-tight text-slate-900">志愿服务平台</h2>
            </div>
            <p class="text-sm text-slate-500 max-w-xs">
              连接爱心，服务社会。致力于打造最专业、最高效的社区志愿服务沟通平台。
            </p>
            <div class="mt-6 flex gap-4">
              <a href="#" class="h-8 w-8 flex items-center justify-center rounded-full bg-slate-100 text-slate-600 hover:text-orange-600">
                <Globe class="h-4 w-4" />
              </a>
              <a href="#" class="h-8 w-8 flex items-center justify-center rounded-full bg-slate-100 text-slate-600 hover:text-orange-600">
                <Users class="h-4 w-4" />
              </a>
              <a href="#" class="h-8 w-8 flex items-center justify-center rounded-full bg-slate-100 text-slate-600 hover:text-orange-600">
                <Share2 class="h-4 w-4" />
              </a>
            </div>
          </div>

          <div>
            <h4 class="text-sm font-bold text-slate-900 mb-4">关于我们</h4>
            <ul class="space-y-2 text-sm text-slate-500">
              <li><button @click="openAbout('mission')" class="hover:text-orange-600 transition-colors">平台简介</button></li>
              <li><button @click="openAbout('policy')" class="hover:text-orange-600 transition-colors">志愿政策</button></li>
              <li><button @click="openAbout('join')" class="hover:text-orange-600 transition-colors">商务合作</button></li>
              <li><a href="mailto:support@volunteer.org" class="hover:text-orange-600 transition-colors">联系方式</a></li>
            </ul>
          </div>

          <div>
            <h4 class="text-sm font-bold text-slate-900 mb-4">帮助中心</h4>
            <ul class="space-y-2 text-sm text-slate-500">
              <li><button @click="openHelp('guide')" class="hover:text-orange-600 transition-colors">如何报名</button></li>
              <li><button @click="openHelp('rules')" class="hover:text-orange-600 transition-colors">积分规则</button></li>
              <li><button @click="openHelp('faq')" class="hover:text-orange-600 transition-colors">常见问题</button></li>
              <li><button @click="ElMessage.info('功能开发中，请关注后续更新')" class="hover:text-orange-600 transition-colors">用户反馈</button></li>
            </ul>
          </div>

          <div>
            <h4 class="text-sm font-bold text-slate-900 mb-4">联系我们</h4>
            <ul class="space-y-2 text-sm text-slate-500">
              <li class="flex items-center gap-2">
                <Phone class="h-4 w-4" />
                <a href="tel:4001234567" class="hover:text-orange-600 transition-colors">400-123-4567</a>
              </li>
              <li class="flex items-center gap-2">
                <Mail class="h-4 w-4" />
                <a href="mailto:support@volunteer.org" class="hover:text-orange-600 transition-colors">support@volunteer.org</a>
              </li>
            </ul>
          </div>
        </div>

        <div class="mt-12 border-t border-slate-100 pt-8">
          <p class="text-center text-xs text-slate-400">
            © 2024 社区志愿服务平台 版权所有。京 ICP 备 00000000 号
          </p>
        </div>
      </div>
    </footer>
    <!-- 📘 帮助中心弹窗 -->
    <div v-if="isHelpOpen" class="fixed inset-0 z-[100] flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-sm">
      <div class="bg-white rounded-3xl w-full max-w-2xl overflow-hidden shadow-2xl animate-in fade-in zoom-in duration-300 flex flex-col max-h-[85vh]">
        <!-- Header -->
        <div class="p-6 border-b border-slate-100 flex justify-between items-center bg-slate-50">
          <div class="flex items-center gap-3">
            <div class="w-10 h-10 rounded-xl bg-orange-600 text-white flex items-center justify-center shadow-lg shadow-orange-600/20">
              <Globe class="w-5 h-5" />
            </div>
            <div>
              <h3 class="font-bold text-slate-900 text-lg">志愿者帮助手册</h3>
              <p class="text-xs text-slate-500">解答您的疑惑，开启温暖之旅</p>
            </div>
          </div>
          <button @click="isHelpOpen = false" class="text-slate-400 hover:text-slate-600 p-2 bg-white rounded-full shadow-sm transition-all hover:rotate-90">
            <X class="h-5 w-5" />
          </button>
        </div>

        <!-- Sidebar / Tabs Content -->
        <div class="flex-1 flex overflow-hidden">
          <!-- Sidebar -->
          <div class="w-32 sm:w-40 bg-slate-50 border-r border-slate-100 p-2 space-y-1">
            <button 
              @click="helpTab = 'guide'" 
              class="w-full text-left px-4 py-3 rounded-xl text-sm font-bold transition-all"
              :class="helpTab === 'guide' ? 'bg-orange-600 text-white shadow-md' : 'text-slate-600 hover:bg-slate-200'"
            >
              🌱 参与指南
            </button>
            <button 
              @click="helpTab = 'rules'" 
              class="w-full text-left px-4 py-3 rounded-xl text-sm font-bold transition-all"
              :class="helpTab === 'rules' ? 'bg-orange-600 text-white shadow-md' : 'text-slate-600 hover:bg-slate-200'"
            >
              📈 等级体系
            </button>
            <button 
              @click="helpTab = 'faq'" 
              class="w-full text-left px-4 py-3 rounded-xl text-sm font-bold transition-all"
              :class="helpTab === 'faq' ? 'bg-orange-600 text-white shadow-md' : 'text-slate-600 hover:bg-slate-200'"
            >
              ❓ 常见问题
            </button>
          </div>

          <!-- Content Area -->
          <div class="flex-1 overflow-y-auto p-6 sm:p-8">
            <!-- 1. 参与指南 -->
            <div v-if="helpTab === 'guide'" class="space-y-6">
              <div class="flex gap-4">
                <div class="flex-shrink-0 w-8 h-8 rounded-full bg-orange-100 text-orange-600 flex items-center justify-center font-bold">1</div>
                <div>
                  <h4 class="font-bold text-slate-900 mb-1">发现活动</h4>
                  <p class="text-sm text-slate-500">在“志愿活动”频道浏览当前正在招募的项目，可按分类筛选感兴趣的领域。</p>
                </div>
              </div>
              <div class="flex gap-4">
                <div class="flex-shrink-0 w-8 h-8 rounded-full bg-orange-100 text-orange-600 flex items-center justify-center font-bold">2</div>
                <div>
                  <h4 class="font-bold text-slate-900 mb-1">报名申请</h4>
                  <p class="text-sm text-slate-500">点击活动卡片查看详情，符合要求后点击“立即报名”。管理员审核通过后您将收到通知。</p>
                </div>
              </div>
              <div class="flex gap-4">
                <div class="flex-shrink-0 w-8 h-8 rounded-full bg-orange-100 text-orange-600 flex items-center justify-center font-bold">3</div>
                <div>
                  <h4 class="font-bold text-slate-900 mb-1">扫码打卡</h4>
                  <p class="text-sm text-slate-500">活动现场使用个人中心的“扫码打卡”功能，完成签到与签退。这是计算工时的唯一依据。</p>
                </div>
              </div>
            </div>

            <!-- 2. 等级体系 -->
            <div v-if="helpTab === 'rules'" class="space-y-4">
              <div class="bg-orange-50 p-4 rounded-2xl border border-orange-100 mb-4">
                <p class="text-sm text-orange-700 font-medium">✨ 我们采用 V1-V5 荣誉勋章体系。每一次志愿服务都会为您积累积分（Points），助您晋升段位。</p>
              </div>
              <div class="space-y-3">
                <div class="flex items-center justify-between p-3 bg-white border border-slate-100 rounded-xl">
                  <span class="text-sm font-bold text-slate-700">🌱 V1 新星志愿者</span>
                  <span class="text-xs text-slate-400">初始段位</span>
                </div>
                <div class="flex items-center justify-between p-3 bg-white border border-slate-100 rounded-xl">
                  <span class="text-sm font-bold text-slate-700">🔥 V2 进阶志愿者</span>
                  <span class="text-xs font-bold text-orange-600">满 100 积分</span>
                </div>
                <div class="flex items-center justify-between p-3 bg-white border border-slate-100 rounded-xl">
                  <span class="text-sm font-bold text-slate-700">⚡ V3 资深志愿者</span>
                  <span class="text-xs font-bold text-orange-600">满 300 积分</span>
                </div>
                <div class="flex items-center justify-between p-3 bg-white border border-slate-100 rounded-xl">
                  <span class="text-sm font-bold text-slate-700">💎 V4 达人志愿者</span>
                  <span class="text-xs font-bold text-orange-600">满 600 积分</span>
                </div>
                <div class="flex items-center justify-between p-3 bg-white border border-slate-100 rounded-xl shadow-sm border-orange-200">
                  <span class="text-sm font-bold text-slate-900 font-serif">👑 V5 卓越志愿者</span>
                  <span class="text-xs font-bold text-orange-600">满 1000 积分</span>
                </div>
              </div>
            </div>

            <!-- 3. 常见问题 -->
            <div v-if="helpTab === 'faq'" class="space-y-5">
              <div class="group">
                <h5 class="text-sm font-bold text-slate-900 mb-1 flex items-center gap-2">
                  <div class="w-1.5 h-1.5 rounded-full bg-orange-600"></div>
                  忘记签退了怎么办？
                </h5>
                <p class="text-xs text-slate-500 leading-relaxed pl-3.5">请及时在公告栏找到该活动的负责人联系方式，说明情况后由管理员在后台为您手动录入工时。</p>
              </div>
              <div class="group">
                <h5 class="text-sm font-bold text-slate-900 mb-1 flex items-center gap-2">
                  <div class="w-1.5 h-1.5 rounded-full bg-orange-600"></div>
                  积分可以做什么？
                </h5>
                <p class="text-xs text-slate-500 leading-relaxed pl-3.5">积分不仅是荣誉段位的象征，还可以在“积分商城”中兑换生活用品、文创周边或服务激励。</p>
              </div>
              <div class="group">
                <h5 class="text-sm font-bold text-slate-900 mb-1 flex items-center gap-2">
                  <div class="w-1.5 h-1.5 rounded-full bg-orange-600"></div>
                  如何获得更多点赞？
                </h5>
                <p class="text-xs text-slate-500 leading-relaxed pl-3.5">用心完成社区居民发布的“微心愿”。当您帮助他们实现愿望后，居民有更高概率为您送上红心。</p>
              </div>
            </div>
          </div>
        </div>

        <!-- Footer -->
        <div class="p-4 border-t border-slate-100 bg-slate-50 shrink-0">
          <button @click="isHelpOpen = false" class="w-full py-3 bg-orange-600 text-white rounded-xl text-sm font-bold hover:bg-orange-700 transition-colors shadow-sm">
            我知道了
          </button>
        </div>
      </div>
    </div>

    <!-- 🏢 关于我们弹窗 -->
    <div v-if="isAboutOpen" class="fixed inset-0 z-[100] flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-sm">
      <div class="bg-white rounded-3xl w-full max-w-2xl overflow-hidden shadow-2xl animate-in fade-in zoom-in duration-300 flex flex-col max-h-[85vh]">
        <!-- Header -->
        <div class="p-6 border-b border-slate-100 flex justify-between items-center bg-slate-50">
          <div class="flex items-center gap-3">
            <div class="w-10 h-10 rounded-xl bg-orange-600 text-white flex items-center justify-center shadow-lg shadow-orange-600/20">
              <Heart class="w-5 h-5 fill-current" />
            </div>
            <div>
              <h3 class="font-bold text-slate-900 text-lg">了解我们的使命</h3>
              <p class="text-xs text-slate-500">汇聚微光，点亮温暖社区</p>
            </div>
          </div>
          <button @click="isAboutOpen = false" class="text-slate-400 hover:text-slate-600 p-2 bg-white rounded-full shadow-sm">
            <X class="h-5 w-5" />
          </button>
        </div>

        <div class="flex-1 flex overflow-hidden">
          <!-- Sidebar -->
          <div class="w-32 sm:w-40 bg-slate-50 border-r border-slate-100 p-2 space-y-1">
            <button 
              @click="aboutTab = 'mission'" 
              class="w-full text-left px-4 py-3 rounded-xl text-sm font-bold transition-all"
              :class="aboutTab === 'mission' ? 'bg-orange-600 text-white shadow-md' : 'text-slate-600 hover:bg-slate-200'"
            >
              🏠 平台简介
            </button>
            <button 
              @click="aboutTab = 'policy'" 
              class="w-full text-left px-4 py-3 rounded-xl text-sm font-bold transition-all"
              :class="aboutTab === 'policy' ? 'bg-orange-600 text-white shadow-md' : 'text-slate-600 hover:bg-slate-200'"
            >
              📜 志愿政策
            </button>
            <button 
              @click="aboutTab = 'join'" 
              class="w-full text-left px-4 py-3 rounded-xl text-sm font-bold transition-all"
              :class="aboutTab === 'join' ? 'bg-orange-600 text-white shadow-md' : 'text-slate-600 hover:bg-slate-200'"
            >
              🤝 商务合作
            </button>
          </div>

          <!-- Content -->
          <div class="flex-1 overflow-y-auto p-6 sm:p-8">
            <!-- 1. 平台简介 -->
            <div v-if="aboutTab === 'mission'" class="space-y-6">
              <div class="prose prose-slate prose-sm text-slate-600 leading-relaxed">
                <p class="font-bold text-slate-900 text-lg mb-4">让爱心更有力量，让志愿更有温度。</p>
                <p>我们致力于构建一个透明、高效的社区志愿服务生态系统。通过数字技术连接社会资源与公益需求，解决社区治理中的“最后一公里”。</p>
                <div class="grid grid-cols-2 gap-4 mt-6">
                  <div class="p-4 bg-orange-50 rounded-2xl border border-orange-100">
                    <div class="text-xl font-bold text-orange-600 mb-1">99%</div>
                    <div class="text-[10px] text-orange-700">服务满意率</div>
                  </div>
                  <div class="p-4 bg-orange-50 rounded-2xl border border-orange-100">
                    <div class="text-xl font-bold text-orange-600 mb-1">10k+</div>
                    <div class="text-[10px] text-orange-700">注册志愿者</div>
                  </div>
                </div>
              </div>
            </div>

            <!-- 2. 志愿政策 -->
            <div v-if="aboutTab === 'policy'" class="space-y-4">
              <h4 class="font-bold text-slate-900">官方认定与保障</h4>
              <p class="text-sm text-slate-500 leading-relaxed">平台严格遵守《志愿服务条例》。您在这里的每一分钟服务时长都将记录在案，作为个人社会信用的重要组成部分。</p>
              <ul class="space-y-2">
                <li class="flex items-start gap-2 text-xs text-slate-600">
                  <span class="text-orange-500 mt-0.5">●</span>
                  志愿服务时长公开、公平、公正核算。
                </li>
                <li class="flex items-start gap-2 text-xs text-slate-600">
                  <span class="text-orange-500 mt-0.5">●</span>
                  积分体系旨在激励公益行为，非法获取积分将受限。
                </li>
                <li class="flex items-start gap-2 text-xs text-slate-600">
                  <span class="text-orange-500 mt-0.5">●</span>
                  涉及线下活动，建议志愿者完善人身意外保险。
                </li>
              </ul>
            </div>

            <!-- 3. 商务合作 -->
            <div v-if="aboutTab === 'join'" class="space-y-6">
              <div class="p-4 bg-orange-50 rounded-2xl border border-orange-100">
                <h4 class="font-bold text-orange-900 mb-2">发布活动入驻</h4>
                <p class="text-xs text-orange-700">如果您是学校团委、企业公益部或非营利组织，欢迎申请发布者账号，共同发起有意义的社会实践项目。</p>
              </div>
              <div class="p-4 bg-slate-50 rounded-2xl border border-slate-100">
                <h4 class="font-bold text-slate-900 mb-2">积分商城赞助</h4>
                <p class="text-xs text-slate-500">欢迎爱心企业提供实物奖品或服务卡券，我们将为您提供品牌展示位，共同回馈热心志愿者。</p>
              </div>
            </div>
          </div>
        </div>

        <div class="p-4 border-t border-slate-100 bg-slate-50 shrink-0 text-center">
          <p class="text-[10px] text-slate-400 mb-4">如有更多疑问，欢迎通过下方电话联系我们</p>
          <button @click="isAboutOpen = false" class="w-full py-3 bg-orange-600 text-white rounded-xl text-sm font-bold hover:bg-orange-700 transition-colors">
            返回
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
