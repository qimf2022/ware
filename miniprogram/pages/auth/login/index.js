const { forceWechatLogin } = require('../../../utils/request')
const { updateProfile, cacheProfilePatch } = require('../../../utils/user-api')

Page({
  data: {
    isLogging: false
  },

  onLoad() {
    const app = getApp()
    if (app && typeof app.suppressAuthGuide === 'function') {
      app.suppressAuthGuide(30000)
    }
  },

  async doLogin(wechatInfo = null) {
    if (this.data.isLogging) return
    this.setData({ isLogging: true })
    wx.showLoading({ title: '登录中', mask: true })
    try {
      await forceWechatLogin(wechatInfo)

      if (wechatInfo && (wechatInfo.nickName || wechatInfo.avatarUrl)) {
        cacheProfilePatch({
          nickname: wechatInfo.nickName || '',
          avatarUrl: wechatInfo.avatarUrl || ''
        })
        try {
          await updateProfile({
            nickname: wechatInfo.nickName || undefined,
            avatarUrl: wechatInfo.avatarUrl || undefined,
            gender: typeof wechatInfo.gender === 'number' ? wechatInfo.gender : undefined
          })
        } catch (_) {}
      }

      wx.setStorageSync('YH_USER_PROFILE_AUTH', 1)
      wx.removeStorageSync('YH_FORCE_GUEST')
      const app = getApp()
      if (app && typeof app.markWechatLoginSuccess === 'function') {
        app.markWechatLoginSuccess()
      } else if (app && app.globalData) {
        app.globalData.authMode = 'wechat'
      }

      wx.showToast({ title: '登录成功', icon: 'success' })
      setTimeout(() => {
        wx.switchTab({ url: '/pages/home/index' })
      }, 260)
    } catch (e) {
      const msg = (e && (e.message || e.errMsg)) || '微信登录失败'
      wx.showToast({ title: msg, icon: 'none' })
    } finally {

      wx.hideLoading()
      this.setData({ isLogging: false })
    }
  },

  onWechatAuthorizeTap() {
    if (this.data.isLogging) return
    if (typeof wx.getUserProfile !== 'function') {
      wx.showToast({ title: '当前微信版本不支持授权', icon: 'none' })
      return
    }
    wx.getUserProfile({
      desc: '用于完善会员资料',
      success: (res) => {
        const userInfo = (res && res.userInfo) || null
        if (!userInfo) {
          wx.showToast({ title: '未获取到微信信息', icon: 'none' })
          return
        }
        this.doLogin(userInfo)
      },
      fail: () => {
        wx.showToast({ title: '你已取消授权', icon: 'none' })
      }
    })
  },

  accountLogin() {
    wx.showToast({ title: '账号登录开发中', icon: 'none' })
  }
})
