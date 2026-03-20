Component({
  properties: {
    state: {
      type: String,
      value: 'loading'
    },
    loadingText: {
      type: String,
      value: '加载中...'
    },
    emptyText: {
      type: String,
      value: '暂无数据'
    },
    errorText: {
      type: String,
      value: '加载失败，请稍后重试'
    },
    actionText: {
      type: String,
      value: '重试'
    }
  },
  methods: {
    onRetry() {
      this.triggerEvent('retry')
    }
  }
})
