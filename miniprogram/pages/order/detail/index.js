const { getOrderDetail } = require('../../../utils/trade-api')

Page({
  data: {
    order: null,
    pageState: 'loading',
    afterSaleRecord: null,
    orderId: ''
  },
  onLoad(options) {
    this.setData({ orderId: options.id || '' })
    this.loadData()
  },
  onShow() {
    if (this.data.orderId) {
      this.loadData()
    }
  },
  async loadData() {
    try {
      const order = await getOrderDetail(this.data.orderId || '')
      this.setData({
        order,
        afterSaleRecord: order.afterSaleRecord,
        pageState: order ? 'ready' : 'empty'
      })
    } catch (e) {
      this.setData({ pageState: 'empty' })
    }
  },
  onApplyAfterSale() {
    if (!this.data.order) return
    const firstItem = (this.data.order.items || [])[0]
    if (!firstItem) {
      wx.showToast({ title: '订单无可售后商品', icon: 'none' })
      return
    }
    wx.navigateTo({ url: `/pages/after-sale/apply/index?orderId=${this.data.order.id}&orderItemId=${firstItem.orderItemId}` })
  },
  onViewAfterSale() {
    if (!this.data.order || !this.data.afterSaleRecord) return
    wx.navigateTo({ url: `/pages/after-sale/detail/index?id=${this.data.afterSaleRecord.id}` })
  },
  onViewLogistics() {
    if (!this.data.order) return
    wx.navigateTo({ url: `/pages/order/logistics/index?orderId=${this.data.order.id}` })
  }
})
