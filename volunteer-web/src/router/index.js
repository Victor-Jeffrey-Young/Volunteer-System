import { createRouter, createWebHistory } from 'vue-router';
import { ElMessage } from 'element-plus';
import { useUserStore } from '../stores/user';

// 按角色返回对应首页，角色来自服务端回灌（JWT 签发，不被 localStorage 篡改）
// 同时供根路径分流与 404 页面的“返回首页”按钮复用
export function roleHome(role) {
    if (role === 'ADMIN') return '/admin/home';
    if (role === 'VOLUNTEER') return '/volunteer/home';
    if (role === 'RESIDENT') return '/resident/wishes';
    return '/login';
}

const routes = [
    {
        path: '/login',
        name: 'Login',
        component: () => import('../views/Login.vue')
    },
    {
        // 根路径分流：角色以 userStore.role 为准（服务端回灌）
        path: '/',
        redirect: () => roleHome(useUserStore().role)
    },
    {
        // 管理员后台路由分组
        path: '/admin',
        component: () => import('../layout/MainLayout.vue'),
        redirect: '/admin/home',
        meta: { requiresRole: 'ADMIN' },
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
        // 志愿者路由分组
        path: '/volunteer',
        component: () => import('../layout/VolunteerLayout.vue'),
        redirect: '/volunteer/home',
        meta: { requiresRole: 'VOLUNTEER' },
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
    },
    {
        // 兜底路由：必须放在最后，未匹配到任何页面时渲染 404
        path: '/:pathMatch(.*)*',
        name: 'NotFound',
        component: () => import('../views/NotFound.vue')
    }
];

const router = createRouter({
    history: createWebHistory(),
    routes
});

// 全局前置路由守卫
router.beforeEach(async (to, from, next) => {
    const userStore = useUserStore();
    const token = userStore.token || localStorage.getItem('token');

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

    // 3. 身份以服务端回灌为准：刷新/直达时先拉取当前用户，
    //    角色判定不看 localStorage，杜绝伪造 role 切换界面
    await userStore.loadCurrentUser().catch(() => {});

    const role = userStore.role;

    // 4. 根路径兜底分流
    if (to.path === '/') {
        next(roleHome(role));
        return;
    }

    // 5. 严格角色校验 (RBAC)
    const requiredRole = to.meta.requiresRole;

    // 如果目标页面要求特定角色
    if (requiredRole) {
        if (role !== requiredRole) {
            ElMessage.error(`权限不足！您的角色是 ${role}，无法访问该区域`);
            next(roleHome(role));
            return;
        }
    }

    next();
});

export default router;
