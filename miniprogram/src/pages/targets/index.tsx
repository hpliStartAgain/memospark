import { useState, useEffect } from 'react'
import Taro from '@tarojs/taro'
import { View, Text, Input } from '@tarojs/components'
import { targetApi } from '../../api'
import { store } from '../../store'
import type { TargetSummary } from '../../types'
import { targetStatusClass, targetStatusLabel } from '../../utils/targetStatus'
import './index.scss'

function readinessClass(score: number) {
  if (score >= 70) return 'high'
  if (score >= 40) return 'medium'
  return 'low'
}

export default function TargetsPage() {
  const [targets, setTargets] = useState<TargetSummary[]>([])
  const [loading, setLoading] = useState(true)
  const [showModal, setShowModal] = useState(false)
  const [company, setCompany] = useState('')
  const [position, setPosition] = useState('')
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    if (!store.isLoggedIn()) {
      Taro.reLaunch({ url: '/pages/login/index' })
      return
    }
    loadTargets()
  }, [])

  Taro.useDidShow(() => {
    loadTargets()
  })

  async function loadTargets() {
    setLoading(true)
    try {
      const data = await targetApi.list()
      setTargets(data)
    } catch {
      // ignore
    } finally {
      setLoading(false)
    }
  }

  async function handleCreate() {
    if (!company.trim() || !position.trim()) {
      Taro.showToast({ title: '请填写公司和岗位', icon: 'none' })
      return
    }
    setSaving(true)
    try {
      await targetApi.create({ title: position.trim(), company: company.trim() })
      setShowModal(false)
      setCompany('')
      setPosition('')
      await loadTargets()
      Taro.showToast({ title: '创建成功', icon: 'success' })
    } catch (e: any) {
      Taro.showToast({ title: e?.message || '创建失败', icon: 'none' })
    } finally {
      setSaving(false)
    }
  }

  return (
    <View className='targets-page'>
      <View className='page-header'>
        <Text className='header-title'>备战目标</Text>
        <View className='add-btn' onClick={() => setShowModal(true)}>
          <Text>+</Text>
        </View>
      </View>

      <View className='target-list'>
        {loading ? (
          <View className='loading-spinner'><Text>加载中...</Text></View>
        ) : targets.length === 0 ? (
          <View className='empty-state'>
            <Text>🎯</Text>
            <Text>还没有备战目标</Text>
            <Text>点击右上角 + 添加</Text>
          </View>
        ) : (
          targets.map((t) => (
            <View
              key={t.id}
              className='target-card'
              onClick={() =>
                Taro.navigateTo({
                  url: `/pages/target-detail/index?id=${t.id}`,
                })
              }
            >
              <View className='target-body'>
                <Text className='company'>{t.company}</Text>
                <Text className='position'>{t.title}</Text>
                <View className='target-meta'>
                  <Text className={`status-badge ${targetStatusClass(t.status)}`}>
                    {targetStatusLabel(t.status)}
                  </Text>
                  <Text className='meta-text'>{t.skillCount} 技能</Text>
                  {t.interviewDate ? (
                    <Text className='meta-text'>
                      📅 {t.interviewDate.substring(0, 10)}
                    </Text>
                  ) : null}
                </View>
              </View>
              <View className={`readiness-badge ${readinessClass(t.readiness)}`}>
                <Text className='score'>{t.readiness}</Text>
                <Text className='score-label'>准备度</Text>
              </View>
            </View>
          ))
        )}
      </View>

      {/* Add target modal */}
      {showModal ? (
        <View className='modal-overlay' onClick={() => setShowModal(false)}>
          <View className='add-modal' onClick={(e) => e.stopPropagation()}>
            <Text className='modal-title'>添加备战目标</Text>

            <View className='input-group'>
              <Text className='input-label'>公司名称</Text>
              <Input
                className='input-field'
                placeholder='例：字节跳动'
                value={company}
                onInput={(e) => setCompany(e.detail.value)}
              />
            </View>

            <View className='input-group'>
              <Text className='input-label'>目标岗位</Text>
              <Input
                className='input-field'
                placeholder='例：后端开发工程师'
                value={position}
                onInput={(e) => setPosition(e.detail.value)}
              />
            </View>

            <View className='modal-actions'>
              <View className='cancel-btn' onClick={() => setShowModal(false)}>
                <Text>取消</Text>
              </View>
              <View
                className='confirm-btn'
                style={saving ? { opacity: 0.6 } : {}}
                onClick={saving ? undefined : handleCreate}
              >
                <Text>{saving ? '创建中...' : '确认创建'}</Text>
              </View>
            </View>
          </View>
        </View>
      ) : null}
    </View>
  )
}
