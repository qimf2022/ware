const {
  getProductDetail,
  getProductSpecs,
  getProductRecommend,
  normalizeProduct,
  normalizeSku,
  uniqueValues
} = require('../../utils/catalog-api')
const { addCart, quickAddProduct, listAddresses, createOrder, requestWechatPay } = require('../../utils/trade-api')
const { favoriteAction } = require('../../utils/user-api')

const BED_TYPE_SIZE_MAP = {
  单人床: '1.2m',
  标准双人床: '1.5m',
  大床: '1.8m',
  加大床: '2.0m'
}

Page({
  data: {
    pageState: 'loading',
    productId: '',
    product: null,
    specs: {
      size: [],
      material: []
    },
    selectedSpecs: {
      size: '',
      material: ''
    },
    specDisabledMap: {
      size: {},
      material: {}
    },
    currentSku: null,
    selectedBedType: '标准双人床',
    assistantSuggestion: '',
    recommendProducts: [],
    galleryImages: [],
    galleryImagesBase: [],
    skuPopupVisible: false,
    skuQuantity: 1,
    pendingAction: 'add',
    submitLoading: false,
    favoriteLoading: false
  },

  onLoad(options) {
    this.setData({ productId: options.id || '' })
    this.loadPage()
  },
  onPullDownRefresh() {
    this.loadPage()
  },
  async loadPage() {
    this.setData({ pageState: 'loading' })
    try {
      const productId = this.data.productId
      const [detailData, specData, recommendData] = await Promise.all([
        getProductDetail(productId),
        getProductSpecs(productId),
        getProductRecommend(productId, 4)
      ])

      const product = normalizeProduct(detailData)
      const skus = (specData.skus || []).map(normalizeSku).filter((item) => item.id)

      if (!skus.length) {
        this.setData({ pageState: 'empty' })
        return
      }

      const sizeList = uniqueValues(skus.map((item) => item.size))
      const materialList = uniqueValues(skus.map((item) => item.material))
      const firstSku = skus.find((item) => (item.stock || 0) > 0) || skus[0]
      const selectedSpecs = {
        size: firstSku.size || '',
        material: firstSku.material || ''
      }

      const recommendProducts = (recommendData.list || []).map(normalizeProduct)
      const mediaImages = (detailData.media || [])
        .map((item) => item.media_url || item.cover_url || '')
        .filter(Boolean)
      const galleryImagesBase = uniqueValues([product.image].concat(mediaImages).filter(Boolean))
      const galleryImages = this.composeGalleryImages(firstSku.image, galleryImagesBase)

      this.setData({
        product: {
          ...product,
          soldCount: Number(detailData.sales_count || 0),
          favoriteCount: Number(detailData.favorite_count || 0),
          isFavorited: !!detailData.is_favorited,
          skuList: skus,
          attrs: detailData.detail_attrs || []
        },
        specs: {
          size: sizeList,
          material: materialList
        },
        selectedSpecs,
        currentSku: firstSku,
        recommendProducts,
        galleryImages,
        galleryImagesBase,
        specDisabledMap: { size: {}, material: {} },
        skuPopupVisible: false,
        skuQuantity: 1,
        pendingAction: 'add',
        submitLoading: false,
        favoriteLoading: false,
        pageState: 'ready'
      })

      this.updateSpecDisabledMap(selectedSpecs)
      this.updateAssistantSuggestion('标准双人床')
    } catch (e) {
      this.setData({ pageState: 'error' })
      wx.showToast({ title: e.message || '商品详情加载失败', icon: 'none' })
    } finally {
      wx.stopPullDownRefresh()
    }
  },
  composeGalleryImages(skuImage, baseList) {
    return uniqueValues([skuImage].concat(baseList || []).filter(Boolean))
  },
  getMatchedSku(selectedSpecs) {
    const skuList = ((this.data.product || {}).skuList) || []
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
  updateSpecDisabledMap(selectedSpecs) {
    const skuList = ((this.data.product || {}).skuList) || []
    const sizeOptions = (this.data.specs && this.data.specs.size) || []
    const materialOptions = (this.data.specs && this.data.specs.material) || []

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

    this.setData({ specDisabledMap: { size, material } })
  },
  onSelectSpec(event) {
    const { key, value } = event.currentTarget.dataset
    const disabledMap = (this.data.specDisabledMap && this.data.specDisabledMap[key]) || {}
    if (disabledMap[value]) return

    const nextSelected = {
      ...this.data.selectedSpecs,
      [key]: value
    }
    this.syncCurrentSku(nextSelected)
  },
  syncCurrentSku(selectedSpecs) {
    const sku = this.getMatchedSku(selectedSpecs)
    if (!sku) {
      wx.showToast({ title: '该规格暂不可售', icon: 'none' })
      return
    }

    const nextSelected = {
      size: sku.size || '',
      material: sku.material || ''
    }

    this.setData({
      selectedSpecs: nextSelected,
      currentSku: sku,
      skuQuantity: Math.min(Math.max(this.data.skuQuantity || 1, 1), Math.max(sku.stock || 0, 1)),
      galleryImages: this.composeGalleryImages(sku.image, this.data.galleryImagesBase)
    })

    this.updateSpecDisabledMap(nextSelected)
  },
  onSelectBedType(event) {
    const { type } = event.currentTarget.dataset
    this.updateAssistantSuggestion(type)
  },
  updateAssistantSuggestion(type) {
    const suggestSize = BED_TYPE_SIZE_MAP[type] || ''
    let matched = ''
    if (suggestSize) {
      const hit = (this.data.specs.size || []).find((item) => item.indexOf(suggestSize) > -1)
      matched = hit || ''
    }
    const suggestion = matched
      ? `推荐选择 ${matched} 尺寸，当前床型为 ${type}。`
      : `当前床型为 ${type}，建议优先选择 ${suggestSize || '标准'} 规格。`
    this.setData({
      selectedBedType: type,
      assistantSuggestion: suggestion
    })
  },
  onTapRecommend(event) {
    const { id } = event.detail
    wx.navigateTo({ url: `/pages/product/detail?id=${id}` })
  },
  async onAddRecommendCart(event) {
    try {
      const { id } = event.detail
      await quickAddProduct(id, 1)
      wx.showToast({ title: '推荐商品已加购', icon: 'success' })
    } catch (e) {
      wx.showToast({ title: e.message || '加购失败', icon: 'none' })
    }
  },
  onRetry() {
    this.loadPage()
  },
  onGoHome() {
    wx.switchTab({ url: '/pages/home/index' })
  },
  async onToggleFavorite() {
    if (this.data.favoriteLoading) return

    const product = this.data.product
    if (!product || !product.id) return

    const action = product.isFavorited ? 'remove' : 'add'
    this.setData({ favoriteLoading: true })

    try {
      const result = await favoriteAction(product.id, action)
      const isFavorited = result && result.is_favorited !== undefined
        ? !!result.is_favorited
        : action === 'add'
      const favoriteCount = result && result.favorite_count !== undefined
        ? Number(result.favorite_count || 0)
        : Math.max(Number(product.favoriteCount || 0) + (isFavorited ? 1 : -1), 0)

      this.setData({
        product: {
          ...product,
          isFavorited,
          favoriteCount
        }
      })
      wx.showToast({ title: isFavorited ? '收藏成功' : '已取消收藏', icon: 'none' })
    } catch (e) {
      wx.showToast({ title: e.message || '收藏操作失败', icon: 'none' })
    } finally {
      this.setData({ favoriteLoading: false })
    }
  },
  openSkuPopup(action) {
    if (!this.data.currentSku || !this.data.product) {
      wx.showToast({ title: '请先选择规格', icon: 'none' })
      return
    }
    this.setData({
      skuPopupVisible: true,
      pendingAction: action,
      skuQuantity: 1
    })
  },
  onAddCart() {
    this.openSkuPopup('add')
  },
  onBuyNow() {
    this.openSkuPopup('buy')
  },
  onCloseSkuPopup() {
    if (this.data.submitLoading) return
    this.setData({ skuPopupVisible: false })
  },
  onTapSkuPopupPanel() {},
  onChangeSkuQty(event) {
    const { type } = event.currentTarget.dataset
    const stock = ((this.data.currentSku || {}).stock) || 0
    const current = this.data.skuQuantity || 1
    if (type === 'dec') {
      if (current <= 1) return
      this.setData({ skuQuantity: current - 1 })
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
    this.setData({ skuQuantity: current + 1 })
  },
  async onConfirmSkuAction(event) {
    if (this.data.submitLoading) return

    const action = (event && event.currentTarget && event.currentTarget.dataset && event.currentTarget.dataset.action) || this.data.pendingAction || 'add'
    const sku = this.data.currentSku
    const product = this.data.product
    if (!product || !sku || !sku.id) {
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
      const quantity = this.data.skuQuantity || 1
      if (action === 'add') {
        await addCart({
          productId: product.id,
          skuId: sku.id,
          quantity
        })
        wx.showToast({ title: '加入购物车成功', icon: 'success' })
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
          sourceType: 'miniapp_detail'
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
