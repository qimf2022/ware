const { quickAddProduct } = require('../../../utils/trade-api')
const { getFootprintGroups } = require('../../../utils/user-api')

Page({
  data: {
    pageState: 'loading',
    groups: []
  },
  async onShow() {
    try {
      const groups = await getFootprintGroups(1, 100)
      const hasData = groups.some((group) => (group.items || []).length)
      this.setData({
        groups,
        pageState: hasData ? 'ready' : 'empty'
      })
    } catch (e) {
      this.setData({ pageState: 'error', groups: [] })
      wx.showToast({ title: e.message || '足迹加载失败', icon: 'none' })
    }
  },
  onTapProduct(event) {
    const { id } = event.currentTarget.dataset
    wx.navigateTo({ url: `/pages/product/detail?id=${id}` })
  },
  async onAddCart(event) {
    const { id } = event.currentTarget.dataset
    try {
      await quickAddProduct(id, 1)
      wx.showToast({ title: '已加入购物车', icon: 'success' })
    } catch (e) {
      wx.showToast({ title: e.message || '加购失败', icon: 'none' })
    }
  }
})
