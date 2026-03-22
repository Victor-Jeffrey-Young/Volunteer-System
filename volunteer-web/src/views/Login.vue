<script setup>
import { ref, reactive } from 'vue';
import { useRouter } from 'vue-router';
import { useUserStore } from '../stores/user';
import { ElMessage } from 'element-plus';
import {
  User as UserIcon,
  Lock,
  EyeOff,
  Eye,
  CheckCircle,
  Smartphone,
  Heart,
} from 'lucide-vue-next';
import request from '../utils/request';

const router = useRouter();
const userStore = useUserStore();

// 状态控制
const isLogin = ref(true);
const showPassword = ref(false);
const loading = ref(false);

// 表单数据
const form = reactive({
  username: '',
  password: '',
  realName: '', // 注册用
  phone: '',    // 注册用：手机号
  email: '',    // 注册用：邮箱
  gender: 1,    // 注册用：性别 (1-男, 2-女)
  role: 'VOLUNTEER' // 默认注册为志愿者
});

const handleLogin = async () => {
  if (!form.username || !form.password) {
    ElMessage.warning('请填写完整账号密码');
    return;
  }
  
  loading.value = true;
  try {
    // 🚨 修正：统一调用 store 的 login 方法
    const data = await userStore.login(form.username, form.password);

    ElMessage.success(`欢迎回来，${data.realName || data.username}！`);
    
    // 根据角色跳转
    if (data.role === 'ADMIN') {
      router.push('/admin/home');
    } else if (data.role === 'VOLUNTEER') {
      router.push('/volunteer/home');
    } else if (data.role === 'RESIDENT') {
      router.push('/resident/wishes');
    }
  } catch (error) {
    console.error("Login Error:", error);
  } finally {
    loading.value = false;
  }
};

const handleRegister = async () => {
  if (!form.username || !form.password || !form.realName || !form.phone) {
    ElMessage.warning('请填写完整注册信息 (包括手机号)');
    return;
  }

  // 手机号格式校验
  if (!/^1[3-9]\d{9}$/.test(form.phone)) {
    ElMessage.warning('请输入正确的手机号码');
    return;
  }

  loading.value = true;
  try {
    await request.post('/api/auth/register', form);
    ElMessage.success('注册成功，请登录');
    isLogin.value = true;
  } catch (error) {
    console.error("Register Error:", error);
  } finally {
    loading.value = false;
  }
};

const toggleMode = () => {
  isLogin.value = !isLogin.value;
};
</script>

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
            <h2 class="text-xl font-bold tracking-tight text-slate-900">志愿服务平台</h2>
          </div>
          <div>
            <span class="text-sm text-slate-500 mr-4 hidden sm:inline">连接爱心，服务社会</span>
          </div>
        </div>
      </div>
    </header>

    <main class="flex-grow flex items-center justify-center p-4 sm:p-6 lg:p-8 relative overflow-hidden">
      <!-- Background Decoration -->
      <div class="absolute inset-0 z-0 opacity-10 pointer-events-none">
        <div class="absolute top-[-10%] right-[-10%] w-[500px] h-[500px] bg-orange-600 rounded-full blur-[120px]"></div>
        <div class="absolute bottom-[-10%] left-[-10%] w-[500px] h-[500px] bg-orange-600/40 rounded-full blur-[120px]"></div>
      </div>

      <div class="w-full max-w-[1000px] grid lg:grid-cols-2 bg-white rounded-3xl overflow-hidden shadow-2xl relative z-10">
        <!-- Left Side: Visual -->
        <div class="hidden lg:block relative overflow-hidden" style="background: linear-gradient(135deg, #ea580c 0%, #c2410c 40%, #9a3412 100%);">
          <!-- 装饰性 SVG 图案代替外部图片 -->
          <div class="absolute inset-0 opacity-10" style="background-image: url('data:image/svg+xml,%3Csvg width=&quot;60&quot; height=&quot;60&quot; viewBox=&quot;0 0 60 60&quot; xmlns=&quot;http://www.w3.org/2000/svg&quot;%3E%3Cg fill=&quot;none&quot; fill-rule=&quot;evenodd&quot;%3E%3Cg fill=&quot;%23ffffff&quot; fill-opacity=&quot;0.4&quot;%3E%3Cpath d=&quot;M36 34v-4h-2v4h-4v2h4v4h2v-4h4v-2h-4zm0-30V0h-2v4h-4v2h4v4h2V6h4V4h-4zM6 34v-4H4v4H0v2h4v4h2v-4h4v-2H6zM6 4V0H4v4H0v2h4v4h2V6h4V4H6z&quot;/%3E%3C/g%3E%3C/g%3E%3C/svg%3E');"></div>
          <!-- 装饰圆形光晕 -->
          <div class="absolute top-[-20%] right-[-20%] w-[300px] h-[300px] bg-orange-400/30 rounded-full blur-[80px]"></div>
          <div class="absolute bottom-[-10%] left-[-10%] w-[250px] h-[250px] bg-orange-300/20 rounded-full blur-[60px]"></div>
          <div class="relative z-20 h-full flex flex-col justify-end p-12 text-white">
            <h1 class="text-4xl font-bold mb-4">开启您的<br />公益之旅</h1>
            <p class="text-lg opacity-90 leading-relaxed mb-8">
              加入志愿者网络，用您的时间和才华为社区带来积极的改变。每一个微小的行动都能汇聚成巨大的力量。
            </p>
            <div class="flex items-center gap-4 text-sm font-medium">
              <span class="flex items-center gap-1"><CheckCircle class="w-4 h-4" /> 真实项目</span>
              <span class="flex items-center gap-1"><CheckCircle class="w-4 h-4" /> 官方认证</span>
              <span class="flex items-center gap-1"><CheckCircle class="w-4 h-4" /> 温暖社区</span>
            </div>
          </div>
        </div>

        <!-- Right Side: Form -->
        <div class="p-8 sm:p-12">
          <div class="mb-8">
            <div class="flex border-b border-slate-200">
              <button
                @click="isLogin = true"
                class="pb-4 px-4 text-sm font-bold border-b-2 transition-all"
                :class="isLogin ? 'border-orange-600 text-orange-600' : 'border-transparent text-slate-500 hover:text-slate-900'"
              >
                登录账号
              </button>
              <button
                @click="isLogin = false"
                class="pb-4 px-4 text-sm font-bold border-b-2 transition-all"
                :class="!isLogin ? 'border-orange-600 text-orange-600' : 'border-transparent text-slate-500 hover:text-slate-900'"
              >
                注册新用户
              </button>
            </div>
          </div>

          <form class="space-y-5" @submit.prevent="isLogin ? handleLogin() : handleRegister()">
            <div class="space-y-1">
              <label class="text-sm font-semibold text-slate-700">账号</label>
              <div class="relative">
                <UserIcon class="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400 w-5 h-5" />
                <input
                  v-model="form.username"
                  class="w-full pl-10 pr-4 py-3 bg-slate-50 border border-slate-200 rounded-xl focus:ring-2 focus:ring-orange-600/20 focus:border-orange-600 outline-none text-sm transition-all"
                  placeholder="请输入账号"
                  type="text"
                />
              </div>
            </div>

            <div v-if="!isLogin" class="space-y-1">
              <label class="text-sm font-semibold text-slate-700">真实姓名</label>
              <div class="relative">
                <CheckCircle class="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400 w-5 h-5" />
                <input
                  v-model="form.realName"
                  class="w-full pl-10 pr-4 py-3 bg-slate-50 border border-slate-200 rounded-xl focus:ring-2 focus:ring-orange-600/20 focus:border-orange-600 outline-none text-sm transition-all"
                  placeholder="请输入您的真实姓名"
                  type="text"
                />
              </div>
            </div>

            <div v-if="!isLogin" class="grid grid-cols-2 gap-4">
              <div class="space-y-1">
                <label class="text-sm font-semibold text-slate-700">身份类型</label>
                <div class="mt-1">
                  <el-radio-group v-model="form.role" class="flex gap-2">
                    <el-radio label="VOLUNTEER">志愿者</el-radio>
                    <el-radio label="RESIDENT">居民</el-radio>
                  </el-radio-group>
                </div>
              </div>
              <div class="space-y-1">
                <label class="text-sm font-semibold text-slate-700">性别</label>
                <div class="mt-1">
                  <el-radio-group v-model="form.gender" class="flex gap-2">
                    <el-radio :label="1">男</el-radio>
                    <el-radio :label="2">女</el-radio>
                  </el-radio-group>
                </div>
              </div>
            </div>

            <div v-if="!isLogin" class="space-y-1">
              <label class="text-sm font-semibold text-slate-700">手机号码</label>
              <div class="relative">
                <Smartphone class="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400 w-5 h-5" />
                <input
                  v-model="form.phone"
                  class="w-full pl-10 pr-4 py-3 bg-slate-50 border border-slate-200 rounded-xl focus:ring-2 focus:ring-orange-600/20 focus:border-orange-600 outline-none text-sm transition-all"
                  placeholder="请输入 11 位手机号"
                  type="tel"
                />
              </div>
            </div>

            <div v-if="!isLogin" class="space-y-1">
              <label class="text-sm font-semibold text-slate-700">电子邮箱</label>
              <div class="relative">
                <CheckCircle class="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400 w-5 h-5" />
                <input
                  v-model="form.email"
                  class="w-full pl-10 pr-4 py-3 bg-slate-50 border border-slate-200 rounded-xl focus:ring-2 focus:ring-orange-600/20 focus:border-orange-600 outline-none text-sm transition-all"
                  placeholder="请输入电子邮箱"
                  type="email"
                />
              </div>
            </div>

            <div class="space-y-1">
              <label class="text-sm font-semibold text-slate-700">密码</label>
              <div class="relative">
                <Lock class="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400 w-5 h-5" />
                <input
                  v-model="form.password"
                  class="w-full pl-10 pr-10 py-3 bg-slate-50 border border-slate-200 rounded-xl focus:ring-2 focus:ring-orange-600/20 focus:border-orange-600 outline-none text-sm transition-all"
                  :placeholder="isLogin ? '请输入密码' : '请设置 6 位以上密码'"
                  :type="showPassword ? 'text' : 'password'"
                />
                <button
                  type="button"
                  class="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600"
                  @click="showPassword = !showPassword"
                >
                  <Eye v-if="showPassword" class="w-5 h-5" />
                  <EyeOff v-else class="w-5 h-5" />
                </button>
              </div>
            </div>

            <button
              class="w-full bg-orange-600 text-white font-bold py-3.5 rounded-xl shadow-lg shadow-orange-600/30 hover:bg-orange-700 transition-all active:scale-[0.98] mt-2 flex justify-center items-center gap-2"
              type="submit"
              :disabled="loading"
            >
              <span v-if="loading" class="animate-spin border-2 border-white border-t-transparent rounded-full w-4 h-4"></span>
              {{ isLogin ? '立即登录' : '注册并登录' }}
            </button>
          </form>

          <!-- 已删除：其他登录方式 -->
        </div>
      </div>
    </main>

    <footer class="py-8 bg-white border-t border-slate-200">
      <div class="max-w-7xl mx-auto px-4 text-center text-slate-500 text-sm">
        <p>© 2024 志愿服务管理系统。让爱心传递，让温暖常在。</p>
      </div>
    </footer>
  </div>
</template>
