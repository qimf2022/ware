const { products } = require('../mock/data')

const CART_KEY = 'YH_CART_ITEMS'
const ADDRESS_KEY = 'YH_ADDRESS_LIST'
const ORDER_KEY = 'YH_ORDER_LIST'
const AFTER_SALE_KEY = 'YH_AFTER_SALE_LIST'

function clone(data) {
  return JSON.parse(JSON.stringify(data))
}

function getDefaultCartItems() {
  const source = products.slice(0, 2)
  return source.map((product, index) => {
    const sku = (product.skuList && product.skuList[0]) || {}
    return {
      id: `c_${product.id}_${index}`,
      productId: product.id,
      title: product.title,
      image: sku.image || product.image,
      size: sku.size || '',
      material: sku.material || '',
      price: sku.price || product.price,
      stock: sku.stock || 99,
      quantity: 1,
      checked: true,
      invalid: false
    }
  })
}

function getCartItems() {
  const cache = wx.getStorageSync(CART_KEY)
  if (Array.isArray(cache)) {
    return clone(cache)
  }
  const defaults = getDefaultCartItems()
  wx.setStorageSync(CART_KEY, defaults)
  return clone(defaults)
}

function setCartItems(items) {
  wx.setStorageSync(CART_KEY, clone(items || []))
}

function addToCart(payload) {
  const list = getCartItems()
  const target = list.find(
    (item) =>
      item.productId === payload.productId &&
      item.size === payload.size &&
      item.material === payload.material &&
      !item.invalid
  )
  if (target) {
    target.quantity = Math.min(target.quantity + (payload.quantity || 1), target.stock || 999)
  } else {
    list.unshift({
      id: `c_${Date.now()}_${Math.floor(Math.random() * 10000)}`,
      productId: payload.productId,
      title: payload.title,
      image: payload.image,
      size: payload.size,
      material: payload.material,
      price: payload.price,
      stock: payload.stock || 999,
      quantity: payload.quantity || 1,
      checked: true,
      invalid: false
    })
  }
  setCartItems(list)
  return clone(list)
}

function updateCartItem(itemId, patch) {
  const list = getCartItems()
  const next = list.map((item) => {
    if (item.id === itemId) {
      return {
        ...item,
        ...patch
      }
    }
    return item
  })
  setCartItems(next)
  return clone(next)
}

function removeCheckedCartItems() {
  const next = getCartItems().filter((item) => !item.checked)
  setCartItems(next)
  return clone(next)
}

function clearCartByIds(ids) {
  const idSet = new Set(ids || [])
  const next = getCartItems().filter((item) => !idSet.has(item.id))
  setCartItems(next)
  return clone(next)
}

function getDefaultAddresses() {
  return [
    {
      id: 'addr_1',
      name: '李女士',
      phone: '13800138000',
      province: '江苏省',
      city: '苏州市',
      district: '工业园区',
      detail: '星海街道 88 号 2 栋 1001',
      isDefault: true
    },
    {
      id: 'addr_2',
      name: '王先生',
      phone: '13900139000',
      province: '上海市',
      city: '上海市',
      district: '浦东新区',
      detail: '张江路 66 号 1 栋 302',
      isDefault: false
    }
  ]
}

function getAddresses() {
  const cache = wx.getStorageSync(ADDRESS_KEY)
  if (Array.isArray(cache)) {
    return clone(cache)
  }
  const defaults = getDefaultAddresses()
  wx.setStorageSync(ADDRESS_KEY, defaults)
  return clone(defaults)
}

function setAddresses(addresses) {
  wx.setStorageSync(ADDRESS_KEY, clone(addresses || []))
}

function upsertAddress(payload) {
  const list = getAddresses()
  const id = payload.id || `addr_${Date.now()}`
  const normalized = {
    ...payload,
    id,
    isDefault: !!payload.isDefault
  }

  let next
  const exists = list.some((item) => item.id === id)
  if (exists) {
    next = list.map((item) => (item.id === id ? normalized : item))
  } else {
    next = [normalized, ...list]
  }

  if (normalized.isDefault) {
    next = next.map((item) => ({ ...item, isDefault: item.id === id }))
  } else if (!next.some((item) => item.isDefault) && next[0]) {
    next[0].isDefault = true
  }

  setAddresses(next)
  return clone(next)
}

function deleteAddress(addressId) {
  let next = getAddresses().filter((item) => item.id !== addressId)
  if (!next.some((item) => item.isDefault) && next[0]) {
    next[0].isDefault = true
  }
  setAddresses(next)
  return clone(next)
}

function createOrder(payload) {
  const orderList = getOrders()
  const now = Date.now()
  const order = {
    id: `o_${now}`,
    orderNo: `YH${now}`,
    status: '待发货',
    createdAt: new Date().toLocaleString(),
    items: clone(payload.items || []),
    address: clone(payload.address || null),
    remark: payload.remark || '',
    amounts: {
      productAmount: payload.productAmount || 0,
      freightAmount: payload.freightAmount || 0,
      payAmount: payload.payAmount || 0
    },
    logistics: {
      company: '顺丰速运',
      trackingNo: `SF${now}`,
      signed: false,
      traces: [
        {
          time: new Date().toLocaleString(),
          status: '订单已创建，待仓库打包'
        }
      ]
    }
  }
  orderList.unshift(order)
  wx.setStorageSync(ORDER_KEY, orderList)
  return clone(order)
}

function getOrders() {
  const cache = wx.getStorageSync(ORDER_KEY)
  if (Array.isArray(cache)) {
    return clone(cache)
  }
  return []
}

function getOrderById(orderId) {
  return getOrders().find((item) => item.id === orderId)
}

function getLogisticsByOrderId(orderId) {
  const order = getOrderById(orderId)
  if (!order) return null
  const baseLogistics = order.logistics || {}
  const traces = Array.isArray(baseLogistics.traces) ? baseLogistics.traces : []
  return {
    orderId: order.id,
    orderNo: order.orderNo,
    company: baseLogistics.company || '顺丰速运',
    trackingNo: baseLogistics.trackingNo || `SF${order.id.slice(-8)}`,
    signed: !!baseLogistics.signed,
    traces: traces.length
      ? traces
      : [
          { time: order.createdAt, status: '订单已创建，待仓库打包' },
          { time: order.createdAt, status: '商家已接单，准备出库' }
        ]
  }
}

function getAfterSaleList() {
  const cache = wx.getStorageSync(AFTER_SALE_KEY)
  if (Array.isArray(cache)) {
    return clone(cache)
  }
  return []
}

function createAfterSale(payload) {
  const list = getAfterSaleList()
  const now = Date.now()
  const record = {
    id: `as_${now}`,
    orderId: payload.orderId,
    orderNo: payload.orderNo || '',
    type: payload.type,
    reason: payload.reason,
    desc: payload.desc || '',
    status: '审核中',
    createdAt: new Date().toLocaleString(),
    refundAmount: payload.refundAmount || 0,
    logs: [
      { time: new Date().toLocaleString(), status: '售后申请已提交' },
      { time: new Date().toLocaleString(), status: '平台审核中，请耐心等待' }
    ]
  }
  list.unshift(record)
  wx.setStorageSync(AFTER_SALE_KEY, list)
  return clone(record)
}

function getAfterSaleById(id) {
  return getAfterSaleList().find((item) => item.id === id)
}

function getAfterSaleByOrderId(orderId) {
  return getAfterSaleList().find((item) => item.orderId === orderId)
}

module.exports = {
  getCartItems,
  setCartItems,
  addToCart,
  updateCartItem,
  removeCheckedCartItems,
  clearCartByIds,
  getAddresses,
  upsertAddress,
  deleteAddress,
  createOrder,
  getOrders,
  getOrderById,
  getLogisticsByOrderId,
  createAfterSale,
  getAfterSaleById,
  getAfterSaleByOrderId
}
