<script setup>
import { ref, computed, onMounted, watch } from 'vue';
import { mallApi } from '../../api/modules';
import { useUserStore } from '../../stores/user';
import { ElMessage } from 'element-plus';
import QRCode from 'qrcode'; // 🚨 切换为更兼容的底层原生库
import {
  Star,
  ShoppingBag,
  X,
  CheckCircle,
  QrCode,
} from 'lucide-vue-next';

const userStore = useUserStore();
const userPoints = computed(() => userStore.user?.currentPoints || 0);
const showHistory = ref(false);
const showRules = ref(false);
const activeCategory = ref('全部');
const sortBy = ref('默认');
const redeemSuccessItem = ref(null);
const currentRedeemCode = ref('');
const qrCodeDataUrl = ref(''); // 🚨 二维码图片 Base64
const selectedQRCode = ref(null);
const items = ref([]);
const exchangeRecords = ref([]);
const loading = ref(false);

const categories = ['全部', '生活用品', '纪念品', '电子产品', '文具', '虚拟卡券','玩具','游戏'];

// 🚨 二维码生成逻辑
const generateQR = async (text) => {
  if (!text) return;
  try {
    qrCodeDataUrl.value = await QRCode.toDataURL(text, {
      width: 300,
      margin: 2,
      color: { dark: '#0f172a', light: '#ffffff' }
    });
  } catch (err) {
    console.error('二维码生成失败:', err);
  }
};

const filteredItems = computed(() => {
  if (!items.value || !Array.isArray(items.value)) return [];
  let result = [...items.value];
  
  if (activeCategory.value !== '全部') {
    result = result.filter(item => item.category === activeCategory.value);
  }

  if (sortBy.value === '积分从低到高') {
    result.sort((a, b) => (a.pointsRequired || 0) - (b.pointsRequired || 0));
  } else if (sortBy.value === '积分从高到低') {
    result.sort((a, b) => (b.pointsRequired || 0) - (a.pointsRequired || 0));
  }
  return result;
});

const fetchData = async () => {
  loading.value = true;
  try {
    const res = await mallApi.getProducts();
    items.value = res.data?.records || [];
  } catch (error) {
    console.error("Fetch mall data error:", error);
  } finally {
    loading.value = false;
  }
};

const fetchHistory = async () => {
  if (!userStore.userId) return;
  try {
    const res = await mallApi.getExchangeRecords(userStore.userId);
    const allRecords = res.data?.records || [];
    exchangeRecords.value = allRecords.filter(r => String(r.userId) === String(userStore.userId));
  } catch (error) {
    console.error("Fetch history error:", error);
  }
};

onMounted(fetchData);

watch(showHistory, (newVal) => {
  if (newVal) fetchHistory();
});

// 🚨 监听核销码变化并生成对应二维码
watch(currentRedeemCode, (newVal) => {
  if (newVal) generateQR(newVal);
});

watch(selectedQRCode, (newVal) => {
  if (newVal) generateQR(newVal);
});

const handleRedeem = async (item) => {
  if (userPoints.value < item.pointsRequired) {
    return ElMessage.error('积分不足');
  }

  try {
    const res = await mallApi.exchange(userStore.userId, item.goodsId);
    const msg = res.data || '';
    const match = msg.match(/\[(.*?)\]/);
    currentRedeemCode.value = match ? match[1] : 'ERROR';

    redeemSuccessItem.value = item;
    fetchData(); 
    userStore.fetchCurrentUser();
  } catch (error) {
    console.error("Redeem error:", error);
  }
};
</script>

<template>
  <div class="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8 py-8">
    <!-- Header & Points Balance -->
    <div class="grid lg:grid-cols-3 gap-8 mb-12">
      <div class="lg:col-span-2">
        <h1 class="text-3xl font-bold text-slate-900 mb-4">积分商城</h1>
        <p class="text-slate-600 max-w-2xl">
          感谢您的辛勤付出！在这里，您可以使用参与志愿服务获得的积分兑换精美礼品。每一分都是对您爱心的肯定。
        </p>
      </div>

      <div class="bg-gradient-to-br from-orange-500 to-red-500 rounded-2xl p-6 text-white shadow-xl shadow-orange-500/20 relative overflow-hidden flex flex-col justify-center">
        <div class="absolute top-0 right-0 -mt-8 -mr-8 w-32 h-32 bg-white/10 rounded-full blur-2xl"></div>
        <div class="relative z-10 flex items-center justify-between">
          <div>
            <p class="text-orange-100 text-sm font-medium mb-1">当前可用积分</p>
            <div class="text-4xl font-bold flex items-baseline gap-2">
              {{ userPoints }} <span class="text-lg font-normal text-orange-200">分</span>
            </div>
          </div>
          <div class="h-16 w-16 bg-white/20 rounded-full flex items-center justify-center backdrop-blur-sm border border-white/30">
            <Star class="h-8 w-8 text-white fill-current" />
          </div>
        </div>
        <div class="relative z-10 mt-6 flex gap-3">
          <button
            @click="showHistory = true"
            class="flex-1 bg-white text-orange-600 rounded-xl py-2.5 text-sm font-bold hover:bg-orange-50 transition-colors shadow-sm"
          >
            兑换记录
          </button>
          <button
            @click="showRules = true"
            class="flex-1 bg-orange-600/20 border border-white/30 text-white rounded-xl py-2.5 text-sm font-bold hover:bg-orange-600/30 transition-colors backdrop-blur-sm"
          >
            如何获取积分
          </button>
        </div>
      </div>
    </div>

    <!-- Categories & Filters -->
    <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-8">
      <div class="flex overflow-x-auto pb-2 -mx-4 px-4 sm:mx-0 sm:px-0 sm:pb-0 hide-scrollbar gap-2">
        <button
          v-for="category in categories"
          :key="category"
          @click="activeCategory = category"
          class="whitespace-nowrap rounded-full px-5 py-2 text-sm font-medium transition-colors"
          :class="activeCategory === category ? 'bg-slate-900 text-white shadow-sm' : 'bg-white text-slate-600 hover:bg-slate-100 border border-slate-200'"
        >
          {{ category }}
        </button>
      </div>

      <div class="flex items-center gap-4 text-sm text-slate-600 bg-white px-4 py-2 rounded-xl border border-slate-200 shadow-sm self-start sm:self-auto">
        <span class="font-medium text-slate-900">排序：</span>
        <button
          @click="sortBy = '默认'"
          class="font-medium transition-colors"
          :class="sortBy === '默认' ? 'text-orange-600' : 'hover:text-orange-600'"
        >
          默认
        </button>
        <div class="w-px h-4 bg-slate-200"></div>
        <button
          @click="sortBy = '积分从低到高'"
          class="font-medium transition-colors"
          :class="sortBy === '积分从低到高' ? 'text-orange-600' : 'hover:text-orange-600'"
        >
          积分从低到高
        </button>
        <div class="w-px h-4 bg-slate-200"></div>
        <button
          @click="sortBy = '积分从高到低'"
          class="font-medium transition-colors"
          :class="sortBy === '积分从高到低' ? 'text-orange-600' : 'hover:text-orange-600'"
        >
          积分从高到低
        </button>
      </div>
    </div>

    <!-- Items Grid -->
    <div v-if="loading" class="flex justify-center py-20">
      <div class="animate-spin rounded-full h-12 w-12 border-4 border-orange-600 border-t-transparent"></div>
    </div>
    <div v-else class="grid sm:grid-cols-2 lg:grid-cols-4 gap-6">
      <div
        v-for="item in filteredItems"
        :key="item.goodsId"
        class="group flex flex-col rounded-2xl bg-white border border-slate-200 overflow-hidden hover:shadow-xl hover:shadow-slate-200/50 transition-all duration-300"
      >
        <div class="relative aspect-square overflow-hidden bg-slate-50 flex items-center justify-center p-6 group-hover:bg-slate-100 transition-colors duration-300">
          <img
            :src="item.image || 'https://images.pexels.com/photos/6646918/pexels-photo-6646918.jpeg?auto=compress&cs=tinysrgb&w=800'"
            :alt="item.name"
            class="h-full w-full object-contain transition-transform duration-500 group-hover:scale-110 drop-shadow-md mix-blend-multiply"
          />
          <div v-if="item.isHot" class="absolute top-3 left-3 inline-flex items-center rounded-full bg-red-500 px-2.5 py-0.5 text-xs font-bold text-white shadow-sm">
            热门兑换
          </div>
          <div v-if="item.category" class="absolute top-3 right-3 inline-flex items-center rounded-full bg-slate-100/80 backdrop-blur-sm px-2.5 py-0.5 text-[10px] font-bold text-slate-600 shadow-sm border border-slate-200">
            {{ item.category }}
          </div>
        </div>

        <div class="flex flex-col flex-1 p-5">
          <h3 class="text-base font-bold text-slate-900 mb-1 group-hover:text-orange-600 transition-colors line-clamp-2">
            {{ item.name }}
          </h3>
          <p class="text-[11px] text-slate-400 mb-3 line-clamp-2 min-h-[32px] leading-relaxed">
            {{ item.description || '暂无商品描述' }}
          </p>
          <p class="text-xs text-slate-500 mb-4">
            剩余库存：{{ item.stock }}件
          </p>

          <div class="mt-auto flex items-end justify-between">
            <div class="flex items-baseline gap-1 text-orange-600">
              <span class="text-2xl font-bold leading-none">{{ item.pointsRequired }}</span>
              <span class="text-xs font-medium">积分</span>
            </div>

            <button
              @click="handleRedeem(item)"
              class="flex items-center justify-center h-10 w-10 rounded-xl transition-colors"
              :class="userPoints >= item.pointsRequired ? 'bg-orange-50 text-orange-600 hover:bg-orange-600 hover:text-white' : 'bg-slate-100 text-slate-400 cursor-not-allowed'"
              :disabled="userPoints < item.pointsRequired"
            >
              <ShoppingBag class="h-5 w-5" />
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- History Modal -->
    <div
      v-if="showHistory"
      class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/50 backdrop-blur-sm"
    >
      <div class="bg-white rounded-2xl w-full max-w-md overflow-hidden shadow-2xl animate-in fade-in zoom-in duration-200">
        <div class="p-4 border-b border-slate-100 flex justify-between items-center bg-slate-50">
          <h3 class="font-bold text-slate-900">兑换记录</h3>
          <button @click="showHistory = false" class="text-slate-400 hover:text-slate-600 p-1">
            <X class="h-5 w-5" />
          </button>
        </div>
        <div class="p-4 max-h-[60vh] overflow-y-auto space-y-4">
          <div v-if="exchangeRecords.length === 0" class="text-center py-8 text-slate-400 text-sm">
            暂无兑换记录
          </div>
          <div 
            v-for="record in exchangeRecords" 
            :key="record.recordId"
            class="flex gap-4 items-start border-b border-slate-50 pb-4"
          >
            <img :src="record.goodsImage || 'https://images.pexels.com/photos/6646918/pexels-photo-6646918.jpeg?auto=compress&cs=tinysrgb&w=150'" alt="item" class="w-16 h-16 rounded-xl object-cover shrink-0 bg-slate-100" />
            <div class="flex-1">
              <div class="flex justify-between items-start">
                <h4 class="font-bold text-sm text-slate-900">{{ record.goodsName }}</h4>
                <div class="text-orange-600 font-bold text-sm">-{{ record.costPoints }} 积分</div>
              </div>
              <p class="text-xs text-slate-500 mt-1">{{ record.createTime ? record.createTime.substring(0, 16) : '未知时间' }}</p>
              <div class="mt-2 flex items-center justify-between bg-slate-50 p-2 rounded-lg border border-slate-100">
                <div class="text-xs font-mono text-slate-700">兑换码：<span class="font-bold">{{ record.redeemCode }}</span></div>
                <button @click="selectedQRCode = record.redeemCode" class="text-orange-600 hover:text-orange-700 p-1">
                  <QrCode class="w-4 h-4" />
                </button>
              </div>
              <div class="mt-1">
                <span 
                  class="text-[10px] px-1.5 py-0.5 rounded-full font-bold"
                  :class="record.status === 1 ? 'bg-emerald-100 text-emerald-700' : 'bg-orange-100 text-orange-700'"
                >
                  {{ record.status === 1 ? '已领取' : '待核销' }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Rules Modal -->
    <div
      v-if="showRules"
      class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/50 backdrop-blur-sm"
    >
      <div class="bg-white rounded-2xl w-full max-w-md overflow-hidden shadow-2xl animate-in fade-in zoom-in duration-200">
        <div class="p-4 border-b border-slate-100 flex justify-between items-center bg-slate-50">
          <h3 class="font-bold text-slate-900">积分获取细则</h3>
          <button @click="showRules = false" class="text-slate-400 hover:text-slate-600 p-1">
            <X class="h-5 w-5" />
          </button>
        </div>
        <div class="p-6 max-h-[60vh] overflow-y-auto space-y-6">
          <div>
            <h4 class="font-bold text-slate-900 text-sm mb-2 flex items-center gap-2">
              <div class="w-6 h-6 rounded-full bg-orange-100 text-orange-600 flex items-center justify-center text-xs">1</div>
              参与志愿活动 (核心途径)
            </h4>
            <p class="text-sm text-slate-600 pl-8 leading-relaxed">
              这是获取积分的最主要方式。每完成 1 小时经过验证的志愿服务时长，系统将为您发放 10 积分。
            </p>
          </div>
          <div>
            <h4 class="font-bold text-slate-900 text-sm mb-2 flex items-center gap-2">
              <div class="w-6 h-6 rounded-full bg-orange-100 text-orange-600 flex items-center justify-center text-xs">2</div>
              自动结算机制
            </h4>
            <p class="text-sm text-slate-600 pl-8 leading-relaxed">
              活动结束后，请务必配合现场签退。当管理员在后台完成“活动结算”并确认您的服务时长后，积分将自动发放至您的账户。
            </p>
          </div>
          <div>
            <h4 class="font-bold text-slate-900 text-sm mb-2 flex items-center gap-2">
              <div class="w-6 h-6 rounded-full bg-orange-100 text-orange-600 flex items-center justify-center text-xs">3</div>
              积分双轨制
            </h4>
            <p class="text-sm text-slate-600 pl-8 leading-relaxed">
              您获得的每一分都会同时增加您的 "总积分"（用于荣誉排名和等级提升）和 "可用积分"（用于在商城兑换礼品）。
            </p>
          </div>
          <div class="bg-blue-50 p-4 rounded-xl border border-blue-100">
            <p class="text-xs text-blue-700 leading-relaxed">
              <strong>💡 温馨提示：</strong>商城兑换只会扣除您的“可用积分”，您的全站荣誉排名和志愿等级不会因为兑换礼品而下降。
            </p>
          </div>
        </div>
        <div class="p-4 border-t border-slate-100 bg-slate-50">
          <button
            @click="showRules = false"
            class="w-full bg-orange-600 text-white rounded-xl py-2.5 text-sm font-bold hover:bg-orange-700 transition-colors"
          >
            我知道了
          </button>
        </div>
      </div>
    </div>

    <!-- Redeem Success Modal -->
    <div
      v-if="redeemSuccessItem"
      class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/50 backdrop-blur-sm"
    >
      <div class="bg-white rounded-3xl w-full max-w-sm overflow-hidden shadow-2xl animate-in fade-in zoom-in duration-200 text-center relative">
        <button
          @click="redeemSuccessItem = null"
          class="absolute top-4 right-4 text-slate-400 hover:text-slate-600 p-1 bg-slate-100 rounded-full"
        >
          <X class="h-5 w-5" />
        </button>

        <div class="p-8 pt-10">
          <div class="w-20 h-20 bg-emerald-100 rounded-full flex items-center justify-center mx-auto mb-6">
            <CheckCircle class="w-10 h-10 text-emerald-500" />
          </div>
          <h3 class="text-2xl font-bold text-slate-900 mb-2">兑换成功！</h3>
          <p class="text-sm text-slate-500 mb-6">
            您已成功兑换 <span class="font-bold text-slate-900">{{ redeemSuccessItem.name }}</span>
          </p>

          <div class="bg-slate-50 rounded-2xl p-4 border border-slate-100 mb-6">
            <p class="text-xs text-slate-500 mb-2">您的专属兑换码</p>
            <div class="text-xl font-mono font-bold text-slate-900 tracking-widest mb-3">
              {{ currentRedeemCode }}
            </div>
            <div class="w-32 h-32 bg-white mx-auto rounded-xl border border-slate-200 flex items-center justify-center overflow-hidden">
              <!-- 🚨 替换为真实的 <img> 显示 Base64 二维码 -->
              <img v-if="qrCodeDataUrl" :src="qrCodeDataUrl" class="w-24 h-24" alt="QR Code" />
            </div>
            <p class="text-[10px] text-slate-400 mt-3">请在服务中心出示此二维码核销</p>
          </div>

          <button
            @click="redeemSuccessItem = null"
            class="w-full bg-orange-600 text-white rounded-xl py-3 text-sm font-bold hover:bg-orange-700 transition-colors shadow-lg shadow-orange-600/30"
          >
            完成
          </button>
        </div>
      </div>
    </div>

    <!-- QR Code Modal for History -->
    <div
      v-if="selectedQRCode"
      class="fixed inset-0 z-[60] flex items-center justify-center p-4 bg-slate-900/50 backdrop-blur-sm"
    >
      <div class="bg-white rounded-3xl w-full max-w-sm overflow-hidden shadow-2xl animate-in fade-in zoom-in duration-200 text-center relative">
        <button
          @click="selectedQRCode = null"
          class="absolute top-4 right-4 text-slate-400 hover:text-slate-600 p-1 bg-slate-100 rounded-full"
        >
          <X class="h-5 w-5" />
        </button>
        <div class="p-8 pt-12">
          <h3 class="text-xl font-bold text-slate-900 mb-2">兑换二维码</h3>
          <p class="text-sm text-slate-500 mb-6">请在服务中心出示此二维码核销</p>
          <div class="bg-slate-50 rounded-2xl p-6 border border-slate-100 mb-6">
            <div class="w-48 h-48 bg-white mx-auto rounded-xl border border-slate-200 flex items-center justify-center mb-4 overflow-hidden">
              <!-- 🚨 替换为真实的 <img> 显示 Base64 二维码 -->
              <img v-if="qrCodeDataUrl" :src="qrCodeDataUrl" class="w-36 h-36" alt="QR Code" />
            </div>
            <div class="text-lg font-mono font-bold text-slate-900 tracking-widest">
              {{ selectedQRCode }}
            </div>
          </div>
          <button
            @click="selectedQRCode = null"
            class="w-full bg-orange-600 text-white rounded-xl py-3 text-sm font-bold hover:bg-orange-700 transition-colors shadow-lg shadow-orange-600/30"
          >
            关闭
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.hide-scrollbar::-webkit-scrollbar {
  display: none;
}
.hide-scrollbar {
  -ms-overflow-style: none;
  scrollbar-width: none;
}
</style>