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
			current: params?.current || params?.page || 1,
			size: params?.size || params?.pageSize || 10,
			title: params?.title || '',
			type: params?.type,
			status: (params?.status === '全部' || params?.status === null) ? undefined : params?.status
		}
	}),
	// 报名/打卡身份一律由后端从 JWT 解析，前端不再传 userId
	signup: (activityId) => request.post('/api/reg/apply', null, {
		params: { activityId }
	}),
	getMySignups: () => request.get('/api/reg/my'),

	// 签到/签退
	sign: (regId) => request.put('/api/reg/sign', null, { params: { regId } }),
	signOut: (regId) => request.put('/api/reg/sign-out', null, { params: { regId } })
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
	// 商品大厅（登录即可浏览，非管理员端点）
	getProducts: () => request.get('/api/shop/page', {
		params: { current: 1, size: 100 }
	}),
	exchange: (userId, goodsId) => request.post('/api/shop/exchange', null, {
		params: { userId, goodsId }
	}),
	// 当前用户（JWT 身份）自己的兑换记录
	getExchangeRecords: () => request.get('/api/shop/my-record', {
		params: { current: 1, size: 100 }
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
	getBaseData: () => request.get('/api/dashboard/base'),
	// 志愿者榜单一览（脱敏），供排行榜页与个人资料实时名次计算
	getVolunteerList: () => request.get('/api/dashboard/volunteers')
};

// 用户信息 API
export const userApi = {
	getUserInfo: () => request.get('/api/user/info'),
	// 头像上传接口
	uploadAvatar: (file) => {
		const formData = new FormData();
		formData.append('file', file); // 后端参数名叫 file
		return request.post('/api/file/upload', formData, {
			headers: { 'Content-Type': 'multipart/form-data' }
		});
	},
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
