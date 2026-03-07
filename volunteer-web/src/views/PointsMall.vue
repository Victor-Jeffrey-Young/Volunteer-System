<template>
  <div class="mall-container">
    <div class="mall-header">
      <h2>🎁 志愿积分商城</h2>
      <p>用您的爱心，兑换一份属于自己的礼物。</p>
      <div class="user-points-card">
        <span>您当前的可用积分为：</span>
        <span class="points-num">{{ userPoints }} 分</span>
      </div>
    </div>

    <el-row :gutter="20">
      <el-col :span="6" v-for="item in goodsList" :key="item.goodsId">
        <el-card class="goods-card" shadow="hover">
          <img :src="item.image" class="goods-image" />
          <div style="padding: 14px;">
            <h4 class="goods-name">{{ item.name }}</h4>
            <div class="goods-desc">{{ item.description }}</div>
            <div class="bottom-area">
              <span class="points-tag">{{ item.pointsRequired }} 积分</span>
              <el-button
                  type="primary"
                  plain
                  round
                  size="small"
                  @click="handleExchange(item)"
                  :disabled="item.stock <= 0 || userPoints < item.pointsRequired"
              >
                {{ item.stock <= 0 ? '已售罄' : '立即兑换' }}
              </el-button>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import request from '../utils/request';
import { ElMessage, ElMessageBox } from 'element-plus';

const goodsList = ref([]);
const userPoints = ref(0);
const userId = localStorage.getItem('userId');

const fetchGoods = async () => {
  const res = await request.get('/api/shop/list');
  goodsList.value = res.data;
};

// 修改 fetchUserPoints 获取 currentPoints
const fetchUserPoints = async () => {
  const res = await request.get(`/api/user/info?userId=${userId}`);
  userPoints.value = res.data.currentPoints || 0; // 🚨 商城显示的是余额
};

const handleExchange = (item) => {
  ElMessageBox.confirm(
      `确定要消耗 ${item.pointsRequired} 积分兑换【${item.name}】吗？`,
      '兑换确认',
      { type: 'warning' }
  ).then(async () => {
    try {
      const res = await request.post(`/api/shop/exchange?userId=${userId}&goodsId=${item.goodsId}`);
      ElMessage.success(res.msg || '兑换成功！');
      // 兑换成功后，重新拉取商品列表(减库存)和用户积分
      fetchGoods();
      fetchUserPoints();
    } catch (e) {}
  });
};

onMounted(() => {
  fetchGoods();
  fetchUserPoints();
});
</script>

<style scoped>
.mall-container { padding: 20px; }
.mall-header { text-align: center; margin-bottom: 30px; }
.user-points-card { background: #fdf6ec; color: #e6a23c; padding: 10px 20px; border-radius: 8px; display: inline-block; }
.points-num { font-weight: bold; font-size: 18px; }

.goods-card { border-radius: 12px; transition: all 0.3s; }
.goods-card:hover { transform: translateY(-5px); box-shadow: 0 4px 20px rgba(0,0,0,0.1); }
.goods-image { width: 100%; height: 180px; object-fit: contain; }
.goods-name { font-size: 16px; margin: 10px 0; }
.goods-desc { font-size: 13px; color: #999; height: 40px; }
.bottom-area { margin-top: 15px; display: flex; justify-content: space-between; align-items: center; }
.points-tag { font-size: 18px; color: #f56c6c; font-weight: bold; }
</style>