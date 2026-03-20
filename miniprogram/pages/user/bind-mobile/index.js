const { getProfile, updateProfile } = require('../../../utils/user-api')

Page({
  data: {
    phone: '',
    code: '',
    countdown: 0
  },
  timer: null,
  async onLoad() {
    try {
      const profile = await getProfile()
      this.setData({ phone: (profile.phone || '').replace(/\*/g, '') })
    } catch (e) {
      this.setData({ phone: '' })
    }
  },
  onUnload() {
    if (this.timer) {
      clearInterval(this.timer)
      this.timer = null
    }
  },
  onInputPhone(event) {
    this.setData({ phone: (event.detail.value || '').trim() })
  },
  onInputCode(event) {
    this.setData({ code: (event.detail.value || '').trim() })
  },
  onSendCode() {
    if (this.data.countdown > 0) return
    if (!/^1\d{10}$/.test(this.data.phone)) {
      wx.showToast({ title: '请输入正确手机号', icon: 'none' })
      return
    }
    wx.showToast({ title: '验证码已发送', icon: 'success' })
    this.setData({ countdown: 60 })
    this.timer = setInterval(() => {
      const next = this.data.countdown - 1
      this.setData({ countdown: next })
      if (next <= 0) {
        clearInterval(this.timer)
        this.timer = null
      }
    }, 1000)
  },
  async onSubmit() {
    if (!/^1\d{10}$/.test(this.data.phone)) {
      wx.showToast({ title: '请输入正确手机号', icon: 'none' })
      return
    }
    if (!this.data.code) {
      wx.showToast({ title: '请输入验证码', icon: 'none' })
      return
    }
    try {
      await updateProfile({ nickname: `诗语用户${this.data.phone.slice(-4)}` })
      wx.showToast({ title: '绑定成功', icon: 'success' })
      setTimeout(() => wx.navigateBack(), 350)
    } catch (e) {
      wx.showToast({ title: e.message || '绑定失败', icon: 'none' })
    }
  }
})
