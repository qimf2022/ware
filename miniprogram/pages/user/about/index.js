Page({
  onCopyWechat() {
    wx.setClipboardData({ data: 'YUHOME_SERVICE' })
  },
  onCallService() {
    wx.makePhoneCall({ phoneNumber: '4008008899' })
  }
})
