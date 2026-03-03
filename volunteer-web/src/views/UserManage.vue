<template>
  <div style="padding: 20px;">
    <h2>用户管理模块测试</h2>

    <!-- 搜索栏 -->
    <div style="margin-bottom: 20px;">
      <input v-model="searchName" placeholder="按姓名搜索" />
      <button @click="fetchUserList(1)" style="margin-left: 10px;">搜索</button>
    </div>

    <!-- 用户列表 -->
    <table border="1" cellspacing="0" cellpadding="10" style="width: 100%; text-align: center;">
      <thead>
      <tr style="background-color: #f2f2f2;">
        <th>ID</th>
        <th>账号</th>
        <th>真实姓名</th>
        <th>角色</th>
        <th>总时长</th>
        <th>状态</th>
        <th>操作</th>
      </tr>
      </thead>
      <tbody>
      <tr v-for="user in userList" :key="user.userId">
        <td>{{ user.userId }}</td>
        <td>{{ user.username }}</td>
        <td>{{ user.realName }}</td>
        <td>{{ user.role }}</td>
        <td>{{ user.totalHours }}h</td>
        <td>
            <span :style="{ color: user.status === 1 ? 'green' : 'red' }">
              {{ user.status === 1 ? '正常' : '已禁用' }}
            </span>
        </td>
        <td>
          <button @click="toggleStatus(user)">
            {{ user.status === 1 ? '禁用' : '启用' }}
          </button>
          <button @click="deleteUser(user.userId)" style="margin-left: 5px; color: red;">删除</button>
        </td>
      </tr>
      </tbody>
    </table>

    <!-- 分页控件 -->
    <div style="margin-top: 20px;">
      <button :disabled="currentPage === 1" @click="fetchUserList(currentPage - 1)">上一页</button>
      <span style="margin: 0 15px;">当前页：{{ currentPage }} / 总页数：{{ totalPages }}</span>
      <button :disabled="currentPage === totalPages" @click="fetchUserList(currentPage + 1)">下一页</button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import request from '../utils/request';

const userList = ref([]);
const currentPage = ref(1);
const totalPages = ref(1);
const searchName = ref('');

// 获取用户分页列表
const fetchUserList = async (page) => {
  try {
    const res = await request.get('/api/user/page', {
      params: {
        current: page,
        size: 5, // 每页显示5条
        name: searchName.value
      }
    });
    userList.value = res.data.records; // MyBatis-Plus 返回的列表在 records 字段
    currentPage.value = res.data.current;
    totalPages.value = res.data.pages;
  } catch (error) {
    console.error("加载失败", error);
  }
};

// 切换用户状态 (禁用/启用)
const toggleStatus = async (user) => {
  const newStatus = user.status === 1 ? 0 : 1;
  try {
    await request.put('/api/user/status', {
      userId: user.userId,
      status: newStatus
    });
    alert('状态修改成功');
    fetchUserList(currentPage.value); // 刷新当前页
  } catch (error) {
    console.error(error);
  }
};

// 删除用户
const deleteUser = async (id) => {
  if (!confirm('确定要删除该用户吗？')) return;
  try {
    await request.delete(`/api/user/${id}`);
    alert('删除成功');
    fetchUserList(currentPage.value);
  } catch (error) {
    console.error(error);
  }
};

onMounted(() => {
  fetchUserList(1); // 页面加载时拉取第一页
});
</script>