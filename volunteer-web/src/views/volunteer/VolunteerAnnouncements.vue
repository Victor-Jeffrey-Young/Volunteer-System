<script setup>
import { ref, onMounted } from 'vue';
import { announcementApi } from '../../api/modules';
import { Calendar, ChevronRight, Bell, X } from 'lucide-vue-next';

const selectedAnnouncement = ref(null);
const announcements = ref([]);
const loading = ref(false);

const fetchData = async () => {
  loading.value = true;
  try {
    const res = await announcementApi.getAnnouncements(1, 20);
    announcements.value = res.data?.records || [];
  } catch (error) {
    console.error("Fetch announcements error:", error);
  } finally {
    loading.value = false;
  }
};

onMounted(fetchData);
</script>

<template>
  <div class="mx-auto max-w-4xl px-4 sm:px-6 lg:px-8 py-8">
    <div class="mb-8">
      <h1 class="text-2xl font-bold text-slate-900 flex items-center gap-2">
        <Bell class="h-6 w-6 text-orange-500" />
        公告记录
      </h1>
      <p class="text-sm text-slate-500 mt-1">查看平台的所有通知与公告信息</p>
    </div>

    <div class="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
      <div class="divide-y divide-slate-100">
        <button
          v-for="item in announcements"
          :key="item.noticeId"
          @click="selectedAnnouncement = item"
          class="w-full text-left p-6 hover:bg-slate-50 transition-colors group flex items-start gap-4"
        >
          <div class="mt-1 bg-orange-100 text-orange-600 p-2 rounded-xl shrink-0 group-hover:bg-orange-500 group-hover:text-white transition-colors">
            <Bell class="w-5 h-5" />
          </div>
          <div class="flex-1">
            <div class="flex items-start justify-between gap-4 mb-2">
              <h3 class="text-lg font-bold text-slate-900 group-hover:text-orange-600 transition-colors">
                <span v-if="item.createTime && new Date(item.createTime) > new Date(Date.now() - 86400000 * 3)" class="inline-block bg-red-500 text-white text-[10px] px-2 py-0.5 rounded-full mr-2 font-bold align-middle">NEW</span>
                {{ item.title }}
              </h3>
              <ChevronRight class="w-5 h-5 text-slate-400 shrink-0 mt-1 group-hover:text-orange-500 transition-colors" />
            </div>
            <p class="text-sm text-slate-500 line-clamp-2 mb-3">
              {{ item.content }}
            </p>
            <div class="flex items-center gap-4 text-xs text-slate-400">
              <div class="flex items-center gap-1">
                <Calendar class="w-3.5 h-3.5" />
                {{ item.createTime ? item.createTime.substring(0, 10) : '未知时间' }}
              </div>
              <div>发布者：{{ item.publisherName || '系统管理员' }}</div>
            </div>
          </div>
        </button>
      </div>
    </div>

    <!-- Pagination -->
    <div class="mt-8 flex justify-center">
      <nav class="flex items-center gap-2">
        <button class="h-10 w-10 flex items-center justify-center rounded-xl border border-slate-200 text-slate-500 hover:bg-slate-50 disabled:opacity-50" disabled>
          &lt;
        </button>
        <button class="h-10 w-10 flex items-center justify-center rounded-xl bg-orange-600 text-white font-medium">
          1
        </button>
        <button class="h-10 w-10 flex items-center justify-center rounded-xl border border-slate-200 text-slate-700 hover:bg-slate-50 font-medium">
          2
        </button>
        <button class="h-10 w-10 flex items-center justify-center rounded-xl border border-slate-200 text-slate-500 hover:bg-slate-50">
          &gt;
        </button>
      </nav>
    </div>

    <!-- Announcement Detail Modal -->
    <div
      v-if="selectedAnnouncement"
      class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/50 backdrop-blur-sm"
    >
      <div class="bg-white rounded-3xl w-full max-w-2xl overflow-hidden shadow-2xl animate-in fade-in zoom-in duration-200 flex flex-col max-h-[90vh]">
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
              {{ selectedAnnouncement.createTime ? selectedAnnouncement.createTime.substring(0, 10) : '未知时间' }}
            </div>
            <div>发布者：{{ selectedAnnouncement.publisherName || '系统管理员' }}</div>
          </div>
          <div class="prose prose-slate prose-orange max-w-none text-slate-700 leading-relaxed">
            <p>{{ selectedAnnouncement.content }}</p>
          </div>
        </div>
        <div class="p-4 border-t border-slate-100 bg-slate-50 shrink-0">
          <button
            @click="selectedAnnouncement = null"
            class="w-full sm:w-auto sm:px-8 py-2.5 bg-orange-600 text-white rounded-xl text-sm font-bold hover:bg-orange-700 transition-colors shadow-sm mx-auto block"
          >
            我知道了
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
