<template>
  <div class="admin-layout">
    <!-- 左侧边栏 -->
    <div class="sidebar">
      <div class="logo">志愿管理系统</div>
      <nav>
        <router-link to="/home" class="nav-item">🏠 系统首页</router-link>

        <!-- 🚨 确认这里的 userRole 是否等于 'ADMIN' -->
        <router-link v-if="userRole === 'ADMIN'" to="/users" class="nav-item">👥 用户管理</router-link>

        <router-link to="/activities" class="nav-item">🎉 活动管理</router-link>
      </nav>
    </div>

    <!-- 右侧主体 -->
    <div class="main-container">
      <header class="top-header">
        <span class="breadcrumb">当前位置：{{ $route.name }}</span>
        <div class="user-info">
          <span>欢迎您，{{ username }}</span>
          <button @click="handleLogout" class="logout-btn">退出登录</button>
        </div>
      </header>

      <main class="content">
        <!-- 路由出口：子页面会在这里渲染 -->
        <router-view />
      </main>
    </div>

  </div>
</template>


<script setup>
import { ref, onMounted, watchEffect } from 'vue'; // 引入 watchEffect
import { useRouter, useRoute } from 'vue-router';

const router = useRouter();
const route = useRoute();

const username = ref('');
const userRole = ref('');

// 使用函数来重新同步状态
const syncUserInfo = () => {
  const name = localStorage.getItem('realName');
  const role = localStorage.getItem('role');

  // 打印日志到控制台，用于调试！
  console.log("当前缓存中的角色:", role);
  console.log("当前缓存中的姓名:", name);

  if (name && role) {
    username.value = name;
    // 🚨 强制转大写比对，防止大小写坑
    userRole.value = role.toUpperCase();
  } else if (route.path !== '/login') {
    router.push('/login');
  }
};

onMounted(() => {
  syncUserInfo();
});

// 监听路由变化，确保在不同页面间切换时信息是最新的
watchEffect(() => {
  if (route.path !== '/login') {
    syncUserInfo();
  }
});

const handleLogout = () => {
  localStorage.clear();
  username.value = '';
  userRole.value = '';
  router.push('/login');
};
</script>

<style scoped>
.admin-layout { display: flex; height: 100vh; background: #f0f2f5; }
.sidebar { width: 200px; background: #001529; color: white; }
.logo { padding: 20px; font-size: 18px; font-weight: bold; text-align: center; border-bottom: 1px solid #002140; }
.nav-item { display: block; padding: 15px 20px; color: #a6adb4; text-decoration: none; }
.nav-item:hover, .router-link-active { background: #1890ff; color: white; }

.main-container { flex: 1; display: flex; flex-direction: column; overflow: hidden; }
.top-header { height: 60px; background: white; display: flex; align-items: center; justify-content: space-between; padding: 0 20px; box-shadow: 0 1px 4px rgba(0,21,41,.08); }
.content { flex: 1; padding: 20px; overflow-y: auto; }
.logout-btn { margin-left: 15px; border: none; background: none; color: #ff4d4f; cursor: pointer; }
</style>