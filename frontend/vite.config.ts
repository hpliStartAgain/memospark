import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': new URL('./src', import.meta.url).pathname,
    },
  },
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/actuator': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  build: {
    outDir: '../src/main/resources/static',
    emptyOutDir: true,
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (!id.includes('node_modules')) return undefined
          if (id.includes('monaco-editor')) return 'editor'
          if (id.includes('recharts') || id.includes('/d3-')) return 'charts'
          if (id.includes('@tanstack') || id.includes('axios')) return 'data'
          if (id.includes('lucide-react')) return 'icons'
          if (id.includes('i18next')) return 'i18n'
          return 'vendor'
        },
      },
    },
  },
})
