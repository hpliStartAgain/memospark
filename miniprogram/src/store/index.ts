import Taro from '@tarojs/taro'
import type { UserDto } from '../types'

const TOKEN_KEY = 'ms_token'
const USER_KEY = 'ms_user'

export const store = {
  getToken(): string | null {
    return Taro.getStorageSync(TOKEN_KEY) || null
  },

  setToken(token: string): void {
    Taro.setStorageSync(TOKEN_KEY, token)
  },

  getUser(): UserDto | null {
    const raw = Taro.getStorageSync(USER_KEY)
    if (!raw) return null
    try {
      return typeof raw === 'string' ? JSON.parse(raw) : raw
    } catch {
      return null
    }
  },

  setUser(user: UserDto): void {
    Taro.setStorageSync(USER_KEY, JSON.stringify(user))
  },

  clear(): void {
    Taro.removeStorageSync(TOKEN_KEY)
    Taro.removeStorageSync(USER_KEY)
  },

  isLoggedIn(): boolean {
    return !!this.getToken()
  },
}
