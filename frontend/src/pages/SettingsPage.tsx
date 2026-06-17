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
import type { SrsSettings } from '@/types'

export default function SettingsPage() {
  const { t } = useTranslation()
  const { user, theme, toggleTheme, lang, setLang } = useAppStore()
  const qc = useQueryClient()
  const [saved, setSaved] = useState(false)

  const { data: srs, isLoading } = useQuery<SrsSettings>({
    queryKey: ['srs'],
    queryFn: settingsApi.getSrs,
  })

  const [form, setForm] = useState({ initialEaseFactor: 2.5, minEaseFactor: 1.3, firstInterval: 1, secondInterval: 6 })

  useEffect(() => {
    if (srs) setForm(srs)
  }, [srs])

  const saveMut = useMutation({
    mutationFn: () => settingsApi.updateSrs(form),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['srs'] })
      setSaved(true)
      setTimeout(() => setSaved(false), 2000)
    },
  })

  if (isLoading) return <PageSpinner />

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
