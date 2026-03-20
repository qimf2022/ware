# 更新记录

## 1.0.4（2026-03-18）

### 一、我的页 UI 像素级同步
- **下拉弹性**：添加 `scroll-view` 支持弹性下拉，空白区域松手后自动恢复。
- **顶部背景**：深紫色渐变（#6b0d6e → #8b1a8f），大弧度圆角（200rpx）。
- **顶部操作栏**：三个图标按钮（会员码、地址、设置），半透明胶囊背景。
- **用户信息**：头像加大（120rpx）带白色边框，昵称大字体（40rpx），点击跳转设置页。
- **品牌标题**：保持"—— 诗语家居 ——"。
- **统计区**：收藏/足迹带图标，数字大字体（48rpx）。
- **我的订单**：5个订单状态图标，横向均分布局。
- **资产卡**：积分/优惠券/卡券，数字+图标横向排列。
- **服务入口**：4个彩色圆形图标（充值/领券/分销/绑定手机）。
- **底部文案**：保持"技术支持：诗语家纺"。

**改动文件**
- `miniprogram/pages/mine/index.js` - 更新图标配置，新增 `onTapQrcode` 方法
- `miniprogram/pages/mine/index.wxml` - 添加 `scroll-view` 弹性滚动容器，整体结构调整
- `miniprogram/pages/mine/index.wxss` - 全面重写样式，支持弹性滚动

### 二、我的页顶部背景优化
- **渐变调整**：从纯紫色改为与首页 banner 一致的紫红到红色渐变（`#6B2D5C → #D81E06`）。
- **装饰元素**：添加半透明圆形装饰和点缀小圆点，增加视觉层次感。
- **头像优化**：增加阴影效果，边框透明度提升。
- **操作栏优化**：使用毛玻璃效果（backdrop-filter），更加现代精致。

**改动文件**
- `miniprogram/pages/mine/index.wxml` - 添加装饰元素节点
- `miniprogram/pages/mine/index.wxss` - 更新背景渐变、添加装饰样式、优化头像和操作栏

### 三、我的页弧度与头像优化
- **背景弧度调整**：改为更平缓的曲线效果，使用独立伪元素实现底部弧线。
- **头像圆环优化**：白色圆环更明显，使用双层结构（外层透明底 + 内层白边）。
- **内边距调整**：优化 hero 区域 padding，与曲线过渡更自然。

**改动文件**
- `miniprogram/pages/mine/index.wxml` - 添加 `hero-curve` 弧度元素
- `miniprogram/pages/mine/index.wxss` - 调整头像圆环样式、添加底部弧度

### 二、新增售后列表页
- 新增售后列表页面，展示用户所有售后申请记录。
- 页面包含：售后单号、售后类型、处理状态、申请时间。
- 空态展示"暂无相关订单"。
- 点击售后列表项可跳转售后详情页。

**新增文件**
- `miniprogram/pages/aftersale/list/index.js`
- `miniprogram/pages/aftersale/list/index.json`
- `miniprogram/pages/aftersale/list/index.wxml`
- `miniprogram/pages/aftersale/list/index.wxss`

**改动文件**
- `miniprogram/pages/mine/index.js` - 售后入口跳转逻辑
- `miniprogram/pages/mine/index.wxml` - 绑定 type 参数
- `miniprogram/app.json` - 注册新页面
- `miniprogram/utils/trade-api.js` - 新增 `listAfterSales` 接口封装

### 二、后端接口
- 复用现有接口：`GET /api/v1/after-sales`
- 后端已支持，无需新增接口。

---

## 1.0.3（2026-03-18）

### 一、我的页 UI 再次对齐（参考 `我的.png`）
- 重做“我的”页头部视觉：紫色弧形背景、用户信息区、右上角设置入口。
- 重排“我的收藏 / 我的足迹”统计卡、“我的订单”快捷入口、资产卡（积分/优惠券/卡券）与服务入口。
- 保留并串通原有页面跳转：订单列表、收藏、足迹、领券、绑定手机号等。

**改动文件**
- `miniprogram/pages/mine/index.js`
- `miniprogram/pages/mine/index.wxml`
- `miniprogram/pages/mine/index.wxss`

### 二、个人资料页样式收口（参考 `我的_设置.png`）
- 调整 `个人资料` 页排版与间距，统一行高、占位态、箭头与头像展示风格。
- 保留原有能力：昵称/姓名/性别/生日、我的地址、绑定手机号、退出登录。

**改动文件**
- `miniprogram/pages/user/profile/index.wxml`
- `miniprogram/pages/user/profile/index.wxss`

### 三、地址页与下单选址流程重做（参考 `我的_地址.png` + `选择地址.png`）
- 地址列表页重构为：地址类型 Tab + 搜索框 + 空态 + 底部双按钮（手动添加/自动获取）。
- 新增地址自动获取：调用 `wx.chooseAddress` 后通过地址接口落库。
- 结算地址选择继续沿用 `mode=select` 回填逻辑，并在列表中显示选中态。
- 订单确认页重做配送方式 Tabs 与地址卡样式，贴近参考图风格。

**改动文件**
- `miniprogram/pages/address/list/index.js`
- `miniprogram/pages/address/list/index.wxml`
- `miniprogram/pages/address/list/index.wxss`
- `miniprogram/pages/order/confirm/index.js`
- `miniprogram/pages/order/confirm/index.wxml`
- `miniprogram/pages/order/confirm/index.wxss`

### 四、接口核对与后端结论
- 本次仅前端 UI/交互重构，后端主链路保持不变。
- 已复用现有接口：
  - `GET /api/v1/user/profile`
  - `PUT /api/v1/user/profile`
  - `GET /api/v1/addresses`
  - `POST /api/v1/addresses`
  - `PUT /api/v1/addresses/{id}/default`
  - `DELETE /api/v1/addresses/{id}`
  - `POST /api/v1/orders/confirm`
  - `POST /api/v1/orders`
- 未发现必须新增后端接口。

---

## 1.0.2（2026-03-18）


### 一、购物车页按参考图（`购物车.png`）完善
- 重做购物车结构：库存提示 + 编辑态入口、店铺名行、商品卡信息区、圆形数量加减器。
- 底部结算栏重排：全选、总计、主操作按钮（编辑态显示“删除”，普通态显示“去结算”）。
- 修复“立即下单/去结算按钮不可见”：结算栏固定在 `tabBar` 之上并提升层级。
- 新增“猜你喜欢”双列商品卡，补齐加购按钮与点击跳转。

**改动文件**
- `miniprogram/pages/cart/index.js`
- `miniprogram/pages/cart/index.wxml`
- `miniprogram/pages/cart/index.wxss`

### 二、前后端购物车接口联通与字段补齐
- 前端 `trade-api` 增补并启用购物车扩展接口：
  - `PUT /cart/select-all`
  - `GET /cart/recommend`
  - `PUT /cart/batch`（已封装，当前页面暂未使用）
- 购物车项字段补齐并映射到前端：`shop_name/brand_name/original_price`。

**改动文件**
- `miniprogram/utils/trade-api.js`

### 三、后端核对结论（按说明文档）
- 已确认后端已具备并可直接支撑当前购物车页面：
  - `GET /api/v1/cart`
  - `PUT /api/v1/cart/{id}`
  - `DELETE /api/v1/cart`
  - `PUT /api/v1/cart/select-all`
  - `PUT /api/v1/cart/batch`
  - `GET /api/v1/cart/recommend`
- 本次联调未发现必须新增的后端接口；当前为前端同步与样式/交互收口为主。

---

## 1.0.1（2026-03-17）


### 一、我的页 UI 重构（对齐 `我的.png`）
- 重做顶部紫色品牌头图与用户信息区，右上角增加设置入口。
- 增加“我的收藏 / 我的足迹”双统计区。
- 重做“我的订单”模块（待付款、待发货、待收货、已完成、售后）。
- 重做“积分 / 优惠券 / 卡券”资产模块与服务入口模块。
- 页面底部新增技术支持文案。

**改动文件**
- `miniprogram/pages/mine/index.js`
- `miniprogram/pages/mine/index.wxml`
- `miniprogram/pages/mine/index.wxss`

### 二、新增设置（个人资料）页（参考 `我的_设置.png`）
- 新增 `个人资料` 页面，包含：ID、头像、昵称、姓名、性别、出生日期、我的地址、绑定手机号、退出登录。
- 资料能力联通：
  - 昵称/性别/头像：调用 `PUT /api/v1/user/profile`
  - 我的地址：跳转地址列表页
  - 绑定手机号：跳转绑定手机号页
  - 退出登录：清本地 token 并回到我的页
- 因后端当前未提供姓名/生日字段，前端先用本地缓存保存（不影响主流程）。

**新增文件**
- `miniprogram/pages/user/profile/index.js`
- `miniprogram/pages/user/profile/index.wxml`
- `miniprogram/pages/user/profile/index.wxss`
- `miniprogram/pages/user/profile/index.json`

**改动文件**
- `miniprogram/app.json`（注册新页面）
- `miniprogram/utils/user-api.js`（补齐 `id/gender/footprintCount/orderCount/cardBalance` 字段映射）

### 三、地址页 UI 重构（对齐 `我的_地址.png` + 选择地址参考）
- 重做地址页为：顶部地址类型 Tab + 搜索框 + 空态 + 底部双按钮。
- 新增“自动获取”能力：调用小程序 `wx.chooseAddress`，自动落库到地址接口。
- 在 `mode=select` 下优化为下单选址模式，保留选择回填逻辑。

**改动文件**
- `miniprogram/pages/address/list/index.js`
- `miniprogram/pages/address/list/index.wxml`
- `miniprogram/pages/address/list/index.wxss`

### 四、下单页选择地址区域重做（对齐 `选择地址_1773742381866.png`）
- 重做订单确认页顶部为配送方式 Tabs（快递配送/到店自提/同城配送）。
- 地址区改为“请添加配送地址”大卡样式，已选地址展示姓名电话与完整地址。
- 商品区、费用区、底部提交栏按参考风格重排。

**改动文件**
- `miniprogram/pages/order/confirm/index.js`
- `miniprogram/pages/order/confirm/index.wxml`
- `miniprogram/pages/order/confirm/index.wxss`

### 五、前后端联通补充
- 我的页“订单状态快捷入口”支持按状态跳转订单列表。
- 订单状态筛选能力增强：
  - 新增支持：`to_pay`、`to_ship`、`to_receive`、`completed`、`after_sale`、`all`
  - 兼容旧参数：`pending`、`shipped`、`received`、`cancelled`

**改动文件**
- `miniprogram/pages/order/list/index.js`
- `shiYu/src/main/java/com/shiyu/backend/service/MockTradeService.java`

### 六、如需进一步增加后端接口（可选）
当前主链路已可用。若要让“个人资料”完全服务端持久化，建议后端追加：
1. 用户资料字段：`real_name`、`birthday`
2. 用户资料接口返回/更新上述字段

---

## 1.0.0（2026-03-17）
- 购物车页面按参考图完成重构：编辑态、全选/结算条、数量加减器、猜你喜欢模块。
- 已串通购物车相关前后端能力：
  - `GET /cart/recommend`
  - `PUT /cart/select-all`
  - `PUT /cart/batch`
  - 购物车项字段补齐：店铺名/品牌名、原价/现价。
