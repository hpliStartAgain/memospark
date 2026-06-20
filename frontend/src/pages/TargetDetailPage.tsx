import { useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { targetApi } from '@/lib/api'
import { useAppStore } from '@/store/appStore'
import { PageSpinner } from '@/components/ui/Spinner'
import Card, { CardBody, CardHeader } from '@/components/ui/Card'
import Button from '@/components/ui/Button'
import Modal from '@/components/ui/Modal'
import Input from '@/components/ui/Input'
import Badge from '@/components/ui/Badge'
import ReadinessRing from '@/components/ReadinessRing'
import { cn } from '@/lib/utils'
import type { TargetDetail, TargetSkill, JobJd } from '@/types'
import {
  ArrowLeft, Plus, Trash2, Sparkles, Building2, CalendarClock,
  FileText, Lightbulb, ChevronDown, ChevronUp,
} from 'lucide-react'

function countdownText(days?: number) {
  if (days == null) return '未设置面试日期'
  if (days < 0) return `面试已过 ${Math.abs(days)} 天`
  if (days === 0) return '今天面试！'
  return `距面试 ${days} 天`
}

function MetricBar({ label, value }: { label: string; value: number }) {
  const color = value >= 70 ? 'bg-green-500' : value >= 40 ? 'bg-yellow-500' : 'bg-red-500'
  return (
    <div className="space-y-1">
      <div className="flex justify-between text-xs text-gray-500 dark:text-gray-400">
        <span>{label}</span><span>{value}</span>
      </div>
      <div className="h-2 rounded-full bg-gray-100 dark:bg-gray-800 overflow-hidden">
        <div className={cn('h-full rounded-full transition-all', color)} style={{ width: `${value}%` }} />
      </div>
    </div>
  )
}

function SkillRow({ skill, targetId }: { skill: TargetSkill; targetId: number }) {
  const qc = useQueryClient()
  const updateMut = useMutation({
    mutationFn: (body: object) => targetApi.updateSkill(targetId, skill.id, body),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['target', String(targetId)] }),
  })
  const deleteMut = useMutation({
    mutationFn: () => targetApi.removeSkill(targetId, skill.id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['target', String(targetId)] }),
  })

  return (
    <div className="p-3 rounded-lg border border-gray-100 dark:border-gray-800 space-y-2">
      <div className="flex items-start justify-between gap-2">
        <div className="min-w-0">
          <div className="flex items-center gap-2 flex-wrap">
            <span className="font-medium text-sm text-gray-900 dark:text-white">{skill.name}</span>
            <Badge variant="warning">重要度 {skill.weight}</Badge>
          </div>
          {skill.description && (
            <p className="text-xs text-gray-500 dark:text-gray-400 mt-1 leading-relaxed">{skill.description}</p>
          )}
        </div>
        <button onClick={() => deleteMut.mutate()} disabled={deleteMut.isPending}
          className="p-1 rounded text-gray-300 hover:text-red-500 shrink-0">
          <Trash2 className="w-4 h-4" />
        </button>
      </div>
      <div className="flex items-center gap-2">
        <span className="text-xs text-gray-400 shrink-0">掌握度</span>
        <div className="flex gap-1">
          {[0, 1, 2, 3, 4, 5].map(lvl => (
            <button key={lvl}
              onClick={() => updateMut.mutate({ selfLevel: lvl })}
              disabled={updateMut.isPending}
              className={cn(
                'w-7 h-7 rounded-md text-xs font-medium transition-colors',
                skill.selfLevel >= lvl && lvl > 0
                  ? 'bg-primary-600 text-white'
                  : skill.selfLevel === 0 && lvl === 0
                    ? 'bg-gray-200 dark:bg-gray-700 text-gray-600 dark:text-gray-300'
                    : 'bg-gray-100 dark:bg-gray-800 text-gray-400 hover:bg-primary-100 dark:hover:bg-primary-900/20',
              )}>
              {lvl}
            </button>
          ))}
        </div>
      </div>
    </div>
  )
}

function JdItem({ jd, targetId }: { jd: JobJd; targetId: number }) {
  const qc = useQueryClient()
  const [open, setOpen] = useState(false)
  const deleteMut = useMutation({
    mutationFn: () => targetApi.removeJd(targetId, jd.id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['target', String(targetId)] }),
  })
  return (
    <div className="rounded-lg border border-gray-100 dark:border-gray-800">
      <div className="flex items-center justify-between gap-2 p-3">
        <button onClick={() => setOpen(o => !o)} className="flex items-center gap-2 min-w-0 text-left">
          {open ? <ChevronUp className="w-4 h-4 shrink-0" /> : <ChevronDown className="w-4 h-4 shrink-0" />}
          <span className="text-sm font-medium text-gray-800 dark:text-gray-200 truncate">
            {jd.title || jd.source || 'JD'}
          </span>
        </button>
        <button onClick={() => deleteMut.mutate()} disabled={deleteMut.isPending}
          className="p-1 rounded text-gray-300 hover:text-red-500 shrink-0">
          <Trash2 className="w-4 h-4" />
        </button>
      </div>
      {open && (
        <p className="px-3 pb-3 text-xs text-gray-600 dark:text-gray-400 whitespace-pre-wrap leading-relaxed border-t border-gray-100 dark:border-gray-800 pt-2">
          {jd.content}
        </p>
      )}
    </div>
  )
}

export default function TargetDetailPage() {
  const { id } = useParams()
  const targetId = Number(id)
  const navigate = useNavigate()
  const qc = useQueryClient()
  const { lang } = useAppStore()

  const [jdOpen, setJdOpen] = useState(false)
  const [jdForm, setJdForm] = useState({ title: '', content: '', source: '' })
  const [skillOpen, setSkillOpen] = useState(false)
  const [skillForm, setSkillForm] = useState({ name: '', description: '', weight: '3' })
  const [confirmDelete, setConfirmDelete] = useState(false)

  const { data: target, isLoading } = useQuery<TargetDetail>({
    queryKey: ['target', String(targetId)],
    queryFn: () => targetApi.get(targetId),
    enabled: !Number.isNaN(targetId),
  })

  const invalidate = () => {
    qc.invalidateQueries({ queryKey: ['target', String(targetId)] })
    qc.invalidateQueries({ queryKey: ['targets'] })
  }

  const addJdMut = useMutation({
    mutationFn: () => targetApi.addJd(targetId, jdForm),
    onSuccess: () => { invalidate(); setJdOpen(false); setJdForm({ title: '', content: '', source: '' }) },
  })
  const analyzeMut = useMutation({
    mutationFn: (replace: boolean) => targetApi.analyze(targetId, lang, replace),
    onSuccess: invalidate,
  })
  const addSkillMut = useMutation({
    mutationFn: () => targetApi.addSkill(targetId, {
      name: skillForm.name, description: skillForm.description || null, weight: Number(skillForm.weight),
    }),
    onSuccess: () => { invalidate(); setSkillOpen(false); setSkillForm({ name: '', description: '', weight: '3' }) },
  })
  const deleteMut = useMutation({
    mutationFn: () => targetApi.remove(targetId),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['targets'] }); navigate('/targets') },
  })

  if (isLoading || !target) return <PageSpinner />

  const r = target.readiness

  return (
    <div className="p-6 space-y-6 max-w-5xl">
      {/* Header */}
      <div className="flex items-start justify-between gap-3 flex-wrap">
        <div className="flex items-start gap-3">
          <button onClick={() => navigate('/targets')} className="p-1.5 mt-0.5 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-800 text-gray-500">
            <ArrowLeft className="w-5 h-5" />
          </button>
          <div>
            <h1 className="text-2xl font-bold text-gray-900 dark:text-white">{target.title}</h1>
            <div className="flex items-center gap-3 mt-1 text-sm text-gray-500 dark:text-gray-400 flex-wrap">
              {target.company && <span className="flex items-center gap-1"><Building2 className="w-4 h-4" />{target.company}</span>}
              <span className="flex items-center gap-1"><CalendarClock className="w-4 h-4" />{countdownText(target.daysUntilInterview)}</span>
            </div>
          </div>
        </div>
        <Button variant="danger" size="sm" onClick={() => setConfirmDelete(true)}>
          <Trash2 className="w-4 h-4" />删除
        </Button>
      </div>

      {/* Readiness */}
      <Card>
        <CardBody className="flex flex-col md:flex-row items-center gap-6">
          <ReadinessRing value={r.overall} size={120} stroke={10} label="面试就绪度" />
          <div className="flex-1 w-full grid grid-cols-1 sm:grid-cols-3 gap-4">
            <MetricBar label="技能覆盖" value={r.skillCoverage} />
            <MetricBar label="卡片健康" value={r.cardHealth} />
            <MetricBar label="错题清空" value={r.wrongClear} />
          </div>
          <div className="flex md:flex-col gap-4 md:gap-2 text-center md:text-right">
            <div><span className="text-lg font-bold text-orange-500">{r.dueCards}</span><p className="text-xs text-gray-400">待复习卡</p></div>
            <div><span className="text-lg font-bold text-red-500">{r.dueNotes}</span><p className="text-xs text-gray-400">待清错题</p></div>
            <div><span className="text-lg font-bold text-yellow-500">{r.weakSkills}</span><p className="text-xs text-gray-400">薄弱技能</p></div>
          </div>
        </CardBody>
      </Card>

      {/* JDs */}
      <Card>
        <CardHeader className="flex items-center justify-between">
          <h2 className="text-base font-semibold text-gray-900 dark:text-white flex items-center gap-2">
            <FileText className="w-4 h-4" />岗位 JD（{target.jds.length}）
          </h2>
          <div className="flex gap-2">
            <Button size="sm" variant="secondary" onClick={() => setJdOpen(true)}>
              <Plus className="w-3.5 h-3.5" />添加 JD
            </Button>
            <Button size="sm" onClick={() => analyzeMut.mutate(target.skills.length > 0)} loading={analyzeMut.isPending}
              disabled={target.jds.length === 0}>
              <Sparkles className="w-3.5 h-3.5" />AI 分析技能
            </Button>
          </div>
        </CardHeader>
        <CardBody className="space-y-2">
          {target.jds.length === 0 ? (
            <p className="text-sm text-gray-400 text-center py-4">还没有 JD。粘贴 1~3 份意向岗位描述，再点「AI 分析技能」。</p>
          ) : (
            target.jds.map(jd => <JdItem key={jd.id} jd={jd} targetId={targetId} />)
          )}
          {analyzeMut.isError && <p className="text-xs text-red-500">分析失败，请确认已配置 AI Key 并稍后重试。</p>}
        </CardBody>
      </Card>

      {/* Skills */}
      <Card>
        <CardHeader className="flex items-center justify-between">
          <h2 className="text-base font-semibold text-gray-900 dark:text-white flex items-center gap-2">
            <Lightbulb className="w-4 h-4" />技能需求与掌握度（{target.skills.length}）
          </h2>
          <Button size="sm" variant="secondary" onClick={() => setSkillOpen(true)}>
            <Plus className="w-3.5 h-3.5" />手动添加
          </Button>
        </CardHeader>
        <CardBody className="space-y-2">
          {target.skills.length === 0 ? (
            <p className="text-sm text-gray-400 text-center py-4">还没有技能项。添加 JD 后点「AI 分析技能」，或手动添加。</p>
          ) : (
            target.skills.map(s => <SkillRow key={s.id} skill={s} targetId={targetId} />)
          )}
        </CardBody>
      </Card>

      {/* Add JD modal */}
      <Modal open={jdOpen} onClose={() => setJdOpen(false)} title="添加岗位 JD" size="xl">
        <div className="space-y-4">
          <div className="grid grid-cols-2 gap-3">
            <Input label="标题（可选）" value={jdForm.title} placeholder="如：字节-后端"
              onChange={e => setJdForm(f => ({ ...f, title: e.target.value }))} />
            <Input label="来源（可选）" value={jdForm.source} placeholder="如：Boss直聘"
              onChange={e => setJdForm(f => ({ ...f, source: e.target.value }))} />
          </div>
          <div className="flex flex-col gap-1">
            <label className="text-sm font-medium text-gray-700 dark:text-gray-300">JD 原文 *</label>
            <textarea value={jdForm.content} rows={10}
              placeholder="粘贴岗位职责与任职要求……"
              onChange={e => setJdForm(f => ({ ...f, content: e.target.value }))}
              className="w-full px-3 py-2 text-sm rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-800 text-gray-900 dark:text-gray-100 focus:outline-none focus:ring-2 focus:ring-primary-500" />
          </div>
          <div className="flex gap-3 justify-end">
            <Button variant="secondary" onClick={() => setJdOpen(false)}>取消</Button>
            <Button onClick={() => addJdMut.mutate()} loading={addJdMut.isPending} disabled={!jdForm.content.trim()}>保存</Button>
          </div>
        </div>
      </Modal>

      {/* Add skill modal */}
      <Modal open={skillOpen} onClose={() => setSkillOpen(false)} title="手动添加技能">
        <div className="space-y-4">
          <Input label="技能名称 *" value={skillForm.name} autoFocus placeholder="如：Redis 原理与实战"
            onChange={e => setSkillForm(f => ({ ...f, name: e.target.value }))} />
          <Input label="说明（可选）" value={skillForm.description}
            onChange={e => setSkillForm(f => ({ ...f, description: e.target.value }))} />
          <div className="flex flex-col gap-1">
            <label className="text-sm font-medium text-gray-700 dark:text-gray-300">重要度 (1-5)</label>
            <select value={skillForm.weight}
              onChange={e => setSkillForm(f => ({ ...f, weight: e.target.value }))}
              className="w-full px-3 py-2 text-sm rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-800 text-gray-900 dark:text-gray-100 focus:outline-none focus:ring-2 focus:ring-primary-500">
              {[1, 2, 3, 4, 5].map(w => <option key={w} value={w}>{w}</option>)}
            </select>
          </div>
          <div className="flex gap-3 justify-end">
            <Button variant="secondary" onClick={() => setSkillOpen(false)}>取消</Button>
            <Button onClick={() => addSkillMut.mutate()} loading={addSkillMut.isPending} disabled={!skillForm.name.trim()}>添加</Button>
          </div>
        </div>
      </Modal>

      {/* Delete confirm */}
      <Modal open={confirmDelete} onClose={() => setConfirmDelete(false)} title="删除目标岗位" size="sm">
        <p className="text-sm text-gray-600 dark:text-gray-300 mb-4">
          确定删除「{target.title}」？其 JD 与技能分析将一并删除，不可撤销。
        </p>
        <div className="flex gap-3 justify-end">
          <Button variant="secondary" onClick={() => setConfirmDelete(false)}>取消</Button>
          <Button variant="danger" loading={deleteMut.isPending} onClick={() => deleteMut.mutate()}>删除</Button>
        </div>
      </Modal>
    </div>
  )
}
