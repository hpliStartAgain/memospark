import { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { ArrowRight } from 'lucide-react'
import { authApi } from '@/lib/api'
import { useAppStore } from '@/store/appStore'
import Button from '@/components/ui/Button'
import Input from '@/components/ui/Input'

export default function AccessPanel() {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const qc = useQueryClient()
  const { setUser } = useAppStore()
  const [mode, setMode] = useState<'login' | 'register'>('login')
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')

  const login = useMutation({
    mutationFn: () => authApi.login(username.trim(), password),
    onSuccess: user => {
      setUser(user)
      qc.invalidateQueries({ queryKey: ['me'] })
      navigate('/')
    },
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    onError: (cause: any) => setError(
      cause.response?.data?.error || cause.response?.data?.message || t('common.error'),
    ),
  })

  const register = useMutation({
    mutationFn: () => authApi.register(username.trim(), password),
    onSuccess: () => login.mutate(),
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    onError: (cause: any) => setError(
      cause.response?.data?.error || cause.response?.data?.message || t('common.error'),
    ),
  })

  const submit = (event: React.FormEvent) => {
    event.preventDefault()
    setError('')
    if (!username.trim() || !password) return
    if (mode === 'login') login.mutate()
    else register.mutate()
  }

  const loading = login.isPending || register.isPending

  return (
    <div className="border border-gray-300 bg-white p-6 md:p-8 shadow-[0_18px_60px_rgba(15,23,42,0.10)] rounded-lg">
      <div className="mb-6 flex items-center justify-between gap-4">
        <div>
          <p className="text-xs font-semibold uppercase text-emerald-700">Workspace access</p>
          <h3 className="mt-1 text-xl font-semibold text-gray-950">
            {mode === 'login' ? '继续今天的计划' : '建立你的学习系统'}
          </h3>
        </div>
        <div className="inline-flex border border-gray-200 rounded-md p-1">
          <button
            type="button"
            onClick={() => { setMode('login'); setError('') }}
            className={`px-3 py-1.5 text-sm rounded ${mode === 'login' ? 'bg-gray-950 text-white' : 'text-gray-500'}`}
          >
            登录
          </button>
          <button
            type="button"
            onClick={() => { setMode('register'); setError('') }}
            className={`px-3 py-1.5 text-sm rounded ${mode === 'register' ? 'bg-gray-950 text-white' : 'text-gray-500'}`}
          >
            注册
          </button>
        </div>
      </div>

      <form onSubmit={submit} className="space-y-4">
        <Input
          label={t('auth.username')}
          value={username}
          onChange={event => setUsername(event.target.value)}
          autoComplete="username"
          autoFocus
          required
        />
        <Input
          label={t('auth.password')}
          type="password"
          value={password}
          onChange={event => setPassword(event.target.value)}
          autoComplete={mode === 'login' ? 'current-password' : 'new-password'}
          required
        />
        {error && (
          <p role="alert" className="border-l-2 border-red-500 bg-red-50 px-3 py-2 text-sm text-red-700">
            {error}
          </p>
        )}
        <Button type="submit" className="w-full" loading={loading}>
          {mode === 'login' ? '进入工作台' : '创建账号'}
          <ArrowRight className="h-4 w-4" />
        </Button>
      </form>

      {mode === 'login' && (
        <button
          type="button"
          onClick={() => navigate('/password-reset')}
          className="mt-4 text-sm text-gray-500 hover:text-gray-950"
        >
          忘记密码
        </button>
      )}
    </div>
  )
}

