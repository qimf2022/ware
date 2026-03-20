const { request, resolveAssetUrl } = require('./request')

const { getProductSpecs } = require('./catalog-api')

function toNumber(value, fallback = 0) {
  const n = Number(value)
  return Number.isFinite(n) ? n : fallback
}

function toArray(value) {
  return Array.isArray(value) ? value : []
}

function parseSkuSpec(specs = {}) {
  const size = specs.size || specs.尺寸 || specs.spec || specs.规格 || specs.style || specs.款式 || specs.型号 || ''
  const material = specs.material || specs.材质 || specs.fabric || specs.面料 || specs.花型 || specs.pattern || specs.color || specs.颜色 || ''
  return { size: String(size || ''), material: String(material || '') }
}

function normalizeCartItem(item = {}, invalid = false) {
  const product = item.product || {}
  const sku = item.sku || {}
  const spec = parseSkuSpec(sku.specs_json || {})
  return {
    id: String(item.id || ''),
    productId: String(product.id || ''),
    skuId: String(sku.id || ''),
    title: product.title || '',
    image: resolveAssetUrl(product.main_image || ''),
    shopName: product.shop_name || product.brand_name || '',
    brandName: product.brand_name || '',

    size: spec.size,
    material: spec.material,
    price: toNumber(sku.price),
    originPrice: toNumber(sku.original_price || sku.price),
    stock: toNumber(sku.stock, 0),
    quantity: toNumber(item.quantity, 1),
    checked: toNumber(item.is_selected, 0) === 1,
    invalid: invalid || !!item.invalid_reason,
    invalidReason: item.invalid_reason || '',
    subtotal: toNumber(item.subtotal)
  }
}

function normalizeRecommendProduct(item = {}) {
  return {
    id: String(item.id || ''),
    title: item.title || '',
    subtitle: item.subtitle || '',
    image: resolveAssetUrl(item.main_image || ''),
    price: toNumber(item.min_price),
    originPrice: toNumber(item.original_min_price || item.min_price),
    soldCount: toNumber(item.sales_count),
    category: (item.category && item.category.name) || ''
  }
}


function normalizeAddress(item = {}) {
  return {
    id: String(item.id || ''),
    name: item.consignee || '',
    phone: item.phone_mask || '',
    province: item.province || '',
    city: item.city || '',
    district: item.district || '',
    detail: `${item.street || ''}${item.detail_address || ''}`,
    street: item.street || '',
    detailAddress: item.detail_address || '',
    isDefault: toNumber(item.is_default, 0) === 1
  }
}

function parseSkuText(text = '') {
  const parts = String(text).split('/').filter(Boolean)
  return {
    size: parts[0] || '',
    material: parts[1] || ''
  }
}

function normalizeOrderSummary(item = {}) {
  return {
    id: String(item.id || ''),
    orderNo: item.order_no || '',
    status: item.status_text || '',
    createdAt: item.created_at || '',
    amounts: {
      payAmount: toNumber(item.pay_amount)
    }
  }
}

function normalizeOrderDetail(data = {}) {
  const address = data.consignee_info || {}
  const items = toArray(data.items).map((item) => {
    const spec = parseSkuText(item.sku_specs)
    return {
      id: String(item.id || ''),
      title: item.product_title || '',
      image: resolveAssetUrl(item.product_image || ''),

      size: spec.size,
      material: spec.material,
      price: toNumber(item.unit_price),
      quantity: toNumber(item.quantity, 1),
      subtotal: toNumber(item.subtotal),
      orderItemId: String(item.id || '')
    }
  })

  const hasAfterSale = toArray(data.after_sales).length > 0

  return {
    id: String(data.id || ''),
    orderNo: data.order_no || '',
    status: data.status_text || '',
    createdAt: data.created_at || '',
    amounts: {
      productAmount: toNumber(data.total_amount),
      freightAmount: toNumber(data.freight_amount),
      payAmount: toNumber(data.pay_amount)
    },
    address: {
      name: address.consignee || '',
      phone: address.phone_mask || '',
      province: '',
      city: '',
      district: '',
      detail: address.full_address || ''
    },
    items,
    remark: data.remark || '',
    afterSaleRecord: hasAfterSale ? { id: String(data.after_sales[0].id || '') } : null
  }
}

function normalizeAfterSaleDetail(data = {}) {
  return {
    id: String(data.after_sale_no || data.id || ''),
    rawId: String(data.id || ''),
    orderId: String((data.order && data.order.id) || ''),
    orderNo: (data.order && data.order.order_no) || '',
    type: data.type_text || '',
    status: data.status_text || '',
    reason: data.reason_code || '',
    desc: data.reason_desc || '',
    logs: toArray(data.logs).map((log) => ({
      status: log.remark || log.action || '',
      time: log.created_at || ''
    }))
  }
}

async function getCart() {
  const data = await request({ url: '/cart', method: 'GET' })
  const valid = toArray(data.items).map((item) => normalizeCartItem(item, false))
  const invalid = toArray(data.invalid_items).map((item) => normalizeCartItem(item, true))
  return {
    items: valid.concat(invalid),
    selectedCount: toNumber(data.selected_count),
    selectedAmount: toNumber(data.selected_amount),
    totalAmount: toNumber(data.total_amount)
  }
}

async function addCart(payload = {}) {
  return request({
    url: '/cart',
    method: 'POST',
    data: {
      product_id: Number(payload.productId),
      sku_id: Number(payload.skuId),
      quantity: payload.quantity || 1
    }
  })
}

async function quickAddProduct(productId, quantity = 1) {
  const specData = await getProductSpecs(productId)
  const sku = toArray(specData.skus).find((item) => toNumber(item.stock, 0) > 0) || toArray(specData.skus)[0]
  if (!sku || !sku.id) {
    throw new Error('该商品暂无可用规格')
  }
  return addCart({ productId, skuId: sku.id, quantity })
}

async function updateCartItem(cartId, patch = {}) {
  return request({
    url: `/cart/${cartId}`,
    method: 'PUT',
    data: {
      quantity: patch.quantity,
      is_selected: patch.checked === undefined ? undefined : patch.checked ? 1 : 0
    }
  })
}

async function selectAllCart(checked) {
  return request({
    url: '/cart/select-all',
    method: 'PUT',
    data: { is_selected: checked ? 1 : 0 }
  })
}

async function batchUpdateCart(ids = [], patch = {}) {
  return request({
    url: '/cart/batch',
    method: 'PUT',
    data: {
      ids: ids.map((id) => Number(id)),
      quantity: patch.quantity,
      is_selected: patch.checked === undefined ? undefined : patch.checked ? 1 : 0
    }
  })
}

async function deleteCartItems(ids = []) {
  return request({
    url: '/cart',
    method: 'DELETE',
    data: { ids: ids.map((id) => Number(id)) }
  })
}

async function getCartRecommend(limit = 6) {
  const data = await request({
    url: '/cart/recommend',
    method: 'GET',
    data: { limit }
  })
  return toArray(data.list).map(normalizeRecommendProduct)
}


async function listAddresses() {
  const data = await request({ url: '/addresses', method: 'GET' })
  return toArray(data.list).map(normalizeAddress)
}

async function upsertAddress(form = {}) {
  const payload = {
    consignee: form.name,
    phone: form.phone,
    province: form.province,
    city: form.city,
    district: form.district,
    street: form.street || '',
    detail_address: form.detailAddress || form.detail,
    is_default: form.isDefault ? 1 : 0
  }
  if (form.id) {
    await request({ url: `/addresses/${form.id}`, method: 'PUT', data: payload })
    return { id: form.id }
  }
  return request({ url: '/addresses', method: 'POST', data: payload })
}

async function deleteAddress(id) {
  return request({ url: `/addresses/${id}`, method: 'DELETE' })
}

async function setDefaultAddress(id) {
  return request({ url: `/addresses/${id}/default`, method: 'PUT' })
}

async function confirmOrder(payload = {}) {
  return request({
    url: '/orders/confirm',
    method: 'POST',
    data: {
      cart_ids: (payload.cartIds || []).map((id) => Number(id)),
      address_id: Number(payload.addressId)
    }
  })
}

async function createOrder(payload = {}) {
  return request({
    url: '/orders',
    method: 'POST',
    requiresSignature: true,
    idempotent: true,
    data: {
      cart_ids: (payload.cartIds || []).map((id) => Number(id)),
      product_id: payload.productId ? Number(payload.productId) : undefined,
      sku_id: payload.skuId ? Number(payload.skuId) : undefined,
      quantity: payload.quantity ? Number(payload.quantity) : undefined,
      address_id: Number(payload.addressId),
      remark: payload.remark || '',
      source_type: payload.sourceType || 'miniapp_cart'
    }
  })
}

async function payOrder(orderId, payChannel = 1) {
  return request({
    url: `/orders/${orderId}/pay`,
    method: 'POST',
    requiresSignature: true,
    idempotent: true,
    data: {
      pay_channel: payChannel
    }
  })
}

async function requestWechatPay(orderId, payChannel = 1) {
  const payData = await payOrder(orderId, payChannel)
  const payParams = payData.pay_params || {}
  const paymentData = {
    timeStamp: String(payParams.timeStamp || ''),
    nonceStr: String(payParams.nonceStr || ''),
    package: String(payParams.package || ''),
    signType: String(payParams.signType || 'RSA'),
    paySign: String(payParams.paySign || '')
  }

  if (!paymentData.timeStamp || !paymentData.nonceStr || !paymentData.package || !paymentData.paySign) {
    const err = new Error('支付参数异常')
    err.raw = payData
    throw err
  }

  return new Promise((resolve, reject) => {
    wx.requestPayment({
      ...paymentData,
      success: (res) => {
        resolve({ payData, res })
      },
      fail: (err) => {
        const raw = (err && err.errMsg) || ''
        const cancelled = raw.indexOf('cancel') > -1
        const ex = new Error(cancelled ? '用户取消支付' : '微信支付失败')
        ex.cancelled = cancelled
        ex.raw = err
        reject(ex)
      }
    })
  })
}


async function listOrders(params = {}) {
  const data = await request({
    url: '/orders',
    method: 'GET',
    data: {
      page: params.page || 1,
      page_size: params.pageSize || 20,
      status: params.status || ''
    }
  })
  return {
    list: toArray(data.list).map(normalizeOrderSummary),
    total: toNumber(data.total),
    hasMore: !!data.has_more
  }
}

async function getOrderDetail(orderId) {
  const data = await request({ url: `/orders/${orderId}`, method: 'GET' })
  return normalizeOrderDetail(data)
}

async function getOrderLogistics(orderId) {
  const data = await request({ url: `/orders/${orderId}/logistics`, method: 'GET' })
  const shipments = toArray(data.shipments)
  const first = shipments[0]
  if (!first) return null
  return {
    company: first.company_name || '',
    trackingNo: first.tracking_no || '',
    signed: toNumber(first.ship_status) === 30,
    traces: toArray(first.tracks).map((track) => ({
      time: track.node_time || '',
      status: track.node_content || track.node_status || ''
    }))
  }
}

async function cancelOrder(orderId, reason = '') {
  return request({
    url: `/orders/${orderId}/cancel`,
    method: 'POST',
    data: { cancel_reason: reason }
  })
}

async function receiveOrder(orderId) {
  return request({
    url: `/orders/${orderId}/receive`,
    method: 'POST'
  })
}

async function applyAfterSale(payload = {}) {
  return request({
    url: '/after-sales',
    method: 'POST',
    requiresSignature: true,
    idempotent: true,
    data: {
      order_id: Number(payload.orderId),
      order_item_id: Number(payload.orderItemId),
      type: Number(payload.type || 1),
      reason_code: payload.reasonCode || '其他',
      reason_desc: payload.reasonDesc || '',
      apply_amount: toNumber(payload.applyAmount)
    }
  })
}

async function listAfterSales(params = {}) {
  const data = await request({
    url: '/after-sales',
    method: 'GET',
    data: {
      page: params.page || 1,
      page_size: params.pageSize || 20,
      status: params.status || ''
    }
  })
  return {
    list: toArray(data.list).map(normalizeAfterSaleSummary),
    total: toNumber(data.total),
    hasMore: !!data.has_more
  }
}

function normalizeAfterSaleSummary(item = {}) {
  return {
    id: String(item.id || ''),
    afterSaleNo: item.after_sale_no || '',
    type: item.type_text || '',
    status: item.status_text || '',
    createdAt: item.created_at || ''
  }
}

async function getAfterSaleDetail(afterSaleId) {
  const data = await request({ url: `/after-sales/${afterSaleId}`, method: 'GET' })
  return normalizeAfterSaleDetail(data)
}

module.exports = {
  getCart,
  addCart,
  quickAddProduct,
  updateCartItem,
  selectAllCart,
  batchUpdateCart,
  deleteCartItems,
  getCartRecommend,
  listAddresses,

  upsertAddress,
  deleteAddress,
  setDefaultAddress,
  confirmOrder,
  createOrder,
  payOrder,
  requestWechatPay,
  listOrders,
  getOrderDetail,
  getOrderLogistics,
  cancelOrder,
  receiveOrder,
  applyAfterSale,
  listAfterSales,
  getAfterSaleDetail
}
