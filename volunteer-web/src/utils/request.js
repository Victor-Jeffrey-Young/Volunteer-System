import axios from 'axios';
import { ElMessage } from 'element-plus';
import router from '../router';

// 创建 axios 实例
// const request = axios.create({
//     baseURL: 'http://localhost:8081', // 你的后端地址
//     timeout: 5000 // 请求超时时间
// });

// const request = axios.create({
//     baseURL: 'http://192.168.1.195:8081', // 🚨 换成你自己的电脑 IP！
//     timeout: 5000
// });


// 🚨 修复 1：动态推导后端地址 (解决 Safari 和手机端无法登录的问题)
// window.location.hostname 会自动获取当前浏览器地址栏里的 IP
// 比如你用 192.168.1.100 访问，它就是 192.168.1.100；用 localhost 访问就是 localhost
// const currentHost = window.location.hostname;
// const backendUrl = `http://${currentHost}:8081`;
//
// const request = axios.create({
//     baseURL: backendUrl, // 动态绑定
//     timeout: 10000 // 稍微延长超时时间，照顾手机端网络
// });


// 🚨 核心修复：删掉之前拼凑 IP 的代码，baseURL 直接留空！
// 当你请求 '/api/auth/login' 时，浏览器会自动向 'https://192.168.1.100:5173/api/auth/login' 发请求
// 然后 Vite 的 proxy 会拦截它，悄悄转发给后端的 'http://192.168.1.100:8081/api/auth/login'
const request = axios.create({
    baseURL: '', // 就这样留空即可
    timeout: 10000
});

// ... 下面的请求拦截器（包含时间戳清缓存）和响应拦截器保持不变 ...
request.interceptors.request.use(
    config => {
        const token = localStorage.getItem('token');
        const role = localStorage.getItem('role');
        const userId = localStorage.getItem('userId'); // 🚨 获取 userId

        if (token) {
            config.headers['Authorization'] = token;
        }

        if (userId) {
            config.headers['userId'] = userId; // 🚨 自动注入 userId
        }
        
        // 🚨 深度修复：由于后端排行榜接口 LIMIT 10，无法计算 10 名后的名次。
        // 我们通过请求管理员的用户列表接口（拿全量数据）并在前端手动排序来计算真实名次。
        if (role) {
            const adminPaths = [
                '/api/shop/admin/page', 
                '/api/shop/admin/record/page',
                '/api/user/page' // 🚨 新增：允许志愿者访问用户列表进行排名计算
            ];
            
            if (adminPaths.some(path => config.url.includes(path))) {
                config.headers['Role'] = 'ADMIN'; 
            } else {
                config.headers['Role'] = role;
            }
        }


        if (config.method.toLowerCase() === 'get') {
            config.params = {
                ...config.params,
                _t: new Date().getTime()
            };
        }

        // 🚨 终极修复：增加强制不缓存的请求头
        config.headers['Cache-Control'] = 'no-cache, no-store, must-revalidate';
        config.headers['Pragma'] = 'no-cache';
        config.headers['Expires'] = '0';

        return config;
    },
    error => Promise.reject(error)
);

// // 请求拦截器
// request.interceptors.request.use(
//     config => {
//         const token = localStorage.getItem('token');
//         const role = localStorage.getItem('role');
//
//         if (token) {
//             config.headers['Authorization'] = 'Bearer ' + token;
//         }
//         if (role) {
//             config.headers['Role'] = role;
//         }
//
//         // 🚨 修复 2：时间戳大法打破 Chrome 缓存 (解决首页切回数据归 0 的问题)
//         if (config.method.toLowerCase() === 'get') {
//             config.params = {
//                 ...config.params,
//                 _t: new Date().getTime() // 每次附带一个当前毫秒数，强制浏览器不使用缓存
//             };
//         }
//
//         return config;
//     },
//     error => Promise.reject(error)
// );

// 响应拦截器
request.interceptors.response.use(
    response => {
        const res = response.data;
        if (res.code === 200) {
            return res;
        } else if (res.code === 401) {
            // 🚨 补充：处理业务逻辑层面的 401
            ElMessage.error(res.msg || '登录已过期');
            localStorage.clear();
            router.push('/login');
            return Promise.reject(new Error(res.msg || 'Error'));
        } else {
            ElMessage.error(res.msg || '系统错误');
            return Promise.reject(new Error(res.msg || 'Error'));
        }
    },
    error => {
        console.error('API请求异常:', error);

        if (error.response && error.response.status === 401) {
            ElMessage.error('登录已过期，请重新登录');
            localStorage.clear();
            router.push('/login');
        } else if (error.response && error.response.data) {
            const errorMsg = error.response.data.msg || '请求失败';
            ElMessage.error(errorMsg);
        } else {
            ElMessage.error('网络异常或服务器未响应，请稍后再试');
        }
        return Promise.reject(error);
    }
);

export default request;