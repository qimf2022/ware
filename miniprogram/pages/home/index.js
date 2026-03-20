const {
  getHomeConfig,
  getHomeRecommend,
  getProductSpecs,
  normalizeProduct,
  normalizeSku,
  uniqueValues
} = require('../../utils/catalog-api')
const { resolveAssetUrl } = require('../../utils/request')
const { addCart, listAddresses, createOrder, requestWechatPay } = require('../../utils/trade-api')

const PAGE_SIZE = 10
const DEFAULT_LOGO = '/pages/assents/logo.png'

Page({
  data: {
    pageState: 'loading',
    categories: [],
    categoryMeta: [],
    products: [],
    bannerText: '限时特惠 · 超舒服的系列',
    bannerImage: DEFAULT_LOGO,
    currentPage: 1,
    hasMore: true,
    loadingMore: false,
    noMore: false,
    skuPopupVisible: false,
    skuPopupLoading: false,
    popupProduct: null,
    popupSpecs: {
      size: [],
      material: []
    },
    popupSelectedSpecs: {
      size: '',
      material: ''
    },
    popupSpecDisabledMap: {
      size: {},
      material: {}
    },
    popupCurrentSku: null,
    popupQuantity: 1,
    pendingAction: 'add',
    submitLoading: false
  },
  onLoad() {
    this.productSpecCache = {}
    this.loadPage()
  },
  onShow() {
    this.syncTabBar(0)
    const app = getApp()
    if (app && typeof app.maybeShowAuthGuide === 'function') {
      app.maybeShowAuthGuide()
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
  onReachBottom() {
    this.loadMoreProducts()
  },
  async loadPage() {
    this.setData({
      pageState: 'loading',
      products: [],
      currentPage: 1,
      hasMore: true,
      loadingMore: false,
      noMore: false,
      skuPopupVisible: false,
      skuPopupLoading: false,
      popupProduct: null,
      popupCurrentSku: null,
      popupQuantity: 1,
      pendingAction: 'add',
      submitLoading: false
    })

    try {
      const [config, recommend] = await Promise.all([getHomeConfig(), getHomeRecommend(1, PAGE_SIZE)])

      const categoryMeta = (config.categories || []).map((item) => ({
        id: String(item.id),
        name: item.name || '',
        image: DEFAULT_LOGO
      }))

      const recommendList = ((recommend && recommend.list) || (config && config.recommend_products) || [])
        .map(normalizeProduct)

      const banner = (config.banners || [])[0] || {}
      const bannerText = banner.title ? `${banner.title} · 超舒服的系列` : '限时特惠 · 超舒服的系列'
      const bannerImage = resolveAssetUrl(banner.image_url || banner.image || '') || DEFAULT_LOGO
      const hasMore = typeof recommend.has_more === 'boolean' ? recommend.has_more : recommendList.length >= PAGE_SIZE

      this.setData({
        categories: categoryMeta,
        categoryMeta,
        products: recommendList,
        bannerText,
        bannerImage,
        currentPage: 1,
        hasMore,
        noMore: recommendList.length > 0 && !hasMore,
        pageState: recommendList.length ? 'ready' : 'empty'
      })
    } catch (e) {
      this.setData({ pageState: 'error' })
      wx.showToast({ title: e.message || '首页加载失败', icon: 'none' })
    } finally {
      wx.stopPullDownRefresh()
    }
  },
  async loadMoreProducts() {
    if (this.data.pageState !== 'ready' || this.data.loadingMore || !this.data.hasMore) {
      return
    }

    const nextPage = (this.data.currentPage || 1) + 1
    this.setData({ loadingMore: true })

    try {
      const pageData = await getHomeRecommend(nextPage, PAGE_SIZE)
      const list = (pageData.list || []).map(normalizeProduct)
      const merged = [...this.data.products, ...list]
      const hasMore = typeof pageData.has_more === 'boolean' ? pageData.has_more : list.length >= PAGE_SIZE

      this.setData({
        products: merged,
        currentPage: nextPage,
        hasMore,
        noMore: merged.length > 0 && !hasMore
      })
    } catch (e) {
      wx.showToast({ title: e.message || '加载失败', icon: 'none' })
    } finally {
      this.setData({ loadingMore: false })
    }
  },
  onRetry() {
    this.loadPage()
  },
  onTapSearch() {
    wx.navigateTo({ url: '/pages/home/search/index' })
  },
  onTapBanner() {
    wx.navigateTo({ url: '/pages/home/topic/index' })
  },
  onTapCategory(event) {
    const { index } = event.currentTarget.dataset
    const hit = this.data.categoryMeta[Number(index)]
    wx.setStorageSync('selectedCategoryIndex', Number(index))
    if (hit && hit.id) {
      wx.setStorageSync('selectedCategoryId', String(hit.id))
    }
    wx.switchTab({
      url: '/pages/category/index'
    })
  },
  onTapProduct(event) {
    const { id } = event.detail
    wx.navigateTo({ url: `/pages/product/detail?id=${id}` })
  },
  async onAddCart(event) {
    const { id } = event.detail || {}
    if (!id || this.data.skuPopupLoading) return

    this.setData({ skuPopupLoading: true })
    try {
      const product = this.data.products.find((item) => String(item.id) === String(id)) || null
      const specData = await this.getProductSpecData(id)
      const skus = specData.skus || []
      if (!skus.length) {
        wx.showToast({ title: '该商品暂无可选规格', icon: 'none' })
        return
      }

      const firstSku = skus.find((item) => (item.stock || 0) > 0) || skus[0]
      const selectedSpecs = {
        size: firstSku.size || '',
        material: firstSku.material || ''
      }

      this.setData({
        popupProduct: {
          id: String(id),
          title: (product && product.title) || '',
          image: (product && product.image) || ''
        },
        popupSpecs: {
          size: specData.sizeList || [],
          material: specData.materialList || []
        },
        popupSelectedSpecs: selectedSpecs,
        popupCurrentSku: firstSku,
        popupSpecDisabledMap: { size: {}, material: {} },
        popupQuantity: 1,
        pendingAction: 'add',
        submitLoading: false,
        skuPopupVisible: true
      })

      this.updatePopupDisabledMap(selectedSpecs)
    } catch (e) {
      wx.showToast({ title: e.message || '规格加载失败', icon: 'none' })
    } finally {
      this.setData({ skuPopupLoading: false })
    }
  },
  async getProductSpecData(productId) {
    const key = String(productId)
    if (this.productSpecCache && this.productSpecCache[key]) {
      return this.productSpecCache[key]
    }

    const raw = await getProductSpecs(productId)
    const skus = (raw.skus || []).map(normalizeSku).filter((item) => item.id)
    const data = {
      skus,
      sizeList: uniqueValues(skus.map((item) => item.size)),
      materialList: uniqueValues(skus.map((item) => item.material))
    }

    this.productSpecCache[key] = data
    return data
  },
  getMatchedPopupSku(selectedSpecs) {
    const popupProduct = this.data.popupProduct || {}
    const specData = this.productSpecCache[String(popupProduct.id)] || { skus: [] }
    const skuList = specData.skus || []
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
  updatePopupDisabledMap(selectedSpecs) {
    const popupProduct = this.data.popupProduct || {}
    const specData = this.productSpecCache[String(popupProduct.id)] || { skus: [] }
    const skuList = specData.skus || []
    const sizeOptions = ((this.data.popupSpecs || {}).size) || []
    const materialOptions = ((this.data.popupSpecs || {}).material) || []

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

    this.setData({ popupSpecDisabledMap: { size, material } })
  },
  syncPopupSku(selectedSpecs) {
    const sku = this.getMatchedPopupSku(selectedSpecs)
    if (!sku) {
      wx.showToast({ title: '该规格暂不可售', icon: 'none' })
      return
    }

    const nextSelected = {
      size: sku.size || '',
      material: sku.material || ''
    }

    this.setData({
      popupSelectedSpecs: nextSelected,
      popupCurrentSku: sku,
      popupQuantity: Math.min(Math.max(this.data.popupQuantity || 1, 1), Math.max(sku.stock || 0, 1))
    })

    this.updatePopupDisabledMap(nextSelected)
  },
  onSelectPopupSpec(event) {
    const { key, value } = event.currentTarget.dataset
    const disabledMap = (this.data.popupSpecDisabledMap && this.data.popupSpecDisabledMap[key]) || {}
    if (disabledMap[value]) return

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
  onCloseSkuPopup() {
    if (this.data.submitLoading) return
    this.setData({ skuPopupVisible: false })
  },
  onTapSkuPopupPanel() {},
  onConfirmPopupAddCart() {
    this.submitPopupAction('add')
  },
  onConfirmPopupBuyNow() {
    this.submitPopupAction('buy')
  },
  async submitPopupAction(action) {
    if (this.data.submitLoading) return

    const sku = this.data.popupCurrentSku
    const product = this.data.popupProduct
    if (!product || !product.id || !sku || !sku.id) {
      wx.showToast({ title: '请选择可售规格', icon: 'none' })
      return
    }
    if ((sku.stock || 0) <= 0) {
      wx.showToast({ title: '当前规格已售罄', icon: 'none' })
      return
    }

    this.setData({
      submitLoading: true,
      pendingAction: action
    })

    try {
      const quantity = this.data.popupQuantity || 1
      if (action === 'add') {
        await addCart({
          productId: product.id,
          skuId: sku.id,
          quantity
        })
        wx.showToast({ title: '已加入购物车', icon: 'success' })
        this.setData({ skuPopupVisible: false })
      } else {
        const addresses = await listAddresses()
        const address = (addresses || []).find((item) => item.isDefault) || (addresses || [])[0] || null
        if (!address) {
          wx.showToast({ title: '请先添加收货地址', icon: 'none' })
          wx.navigateTo({ url: '/pages/address/list/index' })
          return
        }

        const order = await createOrder({
          productId: product.id,
          skuId: sku.id,
          quantity,
          addressId: address.id,
          sourceType: 'miniapp_home'
        })

        await requestWechatPay(order.order_id, 1)
        this.setData({ skuPopupVisible: false })
        wx.navigateTo({
          url: `/pages/pay/result/index?status=success&orderId=${order.order_id}&orderNo=${order.order_no}`
        })
      }
    } catch (e) {
      if (e && e.cancelled) {
        wx.showToast({ title: '已取消支付', icon: 'none' })
      } else {
        wx.showToast({ title: e.message || '操作失败', icon: 'none' })
      }
    } finally {
      this.setData({ submitLoading: false })
    }
  }
})
