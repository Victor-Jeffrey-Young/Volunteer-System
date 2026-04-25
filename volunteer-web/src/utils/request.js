import axios from 'axios';
import { ElMessage } from 'element-plus';
import router from '../router';

const request = axios.create({
    baseURL: '', // 就这样留空即可
    timeout: 10000
});

// ... 下面的请求拦截器（包含时间戳清缓存）和响应拦截器保持不变 ...
request.interceptors.request.use(
    config => {
        const token = localStorage.getItem('token');
        const role = localStorage.getItem('role');
        const userId = localStorage.getItem('userId');  // 获取 userId

        if (token) {
            config.headers['Authorization'] = token;
        }

        if (userId) {
            config.headers['userId'] = userId;                       // 自动注入 userId
        }

        // 我们通过请求管理员的用户列表接口（拿全量数据）并在前端手动排序来计算真实名次。
        if (role) {
            const adminPaths = [
                '/api/shop/admin/page', 
                '/api/shop/admin/record/page',
                '/api/user/page'
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

        // 增加强制不缓存的请求头
        config.headers['Cache-Control'] = 'no-cache, no-store, must-revalidate';
        config.headers['Pragma'] = 'no-cache';
        config.headers['Expires'] = '0';

        return config;
    },
    error => Promise.reject(error)
);

// 响应拦截器
request.interceptors.response.use(
    response => {
        const res = response.data;
        if (res.code === 200) {
            return res;
        } else if (res.code === 401) {
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