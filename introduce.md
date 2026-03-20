# 增加接口记录

## 订单列表状态筛选

前端订单列表页已实现 Tab 切换功能，对应后端接口：

```
GET /api/v1/orders?status={status}&page=1&page_size=20
```

### 支持的状态参数

| 状态值 | 说明 | 对应数据库状态 |
|--------|------|----------------|
| (空) / all | 全部订单 | - |
| pending | 待付款 | ORDER_PENDING_PAY |
| to_ship | 待发货 | ORDER_PENDING_SHIP |
| shipped | 待收货 | ORDER_PENDING_RECEIVE |
| completed | 已完成 | ORDER_COMPLETED |

### 我的页订单入口联动

我的页点击以下入口会跳转到订单列表并自动切换到对应 Tab：

1. **查看更多** -> 全部订单（不带 status 参数）
2. **待付款** -> `status=pending`
3. **待发货** -> `status=to_ship`
4. **待收货** -> `status=shipped`
5. **已完成** -> `status=completed`

### 前端改动文件

- `miniprogram/pages/mine/index.js` - 订单状态列表添加 status 字段
- `miniprogram/pages/mine/index.wxml` - 绑定 status 参数
- `miniprogram/pages/order/list/index.js` - Tab 切换与状态筛选
- `miniprogram/pages/order/list/index.wxml` - Tab UI
- `miniprogram/pages/order/list/index.wxss` - Tab 样式

### 后端接口

后端 `/api/v1/orders` 接口已支持 status 参数，无需新增接口。
