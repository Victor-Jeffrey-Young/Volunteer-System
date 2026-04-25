<template>
  <div class="admin-layout">
    <!-- 移动端遮罩层 -->
    <div 
      v-if="mobileMenuOpen" 
      class="mobile-backdrop"
      @click="mobileMenuOpen = false"
    ></div>

    <!-- 侧边栏 -->
    <div class="sidebar" :class="{ 'collapsed': isCollapsed, 'mobile-open': mobileMenuOpen }">
      <div class="logo-container">
        <div class="logo-circle">
          <Heart class="logo-icon" />
        </div>
        <span v-if="!isCollapsed || (isMobile && mobileMenuOpen)" class="logo-text">社区志愿 管理中心</span>
      </div>

      <nav class="nav-menu">
        <div 
          v-for="item in menuItems" 
          :key="item.path"
          @click="navigateTo(item.path)"
          class="nav-item"
          :class="{ 'active': currentPath.startsWith(item.path) }"
        >
          <component :is="item.icon" class="nav-icon" />
          <span v-if="!isCollapsed || (isMobile && mobileMenuOpen)">{{ item.name }}</span>
        </div>
      </nav>

      <div class="sidebar-footer hide-on-mobile" @click="isCollapsed = !isCollapsed">
        <ChevronLeft v-if="!isCollapsed" class="w-5 h-5" />
        <ChevronRight v-else class="w-5 h-5" />
      </div>
    </div>

    <!-- 主体区域 -->
    <div class="main-container">
      <header class="admin-header">
        <div class="header-left flex items-center gap-4">
          <!-- 移动端菜单按钮 -->
          <button 
            class="mobile-menu-btn" 
            @click="mobileMenuOpen = !mobileMenuOpen"
          >
            <Menu class="w-6 h-6" />
          </button>
          <h2 class="page-title">{{ currentPageName }}</h2>
        </div>
        <div class="header-right">
          <div class="user-info">
            <div class="user-details hide-on-mobile">
              <p class="user-name">{{ realName }}</p>
              <p class="user-role">系统管理员</p>
            </div>
            <el-dropdown trigger="click">
              <div class="avatar-wrapper">
                <img :src="getFullAvatar(avatar)" class="admin-avatar" />
              </div>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="router.push('/admin/profile')">账号设置</el-dropdown-item>
                  <el-dropdown-item divided @click="handleLogout" style="color: #ef4444;">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>
      </header>

      <main class="admin-content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useUserStore } from '../stores/user';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  LayoutDashboard,
  CalendarDays,
  Users,
  ClipboardCheck,
  ShoppingBag,
  Bell,
  Settings,
  Heart,
  ChevronLeft,
  ChevronRight,
  UserCheck,
  Menu,
  X,
  HeartHandshake
} from 'lucide-vue-next';
import { getFullAvatar } from '../utils/file';

const route = useRoute();
const router = useRouter();
const isCollapsed = ref(false);
const mobileMenuOpen = ref(false);
const isMobile = ref(window.innerWidth <= 768);

const handleResize = () => {
  isMobile.value = window.innerWidth <= 768;
  if (!isMobile.value) mobileMenuOpen.value = false;
};

const currentPath = computed(() => route.path);

const userStore = useUserStore();
const realName = computed(() => userStore.user?.realName || localStorage.getItem('realName') || '管理员');
const avatar = computed(() => userStore.user?.avatar || localStorage.getItem('avatar'));

const menuItems = [
  { name: '工作台', path: '/admin/home', icon: LayoutDashboard },
  { name: '活动管理', path: '/admin/activities-manage', icon: CalendarDays },
  { name: '报名审核', path: '/admin/registrations', icon: UserCheck },
  { name: '用户管理', path: '/admin/users', icon: Users },
  { name: '商城管理', path: '/admin/goods-manage', icon: ShoppingBag },
  { name: '兑换核销', path: '/admin/exchange-audit', icon: ClipboardCheck },
  { name: '公告发布', path: '/admin/notices', icon: Bell },
  { name: '微心愿审计', path: '/admin/wishes', icon: HeartHandshake }
];

const currentPageName = computed(() => {
  const item = menuItems.find(m => currentPath.value.includes(m.path));
  return item ? item.name : '后台管理';
});

const navigateTo = (path) => {
  router.push(path);
  if (isMobile.value) mobileMenuOpen.value = false; // 点击后自动关闭菜单
};

const handleLogout = () => {
  ElMessageBox.confirm('确定要退出管理系统吗？', '提示', { type: 'warning' }).then(() => {
    localStorage.clear();
    router.push('/login');
    ElMessage.success('已安全退出');
  });
};

onMounted(() => {
  window.addEventListener('resize', handleResize);
  userStore.fetchCurrentUser();
});

onUnmounted(() => {
  window.removeEventListener('resize', handleResize);
});
</script>

<style scoped>
.admin-layout {
  display: flex;
  height: 100vh;
  background-color: #f8fafc;
  color: #1e293b;
  position: relative;
}

/* 侧边栏样式 */
.sidebar {
  width: 260px;
  background-color: #ffffff;
  border-right: 1px solid #e2e8f0;
  display: flex;
  flex-direction: column;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  z-index: 1000;
}

.sidebar.collapsed {
  width: 80px;
}

.logo-container {
  padding: 24px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo-circle {
  width: 40px;
  height: 40px;
  background: linear-gradient(135deg, #f97316, #ea580c);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(234, 88, 12, 0.2);
}

.logo-icon {
  color: white;
  width: 22px;
  height: 22px;
  fill: currentColor;
}

.logo-text {
  font-size: 18px;
  font-weight: 800;
  background: linear-gradient(to right, #1e293b, #475569);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  white-space: nowrap;
}

.nav-menu {
  flex: 1;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-radius: 12px;
  cursor: pointer;
  color: #64748b;
  transition: all 0.2s;
  white-space: nowrap;
  font-weight: 500;
}

.nav-item:hover {
  background-color: #fff7ed;
  color: #f97316;
}

.nav-item.active {
  background-color: #f97316;
  color: white;
  box-shadow: 0 4px 12px rgba(249, 115, 22, 0.25);
}

.nav-icon {
  width: 20px;
  height: 20px;
}

.sidebar-footer {
  padding: 16px;
  border-top: 1px solid #f1f5f9;
  display: flex;
  justify-content: center;
  cursor: pointer;
  color: #94a3b8;
}

/* 主体内容样式 */
.main-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  width: 100%;
}

.admin-header {
  height: 72px;
  background-color: white;
  border-bottom: 1px solid #e2e8f0;
  padding: 0 32px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.page-title {
  font-size: 20px;
  font-weight: 700;
  color: #0f172a;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 16px;
}

.user-details {
  text-align: right;
}

.user-name {
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
}

.user-role {
  font-size: 12px;
  color: #94a3b8;
}

.avatar-wrapper {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  overflow: hidden;
  border: 2px solid #fff7ed;
  cursor: pointer;
}

.admin-avatar {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.admin-content {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
}

/* 📱 移动端深度适配样式 */
.mobile-menu-btn {
  display: none;
  background: none;
  border: none;
  color: #64748b;
  cursor: pointer;
  padding: 4px;
}

.mobile-backdrop {
  display: none;
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.5);
  backdrop-filter: blur(4px);
  z-index: 999;
}

@media (max-width: 768px) {
  .mobile-menu-btn {
    display: block;
  }

  .mobile-backdrop {
    display: block;
  }

  .sidebar {
    position: fixed;
    height: 100%;
    left: 0;
    top: 0;
    transform: translateX(-100%);
    box-shadow: 20px 0 50px rgba(0,0,0,0.1);
  }

  .sidebar.mobile-open {
    transform: translateX(0);
    width: 280px;
  }

  .admin-header {
    padding: 0 16px;
  }

  .hide-on-mobile {
    display: none;
  }

  .admin-content {
    padding: 12px;
  }
  
  .page-title {
    font-size: 16px;
  }
}
</style>