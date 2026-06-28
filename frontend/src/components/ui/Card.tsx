import { cn } from '@/lib/utils'

interface CardProps {
  className?: string
  children: React.ReactNode
  onClick?: () => void
  hoverable?: boolean
}

export default function Card({ className, children, onClick, hoverable }: CardProps) {
  return (
    <div
      className={cn(
        'bg-white/90 dark:bg-gray-900/80 backdrop-blur-sm rounded-2xl border border-gray-200/70 dark:border-gray-800/70 shadow-sm',
        'transition-all duration-300',
        hoverable && 'hover:-translate-y-0.5 hover:shadow-lg hover:border-primary-300/60 dark:hover:border-primary-700/50 cursor-pointer',
        onClick && !hoverable && 'cursor-pointer',
        className,
      )}
      onClick={onClick}
    >
      {children}
    </div>
  )
}

export function CardHeader({ className, children }: { className?: string; children: React.ReactNode }) {
  return <div className={cn('px-6 py-4 border-b border-gray-100 dark:border-gray-800', className)}>{children}</div>
}

export function CardBody({ className, children }: { className?: string; children: React.ReactNode }) {
  return <div className={cn('px-6 py-4', className)}>{children}</div>
}

export function CardFooter({ className, children }: { className?: string; children: React.ReactNode }) {
  return <div className={cn('px-6 py-4 border-t border-gray-100 dark:border-gray-800', className)}>{children}</div>
}
