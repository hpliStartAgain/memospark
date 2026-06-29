import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import {
  Activity, Cpu, Database, HardDrive, Users, TrendingUp, Shield,
  Ban, KeyRound, Crown, UserCog, Clock, RefreshCw,
} from 'lucide-react'
import {
  AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer,
} from 'recharts'
import { adminApi } from '@/lib/api'
import { useAppStore } from '@/store/appStore'
import { useToastStore } from '@/store/toastStore'
import { PageSpinner } from '@/components/ui/Spinner'
import Button from '@/components/ui/Button'
import Card, { CardBody, CardHeader } from '@/components/ui/Card'
import type { AdminSystemInfo, AdminStats, AdminDauPoint, AdminUser } from '@/types'

function formatUptime(seconds: number): string {
  const d = Math.floor(seconds / 86400)
  const h = Math.floor((seconds % 86400) / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  if (d > 0) return `${d}天 ${h}小时 ${m}分`
  if (h > 0) return `${h}小时 ${m}分`
  return `${m}分`
}

function formatDate(iso?: string): string {
  if (!iso) return '—'
  return new Date(iso).toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

function StatCard({ icon: Icon, label, value, unit, color }: {
  icon: React.ElementType; label: string; value: string | number; unit?: string; color: string
}) {
  return (
    <div className="flex items-center gap-3 p-4 rounded-xl border border-gray-200 dark:border-gray-800 bg-white dark:bg-gray-900">
      <div className={`p-2.5 rounded-xl ${color}`}>
        <Icon className="w-5 h-5 text-white" />
      </div>
      <div className="min-w-0">
        <p className="text-2xl font-bold text-gray-900 dark:text-white">
          {value}{unit && <span className="text-sm font-normal text-gray-400 ml-1">{unit}</span>}
        </p>
        <p className="text-xs text-gray-500 dark:text-gray-400">{label}</p>
      </div>
    </div>
  )
}

export default function AdminPage() {
  const { user } = useAppStore()
  const toast = useToastStore()
  const qc = useQueryClient()
  const [resetTarget, setResetTarget] = useState<AdminUser | null>(null)
  const [newPassword, setNewPassword] = useState('')

  const { data: sys, isLoading: sysLoading } = useQuery<AdminSystemInfo>({ queryKey: ['admin', 'system'], queryFn: adminApi.system })
  const { data: stats, isLoading: statsLoading } = useQuery<AdminStats>({ queryKey: ['admin', 'stats'], queryFn: adminApi.stats })
  const { data: dau = [], isLoading: dauLoading } = useQuery<AdminDauPoint[]>({ queryKey: ['admin', 'dau'], queryFn: () => adminApi.dau(30) })
  const { data: userList, isLoading: usersLoading } = useQuery<{ users: AdminUser[]; total: number }>({ queryKey: ['admin', 'users'], queryFn: adminApi.users })

  const setEnabledMut = useMutation({
    mutationFn: ({ userId, enabled }: { userId: number; enabled: boolean }) =>
      adminApi.setEnabled(userId, enabled),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['admin', 'users'] })
      qc.invalidateQueries({ queryKey: ['admin', 'stats'] })
      toast.success('用户状态已更新')
    },
  })

  const setRoleMut = useMutation({
    mutationFn: ({ userId, role }: { userId: number; role: string }) =>
      adminApi.setRole(userId, role),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['admin', 'users'] })
      qc.invalidateQueries({ queryKey: ['admin', 'stats'] })
      toast.success('用户角色已更新')
    },
  })

  const resetPwdMut = useMutation({
    mutationFn: ({ userId, password }: { userId: number; password: string }) =>
      adminApi.resetPassword(userId, password),
    onSuccess: () => {
      toast.success('密码已重置')
      setResetTarget(null)
      setNewPassword('')
    },
  })

  if (user?.role !== 'ADMIN') {
    return (
      <div className="mx-auto max-w-3xl p-10 text-center">
        <Shield className="h-12 w-12 mx-auto text-gray-300" />
        <h1 className="mt-4 text-2xl font-semibold">需要管理员权限</h1>
        <p className="mt-2 text-gray-500">此页面仅对管理员开放。</p>
      </div>
    )
  }

  if (sysLoading || statsLoading) return <PageSpinner />

  return (
    <div className="mx-auto max-w-7xl space-y-6 p-5 md:p-8">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <p className="text-sm font-medium text-emerald-700 dark:text-emerald-400">后台管理</p>
          <h1 className="mt-1 text-3xl font-semibold text-gray-950 dark:text-white">Admin Panel</h1>
        </div>
        <Button variant="ghost" size="sm" onClick={() => {
          qc.invalidateQueries({ queryKey: ['admin'] })
          toast.info('已刷新数据')
        }}>
          <RefreshCw className="h-4 w-4" /> 刷新
        </Button>
      </div>

      {/* System Info */}
      {sys && (
        <Card>
          <CardHeader>
            <h2 className="text-base font-semibold text-gray-900 dark:text-white flex items-center gap-2">
              <Cpu className="w-5 h-5" /> 实例信息
            </h2>
          </CardHeader>
          <CardBody>
            <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
              <div>
                <p className="text-xs text-gray-400">应用</p>
                <p className="font-medium text-gray-900 dark:text-white">{sys.appName} {sys.appVersion}</p>
              </div>
              <div>
                <p className="text-xs text-gray-400">Spring Boot</p>
                <p className="font-medium text-gray-900 dark:text-white">{sys.springBootVersion}</p>
              </div>
              <div>
                <p className="text-xs text-gray-400">Java</p>
                <p className="font-medium text-gray-900 dark:text-white">{sys.javaVersion}</p>
              </div>
              <div>
                <p className="text-xs text-gray-400">操作系统</p>
                <p className="font-medium text-gray-900 dark:text-white">{sys.osName} {sys.osArch}</p>
              </div>
              <div>
                <p className="text-xs text-gray-400 flex items-center gap-1"><Clock className="w-3 h-3" />启动时间</p>
                <p className="font-medium text-gray-900 dark:text-white">{formatDate(sys.startupTime)}</p>
              </div>
              <div>
                <p className="text-xs text-gray-400">运行时长</p>
                <p className="font-medium text-gray-900 dark:text-white">{formatUptime(sys.uptimeSeconds)}</p>
              </div>
              <div>
                <p className="text-xs text-gray-400 flex items-center gap-1"><HardDrive className="w-3 h-3" />堆内存</p>
                <p className="font-medium text-gray-900 dark:text-white">{sys.heapUsedMb} / {sys.heapMaxMb} MB</p>
              </div>
              <div>
                <p className="text-xs text-gray-400 flex items-center gap-1"><Activity className="w-3 h-3" />线程 / CPU</p>
                <p className="font-medium text-gray-900 dark:text-white">{sys.threadCount} 线程 · {sys.availableProcessors} 核</p>
              </div>
            </div>
          </CardBody>
        </Card>
      )}

      {/* Stats Grid */}
      {stats && (
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          <StatCard icon={Users} label="总用户数" value={stats.totalUsers} color="bg-blue-500" />
          <StatCard icon={TrendingUp} label="今日活跃 (DAU)" value={stats.todayActiveUsers} color="bg-emerald-500" />
          <StatCard icon={UserCog} label="今日新增" value={stats.todayNewUsers} color="bg-purple-500" />
          <StatCard icon={Shield} label="管理员" value={stats.adminCount} color="bg-orange-500" />
          <StatCard icon={Database} label="总牌组" value={stats.totalDecks} color="bg-cyan-500" />
          <StatCard icon={Database} label="总卡片" value={stats.totalCards} color="bg-teal-500" />
          <StatCard icon={Activity} label="总复习次数" value={stats.totalReviews} color="bg-pink-500" />
          <StatCard icon={Activity} label="今日复习" value={stats.todayReviews} color="bg-indigo-500" />
        </div>
      )}

      {/* DAU Chart */}
      {!dauLoading && dau.length > 0 && (
        <Card>
          <CardHeader>
            <h2 className="text-base font-semibold text-gray-900 dark:text-white flex items-center gap-2">
              <TrendingUp className="w-5 h-5" /> 近 30 天 DAU 趋势
            </h2>
          </CardHeader>
          <CardBody>
            <ResponsiveContainer width="100%" height={240}>
              <AreaChart data={dau}>
                <defs>
                  <linearGradient id="dauGradient" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#10b981" stopOpacity={0.3} />
                    <stop offset="95%" stopColor="#10b981" stopOpacity={0} />
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" className="dark:opacity-20" />
                <XAxis dataKey="date" tick={{ fontSize: 11 }} tickFormatter={d => d.slice(5)} />
                <YAxis tick={{ fontSize: 11 }} allowDecimals={false} />
                <Tooltip
                  contentStyle={{ borderRadius: '8px', border: '1px solid #e5e7eb', fontSize: '12px' }}
                  labelFormatter={d => `日期: ${d}`}
                />
                <Area type="monotone" dataKey="activeUsers" name="活跃用户" stroke="#10b981" fill="url(#dauGradient)" strokeWidth={2} />
              </AreaChart>
            </ResponsiveContainer>
          </CardBody>
        </Card>
      )}

      {/* User Management */}
      {!usersLoading && userList && (
        <Card>
          <CardHeader>
            <h2 className="text-base font-semibold text-gray-900 dark:text-white flex items-center gap-2">
              <Users className="w-5 h-5" /> 用户管理 ({userList.total})
            </h2>
          </CardHeader>
          <CardBody className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-gray-200 dark:border-gray-800 text-xs text-gray-400">
                  <th className="text-left py-2 px-3 font-medium">用户</th>
                  <th className="text-left py-2 px-3 font-medium">角色</th>
                  <th className="text-left py-2 px-3 font-medium">状态</th>
                  <th className="text-right py-2 px-3 font-medium">牌组</th>
                  <th className="text-right py-2 px-3 font-medium">卡片</th>
                  <th className="text-right py-2 px-3 font-medium">复习</th>
                  <th className="text-left py-2 px-3 font-medium">注册时间</th>
                  <th className="text-left py-2 px-3 font-medium">最后活跃</th>
                  <th className="text-right py-2 px-3 font-medium">操作</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100 dark:divide-gray-800">
                {userList.users.map(u => (
                  <tr key={u.id} className="hover:bg-gray-50 dark:hover:bg-gray-800/50">
                    <td className="py-2.5 px-3 font-medium text-gray-900 dark:text-white">{u.username}</td>
                    <td className="py-2.5 px-3">
                      <span className={`inline-flex items-center gap-1 px-2 py-0.5 rounded text-xs font-medium ${
                        u.role === 'ADMIN'
                          ? 'bg-orange-100 text-orange-700 dark:bg-orange-900/30 dark:text-orange-300'
                          : 'bg-gray-100 text-gray-600 dark:bg-gray-800 dark:text-gray-400'
                      }`}>
                        {u.role === 'ADMIN' && <Crown className="w-3 h-3" />}
                        {u.role}
                      </span>
                    </td>
                    <td className="py-2.5 px-3">
                      <span className={`inline-flex px-2 py-0.5 rounded text-xs font-medium ${
                        u.enabled
                          ? 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-300'
                          : 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-300'
                      }`}>
                        {u.enabled ? '正常' : '已封禁'}
                      </span>
                    </td>
                    <td className="py-2.5 px-3 text-right text-gray-600 dark:text-gray-400">{u.deckCount}</td>
                    <td className="py-2.5 px-3 text-right text-gray-600 dark:text-gray-400">{u.cardCount}</td>
                    <td className="py-2.5 px-3 text-right text-gray-600 dark:text-gray-400">{u.reviewCount}</td>
                    <td className="py-2.5 px-3 text-xs text-gray-500">{formatDate(u.createdAt)}</td>
                    <td className="py-2.5 px-3 text-xs text-gray-500">{formatDate(u.lastActiveAt)}</td>
                    <td className="py-2.5 px-3">
                      <div className="flex items-center justify-end gap-1">
                        {u.username !== user?.username && (
                          <>
                            <button
                              title={u.enabled ? '封禁' : '解封'}
                              disabled={setEnabledMut.isPending}
                              onClick={() => setEnabledMut.mutate({ userId: u.id, enabled: !u.enabled })}
                              className="p-1.5 rounded hover:bg-gray-200 dark:hover:bg-gray-700 text-gray-500 hover:text-red-500"
                            >
                              <Ban className="w-4 h-4" />
                            </button>
                            <button
                              title={u.role === 'ADMIN' ? '降为普通用户' : '升为管理员'}
                              disabled={setRoleMut.isPending}
                              onClick={() => setRoleMut.mutate({ userId: u.id, role: u.role === 'ADMIN' ? 'USER' : 'ADMIN' })}
                              className="p-1.5 rounded hover:bg-gray-200 dark:hover:bg-gray-700 text-gray-500 hover:text-orange-500"
                            >
                              <Crown className="w-4 h-4" />
                            </button>
                            <button
                              title="重置密码"
                              disabled={resetPwdMut.isPending}
                              onClick={() => { setResetTarget(u); setNewPassword('') }}
                              className="p-1.5 rounded hover:bg-gray-200 dark:hover:bg-gray-700 text-gray-500 hover:text-blue-500"
                            >
                              <KeyRound className="w-4 h-4" />
                            </button>
                          </>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </CardBody>
        </Card>
      )}

      {/* Reset Password Modal */}
      {resetTarget && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50" onClick={() => setResetTarget(null)}>
          <div className="bg-white dark:bg-gray-900 rounded-xl p-6 max-w-sm w-full mx-4 shadow-xl" onClick={e => e.stopPropagation()}>
            <h3 className="text-lg font-semibold text-gray-900 dark:text-white">重置密码</h3>
            <p className="mt-1 text-sm text-gray-500">为用户 <span className="font-medium">{resetTarget.username}</span> 设置新密码</p>
            <input
              type="text"
              value={newPassword}
              onChange={e => setNewPassword(e.target.value)}
              placeholder="输入新密码（至少 3 位）"
              className="mt-4 w-full px-3 py-2 rounded-lg border border-gray-300 dark:border-gray-700 bg-white dark:bg-gray-800 text-sm text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-primary-500"
            />
            <div className="mt-5 flex justify-end gap-2">
              <Button variant="ghost" size="sm" onClick={() => setResetTarget(null)}>取消</Button>
              <Button
                size="sm"
                disabled={newPassword.length < 3 || resetPwdMut.isPending}
                onClick={() => resetPwdMut.mutate({ userId: resetTarget.id, password: newPassword })}
              >
                确认重置
              </Button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
