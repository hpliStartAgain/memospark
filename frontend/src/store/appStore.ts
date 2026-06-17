import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import type { User } from '../types'
import i18n from '../lib/i18n'

interface AppState {
  user: User | null
  theme: 'light' | 'dark'
  lang: 'zh' | 'en'
  setUser: (user: User | null) => void
  setTheme: (theme: 'light' | 'dark') => void
  setLang: (lang: 'zh' | 'en') => void
  toggleTheme: () => void
}

export const useAppStore = create<AppState>()(
  persist(
    (set, get) => ({
      user: null,
      theme: 'light',
      lang: 'zh',
      setUser: (user) => set({ user }),
      setTheme: (theme) => {
        set({ theme })
        document.documentElement.classList.toggle('dark', theme === 'dark')
      },
      setLang: (lang) => {
        set({ lang })
        localStorage.setItem('lang', lang)
        i18n.changeLanguage(lang)
      },
      toggleTheme: () => {
        const next = get().theme === 'light' ? 'dark' : 'light'
        set({ theme: next })
        document.documentElement.classList.toggle('dark', next === 'dark')
      },
    }),
    {
      name: 'memospark-ui',
      partialize: (s) => ({ theme: s.theme, lang: s.lang }),
      onRehydrateStorage: () => (state) => {
        if (state?.theme === 'dark') {
          document.documentElement.classList.add('dark')
        }
        if (state?.lang) {
          i18n.changeLanguage(state.lang)
        }
      },
    },
  ),
)
