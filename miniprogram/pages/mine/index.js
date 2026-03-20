const { getProfile, updateProfile, cacheProfilePatch } = require('../../utils/user-api')
const { forceWechatLogin, getAuthMode } = require('../../utils/request')

function getWechatProfileSafe() {
  return new Promise((resolve) => {
    if (typeof wx.getUserProfile !== 'function') {
      resolve(null)
      return
    }
    wx.getUserProfile({
      desc: '用于完善会员资料',
      success: (res) => resolve((res && res.userInfo) || null),
      fail: () => resolve(null)
    })
  })
}

function extractWechatUserInfo(event) {
  const detail = (event && event.detail) || {}
  return detail.userInfo || null
}


Page({
  data: {
    profile: {
      nickname: '诗语访客',
      memberLevel: '银卡会员',
      phone: '',
      avatarUrl: '',
      points: 0,
      couponCount: 0,
      favoriteCount: 0,
      footprintCount: 0,
      orderCount: 0,
      cardBalance: 0
    },
    avatarText: '语',
    orderStatusList: [
      { label: '待付款', status: 'pending', icon: '💳' },
      { label: '待发货', status: 'to_ship', icon: '📦' },
      { label: '待收货', status: 'shipped', icon: '🚚' },
      { label: '已完成', status: 'completed', icon: '📝' },
      { label: '售后', type: 'aftersale', icon: '↩' }
    ],
    assetCards: [
      { key: 'points', label: '积分', url: '/pages/marketing/points/index', icon: '🏅' },
      { key: 'couponCount', label: '优惠券', url: '/pages/marketing/coupon/index', icon: '🎁' },
      { key: 'cardBalance', label: '卡券', url: '/pages/marketing/gift-card/index', icon: '🎫' }
    ],
    serviceMenus: [
      { label: '充值中心', url: '/pages/marketing/points/index', icon: '¥', color: 'orange' },
      { label: '领券中心', url: '/pages/marketing/coupon/index', icon: '🎫', color: 'red' },
      { label: '分销中心', url: '/pages/marketing/activity/index', icon: '👥', color: 'purple' },
      { label: '绑定手机号', url: '/pages/user/bind-mobile/index', icon: '📱', color: 'green' }
    ],
    authMode: getAuthMode(),
    isLogging: false,
    canIUseGetUserProfile: true
  },
  onLoad() {
    this.setData({ canIUseGetUserProfile: typeof wx.getUserProfile === 'function' })
  },
  async onShow() {
    this.syncTabBar(3)
    const app = getApp()
    const forceGuest = !!wx.getStorageSync('YH_FORCE_GUEST')
    const mode = forceGuest ? 'guest' : ((app && app.globalData && app.globalData.authMode) || getAuthMode())

    if (mode === 'guest') {
      this.setData({
        authMode: 'guest',
        profile: {
          nickname: '诗语访客',
          memberLevel: '银卡会员',
          phone: '',
          avatarUrl: '',
          points: 0,
          couponCount: 0,
          favoriteCount: 0,
          footprintCount: 0,
          orderCount: 0,
          cardBalance: 0
        },
        avatarText: '语'
      })
      return
    }

    this.setData({ authMode: mode })

    try {
      const profile = await getProfile()
      this.setData({
        profile,
        avatarText: (profile.nickname || '诗语访客').charAt(0),
        authMode: 'wechat'
      })
      if (app && app.globalData) {
        app.globalData.authMode = 'wechat'
      }
    } catch (e) {
      this.setData({ authMode: 'guest' })
    }
  },
  syncTabBar(index) {
    if (typeof this.getTabBar !== 'function') return
    const tabBar = this.getTabBar()
    if (tabBar && typeof tabBar.setData === 'function') {
      tabBar.setData({ selected: index })
    }
  },
  async doWechatLogin(wechatInfo = null) {
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
            avatarUrl: wechatInfo.avatarUrl || undefined
          })
        } catch (_) {}
      }

      const profile = await getProfile()
      const app = getApp()
      if (app && typeof app.markWechatLoginSuccess === 'function') {
        app.markWechatLoginSuccess()
      } else if (app && app.globalData) {
        app.globalData.authMode = 'wechat'
      }

      this.setData({
        authMode: 'wechat',
        profile,
        avatarText: (profile.nickname || '诗语访客').charAt(0)
      })
      wx.setStorageSync('YH_USER_PROFILE_AUTH', 1)
      wx.removeStorageSync('YH_FORCE_GUEST')
      wx.showToast({ title: '微信登录成功', icon: 'success' })
    } catch (e) {
      this.setData({ authMode: 'guest' })
      const msg = (e && (e.message || e.errMsg)) || '登录失败，当前为访客模式'
      wx.showToast({ title: msg, icon: 'none' })
    } finally {

      wx.hideLoading()
      this.setData({ isLogging: false })
    }
  },
  async onWechatLogin() {
    const wechatInfo = await getWechatProfileSafe()
    if (!wechatInfo) {
      wx.showToast({ title: '未获取到微信信息', icon: 'none' })
      return
    }
    await this.doWechatLogin(wechatInfo)
  },
  async wxLogin(event) {
    const wechatInfo = extractWechatUserInfo(event)
    if (!wechatInfo) {
      wx.showToast({ title: '微信登录失败', icon: 'none' })
      return
    }
    await this.doWechatLogin(wechatInfo)
  },
  onTapAvatar() {
    if (this.data.authMode === 'guest') {
      wx.navigateTo({ url: '/pages/auth/login/index?from=mine-avatar' })
      return
    }
    this.onTapSetting()
  },
  onTapSetting() {
    wx.navigateTo({
      url: '/pages/user/profile/index',
      fail: () => {
        wx.showToast({ title: '设置页暂不可用', icon: 'none' })
      }
    })
  },
  onTapAddress() {
    wx.navigateTo({
      url: '/pages/address/list/index',
      fail: () => {
        wx.showToast({ title: '地址页暂不可用', icon: 'none' })
      }
    })
  },
  onTapQrcode() {
    wx.showToast({ title: '会员码功能即将上线', icon: 'none' })
  },

  onTapOrder() {
    wx.navigateTo({ url: '/pages/order/list/index' })
  },
  onTapOrderStatus(event) {
    const { status, type } = event.currentTarget.dataset
    if (type === 'aftersale') {
      wx.navigateTo({ url: '/pages/aftersale/list/index' })
      return
    }
    const url = status ? `/pages/order/list/index?status=${status}` : '/pages/order/list/index'
    wx.navigateTo({ url })
  },
  onTapAsset(event) {
    const { url } = event.currentTarget.dataset
    if (url) {
      wx.navigateTo({ url })
    }
  },
  onTapService(event) {
    const { url } = event.currentTarget.dataset
    if (url) {
      wx.navigateTo({ url })
    }
  },
  onTapStat(event) {
    const { type } = event.currentTarget.dataset
    if (type === 'favorite') {
      wx.navigateTo({ url: '/pages/user/favorites/index' })
      return
    }
    wx.navigateTo({ url: '/pages/user/history/index' })
  }
})
