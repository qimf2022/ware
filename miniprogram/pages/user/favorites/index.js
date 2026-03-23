const { quickAddProduct } = require('../../../utils/trade-api')
const { getFavorites, favoriteAction } = require('../../../utils/user-api')

Page({
  data: {
    pageState: 'loading',
    list: []
  },
  onShow() {
    this.loadData()
  },
  async loadData() {
    try {
      const data = await getFavorites(1, 50)
      const list = data.list || []
      this.setData({
        list,
        pageState: list.length ? 'ready' : 'empty'
      })
    } catch (e) {
      this.setData({ pageState: 'error' })
      wx.showToast({ title: e.message || '收藏加载失败', icon: 'none' })
    }
  },
  onTapProduct(event) {
    const { id } = event.currentTarget.dataset || {}
    const productId = String(id || '').trim()
    if (!/^\d+$/.test(productId)) {
      wx.showToast({ title: '商品ID无效', icon: 'none' })
      return
    }
    wx.navigateTo({ url: `/pages/product/detail?id=${productId}` })
  },
  async onRemove(event) {
    const { id } = event.currentTarget.dataset
    try {
      await favoriteAction(id, 'remove')
      await this.loadData()
      wx.showToast({ title: '已取消收藏', icon: 'success' })
    } catch (e) {
      wx.showToast({ title: e.message || '操作失败', icon: 'none' })
    }
  },
  async onAddCart(event) {
    const { id } = event.currentTarget.dataset
    try {
      await quickAddProduct(id, 1)
      wx.showToast({ title: '已加入购物车', icon: 'success' })
    } catch (e) {
      wx.showToast({ title: e.message || '加购失败', icon: 'none' })
    }
  },
  onRetry() {
    this.loadData()
  }
})
