import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { cardApi, deckApi } from '@/lib/api'
import { PageSpinner } from '@/components/ui/Spinner'
import Card, { CardBody } from '@/components/ui/Card'
import Button from '@/components/ui/Button'
import Modal from '@/components/ui/Modal'
import Input from '@/components/ui/Input'
import Badge from '@/components/ui/Badge'
import type { Deck } from '@/types'
import { Plus, BookOpen, Trash2, Play, Layers, Copy, Sparkles, ListChecks, Pencil } from 'lucide-react'
import { useAppStore } from '@/store/appStore'

interface DeckFormState { name: string; description: string; dailyNewCardLimit: string; dailyReviewLimit: string }
const emptyForm = (): DeckFormState => ({ name: '', description: '', dailyNewCardLimit: '20', dailyReviewLimit: '100' })

export default function DecksPage() {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const { user, lang } = useAppStore()
  const qc = useQueryClient()
  const [tab, setTab] = useState<'mine' | 'pool'>('mine')
  const [showCreate, setShowCreate] = useState(false)
  const [editDeck, setEditDeck] = useState<Deck | null>(null)
  const [form, setForm] = useState<DeckFormState>(emptyForm())
  const [deleteTarget, setDeleteTarget] = useState<Deck | null>(null)
  const [fromTextDeck, setFromTextDeck] = useState<Deck | null>(null)
  const [fromTextForm, setFromTextForm] = useState({ text: '', count: '8' })

  const { data: decks = [], isLoading } = useQuery<Deck[]>({ queryKey: ['decks'], queryFn: deckApi.list })
  const { data: pool = [], isLoading: poolLoading } = useQuery<Deck[]>({
    queryKey: ['decks', 'pool'], queryFn: deckApi.pool, enabled: tab === 'pool',
  })

  const createMut = useMutation({
    mutationFn: (body: object) => editDeck ? deckApi.update(editDeck.id, body) : deckApi.create(body),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['decks'] }); setShowCreate(false); setEditDeck(null); setForm(emptyForm()) },
  })
  const deleteMut = useMutation({
    mutationFn: (id: number) => deckApi.remove(id),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['decks'] }); setDeleteTarget(null) },
  })
  const copyMut = useMutation({
    mutationFn: (id: number) => deckApi.copyPool(id),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['decks'] }); setTab('mine') },
  })
  const fromTextMut = useMutation({
    mutationFn: () => cardApi.fromText(fromTextDeck!.id, {
      text: fromTextForm.text,
      count: Number(fromTextForm.count),
      language: lang,
    }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['decks'] })
      setFromTextDeck(null)
      setFromTextForm({ text: '', count: '8' })
    },
  })

  const openEdit = (d: Deck) => {
    setForm({ name: d.name, description: d.description || '', dailyNewCardLimit: String(d.dailyNewCardLimit ?? 20), dailyReviewLimit: String(d.dailyReviewLimit ?? 100) })
    setEditDeck(d)
    setShowCreate(true)
  }

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    createMut.mutate({ name: form.name, description: form.description, dailyNewCardLimit: +form.dailyNewCardLimit, dailyReviewLimit: +form.dailyReviewLimit })
  }

  if (isLoading) return <PageSpinner />

  return (
    <div className="p-6 space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-900 dark:text-white">{t('deck.title')}</h1>
        <Button onClick={() => { setForm(emptyForm()); setEditDeck(null); setShowCreate(true) }}>
          <Plus className="w-4 h-4" />{t('deck.create')}
        </Button>
      </div>

      {/* Tabs */}
      <div className="flex gap-2 border-b border-gray-200 dark:border-gray-700">
        {(['mine', 'pool'] as const).map(t2 => (
          <button key={t2} onClick={() => setTab(t2)}
            className={`px-4 py-2 text-sm font-medium border-b-2 transition-colors ${tab === t2 ? 'border-primary-600 text-primary-600' : 'border-transparent text-gray-500 hover:text-gray-700 dark:hover:text-gray-300'}`}>
            {t2 === 'mine' ? t('deck.title') : t('deck.poolDecks')}
          </button>
        ))}
      </div>

      {/* Deck grid */}
      {tab === 'mine' ? (
        decks.length === 0 ? (
          <div className="text-center py-16 text-gray-400">{t('deck.noDecks')}</div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
            {decks.map(deck => (
              <Card key={deck.id} hoverable>
                <CardBody className="space-y-3">
                  <div className="flex items-start justify-between">
                    <div>
                      <h3 className="font-semibold text-gray-900 dark:text-white">{deck.name}</h3>
                      {deck.description && <p className="text-xs text-gray-400 mt-0.5">{deck.description}</p>}
                    </div>
                    <Badge variant={deck.type === 'BUILTIN' ? 'info' : 'default'}>{deck.type}</Badge>
                  </div>
                  <div className="flex gap-3 text-xs text-gray-500 dark:text-gray-400">
                    <span className="flex items-center gap-1"><Layers className="w-3.5 h-3.5" />{deck.totalCards} {t('deck.cards')}</span>
                    <span className="flex items-center gap-1 text-orange-500"><BookOpen className="w-3.5 h-3.5" />{deck.dueCards} {t('deck.due')}</span>
                    {deck.newCards > 0 && <span className="text-primary-500">{deck.newCards} {t('deck.new')}</span>}
                  </div>
                  <div className="grid grid-cols-2 gap-2">
                    <Button size="sm" onClick={() => navigate(`/review/${deck.id}`)} className="flex-1">
                      <Play className="w-3.5 h-3.5" />{t('deck.startReview')}
                    </Button>
                    <Button size="sm" variant="secondary" onClick={() => navigate(`/decks/${deck.id}`)}>
                      <ListChecks className="w-3.5 h-3.5" />管理卡片
                    </Button>
                    <Button size="sm" variant="secondary" onClick={() => setFromTextDeck(deck)}>
                      <Sparkles className="w-3.5 h-3.5" />一键成卡
                    </Button>
                    <Button size="sm" variant="secondary" onClick={() => openEdit(deck)}>
                      <Pencil className="w-3.5 h-3.5" />{t('common.edit')}
                    </Button>
                    {(user?.role === 'ADMIN' || deck.type === 'CUSTOM') && (
                      <Button title="删除牌组" size="sm" variant="ghost" onClick={() => setDeleteTarget(deck)} className="col-span-2">
                        <Trash2 className="w-3.5 h-3.5" />
                        删除牌组
                      </Button>
                    )}
                  </div>
                </CardBody>
              </Card>
            ))}
          </div>
        )
      ) : (
        poolLoading ? <PageSpinner /> : (
          <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
            {pool.map(deck => (
              <Card key={deck.id} hoverable>
                <CardBody className="space-y-3">
                  <div>
                    <h3 className="font-semibold text-gray-900 dark:text-white">{deck.name}</h3>
                    {deck.description && <p className="text-xs text-gray-400 mt-0.5">{deck.description}</p>}
                    <p className="text-xs text-gray-400">{deck.totalCards} {t('deck.cards')}</p>
                  </div>
                  <Button size="sm" onClick={() => copyMut.mutate(deck.id)} loading={copyMut.isPending} className="w-full">
                    <Copy className="w-3.5 h-3.5" />{t('deck.copyDeck')}
                  </Button>
                </CardBody>
              </Card>
            ))}
          </div>
        )
      )}

      {/* Create/Edit Modal */}
      <Modal open={showCreate} onClose={() => { setShowCreate(false); setEditDeck(null) }}
        title={editDeck ? t('deck.edit') : t('deck.create')}>
        <form onSubmit={handleSubmit} className="space-y-4">
          <Input label={t('deck.name')} value={form.name} onChange={e => setForm(f => ({ ...f, name: e.target.value }))} required autoFocus />
          <Input label={t('deck.description')} value={form.description} onChange={e => setForm(f => ({ ...f, description: e.target.value }))} />
          <div className="grid grid-cols-2 gap-3">
            <Input label={t('deck.dailyNewLimit')} type="number" min="0" value={form.dailyNewCardLimit} onChange={e => setForm(f => ({ ...f, dailyNewCardLimit: e.target.value }))} />
            <Input label={t('deck.dailyReviewLimit')} type="number" min="0" value={form.dailyReviewLimit} onChange={e => setForm(f => ({ ...f, dailyReviewLimit: e.target.value }))} />
          </div>
          <div className="flex gap-3 justify-end">
            <Button type="button" variant="secondary" onClick={() => { setShowCreate(false); setEditDeck(null) }}>{t('common.cancel')}</Button>
            <Button type="submit" loading={createMut.isPending}>{t('common.save')}</Button>
          </div>
        </form>
      </Modal>

      {/* Delete Confirm Modal */}
      <Modal open={!!deleteTarget} onClose={() => setDeleteTarget(null)} title={t('deck.delete')} size="sm">
        <p className="text-sm text-gray-600 dark:text-gray-300 mb-4">
          {t('deck.confirmDelete', { name: deleteTarget?.name })}
        </p>
        <div className="flex gap-3 justify-end">
          <Button variant="secondary" onClick={() => setDeleteTarget(null)}>{t('common.cancel')}</Button>
          <Button variant="danger" loading={deleteMut.isPending} onClick={() => deleteTarget && deleteMut.mutate(deleteTarget.id)}>
            {t('common.delete')}
          </Button>
        </div>
      </Modal>

      <Modal open={!!fromTextDeck} onClose={() => setFromTextDeck(null)} title="面经 / 笔记一键成卡" size="xl">
        <div className="space-y-4">
          <p className="text-sm text-gray-500 dark:text-gray-400">
            目标牌组：<span className="font-medium text-gray-800 dark:text-gray-100">{fromTextDeck?.name}</span>
          </p>
          <div className="grid grid-cols-1 sm:grid-cols-[1fr_120px] gap-3">
            <Input
              label="生成数量"
              type="number"
              min="1"
              max="30"
              value={fromTextForm.count}
              onChange={e => setFromTextForm(f => ({ ...f, count: e.target.value }))}
            />
          </div>
          <div className="space-y-1">
            <label className="text-sm font-medium text-gray-700 dark:text-gray-300">面经 / 笔记原文</label>
            <textarea
              value={fromTextForm.text}
              rows={12}
              placeholder="粘贴面经、复盘笔记、项目材料或知识点摘要，AI 会抽取成原子化问答卡片。"
              onChange={e => setFromTextForm(f => ({ ...f, text: e.target.value }))}
              className="w-full px-3 py-2 text-sm rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-800 text-gray-900 dark:text-gray-100 focus:outline-none focus:ring-2 focus:ring-primary-500"
            />
          </div>
          {fromTextMut.isError && <p className="text-sm text-red-500">生成失败，请确认 AI Key 可用后重试。</p>}
          <div className="flex gap-3 justify-end">
            <Button variant="secondary" onClick={() => setFromTextDeck(null)}>取消</Button>
            <Button onClick={() => fromTextMut.mutate()} loading={fromTextMut.isPending} disabled={!fromTextForm.text.trim()}>
              <Sparkles className="w-4 h-4" />生成卡片
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  )
}
