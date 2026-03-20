Component({
  data: {
    selected: 0,
    color: '#999999',
    selectedColor: '#D81E06',
    list: [
      { pagePath: '/pages/home/index', text: '首页', icon: '⌂', activeIcon: '⌂' },
      { pagePath: '/pages/category/index', text: '分类', icon: '◫', activeIcon: '◫' },
      { pagePath: '/pages/cart/index', text: '购物车', icon: '◍', activeIcon: '◉' },
      { pagePath: '/pages/mine/index', text: '我的', icon: '◌', activeIcon: '●' }
    ]
  },
  methods: {
    switchTab(event) {
      const { index } = event.currentTarget.dataset
      const item = this.data.list[index]
      if (!item) return
      wx.switchTab({ url: item.pagePath })
    },
    setSelected(index) {
      this.setData({ selected: Number(index) || 0 })
    }
  }
})
