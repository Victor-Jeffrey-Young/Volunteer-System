import request from '../utils/request';

// 认证相关 API
export const authApi = {
	login: (data) => request.post('/api/auth/login', data),
	register: (data) => request.post('/api/auth/register', data)
};

// 活动相关 API
export const activityApi = {
	// 获取活动列表 (对应 ActivityController)
	getActivities: (params) => request.get('/api/activity/page', {
		params: {
			current: params?.current || params?.page || 1, // 支持多种参数名兼容
			size: params?.size || params?.pageSize || 10,  // 优先取 size，解决 6 个/页失效问题
			title: params?.title || '',
			type: params?.type, 						   // 分类过滤
			status: (params?.status === '全部' || params?.status === null) ? undefined : params?.status
		}
	}),
	signup: (userId, activityId) => request.post('/api/reg/apply', null, {
		params: { userId, activityId }
	}),
	getMySignups: (userId) => request.get('/api/reg/my', {
		params: { userId }
	}),
	// 签到/签退
	sign: (userId, regId) => request.put('/api/reg/sign', null, { params: { userId, regId } }),
	signOut: (userId, regId) => request.put('/api/reg/sign-out', null, { params: { userId, regId } })
};

// 公告相关 API
export const announcementApi = {
	getAnnouncements: (page = 1, pageSize = 10) => request.get('/api/notice/page', {
		params: { current: page, size: pageSize }
	}),
	markAsRead: (noticeId) => request.post(`/api/notice/read/${noticeId}`),
	markAllAsRead: (userId) => request.post('/api/notice/read-all', null, { params: { userId } })
};

// 积分商城 API
export const mallApi = {
	getProducts: () => request.get('/api/shop/admin/page', {
		params: { current: 1, size: 100 }
	}),
	exchange: (userId, goodsId) => request.post('/api/shop/exchange', null, {
		params: { userId, goodsId }
	}),
	getExchangeRecords: (userId) => request.get('/api/shop/admin/record/page', {
		params: { userId, current: 1, size: 100 }
	})
};

// 排行榜 API
export const leaderboardApi = {
	getLeaderboard: (type = 'points') => request.get('/api/dashboard/volunteer/rank', {
		params: { type }
	})
};

// 数据看板 API
export const dashboardApi = {
	getBaseData: () => request.get('/api/dashboard/base')
};

// 用户信息 API
export const userApi = {
	getUserInfo: (userId) => request.get('/api/user/info', { params: { userId } }),
	// 头像上传接口
	uploadAvatar: (file) => {
		const formData = new FormData();
		formData.append('file', file); // 后端参数名叫 file
		return request.post('/api/file/upload', formData, {
			headers: { 'Content-Type': 'multipart/form-data' }
		});
	},
	// 获取所有志愿者列表用于前端计算排名
	getAllVolunteers: () => request.get('/api/user/page', {
		params: { current: 1, size: 500, role: 'VOLUNTEER' }
	}),
	// 资料更新与安全中心
	updateProfile: (data) => request.put('/api/user/profile', data),
	updatePassword: (data) => request.put('/api/user/password', data)
};

// 微心愿相关 API
export const wishApi = {
	getMyWishes: (userId, role) => request.get('/api/wish/my', {
		params: { userId, role }
	}),
	getPublicFeeds: () => request.get('/api/wish/feeds')
};
