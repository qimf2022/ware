const { getCart, listAddresses, confirmOrder, createOrder, requestWechatPay } = require('../../../utils/trade-api')

Page({
  data: {
    pageState: 'loading',
    checkoutIds: [],
    items: [],
    address: null,
    remark: '',
    productAmount: 0,
    freightAmount: 0,
    payAmount: 0,
    deliveryMode: 'express',
    submitting: false,
    deliveryTabs: [
      { key: 'express', label: '快递配送' },
      { key: 'pickup', label: '到店自提' },
      { key: 'city', label: '同城配送' }
    ]
  },
  onShow() {
    this.loadData()
  },
  async loadData() {
    const checkoutIds = wx.getStorageSync('YH_CHECKOUT_IDS') || []
    if (!checkoutIds.length) {
      this.setData({ pageState: 'empty', items: [], checkoutIds: [] })
      return
    }

    this.setData({ pageState: 'loading' })
    try {
      const pendingAddressId = wx.getStorageSync('YH_PENDING_ADDRESS_ID')
      if (pendingAddressId) {
        wx.removeStorageSync('YH_PENDING_ADDRESS_ID')
      }

      const [cartData, addresses] = await Promise.all([getCart(), listAddresses()])
      const items = (cartData.items || []).filter((item) => checkoutIds.includes(item.id))
      if (!items.length) {
        this.setData({ pageState: 'empty', items: [], checkoutIds: [] })
        return
      }

      let address = null
      if (pendingAddressId) {
        address = addresses.find((item) => item.id === pendingAddressId) || null
      }
      if (!address) {
        address = addresses.find((item) => item.isDefault) || addresses[0] || null
      }

      if (!address) {
        this.setData({
          pageState: 'ready',
          checkoutIds,
          items,
          address: null,
          productAmount: 0,
          freightAmount: 0,
          payAmount: 0
        })
        return
      }

      const confirmData = await confirmOrder({
        cartIds: checkoutIds,
        addressId: address.id
      })

      const price = confirmData.price || {}
      this.setData({
        pageState: 'ready',
        checkoutIds,
        items,
        address,
        productAmount: Number(price.total_amount || 0),
        freightAmount: Number(price.freight_amount || 0),
        payAmount: Number(price.pay_amount || 0)
      })
    } catch (e) {
      this.setData({ pageState: 'error' })
      wx.showToast({ title: e.message || '订单确认加载失败', icon: 'none' })
    }
  },
  onSwitchDelivery(event) {
    const { key } = event.currentTarget.dataset
    if (!key || key === this.data.deliveryMode) return
    this.setData({ deliveryMode: key })
  },
  onInputRemark(event) {
    this.setData({ remark: event.detail.value || '' })
  },
  onChooseAddress() {
    const selectedId = this.data.address ? this.data.address.id : ''
    wx.navigateTo({ url: `/pages/address/list/index?mode=select&selectedId=${selectedId}` })
  },
  async onSubmitOrder() {
    if (this.data.submitting) return

    if (!this.data.address) {
      wx.showToast({ title: '请选择收货地址', icon: 'none' })
      return
    }
    if (!this.data.items.length) {
      wx.showToast({ title: '暂无可提交商品', icon: 'none' })
      return
    }

    this.setData({ submitting: true })
    try {
      const order = await createOrder({
        cartIds: this.data.checkoutIds,
        addressId: this.data.address.id,
        remark: this.data.remark,
        sourceType: 'miniapp_cart'
      })

      await requestWechatPay(order.order_id, 1)
      wx.removeStorageSync('YH_CHECKOUT_IDS')
      wx.navigateTo({
        url: `/pages/pay/result/index?status=success&orderId=${order.order_id}&orderNo=${order.order_no}`
      })
    } catch (e) {
      if (e && e.cancelled) {
        wx.showToast({ title: '已取消支付', icon: 'none' })
        return
      }
      wx.showToast({ title: e.message || '提交订单失败', icon: 'none' })
    } finally {
      this.setData({ submitting: false })
    }
  },
  onRetry() {
    this.loadData()
  },
  onBackCart() {
    wx.switchTab({ url: '/pages/cart/index' })
  }
})
