const { getPointsSummary } = require('../../../utils/user-api')

Page({
  data: {
    summary: null
  },
  async onLoad() {
    try {
      const summary = await getPointsSummary(1, 50)
      this.setData({ summary })
    } catch (e) {
      wx.showToast({ title: e.message || '积分加载失败', icon: 'none' })
    }
  }
})
