<template>
  <div style="padding: 20px;">
    <h2>📣 新闻与公告管理</h2>

    <div style="margin-bottom: 20px;">
      <el-button type="primary" @click="openAdd">发布新公告</el-button>
    </div>

    <el-table :data="noticeList" border stripe>
      <el-table-column prop="title" label="标题" min-width="200" />
      <el-table-column label="类型" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.type === 1 ? 'warning' : 'success'">
            {{ scope.row.type === 1 ? '重要通知' : '志愿新闻' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="publisherName" label="发布人" width="120" />
      <el-table-column prop="createTime" label="发布时间" width="180" />
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="scope">
          <el-button size="small" @click="openEdit(scope.row)">编辑</el-button>
          <el-button size="small" type="danger" @click="handleDelete(scope.row.noticeId)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 发布/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="form.noticeId ? '编辑公告' : '发布新公告'" width="600px">
      <el-form label-width="80px">
        <el-form-item label="标题" required>
          <el-input v-model="form.title" placeholder="请输入公告标题" />
        </el-form-item>
        <el-form-item label="类型" required>
          <el-radio-group v-model="form.type">
            <el-radio :label="1">重要通知</el-radio>
            <el-radio :label="2">志愿新闻</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="正文" required>
          <el-input v-model="form.content" type="textarea" :rows="6" placeholder="请输入公告正文..." />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定发布</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import request from '../utils/request';

const noticeList = ref([]);
const dialogVisible = ref(false);
const form = ref({ noticeId: null, title: '', type: 1, content: '', publisherId: null });

const fetchNotices = async () => {
  const res = await request.get('/api/notice/page');
  noticeList.value = res.data.records;
};

const openAdd = () => {
  form.value = { noticeId: null, title: '', type: 1, content: '', publisherId: localStorage.getItem('userId') };
  dialogVisible.value = true;
};

const openEdit = (row) => {
  form.value = { ...row };
  dialogVisible.value = true;
};

const submitForm = async () => {
  if (!form.value.title || !form.value.content) return ElMessage.warning('标题和正文不能为空');
  if (form.value.noticeId) {
    await request.put('/api/notice/update', form.value);
  } else {
    await request.post('/api/notice/add', form.value);
  }
  ElMessage.success('操作成功');
  dialogVisible.value = false;
  fetchNotices();
};

const handleDelete = (id) => {
  ElMessageBox.confirm('确定删除该公告吗？', '提示', { type: 'warning' }).then(async () => {
    await request.delete(`/api/notice/${id}`);
    ElMessage.success('删除成功');
    fetchNotices();
  });
};

onMounted(fetchNotices);
</script>