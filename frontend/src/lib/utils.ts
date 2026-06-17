import { clsx, type ClassValue } from 'clsx'
import { twMerge } from 'tailwind-merge'

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

export function difficultyColor(difficulty: string) {
  switch (difficulty) {
    case 'Easy':   return 'text-green-600 dark:text-green-400'
    case 'Medium': return 'text-yellow-600 dark:text-yellow-400'
    case 'Hard':   return 'text-red-600 dark:text-red-400'
    default:       return 'text-gray-500'
  }
}

export function difficultyBg(difficulty: string) {
  switch (difficulty) {
    case 'Easy':   return 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-300'
    case 'Medium': return 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900/30 dark:text-yellow-300'
    case 'Hard':   return 'bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-300'
    default:       return 'bg-gray-100 text-gray-800 dark:bg-gray-800 dark:text-gray-300'
  }
}

export function statusColor(status: string) {
  switch (status) {
    case 'ACCEPTED':      return 'text-green-600 dark:text-green-400'
    case 'WRONG_ANSWER':  return 'text-red-600 dark:text-red-400'
    case 'TIME_LIMIT':    return 'text-orange-600 dark:text-orange-400'
    case 'COMPILE_ERROR': return 'text-purple-600 dark:text-purple-400'
    case 'RUNTIME_ERROR': return 'text-yellow-600 dark:text-yellow-400'
    case 'PENDING':       return 'text-gray-500'
    default:              return 'text-gray-500'
  }
}

export function formatDate(dateStr?: string) {
  if (!dateStr) return '—'
  return new Date(dateStr).toLocaleDateString('zh-CN')
}

export function formatDateTime(dateStr?: string) {
  if (!dateStr) return '—'
  return new Date(dateStr).toLocaleString('zh-CN')
}

export function qualityLabel(q: number) {
  const labels: Record<number, string> = {
    0: '完全忘记', 1: '严重错误', 2: '记错了',
    3: '勉强记住', 4: '记住了', 5: '轻松记住',
  }
  return labels[q] ?? '未知'
}
