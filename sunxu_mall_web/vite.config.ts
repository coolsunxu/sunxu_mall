import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src'),
    },
  },
  server: {
    port: 3001,
    open: true,
    proxy: {
      '/api': {
        target: 'http://localhost:8011',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, '/v1'),
      },
    },
  },
  build: {
    // 启用生产环境源码映射（调试用，生产可关闭）
    sourcemap: false,
    // 代码分割配置
    rollupOptions: {
      output: {
        // 分离第三方库
        manualChunks: {
          // Vue 相关库
          'vue-vendor': ['vue', 'vue-router', 'pinia'],
          // Element Plus（如果使用）
          // 'element-plus': ['element-plus'],
        },
        // 静态资源命名
        chunkFileNames: 'assets/js/[name]-[hash].js',
        entryFileNames: 'assets/js/[name]-[hash].js',
        assetFileNames: 'assets/[ext]/[name]-[hash].[ext]',
      },
    },
    // 压缩配置
    minify: 'terser',
    terserOptions: {
      compress: {
        // 生产环境移除 console 和 debugger
        drop_console: true,
        drop_debugger: true,
      },
    },
    // 块大小警告阈值
    chunkSizeWarningLimit: 1000,
  },
})
