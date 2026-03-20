const { request, resolveAssetUrl } = require('./request')

const PROFILE_CACHE_KEY = 'YH_PROFILE_CACHE'
const DEFAULT_NICKNAMES = ['诗语用户', '诗语访客']

function toNumber(value, fallback = 0) {
  const n = Number(value)
  return Number.isFinite(n) ? n : fallback
}

function toArray(value) {
  return Array.isArray(value) ? value : []
}

function memberLevelText(code) {
  const map = {
    silver: '银卡会员',
    gold: '金卡会员',
    platinum: '铂金会员'
  }
  return map[code] || '普通会员'
}

function normalizeProductCard(item = {}) {
  return {
    id: String(item.id || ''),
    title: item.title || '',
    subtitle: '',
    image: resolveAssetUrl(item.main_image || ''),

    price: toNumber(item.min_price),
    originPrice: toNumber(item.max_price !== undefined ? item.max_price : item.min_price),
    size: '',
    material: '',
    stock: 99
  }
}

function formatDateLabel(dateTime = '') {
  const value = String(dateTime)
  if (!value) return '更早'
  return value.slice(0, 10)
}

function getProfileCache() {
  const cache = wx.getStorageSync(PROFILE_CACHE_KEY)
  if (!cache || typeof cache !== 'object') return {}
  return {
    nickname: cache.nickname || '',
    avatarUrl: cache.avatarUrl || ''
  }
}

function sanitizeWechatNickname(nickname = '') {
  const value = String(nickname || '').trim()
  if (!value) return ''
  if (value === '微信用户' || value.indexOf('微信用户') === 0) return ''
  return value
}

function cacheProfilePatch(payload = {}) {
  const nickname = sanitizeWechatNickname(payload.nickname || '')
  const avatarUrl = payload.avatarUrl || ''
  if (!nickname && !avatarUrl) return
  const current = getProfileCache()
  const next = {
    nickname: nickname || current.nickname || '',
    avatarUrl: avatarUrl || current.avatarUrl || ''
  }
  wx.setStorageSync(PROFILE_CACHE_KEY, next)
}


function mergeNickname(serverNickname = '', cacheNickname = '') {
  if (serverNickname && !DEFAULT_NICKNAMES.includes(serverNickname)) return serverNickname
  return cacheNickname || serverNickname || '诗语用户'
}

async function getProfile() {
  const data = await request({ url: '/user/profile', method: 'GET' })
  const stats = data.stats || {}
  const cache = getProfileCache()
  const avatarUrl = resolveAssetUrl(data.avatar_url || '') || cache.avatarUrl || ''
  const nickname = mergeNickname(data.nickname || '', cache.nickname || '')
  return {
    id: String(data.id || ''),
    nickname,
    memberLevel: memberLevelText(data.member_level_code),
    phone: data.phone_mask || '',
    avatarUrl,
    gender: toNumber(data.gender),
    couponCount: toNumber(stats.coupon_count),
    points: toNumber(stats.points),
    favoriteCount: toNumber(stats.favorite_count),
    footprintCount: toNumber(stats.footprint_count),
    orderCount: toNumber(stats.order_count),
    cardBalance: toNumber(stats.card_balance)
  }
}


async function updateProfile(payload = {}) {
  const hasNickname = !!(payload.nickname && String(payload.nickname).trim())
  const hasAvatar = !!(payload.avatarUrl && String(payload.avatarUrl).trim())
  if (hasNickname || hasAvatar) {
    cacheProfilePatch({
      nickname: payload.nickname,
      avatarUrl: payload.avatarUrl
    })
  }
  return request({
    url: '/user/profile',
    method: 'PUT',
    data: {
      nickname: payload.nickname,
      avatar_url: payload.avatarUrl,
      gender: payload.gender
    }
  })
}

async function getFavorites(page = 1, pageSize = 20) {
  const data = await request({
    url: '/user/favorites',
    method: 'GET',
    data: { page, page_size: pageSize }
  })
  return {
    list: toArray(data.list).map((item) => normalizeProductCard(item.product || {})),
    total: toNumber(data.total)
  }
}

async function favoriteAction(productId, action) {
  return request({
    url: '/user/favorites',
    method: 'POST',
    data: {
      product_id: Number(productId),
      action
    }
  })
}

async function getFootprintGroups(page = 1, pageSize = 40) {
  const data = await request({
    url: '/user/footprints',
    method: 'GET',
    data: { page, page_size: pageSize }
  })
  const groups = {}
  toArray(data.list).forEach((item) => {
    const key = formatDateLabel(item.viewed_at)
    if (!groups[key]) groups[key] = []
    groups[key].push(normalizeProductCard(item.product || {}))
  })
  return Object.keys(groups)
    .sort((a, b) => (a > b ? -1 : 1))
    .map((date) => ({ date, items: groups[date] }))
}

async function getCouponCenter(page = 1, pageSize = 20) {
  const data = await request({
    url: '/coupons/available',
    method: 'GET',
    withAuth: false,
    data: { page, page_size: pageSize }
  })
  return {
    list: toArray(data.list).map((item) => ({
      id: String(item.id || ''),
      title: item.name || '',
      amount: toNumber(item.type === 1 ? item.value : 0),
      threshold: toNumber(item.threshold),
      discount: item.type === 2 ? toNumber(item.value) / 10 : 0,
      expireAt: item.end_time || '',
      scope: item.use_rule_desc || '全场可用',
      claimed: !!item.received
    }))
  }
}

async function claimCoupon(couponId) {
  return request({
    url: `/coupons/${couponId}/receive`,
    method: 'POST',
    requiresSignature: true,
    idempotent: true,
    data: {}
  })
}

async function getPointsSummary(page = 1, pageSize = 20) {
  const data = await request({
    url: '/user/points/logs',
    method: 'GET',
    data: { page, page_size: pageSize }
  })
  return {
    balance: toNumber(data.available_points),
    records: toArray(data.list).map((item) => ({
      id: String(item.id || ''),
      title: item.remark || item.change_type_text || '',
      amount: `${toNumber(item.change_type) === 1 ? '+' : '-'}${Math.abs(toNumber(item.points))}`,
      time: item.created_at || ''
    })),
    rules: '100积分可抵扣10元，具体以结算页为准。'
  }
}

module.exports = {
  getProfile,
  updateProfile,
  cacheProfilePatch,
  getFavorites,
  favoriteAction,
  getFootprintGroups,
  getCouponCenter,
  claimCoupon,
  getPointsSummary
}

