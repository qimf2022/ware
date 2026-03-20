const { listOrders } = require('../../../utils/trade-api')

const STATUS_TABS = [
  { label: '全部', value: '' },
  { label: '待付款', value: 'pending' },
  { label: '待发货', value: 'to_ship' },
  { label: '待收货', value: 'shipped' },
  { label: '已完成', value: 'completed' }
]

Page({
  data: {
    orders: [],
    pageState: 'loading',
    tabs: STATUS_TABS,
    activeTab: '',
    statusMap: {}
  },
  onLoad(options) {
    const status = options.status || ''
    this.setData({ activeTab: status })
    this.loadOrders(status)
  },
  onShow() {
    // 避免重复加载，onLoad 已处理
  },
  async loadOrders(status = '') {
    this.setData({ pageState: 'loading' })
    try {
      const data = await listOrders({ page: 1, pageSize: 50, status })
      const list = data.list || []
      this.setData({
        orders: list,
        pageState: list.length ? 'ready' : 'empty'
      })
    } catch (e) {
      this.setData({ pageState: 'error' })
      wx.showToast({ title: e.message || '订单加载失败', icon: 'none' })
    }
  },
  onChangeTab(event) {
    const { value } = event.currentTarget.dataset
    if (value === this.data.activeTab) return
    this.setData({ activeTab: value })
    this.loadOrders(value)
  },
  onTapOrder(event) {
    const { id } = event.currentTarget.dataset
    wx.navigateTo({ url: `/pages/order/detail/index?id=${id}` })
  },
  onRetry() {
    this.loadOrders(this.data.activeTab)
  }
})
