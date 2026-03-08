<template>
  <div class="mall-container">
    <!-- 顶部：积分看板 -->
    <div class="mall-header">
      <div class="header-content">
        <div>
          <h2 class="page-title">🎁 积分商城</h2>
          <p class="page-subtitle" v-if="!isMobile">用爱心兑换礼物，让善意流转。</p>
        </div>

        <!-- 积分余额卡片 (移动端吸顶优化) -->
        <div class="user-points-card">
          <span class="label">可用余额</span>
          <div class="points-box">
            <el-icon><Coin /></el-icon>
            <span class="points-num">{{ userPoints }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 商品列表 (响应式栅格) -->
    <!-- xs=12 表示手机端一行显示 2 个 -->
    <el-row :gutter="isMobile ? 10 : 15">
      <el-col
          :xs="12" :sm="8" :md="6" :lg="4" :xl="4"
          v-for="item in goodsList"
          :key="item.goodsId"
          style="margin-bottom: 15px;"
      >
        <el-card class="goods-card" :body-style="{ padding: '0px' }" shadow="hover">
          <!-- 商品图片容器 -->
          <div class="image-wrapper">
            <el-image :src="item.image" class="goods-image" fit="cover">
              <template #error>
                <div class="img-error"><el-icon><Picture /></el-icon></div>
              </template>
            </el-image>
            <!-- 售罄遮罩 -->
            <div v-if="item.stock <= 0" class="sold-out-mask">
              <span>已抢光</span>
            </div>
          </div>

          <div class="goods-info">
            <h4 class="goods-name">{{ item.name }}</h4>

            <!-- 手机端隐藏描述，PC端显示 -->
            <p class="goods-desc" v-if="!isMobile">{{ item.description }}</p>

            <div class="bottom-area">
              <span class="points-tag">{{ item.pointsRequired }} 积分</span>

              <!-- 兑换按钮：PC端显示文字，手机端显示图标以节省空间 -->
              <el-button
                  type="primary"
                  :plain="item.stock > 0"
                  :disabled="item.stock <= 0 || userPoints < item.pointsRequired"
                  size="small"
                  class="exchange-btn"
                  @click="handleExchange(item)"
              >
                <span v-if="!isMobile">立即兑换</span>
                <el-icon v-else><ShoppingCartFull /></el-icon>
              </el-button>
            </div>

            <!-- 库存提示 (仅剩少量时显示) -->
            <div class="stock-tip" v-if="item.stock > 0 && item.stock < 10">
              仅剩 {{ item.stock }} 件
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-empty v-if="goodsList.length === 0" description="商城正在补货中..." />
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue';
import request from '../utils/request';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Coin, Picture, ShoppingCartFull } from '@element-plus/icons-vue';

// --- 响应式判断 ---
const isMobile = ref(window.innerWidth <= 768);
const handleResize = () => { isMobile.value = window.innerWidth <= 768; };

const goodsList = ref([]);
const userPoints = ref(0);
const userId = localStorage.getItem('userId');

const fetchGoods = async () => {
  try {
    const res = await request.get('/api/shop/list');
    goodsList.value = res.data;
  } catch (e) {}
};

const fetchUserPoints = async () => {
  try {
    const res = await request.get(`/api/user/info?userId=${userId}`);
    userPoints.value = res.data.currentPoints || 0;
  } catch (e) {}
};

const handleExchange = (item) => {
  if (userPoints.value < item.pointsRequired) {
    ElMessage.warning('您的积分不足！快去参加活动赚积分吧~');
    return;
  }

  ElMessageBox.confirm(
      `消耗 ${item.pointsRequired} 积分兑换【${item.name}】？`,
      '兑换确认',
      {
        confirmButtonText: '确定兑换',
        cancelButtonText: '再想想',
        type: 'warning',
        center: true
      }
  ).then(async () => {
    try {
      const res = await request.post(`/api/shop/exchange?userId=${userId}&goodsId=${item.goodsId}`);
      ElMessage.success(res.msg || '兑换成功！请到个人中心查看券码');
      fetchGoods();
      fetchUserPoints();
    } catch (e) {}
  }).catch(() => {});
};

onMounted(() => {
  fetchGoods();
  fetchUserPoints();
  window.addEventListener('resize', handleResize);
});
onUnmounted(() => {
  window.removeEventListener('resize', handleResize);
});
</script>

<style scoped>
/* ====================================================
   🖥️ PC 端基础样式
   ==================================================== */
.mall-container { padding: 20px; }

.mall-header {
  margin-bottom: 30px;
  background: #fff;
  padding: 20px;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.05);
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.page-title { margin: 0; font-size: 24px; color: #333; }
.page-subtitle { margin: 5px 0 0; color: #999; font-size: 14px; }

/* 积分余额卡片 */
.user-points-card {
  background: linear-gradient(135deg, #fdfbfb 0%, #ebedee 100%);
  border: 1px solid #dcdfe6;
  padding: 10px 20px;
  border-radius: 50px;
  display: flex;
  align-items: center;
  gap: 10px;
  box-shadow: 0 4px 10px rgba(0,0,0,0.05);
}
.label { font-size: 12px; color: #606266; }
.points-box { display: flex; align-items: center; gap: 5px; color: #e6a23c; }
.points-num { font-weight: 800; font-size: 24px; font-family: 'Arial', sans-serif; }

/* 商品卡片 */
/* 1. 减小卡片圆角和阴影 */
.goods-card {
  border-radius: 8px; /* 原12px */
  border: 1px solid #f0f0f0; /* 增加极淡边框增强精致感 */
  transition: all 0.3s;
  position: relative;
  overflow: hidden;
}

.goods-card:hover { transform: translateY(-5px); box-shadow: 0 8px 24px rgba(0,0,0,0.12); }

/* 2. 图片容器保持不变，但因为列宽变窄，图片高度会自动变小 */
.image-wrapper {
  position: relative;
  width: 100%;
  padding-top: 100%; /* 1:1 */
}

.goods-image {
  position: absolute;
  top: 0; left: 0;
  width: 100%; height: 100%;
}
.img-error {
  display: flex; justify-content: center; align-items: center;
  width: 100%; height: 100%; background: #f5f7fa; color: #c0c4cc; font-size: 24px;
}

.sold-out-mask {
  position: absolute;
  top: 0; left: 0; width: 100%; height: 100%;
  background: rgba(0,0,0,0.5);
  display: flex; justify-content: center; align-items: center;
  color: #fff; font-weight: bold; font-size: 18px; letter-spacing: 2px;
  backdrop-filter: blur(2px);
}

/* 3. 大幅压缩内容区内边距 */
.goods-info {
  padding: 10px; /* 原15px -> 10px */
}

/* 4. 调整标题字号和间距 */
.goods-name {
  margin: 0 0 4px;
  font-size: 14px; /* 原16px -> 14px */
  color: #303133;
  font-weight: bold;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 5. 调整描述文字 */
.goods-desc {
  font-size: 12px;
  color: #909399;
  height: 32px; /* 原36px */
  line-height: 1.4;
  margin-bottom: 8px;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
}


/* 6. 底部区域紧凑化 */
.bottom-area {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 5px; /* 原10px */
}

.points-tag {
  color: #f56c6c;
  font-weight: bold;
  font-size: 14px; /* 原16px */
}
.stock-tip {
  font-size: 10px;
  color: #e6a23c;
  margin-top: 8px;
  text-align: right;
}

/* 7. 调整按钮大小 */
.exchange-btn {
  padding: 5px 12px;
  font-size: 12px;
  height: 28px;
}

/* ====================================================
   📱 移动端响应式适配 (小于 768px)
   ==================================================== */
@media screen and (max-width: 768px) {
  .mall-container { padding: 10px; }

  /* 1. 头部简化 */
  .mall-header { padding: 15px; margin-bottom: 15px; }
  .page-title { font-size: 18px; }
  .user-points-card { padding: 5px 15px; }
  .points-num { font-size: 20px; }

  /* 2. 商品卡片紧凑化 */
  .goods-info { padding: 10px; }

  .goods-name {
    font-size: 14px;
    margin-bottom: 8px;
  }

  .bottom-area {
    /* 手机端价格和按钮上下排列，或者紧凑排列 */
    justify-content: space-between;
  }

  .points-tag { font-size: 14px; }

  /* 按钮变成圆形图标 */
  .exchange-btn {
    padding: 8px;
    height: 32px;
    width: 32px;
    border-radius: 50%;
  }
}
</style>