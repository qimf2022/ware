const { getOrderDetail, applyAfterSale } = require('../../../utils/trade-api')

const TYPE_MAP = {
  退款: 1,
  退货退款: 2,
  换货: 3
}

Page({
  data: {
    orderId: '',
    orderItemId: '',
    orderNo: '',
    refundAmount: 0,
    type: '退款',
    reason: '',
    desc: ''
  },
  async onLoad(options) {
    const orderId = options.orderId || ''
    const orderItemId = options.orderItemId || ''
    this.setData({ orderId, orderItemId })
    if (!orderId) return
    try {
      const order = await getOrderDetail(orderId)
      const firstItem = (order.items || []).find((item) => item.orderItemId === orderItemId) || (order.items || [])[0]
      this.setData({
        orderNo: order.orderNo,
        orderItemId: firstItem ? firstItem.orderItemId : '',
        refundAmount: firstItem ? firstItem.subtotal : order.amounts.payAmount
      })
    } catch (e) {
      wx.showToast({ title: e.message || '订单信息加载失败', icon: 'none' })
    }
  },
  onChangeType(event) {
    const { type } = event.currentTarget.dataset
    this.setData({ type })
  },
  onInputReason(event) {
    this.setData({ reason: event.detail.value || '' })
  },
  onInputDesc(event) {
    this.setData({ desc: event.detail.value || '' })
  },
  async onSubmit() {
    if (!this.data.reason) {
      wx.showToast({ title: '请填写售后原因', icon: 'none' })
      return
    }
    if (!this.data.orderItemId) {
      wx.showToast({ title: '订单商品信息缺失', icon: 'none' })
      return
    }
    try {
      const record = await applyAfterSale({
        orderId: this.data.orderId,
        orderItemId: this.data.orderItemId,
        type: TYPE_MAP[this.data.type] || 1,
        reasonCode: this.data.reason,
        reasonDesc: this.data.desc,
        applyAmount: this.data.refundAmount
      })
      wx.showToast({ title: '售后申请已提交', icon: 'success' })
      setTimeout(() => {
        wx.navigateTo({ url: `/pages/after-sale/detail/index?id=${record.after_sale_id}` })
      }, 360)
    } catch (e) {
      wx.showToast({ title: e.message || '提交失败', icon: 'none' })
    }
  }
})
