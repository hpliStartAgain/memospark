import { Component } from 'react'
import Taro from '@tarojs/taro'
import { store } from './store'
import { authApi } from './api'
import './app.scss'

class App extends Component {
  componentDidMount() {
    this.autoLogin()
  }

  async autoLogin() {
    if (store.isLoggedIn()) {
      return
    }

    try {
      const res = await new Promise<WechatMiniprogram.LoginSuccessCallbackResult>(
        (resolve, reject) =>
          Taro.login({
            success: resolve,
            fail: reject,
          })
      )

      const { token, user } = await authApi.wxLogin(res.code)
      store.setToken(token)
      store.setUser(user)
    } catch {
      // WeChat login unavailable (H5 or unconfigured) — redirect to manual login
      Taro.reLaunch({ url: '/pages/login/index' })
    }
  }

  render() {
    return this.props.children
  }
}

export default App
