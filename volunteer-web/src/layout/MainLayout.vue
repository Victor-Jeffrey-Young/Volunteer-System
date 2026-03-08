<template>
  <div class="admin-layout">
    <!-- 左侧边栏 (PC) / 底部导航栏 (移动端) -->
    <div class="sidebar">
      <div class="logo-container">
        <el-icon class="logo-icon"><Promotion /></el-icon>
        <span class="logo-text">志愿服务管理</span>
      </div>

      <nav class="nav-menu">
        <router-link to="/home" class="nav-item">
          <el-icon><HomeFilled /></el-icon> <span class="nav-text">首页</span>
        </router-link>

        <!-- 管理员专属 -->
        <template v-if="userRole === 'ADMIN'">
          <div class="menu-divider">管理中心</div>
          <router-link to="/databoard" class="nav-item">
            <el-icon><DataLine /></el-icon> <span class="nav-text">数据</span>
          </router-link>
          <router-link to="/users" class="nav-item">
            <el-icon><User /></el-icon> <span class="nav-text">用户</span>
          </router-link>
          <router-link to="/registrations" class="nav-item">
            <el-icon><Tickets /></el-icon> <span class="nav-text">审核</span>
          </router-link>
          <router-link to="/notices" class="nav-item">
            <el-icon><Bell /></el-icon> <span class="nav-text">公告</span>
          </router-link>
          <router-link to="/goods-manage" class="nav-item">
            <el-icon><Goods /></el-icon> <span class="nav-text">商品</span>
          </router-link>
          <router-link to="/exchange-audit" class="nav-item">
            <el-icon><List /></el-icon> <span class="nav-text">兑换</span>
          </router-link>
        </template>

        <!-- 通用/志愿者 -->
        <div class="menu-divider">业务功能</div>
        <router-link to="/honor" class="nav-item">
          <el-icon><Trophy /></el-icon> <span class="nav-text">荣誉</span>
        </router-link>
        <router-link to="/activities" class="nav-item">
          <el-icon><Flag /></el-icon> <span class="nav-text">活动</span>
        </router-link>
        <router-link v-if="userRole === 'VOLUNTEER'" to="/my-records" class="nav-item">
          <el-icon><Calendar /></el-icon> <span class="nav-text">我的</span>
        </router-link>
        <router-link to="/mall" class="nav-item">
          <el-icon><ShoppingCart /></el-icon> <span class="nav-text">商城</span>
        </router-link>
      </nav>
    </div>

    <!-- 右侧主体 -->
    <div class="main-container">
      <header class="top-header">
        <div class="breadcrumb-area hide-on-mobile">
          <el-icon color="#909399" style="margin-right: 5px;"><LocationInformation /></el-icon>
          <span>当前位置：{{ $route.name }}</span>
        </div>

        <div class="user-info">
          <!-- 段位展示 (手机端隐藏，节省空间) -->
          <div class="level-badge hide-on-mobile" v-if="userRole !== 'ADMIN'">
            <el-tooltip content="荣誉段位(由累计总积分决定)" placement="bottom">
              <el-tag effect="dark" :color="currentLevel.color" class="level-tag">
                <span class="level-icon">{{ currentLevel.icon }}</span>
                <span class="level-name">{{ currentLevel.name }}</span>
                <el-divider direction="vertical" />
                <span>   总分: {{ totalPoints }}</span>
              </el-tag>
            </el-tooltip>
            <el-tooltip content="这是您的钱包余额，可去商城兑换商品" placement="bottom">
              <el-tag type="warning" effect="light" class="points-tag">
                <span>余额: {{ currentPoints }}</span>
              </el-tag>
            </el-tooltip>
          </div>

          <!-- 头像展示 -->
          <el-avatar
              :size="32"
              :src="userAvatar || getDefaultAvatar(userUsername)"
              style="margin-right: 10px; border: 1px solid #ddd;"
          />

          <div class="welcome-text hide-on-mobile">
            <span>Hi, </span>
            <span class="user-name">{{ username }}</span>
            <el-tag size="small" effect="plain" round class="role-tag">
              {{ userRole === 'ADMIN' ? '管理员' : '志愿者' }}
            </el-tag>
          </div>

          <!-- 手机端仅保留图标，PC端保留文字 -->
          <el-button type="primary" plain round size="small" @click="router.push('/profile')" :icon="User" class="action-btn">
            <span class="hide-on-mobile">个人中心</span>
          </el-button>

          <el-button type="danger" plain round size="small" @click="handleLogout" :icon="SwitchButton" class="action-btn" style="margin-left: 10px;">
            <span class="hide-on-mobile">退出</span>
          </el-button>
        </div>
      </header>

      <main class="content">
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
import request from '../utils/request';
import { getLevelInfo, getDefaultAvatar } from '../utils/levelRules';
import {
  HomeFilled, DataLine, User, Flag, Calendar,
  Tickets, Bell, Promotion, LocationInformation,
  SwitchButton, Trophy, ShoppingCart, Goods, List
} from '@element-plus/icons-vue';

const router = useRouter();
const route = useRoute();

const username = ref('');
const userRole = ref('');
const userAvatar = ref('');
const userUsername = ref('');
const totalPoints = ref(0);
const currentPoints = ref(0);
const currentLevel = ref({});

const fetchLatestUserInfo = async () => {
  const userId = localStorage.getItem('userId');
  if (!userId) {
    if (route.path !== '/login') router.push('/login');
    return;
  }

  try {
    const res = await request.get(`/api/user/info?userId=${userId}`);
    const user = res.data;

    username.value = user.realName;
    userRole.value = user.role;
    userUsername.value = user.username;
    userAvatar.value = user.avatar;
    totalPoints.value = user.totalPoints || 0;
    currentPoints.value = user.currentPoints || 0;
    currentLevel.value = getLevelInfo(totalPoints.value);
  } catch (error) {
    console.error("获取用户信息失败", error);
    handleLogout();
  }
};

const handleLogout = () => {
  localStorage.clear();
  username.value = '';
  userRole.value = '';
  router.push('/login');
};

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
/* ====================================================
   🖥️ 默认样式 (PC 端)
   ==================================================== */
.admin-layout {
  display: flex;
  height: 100vh;
  background-color: #f5f7fa;
  overflow: hidden; /* 防止外层滚动 */
}

.sidebar {
  width: 220px;
  background: #ffffff;
  box-shadow: 2px 0 8px rgba(0,0,0,0.05);
  display: flex;
  flex-direction: column;
  z-index: 100;
}

.logo-container {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-bottom: 1px solid #f0f0f0;
  color: #ff6b6b;
}
.logo-icon { font-size: 24px; margin-right: 8px; }
.logo-text { font-size: 18px; font-weight: bold; color: #333; }

.nav-menu { flex: 1; padding: 10px; overflow-y: auto; }

.menu-divider {
  font-size: 12px;
  color: #909399;
  margin: 15px 0 5px 15px;
}

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

.nav-item:hover {
  background-color: #ffeaea;
  color: #ff6b6b;
}

.router-link-active {
  background: linear-gradient(90deg, #ff6b6b, #ff8787);
  color: white !important;
  box-shadow: 0 4px 10px rgba(255, 107, 107, 0.3);
}

.main-container { flex: 1; display: flex; flex-direction: column; overflow: hidden; }

.top-header {
  height: 60px;
  background: white;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.05);
  z-index: 10;
}

.breadcrumb-area { display: flex; align-items: center; color: #606266; font-size: 14px; }
.user-info { display: flex; align-items: center; justify-content: flex-end; }
.welcome-text { margin-right: 20px; font-size: 14px; color: #606266; display: flex; align-items: center; }
.user-name { font-weight: bold; color: #333; margin: 0 5px; }

.content {
  flex: 1;
  padding: 20px;
  overflow-y: auto; /* 主内容区独立滚动 */
}

.fade-enter-active, .fade-leave-active { transition: opacity 0.2s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

/* 积分展示样式 */
.level-badge { display: flex; align-items: center; gap: 12px; margin-right: 20px; }
.level-tag, .points-tag { display: inline-flex; align-items: center; justify-content: center; padding: 0 12px; height: 28px; border: none; }
.level-tag { color: white; font-weight: bold; }
.points-tag { font-weight: bold; }
.level-icon { font-size: 14px; margin-right: 6px; line-height: 1; }
.level-name { margin-right: 8px; }
.level-tag .el-divider--vertical { height: 14px; background-color: rgba(255, 255, 255, 0.5); margin: 0; }
.level-tag > span:last-child { margin-left: 8px; }


/* ====================================================
   📱 移动端响应式适配 (屏幕宽度小于 768px 时生效)
   ==================================================== */
@media screen and (max-width: 768px) {

  /* 辅助类：在手机端隐藏元素 */
  .hide-on-mobile {
    display: none !important;
  }

  /* 顶部 Header：更加紧凑 */
  .top-header {
    padding: 0 10px;
    height: 55px;
    justify-content: flex-end; /* 头像和按钮靠右 */
  }

  /* 按钮只留图标，变成圆形 */
  .action-btn {
    width: 32px;
    height: 32px;
    padding: 0;
    border-radius: 50%;
  }

  /* 侧边栏爆改为底部导航栏 (Bottom Tabbar) */
  .sidebar {
    width: 100% !important;
    height: 60px; /* 固定高度 */
    flex-direction: row;
    position: fixed;
    bottom: 0;
    left: 0;
    box-shadow: 0 -2px 10px rgba(0,0,0,0.1);
    /* 让出 iPhone 底部安全区 */
    padding-bottom: env(safe-area-inset-bottom);
  }

  /* 隐藏 Logo 和 分组标题 */
  .logo-container, .menu-divider {
    display: none !important;
  }

  /* 🚨 核心修复 1：导航菜单允许横向滚动，且隐藏滚动条 */
    .nav-menu {
      display: flex;
      width: 100%;
      padding: 0;
      margin: 0;
      justify-content: flex-start; /* 改为从左向右排，而不是 space-around */
      align-items: center;
      overflow-x: auto; /* 允许横向滚动 */
      overflow-y: hidden;
      -webkit-overflow-scrolling: touch; /* iOS 滑动顺畅 */
    }

    /* 隐藏原生滚动条 */
    .nav-menu::-webkit-scrollbar {
      display: none;
    }

    /* 🚨 核心修复 2：禁止图标被挤压！ */
    .nav-item {
      flex-direction: column;
      padding: 8px 12px; /* 增加一点点击区域 */
      margin: 0;
      border-radius: 0;
      font-size: 11px;
      color: #909399;
      flex-shrink: 0; /* 绝对不能收缩！保持原始宽度 */
      width: 65px; /* 给每个按钮一个固定宽度 */
      text-align: center;
    }

    .nav-item .el-icon {
      margin-right: 0;
      margin-bottom: 4px;
      font-size: 22px;
    }



  /* 手机端取消 Hover 效果 */
  .nav-item:hover {
    background-color: transparent;
    color: #909399;
  }

  /* 激活状态：没有渐变背景，只有图标和文字变色 */
  .router-link-active {
    background: transparent !important;
    color: #ff6b6b !important;
    box-shadow: none !important;
  }

  .router-link-active .nav-text {
    font-weight: bold;
  }

  .content {
    /* 为底部导航栏留空 */
    padding: 10px 10px 80px;
    overflow-y: auto;
    overflow-x: hidden; /* 🚨 必须加上，防止内容左右晃动 */
    -webkit-overflow-scrolling: touch; /* 让 iOS 滑动更流畅 */
  }

  /* 隐藏该区域的滚动条 */
  .content::-webkit-scrollbar {
    display: none;
  }


}
</style>