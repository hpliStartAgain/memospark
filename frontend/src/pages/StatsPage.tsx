import { useTranslation } from 'react-i18next'
import { useQuery } from '@tanstack/react-query'
import { statsApi, targetApi } from '@/lib/api'
import { PageSpinner } from '@/components/ui/Spinner'
import Card, { CardBody, CardHeader } from '@/components/ui/Card'
import {
  AreaChart, Area, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer,
} from 'recharts'
import { BookOpen, Layers, Calendar, Flame, TrendingUp, CheckCircle } from 'lucide-react'
import type { DailyStats, Stats, TargetSummary } from '@/types'

function StatCard({ icon: Icon, label, value, color }: { icon: React.ElementType; label: string; value: string | number; color: string }) {
  return (
    <Card>
      <CardBody className="flex items-center gap-4">
        <div className={`p-3 rounded-xl ${color}`}>
          <Icon className="w-6 h-6 text-white" />
        </div>
        <div>
          <p className="text-2xl font-bold text-gray-900 dark:text-white">{value}</p>
          <p className="text-sm text-gray-500 dark:text-gray-400">{label}</p>
        </div>
      </CardBody>
    </Card>
  )
}

export default function StatsPage() {
  const { t } = useTranslation()
  const { data: stats, isLoading: sl } = useQuery<Stats>({ queryKey: ['stats'], queryFn: statsApi.summary })
  const { data: daily, isLoading: dl } = useQuery<DailyStats[]>({ queryKey: ['stats', 'daily'], queryFn: () => statsApi.daily(30) })
  const { data: targets = [], isLoading: tl } = useQuery<TargetSummary[]>({ queryKey: ['targets'], queryFn: targetApi.list })

  if (sl || dl || tl) return <PageSpinner />

  const chartData = (daily ?? []).map(d => ({
  date: d.date.slice(5),
  count: d.count,
  rate: d.retentionRate,
}))

  return (
    <div className="p-6 space-y-6 max-w-5xl">
      <h1 className="text-2xl font-bold text-gray-900 dark:text-white">{t('stats.title')}</h1>

      {/* Stat cards */}
      <div className="grid grid-cols-2 lg:grid-cols-3 gap-4">
        <StatCard icon={Layers}      label={t('stats.totalDecks')}    value={stats?.totalDecks ?? 0}      color="bg-primary-500" />
        <StatCard icon={BookOpen}    label={t('stats.totalCards')}    value={stats?.totalCards ?? 0}      color="bg-primary-500" />
        <StatCard icon={Calendar}    label={t('stats.dueToday')}      value={stats?.dueToday ?? 0}        color="bg-orange-500" />
        <StatCard icon={CheckCircle} label={t('stats.reviewedToday')} value={stats?.reviewedToday ?? 0}  color="bg-green-500" />
        <StatCard icon={TrendingUp}  label={t('stats.retention')}     value={`${stats?.retentionRate ?? 0}%`} color="bg-teal-500" />
        <StatCard icon={Flame}       label={t('stats.streak')}        value={`${stats?.streakDays ?? 0} ${t('stats.days')}`} color="bg-red-500" />
      </div>

      {/* Review trend chart */}
      <Card>
        <CardHeader>
          <h2 className="text-base font-semibold text-gray-900 dark:text-white">{t('stats.reviewTrend')}</h2>
        </CardHeader>
        <CardBody>
          <ResponsiveContainer width="100%" height={240}>
            <AreaChart data={chartData} margin={{ top: 4, right: 4, left: 0, bottom: 0 }}>
              <defs>
                <linearGradient id="grad" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%"  stopColor="#0ea5e9" stopOpacity={0.3} />
                  <stop offset="95%" stopColor="#0ea5e9" stopOpacity={0} />
                </linearGradient>
              </defs>
              <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" />
              <XAxis dataKey="date" tick={{ fontSize: 11 }} />
              <YAxis yAxisId="reviews" tick={{ fontSize: 11 }} />
              <YAxis yAxisId="retention" orientation="right" domain={[0, 100]} tick={{ fontSize: 11 }} />
              <Tooltip />
              <Area yAxisId="reviews" type="monotone" dataKey="count" stroke="#0ea5e9" fill="url(#grad)" name={t('stats.count')} />
              <Line yAxisId="retention" type="monotone" dataKey="rate" stroke="#16a34a" strokeWidth={2} dot={false} name={t('stats.rate')} />
            </AreaChart>
          </ResponsiveContainer>
        </CardBody>
      </Card>

      <Card>
        <CardHeader>
          <h2 className="text-base font-semibold text-gray-900 dark:text-white">目标岗位就绪度</h2>
        </CardHeader>
        <CardBody className="space-y-4">
          {targets.length === 0 ? (
            <p className="text-sm text-gray-400">还没有目标岗位。</p>
          ) : targets.map(target => (
            <div key={target.id} className="space-y-1">
              <div className="flex items-center justify-between gap-3 text-sm">
                <span className="font-medium text-gray-800 dark:text-gray-200 truncate">{target.title}</span>
                <span className="font-bold text-primary-600">{target.readiness}</span>
              </div>
              <div className="h-2 rounded-full bg-gray-100 dark:bg-gray-800 overflow-hidden">
                <div
                  className="h-full rounded-full bg-primary-600 transition-all"
                  style={{ width: `${Math.max(0, Math.min(100, target.readiness))}%` }}
                />
              </div>
            </div>
          ))}
        </CardBody>
      </Card>
    </div>
  )
}
