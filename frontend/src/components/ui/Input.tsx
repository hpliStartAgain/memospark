import { cn } from '@/lib/utils'
import { type InputHTMLAttributes, forwardRef } from 'react'

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label?: string
  error?: string
}

const Input = forwardRef<HTMLInputElement, InputProps>(({ label, error, className, ...props }, ref) => (
  <div className="flex flex-col gap-1">
    {label && <label className="text-sm font-medium text-gray-700 dark:text-gray-300">{label}</label>}
    <input
      ref={ref}
      className={cn(
        'w-full px-4 py-2.5 text-sm rounded-lg border border-gray-300/80 dark:border-gray-600/80',
        'bg-white/80 dark:bg-gray-800/80 backdrop-blur-sm text-gray-900 dark:text-gray-100',
        'placeholder:text-gray-400 dark:placeholder:text-gray-500',
        'transition-all duration-300',
        'focus:outline-none focus:ring-2 focus:ring-primary-500/60 focus:border-transparent focus:shadow-md',
        'disabled:opacity-50 disabled:cursor-not-allowed',
        error && 'border-red-500 focus:ring-red-500',
        className,
      )}
      {...props}
    />
    {error && <p className="text-xs text-red-600 dark:text-red-400">{error}</p>}
  </div>
))
Input.displayName = 'Input'
export default Input
