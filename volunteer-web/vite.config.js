import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import basicSsl from '@vitejs/plugin-basic-ssl'

export default defineConfig({
  plugins: [
    vue(),
    basicSsl()
  ],
  server: {
    host: true,
    // 🚨 核心修复：配置本地代理
    proxy: {
      '/api': {
        // 这里写你后端的真实 HTTP 地址和端口 (注意看你报错里写的是 8081 端口)
        target: 'http://192.168.1.195:8081',
        changeOrigin: true, // 允许跨域
        secure: false       // 允许代理到不安全的 http 协议上
      }
    }
  }
})