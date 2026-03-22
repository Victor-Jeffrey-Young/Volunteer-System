/**
 * 文件路径处理工具
 */

/**
 * 获取完整的文件访问路径
 * @param {string} path 数据库存储的路径
 * @param {string} defaultType 默认图类型：avatar (头像), cover (封面)
 * @returns {string} 可直接访问的 URL
 */
export const getFullAvatar = (path, defaultType = 'avatar') => {
  // 1. 处理空路径：返回默认占位图
  if (!path) {
    if (defaultType === 'avatar') {
      return '/default-Avatar.png';
    }
    return '/Others.png';
  }

  // 2. 处理已包含完整域名的路径
  if (path.startsWith('http')) {
    return path;
  }

  // 3. 🚨 核心逻辑：防止重复拼接 /files/
  // 如果路径已经以 /files/ 开头，则直接返回（加上 / 确保路径正确）
  if (path.startsWith('/files/') || path.startsWith('files/')) {
    return path.startsWith('/') ? path : `/${path}`;
  }

  // 4. 处理旧版本数据：如果只有文件名，则补全前缀
  // 假设未带前缀的默认归类到 avatar 目录下（保持兼容性）
  return `/files/avatar/${path}`;
};
