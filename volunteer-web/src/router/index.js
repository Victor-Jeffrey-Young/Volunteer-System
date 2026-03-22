import { createRouter, createWebHistory } from 'vue-router';
import { ElMessage } from 'element-plus';

const routes = [
    {
        path: '/login',
        name: 'Login',
        component: () => import('../views/Login.vue')
    },
    {
        // 根路径分流逻辑
        path: '/',
        redirect: () => {
            const role = localStorage.getItem('role');
            if (!role) return '/login';
            if (role === 'ADMIN') return '/admin/home';
            if (role === 'VOLUNTEER') return '/volunteer/home';
            if (role === 'RESIDENT') return '/resident/home';
            return '/login';
        }
    },
    {
        // 管理员后台路由分组
        path: '/admin',
        component: () => import('../layout/MainLayout.vue'),
        redirect: '/admin/home',
        meta: { requiresRole: 'ADMIN' }, // 🚨 标注必须是管理员
        children: [
            { path: 'home', name: 'Home', component: () => import('../views/admin/AdminDashboard.vue') },
            { path: 'users', name: 'UserManage', component: () => import('../views/admin/AdminUserManage.vue') },
            { path: 'activities-manage', name: 'ActivityManage', component: () => import('../views/admin/AdminActivityManage.vue') },
            { path: 'registrations', name: 'RegistrationManage', component: () => import('../views/admin/AdminRegistrationAudit.vue') },
            { path: 'notices', name: 'NoticeManage', component: () => import('../views/admin/AdminNoticeManage.vue') },
            { path: 'databoard', name: 'DataBoard', component: () => import('../views/admin/AdminDataBoard.vue') },
            { path: 'goods-manage', name: 'GoodsManage', component: () => import('../views/admin/AdminGoodsManage.vue') },
            { path: 'exchange-audit', name: 'ExchangeAudit', component: () => import('../views/admin/AdminExchangeVerify.vue') },
            { path: 'wishes', name: 'WishManage', component: () => import('../views/admin/AdminWishManage.vue') },
            { path: 'profile', name: 'Profile', component: () => import('../views/admin/AdminAccountSettings.vue') } 
        ]
    },
    {
        // 志愿者前端路由分组
        path: '/volunteer',
        component: () => import('../layout/VolunteerLayout.vue'),
        redirect: '/volunteer/home',
        meta: { requiresRole: 'VOLUNTEER' }, // 🚨 标注必须是志愿者
        children: [
            { path: 'home', name: 'VolunteerHome', component: () => import('../views/volunteer/VolunteerHome.vue') },
            { path: 'activities', name: 'VolunteerActivities', component: () => import('../views/volunteer/VolunteerActivityList.vue') },
            { path: 'mall', name: 'VolunteerMall', component: () => import('../views/volunteer/VolunteerPointsMall.vue') },
            { path: 'wishes', name: 'VolunteerWishes', component: () => import('../views/volunteer/VolunteerWishWall.vue') },
            { path: 'leaderboard', name: 'VolunteerLeaderboard', component: () => import('../views/volunteer/VolunteerLeaderboard.vue') },
            { path: 'profile', name: 'VolunteerProfile', component: () => import('../views/volunteer/VolunteerProfile.vue') },
            { path: 'announcements', name: 'VolunteerAnnouncements', component: () => import('../views/volunteer/VolunteerAnnouncements.vue') }
        ]
    },
    {
        // 居民端路由分组
        path: '/resident',
        component: () => import('../layout/ResidentLayout.vue'),
        redirect: '/resident/wishes',
        meta: { requiresRole: 'RESIDENT' },
        children: [
            { path: 'home', name: 'ResidentHome', component: () => import('../views/resident/ResidentHome.vue') },
            { path: 'wishes', name: 'ResidentWishes', component: () => import('../views/resident/ResidentWishManage.vue') },
            { path: 'profile', name: 'ResidentProfile', component: () => import('../views/resident/ResidentProfile.vue') },
        ]
    }
];

const router = createRouter({
    history: createWebHistory(),
    routes
});

// 🛡️ 企业级全局前置路由守卫
router.beforeEach((to, from, next) => {
    const token = localStorage.getItem('token');
    const role = localStorage.getItem('role');

    // 1. 公开页面处理
    if (to.path === '/login') {
        next();
        return;
    }

    // 2. 未登录拦截
    if (!token) {
        next('/login');
        return;
    }

    // 3. 严格角色校验 (RBAC)
    const requiredRole = to.meta.requiresRole;
    
    // 如果目标页面要求特定角色
    if (requiredRole) {
        if (role !== requiredRole) {
            ElMessage.error(`权限不足！您的角色是 ${role}，无法访问该区域`);
            // 互斥跳转：管理员去后台，志愿者去前台，居民去居民端
            if (role === 'ADMIN') return next('/admin/home');
            if (role === 'VOLUNTEER') return next('/volunteer/home');
            if (role === 'RESIDENT') return next('/resident/wishes');
            return next('/login');
        }
    }

    // 4. 默认分流（如果访问根路径或其他未定义路径）
    if (to.path === '/') {
        if (role === 'ADMIN') return next('/admin/home');
        if (role === 'VOLUNTEER') return next('/volunteer/home');
        if (role === 'RESIDENT') return next('/resident/wishes');
        return next('/login');
    }

    next();
});

export default router;
