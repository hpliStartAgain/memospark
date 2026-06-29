import axios, { type AxiosRequestConfig } from 'axios'

function getCsrfToken(): string | null {
  const match = document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]*)/)
  return match ? decodeURIComponent(match[1]) : null
}

const api = axios.create({
  baseURL: '/api',
  withCredentials: true,
  headers: { 'Content-Type': 'application/json' },
})

api.interceptors.request.use((config) => {
  const method = config.method?.toUpperCase()
  if (method && ['POST', 'PUT', 'DELETE', 'PATCH'].includes(method)) {
    const token = getCsrfToken()
    if (token) config.headers['X-XSRF-TOKEN'] = token
  }
  return config
})

api.interceptors.response.use(
  (r) => r,
  (err) => {
    if (err.response?.status === 401) {
      window.location.href = '/landing#access'
    }
    return Promise.reject(err)
  },
)

export default api

// ── Auth ──────────────────────────────────────────────────────────────────
export const authApi = {
  login:    (username: string, password: string) =>
    api.post('/auth/login', { username, password }).then(r => r.data),
  register: (username: string, password: string) =>
    api.post('/auth/register', { username, password }).then(r => r.data),
  me:       () => api.get('/auth/me').then(r => r.data),
  logout:   () => api.post('/auth/logout').then(r => r.data),
  requestPasswordReset: (username: string) =>
    api.post('/auth/password-reset/request', { username }).then(r => r.data),
  confirmPasswordReset: (token: string, newPassword: string) =>
    api.post('/auth/password-reset/confirm', { token, newPassword }).then(r => r.data),
}

// ── Decks ─────────────────────────────────────────────────────────────────
export const deckApi = {
  list:       () => api.get('/decks').then(r => r.data),
  get:        (id: number) => api.get(`/decks/${id}`).then(r => r.data),
  create:     (body: object) => api.post('/decks', body).then(r => r.data),
  update:     (id: number, body: object) => api.put(`/decks/${id}`, body).then(r => r.data),
  remove:     (id: number) => api.delete(`/decks/${id}`),
  getTags:    (id: number) => api.get(`/decks/${id}/tags`).then(r => r.data),
  pool:       () => api.get('/decks/pool').then(r => r.data),
  copyPool:   (id: number) => api.post(`/decks/pool/${id}/copy`).then(r => r.data),
}

// ── Cards ─────────────────────────────────────────────────────────────────
export const cardApi = {
  list:        (deckId: number) => api.get(`/decks/${deckId}/cards`).then(r => r.data),
  create:      (deckId: number, body: object) => api.post(`/decks/${deckId}/cards`, body).then(r => r.data),
  update:      (deckId: number, cardId: number, body: object) =>
    api.put(`/decks/${deckId}/cards/${cardId}`, body).then(r => r.data),
  remove:      (deckId: number, cardId: number) => api.delete(`/decks/${deckId}/cards/${cardId}`),
  batchDelete: (deckId: number, cardIds: number[]) =>
    api.post(`/decks/${deckId}/cards/batch-delete`, { cardIds }),
  batchMove:   (fromDeckId: number, toDeckId: number, cardIds: number[]) =>
    api.post(`/decks/${fromDeckId}/cards/batch-move`, { targetDeckId: toDeckId, cardIds }),
  fromText:    (deckId: number, body: object) =>
    api.post(`/decks/${deckId}/cards/from-text`, body).then(r => r.data),
  govern:      (deckId: number, language: string) =>
    api.post(`/decks/${deckId}/cards/govern`, { language }).then(r => r.data),
}

// ── Review ────────────────────────────────────────────────────────────────
export const reviewApi = {
  today:       () => api.get('/review/today').then(r => r.data),
  byDeck:      (deckId: number, tags?: string[]) => {
    const params = tags?.length ? { params: { tags } } : {}
    return api.get(`/review/deck/${deckId}`, params as AxiosRequestConfig).then(r => r.data)
  },
  submit:      (cardId: number, quality: number, timeSpentMs?: number, evidence?: object) =>
    api.post(`/review/${cardId}`, { quality, timeSpentMs, ...(evidence || {}) }).then(r => r.data),
  evaluateAnswer: (cardId: number, userAnswer: string) =>
    api.post(`/review/${cardId}/evaluate-answer`, { userAnswer }).then(r => r.data),
  explainAnswer: (cardId: number, body: object) =>
    api.post(`/review/${cardId}/explain-answer`, body).then(r => r.data),
  hard:        () => api.get('/review/hard').then(r => r.data),
  undo:        (cardId: number) => api.post(`/review/${cardId}/undo`).then(r => r.data),
}

// ── Practice ──────────────────────────────────────────────────────────────
export const practiceApi = {
  problems:    () => api.get('/practice/problems').then(r => r.data),
  problem:     (id: number) => api.get(`/practice/problems/${id}`).then(r => r.data),
  submit:      (id: number, language: string, code: string) =>
    api.post(`/practice/problems/${id}/submit`, { language, code }).then(r => r.data),
  submissions: (id: number) => api.get(`/practice/problems/${id}/submissions`).then(r => r.data),
  notebook:    (type?: string) =>
    api.get('/practice/notebook', type ? { params: { type } } : {}).then(r => r.data),
  notebookSummary: () => api.get('/practice/notebook/summary').then(r => r.data),
  dueNotes:    () => api.get('/practice/notebook/due').then(r => r.data),
  toggleStar:  (problemId: number) => api.post(`/practice/notebook/${problemId}/toggle-star`).then(r => r.data),
  toggleTodo:  (problemId: number) => api.post(`/practice/notebook/${problemId}/toggle-todo`).then(r => r.data),
  saveWrong:   (problemId: number, note: string, errorReason: string) =>
    api.post(`/practice/notebook/${problemId}/wrong`, { note, errorReason }).then(r => r.data),
  removeWrong: (problemId: number) => api.delete(`/practice/notebook/${problemId}/wrong`),
  retry:       (problemId: number, quality: number) =>
    api.post(`/practice/notebook/${problemId}/retry`, { quality }).then(r => r.data),
  toCard:      (problemId: number, deckId: number) =>
    api.post(`/practice/notebook/${problemId}/to-card`, { deckId }).then(r => r.data),
  aiAnalysis:  () => api.post('/practice/notebook/ai-analysis').then(r => r.data),
}

// ── Stats ─────────────────────────────────────────────────────────────────
export const statsApi = {
  summary: () => api.get('/stats').then(r => r.data),
  daily:   (days = 30) => api.get('/stats/daily', { params: { days } }).then(r => r.data),
}

// ── AI ────────────────────────────────────────────────────────────────────
export const aiApi = {
  hint:           (problemDescription: string, userCode: string, level: number) =>
    api.post('/ai/hint', { problemDescription, userCode, level }).then(r => r.data),
  grade:          (question: string, referenceAnswer: string, userAnswer: string) =>
    api.post('/ai/grade', { question, referenceAnswer, userAnswer }).then(r => r.data),
  analyzeTLE:     (problemDescription: string, userCode: string, language: string) =>
    api.post('/ai/analyze-tle', { problemDescription, userCode, language }).then(r => r.data),
  generateCards:  (topic: string, count: number, language: string) =>
    api.post('/ai/generate-cards', { topic, count, language }).then(r => r.data),
  analyzeJds:     (jds: string[], language: string) =>
    api.post('/ai/jd/analyze', { jds, language }).then(r => r.data),
  generateJdCards:(deckName: string, topic: string, count: number, language: string) =>
    api.post('/ai/jd/generate-cards', { deckName, topic, count, language }).then(r => r.data),
}

// ── Targets (interview prep) ────────────────────────────────────────────────
export const targetApi = {
  list:        () => api.get('/targets').then(r => r.data),
  get:         (id: number) => api.get(`/targets/${id}`).then(r => r.data),
  create:      (body: object) => api.post('/targets', body).then(r => r.data),
  update:      (id: number, body: object) => api.put(`/targets/${id}`, body).then(r => r.data),
  updateStatus:(id: number, status: string) => api.patch(`/targets/${id}/status`, { status }).then(r => r.data),
  remove:      (id: number) => api.delete(`/targets/${id}`),
  addJd:       (id: number, body: object) => api.post(`/targets/${id}/jds`, body).then(r => r.data),
  removeJd:    (id: number, jdId: number) => api.delete(`/targets/${id}/jds/${jdId}`),
  analyze:     (id: number, language: string, replace: boolean) =>
    api.post(`/targets/${id}/analyze`, { language, replace }).then(r => r.data),
  addSkill:    (id: number, body: object) => api.post(`/targets/${id}/skills`, body).then(r => r.data),
  updateSkill: (id: number, skillId: number, body: object) =>
    api.put(`/targets/${id}/skills/${skillId}`, body).then(r => r.data),
  removeSkill: (id: number, skillId: number) => api.delete(`/targets/${id}/skills/${skillId}`),
  generateSkillCards: (id: number, skillId: number, lang: string) =>
    api.post(`/targets/${id}/skills/${skillId}/generate-cards`, null, { params: { lang } }).then(r => r.data),
  readiness:   (id: number) => api.get(`/targets/${id}/readiness`).then(r => r.data),
}

// ── Study plans ──────────────────────────────────────────────────────────
export const planApi = {
  get: (targetId: number) =>
    api.get(`/plans/target/${targetId}`)
      .then(r => r.data)
      .catch(error => {
        if (error.response?.status === 404) return null
        throw error
      }),
  generate: (targetId: number, body: object) =>
    api.post(`/plans/target/${targetId}/generate`, body).then(r => r.data),
  today: () => api.get('/plans/today').then(r => r.data),
  updateItem: (itemId: number, completed: boolean) =>
    api.patch(`/plans/items/${itemId}`, { completed }).then(r => r.data),
}

// ── Mock Interviews ───────────────────────────────────────────────────────
export const mockInterviewApi = {
  list:   (targetId: number) => api.get(`/targets/${targetId}/mock-interviews`).then(r => r.data),
  get:    (targetId: number, interviewId: number) =>
    api.get(`/targets/${targetId}/mock-interviews/${interviewId}`).then(r => r.data),
  start:  (targetId: number, body: object) =>
    api.post(`/targets/${targetId}/mock-interviews`, body).then(r => r.data),
  answer: (targetId: number, interviewId: number, questionId: number, answer: string) =>
    api.post(`/targets/${targetId}/mock-interviews/${interviewId}/questions/${questionId}/answer`, { answer }).then(r => r.data),
  finish: (targetId: number, interviewId: number) =>
    api.post(`/targets/${targetId}/mock-interviews/${interviewId}/finish`).then(r => r.data),
}

// ── Settings ──────────────────────────────────────────────────────────────
export const settingsApi = {
  getSrs:    () => api.get('/settings/srs').then(r => r.data),
  updateSrs: (body: object) => api.put('/settings/srs', body).then(r => r.data),
  getAi:     () => api.get('/settings/ai').then(r => r.data),
  updateAi:  (body: object) => api.put('/settings/ai', body).then(r => r.data),
  testAi:    () => api.post('/settings/ai/test').then(r => r.data),
  export:    (deckId: number) => api.get(`/export/csv/${deckId}`, { responseType: 'blob' }),
  importCsv: (deckId: number, file: File) => {
    const fd = new FormData(); fd.append('file', file)
    return api.post(`/import/csv/${deckId}`, fd, { headers: { 'Content-Type': 'multipart/form-data' } })
  },
}

// ── SSE helper ────────────────────────────────────────────────────────────
export function openSubmissionStream(
  submissionId: number,
  onProgress: (data: { case: number; total: number; status: string }) => void,
  onResult: (data: object) => void,
  onError: (msg: string) => void,
): EventSource {
  const es = new EventSource(`/api/practice/submissions/${submissionId}/stream`, { withCredentials: true })
  es.addEventListener('progress', (e) => onProgress(JSON.parse(e.data)))
  es.addEventListener('result',   (e) => { onResult(JSON.parse(e.data)); es.close() })
  es.addEventListener('error',    (e: MessageEvent) => {
    try { onError(JSON.parse(e.data).message) } catch { onError('Connection error') }
    es.close()
  })
  return es
}

/**
 * SSE stream for generate-cards.
 * Events: status | chunk | complete | error
 */
export function openGenerateCardsStream(
  targetId: number,
  skillId: number,
  lang: string,
  callbacks: {
    onStatus?: (msg: string) => void
    onChunk?: (delta: string) => void
    onComplete?: (created: number) => void
    onError?: (msg: string) => void
  },
): EventSource {
  const es = new EventSource(
    `/api/targets/${targetId}/skills/${skillId}/generate-cards/stream?lang=${lang}`,
    { withCredentials: true },
  )
  es.addEventListener('status', (e: MessageEvent) => callbacks.onStatus?.(e.data))
  es.addEventListener('chunk', (e: MessageEvent) => callbacks.onChunk?.(e.data))
  es.addEventListener('complete', (e: MessageEvent) => {
    try { callbacks.onComplete?.(JSON.parse(e.data).created) } catch { callbacks.onComplete?.(0) }
    es.close()
  })
  es.addEventListener('error', (e: MessageEvent) => {
    // EventSource fires 'error' on connection close too — only treat as error if data present
    if (e.data) {
      callbacks.onError?.(e.data)
    } else if (es.readyState === EventSource.CLOSED) {
      // already closed by complete event — ignore
    } else {
      callbacks.onError?.('连接中断')
    }
    es.close()
  })
  return es
}

// ── Admin ────────────────────────────────────────────────────────────────────
export const adminApi = {
  system:  () => api.get('/admin/system').then(r => r.data),
  stats:   () => api.get('/admin/stats').then(r => r.data),
  dau:     (days = 30) => api.get('/admin/dau', { params: { days } }).then(r => r.data),
  users:   () => api.get('/admin/users').then(r => r.data),
  setEnabled: (userId: number, enabled: boolean) =>
    api.patch(`/admin/users/${userId}/enabled`, { enabled }).then(r => r.data),
  setRole: (userId: number, role: string) =>
    api.patch(`/admin/users/${userId}/role`, { role }).then(r => r.data),
  resetPassword: (userId: number, newPassword: string) =>
    api.post(`/admin/users/${userId}/reset-password`, { newPassword }).then(r => r.data),
}
