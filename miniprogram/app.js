const { clearAuthCache, getAuthMode, hasLocalToken } = require('./utils/request')

const USER_PROFILE_AUTH_KEY = 'YH_USER_PROFILE_AUTH'
const FORCE_GUEST_KEY = 'YH_FORCE_GUEST'
const AUTH_GUIDE_SILENT_MS = 8000


App({
  globalData: {
    brandName: '诗语家居（Yu Home）',
    authMode: 'guest',
    loginReady: false
  },

  onLaunch() {
    const forceGuest = !!wx.getStorageSync(FORCE_GUEST_KEY)
    const profileAuthed = !!wx.getStorageSync(USER_PROFILE_AUTH_KEY)
    const mode = getAuthMode()
    const hasToken = hasLocalToken()
    const validWechatLogin = !forceGuest && profileAuthed && mode === 'wechat' && hasToken

    // 如果登录状态不完整，清除所有认证相关缓存（包括 USER_PROFILE_AUTH_KEY）
    // 这样才能在下次进入时正确显示授权弹窗
    if (!validWechatLogin && (mode === 'wechat' || hasToken || profileAuthed)) {
      clearAuthCache()
      wx.removeStorageSync(USER_PROFILE_AUTH_KEY)
    }

    this._authGuideShown = false
    this._authGuideTimer = null
    this._authGuideSilentUntil = 0
    this.globalData.authMode = validWechatLogin ? 'wechat' : 'guest'
    this.globalData.loginReady = true
  },

  suppressAuthGuide(ms = AUTH_GUIDE_SILENT_MS) {
    const nextUntil = Date.now() + Number(ms || AUTH_GUIDE_SILENT_MS)
    this._authGuideSilentUntil = Math.max(this._authGuideSilentUntil || 0, nextUntil)
  },

  markWechatLoginSuccess() {
    this.globalData.authMode = 'wechat'
    this._authGuideShown = true
    this.suppressAuthGuide(15000)
    if (this._authGuideTimer) {
      clearTimeout(this._authGuideTimer)
      this._authGuideTimer = null
    }
  },

  onShow() {
    // 注意：App.onShow 时页面栈为空，maybeShowAuthGuide 会因 getCurrentPages() 返回空而退出
    // 弹窗触发依赖 Page.onShow 中的调用
  },

  maybeShowAuthGuide(options = {}) {
    const force = !!(options && options.force)

    // 先检查页面栈，避免在 App.onShow 时执行后续逻辑
    const pages = getCurrentPages()
    const current = pages[pages.length - 1]
    const route = (current && current.route) || ''
    if (!current || !route) return
    if (route.indexOf('pages/auth/login/index') === 0) return

    // 再检查其他条件
    if (!force && this._authGuideShown) return
    if (Date.now() < (this._authGuideSilentUntil || 0)) return
    if (this.globalData.authMode === 'wechat') return
    if (getAuthMode() === 'wechat' && hasLocalToken()) return
    if (wx.getStorageSync(USER_PROFILE_AUTH_KEY)) return

    this._authGuideShown = true
    if (this._authGuideTimer) {
      clearTimeout(this._authGuideTimer)
      this._authGuideTimer = null
    }
    this._authGuideTimer = setTimeout(() => {

      if (this.globalData.authMode === 'wechat') {
        this._authGuideShown = false
        return
      }
      if (wx.getStorageSync(USER_PROFILE_AUTH_KEY)) {
        this._authGuideShown = false
        return
      }
      this._authGuideTimer = null
      wx.showModal({

        title: '微信授权登录',
        content: '首次使用建议完成微信授权，用于同步头像和昵称。是否现在授权？',
        confirmText: '去授权',
        cancelText: '先逛逛',
        success: (res) => {
          if (!res.confirm) return
          this.suppressAuthGuide(30000)
          wx.navigateTo({
            url: '/pages/auth/login/index?from=launch',
            fail: () => {
              wx.switchTab({ url: '/pages/mine/index' })
            }
          })
        },
        fail: () => {
          this._authGuideShown = false
        }
      })
    }, 120)
  }
})
