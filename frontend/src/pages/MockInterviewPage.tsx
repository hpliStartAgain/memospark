import { useEffect, useMemo, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { mockInterviewApi, targetApi } from '@/lib/api'
import { useAppStore } from '@/store/appStore'
import { formatDateTime, cn } from '@/lib/utils'
import Card, { CardBody, CardHeader } from '@/components/ui/Card'
import Button from '@/components/ui/Button'
import Badge from '@/components/ui/Badge'
import Input from '@/components/ui/Input'
import { PageSpinner } from '@/components/ui/Spinner'
import type { MockInterview, MockInterviewQuestion, MockInterviewType, TargetDetail } from '@/types'
import { ArrowLeft, CheckCircle, MessageSquareText, Play, Send, Trophy } from 'lucide-react'

const TYPE_OPTIONS: Array<{ value: MockInterviewType; label: string }> = [
  { value: 'MIXED', label: '综合' },
  { value: 'TECHNICAL', label: '技术深挖' },
  { value: 'SYSTEM_DESIGN', label: '系统设计' },
  { value: 'BEHAVIORAL', label: 'STAR 行为面' },
]

const dimensionMeta: Record<string, { label: string; variant: 'primary' | 'warning' | 'success' | 'info' }> = {
  STAR: { label: 'STAR', variant: 'warning' },
  TECHNICAL: { label: '技术', variant: 'primary' },
  SYSTEM_DESIGN: { label: '系统设计', variant: 'success' },
}

function scoreColor(score?: number) {
  if (score == null) return 'text-gray-400'
  if (score >= 80) return 'text-green-600 dark:text-green-400'
  if (score >= 60) return 'text-yellow-600 dark:text-yellow-400'
  return 'text-red-600 dark:text-red-400'
}

function QuestionItem({
  question,
  active,
  onClick,
}: {
  question: MockInterviewQuestion
  active: boolean
  onClick: () => void
}) {
  const meta = dimensionMeta[question.dimension] || { label: question.dimension, variant: 'info' as const }
  return (
    <button
      onClick={onClick}
      className={cn(
        'w-full text-left p-3 rounded-lg border transition-colors',
        active
          ? 'border-primary-500 bg-primary-50 dark:bg-primary-900/20'
          : 'border-gray-100 dark:border-gray-800 hover:bg-gray-50 dark:hover:bg-gray-800/60',
      )}
    >
      <div className="flex items-center justify-between gap-2">
        <Badge variant={meta.variant}>{meta.label}</Badge>
        {question.score != null ? (
          <span className={cn('text-sm font-bold', scoreColor(question.score))}>{question.score}</span>
        ) : (
          <span className="text-xs text-gray-400">未答</span>
        )}
      </div>
      <p className="text-sm text-gray-800 dark:text-gray-200 mt-2 line-clamp-2">
        {question.question}
      </p>
    </button>
  )
}

export default function MockInterviewPage() {
  const { id } = useParams()
  const targetId = Number(id)
  const navigate = useNavigate()
  const qc = useQueryClient()
  const { lang } = useAppStore()
  const [selectedId, setSelectedId] = useState<number | null>(null)
  const [selectedQuestionId, setSelectedQuestionId] = useState<number | null>(null)
  const [answer, setAnswer] = useState('')
  const [startForm, setStartForm] = useState({ type: 'MIXED' as MockInterviewType, count: 5 })

  const { data: target, isLoading: targetLoading } = useQuery<TargetDetail>({
    queryKey: ['target', String(targetId)],
    queryFn: () => targetApi.get(targetId),
    enabled: !Number.isNaN(targetId),
  })

  const { data: interviews = [], isLoading: listLoading } = useQuery<MockInterview[]>({
    queryKey: ['mockInterviews', targetId],
    queryFn: () => mockInterviewApi.list(targetId),
    enabled: !Number.isNaN(targetId),
  })

  useEffect(() => {
    if (!selectedId && interviews.length > 0) {
      setSelectedId(interviews[0].id)
    }
  }, [interviews, selectedId])

  const activeId = selectedId ?? interviews[0]?.id
  const { data: activeInterview, isLoading: interviewLoading } = useQuery<MockInterview>({
    queryKey: ['mockInterview', targetId, activeId],
    queryFn: () => mockInterviewApi.get(targetId, activeId as number),
    enabled: !!activeId && !Number.isNaN(targetId),
  })

  const currentQuestion = useMemo(() => {
    if (!activeInterview?.questions.length) return null
    if (selectedQuestionId) {
      const selected = activeInterview.questions.find(q => q.id === selectedQuestionId)
      if (selected) return selected
    }
    return activeInterview.questions.find(q => q.score == null) || activeInterview.questions[0]
  }, [activeInterview, selectedQuestionId])

  useEffect(() => {
    if (currentQuestion) {
      setSelectedQuestionId(currentQuestion.id)
      setAnswer(currentQuestion.userAnswer || '')
    }
  }, [currentQuestion?.id])

  const startMut = useMutation({
    mutationFn: () => mockInterviewApi.start(targetId, {
      type: startForm.type,
      count: startForm.count,
      language: lang,
    }),
    onSuccess: (created: MockInterview) => {
      setSelectedId(created.id)
      setSelectedQuestionId(created.questions[0]?.id ?? null)
      qc.setQueryData(['mockInterview', targetId, created.id], created)
      qc.invalidateQueries({ queryKey: ['mockInterviews', targetId] })
      qc.invalidateQueries({ queryKey: ['target', String(targetId)] })
    },
  })

  const answerMut = useMutation({
    mutationFn: () => mockInterviewApi.answer(targetId, activeInterview!.id, currentQuestion!.id, answer),
    onSuccess: (updated: MockInterview) => {
      qc.setQueryData(['mockInterview', targetId, updated.id], updated)
      qc.invalidateQueries({ queryKey: ['mockInterviews', targetId] })
      qc.invalidateQueries({ queryKey: ['target', String(targetId)] })
      const next = updated.questions.find(q => q.score == null)
      setSelectedQuestionId(next?.id ?? currentQuestion?.id ?? null)
      setAnswer(next?.userAnswer || '')
    },
  })

  const finishMut = useMutation({
    mutationFn: () => mockInterviewApi.finish(targetId, activeInterview!.id),
    onSuccess: (updated: MockInterview) => {
      qc.setQueryData(['mockInterview', targetId, updated.id], updated)
      qc.invalidateQueries({ queryKey: ['mockInterviews', targetId] })
      qc.invalidateQueries({ queryKey: ['target', String(targetId)] })
    },
  })

  if (targetLoading || listLoading) return <PageSpinner />

  const answeredCount = activeInterview?.answeredCount ?? 0
  const totalCount = activeInterview?.questionCount ?? 0
  const done = totalCount > 0 && answeredCount === totalCount

  return (
    <div className="p-6 space-y-6 max-w-6xl">
      <div className="flex items-start justify-between gap-3 flex-wrap">
        <div className="flex items-start gap-3">
          <button onClick={() => navigate(`/targets/${targetId}`)} className="p-1.5 mt-0.5 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-800 text-gray-500">
            <ArrowLeft className="w-5 h-5" />
          </button>
          <div>
            <h1 className="text-2xl font-bold text-gray-900 dark:text-white">模拟面试</h1>
            <p className="text-sm text-gray-500 dark:text-gray-400 mt-0.5">
              {target?.title || '目标岗位'} · 当前模拟分 {target?.readiness.mockPerformance ?? 0}
            </p>
          </div>
        </div>
        <div className="flex items-center gap-2">
          <select
            value={startForm.type}
            onChange={e => setStartForm(f => ({ ...f, type: e.target.value as MockInterviewType }))}
            className="px-3 py-2 text-sm rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-800 text-gray-900 dark:text-gray-100"
          >
            {TYPE_OPTIONS.map(opt => <option key={opt.value} value={opt.value}>{opt.label}</option>)}
          </select>
          <Input
            aria-label="题目数"
            type="number"
            min={1}
            max={8}
            value={startForm.count}
            onChange={e => setStartForm(f => ({ ...f, count: Number(e.target.value) }))}
            className="w-20"
          />
          <Button onClick={() => startMut.mutate()} loading={startMut.isPending}>
            <Play className="w-4 h-4" />开始新一场
          </Button>
        </div>
      </div>

      {startMut.isError && (
        <div className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 dark:border-red-900/50 dark:bg-red-900/20 dark:text-red-300">
          出题失败，请确认已配置 AI Key 后重试。
        </div>
      )}

      {!activeInterview ? (
        <Card>
          <CardBody className="py-16 text-center text-gray-400">
            <MessageSquareText className="w-12 h-12 mx-auto mb-3 opacity-40" />
            <p>还没有模拟面试记录。选择类型和题数，开始一场赛前训练。</p>
          </CardBody>
        </Card>
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-[320px_minmax(0,1fr)] gap-6">
          <div className="space-y-4">
            <Card>
              <CardHeader>
                <div className="flex items-center justify-between gap-2">
                  <h2 className="font-semibold text-gray-900 dark:text-white">本场进度</h2>
                  <Badge variant={activeInterview.status === 'FINISHED' ? 'success' : 'info'}>
                    {activeInterview.status === 'FINISHED' ? '已完成' : '进行中'}
                  </Badge>
                </div>
              </CardHeader>
              <CardBody className="space-y-3">
                <div className="flex items-center justify-between text-sm">
                  <span className="text-gray-500">题目</span>
                  <span className="font-medium">{answeredCount} / {totalCount}</span>
                </div>
                <div className="h-2 rounded-full bg-gray-100 dark:bg-gray-800 overflow-hidden">
                  <div className="h-full bg-primary-600 rounded-full transition-all" style={{ width: `${totalCount ? answeredCount / totalCount * 100 : 0}%` }} />
                </div>
                <div className="flex items-center justify-between text-sm">
                  <span className="text-gray-500">平均分</span>
                  <span className={cn('text-lg font-bold', scoreColor(activeInterview.averageScore))}>
                    {activeInterview.averageScore ?? '—'}
                  </span>
                </div>
                {activeInterview.summaryFeedback && (
                  <p className="text-xs text-gray-500 dark:text-gray-400 leading-relaxed">{activeInterview.summaryFeedback}</p>
                )}
                {done && (
                  <Button className="w-full" variant="success" onClick={() => finishMut.mutate()} loading={finishMut.isPending}>
                    <Trophy className="w-4 h-4" />刷新就绪度
                  </Button>
                )}
              </CardBody>
            </Card>

            {interviews.length > 0 && (
              <Card>
                <CardHeader><h2 className="font-semibold text-gray-900 dark:text-white">历史记录</h2></CardHeader>
                <CardBody className="space-y-2">
                  {interviews.map(item => (
                    <button
                      key={item.id}
                      onClick={() => { setSelectedId(item.id); setSelectedQuestionId(null) }}
                      className={cn(
                        'w-full rounded-lg px-3 py-2 text-left text-sm border transition-colors',
                        item.id === activeInterview.id
                          ? 'border-primary-500 bg-primary-50 dark:bg-primary-900/20'
                          : 'border-gray-100 dark:border-gray-800 hover:bg-gray-50 dark:hover:bg-gray-800/60',
                      )}
                    >
                      <div className="flex items-center justify-between gap-2">
                        <span>{TYPE_OPTIONS.find(opt => opt.value === item.type)?.label || item.type}</span>
                        <span className={cn('font-bold', scoreColor(item.averageScore))}>{item.averageScore ?? '—'}</span>
                      </div>
                      <p className="text-xs text-gray-400 mt-1">{formatDateTime(item.startedAt)}</p>
                    </button>
                  ))}
                </CardBody>
              </Card>
            )}
          </div>

          <div className="space-y-4">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
              {activeInterview.questions.map(q => (
                <QuestionItem
                  key={q.id}
                  question={q}
                  active={q.id === currentQuestion?.id}
                  onClick={() => {
                    setSelectedQuestionId(q.id)
                    setAnswer(q.userAnswer || '')
                  }}
                />
              ))}
            </div>

            <Card>
              <CardHeader>
                <div className="flex items-center justify-between gap-2">
                  <h2 className="font-semibold text-gray-900 dark:text-white">
                    第 {currentQuestion?.questionOrder ?? 1} 题
                  </h2>
                  {currentQuestion?.score != null && (
                    <span className={cn('text-xl font-bold', scoreColor(currentQuestion.score))}>{currentQuestion.score}</span>
                  )}
                </div>
              </CardHeader>
              <CardBody className="space-y-4">
                {interviewLoading ? (
                  <PageSpinner />
                ) : currentQuestion ? (
                  <>
                    <div className="rounded-lg bg-gray-50 dark:bg-gray-800/60 p-4">
                      <p className="text-base font-medium text-gray-900 dark:text-white leading-relaxed">
                        {currentQuestion.question}
                      </p>
                      {currentQuestion.rubric && (
                        <p className="text-xs text-gray-500 dark:text-gray-400 mt-3 whitespace-pre-wrap leading-relaxed">
                          {currentQuestion.rubric}
                        </p>
                      )}
                    </div>

                    <div className="space-y-1">
                      <label className="text-sm font-medium text-gray-700 dark:text-gray-300">你的回答</label>
                      <textarea
                        value={answer}
                        onChange={e => setAnswer(e.target.value)}
                        rows={8}
                        placeholder="按真实面试口径写下你的回答，尽量包含思路、权衡和结论。"
                        className="w-full px-3 py-2 text-sm rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-800 text-gray-900 dark:text-gray-100 focus:outline-none focus:ring-2 focus:ring-primary-500"
                      />
                    </div>

                    <div className="flex items-center justify-between gap-3 flex-wrap">
                      <p className="text-xs text-gray-400">提交后会逐题调用 AI 点评，并回写本场平均分。</p>
                      <Button
                        onClick={() => answerMut.mutate()}
                        loading={answerMut.isPending}
                        disabled={!answer.trim()}
                      >
                        {currentQuestion.score != null ? <CheckCircle className="w-4 h-4" /> : <Send className="w-4 h-4" />}
                        {currentQuestion.score != null ? '重新评分' : '提交点评'}
                      </Button>
                    </div>

                    {answerMut.isError && (
                      <p className="text-sm text-red-500">点评失败，请稍后重试；你的页面输入还在。</p>
                    )}

                    {currentQuestion.feedback && (
                      <div className="rounded-lg border border-primary-100 dark:border-primary-900/40 bg-primary-50/70 dark:bg-primary-900/10 p-4">
                        <h3 className="text-sm font-semibold text-primary-800 dark:text-primary-200 mb-2">AI 点评</h3>
                        <p className="text-sm text-gray-700 dark:text-gray-300 whitespace-pre-wrap leading-relaxed">
                          {currentQuestion.feedback}
                        </p>
                      </div>
                    )}
                  </>
                ) : (
                  <p className="text-sm text-gray-400">没有可用题目。</p>
                )}
              </CardBody>
            </Card>
          </div>
        </div>
      )}
    </div>
  )
}
