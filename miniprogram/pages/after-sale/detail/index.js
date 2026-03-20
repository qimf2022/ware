const { getAfterSaleDetail } = require('../../../utils/trade-api')

Page({
  data: {
    pageState: 'loading',
    detail: null
  },
  async onLoad(options) {
    const id = options.id || ''
    if (!id) {
      this.setData({ pageState: 'empty', detail: null })
      return
    }
    try {
      const detail = await getAfterSaleDetail(id)
      this.setData({
        detail,
        pageState: detail ? 'ready' : 'empty'
      })
    } catch (e) {
      this.setData({ pageState: 'empty', detail: null })
    }
  }
})
