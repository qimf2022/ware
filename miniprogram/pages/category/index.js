const { getCategories, getProducts, getProductSpecs, normalizeProduct, normalizeSku, uniqueValues } = require('../../utils/catalog-api')
const { addCart } = require('../../utils/trade-api')

const PRICE_FILTERS = ['全部', '200以下', '200-399', '400-699', '700以上']

function getPriceBucket(price) {
  if (price < 200) return '200以下'
  if (price < 400) return '200-399'
  if (price < 700) return '400-699'
  return '700以上'
}

function getSortParam(sortType) {
  if (sortType === 'priceAsc') return 'price_asc'
  if (sortType === 'priceDesc') return 'price_desc'
  if (sortType === 'soldDesc') return 'sales'
  return 'comprehensive'
}

Page({
  data: {
    pageState: 'loading',
    categories: [],
    categoryMeta: [],
    activeIndex: 0,
    allProducts: [],
    categoryProducts: [],
    products: [],
    sortType: 'default',
    filterPanelVisible: false,
    filterOptions: {
      size: ['全部'],
      material: ['全部'],
      price: PRICE_FILTERS
    },
    filterSelections: {
      size: '全部',
      material: '全部',
      price: '全部'
    },
    addCartPopupVisible: false,
    addCartPopupLoading: false,
    addCartSubmitting: false,
    popupProduct: null,
    popupSpecs: {
      size: [],
      material: []
    },
    popupSelectedSpecs: {
      size: '',
      material: ''
    },
    popupDisabledMap: {
      size: {},
      material: {}
    },
    popupSkuList: [],
    popupCurrentSku: null,
    popupQuantity: 1,
    popupSubmitAction: ''
  },
  onLoad() {
    this.loadPage()
  },
  onShow() {
    this.syncTabBar(1)
    const selectedCategoryId = wx.getStorageSync('selectedCategoryId')
    if (selectedCategoryId !== '' && selectedCategoryId !== undefined && selectedCategoryId !== null) {
      const targetIndex = this.data.categoryMeta.findIndex((item) => String(item.id) === String(selectedCategoryId))
      if (targetIndex > -1) {
        this.applyCategory(targetIndex)
      }
      wx.removeStorageSync('selectedCategoryId')
      wx.removeStorageSync('selectedCategoryIndex')
      return
    }

    const selectedCategoryIndex = wx.getStorageSync('selectedCategoryIndex')
    if (selectedCategoryIndex !== '' && selectedCategoryIndex !== undefined && selectedCategoryIndex !== null) {
      this.applyCategory(Number(selectedCategoryIndex) || 0)
      wx.removeStorageSync('selectedCategoryIndex')
    }
  },
  syncTabBar(index) {
    if (typeof this.getTabBar !== 'function') return
    const tabBar = this.getTabBar()
    if (tabBar && typeof tabBar.setData === 'function') {
      tabBar.setData({ selected: index })
    }
  },
  onPullDownRefresh() {
    this.loadPage()
  },
  async loadPage() {
    this.setData({ pageState: 'loading' })
    try {
      const categoryData = await getCategories()
      const categoryMeta = (categoryData.list || []).map((item) => ({
        id: String(item.id),
        name: item.name || ''
      }))
      if (!categoryMeta.length) {
        this.setData({ categories: [], categoryMeta: [], products: [], pageState: 'empty' })
        return
      }

      this.setData({
        categories: categoryMeta.map((item) => item.name),
        categoryMeta
      })

      await this.applyCategory(this.data.activeIndex)
    } catch (e) {
      this.setData({ pageState: 'error' })
      wx.showToast({ title: e.message || '分类加载失败', icon: 'none' })
    } finally {
      wx.stopPullDownRefresh()
    }
  },
  async applyCategory(index) {
    const activeIndex = Number(index) || 0
    const currentCategory = this.data.categoryMeta[activeIndex]
    if (!currentCategory) {
      this.setData({ pageState: 'empty', products: [] })
      return
    }

    this.setData({ pageState: 'loading' })
    try {
      const productData = await getProducts({
        categoryId: currentCategory.id,
        sort: getSortParam(this.data.sortType),
        page: 1,
        pageSize: 100
      })

      const list = (productData.list || []).map(normalizeProduct)
      const filters = productData.filters || []
      const sizeFilter = filters.find((item) => item.attr_code === 'size')
      const materialFilter = filters.find((item) => item.attr_code === 'material')

      this.setData({
        activeIndex,
        categoryProducts: list,
        allProducts: list,
        filterOptions: {
          size: ['全部', ...((sizeFilter && sizeFilter.values) || [])],
          material: ['全部', ...((materialFilter && materialFilter.values) || [])],
          price: PRICE_FILTERS
        },
        filterSelections: {
          size: '全部',
          material: '全部',
          price: '全部'
        },
        filterPanelVisible: false
      })
      this.applyFilterAndSort()
    } catch (e) {
      this.setData({ pageState: 'error' })
      wx.showToast({ title: e.message || '商品加载失败', icon: 'none' })
    }
  },
  applyFilterAndSort() {
    const { categoryProducts, filterSelections } = this.data
    let list = categoryProducts.filter((item) => {
      const sizeHit = filterSelections.size === '全部' || item.size === filterSelections.size
      const materialHit = filterSelections.material === '全部' || item.material === filterSelections.material
      const priceHit = filterSelections.price === '全部' || getPriceBucket(item.price || 0) === filterSelections.price
      return sizeHit && materialHit && priceHit
    })

    if (this.data.sortType === 'priceAsc') {
      list = list.sort((a, b) => (a.price || 0) - (b.price || 0))
    } else if (this.data.sortType === 'priceDesc') {
      list = list.sort((a, b) => (b.price || 0) - (a.price || 0))
    } else if (this.data.sortType === 'soldDesc') {
      list = list.sort((a, b) => (b.soldCount || 0) - (a.soldCount || 0))
    }

    this.setData({
      products: list,
      pageState: list.length ? 'ready' : 'empty'
    })
  },
  onChangeCategory(event) {
    const { index } = event.currentTarget.dataset
    this.applyCategory(index)
  },
  onSelectSort(event) {
    const { type } = event.currentTarget.dataset
    this.setData({ sortType: type })
    this.applyCategory(this.data.activeIndex)
  },
  onToggleFilter() {
    this.setData({ filterPanelVisible: !this.data.filterPanelVisible })
  },
  onSelectFilter(event) {
    const { key, value } = event.currentTarget.dataset
    this.setData({
      filterSelections: {
        ...this.data.filterSelections,
        [key]: value
      }
    })
  },
  onResetFilter() {
    this.setData({
      filterSelections: {
        size: '全部',
        material: '全部',
        price: '全部'
      }
    })
  },
  onConfirmFilter() {
    this.setData({ filterPanelVisible: false })
    this.applyFilterAndSort()
  },
  onTapSearch() {
    wx.navigateTo({ url: '/pages/home/search/index' })
  },
  onRetry() {
    this.loadPage()
  },
  onTapProduct(event) {
    const detailId = event && event.detail ? event.detail.id : ''
    const datasetId = event && event.currentTarget && event.currentTarget.dataset ? event.currentTarget.dataset.id : ''
    const productId = String(detailId || datasetId || '').trim()
    if (!/^\d+$/.test(productId)) {
      wx.showToast({ title: '商品ID无效', icon: 'none' })
      return
    }
    wx.navigateTo({ url: `/pages/product/detail?id=${productId}` })
  },

  updatePopupDisabledMap(selectedSpecs) {
    const skuList = this.data.popupSkuList || []
    const sizeOptions = (this.data.popupSpecs && this.data.popupSpecs.size) || []
    const materialOptions = (this.data.popupSpecs && this.data.popupSpecs.material) || []

    const size = {}
    const material = {}

    sizeOptions.forEach((value) => {
      const hasStock = skuList.some((item) => {
        const sizeHit = item.size === value
        const materialHit = !selectedSpecs.material || item.material === selectedSpecs.material
        return sizeHit && materialHit && (item.stock || 0) > 0
      })
      size[value] = !hasStock
    })

    materialOptions.forEach((value) => {
      const hasStock = skuList.some((item) => {
        const sizeHit = !selectedSpecs.size || item.size === selectedSpecs.size
        const materialHit = item.material === value
        return sizeHit && materialHit && (item.stock || 0) > 0
      })
      material[value] = !hasStock
    })

    this.setData({
      popupDisabledMap: {
        size,
        material
      }
    })
  },

  getMatchedPopupSku(selectedSpecs) {
    const skuList = this.data.popupSkuList || []
    if (!skuList.length) return null

    const matchRule = (item) => {
      const sizeHit = !selectedSpecs.size || item.size === selectedSpecs.size
      const materialHit = !selectedSpecs.material || item.material === selectedSpecs.material
      return sizeHit && materialHit
    }

    return skuList.find((item) => matchRule(item) && (item.stock || 0) > 0)
      || skuList.find(matchRule)
      || skuList.find((item) => (item.stock || 0) > 0)
      || skuList[0]
  },

  syncPopupSku(selectedSpecs) {
    const sku = this.getMatchedPopupSku(selectedSpecs)
    if (!sku) {
      this.setData({
        popupCurrentSku: null,
        popupDisabledMap: { size: {}, material: {} }
      })
      return
    }

    const nextSelected = {
      size: sku.size || '',
      material: sku.material || ''
    }

    const maxStock = Math.max(sku.stock || 0, 1)
    this.setData({
      popupSelectedSpecs: nextSelected,
      popupCurrentSku: sku,
      popupQuantity: Math.min(Math.max(this.data.popupQuantity || 1, 1), maxStock)
    })
    this.updatePopupDisabledMap(nextSelected)
  },

  async onAddCart(event) {
    const { id } = event.detail || {}
    if (!id) return

    const popupProduct = (this.data.products || []).find((item) => String(item.id) === String(id)) || null
    this.setData({
      addCartPopupVisible: true,
      addCartPopupLoading: true,
      popupProduct,
      popupQuantity: 1,
      popupSpecs: { size: [], material: [] },
      popupSelectedSpecs: { size: '', material: '' },
      popupDisabledMap: { size: {}, material: {} },
      popupCurrentSku: null,
      popupSkuList: [],
      popupSubmitAction: '',
      filterPanelVisible: false
    })

    try {
      const specData = await getProductSpecs(id)
      const skuList = (specData.skus || []).map(normalizeSku).filter((item) => item.id)
      if (!skuList.length) {
        wx.showToast({ title: '该商品暂无可售规格', icon: 'none' })
        this.onCloseAddCartPopup()
        return
      }

      const sizeList = uniqueValues(skuList.map((item) => item.size))
      const materialList = uniqueValues(skuList.map((item) => item.material))
      const firstSku = skuList.find((item) => (item.stock || 0) > 0) || skuList[0]

      this.setData({
        popupSkuList: skuList,
        popupSpecs: {
          size: sizeList,
          material: materialList
        },
        popupQuantity: 1
      }, () => {
        this.syncPopupSku({
          size: firstSku.size || '',
          material: firstSku.material || ''
        })
      })
    } catch (e) {
      wx.showToast({ title: e.message || '规格加载失败', icon: 'none' })
      this.onCloseAddCartPopup()
    } finally {
      this.setData({ addCartPopupLoading: false })
    }
  },

  onCloseAddCartPopup() {
    this.setData({
      addCartPopupVisible: false,
      addCartPopupLoading: false,
      addCartSubmitting: false,
      popupProduct: null,
      popupSpecs: { size: [], material: [] },
      popupSelectedSpecs: { size: '', material: '' },
      popupDisabledMap: { size: {}, material: {} },
      popupSkuList: [],
      popupCurrentSku: null,
      popupQuantity: 1,
      popupSubmitAction: ''
    })
  },

  onTapAddCartPopupPanel() {},

  onSelectPopupSpec(event) {
    const { key, value } = event.currentTarget.dataset
    const disabledMap = (this.data.popupDisabledMap && this.data.popupDisabledMap[key]) || {}
    if (disabledMap[value]) return
    if (this.data.popupSelectedSpecs[key] === value) return

    const nextSelected = {
      ...this.data.popupSelectedSpecs,
      [key]: value
    }
    this.syncPopupSku(nextSelected)
  },

  onChangePopupQty(event) {
    const { type } = event.currentTarget.dataset
    const stock = ((this.data.popupCurrentSku || {}).stock) || 0
    const current = this.data.popupQuantity || 1

    if (type === 'dec') {
      if (current <= 1) return
      this.setData({ popupQuantity: current - 1 })
      return
    }

    if (stock <= 0) {
      wx.showToast({ title: '当前规格已售罄', icon: 'none' })
      return
    }

    if (current >= stock) {
      wx.showToast({ title: '已达库存上限', icon: 'none' })
      return
    }

    this.setData({ popupQuantity: current + 1 })
  },

  async submitPopupAddCart(goCart) {
    if (this.data.addCartSubmitting || this.data.addCartPopupLoading) return

    const sku = this.data.popupCurrentSku
    const product = this.data.popupProduct
    if (!product || !sku || !sku.id) {
      wx.showToast({ title: '请选择可售规格', icon: 'none' })
      return
    }
    if ((sku.stock || 0) <= 0) {
      wx.showToast({ title: '当前规格已售罄', icon: 'none' })
      return
    }

    this.setData({
      addCartSubmitting: true,
      popupSubmitAction: goCart ? 'buy' : 'add'
    })

    try {
      await addCart({
        productId: product.id,
        skuId: sku.id,
        quantity: this.data.popupQuantity || 1
      })
      wx.showToast({ title: goCart ? '已加入购物车，正在前往' : '加入购物车成功', icon: 'success' })
      this.onCloseAddCartPopup()
      if (goCart) {
        wx.switchTab({ url: '/pages/cart/index' })
      }
    } catch (e) {
      wx.showToast({ title: e.message || '加入购物车失败', icon: 'none' })
      this.setData({
        addCartSubmitting: false,
        popupSubmitAction: ''
      })
    }
  },

  onConfirmPopupAddCart() {
    this.submitPopupAddCart(false)
  },

  onPopupBuyNow() {
    this.submitPopupAddCart(true)
  }
})
