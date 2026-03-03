<template>
  <div class="login-container">
    <h2>{{ isLoginMode ? '志愿服务管理系统 - 登录' : '志愿者注册' }}</h2>

    <div class="form-box">
      <div>
        <label>账 号：</label>
        <input v-model="form.username" type="text" placeholder="输入账号" />
      </div>
      <div style="margin-top: 15px;">
        <label>密 码：</label>
        <input v-model="form.password" type="password" placeholder="输入密码" />
      </div>

      <!-- 注册时才显示的真实姓名栏 -->
      <div v-if="!isLoginMode" style="margin-top: 15px;">
        <label>姓 名：</label>
        <input v-model="form.realName" type="text" placeholder="输入真实姓名" />
      </div>

      <div style="margin-top: 20px;">
        <button v-if="isLoginMode" @click="handleLogin" class="primary-btn">登 录</button>
        <button v-else @click="handleRegister" class="primary-btn">注 册</button>

        <a href="javascript:void(0)" @click="toggleMode" style="margin-left: 15px; color: #1890ff;">
          {{ isLoginMode ? '没有账号？去注册' : '已有账号？去登录' }}
        </a>
      </div>
    </div>

    <!-- 结果展示区 -->
    <div v-if="resultInfo" class="result-box">
      <h3>后端返回结果：</h3>
      <pre>{{ resultInfo }}</pre>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import request from './utils/request';

const isLoginMode = ref(true); // true 为登录模式，false 为注册模式

const form = ref({
  username: '',
  password: '',
  realName: ''
});

const resultInfo = ref(null);

// 切换模式并清空表单
const toggleMode = () => {
  isLoginMode.value = !isLoginMode.value;
  form.value = { username: '', password: '', realName: '' };
  resultInfo.value = null;
};

// 真实登录请求
const handleLogin = async () => {
  try {
    const res = await request.post('/api/auth/login', form.value);
    localStorage.setItem('token', res.data.token);
    resultInfo.value = res.data;
    alert('登录成功！欢迎：' + res.data.realName);
  } catch (error) {
    console.error(error);
  }
};

// 真实注册请求
const handleRegister = async () => {
  if (!form.value.username || !form.value.password || !form.value.realName) {
    alert('请将信息填写完整！');
    return;
  }
  try {
    const res = await request.post('/api/auth/register', form.value);
    alert(res.msg); // 提示注册成功
    toggleMode();   // 注册成功后自动切换回登录页面
  } catch (error) {
    console.error(error);
  }
};
</script>

<style scoped>
.login-container { max-width: 420px; margin: 50px auto; font-family: sans-serif; }
.form-box { padding: 30px; border: 1px solid #ebeef5; border-radius: 8px; box-shadow: 0 2px 12px 0 rgba(0,0,0,0.1); }
input { padding: 8px; width: 220px; border: 1px solid #dcdfe6; border-radius: 4px; outline: none; }
input:focus { border-color: #409eff; }
.primary-btn { padding: 10px 20px; background-color: #409eff; color: white; border: none; cursor: pointer; border-radius: 4px; }
.primary-btn:hover { background-color: #66b1ff; }
.result-box { margin-top: 20px; padding: 15px; background-color: #f4f4f5; border-left: 5px solid #909399; }
</style>