import { useToastStore, type ToastType } from '@/store/toastStore'
import { CheckCircle2, XCircle, Info, AlertTriangle, X } from 'lucide-react'
import { cn } from '@/lib/utils'

const config: Record<ToastType, { icon: typeof Info; bg: string; border: string; text: string }> = {
  success: { icon: CheckCircle2, bg: 'bg-green-50 dark:bg-green-900/30', border: 'border-green-200 dark:border-green-800', text: 'text-green-800 dark:text-green-200' },
  error:   { icon: XCircle,      bg: 'bg-red-50 dark:bg-red-900/30',    border: 'border-red-200 dark:border-red-800',    text: 'text-red-800 dark:text-red-200' },
  info:    { icon: Info,         bg: 'bg-blue-50 dark:bg-blue-900/30',  border: 'border-blue-200 dark:border-blue-800',  text: 'text-blue-800 dark:text-blue-200' },
  warning: { icon: AlertTriangle,bg: 'bg-yellow-50 dark:bg-yellow-900/30',border: 'border-yellow-200 dark:border-yellow-800',text: 'text-yellow-800 dark:text-yellow-200' },
}

export default function ToastContainer() {
  const { toasts, remove } = useToastStore()

  if (toasts.length === 0) return null

  return (
    <div className="fixed top-4 right-4 z-[100] flex flex-col gap-2 max-w-sm w-full pointer-events-none">
      {toasts.map(t => {
        const { icon: Icon, bg, border, text } = config[t.type]
        return (
          <div
            key={t.id}
            className={cn(
              'flex items-start gap-3 p-3.5 rounded-lg border shadow-lg pointer-events-auto',
              'transition-all duration-300',
              bg, border,
            )}
            style={{ animation: 'toastSlideIn 0.3s ease-out' }}
          >
            <Icon className={cn('w-5 h-5 shrink-0 mt-0.5', text)} />
            <p className={cn('flex-1 text-sm leading-5', text)}>{t.message}</p>
            <button
              onClick={() => remove(t.id)}
              className={cn('shrink-0 p-0.5 rounded hover:bg-black/5 dark:hover:bg-white/10', text)}
            >
              <X className="w-3.5 h-3.5" />
            </button>
          </div>
        )
      })}
    </div>
  )
}
