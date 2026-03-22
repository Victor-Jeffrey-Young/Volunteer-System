import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import basicSsl from '@vitejs/plugin-basic-ssl'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig({
  plugins: [
    vue(),
    basicSsl(),
    tailwindcss()
  ],
  server: {
    host: true,
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
