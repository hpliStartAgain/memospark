import { create } from 'zustand'

export type ToastType = 'success' | 'error' | 'info' | 'warning'

export interface Toast {
  id: number
  type: ToastType
  message: string
}

interface ToastState {
  toasts: Toast[]
  add: (type: ToastType, message: string) => void
  remove: (id: number) => void
  success: (message: string) => void
  error: (message: string) => void
  info: (message: string) => void
  warning: (message: string) => void
}

let nextId = 1

export const useToastStore = create<ToastState>((set, get) => ({
  toasts: [],
  add: (type, message) => {
    const id = nextId++
    set(state => ({ toasts: [...state.toasts, { id, type, message }] }))
    // Auto-dismiss after 5s (errors after 8s)
    setTimeout(() => get().remove(id), type === 'error' ? 8000 : 5000)
  },
  remove: id => set(state => ({ toasts: state.toasts.filter(t => t.id !== id) })),
  success: msg => get().add('success', msg),
  error: msg => get().add('error', msg),
  info: msg => get().add('info', msg),
  warning: msg => get().add('warning', msg),
}))

// Convenience functions for non-React contexts (e.g. axios interceptors)
export const toast = {
  success: (msg: string) => useToastStore.getState().success(msg),
  error: (msg: string) => useToastStore.getState().error(msg),
  info: (msg: string) => useToastStore.getState().info(msg),
  warning: (msg: string) => useToastStore.getState().warning(msg),
}
