<template>
  <div style="padding: 20px;">
    <h2>👤 账号设置</h2>

    <el-card class="profile-card">
      <el-tabs v-model="activeTab">
        <!-- 标签页 1：基本资料 -->
        <el-tab-pane label="基本信息" name="info">
          <el-form :model="profileForm" label-width="100px" style="margin-top: 20px; max-width: 500px;">
            <el-form-item label="管理员头像">
              <div class="avatar-section">
                <el-avatar :size="80" :src="getFullAvatar(profileForm.avatar)" />
                <div class="upload-tip">
                  <el-button size="small" @click="triggerUpload">更换头像</el-button>
                  <p>建议尺寸 200x200，支持 JPG/PNG</p>
                </div>
                <input type="file" ref="fileInput" class="hidden-input" @change="onFileChange" accept="image/*" />
              </div>
            </el-form-item>

            <el-form-item label="登录账号">
              <el-input v-model="profileForm.username" disabled />
              <p class="input-tip">系统账号名不可修改</p>
            </el-form-item>

            <el-form-item label="管理权限">
              <el-tag type="danger" effect="dark">系统管理员</el-tag>
            </el-form-item>

            <el-form-item label="真实姓名" required>
              <el-input v-model="profileForm.realName" placeholder="请输入姓名" />
            </el-form-item>

            <el-form-item label="联系电话">
              <el-input v-model="profileForm.phone" placeholder="用于紧急联系" />
            </el-form-item>

            <el-form-item label="电子邮箱">
              <el-input v-model="profileForm.email" placeholder="接收系统通知" />
            </el-form-item>

            <el-form-item>
              <el-button type="primary" @click="submitProfile" :loading="saving">保存基本信息</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- 标签页 2：安全设置 -->
        <el-tab-pane label="安全设置" name="pwd">
          <el-form :model="pwdForm" label-width="100px" style="margin-top: 20px; max-width: 500px;">
            <div class="security-tip">
              <el-icon><Warning /></el-icon>
              定期更换密码有助于保障系统安全
            </div>
            <el-form-item label="原密码" required>
              <el-input v-model="pwdForm.oldPassword" type="password" show-password />
            </el-form-item>
            <el-form-item label="新密码" required>
              <el-input v-model="pwdForm.newPassword" type="password" show-password />
            </el-form-item>
            <el-form-item label="确认新密码" required>
              <el-input v-model="pwdForm.confirmPassword" type="password" show-password />
            </el-form-item>
            <el-form-item>
              <el-button type="danger" @click="submitPassword">确认修改密码</el-button>
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
import { Warning } from '@element-plus/icons-vue';
import request from '../../utils/request';
import { getFullAvatar } from '../../utils/file';

const router = useRouter();
const activeTab = ref('info');
const userId = localStorage.getItem('userId');
const saving = ref(false);
const fileInput = ref(null);

const profileForm = ref({
  userId: userId || '',
  username: '加载中...',
  realName: '',
  phone: '',
  email: '',
  avatar: ''
});

const pwdForm = ref({ oldPassword: '', newPassword: '', confirmPassword: '' });

const fetchUserInfo = async () => {
  try {
    const res = await request.get(`/api/user/info?userId=${userId}`);
    profileForm.value = res.data;
  } catch (error) { console.error(error); }
};

const triggerUpload = () => fileInput.value.click();

const onFileChange = async (e) => {
  const file = e.target.files[0];
  if (!file) return;
  
  const formData = new FormData();
  formData.append('file', file);
  formData.append('type', 'avatar');

  try {
    const res = await request.post('/api/file/upload', formData);
    if (res.data) {
      profileForm.value.avatar = res.data;
      ElMessage.success('头像上传成功，请点击下方保存生效');
    }
  } catch (e) { ElMessage.error('上传失败'); }
};

const submitProfile = async () => {
  if (!profileForm.value.realName) return ElMessage.warning('姓名不能为空');
  saving.value = true;
  try {
    await request.put('/api/user/profile', profileForm.value);
    ElMessage.success('个人资料更新成功');
    localStorage.setItem('realName', profileForm.value.realName);
    localStorage.setItem('avatar', profileForm.value.avatar);
  } catch (e) {
  } finally { saving.value = false; }
};

const submitPassword = async () => {
  if (!pwdForm.value.newPassword) return ElMessage.warning('请输入新密码');
  if (pwdForm.value.newPassword !== pwdForm.value.confirmPassword) return ElMessage.warning('两次密码输入不一致');
  
  try {
    await request.put('/api/user/password', {
      userId,
      oldPassword: pwdForm.value.oldPassword,
      newPassword: pwdForm.value.newPassword
    });
    ElMessage.success('密码修改成功，请重新登录');
    localStorage.clear();
    router.push('/login');
  } catch (e) {}
};

onMounted(fetchUserInfo);
</script>

<style scoped>
.profile-card {
  max-width: 900px;
  margin: 0 auto;
}

.avatar-section {
  display: flex;
  align-items: center;
  gap: 20px;
}

.upload-tip p {
  font-size: 12px;
  color: #909399;
  margin-top: 5px;
}

.input-tip {
  font-size: 12px;
  color: #c0c4cc;
  margin: 4px 0 0;
}

.security-tip {
  background: #fdf6ec;
  color: #e6a23c;
  padding: 12px;
  border-radius: 8px;
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
}

.hidden-input {
  display: none;
}
</style>