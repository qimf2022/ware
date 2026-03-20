const { request, resolveAssetUrl } = require('./request')


function toNumber(value, fallback = 0) {
  const n = Number(value)
  return Number.isFinite(n) ? n : fallback
}

function normalizeProduct(item = {}) {
  return {
    id: String(item.id || ''),
    title: item.title || '',
    subtitle: item.subtitle || '',
    image: resolveAssetUrl(item.main_image || item.image || ''),

    price: toNumber(item.min_price !== undefined ? item.min_price : item.price),
    originPrice: toNumber(
      item.original_min_price !== undefined
        ? item.original_min_price
        : item.originPrice !== undefined
        ? item.originPrice
        : item.max_price !== undefined
        ? item.max_price
        : item.price
    ),
    soldCount: toNumber(item.sales_count !== undefined ? item.sales_count : item.soldCount),
    category: item.category && item.category.name ? item.category.name : item.category || ''
  }
}

function parseSpecMap(raw) {
  if (!raw) return {}
  if (typeof raw === 'string') {
    try {
      const parsed = JSON.parse(raw)
      return parsed && typeof parsed === 'object' ? parsed : {}
    } catch (e) {
      return {}
    }
  }
  return typeof raw === 'object' ? raw : {}
}

function pickFirstValue(source = {}, keys = []) {
  for (let i = 0; i < keys.length; i += 1) {
    const key = keys[i]
    const value = source[key]
    if (value !== undefined && value !== null && String(value).trim() !== '') {
      return String(value).trim()
    }
  }
  return ''
}

function normalizeSku(sku = {}) {
  const specs = parseSpecMap(sku.specs_json)
  return {
    id: String(sku.id || ''),
    size: pickFirstValue(specs, ['size', '尺寸', 'spec', '规格', 'style', '款式', '型号']),
    material: pickFirstValue(specs, ['material', '材质', 'fabric', '面料', 'style', '花型', 'pattern', 'color', '颜色']),
    price: toNumber(sku.price),
    originPrice: toNumber(sku.original_price !== undefined ? sku.original_price : sku.price),
    stock: toNumber(sku.stock, 0),
    image: resolveAssetUrl(sku.image_url || '')

  }
}

function uniqueValues(list) {
  return Array.from(new Set((list || []).filter(Boolean)))
}

async function getHomeConfig() {
  return request({ url: '/home/config', method: 'GET', withAuth: false })
}

async function getHomeRecommend(page = 1, pageSize = 20) {
  return request({
    url: '/home/recommend',
    method: 'GET',
    withAuth: false,
    data: { page, page_size: pageSize }
  })
}

async function getCategories() {
  return request({ url: '/categories', method: 'GET', withAuth: false })
}

async function getProducts(params = {}) {
  const payload = {
    category_id: params.categoryId,
    keyword: params.keyword,
    sort: params.sort,
    page: params.page || 1,
    page_size: params.pageSize || 20
  }
  return request({ url: '/products', method: 'GET', data: payload })
}

async function getProductDetail(productId) {
  return request({ url: `/products/${productId}`, method: 'GET' })
}

async function getProductSpecs(productId) {
  return request({ url: `/products/${productId}/specs`, method: 'GET' })
}

async function getProductRecommend(productId, limit = 4) {
  return request({
    url: `/products/${productId}/recommend`,
    method: 'GET',
    data: { limit }
  })
}

async function getSearchHot() {
  return request({ url: '/search/hot', method: 'GET', withAuth: false })
}

async function getSearchSuggest(keyword) {
  return request({ url: '/search/suggest', method: 'GET', data: { keyword }, withAuth: false })
}

async function getSearchHistory(limit = 10) {
  return request({ url: '/search/history', method: 'GET', data: { limit } })
}

async function clearSearchHistory() {
  return request({ url: '/search/history', method: 'DELETE' })
}

module.exports = {
  getHomeConfig,
  getHomeRecommend,
  getCategories,
  getProducts,
  getProductDetail,
  getProductSpecs,
  getProductRecommend,
  getSearchHot,
  getSearchSuggest,
  getSearchHistory,
  clearSearchHistory,
  normalizeProduct,
  normalizeSku,
  uniqueValues
}
