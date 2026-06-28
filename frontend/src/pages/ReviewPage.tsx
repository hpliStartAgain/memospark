import { useState, useCallback, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { reviewApi, aiApi } from '@/lib/api'
import { PageSpinner } from '@/components/ui/Spinner'
import Button from '@/components/ui/Button'
import Card, { CardBody } from '@/components/ui/Card'
import Modal from '@/components/ui/Modal'
import type { ReviewCard } from '@/types'
import { RotateCcw, ChevronLeft, Brain, CheckCircle } from 'lucide-react'
import { cn } from '@/lib/utils'

const QUALITY_BTNS = [
  { q: 1, label: '再来', color: 'bg-red-500 hover:bg-red-600 text-white' },
  { q: 2, label: '困难', color: 'bg-orange-500 hover:bg-orange-600 text-white' },
  { q: 3, label: '模糊', color: 'bg-yellow-500 hover:bg-yellow-600 text-white' },
  { q: 4, label: '不错', color: 'bg-primary-500 hover:bg-primary-600 text-white' },
  { q: 5, label: '简单', color: 'bg-green-500 hover:bg-green-600 text-white' },
]

export default function ReviewPage() {
  const { t } = useTranslation()
  const { deckId } = useParams<{ deckId: string }>()
  const navigate = useNavigate()
  const qc = useQueryClient()

  const [idx, setIdx] = useState(0)
  const [flipped, setFlipped] = useState(false)
  const [done, setDone] = useState(false)
  const [startTime] = useState(() => Date.now())
  const [cardStartedAt, setCardStartedAt] = useState(() => Date.now())
  const [streak, setStreak] = useState(0)
  const [aiGradeOpen, setAiGradeOpen] = useState(false)
  const [userAnswer, setUserAnswer] = useState('')
  const [gradeResult, setGradeResult] = useState<string>('')
  const [grading, setGrading] = useState(false)

  const { data: cards = [], isLoading } = useQuery<ReviewCard[]>({
    queryKey: ['review', deckId ?? 'today'],
    queryFn: () => deckId ? reviewApi.byDeck(+deckId) : reviewApi.today(),
  })

  const reviewMut = useMutation({
    mutationFn: ({ cardId, quality }: { cardId: number; quality: number }) =>
      reviewApi.submit(cardId, quality, Date.now() - cardStartedAt),
    onSuccess: (_data, variables) => {
      qc.invalidateQueries({ queryKey: ['decks'] })
      setStreak(current => variables.quality >= 4 ? current + 1 : 0)
      const next = idx + 1
      if (next >= cards.length) { setDone(true) }
      else {
        setIdx(next)
        setFlipped(false)
        setCardStartedAt(Date.now())
      }
    },
  })

  const undoMut = useMutation({
    mutationFn: (cardId: number) => reviewApi.undo(cardId),
    onSuccess: () => {
      if (idx > 0) {
        setIdx(i => i - 1)
        setFlipped(false)
        setDone(false)
        setStreak(0)
        setCardStartedAt(Date.now())
      }
    },
  })

  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      const tag = (event.target as HTMLElement | null)?.tagName
      if (aiGradeOpen || ['INPUT', 'TEXTAREA', 'SELECT'].includes(tag || '')) return
      if (event.code === 'Space' && !flipped) {
        event.preventDefault()
        setFlipped(true)
        return
      }
      const qualityKey = /^[1-5]$/.test(event.key)
        ? Number(event.key)
        : (/^(Digit|Numpad)[1-5]$/.test(event.code) ? Number(event.code.slice(-1)) : null)
      if (flipped && qualityKey && !reviewMut.isPending && cards[idx]) {
        event.preventDefault()
        reviewMut.mutate({ cardId: cards[idx].cardId, quality: qualityKey })
      }
    }
    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [aiGradeOpen, cards, flipped, idx, reviewMut.isPending])

  const handleGrade = useCallback(async (card: ReviewCard) => {
    setGrading(true)
    try {
      const res = await aiApi.grade(card.front, card.back, userAnswer)
      setGradeResult(res.feedback || res.result || JSON.stringify(res))
    } catch {
      setGradeResult('AI 评分失败，请重试')
    } finally {
      setGrading(false)
    }
  }, [userAnswer])

  if (isLoading) return <PageSpinner />

  if (done || cards.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[60vh] gap-4">
        <CheckCircle className="w-16 h-16 text-green-500" />
        <h2 className="text-2xl font-bold text-gray-900 dark:text-white">{t('review.finished')}</h2>
        <p className="text-gray-500">{cards.length} 张卡片已完成</p>
        <div className="flex gap-3">
          <Button variant="secondary" onClick={() => navigate('/decks')}><ChevronLeft className="w-4 h-4" />{t('common.back')}</Button>
          <Button onClick={() => { setIdx(0); setFlipped(false); setDone(false) }}>再复习一遍</Button>
        </div>
      </div>
    )
  }

  const card = cards[idx]
  const progress = `${idx + 1} / ${cards.length}`
  const elapsedMs = Math.max(1, Date.now() - startTime)
  const averageMs = idx > 0 ? elapsedMs / idx : 30_000
  const remainingMinutes = Math.max(1, Math.ceil((cards.length - idx) * averageMs / 60_000))

  return (
    <div className="max-w-2xl mx-auto px-4 py-8 space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <Button variant="ghost" size="sm" onClick={() => navigate('/decks')}>
          <ChevronLeft className="w-4 h-4" />{t('common.back')}
        </Button>
        <div className="text-center">
          <span className="text-sm text-gray-500">{progress}</span>
          <p className="text-xs text-gray-400">约 {remainingMinutes} 分钟 · 连击 {streak}</p>
        </div>
        <Button variant="ghost" size="sm" onClick={() => undoMut.mutate(card.cardId)} disabled={idx === 0}>
          <RotateCcw className="w-4 h-4" />{t('review.undo')}
        </Button>
      </div>

      {/* Progress bar */}
      <div className="w-full h-1.5 bg-gray-200 dark:bg-gray-700 rounded-full">
        <div className="h-full bg-primary-500 rounded-full transition-all" style={{ width: `${((idx + 1) / cards.length) * 100}%` }} />
      </div>

      {/* Flashcard */}
      <div className="card-flip" onClick={() => !flipped && setFlipped(true)}>
        <div className={cn('card-flip-inner relative min-h-[260px]', flipped && 'flipped')}>
          {/* Front */}
          <Card className="card-face min-h-[260px] cursor-pointer select-none">
            <CardBody className="flex flex-col items-center justify-center min-h-[260px] text-center">
              <p className="text-sm text-gray-400 mb-3">{t('review.flip')} →</p>
              <p className="text-xl font-medium text-gray-900 dark:text-white whitespace-pre-wrap">{card.front}</p>
              {card.tags && <p className="mt-3 text-xs text-gray-400">{card.tags}</p>}
            </CardBody>
          </Card>
          {/* Back */}
          <Card className="card-face card-back absolute inset-0 min-h-[260px]">
            <CardBody className="flex flex-col items-center justify-center min-h-[260px] text-center">
              <p className="text-sm text-gray-400 mb-3">答案</p>
              <p className="text-lg text-gray-900 dark:text-white whitespace-pre-wrap">{card.back}</p>
            </CardBody>
          </Card>
        </div>
      </div>

      {/* Rating buttons */}
      {flipped && (
        <div className="space-y-3">
          <div className="grid grid-cols-5 gap-2">
            {QUALITY_BTNS.map(({ q, label, color }) => (
              <button
                key={q}
                onClick={() => reviewMut.mutate({ cardId: card.cardId, quality: q })}
                disabled={reviewMut.isPending}
                className={cn('py-3 rounded-xl text-sm font-medium transition-transform active:scale-95', color)}
              >
                {label}
              </button>
            ))}
          </div>
          <Button variant="ghost" size="sm" className="w-full" onClick={() => setAiGradeOpen(true)}>
            <Brain className="w-4 h-4" />{t('review.aiGrade')}
          </Button>
        </div>
      )}

      {/* AI Grade Modal */}
      <Modal open={aiGradeOpen} onClose={() => { setAiGradeOpen(false); setUserAnswer(''); setGradeResult('') }}
        title={t('review.aiGrade')}>
        <div className="space-y-4">
          <div>
            <p className="text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">题目</p>
            <p className="text-sm bg-background-light dark:bg-gray-800 rounded-lg p-3">{card.front}</p>
          </div>
          <div>
            <label className="text-sm font-medium text-gray-700 dark:text-gray-300">{t('review.yourAnswer')}</label>
            <textarea
              value={userAnswer}
              onChange={e => setUserAnswer(e.target.value)}
              rows={4}
              className="mt-1 w-full px-3 py-2 text-sm rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-800 focus:outline-none focus:ring-2 focus:ring-primary-500"
              placeholder="在这里输入你的回答..."
            />
          </div>
          {gradeResult && (
            <div className="bg-primary-50 dark:bg-primary-900/20 rounded-lg p-3 text-sm text-gray-700 dark:text-gray-300 whitespace-pre-wrap">
              {gradeResult}
            </div>
          )}
          <div className="flex gap-3 justify-end">
            <Button variant="secondary" onClick={() => { setAiGradeOpen(false); setUserAnswer(''); setGradeResult('') }}>
              {t('common.close')}
            </Button>
            <Button onClick={() => handleGrade(card)} loading={grading}>
              {grading ? t('review.grading') : '提交评分'}
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  )
}
