import { useState, useEffect } from 'react'
import Taro from '@tarojs/taro'
import { View, Text, Button } from '@tarojs/components'
import { statsApi, targetApi, reviewApi } from '../../api'
import { store } from '../../store'
import type { StatsSummary, TargetSummary } from '../../types'
import { isActiveTargetStatus, targetStatusLabel } from '../../utils/targetStatus'
import './index.scss'

export default function DashboardPage() {
  const [stats, setStats] = useState<StatsSummary | null>(null)
  const [primaryTarget, setPrimaryTarget] = useState<TargetSummary | null>(null)
  const [dueCount, setDueCount] = useState(0)
  const [loading, setLoading] = useState(true)

  const user = store.getUser()

  useEffect(() => {
    loadData()
  }, [])

  Taro.useDidShow(() => {
    loadData()
  })

  async function loadData() {
    if (!store.isLoggedIn()) {
      Taro.reLaunch({ url: '/pages/login/index' })
      return
    }
    try {
      const [statsData, targetsData, todayCards] = await Promise.all([
        statsApi.summary(),
        targetApi.list(),
        reviewApi.today(),
      ])
      setStats(statsData)
      setDueCount(todayCards.length)
      const active = targetsData.find((t) => isActiveTargetStatus(t.status))
      setPrimaryTarget(active || targetsData[0] || null)
    } catch {
      // ignore — show empty state
    } finally {
      setLoading(false)
    }
  }

  function getGreeting() {
    const h = new Date().getHours()
    if (h < 12) return '早上好'
    if (h < 18) return '下午好'
    return '晚上好'
  }

  function readinessColor(score: number) {
    if (score >= 70) return '#22c55e'
    if (score >= 40) return '#f59e0b'
    return '#ef4444'
  }

  if (loading) {
    return (
      <View className='dashboard-page'>
        <View className='loading-spinner'>
          <Text>加载中...</Text>
        </View>
      </View>
    )
  }

  return (
    <View className='dashboard-page'>
      <View className='header-band'>
        <Text className='greeting'>
          {getGreeting()}，{user?.username || '同学'} 👋
        </Text>
        <Text className='sub-greeting'>
          {dueCount > 0
            ? `今日还有 ${dueCount} 张卡片待复习`
            : '今日复习已完成，继续保持！'}
        </Text>
      </View>

      {/* Stats strip */}
      <View className='stats-row'>
        <View className='stat-item'>
          <Text className='stat-value'>{dueCount}</Text>
          <Text className='stat-label'>待复习</Text>
        </View>
        <View className='stat-item'>
          <Text className='stat-value'>{stats?.streakDays ?? 0}</Text>
          <Text className='stat-label'>连续天</Text>
        </View>
        <View className='stat-item'>
          <Text className='stat-value'>{stats?.reviewedToday ?? 0}</Text>
          <Text className='stat-label'>今日已复</Text>
        </View>
      </View>

      {/* Quick actions */}
      <View className='section'>
        <Text className='section-title'>快捷操作</Text>
        <View className='quick-actions'>
          <View
            className='action-btn'
            onClick={() => Taro.switchTab({ url: '/pages/review/index' })}
          >
            <Text className='action-icon'>📚</Text>
            <Text className='action-count'>{dueCount}</Text>
            <Text className='action-label'>开始复习</Text>
          </View>
          <View
            className='action-btn'
            onClick={() => Taro.switchTab({ url: '/pages/targets/index' })}
          >
            <Text className='action-icon'>🎯</Text>
            <Text className='action-count'>{primaryTarget?.readiness ?? '--'}</Text>
            <Text className='action-label'>备战进度</Text>
          </View>
          <View
            className='action-btn'
            onClick={() => Taro.switchTab({ url: '/pages/notebook/index' })}
          >
            <Text className='action-icon'>📝</Text>
            <Text className='action-count'>{stats?.totalCards ?? 0}</Text>
            <Text className='action-label'>错题本</Text>
          </View>
        </View>
      </View>

      {/* Primary target */}
      <View className='section'>
        <Text className='section-title'>备战目标</Text>
        {primaryTarget ? (
          <View
            className='primary-target-card'
            onClick={() =>
              Taro.navigateTo({
                url: `/pages/target-detail/index?id=${primaryTarget.id}`,
              })
            }
          >
            <View className='target-info'>
              <Text className='company'>{primaryTarget.company}</Text>
              <Text className='position'>{primaryTarget.title}</Text>
              <Text className='readiness-label'>
                {targetStatusLabel(primaryTarget.status)}
              </Text>
            </View>
            <View className='readiness-ring'>
              {/* SVG ring */}
              <View style={{ position: 'relative', width: '100px', height: '100px' }}>
                <View
                  style={{
                    position: 'absolute',
                    top: '50%',
                    left: '50%',
                    transform: 'translate(-50%, -50%)',
                    fontSize: '28px',
                    fontWeight: '700',
                    color: readinessColor(primaryTarget.readiness),
                  }}
                >
                  <Text>{primaryTarget.readiness}%</Text>
                </View>
              </View>
            </View>
          </View>
        ) : (
          <View className='no-target'>
            <Text className='no-target-icon'>🎯</Text>
            <Text className='no-target-text'>还没有设置备战目标</Text>
            <Button
              className='add-target-btn'
              onClick={() => Taro.switchTab({ url: '/pages/targets/index' })}
            >
              + 添加目标
            </Button>
          </View>
        )}
      </View>
    </View>
  )
}
