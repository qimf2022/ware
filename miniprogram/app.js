const { clearAuthCache, getAuthMode, hasLocalToken, silentLogin } = require('./utils/request')

const USER_PROFILE_AUTH_KEY = 'YH_USER_PROFILE_AUTH'
const FORCE_GUEST_KEY = 'YH_FORCE_GUEST'
const AUTH_GUIDE_SILENT_MS = 8000


App({
  globalData: {
    brandName: '诗语家居（Yu Home）',
    authMode: 'guest',
    loginReady: false,
    showAuthModal: false
  },

  onLaunch() {
    const forceGuest = !!wx.getStorageSync(FORCE_GUEST_KEY)
    const profileAuthed = !!wx.getStorageSync(USER_PROFILE_AUTH_KEY)
    const mode = getAuthMode()
    const hasToken = hasLocalToken()

    // 如果用户设置了强制访客模式（主动退出登录），不尝试静默登录
    if (forceGuest) {
      this._authGuideShown = false
      this._authGuideTimer = null
      this._authGuideSilentUntil = 0
      this.globalData.authMode = 'guest'
      this.globalData.loginReady = true
      return
    }

    // 如果用户之前授权过，尝试静默登录
    if (profileAuthed) {
      this.trySilentLogin()
      return
    }

    // 如果登录状态不完整，清除所有认证相关缓存
    if (mode === 'wechat' || hasToken) {
      clearAuthCache()
    }

    this._authGuideShown = false
    this._authGuideTimer = null
    this._authGuideSilentUntil = 0
    this.globalData.authMode = 'guest'
    this.globalData.loginReady = true
  },

  /**
   * 尝试静默登录（已授权过的用户）
   */
  async trySilentLogin() {
    try {
      const token = await silentLogin()
      if (token) {
        this.globalData.authMode = 'wechat'
        this.globalData.loginReady = true
        this._authGuideShown = true
        console.log('静默登录成功')
        return
      }
    } catch (e) {
      console.warn('静默登录失败:', e.message)
    }

    // 静默登录失败，清除状态
    clearAuthCache()
    wx.removeStorageSync(USER_PROFILE_AUTH_KEY)

    this._authGuideShown = false
    this._authGuideTimer = null
    this._authGuideSilentUntil = 0
    this.globalData.authMode = 'guest'
    this.globalData.loginReady = true
  },

  suppressAuthGuide(ms = AUTH_GUIDE_SILENT_MS) {
    const nextUntil = Date.now() + Number(ms || AUTH_GUIDE_SILENT_MS)
    this._authGuideSilentUntil = Math.max(this._authGuideSilentUntil || 0, nextUntil)
  },

  markWechatLoginSuccess() {
    this.globalData.authMode = 'wechat'
    this.globalData.showAuthModal = false
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

  /**
   * 隐藏授权弹窗。
   */
  hideAuthModal() {
    this.globalData.showAuthModal = false
    this._authGuideShown = true
    this.suppressAuthGuide(30000)
  },

  maybeShowAuthGuide(options = {}) {
    const force = !!(options && options.force)

    // 如果静默登录正在进行中，稍后重试
    if (!this.globalData.loginReady) {
      setTimeout(() => {
        this.maybeShowAuthGuide(options)
      }, 100)
      return
    }

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

      // 显示自定义授权弹窗
      this.globalData.showAuthModal = true

      // 通知当前页面更新状态
      if (current && typeof current.onAuthModalStateChange === 'function') {
        current.onAuthModalStateChange(true)
      }
    }, 120)
  }
})
