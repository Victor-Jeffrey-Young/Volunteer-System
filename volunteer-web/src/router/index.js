import { createRouter, createWebHistory } from 'vue-router';

const routes = [
    {
        path: '/login',
        name: 'Login',
        component: () => import('../views/Login.vue')
    },
    {
        path: '/',
        component: () => import('../layout/MainLayout.vue'),
        redirect: '/home', // 访问 / 自动转到 /home
        children: [
            {
                path: 'home',
                name: 'Home',
                component: () => import('../views/Home.vue')
            },
            // 🚨 必须放在 children 里面，才能维持后台布局
            {
                path: 'users',
                name: 'UserManage',
                component: () => import('../views/UserManage.vue'),
                meta: { requiresAdmin: true }
            },
            {
                path: 'activities',
                name: 'ActivityManage',
                component: () => import('../views/ActivityManage.vue')
            },
            {
                path: 'my-records',
                name: 'MyRecord',
                component: () => import('../views/MyRecord.vue')
            },
            {
                path: 'registrations',
                name: 'RegistrationManage',
                component: () => import('../views/RegistrationManage.vue'),
                meta: { requiresAdmin: true } // 管理员专属
            },
            {
                path: 'profile',
                name: 'Profile',
                component: () => import('../views/Profile.vue')
            },
            {
                path: 'notices',
                name: 'NoticeManage',
                component: () => import('../views/NoticeManage.vue'),
                meta: { requiresAdmin: true }
            },
            {
                path: 'databoard',
                name: 'DataBoard',
                component: () => import('../views/DataBoard.vue'),
                meta: { requiresAdmin: true }
            },
            {
                path: 'honor',
                name: 'HonorHall',
                component: () => import('../views/HonorHall.vue')
                // 注意：不要加 meta: { requiresAdmin: true }，因为志愿者也要看
            },
            {
                path: 'mall',
                name: 'PointsMall',
                component: () => import('../views/PointsMall.vue')
                // 这个所有人都能看，不用加 meta
            },
            {
                path: 'goods-manage',
                name: 'GoodsManage',
                component: () => import('../views/GoodsManage.vue'),
                meta: { requiresAdmin: true } // 仅管理员可见
            },
            {
                path: 'exchange-audit', // 起个专业点的名字：兑换审计
                name: 'ExchangeAudit',
                component: () => import('../views/ExchangeManage.vue'),
                meta: { requiresAdmin: true }
            }

        ]
    }
];

const router = createRouter({
    history: createWebHistory(),
    routes
});

// 路由守卫
// 全局前置路由守卫 - 终极修复版
router.beforeEach((to, from, next) => {
    const token = localStorage.getItem('token');
    const role = localStorage.getItem('role');

    // 1. 目的地是登录页，直接放行
    // 无论有没有 token，都允许用户访问登录页
    if (to.path === '/login') {
        next();
        return; // 结束执行
    }

    // 2. 没有 token，且目的地不是登录页，强制跳转到登录页
    if (!token) {
        next('/login');
        return; // 结束执行
    }

    // 3. 有 token，但想访问管理员专属页面，权限却不够
    if (to.meta.requiresAdmin && role !== 'ADMIN') {
        ElMessage.error('权限不足，无法访问该页面！');
        next(from.path); // 留在原地，或者弹回首页 next('/home');
        return; // 结束执行
    }

    // 4. 其他所有情况（有 token 且权限足够），一律放行
    next();
});

export default router;