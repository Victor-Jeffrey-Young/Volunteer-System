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
router.beforeEach((to, from, next) => {
    const token = localStorage.getItem('token');
    const role = localStorage.getItem('role');

    // 1. 如果是去登录页，直接放行（这是退出成功的关键）
    if (to.path === '/login') {
        next();
        return;
    }

    // 2. 如果没有 token 且不是去登录页，强制去登录
    if (!token) {
        next('/login');
        return;
    }

    // 3. 权限判断
    if (to.meta.requiresAdmin && role !== 'ADMIN') {
        alert('权限不足！');
        next('/home');
        return;
    }

    // 4. 其余情况放行
    next();
});

export default router;