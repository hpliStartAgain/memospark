import { useState, useEffect } from 'react'
import Taro, { useRouter } from '@tarojs/taro'
import { View, Text, ScrollView } from '@tarojs/components'
import { targetApi } from '../../api'
import type { TargetDetail } from '../../types'
import './index.scss'

function statusLabel(status: string) {
  if (status === 'PREPARING') return '备战中'
  if (status === 'INTERVIEWING') return '面试中'
  return '已结束'
}

function LevelDots({ level }: { level: number }) {
  const MAX = 5
  return (
    <View className='level-dots'>
      {Array.from({ length: MAX }).map((_, i) => (
        <View key={i} className={`dot ${i < level ? 'filled' : ''}`} />
      ))}
    </View>
  )
}

export default function TargetDetailPage() {
  const router = useRouter()
  const id = Number(router.params.id)

  const [detail, setDetail] = useState<TargetDetail | null>(null)
  const [loading, setLoading] = useState(true)
  const [analyzing, setAnalyzing] = useState(false)

  useEffect(() => {
    if (!id) return
    loadDetail()
  }, [id])

  async function loadDetail() {
    setLoading(true)
    try {
      const data = await targetApi.get(id)
      setDetail(data)
    } catch {
      Taro.showToast({ title: '加载失败', icon: 'none' })
    } finally {
      setLoading(false)
    }
  }

  async function handleAnalyze() {
    if (!detail) return
    if (detail.jds.length === 0) {
      Taro.showToast({ title: '请先添加 JD', icon: 'none' })
      return
    }
    setAnalyzing(true)
    try {
      await targetApi.analyze(id, 'zh', true)
      await loadDetail()
      Taro.showToast({ title: 'AI 分析完成', icon: 'success' })
    } catch (e: any) {
      Taro.showToast({ title: e?.message || 'AI 分析失败', icon: 'none' })
    } finally {
      setAnalyzing(false)
    }
  }

  async function updateSkillLevel(skillId: number, delta: number) {
    if (!detail) return
    const skill = detail.skills.find((s) => s.id === skillId)
    if (!skill) return
    const newLevel = Math.max(0, Math.min(5, skill.selfLevel + delta))
    try {
      await targetApi.updateSkill(id, skillId, { selfLevel: newLevel })
      setDetail((prev) =>
        prev
          ? {
              ...prev,
              skills: prev.skills.map((s) =>
                s.id === skillId ? { ...s, selfLevel: newLevel } : s
              ),
            }
          : prev
      )
    } catch {
      Taro.showToast({ title: '更新失败', icon: 'none' })
    }
  }

  if (loading) {
    return (
      <View className='detail-page'>
        <View className='loading-spinner'><Text>加载中...</Text></View>
      </View>
    )
  }

  if (!detail) {
    return (
      <View className='detail-page'>
        <View className='empty-state'><Text>目标不存在</Text></View>
      </View>
    )
  }

  const r = detail.readiness

  return (
    <ScrollView className='detail-page' scrollY>
      {/* Header */}
      <View className='detail-header'>
        <Text className='company-name'>{detail.company}</Text>
        <Text className='position-name'>{detail.title}</Text>
        <View className='header-meta'>
          <Text className='status-tag'>{statusLabel(detail.status)}</Text>
          {detail.interviewDate ? (
            <Text className='date-tag'>📅 {detail.interviewDate.substring(0, 10)}</Text>
          ) : null}
        </View>
      </View>

      {/* Readiness banner */}
      <View className='readiness-banner'>
        <View className='rb-scores'>
          <View className='rb-score-item'>
            <Text className='rb-value'>{r?.skillCoverage ?? 0}</Text>
            <Text className='rb-label'>技能</Text>
          </View>
          <View className='rb-score-item'>
            <Text className='rb-value'>{r?.cardHealth ?? 0}</Text>
            <Text className='rb-label'>卡片</Text>
          </View>
          <View className='rb-score-item'>
            <Text className='rb-value'>{r?.wrongClear ?? 0}</Text>
            <Text className='rb-label'>错题</Text>
          </View>
        </View>
        <View className='rb-overall'>
          <Text className='rb-big'>{r?.overall ?? 0}%</Text>
          <Text className='rb-big-label'>综合准备度</Text>
        </View>
      </View>

      {/* AI Analyze */}
      <View style={{ marginTop: '28px' }}>
        <View
          className='analyze-btn'
          style={analyzing ? { opacity: 0.6 } : {}}
          onClick={analyzing ? undefined : handleAnalyze}
        >
          <Text>{analyzing ? '⏳ AI 分析中...' : '✨ AI 分析 JD，生成技能图谱'}</Text>
        </View>
      </View>

      {/* Skills */}
      <View className='section'>
        <View className='section-header'>
          <Text className='section-title'>技能清单 ({detail.skills.length})</Text>
        </View>
        {detail.skills.length === 0 ? (
          <View className='empty-state'>
            <Text>暂无技能，点击上方 AI 分析自动生成</Text>
          </View>
        ) : (
          detail.skills.map((skill) => (
            <View key={skill.id} className='skill-item'>
              <View className='skill-info'>
                <Text className='skill-name'>{skill.name}</Text>
                {skill.description ? (
                  <Text className='skill-desc'>{skill.description}</Text>
                ) : null}
              </View>
              <View className='skill-level'>
                <LevelDots level={skill.selfLevel} />
                <View style={{ display: 'flex', gap: '16px', marginTop: '8px' }}>
                  <Text
                    style={{ fontSize: '32px', color: '#94a3b8', padding: '4px 12px' }}
                    onClick={() => updateSkillLevel(skill.id, -1)}
                  >
                    −
                  </Text>
                  <Text
                    style={{ fontSize: '32px', color: '#0ea5e9', padding: '4px 12px' }}
                    onClick={() => updateSkillLevel(skill.id, 1)}
                  >
                    +
                  </Text>
                </View>
              </View>
            </View>
          ))
        )}
      </View>

      {/* JDs */}
      <View className='section'>
        <View className='section-header'>
          <Text className='section-title'>职位描述 ({detail.jds.length})</Text>
        </View>
        {detail.jds.length === 0 ? (
          <View className='empty-state'>
            <Text>暂无 JD，可在 Web 端添加后 AI 分析</Text>
          </View>
        ) : (
          detail.jds.map((jd) => (
            <View key={jd.id} className='jd-item'>
              <Text className='jd-title'>{jd.title}</Text>
              <Text className='jd-preview'>
                {jd.content.substring(0, 80)}
                {jd.content.length > 80 ? '...' : ''}
              </Text>
              <Text className='jd-date'>{jd.createdAt.substring(0, 10)}</Text>
            </View>
          ))
        )}
      </View>
    </ScrollView>
  )
}
