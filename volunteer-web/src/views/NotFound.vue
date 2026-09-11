<template>
  <div class="min-h-screen flex flex-col bg-slate-50 text-slate-900 font-sans">
    <!-- Header -->
    <header class="w-full bg-white border-b border-slate-200 sticky top-0 z-50">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="flex items-center justify-between h-16">
          <div class="flex items-center gap-3">
            <div class="bg-orange-600 p-1.5 rounded-lg flex items-center justify-center">
              <Heart class="w-6 h-6 text-white fill-current" />
            </div>
            <h2 class="text-xl font-bold tracking-tight text-slate-900">社区志愿服务平台</h2>
          </div>
          <div>
            <span class="text-sm text-slate-500 mr-4 hidden sm:inline">连接爱心，服务社会</span>
          </div>
        </div>
      </div>
    </header>

    <main class="flex-grow flex items-center justify-center p-4 sm:p-6 lg:p-8 relative overflow-hidden">
      <!-- 背景装饰光晕 -->
      <div class="absolute inset-0 z-0 opacity-10 pointer-events-none">
        <div class="absolute top-[-10%] right-[-10%] w-[500px] h-[500px] bg-orange-600 rounded-full blur-[120px]"></div>
        <div class="absolute bottom-[-10%] left-[-10%] w-[500px] h-[500px] bg-orange-600/40 rounded-full blur-[120px]"></div>
      </div>

      <div class="w-full max-w-xl bg-white rounded-3xl shadow-2xl p-8 sm:p-12 text-center relative z-10">
        <div class="inline-flex items-center justify-center h-20 w-20 rounded-full bg-orange-100 text-orange-600 mb-6">
          <Compass class="h-10 w-10" />
        </div>
        <p class="text-5xl font-black tracking-tighter text-orange-600 mb-4">404</p>
        <h1 class="text-2xl font-bold text-slate-900 mb-3">页面走丢了</h1>
        <p class="text-slate-500 leading-relaxed mb-8">
          抱歉，您访问的页面不存在或已被移除。<br class="hidden sm:block" />
          请检查地址是否正确，或返回首页继续您的公益之旅。
        </p>
        <div class="flex flex-col sm:flex-row items-center justify-center gap-3">
          <button
            @click="goHome"
            class="w-full sm:w-auto inline-flex items-center justify-center gap-2 rounded-xl bg-orange-600 px-8 py-3 text-sm font-bold text-white shadow-lg shadow-orange-600/30 hover:bg-orange-700 transition-all active:scale-[0.98]"
          >
            <Home class="w-4 h-4" />
            返回首页
          </button>
          <RouterLink
            to="/login"
            class="w-full sm:w-auto inline-flex items-center justify-center rounded-xl border border-slate-200 bg-white px-8 py-3 text-sm font-bold text-slate-600 hover:bg-slate-50 transition-colors"
          >
            返回登录页
          </RouterLink>
        </div>
      </div>
    </main>

    <footer class="py-8 bg-white border-t border-slate-200">
      <div class="max-w-7xl mx-auto px-4 text-center text-slate-500 text-sm">
        <p>© 2026 社区志愿服务管理系统。让爱心传递，让温暖常在。</p>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { useRouter, RouterLink } from 'vue-router';
import { useUserStore } from '../stores/user';
import { Compass, Heart, Home } from 'lucide-vue-next';
import { roleHome } from '../router';

const router = useRouter();
const userStore = useUserStore();

// 未登录时统一回登录页，已登录则回各自角色首页
const goHome = () => {
  router.push(roleHome(userStore.role));
};
</script>
