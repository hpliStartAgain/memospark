import { lazy, Suspense, useEffect } from 'react'
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { authApi } from '@/lib/api'
import { useAppStore } from '@/store/appStore'
import Layout from '@/components/Layout'
import { PageSpinner } from '@/components/ui/Spinner'

const LoginPage = lazy(() => import('@/pages/LoginPage'))
const PasswordResetPage = lazy(() => import('@/pages/PasswordResetPage'))
const LandingPage = lazy(() => import('@/pages/LandingPage'))
const DashboardPage = lazy(() => import('@/pages/DashboardPage'))
const TargetsPage = lazy(() => import('@/pages/TargetsPage'))
const TargetDetailPage = lazy(() => import('@/pages/TargetDetailPage'))
const MockInterviewPage = lazy(() => import('@/pages/MockInterviewPage'))
const DecksPage = lazy(() => import('@/pages/DecksPage'))
const ReviewPage = lazy(() => import('@/pages/ReviewPage'))
const PracticePage = lazy(() => import('@/pages/PracticePage'))
const ProblemDetailPage = lazy(() => import('@/pages/ProblemDetailPage'))
const NotebookPage = lazy(() => import('@/pages/NotebookPage'))
const StatsPage = lazy(() => import('@/pages/StatsPage'))
const SettingsPage = lazy(() => import('@/pages/SettingsPage'))

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
  if (isError && !user) return <Navigate to="/landing" replace />
  return <>{children}</>
}

export default function App() {
  const { theme } = useAppStore()

  useEffect(() => {
    document.documentElement.classList.toggle('dark', theme === 'dark')
  }, [theme])

  return (
    <BrowserRouter>
      <Suspense fallback={<PageSpinner />}>
        <Routes>
          <Route path="/landing" element={<LandingPage />} />
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
            <Route index element={<DashboardPage />} />
            <Route path="targets" element={<TargetsPage />} />
            <Route path="targets/:id" element={<TargetDetailPage />} />
            <Route path="targets/:id/mock" element={<MockInterviewPage />} />
            <Route path="decks" element={<DecksPage />} />
            <Route path="review" element={<ReviewPage />} />
            <Route path="review/:deckId" element={<ReviewPage />} />
            <Route path="practice" element={<PracticePage />} />
            <Route path="practice/:id" element={<ProblemDetailPage />} />
            <Route path="notebook" element={<NotebookPage />} />
            <Route path="stats" element={<StatsPage />} />
            <Route path="settings" element={<SettingsPage />} />
          </Route>
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </Suspense>
    </BrowserRouter>
  )
}
