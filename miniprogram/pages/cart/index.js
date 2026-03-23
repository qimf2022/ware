const {
  getCart,
  updateCartItem,
  selectAllCart,
  deleteCartItems,
  getCartRecommend,
  addCart
} = require('../../utils/trade-api')
const {
  getProductSpecs,
  normalizeSku,
  uniqueValues
} = require('../../utils/catalog-api')

// 工具函数
function toArray(value) {
  if (Array.isArray(value)) return value
  if (value == null) return []
  return [value]
}



// 滑动删除配置（使用 px 单位，movable-view 需要具体像素值）
// 手指从右往左滑，商品左移，露出右侧删除按钮
const SWIPE_DISTANCE = -80 // 滑动展开距离（px，负数表示向左）
const SWIPE_THRESHOLD = -30 // 触发展开的阈值（px，负数表示向左）

Page({
  data: {
    pageState: 'loading',
    cartItems: [],
    recommendList: [],
    editMode: false,
    allChecked: false,
    selectedCount: 0,
    totalAmount: 0,
    actionDisabled: true,
    swipeItemId: '',
    skuPopupVisible: false,
    skuPopupLoading: false,
    popupProduct: null,
    popupSpecs: { size: [], material: [] },
    popupSelectedSpecs: { size: '', material: '' },
    popupSpecDisabledMap: { size: {}, material: {} },
    popupCurrentSku: null,
    popupQuantity: 1,
    pendingAction: 'add',
    submitLoading: false
  },
  _routing: false,

  onShow() {
    this.syncTabBar(2)
    this.initPage()
  },
  syncTabBar(index) {
    if (typeof this.getTabBar !== 'function') return
    const tabBar = this.getTabBar()
    if (tabBar && typeof tabBar.setData === 'function') {
      tabBar.setData({ selected: index })
    }
  },
  async initPage() {
    await Promise.all([this.loadCart(), this.loadRecommend()])
  },
  async loadCart() {
    this.setData({ pageState: 'loading' })
    try {
      const data = await getCart()
      const list = data.items || []
      if (!list.length) {
        this.setData({
          pageState: 'empty',
          cartItems: [],
          selectedCount: 0,
          totalAmount: 0,
          allChecked: false,
          actionDisabled: true,
          editMode: false
        })
        return
      }
      this.setData({
        cartItems: list.map((item) => ({ ...item, x: 0 })),
        selectedCount: data.selectedCount || 0,
        totalAmount: data.selectedAmount || 0,
        pageState: 'ready',
        swipeItemId: ''
      })
      this.calcSummary()
    } catch (e) {
      this.setData({ pageState: 'error' })
      wx.showToast({ title: e.message || '购物车加载失败', icon: 'none' })
    }
  },
  async loadRecommend() {
    try {
      const list = await getCartRecommend(6)
      this.setData({ recommendList: list })
    } catch (e) {
      this.setData({ recommendList: [] })
    }
  },
  calcSummary() {
    const checkedItems = this.data.cartItems.filter((item) => item.checked && !item.invalid)
    const selectedCount = checkedItems.reduce((sum, item) => sum + (item.quantity || 0), 0)
    const totalAmount = Number(checkedItems.reduce((sum, item) => sum + item.price * item.quantity, 0).toFixed(2))
    const validItems = this.data.cartItems.filter((item) => !item.invalid)
    const allChecked = !!validItems.length && validItems.every((item) => item.checked)
    this.setData({
      selectedCount,
      totalAmount,
      allChecked,
      actionDisabled: selectedCount <= 0
    })
  },
  onToggleEdit() {
    this.setData({ editMode: !this.data.editMode })
  },
  async onToggleItem(event) {
    const { id } = event.currentTarget.dataset
    const target = this.data.cartItems.find((item) => item.id === id)
    if (!target || target.invalid) return
    try {
      await updateCartItem(id, { checked: !target.checked })
      await this.loadCart()
    } catch (e) {
      wx.showToast({ title: e.message || '操作失败', icon: 'none' })
    }
  },
  async onToggleAll() {
    const nextChecked = !this.data.allChecked
    try {
      await selectAllCart(nextChecked)
      await this.loadCart()
    } catch (e) {
      wx.showToast({ title: e.message || '全选失败', icon: 'none' })
    }
  },
  async onChangeQty(event) {
    const { id, type } = event.currentTarget.dataset
    const target = this.data.cartItems.find((item) => item.id === id)
    if (!target || target.invalid) return
    if (type === 'dec' && target.quantity <= 1) return
    const delta = type === 'inc' ? 1 : -1
    const quantity = Math.max(1, Math.min(target.quantity + delta, target.stock || 999))
    try {
      await updateCartItem(id, { quantity })
      await this.loadCart()
    } catch (e) {
      wx.showToast({ title: e.message || '数量更新失败', icon: 'none' })
    }
  },
  async onDeleteChecked() {
    const checkedIds = this.data.cartItems.filter((item) => item.checked && !item.invalid).map((item) => item.id)
    if (!checkedIds.length) {
      wx.showToast({ title: '请先选择商品', icon: 'none' })
      return
    }

    wx.showModal({
      title: '提示',
      content: `确定删除已选中的 ${checkedIds.length} 件商品吗？`,
      success: async (res) => {
        if (!res.confirm) return
        try {
          await deleteCartItems(checkedIds)
          wx.showToast({ title: '删除成功', icon: 'success' })
          await this.initPage()
        } catch (e) {
          wx.showToast({ title: e.message || '删除失败', icon: 'none' })
        }
      }
    })
  },

  onMovableChange(event) {
    const { id, index } = event.currentTarget.dataset
    const { x } = event.detail
    const nextX = Math.max(SWIPE_DISTANCE, Math.min(0, x))
    this._swipeMeta = { id, index, x: nextX }
  },

  // 滑动结束时判断是否展开或回弹
  onMovableTouchEnd(event) {
    const { id, index } = event.currentTarget.dataset
    const item = this.data.cartItems[index]
    const x = typeof event.detail?.x === 'number'
      ? event.detail.x
      : this._swipeMeta && this._swipeMeta.id === id
        ? this._swipeMeta.x
        : item?.x || 0

    // 手指从右往左滑超过阈值则展开，露出右侧删除按钮
    const shouldOpen = x <= SWIPE_THRESHOLD
    const targetX = shouldOpen ? SWIPE_DISTANCE : 0

    const cartItems = this.data.cartItems.map((cartItem, idx) => {
      if (idx === index) {
        return { ...cartItem, x: targetX }
      }
      return { ...cartItem, x: 0 }
    })

    this._swipeMeta = null
    this.setData({ cartItems, swipeItemId: shouldOpen ? id : '' })
  },

  // 点击商品内容时关闭滑动
  onItemTap(event) {
    const { id, index } = event.currentTarget.dataset
    const item = this.data.cartItems[index]
    if (item && item.x < 0) {
      this.closeAllSwipe()
      return
    }
    this.onTapProduct({ currentTarget: { dataset: { productId: id } } })
  },

  closeAllSwipe() {
    if (!this.data.cartItems.length) return
    const cartItems = this.data.cartItems.map((item) => ({ ...item, x: 0 }))
    this.setData({ cartItems, swipeItemId: '' })
  },

  onPageTap() {
    const hasOpenItem = this.data.cartItems.some((item) => (item.x || 0) < 0)
    if (!hasOpenItem) return
    this.closeAllSwipe()
  },

  onSwipeDelete(event) {
    const { id } = event.currentTarget.dataset
    if (!id) return
    wx.showModal({
      title: '提示',
      content: '确定删除该商品吗？',
      success: async (res) => {
        if (!res.confirm) return
        try {
          await deleteCartItems([id])
          wx.showToast({ title: '删除成功', icon: 'success' })
          await this.loadCart()
        } catch (e) {
          wx.showToast({ title: e.message || '删除失败', icon: 'none' })
        }
      }
    })
  },
  async onSubmitAction() {
    if (this.data.actionDisabled) {
      wx.showToast({ title: this.data.editMode ? '请选择删除商品' : '请选择结算商品', icon: 'none' })
      return
    }
    if (this.data.editMode) {
      await this.onDeleteChecked()
      return
    }
    const checkedIds = this.data.cartItems
      .filter((item) => item.checked && !item.invalid)
      .map((item) => item.id)
    if (this._routing) return
    this._routing = true
    wx.setStorageSync('YH_CHECKOUT_IDS', checkedIds)
    wx.navigateTo({
      url: '/pages/order/confirm/index',
      complete: () => {
        setTimeout(() => {
          this._routing = false
        }, 300)
      }
    })
  },
  onTapProduct(event) {
    const { productId } = event.currentTarget.dataset || {}
    const id = String(productId || '').trim()
    if (!/^\d+$/.test(id) || this._routing) return
    this._routing = true
    wx.navigateTo({
      url: `/pages/product/detail?id=${id}`,
      complete: () => {
        setTimeout(() => {
          this._routing = false
        }, 300)
      }
    })
  },
  onTapRecommend(event) {
    const { id } = event.currentTarget.dataset || {}
    const productId = String(id || '').trim()
    if (!/^\d+$/.test(productId) || this._routing) return
    this._routing = true
    wx.navigateTo({
      url: `/pages/product/detail?id=${productId}`,
      complete: () => {
        setTimeout(() => {
          this._routing = false
        }, 300)
      }
    })
  },

  async onAddRecommendCart(event) {
    const { id } = event.currentTarget.dataset
    if (!id || this.data.skuPopupLoading) return

    this.setData({ skuPopupLoading: true })
    try {
      const product = this.data.recommendList.find((item) => String(item.id) === String(id)) || null
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
    const skus = toArray(raw.skus).map(normalizeSku).filter((item) => item.id)
    const data = {
      skus,
      sizeList: uniqueValues(skus.map((item) => item.size)),
      materialList: uniqueValues(skus.map((item) => item.material))
    }
    this.productSpecCache = this.productSpecCache || {}
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
    const sizeOptions = (this.data.popupSpecs || {}).size || []
    const materialOptions = (this.data.popupSpecs || {}).material || []
    const disabledMap = { size: {}, material: {} }
    sizeOptions.forEach((opt) => {
      const hasMatch = skuList.some((sku) => sku.size === opt && (sku.stock || 0) > 0 && (!selectedSpecs.material || sku.material === selectedSpecs.material))
      if (!hasMatch) disabledMap.size[opt] = true
    })
    materialOptions.forEach((opt) => {
      const hasMatch = skuList.some((sku) => sku.material === opt && (sku.stock || 0) > 0 && (!selectedSpecs.size || sku.size === selectedSpecs.size))
      if (!hasMatch) disabledMap.material[opt] = true
    })
    this.setData({ popupSpecDisabledMap: disabledMap })
  },
  onCloseSkuPopup() {
    this.setData({ skuPopupVisible: false })
  },
  onTapSkuPopupPanel() {
  },
  onSelectPopupSpec(event) {
    const { key, value } = event.currentTarget.dataset
    if ((this.data.popupSpecDisabledMap || {})[key]?.[value]) return
    const nextSpecs = { ...this.data.popupSelectedSpecs, [key]: value }
    const matched = this.getMatchedPopupSku(nextSpecs)
    this.setData({
      popupSelectedSpecs: nextSpecs,
      popupCurrentSku: matched
    })
    this.updatePopupDisabledMap(nextSpecs)
  },
  onChangePopupQty(event) {
    const { type } = event.currentTarget.dataset
    const stock = (this.data.popupCurrentSku || {}).stock || 0
    let nextQty = this.data.popupQuantity || 1
    if (type === 'dec') {
      nextQty = Math.max(1, nextQty - 1)
    } else if (type === 'inc') {
      nextQty = Math.min(stock || 999, nextQty + 1)
    }
    this.setData({ popupQuantity: nextQty })
  },
  async onConfirmPopupAddCart() {
    if (this.data.submitLoading) return
    const sku = this.data.popupCurrentSku || {}
    const product = this.data.popupProduct || {}
    if (!sku.id) {
      wx.showToast({ title: '请选择完整规格', icon: 'none' })
      return
    }
    this.setData({ submitLoading: true, pendingAction: 'add' })
    try {
      await addCart({ productId: product.id, skuId: sku.id, quantity: this.data.popupQuantity || 1 })
      wx.showToast({ title: '已加入购物车', icon: 'success' })
      this.setData({ skuPopupVisible: false })
      await this.loadCart()
    } catch (e) {
      wx.showToast({ title: e.message || '加入购物车失败', icon: 'none' })
    } finally {
      this.setData({ submitLoading: false, pendingAction: 'add' })
    }
  },
  async onConfirmPopupBuyNow() {
    if (this.data.submitLoading) return
    const sku = this.data.popupCurrentSku || {}
    const product = this.data.popupProduct || {}
    if (!sku.id) {
      wx.showToast({ title: '请选择完整规格', icon: 'none' })
      return
    }
    this.setData({ submitLoading: true, pendingAction: 'buy' })
    try {
      await addCart({ productId: product.id, skuId: sku.id, quantity: this.data.popupQuantity || 1 })
      await this.loadCart()
      const newItem = this.data.cartItems.find((item) => String(item.skuId) === String(sku.id))
      if (!newItem) {
        throw new Error('加入购物车失败')
      }
      this.setData({ skuPopupVisible: false })
      wx.setStorageSync('YH_CHECKOUT_IDS', [newItem.id])
      wx.navigateTo({ url: '/pages/order/confirm/index' })
    } catch (e) {
      wx.showToast({ title: e.message || '立即购买失败', icon: 'none' })
    } finally {
      this.setData({ submitLoading: false, pendingAction: 'add' })
    }
  },
  onContinueShopping() {
    if (this._routing) return
    this._routing = true
    wx.switchTab({
      url: '/pages/home/index',
      complete: () => {
        setTimeout(() => {
          this._routing = false
        }, 300)
      }
    })
  },

  onRetry() {
    this.initPage()
  }
})
