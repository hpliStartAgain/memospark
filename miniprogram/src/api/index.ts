import Taro from '@tarojs/taro'
import { store } from '../store'
import type {
  UserDto,
  DeckSummary,
  ReviewCard,
  ReviewResult,
  StatsSummary,
  TargetSummary,
  TargetDetail,
  TargetSkill,
  JobJd,
  Readiness,
  ProblemNote,
  NotebookSummary,
} from '../types'

declare const process: { env: { TARO_APP_API_URL?: string } }

const BASE_URL: string =
  (process.env.TARO_APP_API_URL as string) || 'http://localhost:8080'

function request<T = any>(
  method: 'GET' | 'POST' | 'PUT' | 'DELETE',
  path: string,
  data?: object
): Promise<T> {
  const token = store.getToken()
  return new Promise<T>((resolve, reject) => {
    Taro.request({
      url: BASE_URL + '/api' + path,
      method,
      data,
      header: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      success(res) {
        if (res.statusCode >= 200 && res.statusCode < 300) {
          resolve(res.data as T)
        } else if (res.statusCode === 401) {
          store.clear()
          Taro.reLaunch({ url: '/pages/login/index' })
          reject(new Error('Unauthorized'))
        } else {
          const msg =
            (res.data as any)?.error || `HTTP ${res.statusCode}`
          reject(new Error(msg))
        }
      },
      fail(err) {
        reject(new Error(err.errMsg || 'Network error'))
      },
    })
  })
}

// ── Auth ──────────────────────────────────────────────────────────────────
export const authApi = {
  wxLogin: (code: string) =>
    request<{ token: string; user: UserDto }>('POST', '/auth/wx-login', { code }),
  tokenLogin: (username: string, password: string) =>
    request<{ token: string; user: UserDto }>('POST', '/auth/token', {
      username,
      password,
    }),
  me: () => request<UserDto>('GET', '/auth/me'),
}

// ── Stats ──────────────────────────────────────────────────────────────────
export const statsApi = {
  summary: () => request<StatsSummary>('GET', '/stats'),
}

// ── Decks & Review ─────────────────────────────────────────────────────────
export const deckApi = {
  list: () => request<DeckSummary[]>('GET', '/decks'),
}

export const reviewApi = {
  today: () => request<ReviewCard[]>('GET', '/review/today'),
  byDeck: (deckId: number) =>
    request<ReviewCard[]>('GET', `/review/deck/${deckId}`),
  submit: (cardId: number, quality: number, timeSpentMs?: number) =>
    request<ReviewCard>('POST', `/review/${cardId}`, {
      quality,
      timeSpentMs,
    }),
}

// ── Targets ────────────────────────────────────────────────────────────────
export const targetApi = {
  list: () => request<TargetSummary[]>('GET', '/targets'),
  get: (id: number) => request<TargetDetail>('GET', `/targets/${id}`),
  create: (body: {
    title: string
    company?: string
    status?: string
    interviewDate?: string
    notes?: string
  }) => request<TargetDetail>('POST', '/targets', body),
  update: (id: number, body: object) =>
    request<TargetDetail>('PUT', `/targets/${id}`, body),
  remove: (id: number) => request<void>('DELETE', `/targets/${id}`),
  addJd: (id: number, body: { title: string; content: string; source?: string }) =>
    request<JobJd>('POST', `/targets/${id}/jds`, body),
  analyze: (id: number, language: string, replace: boolean) =>
    request<TargetDetail>('POST', `/targets/${id}/analyze`, {
      language,
      replace,
    }),
  updateSkill: (id: number, skillId: number, body: { selfLevel: number }) =>
    request<TargetSkill>('PUT', `/targets/${id}/skills/${skillId}`, body),
  readiness: (id: number) => request<Readiness>('GET', `/targets/${id}/readiness`),
}

// ── Notebook ───────────────────────────────────────────────────────────────
export const practiceApi = {
  notebook: (type?: string) =>
    request<ProblemNote[]>(
      'GET',
      '/practice/notebook' + (type ? `?type=${type}` : '')
    ),
  notebookSummary: () => request<NotebookSummary>('GET', '/practice/notebook/summary'),
  dueNotes: () => request<ProblemNote[]>('GET', '/practice/notebook/due'),
  retry: (problemId: number, quality: number) =>
    request<void>('POST', `/practice/notebook/${problemId}/retry`, { quality }),
  toggleStar: (problemId: number) =>
    request<void>('POST', `/practice/notebook/${problemId}/toggle-star`),
}
