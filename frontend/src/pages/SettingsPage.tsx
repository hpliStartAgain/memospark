import { useState, useEffect } from 'react'
import { useTranslation } from 'react-i18next'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { settingsApi } from '@/lib/api'
import Card, { CardBody, CardHeader } from '@/components/ui/Card'
import Button from '@/components/ui/Button'
import Input from '@/components/ui/Input'
import { PageSpinner } from '@/components/ui/Spinner'
import { useAppStore } from '@/store/appStore'
import { CheckCircle } from 'lucide-react'
import type { AiSettings, SrsSettings } from '@/types'

export default function SettingsPage() {
  const { t } = useTranslation()
  const { user, theme, toggleTheme, lang, setLang } = useAppStore()
  const qc = useQueryClient()
  const [saved, setSaved] = useState(false)

  const { data: srs, isLoading } = useQuery<SrsSettings>({
    queryKey: ['srs'],
    queryFn: settingsApi.getSrs,
  })
  const { data: ai, isLoading: aiLoading } = useQuery<AiSettings>({
    queryKey: ['ai-settings'],
    queryFn: settingsApi.getAi,
  })

  const [form, setForm] = useState({
    initialEaseFactor: 2.5,
    minEaseFactor: 1.3,
    firstInterval: 1,
    secondInterval: 6,
    desiredRetention: 0.9,
  })
  const [aiForm, setAiForm] = useState({
    provider: 'SenseNova',
    baseUrl: 'https://token.sensenova.cn/v1',
    model: 'deepseek-v4-flash',
    apiKey: '',
    clearApiKey: false,
  })
  const [aiSaved, setAiSaved] = useState(false)
  const [aiTestResult, setAiTestResult] = useState<string | null>(null)

  useEffect(() => {
    if (srs) setForm(srs)
  }, [srs])

  useEffect(() => {
    if (ai) {
      setAiForm({
        provider: ai.provider,
        baseUrl: ai.baseUrl,
        model: ai.model,
        apiKey: '',
        clearApiKey: false,
      })
    }
  }, [ai])

  const saveMut = useMutation({
    mutationFn: () => settingsApi.updateSrs(form),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['srs'] })
      setSaved(true)
      setTimeout(() => setSaved(false), 2000)
    },
  })
  const saveAiMut = useMutation({
    mutationFn: () => settingsApi.updateAi(aiForm),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['ai-settings'] })
      setAiForm(f => ({ ...f, apiKey: '', clearApiKey: false }))
      setAiSaved(true)
      setAiTestResult(null)
      setTimeout(() => setAiSaved(false), 2000)
    },
  })
  const testAiMut = useMutation({
    mutationFn: settingsApi.testAi,
    onSuccess: (result: { ok: boolean; response: string }) => {
      setAiTestResult(result.ok ? `连接成功：${result.response || 'OK'}` : '连接失败')
    },
    onError: () => setAiTestResult('连接失败，请检查 URL、模型与 API Key'),
  })

  if (isLoading || aiLoading) return <PageSpinner />

  return (
    <div className="p-6 space-y-6 max-w-lg">
      <h1 className="text-2xl font-bold text-gray-900 dark:text-white">{t('settings.title')}</h1>

      {/* Account */}
      <Card>
        <CardHeader><h2 className="font-semibold">{t('settings.account')}</h2></CardHeader>
        <CardBody className="space-y-2">
          <p className="text-sm text-gray-700 dark:text-gray-300">
            <span className="text-gray-400 mr-2">{t('auth.username')}:</span>
            <span className="font-medium">{user?.username}</span>
          </p>
          <p className="text-sm text-gray-700 dark:text-gray-300">
            <span className="text-gray-400 mr-2">Role:</span>
            <span className="font-medium">{user?.role}</span>
          </p>
        </CardBody>
      </Card>

      {/* Appearance */}
      <Card>
        <CardHeader><h2 className="font-semibold">{t('settings.theme')} / {t('settings.language')}</h2></CardHeader>
        <CardBody className="flex gap-3">
          <Button variant="secondary" onClick={toggleTheme} size="sm">
            {theme === 'light' ? t('settings.dark') : t('settings.light')}
          </Button>
          <Button variant="secondary" onClick={() => setLang(lang === 'zh' ? 'en' : 'zh')} size="sm">
            {lang === 'zh' ? 'English' : '中文'}
          </Button>
        </CardBody>
      </Card>

      {/* AI Provider */ }
      <Card>
        <CardHeader><h2 className="font-semibold">AI 供应商</h2></CardHeader>
        <CardBody className="space-y-4">
          <div className="flex flex-col gap-1">
            <label className="text-sm font-medium text-gray-700 dark:text-gray-300">供应商</label>
            <select
              value={aiForm.provider}
              onChange={e => {
                const provider = e.target.value
                setAiForm(f => provider === 'SenseNova'
                  ? { ...f, provider, baseUrl: 'https://token.sensenova.cn/v1', model: 'deepseek-v4-flash' }
                  : { ...f, provider })
              }}
              className="w-full px-3 py-2 text-sm rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-800 text-gray-900 dark:text-gray-100 focus:outline-none focus:ring-2 focus:ring-primary-500"
            >
              <option value="SenseNova">SenseNova</option>
              <option value="Custom">OpenAI-compatible</option>
            </select>
          </div>
          <Input
            label="Base URL"
            value={aiForm.baseUrl}
            placeholder="https://token.sensenova.cn/v1"
            onChange={e => setAiForm(f => ({ ...f, baseUrl: e.target.value }))}
          />
          <Input
            label="模型"
            value={aiForm.model}
            placeholder="deepseek-v4-flash"
            onChange={e => setAiForm(f => ({ ...f, model: e.target.value }))}
          />
          <Input
            label={`API Key${ai?.apiKeyConfigured ? `（当前 ${ai.apiKeyMasked}）` : ''}`}
            type="password"
            autoComplete="new-password"
            value={aiForm.apiKey}
            placeholder={ai?.apiKeyConfigured ? '留空则保留当前 Key' : '输入 API Key'}
            onChange={e => setAiForm(f => ({ ...f, apiKey: e.target.value, clearApiKey: false }))}
          />
          {ai?.apiKeyConfigured && (
            <label className="flex items-center gap-2 text-sm text-gray-600 dark:text-gray-300">
              <input
                type="checkbox"
                checked={aiForm.clearApiKey}
                onChange={e => setAiForm(f => ({ ...f, clearApiKey: e.target.checked, apiKey: '' }))}
                className="h-4 w-4 rounded border-gray-300 text-primary-600 focus:ring-primary-500"
              />
              清除已保存的 API Key，改用服务器环境变量
            </label>
          )}
          <div className="flex gap-3">
            <Button
              variant="secondary"
              className="flex-1"
              onClick={() => testAiMut.mutate()}
              loading={testAiMut.isPending}
              disabled={!ai?.apiKeyConfigured}
            >
              {ai?.apiKeyConfigured ? '测试连接' : '保存 Key 后测试'}
            </Button>
            <Button
              className="flex-1"
              onClick={() => saveAiMut.mutate()}
              loading={saveAiMut.isPending}
              disabled={!aiForm.baseUrl.trim() || !aiForm.model.trim()}
            >
              {aiSaved ? <span className="flex items-center gap-1"><CheckCircle className="w-4 h-4" />已保存</span> : '保存 AI 配置'}
            </Button>
          </div>
          {aiTestResult && (
            <p className={`text-sm ${aiTestResult.startsWith('连接成功') ? 'text-green-600' : 'text-red-500'}`}>
              {aiTestResult}
            </p>
          )}
          {saveAiMut.isError && <p className="text-sm text-red-500">保存失败，请检查配置格式。</p>}
        </CardBody>
      </Card>

      {/* SRS Settings */}
      <Card>
        <CardHeader><h2 className="font-semibold">{t('settings.srs')}</h2></CardHeader>
        <CardBody className="space-y-4">
          <Input
            label={t('settings.initialEase')}
            type="number" step="0.1" min="1.3" max="4.0"
            value={form.initialEaseFactor}
            onChange={e => setForm(f => ({ ...f, initialEaseFactor: +e.target.value }))}
          />
          <Input
            label={t('settings.minEase')}
            type="number" step="0.1" min="1.0" max="3.0"
            value={form.minEaseFactor}
            onChange={e => setForm(f => ({ ...f, minEaseFactor: +e.target.value }))}
          />
          <Input
            label={t('settings.firstInterval')}
            type="number" min="1"
            value={form.firstInterval}
            onChange={e => setForm(f => ({ ...f, firstInterval: +e.target.value }))}
          />
          <Input
            label={t('settings.secondInterval')}
            type="number" min="1"
            value={form.secondInterval}
            onChange={e => setForm(f => ({ ...f, secondInterval: +e.target.value }))}
          />
          <Input
            label={t('settings.desiredRetention')}
            type="number" step="0.01" min="0.7" max="0.98"
            value={form.desiredRetention}
            onChange={e => setForm(f => ({ ...f, desiredRetention: +e.target.value }))}
          />
          <Button onClick={() => saveMut.mutate()} loading={saveMut.isPending} className="w-full">
            {saved ? (
              <span className="flex items-center gap-1"><CheckCircle className="w-4 h-4" />{t('settings.saved')}</span>
            ) : t('settings.save')}
          </Button>
        </CardBody>
      </Card>
    </div>
  )
}
