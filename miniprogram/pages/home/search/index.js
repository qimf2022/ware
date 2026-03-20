const {
  getProducts,
  getSearchHot,
  getSearchHistory,
  clearSearchHistory,
  normalizeProduct
} = require('../../../utils/catalog-api')

const SEARCH_HISTORY_KEY = 'YH_SEARCH_HISTORY'

function getLocalSearchHistory() {
  const cache = wx.getStorageSync(SEARCH_HISTORY_KEY)
  if (Array.isArray(cache)) {
    return cache
  }
  return []
}

function setLocalSearchHistory(list) {
  wx.setStorageSync(SEARCH_HISTORY_KEY, list)
}

Page({
  data: {
    keyword: '',
    hotKeywords: ['四件套', '婚嫁套件', '乳胶枕', '冬被', '全棉'],
    historyKeywords: [],
    resultList: [],
    hasSearched: false,
    pageState: 'empty'
  },
  onLoad() {
    this.loadInitData()
  },
  async loadInitData() {
    try {
      const [hotData, historyData] = await Promise.all([getSearchHot(), getSearchHistory(10)])
      const hotKeywords = (hotData.list || []).map((item) => item.keyword).filter(Boolean)
      const historyKeywords = (historyData.list || []).map((item) => item.keyword).filter(Boolean)
      setLocalSearchHistory(historyKeywords)
      this.setData({
        hotKeywords: hotKeywords.length ? hotKeywords : this.data.hotKeywords,
        historyKeywords
      })
    } catch (e) {
      this.setData({ historyKeywords: getLocalSearchHistory() })
    }
  },
  onInputKeyword(event) {
    this.setData({ keyword: (event.detail.value || '').trim() })
  },
  onTapHot(event) {
    const { keyword } = event.currentTarget.dataset
    this.setData({ keyword })
    this.executeSearch(keyword)
  },
  onTapHistory(event) {
    const { keyword } = event.currentTarget.dataset
    this.setData({ keyword })
    this.executeSearch(keyword)
  },
  onSearch() {
    this.executeSearch(this.data.keyword)
  },
  async executeSearch(rawKeyword) {
    const keyword = (rawKeyword || '').trim()
    if (!keyword) {
      wx.showToast({ title: '请输入搜索关键词', icon: 'none' })
      return
    }

    try {
      const data = await getProducts({ keyword, page: 1, pageSize: 20 })
      const resultList = (data.list || []).map(normalizeProduct)

      let history = getLocalSearchHistory().filter((item) => item !== keyword)
      history.unshift(keyword)
      history = history.slice(0, 10)
      setLocalSearchHistory(history)

      this.setData({
        keyword,
        historyKeywords: history,
        resultList,
        hasSearched: true,
        pageState: resultList.length ? 'ready' : 'empty'
      })
    } catch (e) {
      wx.showToast({ title: e.message || '搜索失败', icon: 'none' })
      this.setData({ hasSearched: true, pageState: 'empty', resultList: [] })
    }
  },
  async onClearHistory() {
    try {
      await clearSearchHistory()
    } catch (e) {
    }
    setLocalSearchHistory([])
    this.setData({ historyKeywords: [] })
  },
  onTapResult(event) {
    const { id } = event.currentTarget.dataset
    wx.navigateTo({ url: `/pages/product/detail?id=${id}` })
  }
})
