import { useEffect } from 'react'
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { authApi } from '@/lib/api'
import { useAppStore } from '@/store/appStore'
import Layout from '@/components/Layout'
import LoginPage from '@/pages/LoginPage'
import PasswordResetPage from '@/pages/PasswordResetPage'
import DecksPage from '@/pages/DecksPage'
import ReviewPage from '@/pages/ReviewPage'
import PracticePage from '@/pages/PracticePage'
import ProblemDetailPage from '@/pages/ProblemDetailPage'
import NotebookPage from '@/pages/NotebookPage'
import StatsPage from '@/pages/StatsPage'
import SettingsPage from '@/pages/SettingsPage'
import { PageSpinner } from '@/components/ui/Spinner'

function AuthGuard({ children }: { children: React.ReactNode }) {
  const { user, setUser } = useAppStore()
  const { data, isLoading, isError } = useQuery({
    queryKey: ['me'],
    queryFn: authApi.me,
    retry: false,
    enabled: !user,
  })

  useEffect(() => {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    if (data) setUser(data as any)
  }, [data, setUser])

  if (isLoading && !user) return <PageSpinner />
  if (isError && !user) return <Navigate to="/login" replace />
  return <>{children}</>
}

export default function App() {
  const { theme } = useAppStore()

  useEffect(() => {
    document.documentElement.classList.toggle('dark', theme === 'dark')
  }, [theme])

  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/password-reset" element={<PasswordResetPage />} />
        <Route
          path="/"
          element={
            <AuthGuard>
              <Layout />
            </AuthGuard>
          }
        >
          <Route index element={<Navigate to="/decks" replace />} />
          <Route path="decks" element={<DecksPage />} />
          <Route path="review" element={<ReviewPage />} />
          <Route path="review/:deckId" element={<ReviewPage />} />
          <Route path="practice" element={<PracticePage />} />
          <Route path="practice/:id" element={<ProblemDetailPage />} />
          <Route path="notebook" element={<NotebookPage />} />
          <Route path="stats" element={<StatsPage />} />
          <Route path="settings" element={<SettingsPage />} />
        </Route>
        <Route path="*" element={<Navigate to="/decks" replace />} />
      </Routes>
    </BrowserRouter>
  )
}
