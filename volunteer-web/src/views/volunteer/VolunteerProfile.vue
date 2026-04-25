<template>
  <div class="mx-auto max-w-5xl px-4 sm:px-6 lg:px-8 py-8">
    <div class="grid grid-cols-1 lg:grid-cols-3 gap-8">
      <!-- Left Column: Profile Info -->
      <div class="lg:col-span-1 space-y-6">
        <!-- User Card -->
        <div class="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
          <div class="h-32 bg-gradient-to-r from-orange-400 to-red-500 relative">
            <button @click="showSettings = true" class="absolute top-4 right-4 p-2 bg-white/20 hover:bg-white/30 rounded-full text-white backdrop-blur-sm transition-colors">
              <Settings class="w-5 h-5" />
            </button>
          </div>
          <div class="px-6 pb-6 relative">
            <div class="flex justify-between items-end -mt-12 mb-4">
              <div class="relative">
                <div class="w-24 h-24 rounded-full border-4 border-white overflow-hidden bg-slate-100 shadow-md">
                  <img :src="getFullAvatar(user.avatar)" :alt="user.name" class="w-full h-full object-cover" />
                </div>
                <div class="absolute bottom-0 right-0 bg-orange-500 text-white text-[10px] font-bold px-2 py-0.5 rounded-full border-2 border-white shadow-sm">
                  {{ user.level ? user.level.split(' ')[0] : 'V1' }}
                </div>
              </div>
              <button @click="showEditProfile = true" class="flex items-center gap-1.5 px-4 py-2 bg-slate-100 hover:bg-slate-200 text-slate-700 text-sm font-medium rounded-xl transition-colors">
                <Edit3 class="w-4 h-4" />
                编辑资料
              </button>
            </div>

            <h1 class="text-2xl font-bold text-slate-900 mb-1">{{ user.name }}</h1>
            <p class="text-sm font-bold text-orange-600 mb-4 flex items-center gap-1.5">
              <span>{{ user.levelIcon }}</span>
              <span>{{ user.level }}</span>
            </p>
            
            <!-- 技能标签云 -->
            <div class="flex flex-wrap gap-2 mb-6">
              <span 
                v-for="skill in user.skills" 
                :key="skill"
                class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-bold bg-orange-50 text-orange-600 border border-orange-100 shadow-sm"
              >
                {{ skill }}
              </span>
              <span v-if="!user.skills || user.skills.length === 0" class="text-sm text-slate-400 italic">暂未填写技能特长</span>
            </div>

            <div class="space-y-3 text-sm text-slate-500">
              <div class="flex items-center gap-2">
                <MapPin class="w-4 h-4 text-slate-400" />
                {{ user.location }}
              </div>
              <div class="flex items-center gap-2">
                <Calendar class="w-4 h-4 text-slate-400" />
                加入于 {{ user.joinDate }}
              </div>
            </div>
          </div>
        </div>

        <!-- Menu Links -->
        <div class="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
          <div class="divide-y divide-slate-100">
            <button @click="showCertificate" class="w-full flex items-center justify-between p-4 hover:bg-slate-50 transition-colors group text-left">
              <div class="flex items-center gap-3 text-slate-700 font-medium">
                <div class="w-8 h-8 rounded-lg bg-orange-50 text-orange-600 flex items-center justify-center group-hover:bg-orange-100 transition-colors">
                  <Award class="w-4 h-4" />
                </div>
                荣誉证书
              </div>
              <ChevronRight class="w-5 h-5 text-slate-400 group-hover:text-slate-600" />
            </button>
            <button @click="handleLogout" class="w-full flex items-center justify-between p-4 hover:bg-red-50 transition-colors group text-left">
              <div class="flex items-center gap-3 text-red-600 font-medium">
                <div class="w-8 h-8 rounded-lg bg-red-50 text-red-600 flex items-center justify-center group-hover:bg-red-100 transition-colors">
                  <LogOut class="w-4 h-4" />
                </div>
                退出登录
              </div>
            </button>
          </div>
        </div>
      </div>

      <!-- Right Column: Stats & Activities -->
      <div class="lg:col-span-2 space-y-6">
        <!-- Stats Grid 居中对齐 -->
        <div class="grid grid-cols-2 sm:grid-cols-4 lg:grid-cols-7 gap-3">
          <div class="bg-white p-4 rounded-2xl border border-slate-200 shadow-sm flex flex-col items-center justify-center text-center min-h-[95px]">
            <div class="text-2xl font-bold text-slate-900 mb-1 tabular-nums">{{ user.stats.hours }}</div>
            <div class="text-[10px] font-medium text-slate-500 uppercase tracking-wider leading-tight">总时长 (h)</div>
          </div>
          <div class="bg-white p-4 rounded-2xl border border-slate-200 shadow-sm flex flex-col items-center justify-center text-center min-h-[95px]">
            <div class="text-2xl font-bold text-slate-900 mb-1 tabular-nums">{{ user.stats.activities }}</div>
            <div class="text-[10px] font-medium text-slate-500 uppercase tracking-wider leading-tight">参与总数</div>
          </div>
          <div class="bg-white p-4 rounded-2xl border border-slate-200 shadow-sm flex flex-col items-center justify-center text-center min-h-[95px]">
            <div class="text-2xl font-bold text-orange-600 mb-1 tabular-nums">{{ user.stats.points }}</div>
            <div class="text-[10px] font-medium text-slate-500 uppercase tracking-wider leading-tight">可用积分</div>
          </div>
          <div class="bg-white p-4 rounded-2xl border border-slate-200 shadow-sm flex flex-col items-center justify-center text-center min-h-[95px]">
            <div class="text-2xl font-bold text-rose-600 mb-1 tabular-nums">{{ user.stats.totalPoints }}</div>
            <div class="text-[10px] font-medium text-slate-500 uppercase tracking-wider leading-tight">累计积分</div>
          </div>
          <div class="bg-white p-4 rounded-2xl border border-slate-200 shadow-sm flex flex-col items-center justify-center text-center min-h-[95px]">
            <div class="text-2xl font-bold text-slate-900 mb-1 tabular-nums">{{ user.stats.pointsRank }}</div>
            <div class="text-[10px] font-medium text-slate-500 uppercase tracking-wider leading-tight">积分排名</div>
          </div>
          <div class="bg-white p-4 rounded-2xl border border-slate-200 shadow-sm flex flex-col items-center justify-center text-center min-h-[95px]">
            <div class="text-2xl font-bold text-slate-900 mb-1 tabular-nums">{{ user.stats.hoursRank }}</div>
            <div class="text-[10px] font-medium text-slate-500 uppercase tracking-wider leading-tight">时长排名</div>
          </div>
          <div class="bg-white p-4 rounded-2xl border border-slate-200 shadow-sm flex flex-col items-center justify-center text-center min-h-[95px] group hover:border-rose-200 transition-all">
            <!-- 垂直分层设计：确保数字在水平方向处于绝对几何中心 -->
            <div class="flex flex-col items-center">
              <Heart 
                class="w-3.5 h-3.5 text-rose-500 fill-rose-500 mb-0.5 group-hover:scale-125 transition-transform duration-300 pointer-events-none" 
                :class="user.likes > 0 ? 'opacity-100' : 'opacity-30'"
              />
              <div class="text-2xl font-bold text-rose-500 tabular-nums leading-none">
                {{ user.likes || 0 }}
              </div>
            </div>
            <div class="text-[10px] font-medium text-slate-500 uppercase tracking-wider leading-tight mt-1.5">获赞总数</div>
          </div>
        </div>

        <!-- Recent Activities -->
        <div class="bg-white rounded-2xl border border-slate-200 shadow-sm p-6">
          <div class="flex items-center justify-between mb-6">
            <h2 class="text-lg font-bold text-slate-900">最近活动</h2>
            <button @click="showAllActivities = true" class="text-sm font-medium text-orange-600 hover:text-orange-700">
              查看全部
            </button>
          </div>

          <div class="space-y-6">
            <!-- 主页面仅显示最近 3 条记录 -->
            <div
              v-for="(item, index) in combinedHistory.slice(0, 5)"
              :key="item.regId || item.wishId"
              class="relative pl-6"
            >
              <!-- Timeline Line -->
              <div
                v-if="index !== Math.min(combinedHistory.length, 5) - 1"
                class="absolute left-[11px] top-8 bottom-[-24px] w-px bg-slate-200"
              ></div>
              <!-- Timeline Dot -->
              <div
                class="absolute left-0 top-1.5 w-6 h-6 rounded-full border-4 border-white flex items-center justify-center"
                :class="item.status === 3 ? 'bg-emerald-500' : 'bg-orange-500'"
              >
                <div class="w-2 h-2 rounded-full bg-white"></div>
              </div>

              <div class="bg-slate-50 rounded-xl p-4 border border-slate-100 hover:border-slate-200 transition-colors">
                <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-2 mb-2">
                  <div class="flex items-center gap-2">
                    <el-tag size="small" :type="item.recordType === 'WISH' ? 'warning' : 'success'" effect="plain">
                      {{ item.recordType === 'WISH' ? '微心愿' : '志愿活动' }}
                    </el-tag>
                    <h3 class="font-bold text-slate-900">{{ item.displayTitle }}</h3>
                  </div>
                  <span
                    class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium self-start sm:self-auto"
                    :class="item.statusClass"
                  >
                    {{ item.statusText }}
                  </span>
                </div>

                <div class="flex flex-wrap items-center gap-4 text-sm text-slate-500">
                  <div class="flex items-center gap-1.5">
                    <Calendar class="w-4 h-4 text-slate-400" />
                    {{ item.displayTime ? item.displayTime.substring(0, 10) : '未知日期' }}
                  </div>
                  <template v-if="item.status === 3">
                    <div v-if="item.recordType === 'ACTIVITY'" class="flex items-center gap-1.5">
                      <Clock class="w-4 h-4 text-slate-400" />
                      {{ item.actualHours || 0 }} 小时
                    </div>
                    <div class="flex items-center gap-1.5 text-orange-600 font-medium">
                      <Star class="w-4 h-4" />
                      +{{ item.reward || 0 }} 积分
                    </div>
                  </template>
                </div>
                <div v-if="item.recordType === 'ACTIVITY' && (item.status === 1 || item.status === 5)" class="mt-3">
                  <button
                    @click="triggerScan(item)"
                    class="flex items-center gap-1.5 px-4 py-2 bg-orange-100 text-orange-600 rounded-xl text-sm font-bold hover:bg-orange-200 transition-colors"
                  >
                    <Scan class="w-4 h-4" />
                    {{ item.status === 1 ? '扫码签到' : '扫码签退' }}
                  </button>
                </div>
              </div>
            </div>
            <div v-if="combinedHistory.length === 0" class="text-center py-8 text-slate-400 text-sm">
              暂无活动记录
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 全部活动记录弹窗 -->
    <div
      v-if="showAllActivities"
      class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/50 backdrop-blur-sm"
    >
      <div class="bg-white rounded-3xl w-full max-w-2xl overflow-hidden shadow-2xl animate-in fade-in zoom-in duration-200 flex flex-col max-h-[85vh]">
        <!-- Header -->
        <div class="p-4 border-b border-slate-100 flex justify-between items-center bg-slate-50">
          <div class="flex items-center gap-2">
            <div class="w-8 h-8 rounded-lg bg-orange-100 text-orange-600 flex items-center justify-center">
              <Calendar class="w-4 h-4" />
            </div>
            <h3 class="font-bold text-slate-900">全部志愿服务记录</h3>
          </div>
          <button @click="showAllActivities = false" class="text-slate-400 hover:text-slate-600 p-1 bg-white rounded-full shadow-sm">
            <X class="h-5 w-5" />
          </button>
        </div>

        <!-- Body -->
        <div class="flex-1 overflow-y-auto p-4 sm:p-6 space-y-4">
          <div v-if="recentActivities.length === 0" class="text-center py-12">
            <div class="w-16 h-16 bg-slate-50 rounded-full flex items-center justify-center mx-auto mb-4">
              <Calendar class="w-8 h-8 text-slate-300" />
            </div>
            <p class="text-slate-400">暂无任何志愿服务记录</p>
          </div>

          <div
            v-for="record in combinedHistory"
            :key="record.regId || record.wishId"
            class="bg-slate-50 rounded-2xl p-4 border border-slate-100 hover:border-orange-200 hover:bg-white transition-all group"
          >
            <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
              <div class="flex-1">
                <div class="flex items-center gap-2 mb-1">
                  <el-tag size="small" :type="record.recordType === 'WISH' ? 'warning' : 'success'" effect="plain">
                    {{ record.recordType === 'WISH' ? '微心愿' : '志愿活动' }}
                  </el-tag>
                  <h4 class="font-bold text-slate-900 group-hover:text-orange-600 transition-colors">
                    {{ record.displayTitle }}
                  </h4>
                </div>
                <div class="flex items-center gap-3 text-xs text-slate-500">
                  <span class="flex items-center gap-1">
                    <Calendar class="w-3 h-3" />
                    {{ record.displayTime?.substring(0, 10) }}
                  </span>
                  <span class="w-px h-3 bg-slate-200"></span>
                  <span 
                    class="font-medium"
                    :class="record.statusClass"
                  >
                    {{ record.statusText }}
                  </span>
                </div>
              </div>

              <div class="flex items-center gap-4 shrink-0">
                <!-- 操作按钮 -->
                <button 
                  v-if="record.recordType === 'ACTIVITY' && (record.status === 1 || record.status === 5)"
                  @click="triggerScan(record)"
                  class="px-4 py-1.5 bg-orange-600 text-white rounded-lg text-xs font-bold hover:bg-orange-700 transition-colors shadow-sm"
                >
                  {{ record.status === 1 ? '签到' : '签退' }}
                </button>

                <div v-if="record.status === 3" class="flex items-center gap-4">
                  <div class="flex items-center gap-2">
                    <div v-if="record.recordType === 'ACTIVITY'" class="text-right">
                      <div class="text-sm font-bold text-slate-900">{{ record.actualHours || 0 }} 小时</div>
                      <div class="text-[10px] text-slate-400 uppercase">服务工时</div>
                    </div>
                    <div class="w-px h-8 bg-slate-200" v-if="record.recordType === 'ACTIVITY'"></div>
                    <div class="text-right">
                      <div class="text-sm font-bold text-orange-600">+{{ record.reward || 0 }}</div>
                      <div class="text-[10px] text-slate-400 uppercase">获得积分</div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Footer -->
        <div class="p-4 border-t border-slate-100 bg-slate-50 shrink-0">
          <button
            @click="showAllActivities = false"
            class="w-full sm:w-auto sm:px-8 py-2.5 bg-orange-600 text-white rounded-xl text-sm font-bold hover:bg-orange-700 transition-colors shadow-sm mx-auto block"
          >
            我知道了
          </button>
        </div>
      </div>
    </div>

    <!-- Edit Profile Modal -->
    <div
      v-if="showEditProfile"
      class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/50 backdrop-blur-sm"
    >
      <div class="bg-white rounded-3xl w-full max-w-lg overflow-hidden shadow-2xl animate-in fade-in zoom-in duration-200 flex flex-col max-h-[90vh]">
        <div class="p-4 border-b border-slate-100 flex justify-between items-center bg-slate-50 shrink-0">
          <h3 class="font-bold text-slate-900">编辑个人资料</h3>
          <button @click="showEditProfile = false" class="text-slate-400 hover:text-slate-600 p-1">
            <X class="h-5 w-5" />
          </button>
        </div>
        
        <div class="p-6 space-y-5 overflow-y-auto flex-1 custom-scrollbar">
          <!-- 头像区域 -->
          <div class="flex flex-col items-center mb-2">
            <div class="relative">
              <div class="w-20 h-20 rounded-full border-2 border-slate-200 overflow-hidden bg-slate-100 shadow-inner">
                <img :src="getFullAvatar(user.avatar)" :alt="user.name" class="w-full h-full object-cover" />
              </div>
              <button 
                @click="handleAvatarClick" 
                :disabled="uploadingAvatar"
                class="absolute bottom-0 right-0 bg-white p-1.5 rounded-full border border-slate-200 text-slate-600 hover:text-orange-600 shadow-sm transition-all"
              >
                <span v-if="uploadingAvatar" class="animate-spin border-2 border-orange-600 border-t-transparent rounded-full w-3 h-3 block"></span>
                <Camera v-else class="w-4 h-4" />
              </button>
              <input type="file" ref="avatarInput" class="hidden" accept="image/*" @change="onFileChange" />
            </div>
            <p class="text-[10px] text-slate-400 mt-2">支持高清头像上传，点击按钮更换</p>
          </div>

          <!-- 基础信息 -->
          <div class="grid grid-cols-2 gap-4">
            <div class="space-y-1">
              <label class="text-xs font-bold text-slate-500 uppercase tracking-wider pl-1">真实姓名</label>
              <input type="text" v-model="user.name" class="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:ring-2 focus:ring-orange-600/20 focus:border-orange-600 outline-none text-sm transition-all" />
            </div>
            <div class="space-y-1">
              <label class="text-xs font-bold text-slate-500 uppercase tracking-wider pl-1">性别</label>
              <div class="flex h-[42px] items-center gap-4 px-4 bg-slate-50 border border-slate-200 rounded-xl">
                <label class="flex items-center gap-1.5 cursor-pointer text-sm text-slate-600">
                  <input type="radio" v-model="user.gender" :value="1" class="accent-orange-600" /> 男
                </label>
                <label class="flex items-center gap-1.5 cursor-pointer text-sm text-slate-600">
                  <input type="radio" v-model="user.gender" :value="2" class="accent-orange-600" /> 女
                </label>
              </div>
            </div>
          </div>

          <!-- 联系方式 -->
          <div class="grid grid-cols-2 gap-4">
            <div class="space-y-1">
              <label class="text-xs font-bold text-slate-500 uppercase tracking-wider pl-1">手机号码</label>
              <input type="text" v-model="user.phone" class="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:ring-2 focus:ring-orange-600/20 focus:border-orange-600 outline-none text-sm transition-all" placeholder="11位手机号" />
            </div>
            <div class="space-y-1">
              <label class="text-xs font-bold text-slate-500 uppercase tracking-wider pl-1">电子邮箱</label>
              <input type="email" v-model="user.email" class="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:ring-2 focus:ring-orange-600/20 focus:border-orange-600 outline-none text-sm transition-all" placeholder="name@example.com" />
            </div>
          </div>

          <!-- 专业属性 -->
          <div class="space-y-1">
            <label class="text-xs font-bold text-slate-500 uppercase tracking-wider pl-1">空闲时间</label>
            <select v-model="user.availableTime" class="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:ring-2 focus:ring-orange-600/20 focus:border-orange-600 outline-none text-sm transition-all appearance-none">
              <option value="">请选择空闲时段</option>
              <option value="工作日白天">工作日白天</option>
              <option value="工作日晚上">工作日晚上</option>
              <option value="周末">周末</option>
              <option value="随时">随时</option>
            </select>
          </div>

          <div class="space-y-1">
            <label class="text-xs font-bold text-slate-500 uppercase tracking-wider pl-1">技能特长</label>
            <el-select
              v-model="user.skills"
              multiple
              filterable
              allow-create
              default-first-option
              placeholder="选择或输入您的专长"
              style="width: 100%"
              class="custom-el-select"
            >
              <el-option v-for="item in skillOptions" :key="item" :label="item" :value="item" />
            </el-select>
          </div>

          <div class="space-y-1">
            <label class="text-xs font-bold text-slate-500 uppercase tracking-wider pl-1">所在地</label>
            <input type="text" v-model="user.location" class="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:ring-2 focus:ring-orange-600/20 focus:border-orange-600 outline-none text-sm transition-all" />
          </div>
        </div>

        <div class="p-4 border-t border-slate-100 bg-slate-50 flex gap-3 shrink-0">
          <button @click="showEditProfile = false" class="flex-1 px-4 py-2.5 bg-white border border-slate-200 text-slate-700 rounded-xl text-sm font-bold hover:bg-slate-50 transition-colors">取消</button>
          <button @click="submitProfile" :disabled="savingProfile" class="flex-1 px-4 py-2.5 bg-orange-600 text-white rounded-xl text-sm font-bold hover:bg-orange-700 transition-colors shadow-lg shadow-orange-600/30 flex justify-center items-center gap-2">
            <span v-if="savingProfile" class="animate-spin border-2 border-white border-t-transparent rounded-full w-3 h-3"></span>
            {{ savingProfile ? '保存中...' : '保存修改' }}
          </button>
        </div>
      </div>
    </div>

    <!-- Settings Modal -->
    <div
      v-if="showSettings"
      class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/50 backdrop-blur-sm"
    >
      <div class="bg-white rounded-3xl w-full max-w-md overflow-hidden shadow-2xl animate-in fade-in zoom-in duration-200">
        <div class="p-4 border-b border-slate-100 flex justify-between items-center bg-slate-50">
          <h3 class="font-bold text-slate-900">系统设置</h3>
          <button @click="showSettings = false" class="text-slate-400 hover:text-slate-600 p-1">
            <X class="h-5 w-5" />
          </button>
        </div>
        <div class="p-2">
          <div class="divide-y divide-slate-100">
            <div class="p-4 flex items-center justify-between">
              <div>
                <h4 class="font-medium text-slate-900 text-sm">接收活动通知</h4>
                <p class="text-xs text-slate-500 mt-0.5">当有新活动或活动状态变更时通知我</p>
              </div>
              <label class="relative inline-flex items-center cursor-pointer">
                <input type="checkbox" class="sr-only peer" checked />
                <div class="w-11 h-6 bg-slate-200 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-slate-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-orange-500"></div>
              </label>
            </div>
            <div class="p-4 flex items-center justify-between">
              <div>
                <h4 class="font-medium text-slate-900 text-sm">公开我的志愿时长</h4>
                <p class="text-xs text-slate-500 mt-0.5">允许其他用户在排行榜看到我的数据</p>
              </div>
              <label class="relative inline-flex items-center cursor-pointer">
                <input type="checkbox" class="sr-only peer" checked />
                <div class="w-11 h-6 bg-slate-200 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-slate-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-orange-500"></div>
              </label>
            </div>
            <button
              @click="showSettings = false; showChangePassword = true;"
              class="w-full p-4 flex items-center justify-between hover:bg-slate-50 transition-colors text-left"
            >
              <span class="font-medium text-slate-900 text-sm">修改密码</span>
              <ChevronRight class="w-5 h-5 text-slate-400" />
            </button>
            <button
              @click="showSettings = false; showAboutUs = true;"
              class="w-full p-4 flex items-center justify-between hover:bg-slate-50 transition-colors text-left"
            >
              <span class="font-medium text-slate-900 text-sm">关于我们</span>
              <ChevronRight class="w-5 h-5 text-slate-400" />
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Change Password Modal -->
    <div
      v-if="showChangePassword"
      class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/50 backdrop-blur-sm"
    >
      <div class="bg-white rounded-3xl w-full max-w-md overflow-hidden shadow-2xl animate-in fade-in zoom-in duration-200">
        <div class="p-4 border-b border-slate-100 flex justify-between items-center bg-slate-50">
          <h3 class="font-bold text-slate-900">修改密码</h3>
          <button @click="showChangePassword = false" class="text-slate-400 hover:text-slate-600 p-1">
            <X class="h-5 w-5" />
          </button>
        </div>
        <div class="p-6 space-y-4">
          <div class="space-y-1">
            <label class="text-sm font-semibold text-slate-700">原密码</label>
            <input v-model="passwordForm.oldPassword" type="password" placeholder="请输入当前密码" class="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:ring-2 focus:ring-orange-600/20 focus:border-orange-600 outline-none text-sm transition-all" />
          </div>
          <div class="space-y-1">
            <label class="text-sm font-semibold text-slate-700">新密码</label>
            <input v-model="passwordForm.newPassword" type="password" placeholder="请输入新密码（不少于 6 位）" class="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:ring-2 focus:ring-orange-600/20 focus:border-orange-600 outline-none text-sm transition-all" />
          </div>
          <div class="space-y-1">
            <label class="text-sm font-semibold text-slate-700">确认新密码</label>
            <input v-model="passwordForm.confirmPassword" type="password" placeholder="请再次输入新密码" class="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:ring-2 focus:ring-orange-600/20 focus:border-orange-600 outline-none text-sm transition-all" />
          </div>
        </div>
        <div class="p-4 border-t border-slate-100 bg-slate-50 flex gap-3">
          <button @click="showChangePassword = false" class="flex-1 px-4 py-2.5 bg-white border border-slate-200 text-slate-700 rounded-xl text-sm font-bold hover:bg-slate-50 transition-colors">取消</button>
          <button
            @click="submitPassword"
            :disabled="changingPassword"
            class="flex-1 px-4 py-2.5 bg-orange-600 text-white rounded-xl text-sm font-bold hover:bg-orange-700 transition-colors shadow-sm flex justify-center items-center gap-2"
          >
            <span v-if="changingPassword" class="animate-spin border-2 border-white border-t-transparent rounded-full w-3 h-3"></span>
            {{ changingPassword ? '提交中...' : '确认修改' }}
          </button>
        </div>
      </div>
    </div>

    <!-- About Us Modal -->
    <div
      v-if="showAboutUs"
      class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/50 backdrop-blur-sm"
    >
      <div class="bg-white rounded-3xl w-full max-w-md overflow-hidden shadow-2xl animate-in fade-in zoom-in duration-200">
        <div class="p-4 border-b border-slate-100 flex justify-between items-center bg-slate-50">
          <h3 class="font-bold text-slate-900">关于我们</h3>
          <button @click="showAboutUs = false" class="text-slate-400 hover:text-slate-600 p-1">
            <X class="h-5 w-5" />
          </button>
        </div>
        <div class="p-8 text-center">
          <div class="w-20 h-20 bg-orange-100 text-orange-600 rounded-3xl flex items-center justify-center mx-auto mb-4 shadow-sm">
            <Heart class="w-10 h-10 fill-current" />
          </div>
          <h2 class="text-xl font-bold text-slate-900 mb-1">志愿之光</h2>
          <p class="text-sm text-slate-500 mb-6">版本 1.0.0</p>

          <p class="text-sm text-slate-600 leading-relaxed mb-8 text-left bg-slate-50 p-4 rounded-2xl border border-slate-100">
            "志愿之光"致力于连接热心公益的志愿者与需要帮助的社区 and 组织。我们相信，每一次微小的善举，都能汇聚成照亮世界的温暖光芒。
          </p>

          <div class="space-y-3 text-sm text-slate-500 text-left">
            <div class="flex justify-between border-b border-slate-100 pb-2">
              <span>官方网站</span>
              <a href="#" class="text-orange-600 hover:underline">www.volunteer-light.org</a>
            </div>
            <div class="flex justify-between border-b border-slate-100 pb-2">
              <span>联系邮箱</span>
              <a href="mailto:contact@volunteer-light.org" class="text-orange-600 hover:underline">contact@volunteer-light.org</a>
            </div>
            <div class="flex justify-between pb-2">
              <span>服务协议与隐私政策</span>
              <a href="#" class="text-orange-600 hover:underline">查看</a>
            </div>
          </div>
        </div>
        <div class="p-4 border-t border-slate-100 bg-slate-50">
          <button @click="showAboutUs = false" class="w-full px-4 py-2.5 bg-white border border-slate-200 text-slate-700 rounded-xl text-sm font-bold hover:bg-slate-50 transition-colors">关闭</button>
        </div>
      </div>
    </div>

    <!-- Scanner Modal -->
    <div
      v-if="showScanner"
      class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/90 backdrop-blur-md"
    >
      <div class="bg-white rounded-3xl w-full max-w-sm overflow-hidden shadow-2xl animate-in fade-in zoom-in duration-200 text-center relative">
        <button
          @click="stopScan"
          class="absolute top-4 right-4 text-slate-400 hover:text-slate-600 p-1 bg-slate-100 rounded-full z-10"
        >
          <X class="h-5 w-5" />
        </button>

        <div v-if="!scanSuccess" class="p-8 pt-12">
          <h3 class="text-xl font-bold text-slate-900 mb-2">扫码签到/签退</h3>
          <p class="text-sm text-slate-500 mb-8">请将摄像头对准活动现场的二维码</p>

          <!-- 摄像头渲染容器 -->
          <div class="relative w-64 h-64 mx-auto mb-8 rounded-2xl overflow-hidden bg-black border-4 border-slate-800">
            <div id="reader" style="width: 100%; height: 100%;"></div>
            <!-- 扫描线装饰 -->
            <div class="absolute left-4 right-4 h-0.5 bg-orange-500 shadow-[0_0_8px_2px_rgba(249,115,22,0.5)] animate-[scan_2s_ease-in-out_infinite] z-10"></div>
          </div>

          <p class="text-xs text-slate-400">正在调用摄像头，请稍候...</p>
        </div>

        <div v-else class="p-8 pt-10">
          <div class="w-20 h-20 bg-emerald-100 rounded-full flex items-center justify-center mx-auto mb-6">
            <CheckCircle class="w-10 h-10 text-emerald-500" />
          </div>
          <h3 class="text-2xl font-bold text-slate-900 mb-2">操作成功！</h3>
          <p class="text-sm text-slate-500 mb-6">
            您已完成该活动的打卡操作
          </p>
          <button
            @click="stopScan"
            class="w-full bg-orange-600 text-white rounded-xl py-3 text-sm font-bold hover:bg-orange-700 transition-colors shadow-lg shadow-orange-600/30"
          >
            完成
          </button>
        </div>
      </div>
    </div>

    <!-- 证书弹窗 -->
    <el-dialog v-model="certVisible" title="🏅 我的志愿荣誉证书" width="800px" class="cert-dialog" align-center destroy-on-close>
      <div v-if="certVisible" class="cert-scroll-wrapper">
        <div class="cert-scale-box">
          <div class="cert-border" id="cert-content">
            <div class="cert-inner">
              <div class="cert-header">
                <div style="font-size: 55px; line-height: 1; margin-bottom: 10px;">🏆</div>
                <h1>志愿服务荣誉证书</h1>
                <p>VOLUNTEER HONORARY CERTIFICATE</p>
              </div>
              <div class="cert-body">
                <div class="awardee">尊敬的 <span>{{ user.name }}</span> 志愿者：</div>
                <div class="content-text">
                  &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;感谢您在社区志愿服务中展现出的无私奉献精神。
                  截止今日，您已累计服务 <strong>{{ user.stats.hours || 0 }}</strong> 小时，
                  累计获得 <strong>{{ userStore.user?.totalPoints || 0 }}</strong> 荣誉积分。
                  您的善行义举温暖了社区，特发此证，以资鼓励！
                </div>
              </div>
              <div class="cert-footer">
                <div class="date"><p>智慧社区志愿服务中心</p><p>{{ currentDate }}</p></div>
                <div class="seal"><div class="seal-inner"><span>志愿服务专用章</span></div></div>
              </div>
            </div>
          </div>
        </div>
      </div>
      <template #footer>
        <div class="flex justify-end gap-3 mt-4">
          <button @click="certVisible = false" class="px-6 py-2 bg-slate-100 text-slate-700 rounded-xl text-sm font-bold hover:bg-slate-200 transition-colors">关闭</button>
          <button @click="downloadPDF" :disabled="exportingPDF" class="px-6 py-2 bg-blue-600 text-white rounded-xl text-sm font-bold hover:bg-blue-700 transition-colors flex items-center gap-2">
            <span v-if="exportingPDF" class="animate-spin border-2 border-white border-t-transparent rounded-full w-3 h-3"></span>
            {{ exportingPDF ? '正在导出...' : '下载 PDF' }}
          </button>
          <button @click="downloadImage" :disabled="downloading" class="px-6 py-2 bg-orange-600 text-white rounded-xl text-sm font-bold hover:bg-orange-700 transition-colors flex items-center gap-2">
            <span v-if="downloading" class="animate-spin border-2 border-white border-t-transparent rounded-full w-3 h-3"></span>
            {{ downloading ? '正在生成...' : '下载图片' }}
          </button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, nextTick } from 'vue';
import { useUserStore } from '../../stores/user';
import { activityApi, leaderboardApi, userApi, wishApi } from '../../api/modules';
import { ElMessage, ElMessageBox } from 'element-plus';
import request from '../../utils/request';
import html2canvas from 'html2canvas';
import { jsPDF } from 'jspdf';
import { Html5Qrcode } from "html5-qrcode";
import {
  Settings,
  Edit3,
  MapPin,
  Calendar,
  Clock,
  Heart,
  Award,
  ChevronRight,
  LogOut,
  Star,
  X,
  Camera,
  Scan,
  CheckCircle,
  Trophy,
} from 'lucide-vue-next';
import { useRouter } from 'vue-router';
import { getFullAvatar } from '../../utils/file';
import { getLevelInfo } from '../../utils/levelRules';

const router = useRouter();
const userStore = useUserStore();

// 技能库定义（与管理端对齐）
const skillOptions = ['医疗急救', '心理疏导', '家电维修', '文艺演出', '法律咨询', '计算机IT', '外语翻译', '手工制作'];

const showEditProfile = ref(false);
const showSettings = ref(false);
const showChangePassword = ref(false);
const showAboutUs = ref(false);
const showScanner = ref(false);
const scanSuccess = ref(false);
const showAllActivities = ref(false);
const certVisible = ref(false);
const downloading = ref(false);
const exportingPDF = ref(false);
const savingProfile = ref(false);
const changingPassword = ref(false);
const uploadingAvatar = ref(false);
const avatarInput = ref(null);
const currentOperatingRecord = ref(null);

// 报名记录状态处理
const getRegStatusText = (status) => {
  const map = { 0: '待审核', 1: '待签到', 2: '已拒绝', 3: '已完结', 4: '已取消', 5: '已签到', 6: '待结算' };
  return map[status] || '处理中';
};

const getRegStatusClass = (status) => {
  const map = {
    0: 'bg-orange-100 text-orange-700',
    1: 'bg-blue-100 text-blue-700',
    2: 'bg-red-100 text-red-700',
    3: 'bg-slate-100 text-slate-600',
    4: 'bg-slate-100 text-slate-400',
    5: 'bg-indigo-100 text-indigo-700',
    6: 'bg-emerald-100 text-emerald-700'
  };
  return map[status] || 'bg-slate-100 text-slate-500';
};

// 扫码器相关状态
let html5QrCode = null;
let isProcessing = false;

const passwordForm = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
});

const user = ref({
  name: '',
  avatar: '',
  bio: '',
  joinDate: '',
  location: '',
  level: '',
  stats: {
    hours: 0,
    activities: 0,
    points: 0,
    pointsRank: '-',
    hoursRank: '-',
  },
});

const recentActivities = ref([]);
const wishHistory = ref([]);
const combinedHistory = computed(() => {
  // 合并活动记录和心愿记录
  const activities = (recentActivities.value || []).map(r => ({
    ...r,
    recordType: 'ACTIVITY',
    displayTitle: r.activityTitle,
    displayTime: r.activityStartTime,
    reward: r.rewardPoints,
    statusText: getRegStatusText(r.status),
    statusClass: getRegStatusClass(r.status)
  }));

  const wishes = (wishHistory.value || []).map(w => ({
    ...w,
    recordType: 'WISH',
    displayTitle: w.title,
    displayTime: w.finishTime || w.createTime,
    reward: w.rewardPoints,
    statusText: w.status === 3 ? '已达成' : (w.status === 2 ? '办理中' : '审核中'),
    statusClass: w.status === 3 ? 'bg-emerald-100 text-emerald-700' : 'bg-orange-100 text-orange-700'
  }));

  return [...activities, ...wishes].sort((a, b) =>
      new Date(b.displayTime || 0) - new Date(a.displayTime || 0)
  );
});

const currentDate = computed(() => {
  const date = new Date();
  return `${date.getFullYear()}年${date.getMonth() + 1}月${date.getDate()}日`;
});

const showCertificate = () => {
  if (!user.value.name) return ElMessage.warning('请先完善真实姓名');
  certVisible.value = true;
};

const downloadImage = async () => {
  downloading.value = true;
  const element = document.getElementById("cert-content");

  try {
    const originalStyle = element.style.cssText;
    element.style.position = 'fixed';
    element.style.top = '0';
    element.style.left = '0';
    element.style.zIndex = '9999';
    element.style.transform = 'none';
    element.style.margin = '0';

    const canvas = await html2canvas(element, {
      scale: 2,
      useCORS: true,
      backgroundColor: '#fffaf0',
      scrollX: 0,
      scrollY: 0,
    });

    element.style.cssText = originalStyle;

    const imgUrl = canvas.toDataURL("image/png");
    const link = document.createElement("a");
    link.href = imgUrl;
    link.download = `荣誉证书_${user.value.name}.png`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);

    ElMessage.success('证书下载成功');
  } catch (error) {
    console.error(error);
    ElMessage.error('生成失败，请稍后重试');
  } finally {
    downloading.value = false;
  }
};

const downloadPDF = async () => {
  exportingPDF.value = true;
  const element = document.getElementById("cert-content");

  try {
    const originalStyle = element.style.cssText;
    element.style.position = 'fixed';
    element.style.top = '0';
    element.style.left = '0';
    element.style.zIndex = '9999';
    element.style.transform = 'none';
    element.style.margin = '0';

    const canvas = await html2canvas(element, {
      scale: 2,
      useCORS: true,
      backgroundColor: '#fffaf0',
    });

    element.style.cssText = originalStyle;

    const imgData = canvas.toDataURL('image/png');
    const pdf = new jsPDF('l', 'mm', 'a4');
    const imgProps = pdf.getImageProperties(imgData);
    const pdfWidth = pdf.internal.pageSize.getWidth();
    const pdfHeight = (imgProps.height * pdfWidth) / imgProps.width;
    const margin = (pdf.internal.pageSize.getHeight() - pdfHeight) / 2;
    pdf.addImage(imgData, 'PNG', 0, margin, pdfWidth, pdfHeight);
    pdf.save(`荣誉证书_${user.value.name}.pdf`);

    ElMessage.success('PDF 证书导出成功');
  } catch (error) {
    console.error(error);
    ElMessage.error('导出失败');
  } finally {
    exportingPDF.value = false;
  }
};

const submitProfile = async () => {
  if (!user.value.name) return ElMessage.warning('昵称不能为空');
  savingProfile.value = true;
  try {
    const submitData = {
      userId: userStore.userId || localStorage.getItem('userId'),
      realName: user.value.name,
      phone: user.value.phone,
      email: user.value.email,
      gender: user.value.gender,
      availableTime: user.value.availableTime,
      // 序列化技能标签
      skills: JSON.stringify(user.value.skills || []),
      avatar: user.value.avatar
    };
    await request.put('/api/user/profile', submitData);
    ElMessage.success('个人资料更新成功');
    showEditProfile.value = false;
    await userStore.fetchCurrentUser();
  } catch (error) {
    console.error("Update profile error:", error);
  } finally {
    savingProfile.value = false;
  }
};

// 触发文件选择
const handleAvatarClick = () => {
  avatarInput.value.click();
};

// 处理头像上传 (仅预览)
const onFileChange = async (e) => {
  const file = e.target.files[0];
  if (!file) return;

  const isImg = file.type.startsWith('image/');
  const isLt10M = file.size / 1024 / 1024 < 10;
  if (!isImg) return ElMessage.error('只能上传图片格式的文件');
  if (!isLt10M) return ElMessage.error('图片大小不能超过 10MB');

  uploadingAvatar.value = true;
  try {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('type', 'avatar');

    const res = await request.post('/api/file/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    });

    const newPath = res.data;

    if (newPath) {
      // 仅更新本地预览，不触发 API 保存，不更新全局 Store
      user.value.avatar = newPath;
      ElMessage.info('预览头像已更新，请点击保存修改以生效');
    }
  } catch (error) {
    console.error("Upload preview error:", error);
    ElMessage.error('上传失败');
  } finally {
    uploadingAvatar.value = false;
    e.target.value = ''; // 清空 input
  }
};

const submitPassword = async () => {
  if (!passwordForm.value.oldPassword || !passwordForm.value.newPassword) {
    return ElMessage.warning('请填写完整信息');
  }
  if (passwordForm.value.newPassword.length < 6) {
    return ElMessage.warning('新密码不能少于 6 位');
  }
  if (passwordForm.value.newPassword !== passwordForm.value.confirmPassword) {
    return ElMessage.warning('两次输入的新密码不一致');
  }
  changingPassword.value = true;
  try {
    const submitData = {
      userId: userStore.userId || localStorage.getItem('userId'),
      oldPassword: passwordForm.value.oldPassword,
      newPassword: passwordForm.value.newPassword
    };
    await request.put('/api/user/password', submitData);
    ElMessage.success('密码修改成功，请重新登录');
    showChangePassword.value = false;
    setTimeout(() => { handleLogout(); }, 1500);
  } catch (error) {
    console.error("Change password error:", error);
  } finally {
    changingPassword.value = false;
  }
};

// 启动摄像头扫码逻辑
const startScan = async () => {
  showScanner.value = true;
  scanSuccess.value = false;
  isProcessing = false;
  await nextTick();

  html5QrCode = new Html5Qrcode("reader");
  html5QrCode.start(
      { facingMode: "environment" },
      { fps: 10, qrbox: { width: 250, height: 250 } },
      async (decodedText) => {
        if (isProcessing) return;
        isProcessing = true;
        await stopScan();

        const scannedId = parseInt(decodedText);
        if (isNaN(scannedId)) return ElMessage.error('无效的活动二维码');

        const record = recentActivities.value.find(r => r.activityId === scannedId);
        if (!record) return ElMessage.error('扫码失败：您未报名该活动或申请未通过');

        const userId = userStore.userId || localStorage.getItem('userId');
        try {
          if (record.status === 1) {
            await activityApi.sign(userId, record.regId);
            ElMessage.success('签到成功！');
          } else if (record.status === 5) {
            await activityApi.signOut(userId, record.regId);
            ElMessage.success('签退成功！');
          } else {
            return ElMessage.warning('当前状态无需打卡');
          }
          scanSuccess.value = true;
          const res = await activityApi.getMySignups(userId);
          recentActivities.value = res.data || [];
        } catch (e) {
          console.error(e);
        }
      }
  ).catch(err => {
    console.error(err);
    ElMessage.error('无法调用摄像头，请检查权限或使用 HTTPS');
    showScanner.value = false;
  });
};

const stopScan = async () => {
  if (html5QrCode) {
    try {
      if (html5QrCode.isScanning) await html5QrCode.stop();
      html5QrCode.clear();
    } catch (err) { console.error(err); }
  }
  showScanner.value = false;
};

const triggerScan = (record) => {
  currentOperatingRecord.value = record;
  startScan();
};

const handleLogout = () => {
  userStore.logout();
  router.push('/login');
};

onMounted(async () => {
  const effectiveUserId = userStore.userId || localStorage.getItem('userId');
  if (!userStore.user && effectiveUserId) await userStore.fetchCurrentUser();

  if (userStore.user) {
    const totalPoints = userStore.user.totalPoints || 0;
    const levelInfo = getLevelInfo(totalPoints);

    user.value = {
      ...userStore.user,
      name: userStore.user.realName || userStore.user.username,
      level: levelInfo.name,
      levelIcon: levelInfo.icon,
      avatar: userStore.user.avatar,
      bio: userStore.user.skills || '热爱生活，热心公益。',
      location: '本地社区',
      phone: userStore.user.phone || '',
      email: userStore.user.email || '',
      gender: userStore.user.gender || 0,
      availableTime: userStore.user.availableTime || '',
      // 处理技能标签 JSON
      skills: (() => {
        try {
          return JSON.parse(userStore.user.skills || '[]');
        } catch (e) {
          return userStore.user.skills ? [userStore.user.skills] : [];
        }
      })(),
      joinDate: userStore.user.createTime ? userStore.user.createTime.substring(0, 10) : '未知',
      stats: {
        hours: userStore.user.totalHours || 0,
        activities: 0,
        points: userStore.user.currentPoints || 0,
        totalPoints: userStore.user.totalPoints || 0,
        pointsRank: '-',
        hoursRank: '-',
      }
    };
  }

  if (effectiveUserId) {
    try {
      // 1. 获取活动记录
      const res = await activityApi.getMySignups(effectiveUserId);
      recentActivities.value = res.data || [];

      // 2. 获取微心愿记录
      const wRes = await wishApi.getMyWishes(effectiveUserId, 'VOLUNTEER');
      wishHistory.value = wRes.data || [];

      // 计算统计
      const completedActivities = recentActivities.value.filter(reg => reg.status === 3).length;
      const completedWishes = wishHistory.value.filter(w => w.status === 3).length;
      user.value.stats.activities = completedActivities + completedWishes;

      // 获取全量志愿者列表用于计算真实排名
      const allVolRes = await userApi.getAllVolunteers();
      const allVols = allVolRes.data?.records || [];

      if (allVols.length > 0) {
        // 计算积分排名
        const sortedByPoints = [...allVols].sort((a, b) => (b.totalPoints || 0) - (a.totalPoints || 0));
        const pIndex = sortedByPoints.findIndex(item => String(item.userId) === String(effectiveUserId));
        user.value.stats.pointsRank = pIndex !== -1 ? pIndex + 1 : '500+';

        // 计算时长排名
        const sortedByHours = [...allVols].sort((a, b) => (b.totalHours || 0) - (a.totalHours || 0));
        const hIndex = sortedByHours.findIndex(item => String(item.userId) === String(effectiveUserId));
        user.value.stats.hoursRank = hIndex !== -1 ? hIndex + 1 : '500+';
      }
    } catch (error) {
      console.error("Fetch profile extra data error:", error);
    }
  }
});
</script>

<style scoped>
@keyframes scan {
  0%, 100% { top: 1rem; }
  50% { top: calc(100% - 1rem); }
}

/* 🏅 证书样式 (移植自旧版) */
.cert-scroll-wrapper {
  width: 100%;
  display: flex;
  justify-content: center;
  overflow: hidden;
  padding: 10px 0;
}

.cert-border {
  width: 700px;
  height: 500px;
  background-color: #fffaf0;
  border: 10px solid #d4af37;
  padding: 5px;
  box-shadow: 0 5px 20px rgba(0, 0, 0, 0.15);
  position: relative;
}

.cert-inner {
  width: 100%;
  height: 100%;
  border: 2px solid #d4af37;
  padding: 30px;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  background-image: linear-gradient(to top, rgba(255,250,240,0.5) 0%, rgba(255,250,240,1) 100%);
}

.cert-header { text-align: center; color: #d4af37; }
.cert-header h1 { margin: 5px 0 2px; font-family: "SimHei", serif; font-size: 36px; letter-spacing: 5px; color: #b8860b; }
.cert-header p { margin: 0; font-size: 12px; letter-spacing: 2px; }

.cert-body { margin-top: -20px; font-family: "KaiTi", serif; color: #333; }
.awardee { font-size: 22px; margin-bottom: 15px; text-align: left; }
.awardee span { border-bottom: 2px solid #333; padding: 0 10px; font-weight: bold; font-size: 26px; }
.content-text { font-size: 18px; line-height: 1.8; text-align: justify; text-indent: 2em; }
.content-text strong { color: #d4af37; font-size: 22px; margin: 0 5px; font-family: Arial, sans-serif; }

.cert-footer { text-align: right; position: relative; margin-top: 30px; padding-right: 20px; }
.date p { margin: 5px 0; font-family: "KaiTi", serif; font-size: 18px; }

.seal {
  position: absolute;
  right: 20px;
  top: -30px;
  width: 130px;
  height: 130px;
  border: 4px solid #f56c6c;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #f56c6c;
  font-weight: bold;
  transform: rotate(-15deg);
  opacity: 0.85;
}
.seal-inner {
  width: 116px;
  height: 116px;
  border: 1px solid #f56c6c;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
  font-size: 16px;
  letter-spacing: 2px;
  padding: 15px;
  box-sizing: border-box;
}

@media screen and (max-width: 768px) {
  .cert-scroll-wrapper {
    overflow-x: auto;
    justify-content: flex-start;
  }
  .cert-border {
    transform: scale(0.5);
    transform-origin: top left;
    margin-right: -350px;
    margin-bottom: -250px;
  }
}
</style>
