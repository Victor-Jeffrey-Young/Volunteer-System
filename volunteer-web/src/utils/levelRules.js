/**
 * 根据积分计算段位信息
 * @param {Number} points 积分
 * @returns {Object} { name: '段位名', type: 'ElementPlus颜色类型', color: '自定义Hex颜色' }
 */
export const getLevelInfo = (points) => {
    if (!points) points = 0;

    if (points < 100) {
        return { name: '⭐ 一星志愿者', type: 'info', color: '#909399' }; // 青铜灰
    } else if (points < 300) {
        return { name: '🌙 二星志愿者', type: 'success', color: '#67C23A' }; // 森林绿
    } else if (points < 600) {
        return { name: '☀️ 三星志愿者', type: 'primary', color: '#409EFF' }; // 海洋蓝
    } else if (points < 1000) {
        return { name: '💎 钻石志愿者', type: 'warning', color: '#E6A23C' }; // 钻石金
    } else {
        return { name: '👑 荣耀皇冠', type: 'danger', color: '#F56C6C' }; // 王者红
    }
};

/**
 * 获取默认头像
 * 🚨 关键：必须传入 username (账号)，不要传 realName，因为账号是唯一的且英文的，生成的头像更稳定
 */
export const getDefaultAvatar = (username) => {
    // 加上判空，防止 username 为空时头像乱变
    const seed = username || 'default_user';
    return `https://api.dicebear.com/7.x/miniavs/svg?seed=${seed}`;
};