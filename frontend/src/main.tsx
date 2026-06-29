import React from 'react'
import ReactDOM from 'react-dom/client'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import axios from 'axios'
import App from './App'
import ToastContainer from './components/ui/Toast'
import { toast } from './store/toastStore'
import './index.css'
import './lib/i18n'

function extractErrorMessage(err: unknown): string {
  if (axios.isAxiosError(err)) {
    const data = err.response?.data
    if (data?.error) return data.error
    if (data?.message) return data.message
    if (err.response?.status === 403) return '没有权限执行此操作'
    if (err.response?.status === 404) return '请求的资源不存在'
    if (err.response?.status === 429) return '请求过于频繁，请稍后再试'
    if (err.message === 'Network Error') return '网络错误，无法连接到服务器'
    return `请求失败 (${err.response?.status || 'unknown'})`
  }
  if (err instanceof Error) return err.message
  return '发生未知错误'
}

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      staleTime: 30_000,
    },
    mutations: {
      onError: (err) => {
        toast.error(extractErrorMessage(err))
      },
    },
  },
})

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <QueryClientProvider client={queryClient}>
      <App />
      <ToastContainer />
    </QueryClientProvider>
  </React.StrictMode>,
)

// Register the PWA service worker (production builds only).
if (import.meta.env.PROD && 'serviceWorker' in navigator) {
  window.addEventListener('load', () => {
    navigator.serviceWorker.register('/sw.js').catch(() => {
      /* offline support is best-effort; ignore registration failures */
    })
  })
}
