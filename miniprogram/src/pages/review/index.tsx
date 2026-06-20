import { useState, useEffect, useRef } from 'react'
import Taro from '@tarojs/taro'
import { View, Text, Button } from '@tarojs/components'
import { reviewApi } from '../../api'
import { store } from '../../store'
import type { ReviewCard } from '../../types'
import './index.scss'

type Phase = 'loading' | 'front' | 'back' | 'done' | 'empty'

export default function ReviewPage() {
  const [cards, setCards] = useState<ReviewCard[]>([])
  const [index, setIndex] = useState(0)
  const [phase, setPhase] = useState<Phase>('loading')
  const [reviewed, setReviewed] = useState(0)
  const startTs = useRef<number>(0)

  useEffect(() => {
    if (!store.isLoggedIn()) {
      Taro.reLaunch({ url: '/pages/login/index' })
      return
    }
    loadCards()
  }, [])

  Taro.useDidShow(() => {
    // Only reload if we're in the done/empty state (coming back from another tab)
    if (phase === 'done' || phase === 'empty') loadCards()
  })

  async function loadCards() {
    setPhase('loading')
    setIndex(0)
    setReviewed(0)
    try {
      const data = await reviewApi.today()
      setCards(data)
      setPhase(data.length === 0 ? 'empty' : 'front')
      startTs.current = Date.now()
    } catch {
      setPhase('empty')
    }
  }

  function flipCard() {
    if (phase === 'front') setPhase('back')
  }

  async function rate(quality: number) {
    const card = cards[index]
    if (!card) return
    const elapsed = Date.now() - startTs.current
    try {
      await reviewApi.submit(card.cardId, quality, elapsed)
    } catch {
      // non-blocking: allow session to continue
    }

    const next = index + 1
    setReviewed((r) => r + 1)
    if (next >= cards.length) {
      setPhase('done')
    } else {
      setIndex(next)
      setPhase('front')
      startTs.current = Date.now()
    }
  }

  const current = cards[index]
  const progress = cards.length > 0 ? (index / cards.length) * 100 : 0

  // ── Loading ─────────────────────────────────────────────────────────────
  if (phase === 'loading') {
    return (
      <View className='review-page'>
        <View className='loading-spinner'><Text>加载中...</Text></View>
      </View>
    )
  }

  // ── Empty ────────────────────────────────────────────────────────────────
  if (phase === 'empty') {
    return (
      <View className='review-page'>
        <View className='empty-review'>
          <Text className='empty-icon'>🎉</Text>
          <Text className='empty-title'>今日已全部复习！</Text>
          <Text className='empty-sub'>太棒了，明天继续保持</Text>
        </View>
      </View>
    )
  }

  // ── Done ─────────────────────────────────────────────────────────────────
  if (phase === 'done') {
    return (
      <View className='review-page'>
        <View className='done-screen'>
          <Text className='done-icon'>🏆</Text>
          <Text className='done-title'>本轮复习完成</Text>
          <Text className='done-sub'>坚持复习，面试必胜！</Text>
          <View className='done-stats'>
            <View className='ds-item'>
              <Text className='ds-value'>{reviewed}</Text>
              <Text className='ds-label'>已复习</Text>
            </View>
            <View className='ds-item'>
              <Text className='ds-value'>{cards.length}</Text>
              <Text className='ds-label'>总数</Text>
            </View>
          </View>
          <Button className='btn-primary' onClick={loadCards}>
            继续复习
          </Button>
        </View>
      </View>
    )
  }

  // ── Flashcard ─────────────────────────────────────────────────────────────
  return (
    <View className='review-page'>
      <View className='review-header'>
        <View>
          <Text className='progress-text'>
            {index + 1} / {cards.length}
          </Text>
          {current?.deckName ? (
            <Text className='deck-name'>{current.deckName}</Text>
          ) : null}
        </View>
        <Text className='skip-btn' onClick={() => rate(0)}>跳过</Text>
      </View>

      <View className='progress-bar-wrap'>
        <View className='progress-bar-fill' style={{ width: `${progress}%` }} />
      </View>

      <View className='card-area'>
        <View className='flashcard' onClick={flipCard}>
          <Text className='card-side-label'>
            {phase === 'front' ? 'Q' : 'A'}
          </Text>

          <Text className='card-content'>
            {phase === 'front' ? current?.front : current?.back}
          </Text>

          {phase === 'front' && current?.hint ? (
            <Text className='card-hint'>提示: {current.hint}</Text>
          ) : null}

          {phase === 'front' ? (
            <Text className='tap-hint'>点击翻转查看答案</Text>
          ) : null}
        </View>

        {phase === 'back' ? (
          <View className='rating-area'>
            <Text className='rating-label'>掌握程度如何？</Text>
            <View className='rating-btns'>
              <View className='rating-btn btn-again' onClick={() => rate(0)}>
                <Text className='btn-label'>再来</Text>
                <Text className='btn-sub'>完全不会</Text>
              </View>
              <View className='rating-btn btn-hard' onClick={() => rate(1)}>
                <Text className='btn-label'>困难</Text>
                <Text className='btn-sub'>勉强记得</Text>
              </View>
              <View className='rating-btn btn-good' onClick={() => rate(3)}>
                <Text className='btn-label'>一般</Text>
                <Text className='btn-sub'>大致掌握</Text>
              </View>
              <View className='rating-btn btn-easy' onClick={() => rate(5)}>
                <Text className='btn-label'>简单</Text>
                <Text className='btn-sub'>轻松搞定</Text>
              </View>
            </View>
          </View>
        ) : null}
      </View>
    </View>
  )
}
