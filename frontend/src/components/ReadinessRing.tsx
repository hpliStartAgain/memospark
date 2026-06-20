import { cn } from '@/lib/utils'

interface ReadinessRingProps {
  value: number
  size?: number
  stroke?: number
  label?: string
  className?: string
}

function ringColor(v: number) {
  if (v >= 70) return '#16a34a' // green-600
  if (v >= 40) return '#eab308' // yellow-500
  return '#dc2626'              // red-600
}

export default function ReadinessRing({ value, size = 96, stroke = 8, label, className }: ReadinessRingProps) {
  const clamped = Math.max(0, Math.min(100, Math.round(value)))
  const radius = (size - stroke) / 2
  const circumference = 2 * Math.PI * radius
  const offset = circumference - (clamped / 100) * circumference
  const color = ringColor(clamped)

  return (
    <div className={cn('inline-flex flex-col items-center justify-center', className)}>
      <div className="relative" style={{ width: size, height: size }}>
        <svg width={size} height={size} className="-rotate-90">
          <circle
            cx={size / 2} cy={size / 2} r={radius}
            fill="none" strokeWidth={stroke}
            className="stroke-gray-200 dark:stroke-gray-700"
          />
          <circle
            cx={size / 2} cy={size / 2} r={radius}
            fill="none" strokeWidth={stroke} stroke={color}
            strokeLinecap="round"
            strokeDasharray={circumference}
            strokeDashoffset={offset}
            style={{ transition: 'stroke-dashoffset 0.5s ease' }}
          />
        </svg>
        <div className="absolute inset-0 flex flex-col items-center justify-center">
          <span className="text-xl font-bold text-gray-900 dark:text-white">{clamped}</span>
          <span className="text-[10px] text-gray-400">/ 100</span>
        </div>
      </div>
      {label && <span className="mt-1.5 text-xs text-gray-500 dark:text-gray-400">{label}</span>}
    </div>
  )
}
