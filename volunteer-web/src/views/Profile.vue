<template>
  <div style="padding: 20px;">
    <h2>👤 个人中心</h2>

    <el-card class="profile-card" :body-style="{ padding: isMobile ? '10px' : '20px' }">
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">

        <!-- 标签页 1：基本资料 -->
        <el-tab-pane label="基本资料" name="info">
          <!-- 🚨 关键：label-position 动态切换，移动端设为 top -->
          <el-form
              :model="profileForm"
              :label-width="isMobile ? '80px' : '100px'"
              :label-position="isMobile ? 'top' : 'right'"
              style="margin-top: 20px;"
          >
            <el-form-item label="我的头像">
              <!-- 头像区域响应式：移动端上下排布 -->
              <div class="avatar-upload-wrapper">
                <el-avatar :size="60" :src="profileForm.avatar || defaultAvatarUrl" />
                <el-input v-model="profileForm.avatar" placeholder="图片链接" class="avatar-input">
                  <template #prepend v-if="!isMobile">URL</template>
                </el-input>
              </div>
            </el-form-item>

            <el-form-item label="技能特长">
              <el-select
                  v-model="profileForm.skills"
                  multiple
                  filterable
                  allow-create
                  default-first-option
                  placeholder="请选择或输入技能"
                  style="width: 100%"
              >
                <el-option label="医疗急救" value="医疗急救" />
                <el-option label="心理疏导" value="心理疏导" />
                <el-option label="家电维修" value="家电维修" />
                <el-option label="文艺演出" value="文艺演出" />
                <el-option label="外语翻译" value="外语翻译" />
              </el-select>
            </el-form-item>

            <el-form-item label="空闲时间">
              <el-select v-model="profileForm.availableTime" placeholder="请选择" style="width: 100%">
                <el-option label="工作日白天" value="工作日白天" />
                <el-option label="工作日晚上" value="工作日晚上" />
                <el-option label="周末" value="周末" />
                <el-option label="随时" value="随时" />
              </el-select>
            </el-form-item>

            <el-form-item label="电子邮箱">
              <el-input v-model="profileForm.email" />
            </el-form-item>
            <el-form-item label="登录账号">
              <el-input v-model="profileForm.username" disabled />
            </el-form-item>
            <el-form-item label="真实姓名" required>
              <el-input v-model="profileForm.realName" />
            </el-form-item>
            <el-form-item label="手机号码">
              <el-input v-model="profileForm.phone" />
            </el-form-item>
            <el-form-item label="性别">
              <el-radio-group v-model="profileForm.gender">
                <el-radio :label="1">男</el-radio>
                <el-radio :label="2">女</el-radio>
                <el-radio :label="0">保密</el-radio>
              </el-radio-group>
            </el-form-item>

            <el-form-item class="form-actions">
              <el-button type="primary" @click="submitProfile">保存资料</el-button>
              <el-button color="#d4af37" :icon="Trophy" @click="showCertificate" style="color: white;">
                荣誉证书
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- 标签页 2：修改密码 -->
        <el-tab-pane label="修改密码" name="pwd">
          <el-form :model="pwdForm" label-width="100px" style="margin-top: 20px;">
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
              <el-button type="danger" @click="submitPassword">确认修改</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- 标签页 3：我的兑换 -->
        <el-tab-pane label="我的兑换" name="exchange">
          <div v-if="exchangeList.length === 0" style="text-align: center; color: #999; margin: 40px;">
            暂无兑换记录，快去积分商城看看吧！
          </div>

          <div v-else class="exchange-grid">
            <div v-for="item in exchangeList" :key="item.recordId" class="ticket-card" :class="{ 'used': item.status === 1 }">
              <!-- 左侧 -->
              <div class="ticket-left">
                <el-image :src="item.goodsImage" class="ticket-img">
                  <template #error><div class="img-slot"><el-icon><Picture /></el-icon></div></template>
                </el-image>
                <div class="ticket-info">
                  <h4>{{ item.goodsName }}</h4>
                  <p>消耗积分: {{ item.costPoints }}</p>
                  <p class="time">兑换于: {{ formatTime(item.createTime) }}</p>
                </div>
              </div>

              <!-- 右侧 -->
              <div class="ticket-right">
                <div v-if="item.status === 0">
                  <!-- 🚨 修复点：改用普通的 img 标签，src 绑定生成的二维码 base64 -->
                  <img :src="item.qrCodeUrl" class="qr-img" />
                  <div class="code-text">{{ item.redeemCode }}</div>
                  <div class="tip">请出示给管理员</div>
                </div>
                <div v-else class="used-stamp">
                  <el-icon><CircleCheckFilled /></el-icon>
                  <div>已领取</div>
                  <div style="font-size: 10px;">{{ formatTime(item.exchangeTime) }}</div>
                </div>
              </div>
            </div>
          </div>
        </el-tab-pane>

      </el-tabs>
    </el-card>

    <!-- 证书弹窗 -->
    <el-dialog v-model="certVisible" title="🏅 我的志愿荣誉证书" :width="isMobile ? '95%' : '800px'" top="5vh" destroy-on-close>
      <!-- 滚动容器：解决手机端溢出 -->
      <div class="cert-scroll-wrapper">
        <!-- 缩放容器：仅在移动端通过变量控制缩放，解决位移问题 -->
        <div class="cert-scale-box" :class="{ 'mobile-scale': isMobile }">
          <!-- 🚨 截图目标：保持 1:1 比例，不要在它身上直接写 transform -->
          <div class="cert-border" id="cert-content">
            <div class="cert-inner">
              <!-- ... 证书内部内容保持不变 ... -->
              <div class="cert-header">
                <div style="font-size: 55px; line-height: 1; margin-bottom: 10px;">🏆</div>
                <h1>志愿服务荣誉证书</h1>
                <p>VOLUNTEER HONORARY CERTIFICATE</p>
              </div>
              <div class="cert-body">
                <div class="awardee">尊敬的 <span>{{ profileForm.realName }}</span> 志愿者：</div>
                <div class="content-text">
                  &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;感谢您在社区志愿服务中展现出的无私奉献精神。
                  截止今日，您已累计服务 <strong>{{ profileForm.totalHours || 0 }}</strong> 小时，
                  累计获得 <strong>{{ profileForm.totalPoints || 0 }}</strong> 荣誉积分。
                  您的善行义举温暖了社区，特发此证，以资鼓励！
                </div>
              </div>
              <div class="cert-footer">
                <div class="date"><p>智慧社区志愿服务中心</p><p>{{ currentDate }}</p></div>
                <div class="seal"><div class="seal-inner"><span>志愿服务专用章</span></div></div>
              </div>
            </div>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="certVisible = false">关闭</el-button>
        <el-button type="primary" :loading="downloading" @click="downloadImage">下载保存证书</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, onUnmounted } from 'vue';
import { ElMessage } from 'element-plus';
import { useRouter } from 'vue-router';
import { Trophy, CircleCheckFilled, Picture } from '@element-plus/icons-vue';
import request from '../utils/request';
import html2canvas from 'html2canvas';
import QRCode from 'qrcode'; // 🚨 引入纯 JS 库
import { getDefaultAvatar } from '../utils/levelRules';

const router = useRouter();
const activeTab = ref('info');
const userId = localStorage.getItem('userId');
const certVisible = ref(false);
const downloading = ref(false);

const profileForm = ref({
  userId: userId,
  username: '',
  realName: '',
  phone: '',
  gender: 0,
  email: '',
  avatar: '',
  skills: [],
  availableTime: '',
  totalPoints: 0,
  currentPoints: 0,
  totalHours: 0
});

const pwdForm = ref({ oldPassword: '', newPassword: '', confirmPassword: '' });
const defaultAvatarUrl = ref('');
const exchangeList = ref([]);

const formatTime = (timeStr) => {
  if (!timeStr) return '--';
  return timeStr.split('T')[0];
};

const handleTabChange = (tabName) => {
  if (tabName === 'exchange') {
    fetchExchanges();
  }
};

const fetchUserInfo = async () => {
  try {
    const res = await request.get(`/api/user/info?userId=${userId}`);
    const userData = res.data;

    let parsedSkills = [];
    if (userData.skills) {
      try {
        parsedSkills = JSON.parse(userData.skills);
      } catch (e) {
        parsedSkills = [userData.skills];
      }
    }
    userData.skills = parsedSkills;
    profileForm.value = userData;
    defaultAvatarUrl.value = getDefaultAvatar(res.data.username);
  } catch (error) { console.error(error); }
};

// 🚨 核心修复：获取兑换记录并生成二维码
const fetchExchanges = async () => {
  try {
    const res = await request.get(`/api/shop/my-exchanges?userId=${userId}`);
    const list = res.data;

    // 遍历列表，为每个待核销的订单生成二维码 Base64 图片
    for (let item of list) {
      if (item.status === 0 && item.redeemCode) {
        try {
          // QRCode.toDataURL 是异步方法
          item.qrCodeUrl = await QRCode.toDataURL(item.redeemCode, { width: 80, margin: 1 });
        } catch (err) {
          console.error('二维码生成失败', err);
        }
      }
    }
    exchangeList.value = list;
  } catch (error) {
    console.error("获取兑换记录失败", error);
  }
};

const submitProfile = async () => {
  if (!profileForm.value.realName) return ElMessage.warning('真实姓名不能为空');
  try {
    const submitData = JSON.parse(JSON.stringify(profileForm.value));
    if (Array.isArray(submitData.skills)) {
      submitData.skills = JSON.stringify(submitData.skills);
    } else {
      submitData.skills = '[]';
    }
    await request.put('/api/user/profile', submitData);
    ElMessage.success('更新成功');
    localStorage.setItem('realName', profileForm.value.realName);
    setTimeout(() => location.reload(), 500);
  } catch (e) {}
};

const submitPassword = async () => {
  if (!pwdForm.value.newPassword) return ElMessage.warning('请输入新密码');
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

const currentDate = computed(() => {
  const date = new Date();
  return `${date.getFullYear()}年${date.getMonth() + 1}月${date.getDate()}日`;
});

const showCertificate = () => {
  if (!profileForm.value.realName) return ElMessage.warning('请先完善真实姓名');
  certVisible.value = true;
};

// 终极完美的下载证书方法
const downloadImage = async () => {
  downloading.value = true;
  const element = document.getElementById("cert-content");

  try {
    // 关键修复：在截图前，临时把证书“拎出来”放到屏幕左上角
    // 这样可以避免任何父级元素的 transform 或布局影响
    const originalStyle = element.style.cssText;
    element.style.position = 'fixed';
    element.style.top = '0';
    element.style.left = '0';
    element.style.zIndex = '9999';
    element.style.transform = 'none'; // 强制移除缩放
    element.style.margin = '0';

    const canvas = await html2canvas(element, {
      scale: 2,
      useCORS: true,
      backgroundColor: '#fffaf0',

      // 不再需要写死的 width 和 height，让它自动计算
      // 也不再需要 onclone，因为我们直接操作了原始 DOM
      scrollX: 0,
      scrollY: 0,
    });

    // 截图完成后，立刻把证书放回原位
    element.style.cssText = originalStyle;

    // --- 下载逻辑保持不变 ---
    const imgUrl = canvas.toDataURL("image/png");
    const link = document.createElement("a");
    link.href = imgUrl;
    link.download = `荣誉证书_${profileForm.value.realName}.png`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);

    ElMessage.success('证书下载成功');
  } catch (error) {
    console.error(error);
    ElMessage.error('生成失败，请稍后重试');
  } finally {
    downloading.value = false;
  }
};

// 🚨 响应式布局：监听屏幕宽度
const isMobile = ref(window.innerWidth <= 768);
const handleResize = () => {
  isMobile.value = window.innerWidth <= 768;
};

onMounted(() => {
  fetchUserInfo();
  window.addEventListener('resize', handleResize);
});

onUnmounted(() => {
  window.removeEventListener('resize', handleResize);
});
</script>

<style scoped>
/* ====================================================
   🖥️ 默认样式 (PC 端优先)
   ==================================================== */

/* 基础容器 */
.profile-card {
  max-width: 800px;
  margin: 20px auto;
  transition: all 0.3s;
}

/* 头像上传区域 */
.avatar-upload-wrapper {
  display: flex;
  align-items: center;
  gap: 15px;
}

/* 🎟️ 兑换券基础样式 (Grid 布局) */
.exchange-grid {
  display: grid;
  gap: 15px;
  /* 自适应列宽：PC端每行放两个，宽屏放三个 */
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
}

/* 单张兑换券卡片 (左右结构) */
.ticket-card {
  display: flex;
  flex-direction: row; /* 默认横向排列 */
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  overflow: hidden;
  background: white;
  transition: all 0.3s;
  height: 140px; /* 固定高度 */
}

.ticket-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

.ticket-card.used {
  opacity: 0.6;
  filter: grayscale(100%);
}

/* 券左侧：商品信息 */
.ticket-left {
  flex: 1;
  padding: 15px;
  display: flex;
  gap: 10px;
  border-right: 2px dashed #eee; /* 虚线分割 */
  align-items: center;
}

.ticket-img {
  width: 80px;
  height: 80px;
  border-radius: 6px;
  flex-shrink: 0;
  border: 1px solid #f0f0f0;
}

/* 图片加载失败占位 */
.img-slot {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%;
  height: 100%;
  background: #f5f7fa;
  color: #909399;
  font-size: 20px;
}

.ticket-info {
  display: flex;
  flex-direction: column;
  justify-content: center;
  overflow: hidden; /* 防止文字溢出 */
}

.ticket-info h4 {
  margin: 0 0 8px 0;
  font-size: 15px;
  color: #303133;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.ticket-info p {
  margin: 0;
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
}

/* 券右侧：核销区 */
.ticket-right {
  width: 130px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 10px;
  background: #fafafa;
  flex-shrink: 0;
}

.qr-img {
  width: 80px;
  height: 80px;
  display: block;
  margin-bottom: 5px;
}

.code-text {
  font-weight: bold;
  font-family: 'Consolas', monospace;
  color: #409eff;
  font-size: 12px;
  background: rgba(64, 158, 255, 0.1);
  padding: 2px 5px;
  border-radius: 4px;
}

.tip {
  font-size: 10px;
  color: #c0c4cc;
  margin-top: 5px;
}

/* 已领取印章样式 */
.used-stamp {
  color: #67c23a;
  text-align: center;
  font-weight: bold;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.used-stamp .el-icon {
  font-size: 36px;
  margin-bottom: 5px;
}

/* 🏅 证书预览容器 */
.cert-wrapper {
  padding: 20px;
  background-color: #f0f2f5;
  display: flex;
  justify-content: center;
  overflow: hidden; /* 防止滚动 */
}

/* 证书主体 (仿纸张) */
.cert-border {
  width: 700px;
  height: 500px; /* 黄金比例 */
  background-color: #fffaf0;
  border: 10px solid #d4af37;
  padding: 5px;
  box-shadow: 0 5px 20px rgba(0, 0, 0, 0.15);
  position: relative;
  /* 默认无缩放 */
  transform-origin: top center;
}

.cert-inner {
  width: 100%;
  height: 100%;
  border: 2px solid #d4af37;
  padding: 30px;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  justify-content: space-between;

  /* 🚨 修复：移除 SVG 背景，改用非常浅的渐变模拟纸张质感 */
  background-image: linear-gradient(to top, rgba(255,250,240,0.5) 0%, rgba(255,250,240,1) 100%);
}
.cert-header { text-align: center; color: #d4af37; }
.cert-header h1 { margin: 10px 0 5px; font-family: "SimHei", serif; font-size: 36px; letter-spacing: 5px; color: #b8860b; }
.cert-header p { margin: 0; font-size: 12px; letter-spacing: 2px; }

.cert-body { margin-top: 20px; font-family: "KaiTi", serif; color: #333; }
.awardee { font-size: 22px; margin-bottom: 20px; }
.awardee span { border-bottom: 2px solid #333; padding: 0 10px; font-weight: bold; font-size: 26px; }
.content-text { font-size: 18px; line-height: 1.8; text-align: justify; text-indent: 2em; }
.content-text strong { color: #d4af37; font-size: 22px; margin: 0 5px; font-family: Arial, sans-serif; }

.cert-footer { text-align: right; position: relative; margin-top: 30px; padding-right: 20px; }
.date p { margin: 5px 0; font-family: "KaiTi", serif; font-size: 18px; }

/* 红色公章 */
.seal {
  position: absolute;
  right: 20px;
  top: -30px;
  width: 130px;
  height: 130px;
  border: 4px solid #f56c6c;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #f56c6c;
  font-weight: bold;
  font-family: "SimHei", sans-serif;
  transform: rotate(-15deg);
  opacity: 0.85;
}
.seal-inner {
  width: 116px;
  height: 116px;
  border: 1px solid #f56c6c;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
  font-size: 16px;
  letter-spacing: 2px;
  padding: 15px;
  box-sizing: border-box;
}

/* ====================================================
   📱 移动端响应式适配 (屏幕宽度小于 768px 时生效)
   ==================================================== */
@media screen and (max-width: 768px) {
  .profile-card {
    margin: 0;
    border: none;
    border-radius: 0;
    box-shadow: none !important;
  }

  /* 表单按钮居中 */
  .form-actions :deep(.el-form-item__content) {
    justify-content: center;
    margin-left: 0 !important;
    gap: 15px;
  }

  .avatar-upload-wrapper {
    flex-direction: column;
    align-items: flex-start;
  }

  .avatar-input {
    width: 100% !important;
  }

  /* 🎟️ 兑换券重塑：由左右结构变为上下结构 */
  .ticket-card {
    flex-direction: column;
    height: auto; /* 高度自适应 */
  }

  .ticket-left {
    border-right: none;
    border-bottom: 2px dashed #eee;
    padding: 15px;
  }

  .ticket-info h4 {
    white-space: normal; /* 允许换行 */
  }

  .ticket-right {
    width: 100% !important;
    height: auto;
    padding: 20px 0;
    background: #fffdfa;
  }

  .exchange-grid {
    grid-template-columns: 1fr; /* 手机端单列 */
  }

  /* 🏅 证书弹窗适配：让弹窗近乎全屏，内部可滚动 */
  /* 使用 :deep() 穿透 Element Plus 的组件样式 */
  :deep(.el-dialog) {
    --el-dialog-width: 95% !important; /* 强制弹窗宽度为 95% */
  }

  .cert-scroll-wrapper {
    width: 100%;
    overflow-x: auto; /* 允许横向滚动 */
    -webkit-overflow-scrolling: touch;
    padding-bottom: 10px;
    background-color: #f0f2f5;
    border-radius: 6px;
  }

  .cert-scale-box {
    /* 不再需要 Flex 居中 */
  }

  .mobile-scale .cert-border {
    /* 🚨 修复：增大缩放比例，让预览更清晰 */
    transform: scale(0.8);
    /* 调整 transform-origin，让它从左上角开始缩放 */
    transform-origin: top left;

    /* 关键：由于缩放了，需要用 margin-right 撑开滚动容器的宽度 */
    /* 700px (原宽) * 0.8 (缩放) = 560px */
    /* 700 - 560 = 140px. 负 margin 把右边缩进去 */
    margin-right: -140px;
  }

}
</style>