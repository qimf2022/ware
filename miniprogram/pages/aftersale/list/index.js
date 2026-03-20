const { listAfterSales, getAfterSaleDetail } = require('../../../utils/trade-api')

Page({
  data: {
    list: [],
    pageState: 'loading'
  },
  onShow() {
    this.loadList()
  },
  async loadList() {
    this.setData({ pageState: 'loading' })
    try {
      const data = await listAfterSales({ page: 1, pageSize: 50 })
      const items = data.list || []
      this.setData({
        list: items,
        pageState: items.length ? 'ready' : 'empty'
      })
    } catch (e) {
      this.setData({ pageState: 'error' })
      wx.showToast({ title: e.message || '加载失败', icon: 'none' })
    }
  },
  onTapItem(event) {
    const { id } = event.currentTarget.dataset
    wx.navigateTo({ url: `/pages/aftersale/detail/index?id=${id}` })
  },
  onRetry() {
    this.loadList()
  }
})
