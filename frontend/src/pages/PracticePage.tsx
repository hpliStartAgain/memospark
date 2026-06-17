import { useState, useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import { useQuery } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { practiceApi } from '@/lib/api'
import { PageSpinner } from '@/components/ui/Spinner'
import Card, { CardBody } from '@/components/ui/Card'
import Badge from '@/components/ui/Badge'
import Input from '@/components/ui/Input'
import type { Problem } from '@/types'
import { difficultyBg } from '@/lib/utils'
import { CheckCircle, Search } from 'lucide-react'

const DIFFICULTIES = ['', 'Easy', 'Medium', 'Hard'] as const

export default function PracticePage() {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const [search, setSearch] = useState('')
  const [diff, setDiff] = useState<string>('')
  const [onlyAccepted, setOnlyAccepted] = useState(false)

  const { data: problems = [], isLoading } = useQuery<Problem[]>({
    queryKey: ['problems'],
    queryFn: practiceApi.problems,
  })

  const filtered = useMemo(() => {
    let list = problems
    if (search) list = list.filter(p => p.title.toLowerCase().includes(search.toLowerCase()) || String(p.problemNumber).includes(search))
    if (diff) list = list.filter(p => p.difficulty === diff)
    if (onlyAccepted) list = list.filter(p => p.accepted)
    return list
  }, [problems, search, diff, onlyAccepted])

  if (isLoading) return <PageSpinner />

  const acceptedCount = problems.filter(p => p.accepted).length

  return (
    <div className="p-6 space-y-5">
      <div className="flex items-center justify-between flex-wrap gap-3">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-white">{t('practice.title')}</h1>
          <p className="text-sm text-gray-400 mt-0.5">{acceptedCount} / {problems.length} 已通过</p>
        </div>
      </div>

      {/* Filters */}
      <div className="flex flex-wrap gap-3 items-center">
        <div className="relative flex-1 min-w-48">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
          <input
            className="w-full pl-9 pr-3 py-2 text-sm rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-800 focus:outline-none focus:ring-2 focus:ring-primary-500"
            placeholder={t('practice.search')}
            value={search}
            onChange={e => setSearch(e.target.value)}
          />
        </div>
        <div className="flex gap-1">
          {DIFFICULTIES.map(d => (
            <button key={d}
              onClick={() => setDiff(d)}
              className={`px-3 py-1.5 rounded-lg text-sm font-medium transition-colors ${diff === d ? 'bg-primary-600 text-white' : 'bg-gray-100 dark:bg-gray-800 text-gray-600 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-gray-700'}`}>
              {d || t('practice.all')}
            </button>
          ))}
        </div>
        <button
          onClick={() => setOnlyAccepted(!onlyAccepted)}
          className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-sm font-medium transition-colors ${onlyAccepted ? 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-300' : 'bg-gray-100 dark:bg-gray-800 text-gray-600 dark:text-gray-300'}`}>
          <CheckCircle className="w-4 h-4" />{t('practice.accepted')}
        </button>
      </div>

      {/* Problem list */}
      <div className="space-y-1">
        {filtered.map(p => (
          <Card key={p.id} hoverable onClick={() => navigate(`/practice/${p.id}`)}
            className="transition-all hover:border-primary-200 dark:hover:border-primary-800">
            <CardBody className="flex items-center gap-4 py-3">
              <span className={`w-6 h-6 rounded-full flex items-center justify-center shrink-0 ${p.accepted ? 'text-green-500' : 'text-gray-300 dark:text-gray-600'}`}>
                <CheckCircle className="w-5 h-5" />
              </span>
              <span className="w-12 text-sm text-gray-400 shrink-0">#{p.problemNumber}</span>
              <span className="flex-1 text-sm font-medium text-gray-900 dark:text-white truncate">{p.title}</span>
              {p.category && <span className="text-xs text-gray-400 hidden md:block">{p.category}</span>}
              {p.failCount > 0 && (
                <span className="text-xs text-red-400 hidden md:block">{p.failCount} 次错误</span>
              )}
              <Badge className={difficultyBg(p.difficulty)}>{p.difficulty}</Badge>
            </CardBody>
          </Card>
        ))}
        {filtered.length === 0 && (
          <div className="text-center py-12 text-gray-400">{t('common.noData')}</div>
        )}
      </div>
    </div>
  )
}
