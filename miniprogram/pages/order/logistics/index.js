const { getOrderLogistics } = require('../../../utils/trade-api')

Page({
  data: {
    pageState: 'loading',
    logistics: null
  },
  async onLoad(options) {
    try {
      const logistics = await getOrderLogistics(options.orderId || '')
      this.setData({
        logistics,
        pageState: logistics ? 'ready' : 'empty'
      })
    } catch (e) {
      this.setData({ pageState: 'empty' })
    }
  }
})
