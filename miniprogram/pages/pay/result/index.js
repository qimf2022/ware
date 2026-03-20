Page({
  data: {
    status: 'success',
    orderId: '',
    orderNo: ''
  },
  onLoad(options) {
    this.setData({
      status: options.status || 'success',
      orderId: options.orderId || '',
      orderNo: options.orderNo || ''
    })
  },
  onViewOrder() {
    if (!this.data.orderId) {
      wx.showToast({ title: '订单信息不存在', icon: 'none' })
      return
    }
    wx.navigateTo({ url: `/pages/order/detail/index?id=${this.data.orderId}` })
  },
  onViewOrderList() {
    wx.navigateTo({ url: '/pages/order/list/index' })
  },
  onContinueShopping() {
    wx.switchTab({ url: '/pages/home/index' })
  }
})
