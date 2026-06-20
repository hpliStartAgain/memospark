import { useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { statsApi, practiceApi, targetApi } from '@/lib/api'
import { useAppStore } from '@/store/appStore'
import { PageSpinner } from '@/components/ui/Spinner'
import Card, { CardBody, CardHeader } from '@/components/ui/Card'
import Button from '@/components/ui/Button'
import ReadinessRing from '@/components/ReadinessRing'
import type { Stats, TargetSummary, TargetDetail, ProblemNote } from '@/types'
import {
  BookOpen, AlertTriangle, Lightbulb, Target as TargetIcon, ArrowRight,
  Flame, CheckCircle2, CalendarClock, Plus,
} from 'lucide-react'

function greeting() {
  const h = new Date().getHours()
  if (h < 6) return '夜深了'
  if (h < 12) return '早上好'
  if (h < 14) return '中午好'
  if (h < 18) return '下午好'
  return '晚上好'
}

function countdownText(days?: number) {
  if (days == null) return '未设置面试日期'
  if (days < 0) return `面试已过 ${Math.abs(days)} 天`
  if (days === 0) return '今天面试，加油！'
  return `距面试还有 ${days} 天`
}

function TaskRow({ icon: Icon, color, title, desc, count, cta, onClick }: {
  icon: React.ElementType; color: string; title: string; desc: string; count: number; cta: string; onClick: () => void
}) {
  const done = count === 0
  return (
    <div className="flex items-center gap-3 p-3 rounded-lg border border-gray-100 dark:border-gray-800">
      <div className={`p-2 rounded-lg ${done ? 'bg-green-100 dark:bg-green-900/20' : color}`}>
        {done ? <CheckCircle2 className="w-5 h-5 text-green-600" /> : <Icon className="w-5 h-5 text-white" />}
      </div>
      <div className="flex-1 min-w-0">
        <p className="text-sm font-medium text-gray-900 dark:text-white">{title}</p>
        <p className="text-xs text-gray-400">{done ? '今日已清空，做得好！' : desc}</p>
      </div>
      {!done && (
        <Button size="sm" variant="secondary" onClick={onClick}>
          {cta}<ArrowRight className="w-3.5 h-3.5" />
        </Button>
      )}
    </div>
  )
}

export default function DashboardPage() {
  const navigate = useNavigate()
  const { user } = useAppStore()

  const { data: stats, isLoading: sl } = useQuery<Stats>({ queryKey: ['stats'], queryFn: statsApi.summary })
  const { data: dueNotes = [], isLoading: nl } = useQuery<ProblemNote[]>({ queryKey: ['notebook', 'due'], queryFn: practiceApi.dueNotes })
  const { data: targets = [], isLoading: tl } = useQuery<TargetSummary[]>({ queryKey: ['targets'], queryFn: targetApi.list })

  const primary = targets[0]
  const { data: primaryDetail } = useQuery<TargetDetail>({
    queryKey: ['target', String(primary?.id)],
    queryFn: () => targetApi.get(primary!.id),
    enabled: !!primary,
  })

  if (sl || nl || tl) return <PageSpinner />

  const dueCards = stats?.dueToday ?? 0
  const weakSkills = (primaryDetail?.skills ?? [])
    .filter(s => s.selfLevel < 3)
    .sort((a, b) => b.weight - a.weight)
    .slice(0, 3)

  return (
    <div className="p-6 space-y-6 max-w-5xl">
      <div>
        <h1 className="text-2xl font-bold text-gray-900 dark:text-white">{greeting()}，{user?.username} 👋</h1>
        <p className="text-sm text-gray-500 dark:text-gray-400 mt-0.5">这是你今天的备考驾驶舱</p>
      </div>

      {/* Top row: readiness + quick stats */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
        {/* Primary target readiness */}
        <Card className="lg:col-span-1">
          <CardBody>
            {primary ? (
              <div className="flex items-center gap-4">
                <ReadinessRing value={primary.readiness} size={96} stroke={8} />
                <div className="min-w-0">
                  <p className="text-xs text-gray-400">主目标就绪度</p>
                  <p className="font-semibold text-gray-900 dark:text-white truncate">{primary.title}</p>
                  <p className="text-xs text-gray-500 flex items-center gap-1 mt-1">
                    <CalendarClock className="w-3.5 h-3.5" />{countdownText(primary.daysUntilInterview)}
                  </p>
                  <button onClick={() => navigate(`/targets/${primary.id}`)}
                    className="text-xs text-primary-600 hover:underline mt-1 inline-flex items-center gap-0.5">
                    查看详情 <ArrowRight className="w-3 h-3" />
                  </button>
                </div>
              </div>
            ) : (
              <div className="text-center py-2">
                <TargetIcon className="w-8 h-8 mx-auto mb-2 text-gray-300" />
                <p className="text-sm text-gray-500 mb-3">还没有目标岗位</p>
                <Button size="sm" onClick={() => navigate('/targets')}>
                  <Plus className="w-3.5 h-3.5" />创建目标
                </Button>
              </div>
            )}
          </CardBody>
        </Card>

        {/* Quick stats */}
        <Card className="lg:col-span-2">
          <CardBody className="grid grid-cols-3 gap-4 h-full items-center">
            <div className="flex items-center gap-3">
              <div className="p-3 rounded-xl bg-red-500"><Flame className="w-6 h-6 text-white" /></div>
              <div><p className="text-2xl font-bold text-gray-900 dark:text-white">{stats?.streakDays ?? 0}</p><p className="text-xs text-gray-400">连续学习天数</p></div>
            </div>
            <div className="flex items-center gap-3">
              <div className="p-3 rounded-xl bg-green-500"><CheckCircle2 className="w-6 h-6 text-white" /></div>
              <div><p className="text-2xl font-bold text-gray-900 dark:text-white">{stats?.reviewedToday ?? 0}</p><p className="text-xs text-gray-400">今日已复习</p></div>
            </div>
            <div className="flex items-center gap-3">
              <div className="p-3 rounded-xl bg-teal-500"><BookOpen className="w-6 h-6 text-white" /></div>
              <div><p className="text-2xl font-bold text-gray-900 dark:text-white">{stats?.retentionRate ?? 0}%</p><p className="text-xs text-gray-400">记忆保留率</p></div>
            </div>
          </CardBody>
        </Card>
      </div>

      {/* Today's tasks */}
      <Card>
        <CardHeader>
          <h2 className="text-base font-semibold text-gray-900 dark:text-white">今日任务</h2>
        </CardHeader>
        <CardBody className="space-y-2">
          <TaskRow icon={BookOpen} color="bg-orange-500"
            title={`复习 ${dueCards} 张到期闪卡`} desc="保持记忆曲线，背诵八股不遗忘"
            count={dueCards} cta="去复习" onClick={() => navigate('/review')} />
          <TaskRow icon={AlertTriangle} color="bg-red-500"
            title={`攻克 ${dueNotes.length} 道待清错题`} desc="重做错题，巩固薄弱知识点"
            count={dueNotes.length} cta="去错题本" onClick={() => navigate('/notebook')} />
          <TaskRow icon={Lightbulb} color="bg-yellow-500"
            title={`提升 ${weakSkills.length} 项薄弱技能`}
            desc={weakSkills.length ? weakSkills.map(s => s.name).join('、') : '暂无薄弱技能'}
            count={weakSkills.length} cta="去强化" onClick={() => primary && navigate(`/targets/${primary.id}`)} />
        </CardBody>
      </Card>
    </div>
  )
}
