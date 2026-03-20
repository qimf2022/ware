const { listAddresses, upsertAddress } = require('../../../utils/trade-api')

Page({
  data: {
    form: {
      id: '',
      name: '',
      phone: '',
      province: '',
      city: '',
      district: '',
      detail: '',
      isDefault: false
    },
    submitLoading: false,
    isEdit: false
  },
  async onLoad(options) {
    const id = options.id || ''
    const isEdit = !!id
    this.setData({ isEdit })
    wx.setNavigationBarTitle({ title: isEdit ? '编辑地址' : '新增地址' })

    if (!isEdit) {
      return
    }

    try {
      const list = await listAddresses()
      const target = list.find((item) => item.id === id)
      if (target) {
        this.setData({
          form: {
            id: target.id,
            name: target.name,
            phone: '',
            province: target.province,
            city: target.city,
            district: target.district,
            detail: target.detailAddress || target.detail,
            isDefault: target.isDefault
          }
        })
      }
    } catch (e) {
      wx.showToast({ title: e.message || '地址加载失败', icon: 'none' })
    }
  },
  onInput(event) {
    const { field } = event.currentTarget.dataset
    this.setData({ [`form.${field}`]: event.detail.value })
  },
  onSwitchDefault(event) {
    this.setData({ 'form.isDefault': !!event.detail.value })
  },
  async onSave() {
    if (this.data.submitLoading) return

    const form = this.data.form
    if (!form.name || !form.phone || !form.province || !form.city || !form.district || !form.detail) {
      wx.showToast({ title: '请完善地址信息', icon: 'none' })
      return
    }
    if (!/^1\d{10}$/.test(form.phone)) {
      wx.showToast({ title: '手机号格式错误', icon: 'none' })
      return
    }

    this.setData({ submitLoading: true })
    try {
      await upsertAddress(form)
      wx.showToast({ title: '保存成功', icon: 'success' })
      setTimeout(() => wx.navigateBack(), 400)
    } catch (e) {
      wx.showToast({ title: e.message || '保存失败', icon: 'none' })
    } finally {
      this.setData({ submitLoading: false })
    }
  }
})
