import type { TargetStatus } from '../types'

const ACTIVE_STATUSES: TargetStatus[] = [
  'PREPARING',
  'APPLIED',
  'WRITTEN_TEST',
  'INTERVIEW_1',
  'INTERVIEW_2',
  'HR',
  'INTERVIEWING',
]

export function isActiveTargetStatus(status?: TargetStatus | string) {
  return Boolean(status && ACTIVE_STATUSES.includes(status as TargetStatus))
}

export function targetStatusLabel(status?: TargetStatus | string) {
  switch (status) {
    case 'PREPARING':
      return '准备中'
    case 'APPLIED':
      return '已投递'
    case 'WRITTEN_TEST':
      return '笔试'
    case 'INTERVIEW_1':
      return '一面'
    case 'INTERVIEW_2':
      return '二面'
    case 'HR':
      return 'HR 面'
    case 'OFFER':
      return 'Offer'
    case 'REJECTED':
      return '未通过'
    case 'INTERVIEWING':
      return '面试中'
    case 'CLOSED':
      return '已结束'
    default:
      return '未知'
  }
}

export function targetStatusClass(status?: TargetStatus | string) {
  switch (status) {
    case 'PREPARING':
      return 'preparing'
    case 'APPLIED':
      return 'applied'
    case 'WRITTEN_TEST':
      return 'written'
    case 'INTERVIEW_1':
    case 'INTERVIEW_2':
    case 'INTERVIEWING':
      return 'interviewing'
    case 'HR':
      return 'hr'
    case 'OFFER':
      return 'offer'
    case 'REJECTED':
      return 'rejected'
    case 'CLOSED':
      return 'closed'
    default:
      return 'unknown'
  }
}
