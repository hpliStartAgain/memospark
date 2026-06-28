import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useNavigate, useParams } from 'react-router-dom'
import {
  ArrowLeft, BookOpen, CheckSquare, Pencil, Plus, Search,
  Sparkles, Trash2, X,
} from 'lucide-react'
import { cardApi, deckApi } from '@/lib/api'
import { useAppStore } from '@/store/appStore'
import Badge from '@/components/ui/Badge'
import Button from '@/components/ui/Button'
import Input from '@/components/ui/Input'
import Modal from '@/components/ui/Modal'
import { PageSpinner } from '@/components/ui/Spinner'
import type { CardDifficulty, Deck, LearningStage, ReviewCard } from '@/types'

const stageLabels: Record<LearningStage, string> = {
  FOUNDATION: '入门',
  ADVANCED: '进阶',
  PRACTICE: '实战',
}

const difficultyLabels: Record<CardDifficulty, string> = {
  EASY: '简单',
  MEDIUM: '中等',
  HARD: '困难',
}

const emptyForm = () => ({
  front: '',
  back: '',
  tags: '',
  contentDifficulty: 'MEDIUM' as CardDifficulty,
  learningStage: 'FOUNDATION' as LearningStage,
  stageOrder: '1',
})

export default function DeckDetailPage() {
  const { id } = useParams<{ id: string }>()
  const deckId = Number(id)
  const navigate = useNavigate()
  const qc = useQueryClient()
  const { lang } = useAppStore()
  const [query, setQuery] = useState('')
  const [stage, setStage] = useState<'ALL' | LearningStage>('ALL')
  const [difficulty, setDifficulty] = useState<'ALL' | CardDifficulty>('ALL')
  const [editing, setEditing] = useState<ReviewCard | null>(null)
  const [showEditor, setShowEditor] = useState(false)
  const [form, setForm] = useState(emptyForm())
  const [deleteTarget, setDeleteTarget] = useState<ReviewCard | null>(null)
  const [selected, setSelected] = useState<number[]>([])
  const [governanceMessage, setGovernanceMessage] = useState('')

  const { data: deck, isLoading: deckLoading } = useQuery<Deck>({
    queryKey: ['deck', deckId],
    queryFn: () => deckApi.get(deckId),
    enabled: Number.isFinite(deckId),
  })
  const { data: cards = [], isLoading: cardsLoading } = useQuery<ReviewCard[]>({
    queryKey: ['deck', deckId, 'cards'],
    queryFn: () => cardApi.list(deckId),
    enabled: Number.isFinite(deckId),
  })

  const filteredCards = useMemo(() => {
    const needle = query.trim().toLowerCase()
    return cards.filter(card => {
      if (stage !== 'ALL' && card.learningStage !== stage) return false
      if (difficulty !== 'ALL' && card.contentDifficulty !== difficulty) return false
      if (!needle) return true
      return `${card.front} ${card.back} ${card.tags || ''}`.toLowerCase().includes(needle)
    })
  }, [cards, difficulty, query, stage])

  const saveCard = useMutation({
    mutationFn: () => {
      const body = {
        front: form.front,
        back: form.back,
        tags: form.tags || null,
        contentDifficulty: form.contentDifficulty,
        learningStage: form.learningStage,
        stageOrder: Number(form.stageOrder),
      }
      return editing
        ? cardApi.update(deckId, editing.cardId, body)
        : cardApi.create(deckId, body)
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['deck', deckId] })
      qc.invalidateQueries({ queryKey: ['deck', deckId, 'cards'] })
      qc.invalidateQueries({ queryKey: ['decks'] })
      closeEditor()
    },
  })

  const removeCard = useMutation({
    mutationFn: (cardId: number) => cardApi.remove(deckId, cardId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['deck', deckId] })
      qc.invalidateQueries({ queryKey: ['deck', deckId, 'cards'] })
      qc.invalidateQueries({ queryKey: ['decks'] })
      setDeleteTarget(null)
    },
  })

  const batchDelete = useMutation({
    mutationFn: () => cardApi.batchDelete(deckId, selected),
    onSuccess: () => {
      setSelected([])
      qc.invalidateQueries({ queryKey: ['deck', deckId] })
      qc.invalidateQueries({ queryKey: ['deck', deckId, 'cards'] })
      qc.invalidateQueries({ queryKey: ['decks'] })
    },
  })

  const govern = useMutation({
    mutationFn: () => cardApi.govern(deckId, lang),
    onSuccess: result => {
      qc.setQueryData(['deck', deckId, 'cards'], result.cards)
      setGovernanceMessage(result.summary)
    },
  })

  const closeEditor = () => {
    setShowEditor(false)
    setEditing(null)
    setForm(emptyForm())
  }

  const openCreate = () => {
    setEditing(null)
    setForm({ ...emptyForm(), stageOrder: String(cards.length + 1) })
    setShowEditor(true)
  }

  const openEdit = (card: ReviewCard) => {
    setEditing(card)
    setForm({
      front: card.front,
      back: card.back,
      tags: card.tags || '',
      contentDifficulty: card.contentDifficulty,
      learningStage: card.learningStage,
      stageOrder: String(card.stageOrder),
    })
    setShowEditor(true)
  }

  const toggleSelected = (cardId: number) => {
    setSelected(current => current.includes(cardId)
      ? current.filter(id2 => id2 !== cardId)
      : [...current, cardId])
  }

  if (deckLoading || cardsLoading) return <PageSpinner />

  return (
    <div className="mx-auto max-w-7xl p-5 md:p-8">
      <div className="border-b border-gray-200 pb-6 dark:border-gray-800">
        <Button variant="ghost" size="sm" onClick={() => navigate('/decks')}>
          <ArrowLeft className="h-4 w-4" />返回牌组
        </Button>
        <div className="mt-4 flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
          <div>
            <div className="flex flex-wrap items-center gap-3">
              <h1 className="text-3xl font-semibold text-gray-950 dark:text-white">{deck?.name}</h1>
              <Badge>{cards.length} 张</Badge>
            </div>
            <p className="mt-2 max-w-2xl text-sm text-gray-500">{deck?.description || '管理问题、答案与学习顺序。'}</p>
          </div>
          <div className="flex flex-wrap gap-2">
            <Button variant="secondary" onClick={() => govern.mutate()} loading={govern.isPending}>
              <Sparkles className="h-4 w-4" />AI 治理
            </Button>
            <Button variant="secondary" onClick={() => navigate(`/review/${deckId}`)}>
              <BookOpen className="h-4 w-4" />开始学习
            </Button>
            <Button onClick={openCreate}>
              <Plus className="h-4 w-4" />添加卡片
            </Button>
          </div>
        </div>
      </div>

      {governanceMessage && (
        <div className="mt-5 flex items-center justify-between border-l-2 border-emerald-600 bg-emerald-50 px-4 py-3 text-sm text-emerald-800 dark:bg-emerald-950/30 dark:text-emerald-300">
          <span>{governanceMessage}</span>
          <button title="关闭" onClick={() => setGovernanceMessage('')}><X className="h-4 w-4" /></button>
        </div>
      )}
      {govern.isError && (
        <p className="mt-5 border-l-2 border-red-500 bg-red-50 px-4 py-3 text-sm text-red-700">
          AI 治理失败，请检查 AI 设置后重试。卡片内容没有被修改。
        </p>
      )}

      <div className="mt-6 grid gap-3 md:grid-cols-[1fr_150px_150px_auto]">
        <div className="relative">
          <Search className="absolute left-3 top-3 h-4 w-4 text-gray-400" />
          <input
            value={query}
            onChange={event => setQuery(event.target.value)}
            placeholder="搜索问题、答案或标签"
            className="h-10 w-full rounded-md border border-gray-300 bg-white pl-9 pr-3 text-sm dark:border-gray-700 dark:bg-gray-900"
          />
        </div>
        <select value={stage} onChange={event => setStage(event.target.value as typeof stage)}
          className="h-10 rounded-md border border-gray-300 bg-white px-3 text-sm dark:border-gray-700 dark:bg-gray-900">
          <option value="ALL">全部阶段</option>
          <option value="FOUNDATION">入门</option>
          <option value="ADVANCED">进阶</option>
          <option value="PRACTICE">实战</option>
        </select>
        <select value={difficulty} onChange={event => setDifficulty(event.target.value as typeof difficulty)}
          className="h-10 rounded-md border border-gray-300 bg-white px-3 text-sm dark:border-gray-700 dark:bg-gray-900">
          <option value="ALL">全部难度</option>
          <option value="EASY">简单</option>
          <option value="MEDIUM">中等</option>
          <option value="HARD">困难</option>
        </select>
        {selected.length > 0 && (
          <Button variant="danger" onClick={() => batchDelete.mutate()} loading={batchDelete.isPending}>
            <Trash2 className="h-4 w-4" />删除 {selected.length} 张
          </Button>
        )}
      </div>

      <div className="mt-5 overflow-hidden border border-gray-200 rounded-lg dark:border-gray-800">
        <div className="hidden grid-cols-[44px_80px_90px_1fr_140px_88px] gap-3 border-b border-gray-200 bg-gray-50 px-4 py-3 text-xs font-medium text-gray-500 md:grid dark:border-gray-800 dark:bg-gray-900">
          <span />
          <span>顺序</span>
          <span>难度</span>
          <span>问题</span>
          <span>学习状态</span>
          <span>操作</span>
        </div>
        {filteredCards.length === 0 ? (
          <div className="py-16 text-center text-sm text-gray-400">没有符合条件的卡片</div>
        ) : (
          <div className="divide-y divide-gray-200 dark:divide-gray-800">
            {filteredCards.map(card => (
              <div key={card.cardId} className="grid gap-3 px-4 py-4 md:grid-cols-[44px_80px_90px_1fr_140px_88px] md:items-center">
                <button
                  title="选择卡片"
                  onClick={() => toggleSelected(card.cardId)}
                  className={`flex h-6 w-6 items-center justify-center rounded border ${
                    selected.includes(card.cardId) ? 'border-emerald-600 bg-emerald-600 text-white' : 'border-gray-300 text-transparent'
                  }`}
                >
                  <CheckSquare className="h-4 w-4" />
                </button>
                <span className="font-mono text-xs text-gray-400">#{card.stageOrder}</span>
                <Badge variant={card.contentDifficulty === 'HARD' ? 'danger' : card.contentDifficulty === 'EASY' ? 'success' : 'warning'}>
                  {difficultyLabels[card.contentDifficulty]}
                </Badge>
                <div className="min-w-0">
                  <p className="font-medium text-gray-900 dark:text-white">{card.front}</p>
                  <p className="mt-1 line-clamp-2 text-sm leading-6 text-gray-500">{card.back}</p>
                  {card.tags && <p className="mt-2 text-xs text-gray-400">{card.tags}</p>}
                </div>
                <div>
                  <p className="text-sm font-medium">{stageLabels[card.learningStage]}</p>
                  <p className="mt-1 text-xs text-gray-400">{card.isNew ? '未学习' : `已复习 ${card.repetitions} 次`}</p>
                </div>
                <div className="flex gap-1">
                  <Button title="编辑卡片" variant="ghost" size="icon" onClick={() => openEdit(card)}>
                    <Pencil className="h-4 w-4" />
                  </Button>
                  <Button title="删除卡片" variant="ghost" size="icon" onClick={() => setDeleteTarget(card)}>
                    <Trash2 className="h-4 w-4 text-red-500" />
                  </Button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      <Modal open={showEditor} onClose={closeEditor} title={editing ? '编辑卡片' : '添加卡片'} size="xl">
        <form onSubmit={event => { event.preventDefault(); saveCard.mutate() }} className="space-y-4">
          <div>
            <label className="text-sm font-medium text-gray-700 dark:text-gray-300">问题</label>
            <textarea
              rows={4}
              value={form.front}
              onChange={event => setForm(current => ({ ...current, front: event.target.value }))}
              className="mt-1 w-full rounded-md border border-gray-300 bg-white px-3 py-2 text-sm dark:border-gray-700 dark:bg-gray-900"
              required
              autoFocus
            />
          </div>
          <div>
            <label className="text-sm font-medium text-gray-700 dark:text-gray-300">答案</label>
            <textarea
              rows={10}
              value={form.back}
              onChange={event => setForm(current => ({ ...current, back: event.target.value }))}
              className="mt-1 w-full rounded-md border border-gray-300 bg-white px-3 py-2 text-sm leading-6 dark:border-gray-700 dark:bg-gray-900"
              required
            />
          </div>
          <Input label="标签" value={form.tags} onChange={event => setForm(current => ({ ...current, tags: event.target.value }))} />
          <div className="grid gap-3 sm:grid-cols-3">
            <label className="text-sm font-medium text-gray-700 dark:text-gray-300">
              阶段
              <select value={form.learningStage} onChange={event => setForm(current => ({ ...current, learningStage: event.target.value as LearningStage }))}
                className="mt-1 h-10 w-full rounded-md border border-gray-300 bg-white px-3 dark:border-gray-700 dark:bg-gray-900">
                <option value="FOUNDATION">入门</option>
                <option value="ADVANCED">进阶</option>
                <option value="PRACTICE">实战</option>
              </select>
            </label>
            <label className="text-sm font-medium text-gray-700 dark:text-gray-300">
              难度
              <select value={form.contentDifficulty} onChange={event => setForm(current => ({ ...current, contentDifficulty: event.target.value as CardDifficulty }))}
                className="mt-1 h-10 w-full rounded-md border border-gray-300 bg-white px-3 dark:border-gray-700 dark:bg-gray-900">
                <option value="EASY">简单</option>
                <option value="MEDIUM">中等</option>
                <option value="HARD">困难</option>
              </select>
            </label>
            <Input
              label="阶段内顺序"
              type="number"
              min="1"
              value={form.stageOrder}
              onChange={event => setForm(current => ({ ...current, stageOrder: event.target.value }))}
            />
          </div>
          {saveCard.isError && <p className="text-sm text-red-500">保存失败，请检查内容后重试。</p>}
          <div className="flex justify-end gap-2">
            <Button type="button" variant="secondary" onClick={closeEditor}>取消</Button>
            <Button type="submit" loading={saveCard.isPending}>保存卡片</Button>
          </div>
        </form>
      </Modal>

      <Modal open={!!deleteTarget} onClose={() => setDeleteTarget(null)} title="删除卡片" size="sm">
        <p className="text-sm leading-6 text-gray-600 dark:text-gray-300">删除后该卡片的复习记录也会一并移除。确定删除“{deleteTarget?.front}”吗？</p>
        <div className="mt-5 flex justify-end gap-2">
          <Button variant="secondary" onClick={() => setDeleteTarget(null)}>取消</Button>
          <Button variant="danger" loading={removeCard.isPending} onClick={() => deleteTarget && removeCard.mutate(deleteTarget.cardId)}>
            删除
          </Button>
        </div>
      </Modal>
    </div>
  )
}

