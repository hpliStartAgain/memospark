import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { Zap } from 'lucide-react'
import { authApi } from '@/lib/api'
import { useAppStore } from '@/store/appStore'
import Button from '@/components/ui/Button'
import Input from '@/components/ui/Input'

export default function LoginPage() {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const { setUser } = useAppStore()
  const qc = useQueryClient()
  const [mode, setMode] = useState<'login' | 'register'>('login')
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')

  const loginMut = useMutation({
    mutationFn: () => authApi.login(username, password),
    onSuccess: (user) => {
      setUser(user)
      qc.invalidateQueries({ queryKey: ['me'] })
      navigate('/decks')
    },
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    onError: (e: any) => {
      setError(e.response?.data?.error || e.response?.data?.message || t('common.error'))
    },
  })

  const registerMut = useMutation({
    mutationFn: () => authApi.register(username, password),
    onSuccess: () => {
      loginMut.mutate()
    },
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    onError: (e: any) => {
      setError(e.response?.data?.error || e.response?.data?.message || t('common.error'))
    },
  })

  const handleSubmit = (ev: React.FormEvent) => {
    ev.preventDefault()
    setError('')
    if (!username.trim() || !password) return
    if (mode === 'login') loginMut.mutate()
    else registerMut.mutate()
  }

  const loading = loginMut.isPending || registerMut.isPending

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-primary-50 to-blue-100 dark:from-gray-950 dark:to-gray-900 p-4">
      <div className="w-full max-w-sm">
        {/* Logo */}
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center w-14 h-14 rounded-2xl bg-primary-600 mb-4">
            <Zap className="w-8 h-8 text-white" />
          </div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-white">MemoSpark</h1>
          <p className="text-sm text-gray-500 dark:text-gray-400 mt-1">面试记忆引擎</p>
        </div>

        {/* Card */}
        <div className="bg-white dark:bg-gray-900 rounded-2xl shadow-xl border border-gray-200 dark:border-gray-800 p-8">
          <h2 className="text-xl font-semibold text-gray-900 dark:text-white mb-6">
            {mode === 'login' ? t('auth.login') : t('auth.register')}
          </h2>

          <form onSubmit={handleSubmit} className="space-y-4">
            <Input
              label={t('auth.username')}
              value={username}
              onChange={e => setUsername(e.target.value)}
              placeholder="admin"
              autoFocus
              autoComplete="username"
              required
            />
            <Input
              label={t('auth.password')}
              type="password"
              value={password}
              onChange={e => setPassword(e.target.value)}
              placeholder="••••••••"
              autoComplete={mode === 'login' ? 'current-password' : 'new-password'}
              required
            />

            {error && (
              <p className="text-sm text-red-600 dark:text-red-400 bg-red-50 dark:bg-red-900/20 px-3 py-2 rounded-lg">
                {error}
              </p>
            )}

            <Button type="submit" className="w-full" loading={loading}>
              {loading
                ? (mode === 'login' ? t('auth.loggingIn') : t('auth.registering'))
                : (mode === 'login' ? t('auth.loginBtn') : t('auth.registerBtn'))
              }
            </Button>
          </form>

          <button
            type="button"
            onClick={() => { setMode(m => m === 'login' ? 'register' : 'login'); setError('') }}
            className="mt-4 w-full text-sm text-primary-600 dark:text-primary-400 hover:underline text-center"
          >
            {mode === 'login' ? t('auth.switchToRegister') : t('auth.switchToLogin')}
          </button>
        </div>
      </div>
    </div>
  )
}
