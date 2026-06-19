export interface User {
  id: number
  username: string
  role: 'USER' | 'ADMIN'
}

export interface Deck {
  id: number
  name: string
  description?: string
  type: 'CUSTOM' | 'BUILTIN' | 'POOL'
  createdAt: string
  totalCards: number
  dueCards: number
  newCards: number
  reviewCards: number
  newLearnedToday: number
  dailyReviewLimit?: number
  dailyNewCardLimit?: number
  reviewedToday: number
  ownerUsername?: string
}

export interface Card {
  id: number
  deckId: number
  deckName: string
  front: string
  back: string
  tags?: string
  repetitions: number
  easeFactor: number
  interval: number
  nextReviewDate?: string
  isNew: boolean
}

export interface Problem {
  id: number
  problemNumber: number
  title: string
  difficulty: 'Easy' | 'Medium' | 'Hard'
  category?: string
  tags?: string
  hint?: string
  accepted: boolean
  bookmarkType?: string
  starred: boolean
  failCount: number
  attemptCount: number
}

export interface ProblemDetail extends Problem {
  description: string
  javaTemplate: string
  pythonTemplate: string
}

export interface Submission {
  id: number
  language: string
  status: string
  passedCases: number
  totalCases: number
  submittedAt: string
}

export interface ReviewRequest {
  quality: number
  timeSpentMs?: number
}

export interface ProblemNote {
  id: number
  problemId: number
  problemTitle?: string
  problemDifficulty?: string
  bookmarkType?: string
  starred: boolean
  note?: string
  errorReason?: string
  retryCount: number
  nextRetryDate?: string
  updatedAt: string
}

export interface Stats {
  totalCards: number
  totalDecks: number
  dueToday: number
  reviewedToday: number
  totalReviews: number
  retentionRate: number
  streakDays: number
}

export interface DailyStats {
  date: string
  count: number
  retentionRate: number
}

export interface SrsSettings {
  initialEaseFactor: number
  minEaseFactor: number
  firstInterval: number
  secondInterval: number
}

export interface GeneratedCard {
  front: string
  back: string
}

export interface SubmitResult {
  submissionId: number
  status: string
  passedCases: number
  totalCases: number
  testCases: TestCaseResult[]
}

export interface TestCaseResult {
  index: number
  passed: boolean
  input: string
  expectedOutput: string
  actualOutput: string
}
