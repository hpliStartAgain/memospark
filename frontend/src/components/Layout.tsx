import { useState } from 'react'
import { Outlet, NavLink, useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import {
  Layers, BookOpen, Code2, NotebookPen, BarChart3, Settings, LogOut,
  Sun, Moon, Globe, Zap, Menu, X, LayoutDashboard, Target,
} from 'lucide-react'
import { authApi } from '@/lib/api'
import { useAppStore } from '@/store/appStore'
import { cn } from '@/lib/utils'

export default function Layout() {
  const { t } = useTranslation()
  const { user, setUser, theme, toggleTheme, lang, setLang } = useAppStore()
  const navigate = useNavigate()
  const qc = useQueryClient()
  const [sidebarOpen, setSidebarOpen] = useState(false)

  const logoutMut = useMutation({
    mutationFn: authApi.logout,
    onSuccess: () => {
      setUser(null)
      qc.clear()
      navigate('/login')
    },
  })

  const navItems = [
    { to: '/',         icon: LayoutDashboard, label: t('nav.dashboard'), end: true },
    { to: '/targets',  icon: Target,          label: t('nav.targets') },
    { to: '/decks',    icon: Layers,          label: t('nav.decks') },
    { to: '/review',   icon: BookOpen,        label: t('nav.review') },
    { to: '/practice', icon: Code2,           label: t('nav.practice') },
    { to: '/notebook', icon: NotebookPen,     label: t('nav.notebook') },
    { to: '/stats',    icon: BarChart3,       label: t('nav.stats') },
    { to: '/settings', icon: Settings,        label: t('nav.settings') },
  ]

  const mobileNavItems = navItems.filter(i =>
    ['/', '/targets', '/review', '/practice', '/notebook'].includes(i.to))

  return (
    <div className="flex h-screen overflow-hidden bg-background-light dark:bg-gray-950">
      {/* Mobile top bar */}
      <div className="md:hidden fixed top-0 left-0 right-0 z-30 flex items-center justify-between px-4 py-3 bg-white dark:bg-gray-900 border-b border-gray-200 dark:border-gray-800">
        <div className="flex items-center gap-2">
          <Zap className="w-5 h-5 text-primary-600" />
          <span className="font-bold text-gray-900 dark:text-white">MemoSpark</span>
        </div>
        <button
          onClick={() => setSidebarOpen(true)}
          className="p-1.5 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-800 text-gray-600 dark:text-gray-400"
        >
          <Menu className="w-5 h-5" />
        </button>
      </div>

      {/* Mobile overlay */}
      {sidebarOpen && (
        <div
          className="md:hidden fixed inset-0 z-40 bg-black/50"
          onClick={() => setSidebarOpen(false)}
        />
      )}

      {/* Sidebar */}
      <aside className={cn(
        'fixed md:static z-50 md:z-auto inset-y-0 left-0 w-60 flex flex-col bg-white dark:bg-gray-900 border-r border-gray-200 dark:border-gray-800 shadow-sm shrink-0 transition-transform duration-200',
        sidebarOpen ? 'translate-x-0' : '-translate-x-full md:translate-x-0'
      )}>
        {/* Logo + close (mobile) */}
        <div className="flex items-center justify-between px-5 py-4 border-b border-gray-100 dark:border-gray-800">
          <div className="flex items-center gap-2">
            <Zap className="w-6 h-6 text-primary-600" />
            <span className="text-lg font-bold text-gray-900 dark:text-white">MemoSpark</span>
          </div>
          <button
            onClick={() => setSidebarOpen(false)}
            className="md:hidden p-1 rounded hover:bg-gray-100 dark:hover:bg-gray-800 text-gray-400"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Navigation */}
        <nav className="flex-1 px-3 py-4 space-y-1 overflow-y-auto">
          {navItems.map(({ to, icon: Icon, label, end }) => (
            <NavLink
              key={to}
              to={to}
              end={end}
              onClick={() => setSidebarOpen(false)}
              className={({ isActive }) => cn(
                'flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors',
                isActive
                  ? 'bg-primary-50 dark:bg-primary-900/20 text-primary-700 dark:text-primary-400'
                  : 'text-gray-600 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-800 hover:text-gray-900 dark:hover:text-gray-100',
              )}
            >
              <Icon className="w-5 h-5 shrink-0" />
              {label}
            </NavLink>
          ))}
        </nav>

        {/* Footer */}
        <div className="px-3 py-3 border-t border-gray-100 dark:border-gray-800 space-y-1">
          {/* Theme toggle */}
          <button
            onClick={toggleTheme}
            className="w-full flex items-center gap-3 px-3 py-2 rounded-lg text-sm text-gray-600 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-800"
          >
            {theme === 'light' ? <Moon className="w-4 h-4" /> : <Sun className="w-4 h-4" />}
            {theme === 'light' ? t('settings.dark') : t('settings.light')}
          </button>

          {/* Language toggle */}
          <button
            onClick={() => setLang(lang === 'zh' ? 'en' : 'zh')}
            className="w-full flex items-center gap-3 px-3 py-2 rounded-lg text-sm text-gray-600 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-800"
          >
            <Globe className="w-4 h-4" />
            {lang === 'zh' ? 'English' : '中文'}
          </button>

          {/* User + Logout */}
          <div className="flex items-center justify-between px-3 py-2">
            <span className="text-xs text-gray-500 dark:text-gray-500 truncate">{user?.username}</span>
            <button
              onClick={() => logoutMut.mutate()}
              title={t('nav.logout')}
              className="p-1 rounded hover:bg-gray-100 dark:hover:bg-gray-800 text-gray-400 hover:text-red-500"
            >
              <LogOut className="w-4 h-4" />
            </button>
          </div>
        </div>
      </aside>

      {/* Main content */}
      <main className="flex-1 overflow-y-auto pt-14 md:pt-0 pb-16 md:pb-0">
        <Outlet />
      </main>

      {/* Mobile bottom tab bar */}
      <nav className="md:hidden fixed bottom-0 left-0 right-0 z-30 flex items-center justify-around bg-white dark:bg-gray-900 border-t border-gray-200 dark:border-gray-800">
        {mobileNavItems.map(({ to, icon: Icon, label, end }) => (
          <NavLink
            key={to}
            to={to}
            end={end}
            className={({ isActive }) => cn(
              'flex flex-col items-center gap-0.5 py-2 px-2 flex-1 text-[10px] font-medium transition-colors',
              isActive ? 'text-primary-600' : 'text-gray-400',
            )}
          >
            <Icon className="w-5 h-5" />
            {label}
          </NavLink>
        ))}
      </nav>
    </div>
  )
}
