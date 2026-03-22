<script setup>
import { ref, onMounted } from 'vue';
import { useUserStore } from '../../stores/user';
import { useRouter } from 'vue-router';
import { userApi } from '../../api/modules';
import { ElMessage } from 'element-plus';
import { 
  User, 
  Phone, 
  Mail, 
  ShieldCheck, 
  Camera,
  MapPin,
  Save,
  Key,
  LogOut
} from 'lucide-vue-next';

const userStore = useUserStore();
const router = useRouter();
const loading = ref(false);
const submitting = ref(false);

const form = ref({
    userId: null,
    username: '',
    realName: '',
    phone: '',
    email: '',
    gender: 1,
    avatar: '',
    availableTime: '' // 居民端可作为补充联系地址
});

const pwdForm = ref({
    oldPassword: '',
    newPassword: '',
    confirmPassword: ''
});

const showPwdDialog = ref(false);

const fetchData = async () => {
    if (!userStore.userId) return;
    loading.value = true;
    try {
        const res = await userApi.getUserInfo(userStore.userId);
        if (res.data) {
            form.value = { ...res.data };
        }
    } catch (err) {
        console.error('Fetch user info error:', err);
    } finally {
        loading.value = false;
    }
};

onMounted(fetchData);

const handleUpdateProfile = async () => {
    submitting.value = true;
    try {
        await userApi.updateProfile(form.value);
        ElMessage.success('个人资料已成功更新');
        userStore.fetchCurrentUser(); // 同步全局状态
    } catch (err) {
        console.error('Update profile error:', err);
    } finally {
        submitting.value = false;
    }
};

const handleUpdatePassword = async () => {
    if (pwdForm.value.newPassword !== pwdForm.value.confirmPassword) {
        return ElMessage.error('两次输入的新密码不一致');
    }
    try {
        await userApi.updatePassword({
            userId: userStore.userId,
            oldPassword: pwdForm.value.oldPassword,
            newPassword: pwdForm.value.newPassword
        });
        ElMessage.success('密码修改成功，请牢记新密码');
        showPwdDialog.value = false;
        pwdForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' };
    } catch (err) {
        console.error('Update password error:', err);
    }
};

const handleAvatarUpload = async (file) => {
    try {
        const res = await userApi.uploadAvatar(file.raw);
        if (res.data) {
            form.value.avatar = res.data;
            ElMessage.success('头像上传成功');
        }
    } catch (err) {
        console.error('Upload avatar error:', err);
        ElMessage.error('上传失败，请稍后重试');
    }
};

const handleLogout = () => {
    userStore.logout();
    router.push('/login');
};
</script>

<template>
  <div class="mx-auto max-w-4xl px-4 py-8 sm:px-6 lg:px-8">
    <div class="bg-white rounded-3xl shadow-sm border border-slate-100 overflow-hidden">
      <!-- Banner -->
      <div class="h-32 bg-gradient-to-r from-orange-400 to-amber-500"></div>
      
      <div class="px-8 pb-8">
        <!-- Profile Header -->
        <div class="relative -mt-12 mb-8 flex flex-col sm:flex-row sm:items-end gap-6">
          <div class="relative group">
            <div class="w-32 h-32 rounded-3xl border-4 border-white overflow-hidden bg-slate-100 shadow-lg">
              <img :src="form.avatar || 'https://api.dicebear.com/7.x/avataaars/svg?seed=' + form.username" class="w-full h-full object-cover" />
            </div>
            <el-upload
              action="#"
              :auto-upload="false"
              :show-file-list="false"
              :on-change="handleAvatarUpload"
              class="absolute bottom-2 right-2"
            >
              <button class="w-10 h-10 bg-white rounded-xl shadow-md flex items-center justify-center text-orange-600 hover:text-orange-700 transition-all border border-slate-100">
                <Camera class="w-5 h-5" />
              </button>
            </el-upload>
          </div>
          
          <div class="flex-1 space-y-1">
            <h1 class="text-2xl font-bold text-slate-900">{{ form.realName || '设置姓名' }}</h1>
            <p class="text-slate-500 text-sm">社区居民 · @{{ form.username }}</p>
          </div>

          <button 
            @click="showPwdDialog = true"
            class="flex items-center gap-2 px-4 py-2 text-sm font-medium text-slate-600 bg-slate-50 hover:bg-slate-100 rounded-xl border border-slate-200 transition-colors"
          >
            <Key class="w-4 h-4" />
            修改密码
          </button>
        </div>

        <div class="h-px bg-slate-100 mb-8"></div>

        <!-- Forms -->
        <el-form :model="form" label-position="top" class="grid grid-cols-1 md:grid-cols-2 gap-x-8">
          <el-form-item label="真实姓名">
            <el-input v-model="form.realName" placeholder="输入您的真实姓名">
              <template #prefix><User class="w-4 h-4 text-slate-400" /></template>
            </el-input>
          </el-form-item>

          <el-form-item label="联系电话">
            <el-input v-model="form.phone" placeholder="输入手机号">
              <template #prefix><Phone class="w-4 h-4 text-slate-400" /></template>
            </el-input>
          </el-form-item>

          <el-form-item label="电子邮箱">
            <el-input v-model="form.email" placeholder="输入常用邮箱">
              <template #prefix><Mail class="w-4 h-4 text-slate-400" /></template>
            </el-input>
          </el-form-item>

          <el-form-item label="性别">
            <el-radio-group v-model="form.gender" class="w-full">
              <el-radio-button :value="1">先生</el-radio-button>
              <el-radio-button :value="2">女士</el-radio-button>
              <el-radio-button :value="0">其他</el-radio-button>
            </el-radio-group>
          </el-form-item>

          <el-form-item label="服务接收地址 (常用常在)" class="md:col-span-2">
            <el-input 
              v-model="form.availableTime" 
              type="textarea" 
              :rows="3" 
              placeholder="例如：长乐路 123 号 402 室。此地址将作为您发布心愿时的默认地点。"
            >
              <template #prefix><MapPin class="w-4 h-4 text-slate-400" /></template>
            </el-input>
          </el-form-item>

          <div class="md:col-span-2 flex justify-end pt-6 border-t border-slate-50 mt-4">
            <button 
              @click.prevent="handleUpdateProfile"
              :disabled="submitting"
              class="flex items-center gap-2 px-8 py-3 bg-orange-600 text-white rounded-xl font-bold hover:bg-orange-700 transition-all shadow-lg shadow-orange-600/20 disabled:opacity-50"
            >
              <Save v-if="!submitting" class="w-5 h-5" />
              <div v-else class="animate-spin rounded-full h-5 w-5 border-2 border-white border-t-transparent"></div>
              保存基本信息
            </button>
          </div>
        </el-form>
      </div>
    </div>

    <!-- Security Tips -->
    <div class="mt-8 bg-emerald-50 rounded-2xl p-6 border border-emerald-100 flex gap-4">
      <ShieldCheck class="w-6 h-6 text-emerald-600 shrink-0" />
      <div class="space-y-1">
        <h4 class="font-bold text-emerald-900 text-sm">隐私保护说明</h4>
        <p class="text-xs text-emerald-700 leading-relaxed">
          您的真实姓名和联系电话仅在心愿被志愿者认领后，才会展示给对方。除此之外，您的这些敏感信息将被系统严格加密保护。
        </p>
      </div>
    </div>

    <!-- Mobile Logout Button -->
    <div class="mt-6 lg:hidden">
      <button 
        @click="handleLogout"
        class="w-full flex items-center justify-center gap-2 px-6 py-3.5 bg-rose-50 text-rose-600 rounded-2xl font-bold border border-rose-100 hover:bg-rose-100 transition-colors active:scale-95"
      >
        <LogOut class="w-5 h-5" />
        安全退出登录
      </button>
    </div>

    <!-- Password Dialog -->
    <el-dialog v-model="showPwdDialog" title="修改登录密码" width="400px" border-radius="20px">
      <el-form :model="pwdForm" label-position="top">
        <el-form-item label="当前密码">
          <el-input v-model="pwdForm.oldPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="pwdForm.newPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="确认新密码">
          <el-input v-model="pwdForm.confirmPassword" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="flex gap-3">
          <el-button @click="showPwdDialog = false" class="flex-1">取消</el-button>
          <el-button color="#f97316" @click="handleUpdatePassword" class="flex-1">确认修改</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
:deep(.el-input__wrapper) {
  border-radius: 12px;
  padding: 8px 12px;
  box-shadow: 0 0 0 1px #e2e8f0 inset;
}
:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #f97316 inset !important;
}
:deep(.el-radio-button__inner) {
  border-radius: 10px !important;
  margin-right: 8px;
  border: 1px solid #e2e8f0 !important;
}
:deep(.el-radio-button:first-child .el-radio-button__inner) {
  border-radius: 10px !important;
}
</style>
