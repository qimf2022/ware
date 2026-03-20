const { getProfile, updateProfile } = require('../../../utils/user-api')
const { clearAuthCache } = require('../../../utils/request')

const REAL_NAME_KEY = 'YH_PROFILE_REAL_NAME'
const BIRTHDAY_KEY = 'YH_PROFILE_BIRTHDAY'

function genderText(value) {
  if (Number(value) === 1) return '男'
  if (Number(value) === 2) return '女'
  return '未设置'
}

Page({
  data: {
    profile: {
      id: '',
      nickname: '',
      avatarUrl: '',
      phone: '',
      gender: 0
    },
    realName: '',
    birthday: '',
    genderLabel: '未设置'
  },

  async onShow() {
    await this.loadProfile()
  },
  async loadProfile() {
    try {
      const profile = await getProfile()
      this.setData({
        profile,
        realName: wx.getStorageSync(REAL_NAME_KEY) || '',
        birthday: wx.getStorageSync(BIRTHDAY_KEY) || '',
        genderLabel: genderText(profile.gender)
      })

    } catch (e) {
      wx.showToast({ title: e.message || '资料加载失败', icon: 'none' })
    }
  },
  async onEditNickname() {
    const current = this.data.profile.nickname || ''
    wx.showModal({
      title: '修改昵称',
      editable: true,
      placeholderText: '请输入昵称',
      content: current,
      success: async (res) => {
        if (!res.confirm) return
        const value = (res.content || '').trim()
        if (!value) return
        try {
          await updateProfile({ nickname: value })
          this.setData({ 'profile.nickname': value })
          wx.showToast({ title: '已更新', icon: 'success' })
        } catch (e) {
          wx.showToast({ title: e.message || '更新失败', icon: 'none' })
        }
      }
    })
  },
  onEditRealName() {
    const current = this.data.realName || ''
    wx.showModal({
      title: '填写姓名',
      editable: true,
      placeholderText: '请输入姓名',
      content: current,
      success: (res) => {
        if (!res.confirm) return
        const value = (res.content || '').trim()
        wx.setStorageSync(REAL_NAME_KEY, value)
        this.setData({ realName: value })
      }
    })
  },
  async onChooseGender() {
    wx.showActionSheet({
      itemList: ['男', '女', '保密'],
      success: async (res) => {
        const map = [1, 2, 0]
        const gender = map[res.tapIndex]
        try {
          await updateProfile({ gender })
          this.setData({ 'profile.gender': gender, genderLabel: genderText(gender) })

          wx.showToast({ title: '已更新', icon: 'success' })
        } catch (e) {
          wx.showToast({ title: e.message || '更新失败', icon: 'none' })
        }
      }
    })
  },
  onBirthdayChange(event) {
    const birthday = event.detail.value || ''
    wx.setStorageSync(BIRTHDAY_KEY, birthday)
    this.setData({ birthday })
  },
  async onChooseAvatar() {
    try {
      const media = await wx.chooseMedia({ count: 1, mediaType: ['image'], sourceType: ['album', 'camera'] })
      const file = (media.tempFiles || [])[0]
      if (!file || !file.tempFilePath) return
      await updateProfile({ avatarUrl: file.tempFilePath })
      this.setData({ 'profile.avatarUrl': file.tempFilePath })
      wx.showToast({ title: '头像已更新', icon: 'success' })
    } catch (e) {
      if (e && e.errMsg && e.errMsg.indexOf('cancel') > -1) return
      wx.showToast({ title: (e && e.message) || '头像更新失败', icon: 'none' })
    }
  },
  onTapAddress() {
    wx.navigateTo({ url: '/pages/address/list/index' })
  },
  onTapBindMobile() {
    wx.navigateTo({ url: '/pages/user/bind-mobile/index' })
  },
  onLogout() {
    wx.showModal({
      title: '退出登录',
      content: '确认退出当前账号吗？',
      success: (res) => {
        if (!res.confirm) return
        clearAuthCache()
        wx.setStorageSync('YH_FORCE_GUEST', 1)
        wx.removeStorageSync('YH_USER_PROFILE_AUTH')
        wx.removeStorageSync('YH_PENDING_ADDRESS_ID')
        const app = getApp()
        if (app && app.globalData) {
          app.globalData.authMode = 'guest'
        }
        wx.showToast({ title: '已退出', icon: 'success' })
        setTimeout(() => {
          wx.switchTab({ url: '/pages/mine/index' })
        }, 260)
      }
    })
  }
})