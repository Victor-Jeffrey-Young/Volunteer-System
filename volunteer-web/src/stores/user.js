import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import { authApi, userApi } from '../api/modules';

export const useUserStore = defineStore('user', () => {
  const user = ref(null);
  const token = ref(localStorage.getItem('token') || '');
  const role = ref(localStorage.getItem('role') || '');
  const userId = ref(localStorage.getItem('userId') || '');

  const isLoggedIn = computed(() => !!token.value);

  // 设置 token
  const setToken = (newToken) => {
    token.value = newToken;
    localStorage.setItem('token', newToken);
  };

  // 设置角色
  const setRole = (newRole) => {
    role.value = newRole;
    localStorage.setItem('role', newRole);
  };

  // 设置用户信息本体
  const setUser = (userData) => {
    user.value = userData;
    if (userData) {
      if (userData.realName) localStorage.setItem('realName', userData.realName);
      if (userData.currentPoints !== undefined) localStorage.setItem('points', userData.currentPoints);
      if (userData.avatar) localStorage.setItem('avatar', userData.avatar);
      if (userData.userId) {
        userId.value = userData.userId;
        localStorage.setItem('userId', userData.userId);
      }
    }
  };

  // 登录核心逻辑
  const login = async (username, password) => {
    const res = await authApi.login({ username, password });
    const data = res.data;
    
    // 1. 同步状态到 Pinia 和 LocalStorage
    setToken(data.token);
    setRole(data.role);
    userId.value = data.userId;
    localStorage.setItem('userId', data.userId);
    
    // 2. 存储用户基本对象
    setUser(data);
    
    return data;
  };

  // 获取当前详细用户信息 (供个人中心等页面使用)
  const fetchCurrentUser = async () => {
    // 确保有 ID 才能查
    const id = userId.value || localStorage.getItem('userId');
    if (!token.value || !id) return;
    
    try {
      const res = await userApi.getUserInfo(id);
      setUser(res.data);
    } catch (error) {
      console.error('获取用户信息失败:', error);
    }
  };

  // 登出清理逻辑
  const logout = () => {
    user.value = null;
    token.value = '';
    role.value = '';
    userId.value = '';
    
    // 不清空所有缓存，只删除认证相关的关键信息
    localStorage.removeItem('token');
    localStorage.removeItem('role');
    localStorage.removeItem('userId');
    localStorage.removeItem('realName');
    localStorage.removeItem('avatar');
    localStorage.removeItem('points');
  };

  return {
    user,
    token,
    role,
    userId,
    isLoggedIn,
    setUser,
    setToken,
    setRole,
    login,
    logout,
    fetchCurrentUser,
  };
});
