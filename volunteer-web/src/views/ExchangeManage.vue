<template>
  <div style="padding: 20px;">
    <h2>🧾 积分兑换记录审计</h2>

    <!-- 顶部筛选栏 -->
    <div style="margin-bottom: 20px; display: flex; gap: 15px; background: #fff; padding: 15px; border-radius: 8px;">
      <el-input
          v-model="searchCode"
          placeholder="输入核销码搜索 (如 GIFT-xxx)"
          clearable
          style="width: 250px;"
          @clear="fetchList(1)"
          @keyup.enter="fetchList(1)"
      >
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>

      <el-select v-model="filterStatus" placeholder="兑换状态" clearable style="width: 150px" @change="fetchList(1)">
        <el-option label="待核销" :value="0" />
        <el-option label="已领取" :value="1" />
      </el-select>

      <el-button type="primary" @click="fetchList(1)">查询</el-button>
    </div>

    <!-- 数据表格 -->
    <el-table :data="recordList" border stripe v-loading="loading">
      <el-table-column prop="redeemCode" label="核销码" width="160" align="center">
        <template #default="scope">
          <span style="font-family: monospace; font-weight: bold; color: #409eff;">{{ scope.row.redeemCode }}</span>
        </template>
      </el-table-column>

      <el-table-column label="兑换商品" min-width="200">
        <template #default="scope">
          <div style="display: flex; align-items: center; gap: 10px;">
            <el-image :src="scope.row.goodsImage" style="width: 40px; height: 40px; border-radius: 4px;" fit="cover" />
            <span>{{ scope.row.goodsName }}</span>
          </div>
        </template>
      </el-table-column>

      <el-table-column prop="userName" label="兑换人" width="120" align="center" />

      <el-table-column prop="costPoints" label="消耗积分" width="100" align="center">
        <template #default="scope">
          <span style="color: #e6a23c;">- {{ scope.row.costPoints }}</span>
        </template>
      </el-table-column>

      <el-table-column label="状态" width="100" align="center">
        <template #default="scope">
          <el-tag :type="scope.row.status === 1 ? 'success' : 'info'" effect="dark">
            {{ scope.row.status === 1 ? '已领取' : '待核销' }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column prop="createTime" label="兑换时间" width="170" align="center">
        <template #default="scope">{{ formatTime(scope.row.createTime) }}</template>
      </el-table-column>

      <el-table-column prop="exchangeTime" label="核销/领取时间" width="170" align="center">
        <template #default="scope">{{ formatTime(scope.row.exchangeTime) }}</template>
      </el-table-column>

      <!-- 操作列：提供手动核销功能 -->
      <el-table-column label="操作" width="120" fixed="right" align="center">
        <template #default="scope">
          <el-button
              v-if="scope.row.status === 0"
              type="success"
              size="small"
              @click="handleManualVerify(scope.row.redeemCode)"
          >
            确认发货
          </el-button>
          <span v-else style="color: #ccc; font-size: 12px;">已完成</span>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页器 -->
    <div style="margin-top: 20px; display: flex; justify-content: flex-end;">
      <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next, jumper"
          @current-change="handlePageChange"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { Search } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import request from '../utils/request';

const recordList = ref([]);
const loading = ref(false);
const searchCode = ref('');
const filterStatus = ref(null);
const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(0);

const formatTime = (timeStr) => {
  return timeStr ? timeStr.replace('T', ' ') : '--';
};

const fetchList = async (page = 1) => {
  loading.value = true;
  try {
    const res = await request.get('/api/shop/admin/record/page', {
      params: {
        current: page,
        size: pageSize.value,
        code: searchCode.value,
        status: filterStatus.value
      }
    });
    recordList.value = res.data.records;
    total.value = res.data.total;
    currentPage.value = res.data.current;
  } catch (e) {
    console.error(e);
  } finally {
    loading.value = false;
  }
};

const handlePageChange = (val) => {
  fetchList(val);
};

// 手动核销 (复用之前的核销接口)
const handleManualVerify = (code) => {
  ElMessageBox.confirm(`确认该用户已线下领取物品？核销码：${code}`, '发货确认', {
    confirmButtonText: '确认已发货',
    type: 'success'
  }).then(async () => {
    await request.post(`/api/shop/admin/verify?code=${code}`);
    ElMessage.success('核销成功');
    fetchList(currentPage.value); // 刷新列表
  }).catch(() => {});
};

onMounted(() => {
  fetchList();
});
</script>