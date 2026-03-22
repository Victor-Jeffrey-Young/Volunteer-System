/**
 * 根据积分计算段位信息
 * @param {Number} points 积分
 * @returns {Object} { name: '段位名', type: 'ElementPlus颜色类型', color: '自定义Hex颜色' }
 */
export const getLevelInfo = (points) => {
    if (!points) points = 0;

    if (points < 100) {
        return { icon: '🌱', name: 'V1 新星志愿者', type: 'info', color: '#94a3b8' };
    } else if (points < 300) {
        return { icon: '🔥', name: 'V2 进阶志愿者', type: 'success', color: '#10b981' };
    } else if (points < 600) {
        return { icon: '⚡', name: 'V3 资深志愿者', type: 'primary', color: '#3b82f6' };
    } else if (points < 1000) {
        return { icon: '💎', name: 'V4 达人志愿者', type: 'warning', color: '#f59e0b' };
    } else {
        return { icon: '👑', name: 'V5 卓越志愿者', type: 'danger', color: '#ef4444' };
    }
};

/**
 * 获取默认头像
 * 🚨 关键：必须传入 username (账号)，不要传 realName，因为账号是唯一的且英文的，生成的头像更稳定
 */
export const getDefaultAvatar = (username) => {
    const seed = username || 'default_user';
    // 🚨 将 7.x 改为最新的 9.x，避开旧服务器的宕机节点
    return `https://api.dicebear.com/9.x/miniavs/svg?seed=${seed}`;
};