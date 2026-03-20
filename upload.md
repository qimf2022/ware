# 上传说明

## 本次完成内容（2026-03-19）

### 1. 分类页面 UI 按钮统一
- 统一排序按钮为胶囊按钮样式（默认/选中态一致化）。
- 统一筛选按钮样式（高度、圆角、字号、边框）。
- 统一筛选面板 `chip` 与底部 `重置/确定` 按钮视觉规范。

涉及文件：
- `miniprogram/pages/category/index.wxss`

### 2. 购物车右滑删除功能
- 购物车商品项新增 `movable-area + movable-view` 结构。
- 左滑可露出删除按钮，支持单项删除确认。
- 删除操作调用后端接口：`DELETE /api/v1/cart`（`deleteCartItems`）。
- 增加滑动态复位逻辑（点击空白关闭、切换项自动收起）。

涉及文件：
- `miniprogram/pages/cart/index.wxml`
- `miniprogram/pages/cart/index.wxss`
- `miniprogram/pages/cart/index.js`

### 3. 页面右滑返回上一层体验
- 保持各页面 `navigateTo + navigateBack` 栈式跳转。
- 将售后申请提交后的跳转由 `redirectTo` 调整为 `navigateTo`，保留右滑返回路径。

涉及文件：
- `miniprogram/pages/after-sale/apply/index.js`

### 4. 前端交互接口缺失补齐
- 补齐搜索建议接口：`getSearchSuggest(keyword)` → `GET /api/v1/search/suggest`。
- 补齐订单操作接口：
  - `cancelOrder(orderId, reason)` → `POST /api/v1/orders/{id}/cancel`
  - `receiveOrder(orderId)` → `POST /api/v1/orders/{id}/receive`

涉及文件：
- `miniprogram/utils/catalog-api.js`
- `miniprogram/utils/trade-api.js`

### 5. 我的/购物车/分类页面后端数据接入确认
- 我的页：`getProfile()`（`/user/profile`）
- 购物车页：`getCart/updateCartItem/selectAllCart/deleteCartItems/getCartRecommend`
- 分类页：`getCategories/getProducts`
- 分类页加购动作从本地 Toast 改为真实后端加购：`quickAddProduct`。

涉及文件：
- `miniprogram/pages/mine/index.js`
- `miniprogram/pages/cart/index.js`
- `miniprogram/pages/category/index.js`

---

## 接口联调结果
- 核心交互（分类加购、购物车删除、数量修改、全选）已走后端接口。
- 当前检查未发现新增语法/lint 错误（CommonJS 提示为项目现状提示）。
