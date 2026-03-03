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