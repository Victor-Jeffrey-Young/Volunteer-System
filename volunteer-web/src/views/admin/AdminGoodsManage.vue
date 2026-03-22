<template>
  <div class="goods-manage-container">
    <el-card shadow="never" class="box-card" :body-style="{ padding: isMobile ? '10px' : '20px' }">
      <template #header>
        <div class="card-header">
          <span style="font-size: 18px; font-weight: bold;">🛒 积分商城商品管理</span>
        </div>
      </template>

      <!-- 顶部操作栏 (响应式) -->
      <div class="toolbar" :class="{ 'mobile-toolbar': isMobile }">
        <div class="action-group">
          <el-button type="primary" :icon="Plus" @click="openAddDialog" class="action-btn">上架商品</el-button>
          <!-- 之前的核销按钮 -->
          <el-button type="success" :icon="Checked" @click="verifyVisible = true" class="action-btn">核销兑换</el-button>
        </div>

        <div class="search-group">
          <el-input v-model="searchName" placeholder="搜商品名称..." clearable class="search-input"
            @clear="fetchGoodsList(1)" @keyup.enter="fetchGoodsList(1)">
            <template #prefix><el-icon>
                <Search />
              </el-icon></template>
          </el-input>
          <el-button :icon="Refresh" circle @click="fetchGoodsList(1)" />
        </div>
      </div>

      <!-- ========================================== -->
      <!-- 🖥️ PC 端视图：标准表格 -->
      <!-- ========================================== -->
      <el-table v-if="!isMobile" :data="goodsList" stripe style="width: 100%; margin-top: 20px;" v-loading="loading">
        <el-table-column prop="goodsId" label="ID" width="70" align="center" />

        <el-table-column label="主图" width="90" align="center">
          <template #default="scope">
            <el-image style="width: 50px; height: 50px; border-radius: 4px; border: 1px solid #eee;"
              :src="scope.row.image" :preview-src-list="[scope.row.image]" preview-teleported fit="cover">
              <template #error>
                <div class="image-slot"><el-icon>
                    <Picture />
                  </el-icon></div>
              </template>
            </el-image>
          </template>
        </el-table-column>

        <el-table-column prop="name" label="商品名称" min-width="180" show-overflow-tooltip />

        <el-table-column label="积分单价" width="120" align="center">
          <template #default="scope">
            <span style="color: #e6a23c; font-weight: bold;">{{ scope.row.pointsRequired }}</span>
          </template>
        </el-table-column>

        <el-table-column label="分类" width="100" align="center">
          <template #default="scope">
            <el-tag size="small" type="info" effect="plain">{{ scope.row.category || '未分类' }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="库存状态" width="120" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.stock > 10 ? 'success' : (scope.row.stock > 0 ? 'warning' : 'danger')"
              effect="plain">
              {{ scope.row.stock > 0 ? `余 ${scope.row.stock}` : '已售罄' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="createTime" label="上架时间" width="160" align="center">
          <template #default="scope">{{ formatTime(scope.row.createTime) }}</template>
        </el-table-column>

        <el-table-column label="操作" width="180" fixed="right" align="center">
          <template #default="scope">
            <el-button size="small" type="primary" plain @click="openEditDialog(scope.row)">编辑</el-button>
            <el-button size="small" type="danger" plain @click="handleDelete(scope.row.goodsId)">下架</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- ========================================== -->
      <!-- 📱 移动端视图：电商卡片列表 -->
      <!-- ========================================== -->
      <div v-else class="mobile-list" v-loading="loading">
        <div v-for="item in goodsList" :key="item.goodsId" class="m-goods-card">
          <!-- 左侧大图 -->
          <div class="m-img-wrapper">
            <el-image :src="item.image" class="m-img" fit="cover">
              <template #error>
                <div class="m-img-error"><el-icon>
                    <Picture />
                  </el-icon></div>
              </template>
            </el-image>
            <div class="m-id-badge">#{{ item.goodsId }}</div>
          </div>

          <!-- 右侧信息 -->
          <div class="m-info">
            <div class="m-header">
              <h4 class="m-title">{{ item.name }}</h4>
              <el-tag :type="item.stock > 0 ? 'success' : 'danger'" size="small" effect="dark">
                {{ item.stock > 0 ? `库 ${item.stock}` : '售罄' }}
              </el-tag>
            </div>

            <div class="m-price">
              <el-icon>
                <Coin />
              </el-icon> {{ item.pointsRequired }} 积分
            </div>

            <div class="m-footer">
              <span class="m-time">{{ formatTimeShort(item.createTime) }}</span>
              <div class="m-actions">
                <el-button size="small" type="primary" plain :icon="Edit" @click="openEditDialog(item)"></el-button>
                <el-button size="small" type="danger" plain :icon="Delete"
                  @click="handleDelete(item.goodsId)"></el-button>
              </div>
            </div>
          </div>
        </div>

        <el-empty v-if="goodsList.length === 0" description="暂无商品" />
      </div>

      <!-- 分页控件 -->
      <div class="pagination-box" :class="{ 'mobile-pagination': isMobile }">
        <el-pagination v-model:current-page="currentPage" v-model:page-size="pageSize" :total="total" background
          :layout="isMobile ? 'total, prev, next' : 'total, prev, pager, next, jumper'" @current-change="fetchGoodsList"
          :size="isMobile ? 'small' : 'default'" />
      </div>
    </el-card>

    <!-- 上架/编辑商品弹窗 (响应式) -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" :width="isMobile ? '95%' : '600px'" align-center
      destroy-on-close>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="80px" label-position="top">
        <el-form-item label="商品名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入商品名称" />
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="所需积分" prop="pointsRequired">
              <el-input-number v-model="form.pointsRequired" :min="1" :step="10" style="width: 100%;"
                controls-position="right" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="初始库存" prop="stock">
              <el-input-number v-model="form.stock" :min="0" :step="5" style="width: 100%;" controls-position="right" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="商品分类" prop="category">
          <el-select v-model="form.category" placeholder="请选择分类" style="width: 100%;">
            <el-option label="生活用品" value="生活用品" />
            <el-option label="纪念品" value="纪念品" />
            <el-option label="电子产品" value="电子产品" />
            <el-option label="文具" value="文具" />
            <el-option label="虚拟卡券" value="虚拟卡券" />
            <el-option label="玩具" value="玩具" />
            <el-option label="游戏" value="游戏" />
          </el-select>
        </el-form-item>

        <!-- 📷 图片上传项 (适配移动端) -->
        <el-form-item label="商品图片" prop="image">
          <div class="upload-container" :class="{ 'mobile-upload': isMobile }">
            <!-- 上传组件 -->
            <el-upload class="avatar-uploader" action="/api/file/upload" :headers="uploadHeaders"
              :show-file-list="false" :on-success="handleAvatarSuccess" :before-upload="beforeAvatarUpload" name="file">
              <img v-if="form.image" :src="form.image" class="avatar" />
              <el-icon v-else class="avatar-uploader-icon">
                <Plus />
              </el-icon>
              <div v-if="form.image" class="upload-mask"><span>替换</span></div>
            </el-upload>

            <!-- 链接输入 -->
            <div class="upload-info">
              <el-input v-model="form.image" placeholder="粘贴图片链接" clearable>
                <template #prefix><el-icon>
                    <Link />
                  </el-icon></template>
              </el-input>
              <div class="upload-tip">
                支持 JPG/PNG，建议 1:1 比例，大小 &lt; 2MB。
              </div>
            </div>
          </div>
        </el-form-item>

        <el-form-item label="商品描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="介绍一下该商品..." />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitLoading" @click="submitForm">确定保存</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 核销弹窗 (集成扫码功能) -->
    <el-dialog v-model="verifyVisible" title="📦 礼品兑换核销" :width="isMobile ? '90%' : '400px'" align-center
      @close="stopScan" destroy-on-close>
      <div style="text-align: center; display: flex; flex-direction: column; align-items: center;">

        <!-- 摄像头取景框 (仅在点击扫码后显示) -->
        <div v-show="isScanning" id="reader-verify"
          style="width: 100%; min-height: 250px; background: #000; margin-bottom: 15px; border-radius: 8px; overflow: hidden;">
        </div>

        <p v-if="!isScanning">请输入兑换码，或点击下方按钮扫码</p>

        <!-- 扫码按钮 -->
        <el-button v-if="!isScanning" type="warning" size="large" round style="width: 100%; margin-bottom: 20px;"
          @click="startScan">
          启动摄像头扫码
        </el-button>

        <!-- 手动输入框 (作为备用方案) -->
        <el-input v-model="verifyCode" placeholder="例如：GIFT-xxx" size="large" clearable>
          <template #prefix><el-icon>
              <Scissor />
            </el-icon></template>
        </el-input>
      </div>

      <template #footer>
        <el-button @click="verifyVisible = false">取消</el-button>
        <el-button type="primary" @click="submitVerify" :disabled="!verifyCode">确认核销</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'; // 补全 nextTick
import { ElMessage, ElMessageBox } from 'element-plus';
import { Plus, Search, Picture, Link, Coin, Edit, Delete, Refresh, Checked, Scissor } from '@element-plus/icons-vue';
import request from '../../utils/request';
import { Html5Qrcode } from "html5-qrcode"; // 引入扫码库

// --- 响应式判断 ---
const isMobile = ref(window.innerWidth <= 768);
const handleResize = () => { isMobile.value = window.innerWidth <= 768; };

const goodsList = ref([]);
const loading = ref(false);
const searchName = ref('');
const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(0);

// 弹窗状态
const dialogVisible = ref(false);
const dialogTitle = ref('');
const submitLoading = ref(false);
const verifyVisible = ref(false);
const verifyCode = ref('');
const formRef = ref(null);

// 扫码相关变量
const isScanning = ref(false);
let html5QrCode = null;
let isProcessing = false; // 防抖锁

const form = ref({
  goodsId: null, name: '', category: '', description: '', pointsRequired: 100, stock: 10, image: ''
});

// 🚨 新增：让 el-upload 也能携带 Token 和角色，穿透后端的拦截器
const uploadHeaders = {
  Authorization: 'Bearer ' + localStorage.getItem('token'),
  Role: localStorage.getItem('role')
};

const rules = {
  name: [{ required: true, message: '名称必填', trigger: 'blur' }],
  category: [{ required: true, message: '分类必选', trigger: 'change' }],
  pointsRequired: [{ required: true, message: '积分必填', trigger: 'blur' }],
  stock: [{ required: true, message: '库存必填', trigger: 'blur' }]
};

const formatTime = (str) => str ? str.replace('T', ' ') : '--';
const formatTimeShort = (str) => str ? str.split('T')[0] : '--';

const fetchGoodsList = async (page = 1) => {
  if (typeof page === 'number') currentPage.value = page;
  loading.value = true;
  try {
    const res = await request.get('/api/shop/admin/page', {
      params: { current: currentPage.value, size: pageSize.value, name: searchName.value }
    });
    goodsList.value = res.data.records;
    total.value = res.data.total;
  } catch (e) { }
  finally { loading.value = false; }
};

const openAddDialog = () => {
  dialogTitle.value = '🛍️ 上架商品';
  form.value = { goodsId: null, name: '', category: '', description: '', pointsRequired: 100, stock: 10, image: '' };
  dialogVisible.value = true;
};

const openEditDialog = (row) => {
  dialogTitle.value = '✏️ 编辑商品';
  form.value = { ...row };
  dialogVisible.value = true;
};

const submitForm = async () => {
  if (!formRef.value) return;
  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitLoading.value = true;
      try {
        const url = form.value.goodsId ? '/api/shop/admin/update' : '/api/shop/admin/add';
        const method = form.value.goodsId ? 'put' : 'post';
        await request[method](url, form.value);
        ElMessage.success('操作成功');
        dialogVisible.value = false;
        fetchGoodsList(currentPage.value);
      } catch (e) { }
      finally { submitLoading.value = false; }
    }
  });
};

const handleDelete = (id) => {
  ElMessageBox.confirm('确定下架该商品吗？', '提示', { type: 'warning' }).then(async () => {
    await request.delete(`/api/shop/admin/${id}`);
    ElMessage.success('已下架');
    fetchGoodsList(currentPage.value);
  }).catch(() => { });
};


// 图片上传相关
const handleAvatarSuccess = (res) => {
  if (res.code === 200) { form.value.image = res.data; ElMessage.success('上传成功'); }
};
const beforeAvatarUpload = (file) => {
  if (file.size / 1024 / 1024 > 2) { ElMessage.error('图片需小于 2MB'); return false; }
  return true;
};

// 启动扫码
const startScan = async () => {
  isScanning.value = true;
  isProcessing = false;
  await nextTick(); // 等待 DOM 渲染

  html5QrCode = new Html5Qrcode("reader-verify"); // 注意 ID 别写错

  html5QrCode.start(
    { facingMode: "environment" },
    { fps: 10, qrbox: { width: 250, height: 250 } },
    async (decodedText) => {
      // 🚨 防抖逻辑
      if (isProcessing) return;
      isProcessing = true;

      // 1. 扫码成功，先播放声音或震动(可选)，然后安全关闭摄像头
      ElMessage.success('扫码成功！正在核销...');
      await stopScan();

      // 2. 自动填入并提交
      verifyCode.value = decodedText;
      submitVerify();
    },
    () => { }
  ).catch(err => {
    console.error(err);
    ElMessage.error('无法调用摄像头，请检查 HTTPS 或 权限');
    isScanning.value = false;
  });
};

// 安全停止扫码
const stopScan = async () => {
  if (html5QrCode) {
    try {
      if (html5QrCode.isScanning) {
        await html5QrCode.stop();
      }
      html5QrCode.clear();
    } catch (e) { console.error(e); }
  }
  isScanning.value = false;
};

// 提交核销 (原逻辑保持不变)
const submitVerify = async () => {
  if (!verifyCode.value) return;
  try {
    await request.post(`/api/shop/admin/verify?code=${verifyCode.value}`);
    ElMessage.success('核销成功！物品已发放');
    verifyVisible.value = false;
    verifyCode.value = '';
    // 可以在这里刷新一下商品列表(虽然不是必须的)
  } catch (e) { }
};

onMounted(() => {
  fetchGoodsList();
  window.addEventListener('resize', handleResize);
});
onUnmounted(() => {
  window.removeEventListener('resize', handleResize);
});
</script>

<style scoped>
/* PC端基础样式 */
.goods-manage-container {
  padding: 15px;
}

.box-card {
  border-radius: 8px;
  border: none;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.search-group {
  display: flex;
  gap: 10px;
}

.action-group {
  display: flex;
  gap: 10px;
}

.search-input {
  width: 250px;
}

.pagination-box {
  margin-top: 25px;
  display: flex;
  justify-content: flex-end;
}

/* 弹窗上传样式 */
.upload-container {
  display: flex;
  align-items: flex-start;
  gap: 20px;
  width: 100%;
}

.avatar-uploader {
  position: relative;
  overflow: hidden;
  cursor: pointer;
  border: 1px dashed #dcdfe6;
  border-radius: 6px;
  width: 100px;
  height: 100px;
  flex-shrink: 0;
}

.avatar-uploader:hover {
  border-color: #409eff;
}

.avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 100px;
  height: 100px;
  text-align: center;
  line-height: 100px;
}

.avatar {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.upload-mask {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s;
  font-size: 12px;
}

.avatar-uploader:hover .upload-mask {
  opacity: 1;
}

.upload-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  height: 100px;
}

.upload-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 8px;
}

.image-slot {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%;
  height: 100%;
  background: #f5f7fa;
  color: #909399;
}

/* 🚨 核心：现代橙色表格皮肤 */
:deep(.el-table) {
  --el-table-border-color: #e2e8f0;
  --el-table-header-bg-color: #f8fafc;
  border-radius: 16px !important;
  overflow: hidden !important;
  margin-top: 20px;
}

:deep(.el-table__row) {
  transition: all 0.2s;
}

:deep(.el-table__row:hover) {
  background-color: #fff7ed !important;
}

:deep(.el-table--border) {
  border: none !important;
}

/* ====================================================
   📱 移动端电商卡片布局 (小于 768px)
   ==================================================== */
@media screen and (max-width: 768px) {
  .goods-manage-container {
    padding: 5px;
  }

  .box-card {
    border-radius: 0;
    box-shadow: none !important;
  }

  /* 顶部工具栏 */
  .mobile-toolbar {
    flex-direction: column-reverse;
    gap: 15px;
  }

  .search-group,
  .action-group {
    width: 100%;
  }

  .search-input {
    flex: 1;
  }

  .action-btn {
    flex: 1;
  }

  /* 两个按钮平分宽度 */

  /* 移动端卡片列表 */
  .mobile-list {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  .m-goods-card {
    background: #fff;
    border: 1px solid #ebeef5;
    border-radius: 8px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
    display: flex;
    /* 左右结构 */
    padding: 10px;
    gap: 12px;
  }

  /* 左侧大图 */
  .m-img-wrapper {
    position: relative;
    width: 100px;
    height: 100px;
    flex-shrink: 0;
  }

  .m-img {
    width: 100%;
    height: 100%;
    border-radius: 6px;
    border: 1px solid #f0f0f0;
  }

  .m-id-badge {
    position: absolute;
    top: 0;
    left: 0;
    background: rgba(0, 0, 0, 0.6);
    color: #fff;
    font-size: 10px;
    padding: 2px 4px;
    border-top-left-radius: 6px;
    border-bottom-right-radius: 6px;
  }

  /* 右侧详情 */
  .m-info {
    flex: 1;
    display: flex;
    flex-direction: column;
    justify-content: space-between;
  }

  .m-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
  }

  .m-title {
    margin: 0;
    font-size: 15px;
    color: #333;
    line-height: 1.4;
    display: -webkit-box;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 2;
    line-clamp: 2;
    overflow: hidden;
  }

  .m-price {
    font-size: 16px;
    color: #e6a23c;
    font-weight: bold;
    display: flex;
    align-items: center;
    gap: 4px;
    margin: 5px 0;
  }

  .m-footer {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .m-time {
    font-size: 12px;
    color: #999;
  }

  .m-actions {
    display: flex;
    gap: 8px;
  }

  /* 弹窗上传区域适配 */
  .mobile-upload {
    flex-direction: column;
    /* 上下堆叠 */
    gap: 10px;
  }

  .mobile-upload .upload-info {
    height: auto;
    width: 100%;
  }

  .mobile-pagination {
    justify-content: center;
  }
}
</style>