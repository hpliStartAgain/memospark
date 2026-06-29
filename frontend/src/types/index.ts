export interface User {
  id: number
  username: string
  role: 'USER' | 'ADMIN'
  enabled?: boolean
  createdAt?: string
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
  cardId: number
  deckId: number
  deckName: string
  front: string
  back: string
  tags?: string
  contentDifficulty: CardDifficulty
  learningStage: LearningStage
  stageOrder: number
  governanceNote?: string
  repetitions: number
  easeFactor: number
  interval: number
  nextReviewDate?: string
  isNew: boolean
}

export interface ReviewCard {
  cardId: number
  deckId: number
  deckName: string
  front: string
  back: string
  tags?: string
  contentDifficulty: CardDifficulty
  learningStage: LearningStage
  stageOrder: number
  governanceNote?: string
  repetitions: number
  easeFactor: number
  interval: number
  nextReviewDate?: string
  isNew: boolean
}

export type CardDifficulty = 'EASY' | 'MEDIUM' | 'HARD'
export type LearningStage = 'FOUNDATION' | 'ADVANCED' | 'PRACTICE'

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
  userAnswer?: string
  aiGrade?: string
  aiFeedback?: string
  aiSuggestedAnswer?: string
}

export interface AnswerEvaluation {
  grade: string
  quality: number
  score: number
  feedback: string
  missingPoints: string[]
  suggestedAnswer: string
  recommendedReviewDays?: number
  coachingTip?: string
  learningMode: 'LEARNING' | 'REVIEW'
}

export interface AnswerChatMessage {
  role: 'user' | 'assistant'
  content: string
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
  desiredRetention: number
}

export interface AiSettings {
  provider: string
  baseUrl: string
  model: string
  apiKeyConfigured: boolean
  apiKeyMasked?: string
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

export type TargetStatus =
  | 'PREPARING'
  | 'APPLIED'
  | 'WRITTEN_TEST'
  | 'INTERVIEW_1'
  | 'INTERVIEW_2'
  | 'HR'
  | 'OFFER'
  | 'REJECTED'
  | 'INTERVIEWING'
  | 'CLOSED'

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

export interface JobJd {
  id: number
  title?: string
  content: string
  source?: string
  createdAt: string
}

export interface TargetSkill {
  id: number
  name: string
  category?: string
  description?: string
  weight: number
  selfLevel: number
  deckId?: number | null
  cardCount: number
  deckLinkSource?: 'AI_CREATED' | 'MATCHED_EXISTING' | 'MANUAL'
  matchedDeckName?: string
  deckMatchScore?: number
}

export interface Readiness {
  overall: number
  skillCoverage: number
  cardHealth: number
  wrongClear: number
  mockPerformance: number
  dueCards: number
  dueNotes: number
  weakSkills: number
  daysUntilInterview?: number
}

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

export type StudyPlanItemType = 'LEARN' | 'REVIEW' | 'PRACTICE' | 'CHECKPOINT'

export interface StudyPlanItem {
  id: number
  date: string
  type: StudyPlanItemType
  deckId?: number
  deckName?: string
  stage?: LearningStage
  title: string
  objective?: string
  targetCount: number
  completedCount: number
  completed: boolean
}

export interface StudyPlanDay {
  date: string
  items: StudyPlanItem[]
}

export interface StudyPlanWeek {
  weekNumber: number
  startDate: string
  endDate: string
  objective: string
  stage: LearningStage
  days: StudyPlanDay[]
}

export interface StudyPlanPhase {
  name: string
  startWeek: number
  endWeek: number
  goal: string
}

export interface StudyPlan {
  id: number
  targetId: number
  targetTitle: string
  startDate: string
  targetDate: string
  weeklyHours: number
  summary: string
  strategy: string
  roadmap: {
    phases?: StudyPlanPhase[]
    risks?: string[]
  }
  weeks: StudyPlanWeek[]
  generatedAt: string
}

export type MockInterviewType = 'MIXED' | 'BEHAVIORAL' | 'TECHNICAL' | 'SYSTEM_DESIGN'
export type MockInterviewStatus = 'IN_PROGRESS' | 'FINISHED'

export interface MockInterviewQuestion {
  id: number
  questionOrder: number
  dimension: string
  question: string
  rubric?: string
  userAnswer?: string
  score?: number
  feedback?: string
  answeredAt?: string
}

export interface MockInterview {
  id: number
  targetId: number
  type: MockInterviewType
  status: MockInterviewStatus
  questionCount: number
  answeredCount: number
  averageScore?: number
  summaryFeedback?: string
  startedAt: string
  finishedAt?: string
  questions: MockInterviewQuestion[]
}

// ── Admin ──────────────────────────────────────────────────────────────────
export interface AdminSystemInfo {
  appName: string
  appVersion: string
  javaVersion: string
  javaVendor: string
  osName: string
  osArch: string
  springBootVersion: string
  startupTime: string
  uptimeSeconds: number
  heapUsedMb: number
  heapMaxMb: number
  availableProcessors: number
  threadCount: number
}

export interface AdminStats {
  totalUsers: number
  enabledUsers: number
  todayNewUsers: number
  todayActiveUsers: number
  totalDecks: number
  totalCards: number
  totalReviews: number
  totalTargets: number
  todayReviews: number
  overallRetentionRate: number
  adminCount: number
}

export interface AdminDauPoint {
  date: string
  activeUsers: number
  totalRequests: number
}

export interface AdminUser {
  id: number
  username: string
  role: 'USER' | 'ADMIN'
  enabled: boolean
  createdAt: string
  lastActiveAt?: string
  deckCount: number
  cardCount: number
  reviewCount: number
}

export interface AdminUserList {
  users: AdminUser[]
  total: number
}
