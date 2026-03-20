const { getCampaignList } = require('../../../utils/user-store')

Page({
  data: {
    list: []
  },
  onLoad() {
    this.setData({ list: getCampaignList() })
  },
  onGoCoupon() {
    wx.navigateTo({ url: '/pages/marketing/coupon/index' })
  },
  onGoHome() {
    wx.switchTab({ url: '/pages/home/index' })
  }
})
