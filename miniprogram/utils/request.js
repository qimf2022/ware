const DEFAULT_BASE_URL = 'http://192.168.0.101:8099/api/v1'
const BASE_URL = DEFAULT_BASE_URL
const REQUEST_PATH_PREFIX = '/api/v1'
const TOKEN_KEY = 'YH_ACCESS_TOKEN'
const BASE_URL_KEY = 'YH_BASE_URL'


const USER_ID_KEY = 'YH_USER_ID'
const AUTH_MODE_KEY = 'YH_AUTH_MODE'
const SIGN_SECRET = 'shiyu-signature-key-2026'

let loginPromise = null

function toQuery(params) {
  const pairs = []
  Object.keys(params || {}).forEach((key) => {
    const value = params[key]
    if (value === undefined || value === null || value === '') return
    pairs.push(`${encodeURIComponent(key)}=${encodeURIComponent(value)}`)
  })
  return pairs.join('&')
}

function getBaseUrl() {
  const custom = String(wx.getStorageSync(BASE_URL_KEY) || '').trim()
  if (!custom) return DEFAULT_BASE_URL
  return custom.replace(/\/$/, '')
}

function getApiOrigin() {
  return getBaseUrl().replace(/\/api\/v1$/, '')
}

function withNetworkHint(message = '', requestUrl = '') {
  const msg = String(message || '').trim()
  const url = String(requestUrl || '').trim()
  if (url.indexOf('localhost') > -1 || url.indexOf('127.0.0.1') > -1) {
    return `${msg || '网络请求失败'}（真机请将 YH_BASE_URL 改为电脑局域网IP地址）`
  }
  return msg || '网络请求失败'
}

function wxRequest(options) {
  return new Promise((resolve, reject) => {
    wx.request({
      ...options,
      success: resolve,
      fail: (err) => {
        const raw = (err && err.errMsg) || ''
        reject(new Error(withNetworkHint(raw, options && options.url)))
      }
    })
  })
}


function setAuthMode(mode = 'guest') {
  wx.setStorageSync(AUTH_MODE_KEY, mode)
}

function getAuthMode() {
  return wx.getStorageSync(AUTH_MODE_KEY) || 'guest'
}

function hasLocalToken() {
  return !!wx.getStorageSync(TOKEN_KEY)
}

function clearAuthCache() {

  wx.removeStorageSync(TOKEN_KEY)
  wx.removeStorageSync(USER_ID_KEY)
  setAuthMode('guest')
}

function randomString(length = 16) {
  return `${Date.now().toString(36)}${Math.random().toString(36).slice(2)}`.slice(0, length)
}

function rightRotate(value, amount) {
  return (value >>> amount) | (value << (32 - amount))
}

function sha256(ascii) {
  const maxWord = Math.pow(2, 32)
  let result = ''

  const words = []
  const asciiBitLength = ascii.length * 8

  const initialHash = (sha256.h = sha256.h || [])
  const k = (sha256.k = sha256.k || [])
  let primeCounter = k.length

  const isComposite = {}
  for (let candidate = 2; primeCounter < 64; candidate += 1) {
    if (!isComposite[candidate]) {
      for (let i = 0; i < 313; i += candidate) {
        isComposite[i] = candidate
      }
      initialHash[primeCounter] = (Math.pow(candidate, 0.5) * maxWord) | 0
      k[primeCounter] = (Math.pow(candidate, 1 / 3) * maxWord) | 0
      primeCounter += 1
    }
  }

  let hash = initialHash.slice(0)


  ascii += '\x80'
  while ((ascii.length % 64) - 56) ascii += '\x00'
  for (let i = 0; i < ascii.length; i += 1) {
    const j = ascii.charCodeAt(i)
    words[i >> 2] |= j << (((3 - i) % 4) * 8)
  }
  words[words.length] = (asciiBitLength / maxWord) | 0
  words[words.length] = asciiBitLength

  for (let j = 0; j < words.length; ) {
    const w = words.slice(j, (j += 16))
    const oldHash = hash.slice(0)

    for (let i = 0; i < 64; i += 1) {
      const w15 = w[i - 15]
      const w2 = w[i - 2]

      const a = hash[0]
      const e = hash[4]
      const temp1 =
        hash[7] +
        (rightRotate(e, 6) ^ rightRotate(e, 11) ^ rightRotate(e, 25)) +
        ((e & hash[5]) ^ (~e & hash[6])) +
        k[i] +
        (w[i] =
          i < 16
            ? w[i]
            : (w[i - 16] +
                (rightRotate(w15, 7) ^ rightRotate(w15, 18) ^ (w15 >>> 3)) +
                w[i - 7] +
                (rightRotate(w2, 17) ^ rightRotate(w2, 19) ^ (w2 >>> 10))) |
              0)

      const temp2 =
        (rightRotate(a, 2) ^ rightRotate(a, 13) ^ rightRotate(a, 22)) +
        ((a & hash[1]) ^ (a & hash[2]) ^ (hash[1] & hash[2]))

      hash = [(temp1 + temp2) | 0].concat(hash)
      hash[4] = (hash[4] + temp1) | 0
      hash.pop()
    }

    for (let i = 0; i < 8; i += 1) {
      hash[i] = (hash[i] + oldHash[i]) | 0
    }
  }

  for (let i = 0; i < 8; i += 1) {
    for (let j = 3; j + 1; j -= 1) {
      const b = (hash[i] >> (j * 8)) & 255
      result += (b < 16 ? '0' : '') + b.toString(16)
    }
  }
  return result
}

function signPayload(method, path, timestamp, nonce, secret) {
  const utf8Payload = unescape(encodeURIComponent(`${method}\n${path}\n${timestamp}\n${nonce}\n${secret}`))
  return sha256(utf8Payload)
}

function resolveAssetUrl(url) {
  const value = (url || '').trim()
  if (!value) return ''
  if (/^https?:\/\//i.test(value)) return value
  if (value.indexOf('//') === 0) return `https:${value}`
  const origin = getApiOrigin()
  if (value.indexOf('/') === 0) return `${origin}${value}`
  return `${origin}/${value}`
}



function normalizeWechatUserInfo(userInfo = null) {
  if (!userInfo || typeof userInfo !== 'object') return undefined
  return {
    nickName: userInfo.nickName || '',
    avatarUrl: userInfo.avatarUrl || '',
    gender: Number(userInfo.gender || 0),
    city: userInfo.city || '',
    province: userInfo.province || '',
    country: userInfo.country || '',
    language: userInfo.language || ''
  }
}

function hasAuthorizedWechatProfile(userInfo = null) {
  if (!userInfo || typeof userInfo !== 'object') return false
  const nickName = String(userInfo.nickName || '').trim()
  const avatarUrl = String(userInfo.avatarUrl || '').trim()
  return !!(nickName || avatarUrl)
}


async function runWechatLogin(userInfo = null) {
  const loginRes = await new Promise((resolve, reject) => {
    wx.login({
      success: resolve,
      fail: () => reject(new Error('微信登录调用失败'))
    })
  })

  const code = (loginRes && loginRes.code) || ''
  if (!code) {
    throw new Error('微信登录失败：未获取到 code')
  }

  const loginResp = await wxRequest({
    url: `${getBaseUrl()}/auth/login`,

    method: 'POST',
    header: {
      'Content-Type': 'application/json',
      'X-Platform': 'miniprogram',
      'X-Version': '1.0.0'
    },
    data: {
      code,
      userInfo: normalizeWechatUserInfo(userInfo)
    }
  })


  const body = loginResp.data || {}
  if (
    loginResp.statusCode < 200 ||
    loginResp.statusCode >= 300 ||
    body.code !== 0 ||
    !body.data ||
    !body.data.token
  ) {
    throw new Error((body && body.message) || '登录接口异常')
  }

  wx.setStorageSync(TOKEN_KEY, body.data.token)
  if (body.data.userId !== undefined && body.data.userId !== null) {
    wx.setStorageSync(USER_ID_KEY, body.data.userId)
  }
  setAuthMode('wechat')
  return body.data.token
}

async function ensureToken(forceRefresh = false, userInfo = null) {
  const cached = wx.getStorageSync(TOKEN_KEY)
  if (cached && !forceRefresh) return cached

  if (!forceRefresh) {
    throw new Error('请先登录')
  }

  if (loginPromise) return loginPromise


  loginPromise = (async () => {
    try {
      return await runWechatLogin(userInfo)
    } catch (e) {
      clearAuthCache()
      throw e
    }
  })()


  try {
    return await loginPromise
  } finally {
    loginPromise = null
  }
}

async function initAuth(options = {}) {
  const { silentGuest = true, forceRefresh = false } = options
  try {
    await ensureToken(forceRefresh)
    return { loggedIn: true, mode: 'wechat' }
  } catch (e) {
    clearAuthCache()
    if (!silentGuest) throw e
    return { loggedIn: false, mode: 'guest', message: e.message || '已进入访客模式' }
  }
}

async function forceWechatLogin(userInfo = null) {
  if (!hasAuthorizedWechatProfile(userInfo)) {
    throw new Error('请先完成微信授权')
  }
  clearAuthCache()
  return ensureToken(true, userInfo)
}

/**
 * 静默登录：使用微信 code 登录，不需要用户信息
 * 适用于已授权过的老用户
 */
async function silentLogin() {
  const loginRes = await new Promise((resolve, reject) => {
    wx.login({
      success: resolve,
      fail: () => reject(new Error('微信登录调用失败'))
    })
  })

  const code = (loginRes && loginRes.code) || ''
  if (!code) {
    throw new Error('微信登录失败：未获取到 code')
  }

  // 静默登录，不传 userInfo
  const loginResp = await wxRequest({
    url: `${getBaseUrl()}/auth/login`,
    method: 'POST',
    header: {
      'Content-Type': 'application/json',
      'X-Platform': 'miniprogram',
      'X-Version': '1.0.0'
    },
    data: {
      code,
      userInfo: null
    }
  })

  const body = loginResp.data || {}
  if (
    loginResp.statusCode < 200 ||
    loginResp.statusCode >= 300 ||
    body.code !== 0 ||
    !body.data ||
    !body.data.token
  ) {
    throw new Error((body && body.message) || '登录接口异常')
  }

  wx.setStorageSync(TOKEN_KEY, body.data.token)
  if (body.data.userId !== undefined && body.data.userId !== null) {
    wx.setStorageSync(USER_ID_KEY, body.data.userId)
  }
  setAuthMode('wechat')
  return body.data.token
}


async function request(options = {}) {
  const {
    url,
    method = 'GET',
    data = {},
    withAuth = true,
    retryOnAuthFail = true,
    requiresSignature = false,
    idempotent = false
  } = options

  const upperMethod = method.toUpperCase()
  const token = withAuth ? await ensureToken() : ''
  const query = upperMethod === 'GET' ? toQuery(data) : ''
  const fullUrl = `${getBaseUrl()}${url}${query ? `?${query}` : ''}`


  const headers = {
    'Content-Type': 'application/json',
    'X-Platform': 'miniprogram',
    'X-Version': '1.0.0',
    ...(token ? { Authorization: `Bearer ${token}` } : {})
  }

  if (requiresSignature) {
    const timestamp = String(Math.floor(Date.now() / 1000))
    const nonce = randomString(24)
    const requestPath = `${REQUEST_PATH_PREFIX}${url}`
    headers['X-Timestamp'] = timestamp
    headers['X-Nonce'] = nonce
    headers['X-Signature'] = signPayload(upperMethod, requestPath, timestamp, nonce, SIGN_SECRET)
  }

  if (idempotent) {
    headers['X-Idempotency-Key'] = randomString(32)
  }

  const response = await wxRequest({
    url: fullUrl,
    method: upperMethod,
    header: headers,
    data: upperMethod === 'GET' ? undefined : data
  })

  const body = response.data || {}
  if (response.statusCode < 200 || response.statusCode >= 300) {
    throw new Error(body.message || `网络请求失败(${response.statusCode})`)
  }

  if (body.code !== 0) {
    if (body.code === 10002 && retryOnAuthFail && withAuth) {
      clearAuthCache()
      return request({ ...options, retryOnAuthFail: false })
    }
    throw new Error(body.message || '请求失败')
  }

  return body.data
}

module.exports = {
  request,
  clearAuthCache,
  resolveAssetUrl,
  BASE_URL,
  getBaseUrl,
  initAuth,
  forceWechatLogin,
  silentLogin,
  getAuthMode,
  hasLocalToken
}


