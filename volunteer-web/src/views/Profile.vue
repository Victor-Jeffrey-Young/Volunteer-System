<template>
  <div style="padding: 20px;">
    <h2>👤 个人中心</h2>

    <el-card style="max-width: 600px; margin-top: 20px;">
      <el-tabs v-model="activeTab">

        <!-- 标签页 1：基本资料 -->
        <el-tab-pane label="基本资料" name="info">
          <el-form-item label="我的头像">
            <div style="display: flex; align-items: center; gap: 15px;">
              <!-- 显示头像 -->
              <el-avatar :size="60" :src="profileForm.avatar || defaultAvatarUrl" />

              <!-- 修改头像输入框 (实际项目中通常是上传文件，这里简化为填URL) -->
              <el-input v-model="profileForm.avatar" placeholder="输入头像图片URL" style="width: 300px;">
                <template #prepend>URL</template>
              </el-input>
            </div>
            <div style="font-size: 12px; color: #999; margin-top: 5px;">
              提示：如果留空，系统将根据用户名自动生成卡通头像。
            </div>
          </el-form-item>
          <!-- 加入邮箱输入 -->
          <el-form-item label="电子邮箱">
            <el-input v-model="profileForm.email" placeholder="example@volunteer.com" />
          </el-form-item>
          <el-form :model="profileForm" label-width="100px" style="margin-top: 20px;">
            <el-form-item label="登录账号">
              <el-input v-model="profileForm.username" disabled placeholder="账号不可修改" />
            </el-form-item>
            <el-form-item label="真实姓名" required>
              <el-input v-model="profileForm.realName" placeholder="请输入真实姓名" />
            </el-form-item>
            <el-form-item label="手机号码">
              <el-input v-model="profileForm.phone" placeholder="请输入手机号码" />
            </el-form-item>
            <el-form-item label="性别">
              <el-radio-group v-model="profileForm.gender">
                <el-radio :label="1">男</el-radio>
                <el-radio :label="2">女</el-radio>
                <el-radio :label="0">保密</el-radio>
              </el-radio-group>
            </el-form-item>

            <el-form-item>
              <el-button type="primary" @click="submitProfile">保存资料</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- 标签页 2：修改密码 -->
        <el-tab-pane label="修改密码" name="pwd">
          <el-form :model="pwdForm" label-width="100px" style="margin-top: 20px;">
            <el-form-item label="原密码" required>
              <el-input v-model="pwdForm.oldPassword" type="password" show-password placeholder="请输入当前密码" />
            </el-form-item>
            <el-form-item label="新密码" required>
              <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="请输入新密码" />
            </el-form-item>
            <el-form-item label="确认新密码" required>
              <el-input v-model="pwdForm.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
            </el-form-item>

            <el-form-item>
              <el-button type="danger" @click="submitPassword">确认修改</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { useRouter } from 'vue-router';
import request from '../utils/request';
import { getDefaultAvatar } from '../utils/levelRules'; // 引入工具

const router = useRouter();
const activeTab = ref('info');
const userId = localStorage.getItem('userId');

const profileForm = ref({
  userId: userId,
  username: '',
  realName: '',
  phone: '',
  gender: 0
});

const pwdForm = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
});

// 在 setup 中
const defaultAvatarUrl = ref('');

// 在 fetchUserInfo 获取成功后：
const fetchUserInfo = async () => {
  try {
    const res = await request.get(`/api/user/info?userId=${userId}`);
    profileForm.value = res.data;

    // 🚨 关键：这里也要用 res.data.username 来生成，保证和导航栏一致
    defaultAvatarUrl.value = getDefaultAvatar(res.data.username);

  } catch (error) {
    console.error(error);
  }
};

// 提交个人资料修改
const submitProfile = async () => {
  if (!profileForm.value.realName) {
    ElMessage.warning('真实姓名不能为空');
    return;
  }
  try {
    await request.put('/api/user/profile', profileForm.value);
    ElMessage.success('个人资料更新成功');
    // 更新右上角显示的姓名
    localStorage.setItem('realName', profileForm.value.realName);
    // 触发页面刷新，让 Header 里的名字也变过来 (偷懒的做法，实际应用可使用 Pinia)
    window.location.reload();
  } catch (error) {
    ElMessage.error('更新失败');
  }
};

// 提交密码修改
const submitPassword = async () => {
  if (!pwdForm.value.oldPassword || !pwdForm.value.newPassword) {
    ElMessage.warning('请将密码填写完整');
    return;
  }
  if (pwdForm.value.newPassword !== pwdForm.value.confirmPassword) {
    ElMessage.error('两次输入的新密码不一致！');
    return;
  }

  try {
    const res = await request.put('/api/user/password', {
      userId: userId,
      oldPassword: pwdForm.value.oldPassword,
      newPassword: pwdForm.value.newPassword
    });

    ElMessage.success(res.msg || '密码修改成功，请重新登录');

    // 修改密码成功后，强制清除本地存储并退回登录页
    setTimeout(() => {
      localStorage.clear();
      router.push('/login');
    }, 1500);

  } catch (error) {
    // 错误在 request.js 已拦截
  }
};

onMounted(() => {
  fetchUserInfo();
});
</script>