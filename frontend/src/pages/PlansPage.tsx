import { useEffect, useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useNavigate, useParams } from 'react-router-dom'
import {
  ArrowRight, BookOpen, CalendarDays, Check, CheckCircle2,
  Clock3, RefreshCcw, Route, Sparkles, Target,
} from 'lucide-react'
import { planApi, targetApi } from '@/lib/api'
import { useAppStore } from '@/store/appStore'
import { useToastStore } from '@/store/toastStore'
import Button from '@/components/ui/Button'
import Input from '@/components/ui/Input'
import { PageSpinner } from '@/components/ui/Spinner'
import type { StudyPlan, StudyPlanItem, TargetSummary } from '@/types'

const stageLabel = {
  FOUNDATION: '入门',
  ADVANCED: '进阶',
  PRACTICE: '实战',
}

const typeLabel = {
  LEARN: '学习',
  REVIEW: '复习',
  PRACTICE: '练习',
  CHECKPOINT: '复盘',
}

function itemHref(item: StudyPlanItem) {
  if (item.type === 'LEARN' && item.deckId) return `/review/${item.deckId}`
  if (item.type === 'REVIEW') return item.deckId ? `/review/${item.deckId}` : '/review'
  if (item.type === 'PRACTICE') return '/practice'
  return null
}

export default function PlansPage() {
  const { targetId } = useParams<{ targetId?: string }>()
  const navigate = useNavigate()
  const qc = useQueryClient()
  const { lang } = useAppStore()
  const toast = useToastStore()
  const [weeklyHours, setWeeklyHours] = useState('7')
  const [targetDate, setTargetDate] = useState('')
  const [activeWeek, setActiveWeek] = useState(1)

  const { data: targets = [], isLoading: targetsLoading } = useQuery<TargetSummary[]>({
    queryKey: ['targets'],
    queryFn: targetApi.list,
  })
  const selectedId = Number(targetId || targets[0]?.id || 0)
  const selectedTarget = targets.find(target => target.id === selectedId)

  useEffect(() => {
    if (!targetId && targets[0]) navigate(`/plans/${targets[0].id}`, { replace: true })
  }, [navigate, targetId, targets])

  useEffect(() => {
    if (selectedTarget?.interviewDate) setTargetDate(selectedTarget.interviewDate)
  }, [selectedTarget?.interviewDate])

  const { data: plan, isLoading: planLoading } = useQuery<StudyPlan | null>({
    queryKey: ['plan', selectedId],
    queryFn: () => planApi.get(selectedId),
    enabled: selectedId > 0,
    retry: false,
  })

  const generate = useMutation<StudyPlan>({
    mutationFn: () => planApi.generate(selectedId, {
      weeklyHours: Number(weeklyHours),
      targetDate: targetDate || null,
      language: lang,
    }),
    onSuccess: result => {
      qc.setQueryData(['plan', selectedId], result)
      qc.invalidateQueries({ queryKey: ['plan', 'today'] })
      setActiveWeek(1)
      toast.success('学习计划已生成')
    },
  })

  const updateItem = useMutation({
    mutationFn: ({ id, completed }: { id: number; completed: boolean }) =>
      planApi.updateItem(id, completed),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['plan', selectedId] })
      qc.invalidateQueries({ queryKey: ['plan', 'today'] })
    },
  })

  const currentWeek = useMemo(
    () => plan?.weeks.find(week => week.weekNumber === activeWeek) || plan?.weeks[0],
    [activeWeek, plan],
  )

  if (targetsLoading) return <PageSpinner />

  if (targets.length === 0) {
    return (
      <div className="mx-auto max-w-3xl p-6 md:p-10">
        <Target className="h-10 w-10 text-gray-300" />
        <h1 className="mt-5 text-2xl font-semibold">先建立目标岗位</h1>
        <p className="mt-2 text-gray-500">学习计划需要以 JD 和目标日期为事实源。</p>
        <Button className="mt-6" onClick={() => navigate('/targets')}>创建目标</Button>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-7xl space-y-8 p-5 md:p-8">
      <div className="flex flex-col gap-4 border-b border-gray-200 pb-6 md:flex-row md:items-end md:justify-between dark:border-gray-800">
        <div>
          <p className="text-sm font-medium text-emerald-700 dark:text-emerald-400">目标驱动</p>
          <h1 className="mt-1 text-3xl font-semibold text-gray-950 dark:text-white">学习计划</h1>
          <p className="mt-2 max-w-2xl text-sm text-gray-500">
            长期路线由 JD 决定，未来四周按真实卡片与复习表现滚动更新。
          </p>
        </div>
        <select
          value={selectedId}
          onChange={event => navigate(`/plans/${event.target.value}`)}
          className="h-10 min-w-56 rounded-md border border-gray-300 bg-white px-3 text-sm dark:border-gray-700 dark:bg-gray-900"
        >
          {targets.map(target => <option key={target.id} value={target.id}>{target.title}</option>)}
        </select>
      </div>

      <div className="grid gap-4 border-b border-gray-200 pb-8 md:grid-cols-[160px_220px_auto] md:items-end dark:border-gray-800">
        <Input
          label="每周投入（小时）"
          type="number"
          min="3"
          max="40"
          value={weeklyHours}
          onChange={event => setWeeklyHours(event.target.value)}
        />
        <Input
          label="目标日期"
          type="date"
          value={targetDate}
          onChange={event => setTargetDate(event.target.value)}
        />
        <Button onClick={() => generate.mutate()} loading={generate.isPending}>
          {plan ? <RefreshCcw className="h-4 w-4" /> : <Sparkles className="h-4 w-4" />}
          {plan ? '重新规划' : '生成计划'}
        </Button>
      </div>

      {planLoading ? <PageSpinner /> : !plan ? (
        <div className="grid gap-8 py-10 md:grid-cols-[0.8fr_1.2fr]">
          <div>
            <Route className="h-10 w-10 text-[#c65f3f]" />
            <h2 className="mt-5 text-2xl font-semibold">还没有执行路线</h2>
            <p className="mt-3 leading-7 text-gray-500">
              生成后你会看到长期阶段、四周目标和每日任务。AI 负责确定优先级，
              到期复习和完成度由系统事实回算。
            </p>
          </div>
          <div className="border-l-2 border-gray-200 pl-6 dark:border-gray-700">
            {['读取 JD 与技能权重', '检查牌组的阶段与题量', '生成长期阶段', '排出未来四周的每天动作'].map((text, index) => (
              <div key={text} className="flex gap-4 py-3">
                <span className="font-mono text-xs text-gray-400">0{index + 1}</span>
                <span className="text-sm">{text}</span>
              </div>
            ))}
          </div>
        </div>
      ) : (
        <>
          <section className="grid gap-8 md:grid-cols-[1fr_320px]">
            <div>
              <div className="flex items-center gap-2 text-sm text-gray-500">
                <CalendarDays className="h-4 w-4" />
                {plan.startDate} 至 {plan.targetDate} · 每周 {plan.weeklyHours} 小时
              </div>
              <h2 className="mt-4 text-2xl font-semibold leading-tight">{plan.summary}</h2>
              <p className="mt-4 leading-7 text-gray-600 dark:text-gray-300">{plan.strategy}</p>
            </div>
            <div className="border border-gray-200 p-5 rounded-lg dark:border-gray-800">
              <p className="text-xs font-semibold uppercase text-gray-400">长期阶段</p>
              <div className="mt-4 space-y-5">
                {(plan.roadmap.phases || []).map(phase => (
                  <div key={`${phase.name}-${phase.startWeek}`} className="border-l-2 border-[#c65f3f] pl-4">
                    <div className="flex items-center justify-between gap-2">
                      <p className="font-medium">{phase.name}</p>
                      <span className="text-xs text-gray-400">W{phase.startWeek}-W{phase.endWeek}</span>
                    </div>
                    <p className="mt-1 text-xs leading-5 text-gray-500">{phase.goal}</p>
                  </div>
                ))}
              </div>
            </div>
          </section>

          <section>
            <div className="flex items-center justify-between gap-4">
              <div>
                <p className="text-sm font-medium text-emerald-700 dark:text-emerald-400">未来四周</p>
                <h2 className="mt-1 text-2xl font-semibold">执行窗口</h2>
              </div>
              <div className="inline-flex rounded-md border border-gray-200 p-1 dark:border-gray-700">
                {plan.weeks.map(week => (
                  <button
                    key={week.weekNumber}
                    onClick={() => setActiveWeek(week.weekNumber)}
                    className={`h-8 min-w-11 rounded px-3 text-sm ${
                      currentWeek?.weekNumber === week.weekNumber
                        ? 'bg-gray-950 text-white dark:bg-white dark:text-gray-950'
                        : 'text-gray-500'
                    }`}
                  >
                    W{week.weekNumber}
                  </button>
                ))}
              </div>
            </div>

            {currentWeek && (
              <div className="mt-6">
                <div className="flex flex-wrap items-center gap-3 border-b border-gray-200 pb-4 dark:border-gray-800">
                  <span className="rounded bg-emerald-50 px-2 py-1 text-xs font-medium text-emerald-700 dark:bg-emerald-950 dark:text-emerald-300">
                    {stageLabel[currentWeek.stage] || currentWeek.stage}
                  </span>
                  <p className="text-sm text-gray-600 dark:text-gray-300">{currentWeek.objective}</p>
                </div>
                <div className="divide-y divide-gray-200 dark:divide-gray-800">
                  {currentWeek.days.map(day => (
                    <div key={day.date} className="grid gap-3 py-5 md:grid-cols-[130px_1fr]">
                      <div>
                        <p className="font-medium">{new Date(`${day.date}T00:00:00`).toLocaleDateString('zh-CN', { weekday: 'short' })}</p>
                        <p className="mt-1 font-mono text-xs text-gray-400">{day.date.slice(5)}</p>
                      </div>
                      <div className="space-y-2">
                        {day.items.map(item => {
                          const href = itemHref(item)
                          const manuallyCompletable = item.type === 'PRACTICE' || item.type === 'CHECKPOINT'
                          return (
                            <div key={item.id} className="flex min-h-14 items-center gap-3 border border-gray-200 px-3 py-2 rounded-md dark:border-gray-800">
                              <button
                                title={manuallyCompletable ? '标记完成' : '完成度由学习记录自动计算'}
                                disabled={!manuallyCompletable || updateItem.isPending}
                                onClick={() => updateItem.mutate({ id: item.id, completed: !item.completed })}
                                className={`flex h-6 w-6 shrink-0 items-center justify-center rounded border ${
                                  item.completed ? 'border-emerald-600 bg-emerald-600 text-white' : 'border-gray-300 text-transparent'
                                } disabled:cursor-default`}
                              >
                                <Check className="h-4 w-4" />
                              </button>
                              <div className="min-w-0 flex-1">
                                <div className="flex flex-wrap items-center gap-2">
                                  <span className="text-xs text-gray-400">{typeLabel[item.type]}</span>
                                  <p className="truncate text-sm font-medium">{item.title}</p>
                                </div>
                                <p className="mt-1 text-xs text-gray-500">
                                  {item.completedCount}/{item.targetCount}
                                  {item.deckName ? ` · ${item.deckName}` : ''}
                                </p>
                              </div>
                              {href && (
                                <Button size="sm" variant="ghost" onClick={() => navigate(href)}>
                                  开始 <ArrowRight className="h-3.5 w-3.5" />
                                </Button>
                              )}
                              {item.completed && <CheckCircle2 className="h-4 w-4 text-emerald-600" />}
                            </div>
                          )
                        })}
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </section>

          {(plan.roadmap.risks?.length || 0) > 0 && (
            <section className="border-t border-gray-200 pt-6 dark:border-gray-800">
              <div className="flex items-start gap-3">
                <Clock3 className="mt-0.5 h-5 w-5 text-[#c65f3f]" />
                <div>
                  <h3 className="font-medium">计划护栏</h3>
                  {plan.roadmap.risks?.map(risk => <p key={risk} className="mt-2 text-sm text-gray-500">{risk}</p>)}
                </div>
              </div>
            </section>
          )}
        </>
      )}
    </div>
  )
}

