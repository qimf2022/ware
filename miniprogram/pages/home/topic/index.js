const { getCampaignList } = require('../../../utils/user-store')

Page({
  data: {
    campaigns: []
  },
  onLoad() {
    this.setData({ campaigns: getCampaignList() })
  },
  onGoCoupon() {
    wx.navigateTo({ url: '/pages/marketing/coupon/index' })
  },
  onGoActivity() {
    wx.navigateTo({ url: '/pages/marketing/activity/index' })
  }
})
