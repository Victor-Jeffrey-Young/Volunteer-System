import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import { authApi, userApi } from '../api/modules';

export const useUserStore = defineStore('user', () => {
  const user = ref(null);
  // 唯一的本地长期凭证是 token（带签名、客户端不可篡改）。
  // role / userId 一律以服务端为准：登录响应签发，刷新时由 /api/user/info 回灌，
  // 绝不从 localStorage 读取，杜绝伪造身份切换界面/提权。
  const token = ref(localStorage.getItem('token') || '');
  const role = ref('');
  const userId = ref(null);

  const isLoggedIn = computed(() => !!token.value);

  // 设置 token 并持久化（token 是真正被后端信任的凭证）
  const setToken = (newToken) => {
    token.value = newToken;
    localStorage.setItem('token', newToken);
  };

  // role 仅存内存，刷新后由服务端回灌，不再写入 localStorage
  const setRole = (newRole) => {
    role.value = newRole;
  };

  // 用服务端返回的用户数据更新状态；role/userId 只存内存，
  // 其余仅作为展示型缓存（非安全字段）
  const setUser = (userData) => {
    user.value = userData;
    if (userData) {
      role.value = userData.role || role.value;
      userId.value = userData.userId ?? userId.value;
      if (userData.realName) localStorage.setItem('realName', userData.realName);
      if (userData.currentPoints !== undefined) localStorage.setItem('points', userData.currentPoints);
      if (userData.avatar) localStorage.setItem('avatar', userData.avatar);
    }
  };

  // 登录：身份取自登录响应（服务端签发），仅 token 持久化
  const login = async (username, password) => {
    const res = await authApi.login({ username, password });
    const data = res.data;
    setToken(data.token);
    setUser(data);
    return data;
  };

  // 主动刷新用户信息（供页面 onMounted / 资料更新后同步全局状态）。
  // 后端从 JWT 解析身份，返回的就是当前登录用户，无需再传 userId。
  const fetchCurrentUser = async () => {
    if (!token.value) return;
    try {
      const res = await userApi.getUserInfo();
      if (res.data) setUser(res.data);
    } catch (err) {
      console.error('获取用户信息失败:', err);
    }
  };

  // 幂等加载：供路由守卫 / 应用启动时调用，身份已就绪则直接返回
  const loadCurrentUser = () => {
    if (!token.value) return Promise.resolve();
    if (user.value && role.value) return Promise.resolve();
    return fetchCurrentUser();
  };

  // 登出清理
  const logout = () => {
    user.value = null;
    token.value = '';
    role.value = '';
    userId.value = null;
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
    loadCurrentUser,
  };
});