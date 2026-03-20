const { getCouponCenter, claimCoupon } = require('../../../utils/user-api')

Page({
  data: {
    list: [],
    pageState: 'loading'
  },
  onShow() {
    this.loadCoupons()
  },
  async loadCoupons() {
    try {
      const data = await getCouponCenter(1, 50)
      const list = data.list || []
      this.setData({
        list,
        pageState: list.length ? 'ready' : 'empty'
      })
    } catch (e) {
      this.setData({ pageState: 'error' })
      wx.showToast({ title: e.message || '优惠券加载失败', icon: 'none' })
    }
  },
  async onClaim(event) {
    const { id, claimed } = event.currentTarget.dataset
    if (claimed) {
      wx.showToast({ title: '已领取', icon: 'none' })
      return
    }
    try {
      await claimCoupon(id)
      wx.showToast({ title: '领取成功', icon: 'success' })
      await this.loadCoupons()
    } catch (e) {
      wx.showToast({ title: e.message || '领取失败', icon: 'none' })
    }
  }
})
