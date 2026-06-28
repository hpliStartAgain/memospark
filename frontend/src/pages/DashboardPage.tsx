import { useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { statsApi, practiceApi, targetApi, planApi } from '@/lib/api'
import { useAppStore } from '@/store/appStore'
import { PageSpinner } from '@/components/ui/Spinner'
import Card, { CardBody, CardHeader } from '@/components/ui/Card'
import Button from '@/components/ui/Button'
import ReadinessRing from '@/components/ReadinessRing'
import type { Stats, TargetSummary, TargetDetail, ProblemNote, StudyPlan, StudyPlanItem } from '@/types'
import {
  BookOpen, AlertTriangle, Lightbulb, Target as TargetIcon, ArrowRight,
  Flame, CheckCircle2, CalendarClock, Plus, Sparkles, Code2, Bot, BarChart3,
  CalendarRange,
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
    <div className="flex items-center gap-3 p-3 rounded-xl border border-gray-100 dark:border-gray-800 transition-all duration-300 hover:border-primary-200 dark:hover:border-primary-800/60 hover:bg-primary-50/40 dark:hover:bg-primary-900/10">
      <div className={`p-2 rounded-xl ${done ? 'bg-green-100 dark:bg-green-900/20' : color}`}>
        {done ? <CheckCircle2 className="w-5 h-5 text-green-600" /> : <Icon className="w-5 h-5 text-white" />}
      </div>
      <div className="flex-1 min-w-0">
        <p className="text-sm font-medium text-gray-900 dark:text-white">{title}</p>
        <p className="text-xs text-gray-400 truncate">{done ? '今日已清空，做得好！' : desc}</p>
      </div>
      {!done && (
        <Button size="sm" variant="secondary" onClick={onClick}>
          {cta}<ArrowRight className="w-3.5 h-3.5" />
        </Button>
      )}
    </div>
  )
}

function QuickEntry({ icon: Icon, title, desc, to }: {
  icon: React.ElementType; title: string; desc: string; to: string
}) {
  const navigate = useNavigate()
  return (
    <button
      onClick={() => navigate(to)}
      className="group w-full text-left p-4 rounded-2xl bg-white/80 dark:bg-gray-900/70 backdrop-blur-sm border border-gray-200/70 dark:border-gray-800/70 shadow-sm transition-all duration-300 hover:-translate-y-0.5 hover:shadow-lg hover:border-primary-300/60 dark:hover:border-primary-700/50"
    >
      <div className="flex items-center gap-3">
        <div className="w-10 h-10 rounded-xl bg-primary-100 dark:bg-primary-900/30 flex items-center justify-center shrink-0">
          <Icon className="w-5 h-5 text-primary-600 dark:text-primary-400" />
        </div>
        <div className="flex-1 min-w-0">
          <p className="text-sm font-semibold text-gray-900 dark:text-white">{title}</p>
          <p className="text-xs text-gray-500 dark:text-gray-400 truncate">{desc}</p>
        </div>
        <ArrowRight className="w-4 h-4 text-gray-300 group-hover:text-primary-500 group-hover:translate-x-0.5 transition-all" />
      </div>
    </button>
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
  const { data: primaryPlan } = useQuery<StudyPlan | null>({
    queryKey: ['plan', primary?.id],
    queryFn: () => planApi.get(primary!.id),
    enabled: !!primary,
    retry: false,
  })
  const { data: todayPlan = [] } = useQuery<StudyPlanItem[]>({
    queryKey: ['plan', 'today'],
    queryFn: planApi.today,
  })

  if (sl || nl || tl) return <PageSpinner />

  const dueCards = stats?.dueToday ?? 0
  const reviewedToday = stats?.reviewedToday ?? 0
  const streakDays = stats?.streakDays ?? 0
  const retention = stats?.retentionRate ?? 0
  const weakSkills = (primaryDetail?.skills ?? [])
    .filter(s => s.selfLevel < 3)
    .sort((a, b) => b.weight - a.weight)
    .slice(0, 3)
  const remainingPlanItems = todayPlan.filter(item => !item.completed)

  // 备考进度：今日任务完成度
  const totalTasks = dueCards + dueNotes.length + weakSkills.length
  const doneTasks = (dueCards === 0 ? 1 : 0) + (dueNotes.length === 0 ? 1 : 0) + (weakSkills.length === 0 ? 1 : 0)
  const progress = totalTasks === 0 ? 100 : Math.round((doneTasks / 3) * 100)

  return (
    <div className="p-6 md:p-8 space-y-6 max-w-7xl mx-auto">
      {/* 顶部问候 + 进度带 */}
      <div className="fade-in-up">
        <div className="flex flex-wrap items-end justify-between gap-3">
          <div>
            <h1 className="text-2xl md:text-3xl font-bold tracking-tight text-gray-900 dark:text-white">
              {greeting()}，{user?.username}
            </h1>
            <p className="text-sm text-gray-500 dark:text-gray-400 mt-1">这是你今天的备考驾驶舱</p>
          </div>
          <div className="flex items-center gap-2 text-xs text-primary-700 dark:text-primary-300 bg-primary-50 dark:bg-primary-900/20 px-3 py-1.5 rounded-full">
            <Sparkles className="w-3.5 h-3.5" />
            今日备考进度 {progress}%
          </div>
        </div>
        <div className="mt-3 h-2 rounded-full bg-gray-100 dark:bg-gray-800 overflow-hidden">
          <div className="h-full rounded-full bg-gradient-to-r from-primary-500 to-primary-700 transition-all duration-700"
            style={{ width: `${progress}%` }} />
        </div>
      </div>

      {/* 左右两栏 */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* 左栏：就绪度 + 今日任务 */}
        <div className="lg:col-span-2 space-y-6">
          {/* 主目标就绪度 */}
          <Card>
            <CardHeader>
              <div className="flex items-center justify-between">
                <h2 className="text-base font-semibold text-gray-900 dark:text-white">主目标就绪度</h2>
                {primary && (
                  <button onClick={() => navigate(`/targets/${primary.id}`)}
                    className="text-xs text-primary-600 hover:underline inline-flex items-center gap-0.5">
                    查看详情 <ArrowRight className="w-3 h-3" />
                  </button>
                )}
              </div>
            </CardHeader>
            <CardBody>
              {primary ? (
                <div className="flex items-center gap-6">
                  <ReadinessRing value={primary.readiness} size={120} stroke={10} />
                  <div className="min-w-0 flex-1">
                    <p className="font-semibold text-lg text-gray-900 dark:text-white truncate">{primary.title}</p>
                    <p className="text-sm text-gray-500 dark:text-gray-400 flex items-center gap-1.5 mt-1.5">
                      <CalendarClock className="w-4 h-4" />{countdownText(primary.daysUntilInterview)}
                    </p>
                    <div className="mt-3 flex flex-wrap gap-2">
                      <Button size="sm" onClick={() => navigate(`/targets/${primary.id}`)}>
                        <TargetIcon className="w-3.5 h-3.5" />技能图谱
                      </Button>
                      <Button size="sm" variant="secondary" onClick={() => navigate(`/targets/${primary.id}/mock`)}>
                        模拟面试
                      </Button>
                    </div>
                  </div>
                </div>
              ) : (
                <div className="text-center py-4">
                  <TargetIcon className="w-10 h-10 mx-auto mb-3 text-gray-300" />
                  <p className="text-sm text-gray-500 mb-3">还没有目标岗位，先创建一个让备考有的放矢</p>
                  <Button size="sm" onClick={() => navigate('/targets')}>
                    <Plus className="w-3.5 h-3.5" />创建目标
                  </Button>
                </div>
              )}
            </CardBody>
          </Card>

          {/* 今日任务 */}
          <Card>
            <CardHeader>
              <h2 className="text-base font-semibold text-gray-900 dark:text-white">今日任务</h2>
            </CardHeader>
            <CardBody className="space-y-2.5">
              <TaskRow icon={CalendarRange} color="bg-emerald-600"
                title={primaryPlan
                  ? `${remainingPlanItems.length} 项计划待完成`
                  : '生成目标岗位学习计划'}
                desc={primaryPlan
                  ? (remainingPlanItems[0]?.title || '今天的计划已经完成')
                  : '把 JD、牌组和复习节奏排成每天可执行的路线'}
                count={primaryPlan ? remainingPlanItems.length : 1}
                cta={primaryPlan ? '看计划' : '去规划'}
                onClick={() => navigate(primary ? `/plans/${primary.id}` : '/targets')} />
              <TaskRow icon={BookOpen} color="bg-orange-500"
                title={`复习 ${dueCards} 张到期闪卡`} desc="保持记忆曲线，背诵八股不遗忘"
                count={dueCards} cta="去复习" onClick={() => navigate('/review')} />
              <TaskRow icon={AlertTriangle} color="bg-red-500"
                title={`攻克 ${dueNotes.length} 道待清错题`} desc="重做错题，巩固薄弱知识点"
                count={dueNotes.length} cta="去错题本" onClick={() => navigate('/notebook')} />
              <TaskRow icon={Lightbulb} color="bg-yellow-500"
                title={`提升 ${weakSkills.length} 项薄弱技能`}
                desc={weakSkills.length ? weakSkills.map(s => s.name).join('、') : '暂无薄弱技能'}
                count={weakSkills.length} cta="去强化" onClick={() => {
                  const top = weakSkills.find(s => s.deckId && s.cardCount > 0)
                  if (top?.deckId) navigate(`/review/${top.deckId}`)
                  else if (primary) navigate(`/targets/${primary.id}`)
                }} />
            </CardBody>
          </Card>
        </div>

        {/* 右栏：统计 + 薄弱技能 + 产品功能入口 */}
        <div className="space-y-6">
          {/* 快速统计 */}
          <Card>
            <CardHeader>
              <h2 className="text-base font-semibold text-gray-900 dark:text-white">学习数据</h2>
            </CardHeader>
            <CardBody className="space-y-3">
              <div className="flex items-center gap-3 p-3 rounded-xl bg-red-50 dark:bg-red-900/10">
                <div className="p-2.5 rounded-xl bg-red-500"><Flame className="w-5 h-5 text-white" /></div>
                <div className="flex-1">
                  <p className="text-xl font-bold text-gray-900 dark:text-white">{streakDays} <span className="text-xs font-normal text-gray-400">天</span></p>
                  <p className="text-xs text-gray-400">连续学习</p>
                </div>
              </div>
              <div className="flex items-center gap-3 p-3 rounded-xl bg-green-50 dark:bg-green-900/10">
                <div className="p-2.5 rounded-xl bg-green-500"><CheckCircle2 className="w-5 h-5 text-white" /></div>
                <div className="flex-1">
                  <p className="text-xl font-bold text-gray-900 dark:text-white">{reviewedToday} <span className="text-xs font-normal text-gray-400">张</span></p>
                  <p className="text-xs text-gray-400">今日已复习</p>
                </div>
              </div>
              <div className="flex items-center gap-3 p-3 rounded-xl bg-teal-50 dark:bg-teal-900/10">
                <div className="p-2.5 rounded-xl bg-teal-500"><BookOpen className="w-5 h-5 text-white" /></div>
                <div className="flex-1">
                  <p className="text-xl font-bold text-gray-900 dark:text-white">{retention}<span className="text-xs font-normal text-gray-400">%</span></p>
                  <p className="text-xs text-gray-400">记忆保留率</p>
                </div>
              </div>
              <Button variant="ghost" size="sm" className="w-full" onClick={() => navigate('/stats')}>
                查看完整统计 <ArrowRight className="w-3.5 h-3.5" />
              </Button>
            </CardBody>
          </Card>

          {/* 薄弱技能 */}
          {weakSkills.length > 0 && (
            <Card>
              <CardHeader>
                <h2 className="text-base font-semibold text-gray-900 dark:text-white">薄弱技能</h2>
              </CardHeader>
              <CardBody className="space-y-2.5">
                {weakSkills.map(s => (
                  <div key={s.id} className="flex items-center justify-between gap-2 text-sm">
                    <span className="text-gray-700 dark:text-gray-300 truncate">{s.name}</span>
                    <div className="flex items-center gap-2 shrink-0">
                      <div className="w-16 h-1.5 rounded-full bg-gray-200 dark:bg-gray-700 overflow-hidden">
                        <div className="h-full rounded-full bg-primary-500" style={{ width: `${(s.selfLevel / 5) * 100}%` }} />
                      </div>
                      <span className="text-xs text-gray-400 w-6 text-right">L{s.selfLevel}</span>
                    </div>
                  </div>
                ))}
              </CardBody>
            </Card>
          )}

          {/* 产品功能快捷入口 */}
          <Card>
            <CardHeader>
              <h2 className="text-base font-semibold text-gray-900 dark:text-white">快捷入口</h2>
            </CardHeader>
            <CardBody className="space-y-2.5">
              <QuickEntry icon={Code2} title="LeetCode 刷题" desc="Hot 100 在线评测" to="/practice" />
              <QuickEntry icon={BookOpen} title="闪卡牌组" desc="管理复习卡片" to="/decks" />
              <QuickEntry icon={TargetIcon} title="目标岗位" desc="JD 拆解与就绪度" to="/targets" />
              <QuickEntry icon={CalendarRange} title="学习计划" desc="路线、周目标与今日动作" to={primary ? `/plans/${primary.id}` : '/plans'} />
              <QuickEntry icon={Bot} title="AI 模拟面试" desc="按目标岗位出题" to={primary ? `/targets/${primary.id}/mock` : '/targets'} />
              <QuickEntry icon={BarChart3} title="数据看板" desc="学习曲线与保留率" to="/stats" />
            </CardBody>
          </Card>
        </div>
      </div>
    </div>
  )
}
