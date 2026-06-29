import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useMutation } from '@tanstack/react-query'
import { ChevronLeft } from 'lucide-react'
import { authApi } from '@/lib/api'
import Button from '@/components/ui/Button'
import Input from '@/components/ui/Input'

export default function PasswordResetPage() {
  const navigate = useNavigate()
  const [step, setStep] = useState<'request' | 'confirm' | 'done'>('request')
  const [username, setUsername] = useState('')
  const [token, setToken] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')

  const requestMut = useMutation({
    mutationFn: () => authApi.requestPasswordReset(username),
    onSuccess: (data) => {
      setMessage(data.message)
      if (data.token) {
        setToken(data.token)
      }
      setStep('confirm')
    },
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    onError: (e: any) => {
      setError(e.response?.data?.error || e.response?.data?.message || 'Request failed')
    },
  })

  const confirmMut = useMutation({
    mutationFn: () => authApi.confirmPasswordReset(token, newPassword),
    onSuccess: () => {
      setStep('done')
    },
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    onError: (e: any) => {
      setError(e.response?.data?.error || e.response?.data?.message || 'Reset failed')
    },
  })

  const handleRequest = (ev: React.FormEvent) => {
    ev.preventDefault()
    setError('')
    if (!username.trim()) return
    requestMut.mutate()
  }

  const handleConfirm = (ev: React.FormEvent) => {
    ev.preventDefault()
    setError('')
    if (!token.trim() || newPassword.length < 6) return
    confirmMut.mutate()
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-background-light dark:from-gray-950 dark:to-gray-900 p-4">
      <div className="w-full max-w-sm">
        <div className="text-center mb-8">
          <img src="/icon-192.png" alt="" className="mx-auto mb-4 h-14 w-14 rounded-2xl shadow-lg" />
          <h1 className="text-2xl font-bold text-gray-900 dark:text-white">MemoSpark</h1>
        </div>

        <div className="bg-white dark:bg-gray-900 rounded-2xl shadow-xl border border-gray-200 dark:border-gray-800 p-8">
          {step === 'request' && (
            <>
              <h2 className="text-xl font-semibold text-gray-900 dark:text-white mb-6">重置密码</h2>
              <form onSubmit={handleRequest} className="space-y-4">
                <Input
                  label="用户名"
                  value={username}
                  onChange={e => setUsername(e.target.value)}
                  placeholder="输入你的用户名"
                  autoFocus
                  required
                />
                {error && (
                  <p className="text-sm text-red-600 dark:text-red-400 bg-red-50 dark:bg-red-900/20 px-3 py-2 rounded-lg">
                    {error}
                  </p>
                )}
                <Button type="submit" className="w-full" loading={requestMut.isPending}>
                  获取重置令牌
                </Button>
              </form>
            </>
          )}

          {step === 'confirm' && (
            <>
              <h2 className="text-xl font-semibold text-gray-900 dark:text-white mb-4">设置新密码</h2>
              {message && (
                <p className="text-sm text-green-600 dark:text-green-400 bg-green-50 dark:bg-green-900/20 px-3 py-2 rounded-lg mb-4">
                  {message}
                </p>
              )}
              <form onSubmit={handleConfirm} className="space-y-4">
                <Input
                  label="重置令牌"
                  value={token}
                  onChange={e => setToken(e.target.value)}
                  placeholder="rst_..."
                  required
                />
                <Input
                  label="新密码"
                  type="password"
                  value={newPassword}
                  onChange={e => setNewPassword(e.target.value)}
                  placeholder="至少6位"
                  required
                />
                {error && (
                  <p className="text-sm text-red-600 dark:text-red-400 bg-red-50 dark:bg-red-900/20 px-3 py-2 rounded-lg">
                    {error}
                  </p>
                )}
                <Button type="submit" className="w-full" loading={confirmMut.isPending}>
                  确认重置
                </Button>
              </form>
            </>
          )}

          {step === 'done' && (
            <div className="text-center space-y-4">
              <h2 className="text-xl font-semibold text-gray-900 dark:text-white">密码重置成功</h2>
              <p className="text-sm text-gray-500 dark:text-gray-400">
                请使用新密码登录。
              </p>
              <Button className="w-full" onClick={() => navigate('/login')}>
                返回登录
              </Button>
            </div>
          )}

          {step !== 'done' && (
            <button
              type="button"
              onClick={() => navigate('/login')}
              className="mt-4 w-full flex items-center justify-center gap-1 text-sm text-primary-600 dark:text-primary-400 hover:underline text-center"
            >
              <ChevronLeft className="w-4 h-4" />
              返回登录
            </button>
          )}
        </div>
      </div>
    </div>
  )
}
