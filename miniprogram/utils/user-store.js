const { products } = require('../mock/data')

const PROFILE_KEY = 'YH_USER_PROFILE'
const FAVORITES_KEY = 'YH_USER_FAVORITES'
const COUPON_KEY = 'YH_USER_COUPONS'

function clone(data) {
  return JSON.parse(JSON.stringify(data))
}

function getDefaultProfile() {
  return {
    nickname: '诗语访客',
    memberLevel: '银卡会员',
    points: 1280,
    couponCount: 3,
    favoriteCount: 0,
    phone: ''
  }
}

function getProfile() {
  const cache = wx.getStorageSync(PROFILE_KEY)
  if (cache && typeof cache === 'object') {
    return clone(cache)
  }
  const defaults = getDefaultProfile()
  wx.setStorageSync(PROFILE_KEY, defaults)
  return clone(defaults)
}

function setProfile(profile) {
  wx.setStorageSync(PROFILE_KEY, clone(profile || {}))
}

function bindMobile(phone) {
  const profile = getProfile()
  const next = {
    ...profile,
    phone,
    nickname: profile.nickname === '诗语访客' ? '诗语用户' : profile.nickname
  }
  setProfile(next)
  return clone(next)
}

function normalizeProduct(product) {
  const sku = (product.skuList && product.skuList[0]) || {}
  return {
    id: product.id,
    title: product.title,
    subtitle: product.subtitle,
    image: sku.image || product.image,
    price: sku.price || product.price,
    originPrice: sku.originPrice || product.originPrice,
    size: sku.size || '',
    material: sku.material || '',
    stock: sku.stock || 99
  }
}

function getFavorites() {
  const cache = wx.getStorageSync(FAVORITES_KEY)
  if (Array.isArray(cache)) {
    return clone(cache)
  }
  const defaults = products.slice(0, 3).map(normalizeProduct)
  wx.setStorageSync(FAVORITES_KEY, defaults)
  const profile = getProfile()
  profile.favoriteCount = defaults.length
  setProfile(profile)
  return clone(defaults)
}

function setFavorites(list) {
  wx.setStorageSync(FAVORITES_KEY, clone(list || []))
  const profile = getProfile()
  profile.favoriteCount = (list || []).length
  setProfile(profile)
}

function removeFavorite(productId) {
  const next = getFavorites().filter((item) => item.id !== productId)
  setFavorites(next)
  return clone(next)
}

function getFootprintGroups() {
  const source = products.map(normalizeProduct)
  return [
    { date: '今天', items: source.slice(0, 2) },
    { date: '昨天', items: source.slice(2, 4) }
  ]
}

function getCoupons() {
  const cache = wx.getStorageSync(COUPON_KEY)
  if (Array.isArray(cache)) {
    return clone(cache)
  }
  const defaults = [
    { id: 'cp_1', title: '满399减40', amount: 40, threshold: 399, expireAt: '2026-04-30', scope: '全场床品', claimed: true },
    { id: 'cp_2', title: '满699减90', amount: 90, threshold: 699, expireAt: '2026-04-30', scope: '套件/被芯', claimed: false },
    { id: 'cp_3', title: '新人专享95折', amount: 0, threshold: 0, discount: 9.5, expireAt: '2026-05-15', scope: '首单可用', claimed: false }
  ]
  wx.setStorageSync(COUPON_KEY, defaults)
  return clone(defaults)
}

function claimCoupon(couponId) {
  const next = getCoupons().map((item) => (item.id === couponId ? { ...item, claimed: true } : item))
  wx.setStorageSync(COUPON_KEY, next)
  const profile = getProfile()
  profile.couponCount = next.filter((item) => item.claimed).length
  setProfile(profile)
  return clone(next)
}

function getPointsSummary() {
  return {
    balance: getProfile().points,
    records: [
      { id: 'pt_1', title: '签到奖励', amount: '+20', time: '2026-03-12 09:20' },
      { id: 'pt_2', title: '订单抵扣', amount: '-300', time: '2026-03-11 18:02' },
      { id: 'pt_3', title: '评价返积分', amount: '+80', time: '2026-03-10 20:16' }
    ],
    rules: '100积分可抵扣1元，单笔订单最高可抵扣实付金额的20%。'
  }
}

function getGiftCardSummary() {
  return {
    balance: 260,
    cards: [
      { id: 'gc_1', title: '春季礼品卡', amount: 200, status: '可用', expireAt: '2026-08-31' },
      { id: 'gc_2', title: '老客回馈卡', amount: 60, status: '可用', expireAt: '2026-06-30' }
    ],
    records: [
      { id: 'gr_1', title: '订单抵扣', amount: '-39', time: '2026-03-09 13:22' },
      { id: 'gr_2', title: '活动赠送', amount: '+60', time: '2026-03-06 10:02' }
    ]
  }
}

function getCampaignList() {
  return [
    {
      id: 'act_1',
      title: '春日焕新季',
      desc: '套件/被芯组合购，满699立减90，可叠加积分抵扣。',
      couponHint: '专属券：满699减90'
    },
    {
      id: 'act_2',
      title: '婚嫁专区',
      desc: '婚庆四件套、龙凤绣枕、仪式感软装一站购齐。',
      couponHint: '加赠婚嫁礼包券'
    },
    {
      id: 'act_3',
      title: '老客复购计划',
      desc: '近90天购买用户可领取复购券并获得补货提醒。',
      couponHint: '复购券：满399减40'
    }
  ]
}

module.exports = {
  getProfile,
  bindMobile,
  getFavorites,
  removeFavorite,
  getFootprintGroups,
  getCoupons,
  claimCoupon,
  getPointsSummary,
  getGiftCardSummary,
  getCampaignList
}
