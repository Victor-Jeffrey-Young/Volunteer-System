import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router'
import { useUserStore } from './stores/user'
import './style.css'

async function bootstrap() {
  const app = createApp(App)
  const pinia = createPinia()
  app.use(pinia)
  app.use(ElementPlus)

  // 刷新/直达时先以 JWT 身份回灌 role/userId，再由路由据此决定进入哪个界面。
  // 角色不再从 localStorage 读取，因此本地篡改 role 无法影响界面分配。
  const userStore = useUserStore()
  await userStore.loadCurrentUser().catch(() => {})

  app.use(router)
  app.mount('#app')
}

bootstrap()
