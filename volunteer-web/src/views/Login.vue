<template>
  <div class="login-container">
    <!-- 背景装饰圆 -->
    <div class="circle circle-1"></div>
    <div class="circle circle-2"></div>

    <div class="login-box">
      <!-- 左侧：宣传插画区 -->
      <div class="login-left">
        <div class="glass-overlay">
          <div class="left-content">
            <el-icon class="logo-icon"><Promotion /></el-icon>
            <h2 class="system-title">志愿服务管理系统</h2>
            <p class="slogan">连接爱心，汇聚力量<br/>让每一份善意都有归处</p>
            <div class="feature-tags">
              <span>🚀 高效管理</span>
              <span>🤝 互助友爱</span>
              <span>🌟 价值实现</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧：表单区 -->
      <div class="login-right">
        <div class="form-wrapper">
          <h3 class="form-title">
            {{ isLoginMode ? '欢迎登录' : '注册志愿者' }}
          </h3>
          <p class="form-subtitle">
            {{ isLoginMode ? '请输入您的账号密码' : '加入我们，成为光荣的志愿者' }}
          </p>

          <!-- 登录/注册表单 -->
          <el-form
              ref="formRef"
              :model="form"
              :rules="rules"
              label-width="0"
              size="large"
              class="custom-form"
          >
            <!-- 账号 -->
            <el-form-item prop="username">
              <el-input
                  v-model="form.username"
                  placeholder="请输入账号"
                  :prefix-icon="User"
              />
            </el-form-item>

            <!-- 密码 -->
            <el-form-item prop="password">
              <el-input
                  v-model="form.password"
                  type="password"
                  placeholder="请输入密码"
                  show-password
                  :prefix-icon="Lock"
              />
            </el-form-item>

            <!-- 真实姓名 (仅注册显示) -->
            <transition name="el-zoom-in-top">
              <el-form-item prop="realName" v-if="!isLoginMode">
                <el-input
                    v-model="form.realName"
                    placeholder="请输入真实姓名 (用于证书生成)"
                    :prefix-icon="Postcard"
                />
              </el-form-item>
            </transition>

            <!-- 按钮区 -->
            <el-form-item>
              <el-button
                  type="primary"
                  class="submit-btn"
                  :loading="loading"
                  @click="handleSubmit"
                  round
              >
                {{ isLoginMode ? '立即登录' : '立即注册' }}
              </el-button>
            </el-form-item>
          </el-form>

          <!-- 底部切换 -->
          <div class="form-footer">
            <span v-if="isLoginMode">
              还没有账号？
              <a href="javascript:;" @click="toggleMode" class="toggle-link">去注册</a>
            </span>
            <span v-else>
              已有账号？
              <a href="javascript:;" @click="toggleMode" class="toggle-link">去登录</a>
            </span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue';
import { useRouter } from 'vue-router';
import { User, Lock, Postcard, Promotion } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import request from '../utils/request';

const router = useRouter();
const formRef = ref(null);
const isLoginMode = ref(true); // true: 登录, false: 注册
const loading = ref(false);

// 表单数据
const form = reactive({
  username: '',
  password: '',
  realName: ''
});

// 表单校验规则
const rules = {
  username: [
    { required: true, message: '请输入账号', trigger: 'blur' },
    { min: 3, max: 20, message: '长度在 3 到 20 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于 6 位', trigger: 'blur' }
  ],
  realName: [
    { required: true, message: '请输入真实姓名', trigger: 'blur' }
  ]
};

// 切换模式
const toggleMode = () => {
  isLoginMode.value = !isLoginMode.value;
  // 清空表单校验状态，避免红字残留
  if (formRef.value) formRef.value.resetFields();
};

// 提交处理
const handleSubmit = async () => {
  if (!formRef.value) return;

  await formRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true;
      try {
        if (isLoginMode.value) {
          // --- 登录逻辑 ---
          const res = await request.post('/api/auth/login', form);

          // 存储全套用户信息
          localStorage.setItem('token', res.data.token);
          localStorage.setItem('role', res.data.role);
          localStorage.setItem('realName', res.data.realName);
          localStorage.setItem('userId', res.data.userId);
          localStorage.setItem('username', res.data.username);
          localStorage.setItem('points', res.data.points || 0);
          localStorage.setItem('avatar', res.data.avatar || '');

          ElMessage.success(`欢迎回来，${res.data.realName}！`);
          router.push('/home');
        } else {
          // --- 注册逻辑 ---
          await request.post('/api/auth/register', form);
          ElMessage.success('注册成功，请登录');
          toggleMode(); // 自动切回登录
        }
      } catch (error) {
        // request.js 已拦截错误提示
      } finally {
        loading.value = false;
      }
    }
  });
};
</script>

<style scoped>
/* ====================================================
   🖥️ 默认样式 (PC 端)
   ==================================================== */
/* 全屏背景容器 */
.login-container {
  height: 100vh;
  width: 100vw;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  position: relative;
  overflow: hidden;
}

/* 装饰背景圆 */
.circle {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  z-index: 0;
}
.circle-1 {
  width: 400px;
  height: 400px;
  background: #ff9a9e;
  top: -100px;
  left: -100px;
  opacity: 0.4;
}
.circle-2 {
  width: 300px;
  height: 300px;
  background: #a18cd1;
  bottom: -50px;
  right: -50px;
  opacity: 0.4;
}

/* 登录卡片主体 */
.login-box {
  width: 900px;
  height: 550px;
  background: #fff;
  border-radius: 20px;
  box-shadow: 0 15px 35px rgba(0, 0, 0, 0.1);
  display: flex;
  overflow: hidden;
  z-index: 1;
}

/* --- 左侧区域 --- */
.login-left {
  flex: 1.1;
  background: url('https://source.unsplash.com/featured/800x600/?volunteer,community') center/cover no-repeat;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
}
/* 左侧磨砂玻璃遮罩 */
.glass-overlay {
  width: 100%;
  height: 100%;
  background: rgba(255, 107, 107, 0.85); /* 志愿红主色调，半透明 */
  backdrop-filter: blur(5px);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  padding: 40px;
  text-align: center;
}
.logo-icon {
  font-size: 60px;
  margin-bottom: 20px;
}
.system-title {
  font-size: 28px;
  margin-bottom: 10px;
  font-weight: 800;
  letter-spacing: 2px;
}
.slogan {
  font-size: 16px;
  opacity: 0.9;
  line-height: 1.6;
  margin-bottom: 30px;
}
.feature-tags span {
  display: inline-block;
  background: rgba(255, 255, 255, 0.2);
  padding: 5px 12px;
  border-radius: 20px;
  margin: 0 5px;
  font-size: 12px;
  border: 1px solid rgba(255, 255, 255, 0.3);
}

/* --- 右侧区域 --- */
.login-right {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #ffffff;
}
.form-wrapper {
  width: 80%;
  max-width: 350px;
}
.form-title {
  font-size: 26px;
  color: #333;
  margin-bottom: 10px;
}
.form-subtitle {
  color: #999;
  font-size: 14px;
  margin-bottom: 30px;
}
.custom-form .el-input__wrapper {
  background-color: #f5f7fa;
  box-shadow: none !important; /* 去掉默认边框 */
  border-radius: 8px;
  padding: 10px;
}
.custom-form .el-input__wrapper:hover,
.custom-form .el-input__wrapper.is-focus {
  background-color: #fff;
  box-shadow: 0 0 0 1px #ff6b6b !important; /* 聚焦时显示红色边框 */
}
.submit-btn {
  width: 100%;
  height: 45px;
  font-size: 16px;
  letter-spacing: 2px;
  background: linear-gradient(90deg, #ff6b6b, #ff8787);
  border: none;
  margin-top: 10px;
  box-shadow: 0 4px 12px rgba(255, 107, 107, 0.3);
  transition: all 0.3s;
}
.submit-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 15px rgba(255, 107, 107, 0.4);
}
.form-footer {
  text-align: center;
  margin-top: 20px;
  font-size: 14px;
  color: #666;
}
.toggle-link {
  color: #ff6b6b;
  text-decoration: none;
  font-weight: bold;
  margin-left: 5px;
  cursor: pointer;
}
.toggle-link:hover {
  text-decoration: underline;
}

/* ====================================================
   📱 移动端响应式适配 (屏幕宽度小于 768px 时生效)
   ==================================================== */
@media screen and (max-width: 768px) {
  /* 1. 盒子变窄，取消固定高度，改为上下排叠 */
  .login-box {
    width: 90%;
    height: auto;
    min-height: 500px;
    flex-direction: column;
    border-radius: 15px;
  }

  /* 2. 左侧的宣传图改为顶部的一个小 Banner */
  .login-left {
    flex: none;
    height: 180px; /* 固定一个小高度 */
  }

  /* 压缩磨砂遮罩内的元素间距 */
  .glass-overlay {
    padding: 20px;
  }
  .logo-icon {
    font-size: 40px; /* 缩小图标 */
    margin-bottom: 5px;
  }
  .system-title {
    font-size: 22px; /* 缩小标题 */
    margin-bottom: 5px;
  }
  .slogan {
    font-size: 13px;
    margin-bottom: 0;
  }
  /* 隐藏标签，节省垂直空间，防止手机端软键盘弹出时遮挡表单 */
  .feature-tags {
    display: none;
  }

  /* 3. 右侧表单区域填满剩余空间 */
  .login-right {
    padding: 30px 20px;
  }
  .form-wrapper {
    width: 100%;
    max-width: 100%;
  }
  .form-title {
    font-size: 22px;
  }
  .form-subtitle {
    margin-bottom: 20px;
  }

  /* 4. 调整背景装饰圆，防止在手机上过大 */
  .circle-1 {
    width: 250px;
    height: 250px;
    top: -50px;
    left: -50px;
  }
  .circle-2 {
    width: 200px;
    height: 200px;
    bottom: -20px;
    right: -20px;
  }
}
</style>