const { request, getBaseUrl } = require('../../utils/request')

const DEFAULT_AVATAR = '/pages/assents/logo.png'

Component({
  properties: {
    visible: {
      type: Boolean,
      value: false
    }
  },

  data: {
    defaultAvatar: DEFAULT_AVATAR,
    avatarUrl: '',
    nickname: '',
    tempAvatarPath: '', // 临时头像路径
    loading: false
  },

  methods: {
    /**
     * 选择头像回调。
     */
    onChooseAvatar(e) {
      const { avatarUrl } = e.detail || {}
      if (avatarUrl) {
        this.setData({ 
          avatarUrl: avatarUrl,
          tempAvatarPath: avatarUrl 
        })
      }
    },

    /**
     * 昵称输入回调。
     */
    onNicknameInput(e) {
      const value = (e.detail && e.detail.value) || ''
      this.setData({ nickname: value })
    },

    /**
     * 昵称失焦回调（微信会在此进行安全校验）。
     */
    onNicknameBlur(e) {
      const value = (e.detail && e.detail.value) || ''
      this.setData({ nickname: value })
    },

    /**
     * 取消授权。
     */
    onCancel() {
      this.resetForm()
      this.triggerEvent('cancel')
    },

    /**
     * 确认授权。
     * 流程：先用昵称登录 -> 上传头像 -> 更新用户头像
     */
    async onConfirm() {
      const { nickname, tempAvatarPath } = this.data

      // 验证昵称
      const trimmedNickname = (nickname || '').trim()
      if (!trimmedNickname) {
        wx.showToast({ title: '请输入昵称', icon: 'none' })
        return
      }

      if (trimmedNickname === '微信用户' || trimmedNickname.startsWith('微信用户')) {
        wx.showToast({ title: '请输入有效的昵称', icon: 'none' })
        return
      }

      this.setData({ loading: true })

      try {
        // 第一步：先用昵称登录（头像可为空）
        const loginResult = await this.doLogin(trimmedNickname, '')

        let avatarUrl = ''
        
        // 第二步：如果选择了头像，上传并更新
        if (tempAvatarPath) {
          try {
            avatarUrl = await this.uploadAvatar(tempAvatarPath)
            // 更新用户头像
            await this.updateUserProfile(trimmedNickname, avatarUrl)
          } catch (uploadErr) {
            // 头像上传失败不影响登录，只是头像不会更新
            console.warn('头像上传失败:', uploadErr.message)
          }
        }

        // 标记授权成功
        wx.setStorageSync('YH_USER_PROFILE_AUTH', 1)
        wx.removeStorageSync('YH_FORCE_GUEST')

        const app = getApp()
        if (app && typeof app.markWechatLoginSuccess === 'function') {
          app.markWechatLoginSuccess()
        }

        this.resetForm()
        this.triggerEvent('success', { nickname: trimmedNickname, avatarUrl })
        wx.showToast({ title: '授权成功', icon: 'success' })
      } catch (e) {
        wx.showToast({ title: e.message || '授权失败', icon: 'none' })
      } finally {
        this.setData({ loading: false })
      }
    },

    /**
     * 上传头像到 OSS。
     */
    async uploadAvatar(tempFilePath) {
      return new Promise((resolve, reject) => {
        const token = wx.getStorageSync('YH_ACCESS_TOKEN')
        if (!token) {
          reject(new Error('请先登录'))
          return
        }

        wx.uploadFile({
          url: `${getBaseUrl()}/upload/avatar`,
          filePath: tempFilePath,
          name: 'file',
          header: {
            'Authorization': `Bearer ${token}`,
            'X-Platform': 'miniprogram',
            'X-Version': '1.0.0'
          },
          success: (res) => {
            try {
              const data = JSON.parse(res.data)
              if (data.code === 0 && data.data && data.data.url) {
                resolve(data.data.url)
              } else {
                reject(new Error(data.message || '头像上传失败'))
              }
            } catch (e) {
              reject(new Error('头像上传失败'))
            }
          },
          fail: (err) => {
            reject(new Error('网络错误，请重试'))
          }
        })
      })
    },

    /**
     * 执行微信登录。
     */
    async doLogin(nickname, avatarUrl) {
      // 获取微信登录 code
      const loginRes = await new Promise((resolve, reject) => {
        wx.login({
          success: resolve,
          fail: () => reject(new Error('微信登录调用失败'))
        })
      })

      const code = (loginRes && loginRes.code) || ''
      if (!code) {
        throw new Error('微信登录失败：未获取到 code')
      }

      // 调用后端登录接口
      const resp = await request({
        url: '/auth/login',
        method: 'POST',
        data: {
          code,
          userInfo: {
            nickName: nickname,
            avatarUrl: avatarUrl,
            gender: 0
          }
        },
        withAuth: false
      })

      if (!resp || !resp.token) {
        throw new Error('登录失败')
      }

      // 保存 token
      wx.setStorageSync('YH_ACCESS_TOKEN', resp.token)
      if (resp.userId !== undefined && resp.userId !== null) {
        wx.setStorageSync('YH_USER_ID', resp.userId)
      }

      return resp
    },

    /**
     * 更新用户信息（头像和昵称）。
     */
    async updateUserProfile(nickname, avatarUrl) {
      return request({
        url: '/user/profile',
        method: 'PUT',
        data: {
          nickname: nickname,
          avatar_url: avatarUrl
        },
        withAuth: true
      })
    },

    /**
     * 重置表单。
     */
    resetForm() {
      this.setData({
        avatarUrl: '',
        nickname: '',
        tempAvatarPath: '',
        loading: false
      })
    },

    /**
     * 阻止事件冒泡。
     */
    onPreventBubble() {}
  }
})
