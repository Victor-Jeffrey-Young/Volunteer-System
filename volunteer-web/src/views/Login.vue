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
import { useRouter } from 'vue-router'; // 1. 引入路由跳转钩子
import request from '../utils/request';

const router = useRouter(); // 2. 初始化路由实例
const isLoginMode = ref(true);

const form = ref({
  username: '',
  password: '',
  realName: ''
});

const toggleMode = () => {
  isLoginMode.value = !isLoginMode.value;
  form.value = { username: '', password: '', realName: '' };
};

// 登录处理
const handleLogin = async () => {
  try {
    const res = await request.post('/api/auth/login', form.value);

    // 🚨 检查点：确保后端返回的字段名是 token, role, realName
    // 如果你后端返回的是 username，这里也要对应改
    localStorage.setItem('userId', res.data.userId);
    localStorage.setItem('token', res.data.token);
    localStorage.setItem('role', res.data.role);     // 这个值必须是 'ADMIN' 或 'VOLUNTEER'
    localStorage.setItem('realName', res.data.realName);

    console.log('登录成功，角色为:', res.data.role); // 调试用

    // 确保存储完成后再跳转
    router.push('/home');
  } catch (error) {
    console.error("登录失败:", error);
  }
};

// 注册处理
const handleRegister = async () => {
  try {
    const res = await request.post('/api/auth/register', form.value);
    alert(res.msg || '注册成功');
    isLoginMode.value = true; // 注册成功自动切换到登录模式
  } catch (error) {
    console.error("注册出错", error);
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