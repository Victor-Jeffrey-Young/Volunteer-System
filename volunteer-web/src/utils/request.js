import axios from 'axios';

// 创建 axios 实例
// const request = axios.create({
//     baseURL: 'http://localhost:8081', // 你的后端地址
//     timeout: 5000 // 请求超时时间
// });

const request = axios.create({
    baseURL: 'http://192.168.1.195:8081', // 🚨 换成你自己的电脑 IP！
    timeout: 5000
});

// 请求拦截器（未来这里会自动给请求头加上 token）
request.interceptors.request.use(
    config => {
        const token = localStorage.getItem('token');
        const role = localStorage.getItem('role'); // 读取角色

        if (token) {
            config.headers['Authorization'] = 'Bearer ' + token;
        }
        if (role) {
            config.headers['Role'] = role; // 🚨 这样后端就能收到 Role 头了
        }
        return config;
    },
    error => Promise.reject(error)
);

// 响应拦截器（统一处理后端的 Result 对象）
request.interceptors.response.use(
    response => {
        const res = response.data;
        // 如果 code 是 200，说明成功，直接返回 data
        if (res.code === 200) {
            return res;
        } else {
            // 否则抛出错误提示
            alert(res.msg || '系统错误');
            return Promise.reject(new Error(res.msg || 'Error'));
        }
    },
    error => {
        alert('网络异常，请稍后再试');
        return Promise.reject(error);
    }
);

export default request;