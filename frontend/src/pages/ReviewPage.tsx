import { useState, useEffect, useCallback } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { cardApi, reviewApi } from '@/lib/api'
import { PageSpinner } from '@/components/ui/Spinner'
import Button from '@/components/ui/Button'
import Card, { CardBody } from '@/components/ui/Card'
import Badge from '@/components/ui/Badge'
import type { AnswerChatMessage, AnswerEvaluation, ReviewCard } from '@/types'
import {
  RotateCcw, ChevronLeft, Brain, CheckCircle, Eye, Send,
  MessageCircle, Save, Mic, GraduationCap, CalendarClock, Lightbulb,
} from 'lucide-react'
import { cn } from '@/lib/utils'

const QUALITY_BTNS = [
  { q: 0, label: '未掌握', color: 'bg-red-600 hover:bg-red-700 text-white' },
  { q: 1, label: '再来', color: 'bg-red-500 hover:bg-red-600 text-white' },
  { q: 2, label: '困难', color: 'bg-orange-500 hover:bg-orange-600 text-white' },
  { q: 3, label: '模糊', color: 'bg-yellow-500 hover:bg-yellow-600 text-white' },
  { q: 4, label: '不错', color: 'bg-primary-500 hover:bg-primary-600 text-white' },
  { q: 5, label: '简单', color: 'bg-green-500 hover:bg-green-600 text-white' },
]

const STAGE_LABEL = {
  FOUNDATION: '入门',
  ADVANCED: '进阶',
  PRACTICE: '实战',
}

const DIFFICULTY_LABEL = {
  EASY: '简单',
  MEDIUM: '中等',
  HARD: '困难',
}

function gradeVariant(grade?: string): 'success' | 'primary' | 'warning' | 'danger' | 'default' {
  if (grade === 'A' || grade === 'B') return 'success'
  if (grade === 'C') return 'warning'
  if (grade === 'D' || grade === 'E') return 'danger'
  return 'default'
}

export default function ReviewPage() {
  const { t } = useTranslation()
  const { deckId } = useParams<{ deckId: string }>()
  const navigate = useNavigate()
  const qc = useQueryClient()
  const reviewQueryKey = ['review', deckId ?? 'today'] as const

  const [idx, setIdx] = useState(0)
  const [done, setDone] = useState(false)
  const [startTime] = useState(() => Date.now())
  const [cardStartedAt, setCardStartedAt] = useState(() => Date.now())
  const [streak, setStreak] = useState(0)
  const [revealed, setRevealed] = useState(false)
  const [userAnswer, setUserAnswer] = useState('')
  const [evaluation, setEvaluation] = useState<AnswerEvaluation | null>(null)
  const [selectedQuality, setSelectedQuality] = useState<number | null>(null)
  const [finalAnswer, setFinalAnswer] = useState('')
  const [followUp, setFollowUp] = useState('')
  const [chat, setChat] = useState<AnswerChatMessage[]>([])

  const resetAnswerState = useCallback(() => {
    setRevealed(false)
    setUserAnswer('')
    setEvaluation(null)
    setSelectedQuality(null)
    setFinalAnswer('')
    setFollowUp('')
    setChat([])
  }, [])

  const { data: cards = [], isLoading } = useQuery<ReviewCard[]>({
    queryKey: reviewQueryKey,
    queryFn: () => deckId ? reviewApi.byDeck(+deckId) : reviewApi.today(),
  })

  const card = cards[idx]
  const activeCardId = card?.cardId
  const firstLearning = card?.isNew ?? false

  useEffect(() => {
    if (!activeCardId) return
    resetAnswerState()
    setCardStartedAt(Date.now())
  }, [activeCardId, resetAnswerState])

  const evaluateMut = useMutation<AnswerEvaluation>({
    mutationFn: () => {
      if (!card) throw new Error('No active card')
      return reviewApi.evaluateAnswer(card.cardId, userAnswer)
    },
    onSuccess: result => {
      setEvaluation(result)
      setSelectedQuality(result.quality)
      setFinalAnswer(result.suggestedAnswer || card?.back || '')
      setRevealed(true)
      setChat([])
    },
  })

  const explainMut = useMutation<{ reply: string }, Error, string>({
    mutationFn: message => {
      if (!card) throw new Error('No active card')
      return reviewApi.explainAnswer(card.cardId, { userAnswer, message, history: chat })
    },
    onMutate: message => {
      setChat(current => [...current, { role: 'user', content: message }])
      setFollowUp('')
    },
    onSuccess: res => {
      setChat(current => [...current, { role: 'assistant', content: res.reply }])
    },
    onError: () => {
      setChat(current => [...current, { role: 'assistant', content: 'AI 解释暂时不可用，请稍后再试。' }])
    },
  })

  const replaceAnswerMut = useMutation<ReviewCard>({
    mutationFn: () => {
      if (!card) throw new Error('No active card')
      return cardApi.update(card.deckId, card.cardId, {
        front: card.front,
        back: finalAnswer,
        tags: card.tags || null,
      })
    },
    onSuccess: updated => {
      qc.setQueryData<ReviewCard[]>(reviewQueryKey, old =>
        old?.map(item => item.cardId === updated.cardId ? { ...item, back: updated.back } : item),
      )
    },
  })

  const reviewMut = useMutation({
    mutationFn: ({ cardId, quality, evidence }: { cardId: number; quality: number; evidence: object }) =>
      reviewApi.submit(cardId, quality, Date.now() - cardStartedAt, evidence),
    onSuccess: (_data, variables) => {
      qc.invalidateQueries({ queryKey: ['decks'] })
      setStreak(current => variables.quality >= 4 ? current + 1 : 0)
      const next = idx + 1
      if (next >= cards.length) {
        setDone(true)
      } else {
        setIdx(next)
      }
    },
  })

  const undoMut = useMutation({
    mutationFn: (cardId: number) => reviewApi.undo(cardId),
    onSuccess: () => {
      if (idx > 0) {
        setIdx(i => i - 1)
        setDone(false)
        setStreak(0)
      }
    },
  })

  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      const tag = (event.target as HTMLElement | null)?.tagName
      if (['INPUT', 'TEXTAREA', 'SELECT'].includes(tag || '')) return
      if (event.code === 'Space' && !revealed) {
        event.preventDefault()
        setRevealed(true)
        setFinalAnswer(card?.back || '')
        return
      }
      const qualityKey = /^[0-5]$/.test(event.key)
        ? Number(event.key)
        : (/^(Digit|Numpad)[0-5]$/.test(event.code) ? Number(event.code.slice(-1)) : null)
      if (revealed && qualityKey !== null && !reviewMut.isPending && card) {
        event.preventDefault()
        submitReview(qualityKey)
      }
    }
    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  })

  const submitReview = (qualityOverride?: number) => {
    if (!card) return
    const quality = qualityOverride ?? selectedQuality ?? evaluation?.quality ?? 3
    reviewMut.mutate({
      cardId: card.cardId,
      quality,
      evidence: {
        userAnswer,
        aiGrade: evaluation?.grade,
        aiFeedback: evaluation?.feedback,
        aiSuggestedAnswer: evaluation?.suggestedAnswer,
        aiRecommendedReviewDays: evaluation?.recommendedReviewDays,
      },
    })
  }

  const progress = `${idx + 1} / ${cards.length}`
  const elapsedMs = Math.max(1, Date.now() - startTime)
  const averageMs = idx > 0 ? elapsedMs / idx : 45_000
  const remainingMinutes = Math.max(1, Math.ceil((cards.length - idx) * averageMs / 60_000))

  if (isLoading) return <PageSpinner />

  if (done || cards.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[60vh] gap-4">
        <CheckCircle className="w-16 h-16 text-green-500" />
        <h2 className="text-2xl font-bold text-gray-900 dark:text-white">{t('review.finished')}</h2>
        <p className="text-gray-500">{cards.length} 张卡片已完成</p>
        <div className="flex gap-3">
          <Button variant="secondary" onClick={() => navigate('/decks')}><ChevronLeft className="w-4 h-4" />{t('common.back')}</Button>
          <Button onClick={() => { setIdx(0); setDone(false); resetAnswerState() }}>再复习一遍</Button>
        </div>
      </div>
    )
  }

  return (
    <div className="max-w-3xl mx-auto px-4 py-8 space-y-5">
      <div className="flex items-center justify-between gap-3">
        <Button variant="ghost" size="sm" onClick={() => navigate('/decks')}>
          <ChevronLeft className="w-4 h-4" />{t('common.back')}
        </Button>
        <div className="text-center">
          <span className="text-sm text-gray-500">{progress}</span>
          <p className="text-xs text-gray-400">约 {remainingMinutes} 分钟 · 连击 {streak}</p>
        </div>
        <Button variant="ghost" size="sm" onClick={() => card && undoMut.mutate(card.cardId)} disabled={idx === 0 || undoMut.isPending}>
          <RotateCcw className="w-4 h-4" />{t('review.undo')}
        </Button>
      </div>

      <div className="w-full h-1.5 bg-gray-200 dark:bg-gray-700 rounded-full">
        <div className="h-full bg-primary-500 rounded-full transition-all" style={{ width: `${((idx + 1) / cards.length) * 100}%` }} />
      </div>

      <Card>
        <CardBody className="space-y-5">
          <div className="text-center min-h-[140px] flex flex-col items-center justify-center">
            <div className="mb-4 flex flex-wrap items-center justify-center gap-2">
              <Badge variant={firstLearning ? 'success' : 'primary'}>
                {firstLearning ? '首次学习' : '间隔复习'}
              </Badge>
              <span className="text-xs text-gray-400">{card.deckName}</span>
              <span className="text-xs text-gray-400">{STAGE_LABEL[card.learningStage]} · {DIFFICULTY_LABEL[card.contentDifficulty]}</span>
            </div>
            <h1 className="text-xl md:text-2xl font-semibold text-gray-900 dark:text-white whitespace-pre-wrap leading-relaxed">
              {card.front}
            </h1>
            {card.tags && <p className="mt-3 text-xs text-gray-400">{card.tags}</p>}
          </div>

          {firstLearning && (
            <div className="flex gap-3 border-l-2 border-emerald-600 bg-emerald-50 px-4 py-3 text-left dark:bg-emerald-950/30">
              <GraduationCap className="mt-0.5 h-5 w-5 shrink-0 text-emerald-700 dark:text-emerald-400" />
              <div>
                <p className="text-sm font-medium text-emerald-900 dark:text-emerald-200">先建立理解，不要求第一次就完整背出</p>
                <p className="mt-1 text-xs leading-5 text-emerald-800 dark:text-emerald-300">可以先写下你的直觉，也可以留空让 AI 从核心心智模型开始讲解。</p>
              </div>
            </div>
          )}

          <div className="space-y-2">
            <div className="flex items-center justify-between gap-2">
              <label className="text-sm font-medium text-gray-700 dark:text-gray-300">
                {firstLearning ? '你目前的理解（可留空）' : t('review.yourAnswer')}
              </label>
              <Button type="button" variant="ghost" size="icon" disabled title="语音输入待接入">
                <Mic className="w-4 h-4" />
              </Button>
            </div>
            <textarea
              value={userAnswer}
              onChange={e => setUserAnswer(e.target.value)}
              rows={6}
              className="w-full px-3 py-2 text-sm rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-800 text-gray-900 dark:text-gray-100 focus:outline-none focus:ring-2 focus:ring-primary-500 resize-y"
              placeholder={firstLearning ? '先写下关键词、因果关系或你不确定的地方。' : t('review.answerPlaceholder')}
            />
          </div>

          <div className="flex flex-col sm:flex-row gap-2">
            <Button className="flex-1" onClick={() => evaluateMut.mutate()} loading={evaluateMut.isPending} disabled={!firstLearning && !userAnswer.trim()}>
              <Brain className="w-4 h-4" />
              {firstLearning && !userAnswer.trim() ? '从讲解开始' : t('review.submitAiReview')}
            </Button>
            <Button variant="secondary" className="flex-1" onClick={() => { setRevealed(true); setFinalAnswer(card.back) }}>
              <Eye className="w-4 h-4" />{t('review.revealAnswer')}
            </Button>
          </div>

          {evaluateMut.isError && (
            <p className="text-sm text-red-500">AI 评审失败，请直接翻面自评或稍后重试。</p>
          )}
        </CardBody>
      </Card>

      {revealed && (
        <div className="space-y-5">
          {evaluation && (
            <Card>
              <CardBody className="space-y-4">
                <div className="flex items-center justify-between gap-3 flex-wrap">
                  <div className="flex items-center gap-2">
                    <Badge variant={gradeVariant(evaluation.grade)}>
                      {firstLearning ? '学习反馈' : `Grade ${evaluation.grade}`}
                    </Badge>
                    <span className="text-sm text-gray-500">Score {evaluation.score}</span>
                  </div>
                  <div className="flex items-center gap-3 text-sm text-gray-500">
                    <span>建议评分：{evaluation.quality}</span>
                    {evaluation.recommendedReviewDays && (
                      <span className="inline-flex items-center gap-1">
                        <CalendarClock className="h-4 w-4" />
                        {evaluation.recommendedReviewDays} 天后再看
                      </span>
                    )}
                  </div>
                </div>
                <p className="text-sm text-gray-700 dark:text-gray-300 whitespace-pre-wrap leading-relaxed">
                  {evaluation.feedback}
                </p>
                {evaluation.missingPoints.length > 0 && (
                  <div className="rounded-lg bg-yellow-50 dark:bg-yellow-900/20 p-3">
                    <p className="text-sm font-medium text-yellow-800 dark:text-yellow-200 mb-2">缺失要点</p>
                    <ul className="space-y-1 text-sm text-yellow-800 dark:text-yellow-100">
                      {evaluation.missingPoints.map((point, i) => <li key={i}>· {point}</li>)}
                    </ul>
                  </div>
                )}
                {evaluation.coachingTip && (
                  <div className="flex gap-2 border-l-2 border-emerald-600 bg-emerald-50 px-3 py-2 text-sm text-emerald-900 dark:bg-emerald-950/30 dark:text-emerald-200">
                    <Lightbulb className="mt-0.5 h-4 w-4 shrink-0" />
                    <span>{evaluation.coachingTip}</span>
                  </div>
                )}
              </CardBody>
            </Card>
          )}

          <Card>
            <CardBody className="space-y-4">
              <div>
                <p className="text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">{t('review.referenceAnswer')}</p>
                <p className="text-sm rounded-lg bg-background-light dark:bg-gray-800 p-3 text-gray-800 dark:text-gray-200 whitespace-pre-wrap leading-relaxed">
                  {card.back}
                </p>
              </div>

              <div>
                <label className="text-sm font-medium text-gray-700 dark:text-gray-300">{t('review.finalAnswer')}</label>
                <textarea
                  value={finalAnswer}
                  onChange={e => setFinalAnswer(e.target.value)}
                  rows={5}
                  className="mt-1 w-full px-3 py-2 text-sm rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-800 text-gray-900 dark:text-gray-100 focus:outline-none focus:ring-2 focus:ring-primary-500 resize-y"
                />
                <div className="flex justify-end mt-2">
                  <Button
                    variant="secondary"
                    size="sm"
                    onClick={() => replaceAnswerMut.mutate()}
                    loading={replaceAnswerMut.isPending}
                    disabled={!finalAnswer.trim()}
                  >
                    <Save className="w-4 h-4" />{t('review.replaceAnswer')}
                  </Button>
                </div>
              </div>
            </CardBody>
          </Card>

          <Card>
            <CardBody className="space-y-4">
              <div className="space-y-2">
                {chat.map((msg, i) => (
                  <div key={i} className={cn(
                    'rounded-lg px-3 py-2 text-sm whitespace-pre-wrap',
                    msg.role === 'user'
                      ? 'bg-primary-50 dark:bg-primary-900/20 text-gray-800 dark:text-gray-100'
                      : 'bg-gray-50 dark:bg-gray-800 text-gray-700 dark:text-gray-200',
                  )}>
                    {msg.content}
                  </div>
                ))}
              </div>
              <div className="flex gap-2">
                <input
                  value={followUp}
                  onChange={e => setFollowUp(e.target.value)}
                  className="flex-1 px-3 py-2 text-sm rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-800 text-gray-900 dark:text-gray-100 focus:outline-none focus:ring-2 focus:ring-primary-500"
                  placeholder={t('review.followUpPlaceholder')}
                />
                <Button
                  variant="secondary"
                  onClick={() => followUp.trim() && explainMut.mutate(followUp.trim())}
                  loading={explainMut.isPending}
                  disabled={!followUp.trim()}
                >
                  <MessageCircle className="w-4 h-4" />{t('review.ask')}
                </Button>
              </div>
            </CardBody>
          </Card>

          <div className="space-y-3">
            <div className="grid grid-cols-2 sm:grid-cols-6 gap-2">
              {QUALITY_BTNS.map(({ q, label, color }) => (
                <button
                  key={q}
                  onClick={() => setSelectedQuality(q)}
                  disabled={reviewMut.isPending}
                  className={cn(
                    'h-11 rounded-lg text-sm font-medium transition-all active:scale-95 ring-offset-2',
                    color,
                    selectedQuality === q && 'ring-2 ring-gray-900 dark:ring-white',
                  )}
                >
                  {label}
                </button>
              ))}
            </div>
            <Button className="w-full" onClick={() => submitReview()} loading={reviewMut.isPending}>
              <Send className="w-4 h-4" />{t('review.confirmMastery')}
            </Button>
          </div>
        </div>
      )}
    </div>
  )
}
