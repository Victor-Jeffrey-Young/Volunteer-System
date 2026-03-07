<template>
  <div style="padding: 20px;">
    <h2>🛒 积分商城商品管理</h2>

    <!-- 顶部操作栏 -->
    <div style="margin-bottom: 20px; display: flex; justify-content: space-between; align-items: center;">
      <el-button type="primary" :icon="Plus" @click="openAddDialog">上架新商品</el-button>
      <!-- 在“上架新商品”按钮旁边增加 -->
      <el-button type="success" :icon="Checked" @click="verifyVisible = true">核销兑换</el-button>

      <div style="display: flex;">
        <el-input
            v-model="searchName"
            placeholder="输入商品名称搜索"
            clearable
            style="width: 250px; margin-right: 10px;"
            @clear="fetchGoodsList(1)"
            @keyup.enter="fetchGoodsList(1)"
        />
        <el-button type="primary" :icon="Search" @click="fetchGoodsList(1)">搜索</el-button>
      </div>
    </div>

    <!-- 商品列表表格 -->
    <el-table :data="goodsList" border stripe v-loading="loading">
      <el-table-column prop="goodsId" label="ID" width="80" align="center" />

      <el-table-column label="商品主图" width="100" align="center">
        <template #default="scope">
          <el-image
              style="width: 50px; height: 50px; border-radius: 4px;"
              :src="scope.row.image"
              :preview-src-list="[scope.row.image]"
              preview-teleported
              fit="cover"
          >
            <template #error>
              <div class="image-slot">
                <el-icon><Picture /></el-icon>
              </div>
            </template>
          </el-image>
        </template>
      </el-table-column>

      <el-table-column prop="name" label="商品名称" min-width="150" />

      <el-table-column label="兑换单价" width="120" align="center">
        <template #default="scope">
          <span style="color: #e6a23c; font-weight: bold;">{{ scope.row.pointsRequired }} 积分</span>
        </template>
      </el-table-column>

      <el-table-column label="当前库存" width="120" align="center">
        <template #default="scope">
          <el-tag :type="scope.row.stock > 10 ? 'success' : (scope.row.stock > 0 ? 'warning' : 'danger')">
            {{ scope.row.stock > 0 ? `剩余 ${scope.row.stock} 件` : '已售罄' }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column prop="createTime" label="上架时间" width="170" align="center">
        <template #default="scope">
          {{ scope.row.createTime ? scope.row.createTime.replace('T', ' ') : '--' }}
        </template>
      </el-table-column>

      <!-- 操作列 -->
      <el-table-column label="操作" width="180" fixed="right" align="center">
        <template #default="scope">
          <el-button size="small" type="primary" plain @click="openEditDialog(scope.row)">编辑/补库</el-button>
          <el-button size="small" type="danger" plain @click="handleDelete(scope.row.goodsId)">下架</el-button>
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
          @current-change="handleCurrentChange"
      />
    </div>

    <!-- 上架/编辑商品弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="550px" destroy-on-close>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="商品名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入商品名称，如：社区纪念水杯" />
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="所需积分" prop="pointsRequired">
              <el-input-number v-model="form.pointsRequired" :min="1" :step="10" style="width: 100%;" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="初始库存" prop="stock">
              <el-input-number v-model="form.stock" :min="0" :step="5" style="width: 100%;" />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 📷 图片上传项 - 样式精修版 -->
        <el-form-item label="商品图片" prop="image">
          <div class="upload-container">
            <!-- 左侧：上传组件 -->
            <el-upload
                class="avatar-uploader"
                action="http://localhost:8080/api/file/upload"
                :show-file-list="false"
                :on-success="handleAvatarSuccess"
                :before-upload="beforeAvatarUpload"
                name="file"
            >
              <!-- 有图片时显示图片 -->
              <img v-if="form.image" :src="form.image" class="avatar" />
              <!-- 没图片时显示加号 -->
              <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>

              <!-- 悬浮遮罩（可选，增加交互感） -->
              <div v-if="form.image" class="upload-mask">
                <span>点击替换</span>
              </div>
            </el-upload>

            <!-- 右侧：网络链接输入 & 提示 -->
            <div class="upload-info">
              <el-input
                  v-model="form.image"
                  placeholder="粘贴网络图片链接，或点击左侧上传"
                  clearable
              >
                <template #prefix><el-icon><Link /></el-icon></template>
              </el-input>
              <div class="upload-tip">
                支持 JPG/PNG 格式，建议尺寸 1:1 (如 400x400)，大小不超过 2MB。
              </div>
            </div>
          </div>
        </el-form-item>

        <el-form-item label="商品描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="介绍一下该商品..." />
        </el-form-item>
      </el-form>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitLoading" @click="submitForm">确定保存</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 核销弹窗 -->
    <el-dialog v-model="verifyVisible" title="📦 礼品兑换核销" width="400px">
      <div style="text-align: center;">
        <p>请输入志愿者出示的兑换码（或扫描二维码）</p>
        <el-input
            v-model="verifyCode"
            placeholder="例如：GIFT-1234-56"
            size="large"
            style="margin: 20px 0;"
            clearable
        >
          <template #prefix><el-icon><Scissor /></el-icon></template>
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
import { ref, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Plus, Search, Picture,Checked, Scissor } from '@element-plus/icons-vue';
import request from '../utils/request';

const goodsList = ref([]);
const loading = ref(false);
const searchName = ref('');
const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(0);
const verifyVisible = ref(false);
const verifyCode = ref('');


// 弹窗相关
const dialogVisible = ref(false);
const dialogTitle = ref('');
const submitLoading = ref(false);
const formRef = ref(null);
const form = ref({
  goodsId: null,
  name: '',
  description: '',
  pointsRequired: 100,
  stock: 10,
  image: ''
});

// 简单的表单校验规则
const rules = {
  name:[{ required: true, message: '商品名称不能为空', trigger: 'blur' }],
  pointsRequired:[{ required: true, message: '积分价格不能为空', trigger: 'blur' }],
  stock:[{ required: true, message: '库存不能为空', trigger: 'blur' }]
};

// 获取列表
const fetchGoodsList = async (page = 1) => {
  loading.value = true;
  try {
    const res = await request.get('/api/shop/admin/page', {
      params: { current: page, size: pageSize.value, name: searchName.value }
    });
    goodsList.value = res.data.records;
    total.value = res.data.total;
    currentPage.value = res.data.current;
  } catch (error) {
    console.error(error);
  } finally {
    loading.value = false;
  }
};

const handleCurrentChange = (val) => {
  fetchGoodsList(val);
};

// 打开新增弹窗
const openAddDialog = () => {
  dialogTitle.value = '🛍️ 上架新商品';
  form.value = { goodsId: null, name: '', description: '', pointsRequired: 100, stock: 10, image: '' };
  dialogVisible.value = true;
};

// 打开编辑弹窗 (支持补库改价)
const openEditDialog = (row) => {
  dialogTitle.value = '✏️ 修改商品信息';
  form.value = { ...row }; // 深拷贝防止修改取消时污染列表
  dialogVisible.value = true;
};

// 提交表单
const submitForm = async () => {
  if (!formRef.value) return;
  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitLoading.value = true;
      try {
        if (form.value.goodsId) {
          await request.put('/api/shop/admin/update', form.value);
          ElMessage.success('商品修改成功');
        } else {
          await request.post('/api/shop/admin/add', form.value);
          ElMessage.success('商品上架成功');
        }
        dialogVisible.value = false;
        fetchGoodsList(currentPage.value); // 刷新列表
      } catch (error) {
        // 请求拦截器会处理错误
      } finally {
        submitLoading.value = false;
      }
    }
  });
};

// 删除商品
const handleDelete = (id) => {
  ElMessageBox.confirm('下架后，志愿者将无法在商城中看到该商品，且无法恢复。是否继续？', '下架确认', {
    confirmButtonText: '确认下架',
    cancelButtonText: '取消',
    type: 'error'
  }).then(async () => {
    try {
      await request.delete(`/api/shop/admin/${id}`);
      ElMessage.success('下架成功');
      // 处理当前页只有一条数据被删除的情况
      if (goodsList.value.length === 1 && currentPage.value > 1) {
        fetchGoodsList(currentPage.value - 1);
      } else {
        fetchGoodsList(currentPage.value);
      }
    } catch (error) {}
  }).catch(() => {});
};

const submitVerify = async () => {
  try {
    await request.post(`/api/shop/admin/verify?code=${verifyCode.value}`);
    ElMessage.success('核销成功！请发放物品。');
    verifyVisible.value = false;
    verifyCode.value = '';
  } catch (e) {
    // 错误在拦截器处理
  }
};

// 上传成功的回调
const handleAvatarSuccess = (response, uploadFile) => {
  if (response.code === 200) {
    // 后端返回了完整的 http://localhost:8080/files/xxx.png
    form.value.image = response.data;
    ElMessage.success('图片上传成功');
  } else {
    ElMessage.error('上传失败');
  }
};

// 上传前的校验 (限制图片大小和格式)
const beforeAvatarUpload = (rawFile) => {
  if (rawFile.type !== 'image/jpeg' && rawFile.type !== 'image/png') {
    ElMessage.error('图片必须是 JPG 或 PNG 格式!');
    return false;
  } else if (rawFile.size / 1024 / 1024 > 2) {
    ElMessage.error('图片大小不能超过 2MB!');
    return false;
  }
  return true;
};

onMounted(() => {
  fetchGoodsList();
});
</script>

<style scoped>
/* 容器布局：左图右文 */
.upload-container {
  display: flex;
  align-items: flex-start; /* 顶部对齐 */
  gap: 20px; /* 图片和输入框的间距 */
  width: 100%;
}

/* 上传组件样式重置 */
.avatar-uploader {
  position: relative;
  overflow: hidden;
  cursor: pointer;
  border: 1px dashed var(--el-border-color);
  border-radius: 6px;
  transition: var(--el-transition-duration-fast);
  width: 100px;  /* 固定宽度 */
  height: 100px; /* 固定高度 */
  flex-shrink: 0; /* 防止被挤压 */
}

.avatar-uploader:hover {
  border-color: var(--el-color-primary);
}

/* 加号图标样式 */
.avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 100px;
  height: 100px;
  text-align: center;
  line-height: 100px; /* 垂直居中 */
}

/* 图片样式 */
.avatar {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover; /* 保持比例填充，不拉伸变形 */
}

/* 右侧信息区域 */
.upload-info {
  flex: 1; /* 占满剩余空间 */
  display: flex;
  flex-direction: column;
  justify-content: center;
  height: 100px; /* 和图片高度保持一致，视觉更整齐 */
}

.upload-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 8px;
  line-height: 1.4;
}

/* (可选) 图片悬浮时的“点击替换”遮罩效果 */
.upload-mask {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.5);
  color: white;
  display: flex;
  justify-content: center;
  align-items: center;
  opacity: 0;
  transition: opacity 0.3s;
  font-size: 12px;
}

.avatar-uploader:hover .upload-mask {
  opacity: 1;
}
</style>