export interface UserDto {
  id: number
  username: string
  role: 'USER' | 'ADMIN'
}

export interface AuthState {
  token: string | null
  user: UserDto | null
}

// ── Decks & Cards ──────────────────────────────────────────────────────────
// Mirrors backend DeckSummaryDto (subset used by the mini-program).
export interface DeckSummary {
  id: number
  name: string
  description: string
  type: string
  totalCards: number
  dueCards: number
}

export interface Card {
  id: number
  front: string
  back: string
  hint?: string
  difficulty?: string
}

export interface ReviewCard {
  cardId: number
  front: string
  back: string
  hint?: string
  deckName: string
  difficulty?: string
  dueDate?: string
}

export interface ReviewResult {
  nextDue: string
  interval: number
  easeFactor: number
}

// ── Stats ──────────────────────────────────────────────────────────────────
// Mirrors backend StatsDto.
export interface StatsSummary {
  totalCards: number
  totalDecks: number
  dueToday: number
  reviewedToday: number
  totalReviews: number
  retentionRate: number
  streakDays: number
}

// ── Targets ────────────────────────────────────────────────────────────────
export type TargetStatus = 'PREPARING' | 'INTERVIEWING' | 'CLOSED'

// Mirrors backend TargetSummaryDto.
export interface TargetSummary {
  id: number
  title: string
  company?: string
  status: TargetStatus
  interviewDate?: string
  daysUntilInterview?: number
  jdCount: number
  skillCount: number
  readiness: number
}

export interface TargetSkill {
  id: number
  name: string
  category?: string
  description?: string
  weight: number
  selfLevel: number
}

export interface JobJd {
  id: number
  title: string
  content: string
  source?: string
  createdAt: string
}

// Mirrors backend ReadinessDto.
export interface Readiness {
  overall: number
  skillCoverage: number
  cardHealth: number
  wrongClear: number
  dueCards: number
  dueNotes: number
  weakSkills: number
  daysUntilInterview?: number
}

// Mirrors backend TargetDetailDto.
export interface TargetDetail {
  id: number
  title: string
  company?: string
  status: TargetStatus
  interviewDate?: string
  daysUntilInterview?: number
  notes?: string
  jds: JobJd[]
  skills: TargetSkill[]
  readiness: Readiness
}

// ── Notebook ───────────────────────────────────────────────────────────────
// Mirrors backend ProblemNoteDto.
export interface ProblemNote {
  id: number
  problemId: number
  problemNumber: number
  title: string
  difficulty?: string
  category?: string
  bookmarkType: 'WRONG' | 'STARRED' | 'TODO'
  starred: boolean
  note?: string
  errorReason?: string
  retryCount: number
  nextRetryDate?: string
  isDueForRetry: boolean
}

// Mirrors the backend notebook summary map keys.
export interface NotebookSummary {
  wrong: number
  starred: number
  todo: number
  dueRetries: number
}
