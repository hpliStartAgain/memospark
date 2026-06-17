import { Outlet, NavLink, useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import {
  Layers, BookOpen, Code2, NotebookPen, BarChart3, Settings, LogOut,
  Sun, Moon, Globe, Zap,
} from 'lucide-react'
import { authApi } from '@/lib/api'
import { useAppStore } from '@/store/appStore'
import { cn } from '@/lib/utils'

export default function Layout() {
  const { t } = useTranslation()
  const { user, setUser, theme, toggleTheme, lang, setLang } = useAppStore()
  const navigate = useNavigate()
  const qc = useQueryClient()

  const logoutMut = useMutation({
    mutationFn: authApi.logout,
    onSuccess: () => {
      setUser(null)
      qc.clear()
      navigate('/login')
    },
  })

  const navItems = [
    { to: '/decks',    icon: Layers,       label: t('nav.decks') },
    { to: '/review',   icon: BookOpen,     label: t('nav.review') },
    { to: '/practice', icon: Code2,        label: t('nav.practice') },
    { to: '/notebook', icon: NotebookPen,  label: t('nav.notebook') },
    { to: '/stats',    icon: BarChart3,    label: t('nav.stats') },
    { to: '/settings', icon: Settings,     label: t('nav.settings') },
  ]

  return (
    <div className="flex h-screen overflow-hidden bg-gray-50 dark:bg-gray-950">
      {/* Sidebar */}
      <aside className="w-60 flex flex-col bg-white dark:bg-gray-900 border-r border-gray-200 dark:border-gray-800 shadow-sm shrink-0">
        {/* Logo */}
        <div className="flex items-center gap-2 px-5 py-4 border-b border-gray-100 dark:border-gray-800">
          <Zap className="w-6 h-6 text-primary-600" />
          <span className="text-lg font-bold text-gray-900 dark:text-white">MemoSpark</span>
        </div>

        {/* Navigation */}
        <nav className="flex-1 px-3 py-4 space-y-1 overflow-y-auto">
          {navItems.map(({ to, icon: Icon, label }) => (
            <NavLink
              key={to}
              to={to}
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
      <main className="flex-1 overflow-y-auto">
        <Outlet />
      </main>
    </div>
  )
}
