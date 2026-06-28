import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { targetApi } from '@/lib/api'
import { PageSpinner } from '@/components/ui/Spinner'
import Card, { CardBody } from '@/components/ui/Card'
import Button from '@/components/ui/Button'
import Modal from '@/components/ui/Modal'
import Input from '@/components/ui/Input'
import Badge from '@/components/ui/Badge'
import ReadinessRing from '@/components/ReadinessRing'
import type { TargetSummary, TargetStatus } from '@/types'
import { Plus, Target as TargetIcon, Building2, CalendarClock, Layers, Lightbulb } from 'lucide-react'

const STATUS_META: Record<TargetStatus, { label: string; variant: 'info' | 'primary' | 'warning' | 'default' | 'success' | 'danger' }> = {
  PREPARING:    { label: '准备中', variant: 'info' },
  APPLIED:      { label: '已投递', variant: 'primary' },
  WRITTEN_TEST: { label: '笔试', variant: 'warning' },
  INTERVIEW_1:  { label: '一面', variant: 'warning' },
  INTERVIEW_2:  { label: '二面', variant: 'warning' },
  HR:           { label: 'HR 面', variant: 'info' },
  OFFER:        { label: 'Offer', variant: 'success' },
  REJECTED:     { label: '淘汰', variant: 'danger' },
  INTERVIEWING: { label: '面试中（旧）', variant: 'warning' },
  CLOSED:       { label: '已结束（旧）', variant: 'default' },
}

const PIPELINE_STAGES: TargetStatus[] = [
  'PREPARING', 'APPLIED', 'WRITTEN_TEST', 'INTERVIEW_1', 'INTERVIEW_2',
  'HR', 'OFFER', 'REJECTED', 'INTERVIEWING', 'CLOSED',
]

interface FormState { title: string; company: string; interviewDate: string; status: TargetStatus; notes: string }
const emptyForm = (): FormState => ({ title: '', company: '', interviewDate: '', status: 'PREPARING', notes: '' })

function countdownText(days?: number) {
  if (days == null) return '未设置面试日期'
  if (days < 0) return `面试已过 ${Math.abs(days)} 天`
  if (days === 0) return '今天面试！'
  return `距面试 ${days} 天`
}

export default function TargetsPage() {
  const navigate = useNavigate()
  const qc = useQueryClient()
  const [showCreate, setShowCreate] = useState(false)
  const [form, setForm] = useState<FormState>(emptyForm())

  const { data: targets = [], isLoading } = useQuery<TargetSummary[]>({
    queryKey: ['targets'], queryFn: targetApi.list,
  })

  const createMut = useMutation({
    mutationFn: (body: object) => targetApi.create(body),
    onSuccess: (created: { id: number }) => {
      qc.invalidateQueries({ queryKey: ['targets'] })
      setShowCreate(false)
      setForm(emptyForm())
      if (created?.id) navigate(`/targets/${created.id}`)
    },
  })
  const statusMut = useMutation({
    mutationFn: ({ id, status }: { id: number; status: TargetStatus }) => targetApi.updateStatus(id, status),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['targets'] })
      qc.invalidateQueries({ queryKey: ['target'] })
    },
  })

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    createMut.mutate({
      title: form.title,
      company: form.company || null,
      status: form.status,
      interviewDate: form.interviewDate || null,
      notes: form.notes || null,
    })
  }

  if (isLoading) return <PageSpinner />

  return (
    <div className="p-6 space-y-6">
      <div className="flex items-center justify-between flex-wrap gap-3">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-white">目标岗位</h1>
          <p className="text-sm text-gray-500 dark:text-gray-400 mt-0.5">录入意向岗位与 JD，AI 分析技能缺口并跟踪面试就绪度</p>
        </div>
        <Button onClick={() => { setForm(emptyForm()); setShowCreate(true) }}>
          <Plus className="w-4 h-4" />新建目标
        </Button>
      </div>

      {targets.length === 0 ? (
        <div className="text-center py-20 text-gray-400">
          <TargetIcon className="w-12 h-12 mx-auto mb-3 opacity-40" />
          <p>还没有目标岗位，点击「新建目标」开始你的备考闭环</p>
        </div>
      ) : (
        <div className="overflow-x-auto pb-3">
          <div className="flex gap-4 min-w-max items-start">
            {PIPELINE_STAGES.map(status => {
              const stageTargets = targets.filter(target => target.status === status)
              if (stageTargets.length === 0 && ['INTERVIEWING', 'CLOSED'].includes(status)) return null
              return (
                <section key={status} className="w-72 shrink-0">
                  <div className="flex items-center justify-between px-1 mb-2">
                    <div className="flex items-center gap-2">
                      <Badge variant={STATUS_META[status].variant}>{STATUS_META[status].label}</Badge>
                      <span className="text-xs text-gray-400">{stageTargets.length}</span>
                    </div>
                  </div>
                  <div className="space-y-3 min-h-24">
                    {stageTargets.map(t => (
                      <Card key={t.id} hoverable onClick={() => navigate(`/targets/${t.id}`)}>
                        <CardBody className="space-y-3">
                          <div className="flex items-center gap-3">
                            <ReadinessRing value={t.readiness} size={64} stroke={6} />
                            <div className="flex-1 min-w-0">
                              <h3 className="font-semibold text-gray-900 dark:text-white truncate">{t.title}</h3>
                              {t.company && (
                                <p className="text-xs text-gray-500 flex items-center gap-1 truncate mt-1">
                                  <Building2 className="w-3.5 h-3.5 shrink-0" />{t.company}
                                </p>
                              )}
                            </div>
                          </div>
                          <p className="text-xs text-gray-500 flex items-center gap-1">
                            <CalendarClock className="w-3.5 h-3.5 shrink-0" />{countdownText(t.daysUntilInterview)}
                          </p>
                          <div className="flex gap-3 text-xs text-gray-400">
                            <span className="flex items-center gap-1"><Layers className="w-3.5 h-3.5" />{t.jdCount} JD</span>
                            <span className="flex items-center gap-1"><Lightbulb className="w-3.5 h-3.5" />{t.skillCount} 技能</span>
                          </div>
                          <select
                            value={t.status}
                            onClick={e => e.stopPropagation()}
                            onChange={e => statusMut.mutate({ id: t.id, status: e.target.value as TargetStatus })}
                            disabled={statusMut.isPending}
                            className="w-full px-2 py-1.5 text-xs rounded-md border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 text-gray-700 dark:text-gray-200"
                          >
                            {PIPELINE_STAGES.map(s => (
                              <option key={s} value={s}>{STATUS_META[s].label}</option>
                            ))}
                          </select>
                        </CardBody>
                      </Card>
                    ))}
                  </div>
                </section>
              )
            })}
          </div>
        </div>
      )}

      <Modal open={showCreate} onClose={() => setShowCreate(false)} title="新建目标岗位">
        <form onSubmit={handleSubmit} className="space-y-4">
          <Input label="岗位名称 *" value={form.title} autoFocus required
            placeholder="如：后端开发工程师 (Java)"
            onChange={e => setForm(f => ({ ...f, title: e.target.value }))} />
          <Input label="公司 / 方向" value={form.company}
            placeholder="如：字节跳动 / 中厂后端"
            onChange={e => setForm(f => ({ ...f, company: e.target.value }))} />
          <div className="grid grid-cols-2 gap-3">
            <Input label="面试日期" type="date" value={form.interviewDate}
              onChange={e => setForm(f => ({ ...f, interviewDate: e.target.value }))} />
            <div className="flex flex-col gap-1">
              <label className="text-sm font-medium text-gray-700 dark:text-gray-300">状态</label>
              <select value={form.status}
                onChange={e => setForm(f => ({ ...f, status: e.target.value as TargetStatus }))}
                className="w-full px-3 py-2 text-sm rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-800 text-gray-900 dark:text-gray-100 focus:outline-none focus:ring-2 focus:ring-primary-500">
                {PIPELINE_STAGES.filter(s => !['INTERVIEWING', 'CLOSED'].includes(s)).map(status => (
                  <option key={status} value={status}>{STATUS_META[status].label}</option>
                ))}
              </select>
            </div>
          </div>
          <div className="flex gap-3 justify-end">
            <Button type="button" variant="secondary" onClick={() => setShowCreate(false)}>取消</Button>
            <Button type="submit" loading={createMut.isPending}>创建</Button>
          </div>
        </form>
      </Modal>
    </div>
  )
}
