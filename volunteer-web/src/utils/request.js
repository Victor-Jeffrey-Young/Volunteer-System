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
        // 仅携带 token（真正被后端信任的凭证）。
        // 不再从 localStorage 注入 Role / userId 请求头——身份只由后端解析 JWT，
        // 客户端头里伪造的角色/用户 ID 一律无效。
        const token = localStorage.getItem('token');
        if (token) {
            config.headers['Authorization'] = token;
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