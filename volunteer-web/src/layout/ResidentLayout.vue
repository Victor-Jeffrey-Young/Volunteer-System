<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRoute, useRouter, RouterLink } from 'vue-router';
import { getFullAvatar } from '../utils/file';
import { useUserStore } from '../stores/user';
import { Heart, Menu, X, Home, HeartHandshake, User, LogOut } from 'lucide-vue-next';

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();

const userAvatar = computed(() => getFullAvatar(userStore.user?.avatar || localStorage.getItem('avatar')));
const userName = computed(() => userStore.user?.realName || localStorage.getItem('realName') || '社区居民');

const navLinks = [
  { name: '首页', path: '/resident/home', icon: Home },
  { name: '微心愿管理', path: '/resident/wishes', icon: HeartHandshake },
  { name: '账号设置', path: '/resident/profile', icon: User },
];

const isActive = (path) => route.path.includes(path);

const handleLogout = () => {
    userStore.logout();
    router.push('/login');
};

onMounted(() => {
    userStore.fetchCurrentUser();
});
</script>

<template>
  <div class="min-h-screen flex flex-col bg-slate-50 text-slate-900 font-sans">
    <!-- Header -->
    <header class="sticky top-0 z-50 w-full border-b border-slate-200 bg-white/80 backdrop-blur-md">
      <div class="mx-auto flex max-w-7xl items-center justify-between px-4 py-3 sm:px-6 lg:px-8">
        <div class="flex items-center gap-2 sm:gap-4">
          <RouterLink to="/resident/home" class="flex items-center gap-2 text-orange-600">
            <Heart class="h-8 w-8 fill-current" />
            <h2 class="hidden text-xl font-bold tracking-tight text-slate-900 sm:block">社区志愿服务平台 · 居民</h2>
          </RouterLink>
        </div>

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
          <!-- PC端退出 -->
          <button @click="handleLogout" class="hidden lg:block text-xs text-slate-400 hover:text-orange-600 transition-colors">退出登录</button>
          <div class="flex items-center gap-3">
            <span class="hidden lg:block text-sm font-medium text-slate-700">{{ userName }}</span>
            <div class="h-10 w-10 overflow-hidden rounded-full border-2 border-orange-600/20 bg-slate-200">
              <img :src="userAvatar" alt="User profile" class="h-full w-full object-cover" />
            </div>
          </div>
        </div>
      </div>
    </header>

    <!-- Main Content -->
    <main class="flex-1 pb-20 lg:pb-0">
      <RouterView />
    </main>

    <footer class="mt-auto border-t border-slate-200 bg-white px-4 py-8 hidden lg:block">
      <p class="text-center text-xs text-slate-400">© 2026 邻里互助平台 · 竭诚为您服务</p>
    </footer>

    <!-- App-style Bottom Navigation for Mobile -->
    <nav class="lg:hidden fixed bottom-0 w-full bg-white/90 backdrop-blur-xl border-t border-slate-200 flex items-center justify-around z-50 px-2 py-2 pb-safe shadow-[0_-4px_10px_rgba(0,0,0,0.05)]">
      <RouterLink
        v-for="link in navLinks"
        :key="link.path"
        :to="link.path"
        class="flex flex-col items-center gap-1.5 p-2 w-full transition-colors active:scale-95"
        :class="isActive(link.path) ? 'text-orange-600' : 'text-slate-400 hover:text-slate-600'"
      >
        <component :is="link.icon" class="w-6 h-6" :class="isActive(link.path) ? 'fill-orange-100' : ''" />
        <span class="text-[10px] font-bold tracking-wide">{{ link.name }}</span>
      </RouterLink>
    </nav>

  </div>
</template>
