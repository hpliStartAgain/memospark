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
        'bg-white dark:bg-gray-900 rounded-xl border border-gray-200 dark:border-gray-800 shadow-sm',
        hoverable && 'transition-shadow hover:shadow-md cursor-pointer',
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
