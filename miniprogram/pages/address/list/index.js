const { listAddresses, upsertAddress, deleteAddress, setDefaultAddress } = require('../../../utils/trade-api')

Page({
  data: {
    mode: '',
    selectedId: '',
    addresses: [],
    filteredAddresses: [],
    activeTab: 'express',
    keyword: ''
  },
  onLoad(options) {
    this.setData({
      mode: options.mode || '',
      selectedId: options.selectedId || ''
    })
  },
  onShow() {
    this.loadAddresses()
  },
  async loadAddresses() {
    try {
      const addresses = await listAddresses()
      this.setData({ addresses })
      this.applyFilter(addresses, this.data.keyword)
    } catch (e) {
      wx.showToast({ title: e.message || '地址加载失败', icon: 'none' })
    }
  },
  applyFilter(addresses = this.data.addresses, keyword = this.data.keyword) {
    const key = (keyword || '').trim()
    if (!key) {
      this.setData({ filteredAddresses: addresses, keyword: key })
      return
    }
    const list = addresses.filter((item) => {
      const text = `${item.name || ''}${item.phone || ''}${item.province || ''}${item.city || ''}${item.district || ''}${item.detail || ''}`
      return text.indexOf(key) > -1
    })
    this.setData({ filteredAddresses: list, keyword: key })
  },
  onInputKeyword(event) {
    this.applyFilter(this.data.addresses, event.detail.value || '')
  },
  onSwitchTab(event) {
    const { tab } = event.currentTarget.dataset
    if (!tab || tab === this.data.activeTab) return
    this.setData({ activeTab: tab })
  },
  onSelect(event) {
    if (this.data.mode !== 'select') return
    const { id } = event.currentTarget.dataset
    wx.setStorageSync('YH_PENDING_ADDRESS_ID', id)
    wx.navigateBack()
  },
  onAddAddress() {
    wx.navigateTo({ url: '/pages/address/edit/index?mode=create' })
  },
  async onAutoGetAddress() {
    try {
      const res = await wx.chooseAddress()
      if (!res || !res.userName || !res.telNumber) {
        wx.showToast({ title: '地址信息不完整', icon: 'none' })
        return
      }
      await upsertAddress({
        name: res.userName,
        phone: res.telNumber,
        province: res.provinceName,
        city: res.cityName,
        district: res.countyName,
        detail: `${res.detailInfo || ''}`,
        isDefault: this.data.addresses.length === 0
      })
      wx.showToast({ title: '地址已获取', icon: 'success' })
      await this.loadAddresses()
    } catch (e) {
      if (e && e.errMsg && e.errMsg.indexOf('cancel') > -1) return
      wx.showToast({ title: (e && e.message) || '自动获取失败', icon: 'none' })
    }
  },
  onEditAddress(event) {
    const { id } = event.currentTarget.dataset
    wx.navigateTo({ url: `/pages/address/edit/index?id=${id}` })
  },
  async onDeleteAddress(event) {
    const { id } = event.currentTarget.dataset
    wx.showModal({
      title: '删除地址',
      content: '确认删除该地址吗？',
      success: async (res) => {
        if (!res.confirm) return
        try {
          await deleteAddress(id)
          await this.loadAddresses()
          wx.showToast({ title: '已删除', icon: 'success' })
        } catch (e) {
          wx.showToast({ title: e.message || '删除失败', icon: 'none' })
        }
      }
    })
  },
  async onSetDefault(event) {
    const { id } = event.currentTarget.dataset
    try {
      await setDefaultAddress(id)
      await this.loadAddresses()
      wx.showToast({ title: '已设为默认地址', icon: 'success' })
    } catch (e) {
      wx.showToast({ title: e.message || '设置失败', icon: 'none' })
    }
  }
})
