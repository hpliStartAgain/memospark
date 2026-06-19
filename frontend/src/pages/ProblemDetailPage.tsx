import { useState, useEffect, useRef, useCallback } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { practiceApi, openSubmissionStream } from '@/lib/api'
import { PageSpinner } from '@/components/ui/Spinner'
import Button from '@/components/ui/Button'
import Badge from '@/components/ui/Badge'
import Card, { CardBody, CardHeader } from '@/components/ui/Card'
import type { ProblemDetail, Submission, SubmitResult, TestCaseResult } from '@/types'
import { difficultyBg, statusColor, formatDateTime } from '@/lib/utils'
import { ChevronLeft, CheckCircle, XCircle, Clock } from 'lucide-react'
import { cn } from '@/lib/utils'
import Editor from '@monaco-editor/react'

type Tab = 'description' | 'submissions'
type Lang = 'java' | 'python'

export default function ProblemDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const { t } = useTranslation()
  const qc = useQueryClient()

  const [tab, setTab] = useState<Tab>('description')
  const [lang, setLang] = useState<Lang>('java')
  const [code, setCode] = useState('')
  const [submitResult, setSubmitResult] = useState<SubmitResult | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [progress, setProgress] = useState<{ case: number; total: number } | null>(null)
  const [isDark, setIsDark] = useState(false)
  const esRef = useRef<EventSource | null>(null)

  const codeStorageKey = `code_${id}_${lang}`

  // Detect dark mode from document class
  useEffect(() => {
    const checkDark = () => setIsDark(document.documentElement.classList.contains('dark'))
    checkDark()
    const observer = new MutationObserver(checkDark)
    observer.observe(document.documentElement, { attributes: true, attributeFilter: ['class'] })
    return () => observer.disconnect()
  }, [])

  const { data: problem, isLoading } = useQuery<ProblemDetail>({
    queryKey: ['problem', id],
    queryFn: () => practiceApi.problem(Number(id)),
  })

  const { data: submissions = [] } = useQuery<Submission[]>({
    queryKey: ['submissions', id],
    queryFn: () => practiceApi.submissions(Number(id)),
    enabled: tab === 'submissions',
  })

  useEffect(() => {
    if (problem) {
      const saved = localStorage.getItem(codeStorageKey)
      if (saved && saved.trim()) {
        setCode(saved)
      } else {
        setCode(lang === 'java' ? (problem.javaTemplate || '') : (problem.pythonTemplate || ''))
      }
    }
  }, [problem, lang, codeStorageKey])

  // Auto-save code to localStorage with debounce
  useEffect(() => {
    if (!code) return
    const timer = setTimeout(() => {
      localStorage.setItem(codeStorageKey, code)
    }, 500)
    return () => clearTimeout(timer)
  }, [code, codeStorageKey])

  useEffect(() => () => { esRef.current?.close() }, [])

  const handleSubmit = async () => {
    if (!problem || !code.trim() || submitting) return
    setSubmitting(true)
    setSubmitResult(null)
    setProgress(null)

    try {
      const res = await practiceApi.submit(problem.id, lang, code)
      const submissionId: number = res.submissionId

      esRef.current?.close()
      esRef.current = openSubmissionStream(
        submissionId,
        (p) => setProgress({ case: p.case, total: p.total }),
        (result) => {
          setSubmitResult(result as SubmitResult)
          setSubmitting(false)
          setProgress(null)
          qc.invalidateQueries({ queryKey: ['problems'] })
          qc.invalidateQueries({ queryKey: ['submissions', id] })
        },
        (msg) => {
          setSubmitResult({ submissionId, status: 'SYSTEM_ERROR', passedCases: 0, totalCases: 0, testCases: [] })
          setSubmitting(false)
          console.error('SSE error:', msg)
        },
      )
    } catch {
      setSubmitting(false)
    }
  }

  if (isLoading || !problem) return <PageSpinner />

  return (
    <div className="flex h-screen overflow-hidden">
      {/* Left panel: problem info */}
      <div className="w-[42%] flex flex-col border-r border-gray-200 dark:border-gray-800 overflow-y-auto">
        {/* Header */}
        <div className="flex items-center gap-3 px-4 py-3 border-b border-gray-100 dark:border-gray-800 shrink-0">
          <button onClick={() => navigate('/practice')} className="text-gray-400 hover:text-gray-600">
            <ChevronLeft className="w-5 h-5" />
          </button>
          <span className="text-sm font-semibold text-gray-900 dark:text-white truncate">
            {problem.problemNumber}. {problem.title}
          </span>
          <Badge className={cn('ml-auto shrink-0', difficultyBg(problem.difficulty))}>{problem.difficulty}</Badge>
        </div>

        {/* Tabs */}
        <div className="flex gap-0 border-b border-gray-100 dark:border-gray-800 shrink-0">
          {(['description', 'submissions'] as Tab[]).map(tb => (
            <button key={tb} onClick={() => setTab(tb)}
              className={`px-4 py-2.5 text-sm font-medium border-b-2 transition-colors ${tab === tb ? 'border-primary-600 text-primary-600' : 'border-transparent text-gray-500 hover:text-gray-700 dark:hover:text-gray-300'}`}>
              {tb === 'description' ? '题目' : t('practice.submissions')}
            </button>
          ))}
        </div>

        <div className="flex-1 overflow-y-auto p-4">
          {tab === 'description' ? (
            <div className="space-y-4">
              <div className="flex gap-2 flex-wrap">
                {problem.category && <Badge>{problem.category}</Badge>}
                {problem.tags?.split(',').map(tag => <Badge key={tag} variant="info">{tag.trim()}</Badge>)}
              </div>
              <div className="prose dark:prose-invert text-sm text-gray-700 dark:text-gray-300 whitespace-pre-wrap leading-relaxed">
                {problem.description || '（暂无描述）'}
              </div>
              {problem.hint && (
                <details className="text-sm">
                  <summary className="cursor-pointer text-primary-600 font-medium">{t('practice.hint')}</summary>
                  <p className="mt-2 text-gray-600 dark:text-gray-400 pl-4">{problem.hint}</p>
                </details>
              )}
            </div>
          ) : (
            <div className="space-y-2">
              {submissions.length === 0 ? (
                <p className="text-sm text-gray-400 text-center py-8">{t('common.noData')}</p>
              ) : (
                submissions.map(s => (
                  <Card key={s.id}>
                    <CardBody className="flex items-center gap-3 py-2.5">
                      <span className={cn('text-sm font-medium', statusColor(s.status))}>
                        {t(`practice.status.${s.status}`) || s.status}
                      </span>
                      <span className="text-xs text-gray-400">{s.language}</span>
                      <span className="text-xs text-gray-400">{s.passedCases}/{s.totalCases}</span>
                      <span className="ml-auto text-xs text-gray-400">{formatDateTime(s.submittedAt)}</span>
                    </CardBody>
                  </Card>
                ))
              )}
            </div>
          )}
        </div>
      </div>

      {/* Right panel: code editor + result */}
      <div className="flex-1 flex flex-col overflow-hidden">
        {/* Toolbar */}
        <div className="flex items-center gap-3 px-4 py-2.5 border-b border-gray-200 dark:border-gray-800 bg-white dark:bg-gray-900 shrink-0">
          <div className="flex rounded-lg border border-gray-200 dark:border-gray-700 overflow-hidden">
            {(['java', 'python'] as Lang[]).map(l => (
              <button key={l} onClick={() => setLang(l)}
                className={`px-3 py-1.5 text-sm font-medium transition-colors ${lang === l ? 'bg-primary-600 text-white' : 'text-gray-600 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-800'}`}>
                {l === 'java' ? 'Java' : 'Python'}
              </button>
            ))}
          </div>
          <div className="ml-auto flex items-center gap-2">
            {progress && (
              <span className="text-xs text-gray-500">
                测试用例 {progress.case}/{progress.total}...
              </span>
            )}
            <Button size="sm" onClick={handleSubmit} loading={submitting}>
              {submitting ? t('practice.submitting') : t('practice.submit')}
            </Button>
          </div>
        </div>

        {/* Code editor */}
        <div className="flex-1 overflow-hidden relative">
          <Editor
            language={lang === 'java' ? 'java' : 'python'}
            theme={isDark ? 'vs-dark' : 'vs'}
            value={code}
            onChange={(val) => setCode(val || '')}
            options={{
              minimap: { enabled: false },
              fontSize: 14,
              scrollBeyondLastLine: false,
              wordWrap: 'on',
              tabSize: 4,
              automaticLayout: true,
            }}
          />
        </div>

        {/* Result panel */}
        {submitResult && (
          <div className="shrink-0 border-t border-gray-200 dark:border-gray-800 bg-white dark:bg-gray-900 max-h-64 overflow-y-auto">
            <div className="p-3 space-y-2">
              <div className="flex items-center gap-2">
                {submitResult.status === 'ACCEPTED'
                  ? <CheckCircle className="w-5 h-5 text-green-500" />
                  : <XCircle className="w-5 h-5 text-red-500" />
                }
                <span className={cn('font-semibold text-sm', statusColor(submitResult.status))}>
                  {t(`practice.status.${submitResult.status}`) || submitResult.status}
                </span>
                <span className="text-xs text-gray-400 ml-auto">
                  {submitResult.passedCases}/{submitResult.totalCases} 通过
                </span>
              </div>
              {submitResult.testCases.map((tc: TestCaseResult) => (
                <div key={tc.index} className={cn('rounded-lg p-2.5 text-xs font-mono', tc.passed ? 'bg-green-50 dark:bg-green-900/10' : 'bg-red-50 dark:bg-red-900/10')}>
                  <div className="flex items-center gap-1.5 mb-1">
                    {tc.passed ? <CheckCircle className="w-3.5 h-3.5 text-green-500" /> : <XCircle className="w-3.5 h-3.5 text-red-500" />}
                    <span>Case {tc.index}</span>
                  </div>
                  {!tc.passed && (
                    <>
                      <div className="text-gray-600 dark:text-gray-400">输入: <span className="text-gray-900 dark:text-gray-100">{tc.input}</span></div>
                      <div className="text-gray-600 dark:text-gray-400">期望: <span className="text-green-600 dark:text-green-400">{tc.expectedOutput}</span></div>
                      <div className="text-gray-600 dark:text-gray-400">实际: <span className="text-red-600 dark:text-red-400">{tc.actualOutput}</span></div>
                    </>
                  )}
                </div>
              ))}
            </div>
          </div>
        )}
      </div>
    </div>
  )
}
