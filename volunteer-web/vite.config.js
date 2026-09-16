import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import basicSsl from '@vitejs/plugin-basic-ssl'
import tailwindcss from '@tailwindcss/vite'

// 默认开自签 HTTPS：扫码签到依赖浏览器 getUserMedia，它只在安全上下文（HTTPS/localhost）里可用。
// 但有两种场景必须关掉自签证书：
//   ① Codespaces / Cloudflare Tunnel 这类「外层已经终结 TLS」的转发 —— 内层再用自签证书会握手失败；
//   ② 只想用 http 从局域网另一台设备访问。
//   npm run dev        → https（默认，本地开发用这个）
//   npm run dev:http   → http（外层已有 HTTPS 时用这个）
const useHttps = process.env.VITE_HTTPS !== 'false'

export default defineConfig({
  plugins: [
    vue(),
    ...(useHttps ? [basicSsl()] : []),
    tailwindcss()
  ],
  server: {
    host: true,
    // 允许任意 Host 头：Codespaces / 隧道会把访问域名换成 xxx-5173.app.github.dev 之类，
    // 而 Vite 默认只认 localhost，会直接返回 "Blocked request. This host is not allowed."
    // 只作用于本地 dev server（生产由 nginx 托管），不影响构建产物。
    allowedHosts: true,
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8081',
        changeOrigin: true,
        secure: false
      },
      '/files': {
        target: 'http://127.0.0.1:8081',
        changeOrigin: true,
        secure: false
      }
    }
  }
})
