import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { practiceApi } from '@/lib/api'
import { PageSpinner } from '@/components/ui/Spinner'
import Card, { CardBody } from '@/components/ui/Card'
import Button from '@/components/ui/Button'
import Badge from '@/components/ui/Badge'
import Modal from '@/components/ui/Modal'
import type { ProblemNote } from '@/types'
import { difficultyBg, formatDate } from '@/lib/utils'
import { Star, Brain, BookOpen, AlertTriangle } from 'lucide-react'
import { cn } from '@/lib/utils'

type FilterType = '' | 'WRONG' | 'TODO' | 'STAR'

const QUALITY_BTNS = [
  { q: 1, label: '完全不会' },
  { q: 2, label: '模糊' },
  { q: 3, label: '勉强记住' },
  { q: 4, label: '记住了' },
  { q: 5, label: '轻松记住' },
]

export default function NotebookPage() {
  const { t } = useTranslation()
  const qc = useQueryClient()
  const [filter, setFilter] = useState<FilterType>('')
  const [aiOpen, setAiOpen] = useState(false)
  const [aiText, setAiText] = useState('')
  const [aiLoading, setAiLoading] = useState(false)
  const [retryNote, setRetryNote] = useState<ProblemNote | null>(null)

  const { data: notes = [], isLoading } = useQuery<ProblemNote[]>({
    queryKey: ['notebook', filter],
    queryFn: () => practiceApi.notebook(filter || undefined),
  })

  const toggleStarMut = useMutation({
    mutationFn: (id: number) => practiceApi.toggleStar(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['notebook'] }),
  })
  const toggleTodoMut = useMutation({
    mutationFn: (id: number) => practiceApi.toggleTodo(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['notebook'] }),
  })
  const retryMut = useMutation({
    mutationFn: ({ id, quality }: { id: number; quality: number }) => practiceApi.retry(id, quality),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['notebook'] }); setRetryNote(null) },
  })

  const handleAiAnalysis = async () => {
    setAiLoading(true)
    setAiOpen(true)
    try {
      const res = await practiceApi.aiAnalysis()
      setAiText(res.aiAnalysis || '暂无分析结果')
    } catch {
      setAiText('AI 分析失败，请稍后重试')
    } finally {
      setAiLoading(false)
    }
  }

  if (isLoading) return <PageSpinner />

  const FILTERS: { key: FilterType; label: string; icon: React.ElementType }[] = [
    { key: '', label: t('notebook.all'), icon: BookOpen },
    { key: 'WRONG', label: t('notebook.wrong'), icon: AlertTriangle },
    { key: 'TODO', label: t('notebook.todo'), icon: BookOpen },
    { key: 'STAR', label: t('notebook.starred'), icon: Star },
  ]

  return (
    <div className="p-6 space-y-5">
      <div className="flex items-center justify-between flex-wrap gap-3">
        <h1 className="text-2xl font-bold text-gray-900 dark:text-white">{t('notebook.title')}</h1>
        <Button variant="secondary" onClick={handleAiAnalysis} loading={aiLoading}>
          <Brain className="w-4 h-4" />{t('notebook.aiAnalysis')}
        </Button>
      </div>

      {/* Filter tabs */}
      <div className="flex gap-2 border-b border-gray-200 dark:border-gray-700">
        {FILTERS.map(({ key, label, icon: Icon }) => (
          <button key={key} onClick={() => setFilter(key)}
            className={`flex items-center gap-1.5 px-4 py-2 text-sm font-medium border-b-2 transition-colors ${filter === key ? 'border-primary-600 text-primary-600' : 'border-transparent text-gray-500 hover:text-gray-700 dark:hover:text-gray-300'}`}>
            <Icon className="w-4 h-4" />{label}
          </button>
        ))}
      </div>

      {/* Notes list */}
      {notes.length === 0 ? (
        <div className="text-center py-16 text-gray-400">{t('notebook.noNotes')}</div>
      ) : (
        <div className="space-y-3">
          {notes.map(note => (
            <Card key={note.id}>
              <CardBody className="space-y-2">
                <div className="flex items-start justify-between gap-3">
                  <div className="flex items-center gap-2 flex-wrap">
                    <span className="font-medium text-sm text-gray-900 dark:text-white">{note.problemTitle}</span>
                    {note.problemDifficulty && (
                      <Badge className={difficultyBg(note.problemDifficulty)}>{note.problemDifficulty}</Badge>
                    )}
                    {note.bookmarkType && <Badge variant="warning">{note.bookmarkType}</Badge>}
                  </div>
                  <div className="flex items-center gap-1 shrink-0">
                    <button onClick={() => toggleStarMut.mutate(note.problemId)}
                      className={cn('p-1 rounded transition-colors', note.starred ? 'text-yellow-500' : 'text-gray-300 hover:text-yellow-400')}>
                      <Star className="w-4 h-4" fill={note.starred ? 'currentColor' : 'none'} />
                    </button>
                    <button onClick={() => toggleTodoMut.mutate(note.problemId)}
                      className="p-1 rounded text-gray-400 hover:text-primary-500">
                      <BookOpen className="w-4 h-4" />
                    </button>
                  </div>
                </div>

                {note.note && (
                  <p className="text-xs text-gray-600 dark:text-gray-400 bg-background-light dark:bg-gray-800 rounded-lg p-2">
                    {note.note}
                  </p>
                )}
                {note.errorReason && (
                  <p className="text-xs text-red-600 dark:text-red-400">
                    <span className="font-medium">错误原因：</span>{note.errorReason}
                  </p>
                )}

                <div className="flex items-center justify-between text-xs text-gray-400">
                  <span>复习 {note.retryCount} 次</span>
                  {note.nextRetryDate && <span>下次复习：{formatDate(note.nextRetryDate)}</span>}
                  <Button size="sm" variant="ghost" onClick={() => setRetryNote(note)}>
                    {t('notebook.retry')}
                  </Button>
                </div>
              </CardBody>
            </Card>
          ))}
        </div>
      )}

      {/* AI Analysis Modal */}
      <Modal open={aiOpen} onClose={() => setAiOpen(false)} title={t('notebook.aiAnalysis')} size="lg">
        {aiLoading ? (
          <div className="text-center py-8 text-gray-400">{t('notebook.analyzing')}</div>
        ) : (
          <p className="text-sm text-gray-700 dark:text-gray-300 whitespace-pre-wrap leading-relaxed">{aiText}</p>
        )}
      </Modal>

      {/* Retry Modal */}
      <Modal open={!!retryNote} onClose={() => setRetryNote(null)} title="复习评分" size="sm">
        <div className="space-y-3">
          <p className="text-sm text-gray-700 dark:text-gray-300">《{retryNote?.problemTitle}》你记得多少？</p>
          <div className="grid grid-cols-5 gap-2">
            {QUALITY_BTNS.map(({ q, label }) => (
              <button key={q}
                onClick={() => retryNote && retryMut.mutate({ id: retryNote.problemId, quality: q })}
                disabled={retryMut.isPending}
                className="py-2 rounded-lg text-xs font-medium bg-gray-100 dark:bg-gray-800 hover:bg-primary-100 dark:hover:bg-primary-900/20 hover:text-primary-600 transition-colors">
                {label}
              </button>
            ))}
          </div>
        </div>
      </Modal>
    </div>
  )
}
