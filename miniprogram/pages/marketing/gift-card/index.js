const { getGiftCardSummary } = require('../../../utils/user-store')

Page({
  data: {
    summary: null
  },
  onLoad() {
    this.setData({ summary: getGiftCardSummary() })
  }
})
