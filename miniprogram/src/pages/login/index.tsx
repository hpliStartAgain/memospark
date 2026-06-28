import { useState } from 'react'
import Taro from '@tarojs/taro'
import { View, Text, Input, Button } from '@tarojs/components'
import { authApi } from '../../api'
import { store } from '../../store'
import './index.scss'

export default function LoginPage() {
  const [tab, setTab] = useState<'wx' | 'pwd'>('wx')
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const handleWxLogin = async () => {
    setLoading(true)
    setError('')
    try {
      const loginRes = await new Promise<any>((resolve, reject) =>
        Taro.login({ success: resolve, fail: reject })
      )
      const { token, user } = await authApi.wxLogin(loginRes.code)
      store.setToken(token)
      store.setUser(user)
      Taro.switchTab({ url: '/pages/index/index' })
    } catch (e: any) {
      setError(e?.message || '微信登录失败，请重试')
    } finally {
      setLoading(false)
    }
  }

  const handlePwdLogin = async () => {
    if (!username.trim() || !password.trim()) {
      setError('请输入用户名和密码')
      return
    }
    setLoading(true)
    setError('')
    try {
      const { token, user } = await authApi.tokenLogin(username.trim(), password)
      store.setToken(token)
      store.setUser(user)
      Taro.switchTab({ url: '/pages/index/index' })
    } catch (e: any) {
      setError(e?.message || '用户名或密码错误')
    } finally {
      setLoading(false)
    }
  }

  return (
    <View className='login-page'>
      <View className='logo-area'>
        <View className='logo-icon'>
          <Text>⚡</Text>
        </View>
        <Text className='app-name'>MemoSpark</Text>
        <Text className='app-slogan'>备战面试，高效复习</Text>
      </View>

      <View className='login-card'>
        {tab === 'wx' ? (
          <>
            <Button
              className='wx-login-btn'
              loading={loading}
              onClick={handleWxLogin}
            >
              微信一键登录
            </Button>

            <View className='divider'>
              <View className='line' />
              <Text>或账号密码登录</Text>
              <View className='line' />
            </View>

            <Button
              className='login-submit-btn'
              style={{ background: '#64748b' }}
              onClick={() => setTab('pwd')}
            >
              账号密码登录
            </Button>
          </>
        ) : (
          <>
            <View className='input-group'>
              <Text className='input-label'>用户名</Text>
              <Input
                className='input-field'
                placeholder='请输入用户名'
                value={username}
                onInput={(e) => setUsername(e.detail.value)}
              />
            </View>

            <View className='input-group'>
              <Text className='input-label'>密码</Text>
              <Input
                className='input-field'
                placeholder='请输入密码'
                password
                value={password}
                onInput={(e) => setPassword(e.detail.value)}
              />
            </View>

            <Button
              className='login-submit-btn'
              loading={loading}
              onClick={handlePwdLogin}
            >
              登录
            </Button>

            <Button
              style={{ marginTop: '20px', background: 'transparent', color: '#da7756' }}
              onClick={() => setTab('wx')}
            >
              ← 返回微信登录
            </Button>
          </>
        )}

        {error ? <Text className='error-msg'>{error}</Text> : null}
      </View>
    </View>
  )
}
