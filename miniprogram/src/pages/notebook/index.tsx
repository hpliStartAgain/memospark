import { useState, useEffect } from 'react'
import Taro from '@tarojs/taro'
import { View, Text } from '@tarojs/components'
import { practiceApi } from '../../api'
import { store } from '../../store'
import type { ProblemNote, NotebookSummary } from '../../types'
import './index.scss'

type FilterType = 'ALL' | 'WRONG' | 'STARRED' | 'TODO' | 'DUE'

const TABS: { key: FilterType; label: string }[] = [
  { key: 'ALL',     label: '全部' },
  { key: 'WRONG',   label: '错题' },
  { key: 'STARRED', label: '收藏' },
  { key: 'TODO',    label: '待做' },
  { key: 'DUE',     label: '待复习' },
]

function badgeClass(type: string) {
  if (type === 'WRONG')   return 'wrong'
  if (type === 'STARRED') return 'starred'
  return 'todo'
}

function badgeLabel(type: string) {
  if (type === 'WRONG')   return '错题'
  if (type === 'STARRED') return '★ 收藏'
  return '待做'
}

function diffClass(d?: string) {
  if (!d) return ''
  const lower = d.toLowerCase()
  if (lower.includes('easy') || lower.includes('简单')) return 'easy'
  if (lower.includes('hard') || lower.includes('困难')) return 'hard'
  return 'medium'
}

export default function NotebookPage() {
  const [filter, setFilter] = useState<FilterType>('ALL')
  const [notes, setNotes] = useState<ProblemNote[]>([])
  const [summary, setSummary] = useState<NotebookSummary | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (!store.isLoggedIn()) {
      Taro.reLaunch({ url: '/pages/login/index' })
      return
    }
    loadSummary()
    loadNotes('ALL')
  }, [])

  Taro.useDidShow(() => {
    loadSummary()
    loadNotes(filter)
  })

  async function loadSummary() {
    try {
      const data = await practiceApi.notebookSummary()
      setSummary(data)
    } catch {
      // ignore
    }
  }

  async function loadNotes(f: FilterType) {
    setLoading(true)
    try {
      let data: ProblemNote[]
      if (f === 'DUE') {
        data = await practiceApi.dueNotes()
      } else if (f === 'ALL') {
        data = await practiceApi.notebook()
      } else {
        data = await practiceApi.notebook(f)
      }
      setNotes(data)
    } catch {
      setNotes([])
    } finally {
      setLoading(false)
    }
  }

  function handleTabChange(f: FilterType) {
    setFilter(f)
    loadNotes(f)
  }

  async function handleRetry(problemId: number, quality: number) {
    try {
      await practiceApi.retry(problemId, quality)
      Taro.showToast({ title: '已记录', icon: 'success' })
      loadNotes(filter)
      loadSummary()
    } catch {
      Taro.showToast({ title: '操作失败', icon: 'none' })
    }
  }

  const isToday = (date?: string) => {
    if (!date) return false
    return new Date(date).toDateString() === new Date().toDateString()
  }

  return (
    <View className='notebook-page'>
      <View className='nb-header'>
        <Text className='nb-title'>错题笔记</Text>
        <View className='nb-stats'>
          <View className='nb-stat'>
            <Text className='ns-value'>{summary?.wrong ?? 0}</Text>
            <Text className='ns-label'>错题</Text>
          </View>
          <View className='nb-stat'>
            <Text className='ns-value'>{summary?.starred ?? 0}</Text>
            <Text className='ns-label'>收藏</Text>
          </View>
          <View className='nb-stat'>
            <Text className='ns-value'>{summary?.dueRetries ?? 0}</Text>
            <Text className='ns-label'>待复习</Text>
          </View>
          <View className='nb-stat'>
            <Text className='ns-value'>{summary?.todo ?? 0}</Text>
            <Text className='ns-label'>待做</Text>
          </View>
        </View>
      </View>

      <View className='filter-tabs'>
        {TABS.map((t) => (
          <View
            key={t.key}
            className={`tab-item ${filter === t.key ? 'active' : ''}`}
            onClick={() => handleTabChange(t.key)}
          >
            <Text>{t.label}</Text>
          </View>
        ))}
      </View>

      <View className='note-list'>
        {loading ? (
          <View className='loading-spinner'><Text>加载中...</Text></View>
        ) : notes.length === 0 ? (
          <View className='empty-state'>
            <Text>📝</Text>
            <Text>暂无笔记</Text>
          </View>
        ) : (
          notes.map((note) => (
            <View key={note.problemId} className='note-card'>
              <View className='note-top'>
                <Text className={`note-badge ${badgeClass(note.bookmarkType)}`}>
                  {badgeLabel(note.bookmarkType)}
                </Text>
                <Text className='note-title'>{note.title}</Text>
                {note.difficulty ? (
                  <Text className={`diff-badge ${diffClass(note.difficulty)}`}>
                    {note.difficulty}
                  </Text>
                ) : null}
              </View>

              {note.note ? (
                <Text className='note-text'>{note.note}</Text>
              ) : null}

              {note.errorReason ? (
                <Text className='note-text' style={{ color: '#ef4444' }}>
                  错因: {note.errorReason}
                </Text>
              ) : null}

              {note.isDueForRetry ? (
                <View className='note-footer'>
                  <Text className='due-tag'>
                    {isToday(note.nextRetryDate) ? '今日待复习' : '已逾期'}
                  </Text>
                  <View className='retry-btns'>
                    <View className='retry-btn hard' onClick={() => handleRetry(note.problemId, 1)}>
                      <Text>困难</Text>
                    </View>
                    <View className='retry-btn good' onClick={() => handleRetry(note.problemId, 3)}>
                      <Text>一般</Text>
                    </View>
                    <View className='retry-btn easy' onClick={() => handleRetry(note.problemId, 5)}>
                      <Text>熟练</Text>
                    </View>
                  </View>
                </View>
              ) : null}
            </View>
          ))
        )}
      </View>
    </View>
  )
}
