<template>
  <div class="exchange-audit-container">
    <el-card shadow="never" class="box-card" :body-style="{ padding: isMobile ? '10px' : '20px' }">
      <template #header>
        <div class="card-header">
          <span style="font-size: 18px; font-weight: bold;">🧾 积分兑换记录审计</span>
        </div>
      </template>

      <!-- 顶部筛选栏 (响应式) -->
      <div class="toolbar" :class="{ 'mobile-toolbar': isMobile }">
        <div class="filter-group">
          <el-input
              v-model="searchCode"
              placeholder="搜核销码..."
              clearable
              class="filter-input"
              @clear="fetchList(1)"
              @keyup.enter="fetchList(1)"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>

          <el-select v-model="filterStatus" placeholder="状态筛选" clearable class="filter-select" @change="fetchList(1)">
            <el-option label="⏳ 待核销" :value="0" />
            <el-option label="✅ 已领取" :value="1" />
          </el-select>
        </div>

        <el-button type="primary" :icon="Refresh" @click="fetchList(1)" class="refresh-btn">刷新</el-button>
      </div>

      <!-- ========================================== -->
      <!-- 🖥️ PC 端视图：标准表格 -->
      <!-- ========================================== -->
      <el-table
          v-if="!isMobile"
          :data="recordList"
          border
          stripe
          style="width: 100%; margin-top: 20px;"
          v-loading="loading"
      >
        <el-table-column prop="redeemCode" label="核销码" width="160" align="center">
          <template #default="scope">
            <span class="code-font">{{ scope.row.redeemCode }}</span>
          </template>
        </el-table-column>

        <el-table-column label="兑换商品" min-width="200">
          <template #default="scope">
            <div style="display: flex; align-items: center; gap: 10px;">
              <el-image
                  :src="scope.row.goodsImage"
                  style="width: 40px; height: 40px; border-radius: 4px; border: 1px solid #eee;"
                  fit="cover"
              >
                <template #error><el-icon><Picture /></el-icon></template>
              </el-image>
              <span>{{ scope.row.goodsName }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="userName" label="兑换人" width="120" align="center" />

        <el-table-column label="消耗积分" width="100" align="center">
          <template #default="scope">
            <span style="color: #e6a23c; font-weight: bold;">- {{ scope.row.costPoints }}</span>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="100" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.status === 1 ? 'success' : 'info'" effect="dark">
              {{ scope.row.status === 1 ? '已领取' : '待核销' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="时间信息" width="260">
          <template #default="scope">
            <div style="font-size: 12px; color: #666;">
              <div>兑换: {{ formatTime(scope.row.createTime) }}</div>
              <div v-if="scope.row.exchangeTime">核销: {{ formatTime(scope.row.exchangeTime) }}</div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="120" fixed="right" align="center">
          <template #default="scope">
            <el-button
                v-if="scope.row.status === 0"
                type="success"
                size="small"
                plain
                @click="handleManualVerify(scope.row.redeemCode)"
            >
              确认发货
            </el-button>
            <span v-else style="color: #ccc; font-size: 12px;">已完成</span>
          </template>
        </el-table-column>
      </el-table>

      <!-- ========================================== -->
      <!-- 📱 移动端视图：流水账单卡片 -->
      <!-- ========================================== -->
      <div v-else class="mobile-list" v-loading="loading">
        <div v-for="item in recordList" :key="item.recordId" class="m-bill-card">
          <!-- 顶部：核销码 + 状态 -->
          <div class="m-card-header">
            <span class="m-code"># {{ item.redeemCode }}</span>
            <el-tag :type="item.status === 1 ? 'success' : 'info'" size="small" effect="dark">
              {{ item.status === 1 ? '已领取' : '待核销' }}
            </el-tag>
          </div>

          <!-- 中部：商品详情 -->
          <div class="m-card-body">
            <el-image :src="item.goodsImage" class="m-goods-img" fit="cover">
              <template #error><div class="m-img-error"><el-icon><Picture /></el-icon></div></template>
            </el-image>
            <!-- m-info-box 内部 -->
            <div class="m-info-box">
              <h4 class="m-goods-name">{{ item.goodsName }}</h4>

              <div class="m-user-row">
                <!-- 图标和名字放在一起，不要加额外的 margin -->
                <span style="display:flex; align-items:center; gap:4px;">
                  <el-icon><User /></el-icon> {{ item.userName }}
                </span>
                <!-- 积分自动靠右 -->
                <span class="m-cost">-{{ item.costPoints }} 积分</span>
              </div>
            </div>
          </div>

          <!-- 分割线 -->
          <div class="m-divider"></div>

          <!-- 底部：时间 + 操作 -->
          <div class="m-card-footer">
            <div class="m-time-box">
              <div>兑: {{ formatTime(item.createTime) }}</div>
              <div v-if="item.exchangeTime">核: {{ formatTime(item.exchangeTime) }}</div>
            </div>

            <div class="m-action-box">
              <el-button
                  v-if="item.status === 0"
                  type="success"
                  size="small"
                  @click="handleManualVerify(item.redeemCode)"
              >
                确认发货
              </el-button>
              <span v-else class="m-done-text">
                <el-icon><CircleCheckFilled /></el-icon>
              </span>
            </div>
          </div>
        </div>

        <el-empty v-if="recordList.length === 0" description="暂无记录" />
      </div>

      <!-- 分页控件 -->
      <div class="pagination-box" :class="{ 'mobile-pagination': isMobile }">
        <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :total="total"
            background
            :layout="isMobile ? 'total, prev, next' : 'total, prev, pager, next, jumper'"
            @current-change="fetchList"
            :small="isMobile"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue';
import { Search, Refresh, Picture, User, CircleCheckFilled } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import request from '../utils/request';

// --- 响应式判断 ---
const isMobile = ref(window.innerWidth <= 768);
const handleResize = () => { isMobile.value = window.innerWidth <= 768; };

const recordList = ref([]);
const loading = ref(false);
const searchCode = ref('');
const filterStatus = ref(null);
const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(0);

const formatTime = (timeStr) => timeStr ? timeStr.replace('T', ' ').substring(0, 16) : '--';

const fetchList = async (page = 1) => {
  if (typeof page === 'number') currentPage.value = page;
  loading.value = true;
  try {
    const res = await request.get('/api/shop/admin/record/page', {
      params: {
        current: currentPage.value,
        size: pageSize.value,
        code: searchCode.value,
        status: filterStatus.value
      }
    });
    recordList.value = res.data.records;
    total.value = res.data.total;
  } catch (e) { console.error(e); }
  finally { loading.value = false; }
};

// 手动核销
const handleManualVerify = (code) => {
  ElMessageBox.confirm(`确认该用户已线下领取物品？\n核销码：${code}`, '发货确认', {
    confirmButtonText: '确认已发货',
    cancelButtonText: '取消',
    type: 'success'
  }).then(async () => {
    await request.post(`/api/shop/admin/verify?code=${code}`);
    ElMessage.success('核销成功');
    fetchList(currentPage.value);
  }).catch(() => {});
};

onMounted(() => {
  fetchList();
  window.addEventListener('resize', handleResize);
});
onUnmounted(() => {
  window.removeEventListener('resize', handleResize);
});
</script>

<style scoped>
/* PC端基础样式 */
.exchange-audit-container { padding: 15px; }
.box-card { border-radius: 8px; border: none; }
.code-font { font-family: 'Consolas', monospace; font-weight: bold; color: #409eff; }

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  background-color: #f8f9fa;
  padding: 15px;
  border-radius: 6px;
}
.filter-group { display: flex; gap: 10px; }
.filter-input { width: 200px; }
.filter-select { width: 140px; }
.pagination-box { margin-top: 25px; display: flex; justify-content: flex-end; }

/* ====================================================
   📱 移动端响应式适配 (小于 768px)
   ==================================================== */
@media screen and (max-width: 768px) {
  .exchange-audit-container { padding: 5px; }
  .box-card { border-radius: 0; box-shadow: none !important; }

  /* 顶部工具栏垂直排列 */
  .mobile-toolbar {
    flex-direction: column;
    align-items: stretch;
    gap: 10px;
  }
  .filter-group {
    flex-direction: column;
    width: 100%;
  }
  .filter-input, .filter-select { width: 100% !important; }
  .refresh-btn { width: 100%; }

  /* 移动端卡片列表 */
  .mobile-list {
    margin-top: 15px;
    display: flex;
    flex-direction: column;
    gap: 15px;
  }

  /* 🧾 账单小票风格卡片 */
  .m-bill-card {
    background: #fff;
    border: 1px solid #ebeef5;
    border-radius: 8px;
    box-shadow: 0 2px 8px rgba(0,0,0,0.05);
    overflow: hidden;
    position: relative;
  }

  /* 顶部锯齿装饰（可选，模拟小票效果） */
  .m-bill-card::before {
    content: '';
    position: absolute;
    top: 0; left: 0; right: 0; height: 4px;
    background: repeating-linear-gradient(45deg, #f5f7fa, #f5f7fa 5px, #fff 5px, #fff 10px);
  }

  .m-card-header {
    padding: 12px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    border-bottom: 1px dashed #eee;
  }
  .m-code { font-family: monospace; font-weight: bold; font-size: 14px; color: #333; }

  /* 1. 优化商品区域布局 */
  .m-card-body {
    padding: 12px;
    display: flex;
    gap: 12px; /* 图片和右侧信息的间距 */
    align-items: center; /* 垂直居中 */
  }
  .m-goods-img { width: 60px; height: 60px; border-radius: 4px; border: 1px solid #f0f0f0; flex-shrink: 0; }
  .m-img-error { width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; background: #f5f7fa; color: #909399; }

  /* 2. 核心修复：优化右侧信息栏布局 */
  .m-info-box {
    flex: 1;
    display: flex;
    flex-direction: column;
    justify-content: center; /* 垂直居中 */
    gap: 6px; /* 控制内部元素的间距 */
  }

  .m-goods-name { margin: 0; font-size: 14px; color: #303133; font-weight: bold; }
  /* 3. 核心修复：让头像图标和名字“抱团” */
  .m-user-row {
    display: flex;
    align-items: center; /* 确保图标和文字垂直对齐 */
    gap: 6px;           /* 🚨 这里是关键：调整图标和文字的间距 */
    font-size: 13px;
    color: #666;
  }

  /* 4. 确保积分靠右 */
  .m-cost {
    margin-left: auto;  /* 自动把积分推到最右侧 */
    color: #e6a23c;
    font-weight: bold;
  }

  .m-divider {
    height: 1px;
    background-image: linear-gradient(to right, #ccc 50%, rgba(255,255,255,0) 0%);
    background-position: bottom;
    background-size: 10px 1px;
    background-repeat: repeat-x;
    margin: 0 12px;
  }

  .m-card-footer {
    padding: 10px 12px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    background: #fafafa;
  }
  .m-time-box { font-size: 11px; color: #999; line-height: 1.4; }
  .m-done-text { font-size: 20px; color: #67c23a; }

  .mobile-pagination { justify-content: center; }
}
</style>