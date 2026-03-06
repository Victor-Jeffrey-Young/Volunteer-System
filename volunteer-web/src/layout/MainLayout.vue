<template>
  <div class="admin-layout">
    <!-- 左侧边栏：改为白色背景，带轻微阴影 -->
    <div class="sidebar">
      <div class="logo-container">
        <!-- 加个简单的 Logo 图标 -->
        <el-icon class="logo-icon"><Promotion /></el-icon>
        <span class="logo-text">志愿服务管理</span>
      </div>

      <nav class="nav-menu">
        <router-link to="/home" class="nav-item">
          <el-icon><HomeFilled /></el-icon> <span>系统首页</span>
        </router-link>

        <!-- 管理员专属 -->
        <template v-if="userRole === 'ADMIN'">
          <div class="menu-divider">管理中心</div>
          <router-link to="/databoard" class="nav-item">
            <el-icon><DataLine /></el-icon> <span>数据大屏</span>
          </router-link>
          <router-link to="/users" class="nav-item">
            <el-icon><User /></el-icon> <span>用户管理</span>
          </router-link>
          <router-link to="/registrations" class="nav-item">
            <el-icon><Tickets /></el-icon> <span>报名审核</span>
          </router-link>
          <router-link to="/notices" class="nav-item">
            <el-icon><Bell /></el-icon> <span>新闻公告</span>
          </router-link>
        </template>

        <!-- 通用/志愿者 -->
        <div class="menu-divider">业务功能</div>
        <router-link to="/honor" class="nav-item">
          <el-icon><Trophy /></el-icon> <span>荣誉殿堂</span>
        </router-link>
        <router-link to="/activities" class="nav-item">
          <el-icon><Flag /></el-icon> <span>志愿活动</span>
        </router-link>
        <router-link v-if="userRole === 'VOLUNTEER'" to="/my-records" class="nav-item">
          <el-icon><Calendar /></el-icon> <span>我的报名</span>
        </router-link>
      </nav>
    </div>

    <!-- 右侧主体 -->
    <div class="main-container">
      <header class="top-header">
        <div class="breadcrumb-area">
          <el-icon color="#909399" style="margin-right: 5px;"><LocationInformation /></el-icon>
          <span>当前位置：{{ $route.name }}</span>
        </div>

        <div class="user-info">
          <!-- 段位展示 -->
          <div class="level-badge" v-if="userRole !== 'ADMIN'">
            <el-tooltip content="这是您的志愿荣誉段位，快去参加活动升级吧！" placement="bottom">
              <el-tag effect="dark" :color="currentLevel.color" style="border:none; color:white; margin-right: 15px;">
                {{ currentLevel.name }} ({{ userPoints }}分)
              </el-tag>
            </el-tooltip>
          </div>

          <!-- 头像展示 -->
          <el-avatar
              :size="32"
              :src="userAvatar || defaultAvatar"
              style="margin-right: 10px; border: 1px solid #ddd;"
          />

          <div class="welcome-text">
            <span>Hi, </span>
            <span class="user-name">{{ username }}</span>
            <el-tag size="small" effect="plain" round class="role-tag">
              {{ userRole === 'ADMIN' ? '管理员' : '志愿者' }}
            </el-tag>
          </div>

          <el-button type="primary" plain round size="small" @click="router.push('/profile')" :icon="User">
            个人中心
          </el-button>

          <el-button type="danger" plain round size="small" @click="handleLogout" :icon="SwitchButton" style="margin-left: 10px;">
            退出
          </el-button>
        </div>
      </header>

      <main class="content">
        <!-- 路由出口：增加过渡动画 -->
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watchEffect } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import request from '../utils/request'; // 引入请求工具
import { getLevelInfo, getDefaultAvatar } from '../utils/levelRules';

// 引入图标 (确保已安装 @element-plus/icons-vue)
import {
  HomeFilled, DataLine, User, Flag, Calendar,
  Tickets, Bell, Promotion, LocationInformation,
  SwitchButton,Trophy
} from '@element-plus/icons-vue';

const router = useRouter();
const route = useRoute();
// 响应式变量
const username = ref('');
const userRole = ref('');
const userPoints = ref(0);
const userAvatar = ref('');
const currentLevel = ref({});
const defaultAvatar = ref('');

const syncUserInfo = () => {
  const name = localStorage.getItem('realName');
  const role = localStorage.getItem('role');
  const points = parseInt(localStorage.getItem('points') || 0);
  const avatar = localStorage.getItem('avatar');
  const uName = localStorage.getItem('username') || 'user'; // 用于生成默认头像

  if (name && role) {
    username.value = name;
    userRole.value = role.toUpperCase();
    userPoints.value = points;
    userAvatar.value = avatar;
    defaultAvatar.value = getDefaultAvatar(uName);
    currentLevel.value = getLevelInfo(points);
  } else if (route.path !== '/login') {
    router.push('/login');
  }
};

const fetchLatestUserInfo = async () => {
  const userId = localStorage.getItem('userId');
  if (!userId) return;

  try {
    // 调用之前写好的获取个人信息接口
    const res = await request.get(`/api/user/info?userId=${userId}`);
    const user = res.data;

    // 1. 更新基础信息
    username.value = user.realName;
    userRole.value = user.role;
    userPoints.value = user.points || 0; // 防止为 null

    // 2. 更新段位 (根据最新积分计算)
    currentLevel.value = getLevelInfo(userPoints.value);

    // 3. 更新头像 (优先用数据库的，没有则用 username 生成默认的)
    // 🚨 注意：这里传入 user.username，确保和个人中心一致
    userAvatar.value = user.avatar || getDefaultAvatar(user.username);

  } catch (error) {
    console.error("获取用户信息失败", error);
    // 如果接口失败，回退到读取 localStorage (兜底方案)
    syncFromLocalStorage();
  }
};

// 兜底方案：只读缓存
const syncFromLocalStorage = () => {
  const name = localStorage.getItem('realName');
  const role = localStorage.getItem('role');
  if (name && role) {
    username.value = name;
    userRole.value = role;
    // 缓存里可能没有 points，所以这里只是临时显示
  } else if (route.path !== '/login') {
    router.push('/login');
  }
};

const handleLogout = () => {
  localStorage.clear();
  username.value = '';
  userRole.value = '';
  router.push('/login');
};

// 监听路由变化，每次切换页面都刷新一下数据（保证积分变动后导航栏同步更新）
watchEffect(() => {
  if (route.path !== '/login') {
    fetchLatestUserInfo();
  }
});

onMounted(() => {
  fetchLatestUserInfo();
});

</script>

<style scoped>
/* 1. 整体布局：背景色改用更柔和的浅灰 */
.admin-layout {
  display: flex;
  height: 100vh;
  background-color: #f5f7fa;
}

/* 2. 侧边栏：由深色改为白色，增加阴影，更清爽 */
.sidebar {
  width: 220px;
  background: #ffffff;
  box-shadow: 2px 0 8px rgba(0,0,0,0.05);
  display: flex;
  flex-direction: column;
  z-index: 10;
}

/* Logo 区域：增加品牌色 */
.logo-container {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-bottom: 1px solid #f0f0f0;
  color: #ff6b6b; /* 志愿红 */
}
.logo-icon { font-size: 24px; margin-right: 8px; }
.logo-text { font-size: 18px; font-weight: bold; color: #333; }

/* 菜单区域 */
.nav-menu { flex: 1; padding: 10px; overflow-y: auto; }

/* 菜单分组标题 */
.menu-divider {
  font-size: 12px;
  color: #909399;
  margin: 15px 0 5px 15px;
}

/* 菜单项：改为圆角卡片风格 */
.nav-item {
  display: flex;
  align-items: center;
  padding: 12px 15px;
  margin-bottom: 5px;
  color: #606266;
  text-decoration: none;
  border-radius: 8px;
  transition: all 0.3s;
  font-size: 14px;
}

.nav-item .el-icon { margin-right: 10px; font-size: 16px; }

/* 悬停效果 */
.nav-item:hover {
  background-color: #ffeaea; /* 淡淡的红色背景 */
  color: #ff6b6b;
}

/* 选中激活状态：高亮显示 */
.router-link-active {
  background: linear-gradient(90deg, #ff6b6b, #ff8787);
  color: white !important;
  box-shadow: 0 4px 10px rgba(255, 107, 107, 0.3);
}

/* 3. 顶部 Header：纯白背景，底部细线 */
.main-container { flex: 1; display: flex; flex-direction: column; overflow: hidden; }

.top-header {
  height: 60px;
  background: white;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
}

.breadcrumb-area {
  display: flex;
  align-items: center;
  color: #606266;
  font-size: 14px;
}

.user-info { display: flex; align-items: center; }
.welcome-text { margin-right: 20px; font-size: 14px; color: #606266; display: flex; align-items: center; }
.user-name { font-weight: bold; color: #333; margin: 0 5px; }
.role-tag { margin-left: 5px; }

/* 4. 内容区域 */
.content {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
}

/* 页面切换动画 (淡入淡出) */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>