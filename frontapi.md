# 诗语家居小程序 - 前端接口文档

## 1. 接口基础规范

### 1.1 协议与域名
- **协议**：HTTPS
- **接口版本**：/api/v1
- **完整地址**：https://api.shiyuhome.com/api/v1

### 1.2 请求方式
- **GET**：查询操作
- **POST**：创建操作
- **PUT**：更新操作
- **DELETE**：删除操作

### 1.3 请求头
```
Content-Type: application/json
Authorization: Bearer {token}
X-OpenID: {openid}
X-Platform: miniprogram
X-Version: 1.0.0
```

### 1.4 响应格式

#### 成功响应
```json
{
  "code": 0,
  "message": "success",
  "data": {
    // 业务数据
  }
}
```

#### 失败响应
```json
{
  "code": 10001,
  "message": "错误描述",
  "errors": [
    {
      "field": "mobile",
      "message": "手机号格式不正确"
    }
  ]
}
```

### 1.5 分页格式
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "list": [],
    "total": 100,
    "page": 1,
    "page_size": 20,
    "has_more": true
  }
}
```

### 1.6 错误码定义

| 错误码 | 说明 |
| --- | --- |
| 0 | 成功 |
| 10001 | 参数错误 |
| 10002 | 认证失败 |
| 10003 | 权限不足 |
| 10004 | 资源不存在 |
| 10005 | 业务逻辑错误 |
| 20001 | 商品不存在 |
| 20002 | 商品已下架 |
| 20003 | 库存不足 |
| 30001 | 订单不存在 |
| 30002 | 订单状态错误 |
| 30003 | 支付失败 |
| 40001 | 优惠券已领完 |
| 40002 | 优惠券已过期 |
| 40003 | 优惠券不满足使用条件 |

---

## 2. 首页模块

### 2.1 获取首页配置
**接口**：`GET /home/config`

**描述**：获取首页轮播图、分类金刚区、推荐商品等全部配置

**请求参数**：无

**响应示例**：
```json
{
  "code": 0,
  "data": {
    "banners": [
      {
        "id": 1,
        "image_url": "https://cdn.shiyuhome.com/banner1.jpg",
        "title": "限时特惠",
        "link_type": "topic",
        "link_value": "wedding_2024"
      }
    ],
    "categories": [
      {
        "id": 1,
        "name": "床盖",
        "code": "chuanggai",
        "icon_url": "https://cdn.shiyuhome.com/cat1.png"
      }
    ],
    "recommend_products": [
      {
        "id": 100,
        "title": "法式轻奢四件套",
        "main_image": "https://cdn.shiyuhome.com/prod1.jpg",
        "min_price": 599.00,
        "max_price": 1299.00,
        "sales_count": 1234,
        "stock_status": 1
      }
    ]
  }
}
```

### 2.2 获取推荐商品
**接口**：`GET /home/recommend`

**描述**：获取首页推荐商品流,支持分页

**请求参数**：
| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| position_code | string | N | 推荐位编码,如home_hot,默认首页推荐 |
| page | int | N | 页码,默认1 |
| page_size | int | N | 每页数量,默认20 |

**响应示例**：
```json
{
  "code": 0,
  "data": {
    "list": [
      {
        "id": 100,
        "title": "法式轻奢四件套",
        "subtitle": "100支全棉贡缎",
        "main_image": "https://cdn.shiyuhome.com/prod1.jpg",
        "min_price": 599.00,
        "max_price": 1299.00,
        "original_min_price": 899.00,
        "sales_count": 1234,
        "stock_status": 1,
        "is_new": 0,
        "is_hot": 1
      }
    ],
    "total": 50,
    "page": 1,
    "page_size": 20,
    "has_more": true
  }
}
```

---

## 3. 分类与搜索模块

### 3.1 获取分类列表
**接口**：`GET /categories`

**描述**：获取全部分类列表(树形结构)

**请求参数**：
| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| parent_id | int | N | 父分类ID,不传返回全部一级分类 |

**响应示例**：
```json
{
  "code": 0,
  "data": {
    "list": [
      {
        "id": 1,
        "name": "床盖",
        "code": "chuanggai",
        "icon_url": "https://cdn.shiyuhome.com/cat1.png",
        "sort_order": 1,
        "children": [
          {
            "id": 11,
            "name": "纯色床盖",
            "code": "chuanggai_solid"
          }
        ]
      }
    ]
  }
}
```

### 3.2 获取分类商品列表
**接口**：`GET /products`

**描述**：获取商品列表,支持筛选、排序、分页

**请求参数**：
| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| category_id | int | N | 分类ID |
| keyword | string | N | 搜索关键词 |
| filters | string | N | 筛选条件JSON,如`{"size":"1.8m","material":"全棉"}` |
| sort | string | N | 排序字段:comprehensive/price_asc/price_desc/sales/new,默认comprehensive |
| page | int | N | 页码,默认1 |
| page_size | int | N | 每页数量,默认20 |

**响应示例**：
```json
{
  "code": 0,
  "data": {
    "list": [
      {
        "id": 100,
        "title": "法式轻奢四件套",
        "subtitle": "100支全棉贡缎",
        "main_image": "https://cdn.shiyuhome.com/prod1.jpg",
        "min_price": 599.00,
        "max_price": 1299.00,
        "original_min_price": 899.00,
        "sales_count": 1234,
        "stock_status": 1,
        "category": {
          "id": 1,
          "name": "床盖"
        }
      }
    ],
    "total": 100,
    "page": 1,
    "page_size": 20,
    "has_more": true,
    "filters": [
      {
        "attr_code": "size",
        "attr_name": "尺寸",
        "input_type": 1,
        "values": ["1.5m", "1.8m", "2.0m"]
      }
    ]
  }
}
```

### 3.3 获取热搜词
**接口**：`GET /search/hot`

**描述**：获取热门搜索词列表

**请求参数**：无

**响应示例**：
```json
{
  "code": 0,
  "data": {
    "list": [
      {
        "id": 1,
        "keyword": "四件套",
        "sort_order": 1
      },
      {
        "id": 2,
        "keyword": "婚嫁床品",
        "sort_order": 2
      }
    ]
  }
}
```

### 3.4 获取搜索建议
**接口**：`GET /search/suggest`

**描述**：根据输入关键词获取联想词

**请求参数**：
| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| keyword | string | Y | 输入关键词 |
| limit | int | N | 返回数量,默认10 |

**响应示例**：
```json
{
  "code": 0,
  "data": {
    "list": [
      {
        "id": 1,
        "keyword": "四件套",
        "suggest_word": "四件套全棉",
        "target_type": "product",
        "target_id": 100
      }
    ]
  }
}
```

### 3.5 获取搜索历史
**接口**：`GET /search/history`

**描述**：获取用户搜索历史

**请求参数**：
| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| limit | int | N | 返回数量,默认10 |

**响应示例**：
```json
{
  "code": 0,
  "data": {
    "list": [
      {
        "id": 1,
        "keyword": "四件套",
        "search_count": 5,
        "last_search_at": "2024-01-15 10:30:00"
      }
    ]
  }
}
```

### 3.6 清空搜索历史
**接口**：`DELETE /search/history`

**描述**：清空用户搜索历史

**请求参数**：无

**响应示例**：
```json
{
  "code": 0,
  "message": "清空成功"
}
```

---

## 4. 商品详情模块

### 4.1 获取商品详情
**接口**：`GET /products/{id}`

**描述**：获取商品详情信息

**请求参数**：
| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| id | int | Y | 商品ID |

**响应示例**：
```json
{
  "code": 0,
  "data": {
    "id": 100,
    "product_no": "PROD2024001",
    "title": "法式轻奢四件套",
    "subtitle": "100支全棉贡缎",
    "main_image": "https://cdn.shiyuhome.com/prod1.jpg",
    "min_price": 599.00,
    "max_price": 1299.00,
    "original_min_price": 899.00,
    "sales_count": 1234,
    "favorite_count": 567,
    "stock_status": 1,
    "is_new": 0,
    "is_hot": 1,
    "category": {
      "id": 1,
      "name": "床盖"
    },
    "media": [
      {
        "id": 1,
        "media_type": 1,
        "media_url": "https://cdn.shiyuhome.com/prod1.jpg",
        "sort_order": 1
      }
    ],
    "detail_attrs": [
      {
        "group_name": "产品参数",
        "items": [
          {
            "attr_name": "材质",
            "attr_value": "100支全棉贡缎"
          },
          {
            "attr_name": "工艺",
            "attr_value": "活性印染"
          }
        ]
      },
      {
        "group_name": "洗护说明",
        "items": [
          {
            "attr_name": "洗涤方式",
            "attr_value": "可机洗"
          }
        ]
      }
    ],
    "is_favorited": false
  }
}
```

### 4.2 获取商品规格
**接口**：`GET /products/{id}/specs`

**描述**：获取商品规格组和SKU列表

**请求参数**：
| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| id | int | Y | 商品ID |

**响应示例**：
```json
{
  "code": 0,
  "data": {
    "spec_groups": [
      {
        "id": 1,
        "name": "尺寸",
        "sort_order": 1,
        "values": [
          {
            "id": 101,
            "value": "1.5m",
            "image_url": null,
            "sort_order": 1
          },
          {
            "id": 102,
            "value": "1.8m",
            "image_url": null,
            "sort_order": 2
          }
        ]
      },
      {
        "id": 2,
        "name": "颜色",
        "sort_order": 2,
        "values": [
          {
            "id": 201,
            "value": "奶白",
            "image_url": "https://cdn.shiyuhome.com/color1.jpg",
            "sort_order": 1
          },
          {
            "id": 202,
            "value": "浅灰",
            "image_url": "https://cdn.shiyuhome.com/color2.jpg",
            "sort_order": 2
          }
        ]
      }
    ],
    "skus": [
      {
        "id": 1001,
        "sku_code": "SKU1001",
        "specs_json": {
          "size": "1.5m",
          "color": "奶白"
        },
        "price": 599.00,
        "original_price": 899.00,
        "stock": 100,
        "image_url": "https://cdn.shiyuhome.com/sku1.jpg",
        "status": 1
      },
      {
        "id": 1002,
        "sku_code": "SKU1002",
        "specs_json": {
          "size": "1.8m",
          "color": "奶白"
        },
        "price": 799.00,
        "original_price": 1099.00,
        "stock": 50,
        "image_url": "https://cdn.shiyuhome.com/sku2.jpg",
        "status": 1
      }
    ]
  }
}
```

### 4.3 获取搭配推荐
**接口**：`GET /products/{id}/recommend`

**描述**：获取商品搭配推荐

**请求参数**：
| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| id | int | Y | 商品ID |
| relation_type | string | N | 关联类型:bundle/same_series/repurchase,默认全部 |
| limit | int | N | 返回数量,默认10 |

**响应示例**：
```json
{
  "code": 0,
  "data": {
    "list": [
      {
        "id": 101,
        "title": "配套枕套",
        "main_image": "https://cdn.shiyuhome.com/prod2.jpg",
        "min_price": 99.00,
        "max_price": 199.00,
        "relation_type": "bundle",
        "recommend_reason": "搭配购买更优惠"
      }
    ]
  }
}
```

---

## 5. 购物车模块

### 5.1 获取购物车
**接口**：`GET /cart`

**描述**：获取购物车商品列表

**请求参数**：无

**响应示例**：
```json
{
  "code": 0,
  "data": {
    "items": [
      {
        "id": 1,
        "product": {
          "id": 100,
          "title": "法式轻奢四件套",
          "main_image": "https://cdn.shiyuhome.com/prod1.jpg",
          "status": 1
        },
        "sku": {
          "id": 1001,
          "specs_json": {
            "size": "1.8m",
            "color": "奶白"
          },
          "price": 799.00,
          "stock": 50,
          "status": 1
        },
        "quantity": 2,
        "is_selected": 1,
        "invalid_reason": null,
        "subtotal": 1598.00
      }
    ],
    "invalid_items": [
      {
        "id": 2,
        "product": {
          "id": 102,
          "title": "已下架商品",
          "main_image": "https://cdn.shiyuhome.com/prod3.jpg",
          "status": 0
        },
        "sku": {
          "id": 1003,
          "specs_json": {},
          "price": 0,
          "stock": 0,
          "status": 0
        },
        "quantity": 1,
        "is_selected": 0,
        "invalid_reason": "商品已下架",
        "subtotal": 0
      }
    ],
    "total_count": 3,
    "selected_count": 2,
    "total_amount": 1598.00,
    "selected_amount": 1598.00
  }
}
```

### 5.2 添加购物车
**接口**：`POST /cart`

**描述**：添加商品到购物车

**请求参数**：
| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| product_id | int | Y | 商品ID |
| sku_id | int | Y | SKU ID |
| quantity | int | Y | 数量,默认1 |

**请求示例**：
```json
{
  "product_id": 100,
  "sku_id": 1001,
  "quantity": 1
}
```

**响应示例**：
```json
{
  "code": 0,
  "message": "添加成功",
  "data": {
    "cart_id": 1,
    "total_count": 3
  }
}
```

### 5.3 更新购物车商品
**接口**：`PUT /cart/{id}`

**描述**：更新购物车商品数量或选中状态

**请求参数**：
| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| id | int | Y | 购物车项ID |
| quantity | int | N | 数量 |
| is_selected | int | N | 是否选中:0/1 |

**请求示例**：
```json
{
  "quantity": 3,
  "is_selected": 1
}
```

**响应示例**：
```json
{
  "code": 0,
  "message": "更新成功"
}
```

### 5.4 删除购物车商品
**接口**：`DELETE /cart`

**描述**：批量删除购物车商品

**请求参数**：
| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| ids | array | Y | 购物车项ID数组 |

**请求示例**：
```json
{
  "ids": [1, 2, 3]
}
```

**响应示例**：
```json
{
  "code": 0,
  "message": "删除成功"
}
```

### 5.5 检查购物车库存
**接口**：`POST /cart/check`

**描述**：结算前检查购物车商品库存和状态

**请求参数**：
| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| cart_ids | array | N | 购物车项ID数组,不传则检查选中商品 |

**请求示例**：
```json
{
  "cart_ids": [1, 2]
}
```

**响应示例**：
```json
{
  "code": 0,
  "data": {
    "can_checkout": true,
    "check_items": [
      {
        "cart_id": 1,
        "sku_id": 1001,
        "quantity": 2,
        "stock": 50,
        "is_valid": true,
        "invalid_reason": null
      }
    ],
    "invalid_count": 0
  }
}
```

---

## 6. 地址管理模块

### 6.1 获取地址列表
**接口**：`GET /addresses`

**描述**：获取用户收货地址列表

**请求参数**：无

**响应示例**：
```json
{
  "code": 0,
  "data": {
    "list": [
      {
        "id": 1,
        "consignee": "张三",
        "phone_mask": "138****8888",
        "province": "江苏省",
        "city": "苏州市",
        "district": "工业园区",
        "street": "",
        "detail_address": "星湖街328号",
        "tag": "家",
        "is_default": 1,
        "longitude": 120.123456,
        "latitude": 31.123456
      }
    ]
  }
}
```

### 6.2 新增地址
**接口**：`POST /addresses`

**描述**：新增收货地址

**请求参数**：
| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| consignee | string | Y | 收件人 |
| phone | string | Y | 手机号 |
| province | string | Y | 省 |
| city | string | Y | 市 |
| district | string | Y | 区 |
| street | string | N | 街道 |
| detail_address | string | Y | 详细地址 |
| postal_code | string | N | 邮编 |
| tag | string | N | 标签:家/公司/学校 |
| longitude | decimal | N | 经度 |
| latitude | decimal | N | 纬度 |
| is_default | int | N | 是否默认:0/1 |

**请求示例**：
```json
{
  "consignee": "张三",
  "phone": "13888888888",
  "province": "江苏省",
  "city": "苏州市",
  "district": "工业园区",
  "detail_address": "星湖街328号",
  "tag": "家",
  "is_default": 1
}
```

**响应示例**：
```json
{
  "code": 0,
  "message": "添加成功",
  "data": {
    "id": 1
  }
}
```

### 6.3 更新地址
**接口**：`PUT /addresses/{id}`

**描述**：更新收货地址

**请求参数**：同新增地址

**响应示例**：
```json
{
  "code": 0,
  "message": "更新成功"
}
```

### 6.4 删除地址
**接口**：`DELETE /addresses/{id}`

**描述**：删除收货地址

**请求参数**：
| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| id | int | Y | 地址ID |

**响应示例**：
```json
{
  "code": 0,
  "message": "删除成功"
}
```

### 6.5 设置默认地址
**接口**：`PUT /addresses/{id}/default`

**描述**：设置默认收货地址

**请求参数**：
| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| id | int | Y | 地址ID |

**响应示例**：
```json
{
  "code": 0,
  "message": "设置成功"
}
```

---

## 7. 订单模块

### 7.1 订单结算确认
**接口**：`POST /orders/confirm`

**描述**：订单结算前确认,计算价格、优惠、运费

**请求参数**：
| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| cart_ids | array | N | 购物车项ID数组 |
| product_id | int | N | 商品ID(立即购买) |
| sku_id | int | N | SKU ID(立即购买) |
| quantity | int | N | 数量(立即购买) |
| address_id | int | Y | 地址ID |
| coupon_id | int | N | 优惠券ID |
| use_points | int | N | 使用积分数量 |
| card_id | int | N | 储值卡ID |

**请求示例**：
```json
{
  "cart_ids": [1, 2],
  "address_id": 1,
  "coupon_id": 10,
  "use_points": 100
}
```

**响应示例**：
```json
{
  "code": 0,
  "data": {
    "address": {
      "id": 1,
      "consignee": "张三",
      "phone_mask": "138****8888",
      "full_address": "江苏省苏州市工业园区星湖街328号"
    },
    "items": [
      {
        "product_id": 100,
        "sku_id": 1001,
        "product_title": "法式轻奢四件套",
        "product_image": "https://cdn.shiyuhome.com/prod1.jpg",
        "sku_specs": "1.8m/奶白",
        "unit_price": 799.00,
        "quantity": 2,
        "subtotal": 1598.00
      }
    ],
    "price": {
      "total_amount": 1598.00,
      "freight_amount": 0,
      "discount_amount": 100.00,
      "coupon_amount": 50.00,
      "points_amount": 10.00,
      "card_amount": 0,
      "pay_amount": 1438.00
    },
    "available_coupons": [
      {
        "id": 10,
        "name": "满100减50",
        "value": 50.00,
        "threshold": 100.00,
        "start_time": "2024-01-01 00:00:00",
        "end_time": "2024-12-31 23:59:59"
      }
    ],
    "user_points": 500,
    "points_to_money_rate": 10
  }
}
```

### 7.2 创建订单
**接口**：`POST /orders`

**描述**：创建订单

**请求参数**：
| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| cart_ids | array | N | 购物车项ID数组 |
| product_id | int | N | 商品ID(立即购买) |
| sku_id | int | N | SKU ID(立即购买) |
| quantity | int | N | 数量(立即购买) |
| address_id | int | Y | 地址ID |
| coupon_id | int | N | 优惠券ID |
| use_points | int | N | 使用积分数量 |
| card_id | int | N | 储值卡ID |
| remark | string | N | 订单备注 |
| source_type | string | Y | 来源:home/cart/topic |

**请求示例**：
```json
{
  "cart_ids": [1, 2],
  "address_id": 1,
  "coupon_id": 10,
  "use_points": 100,
  "remark": "周末送货",
  "source_type": "cart"
}
```

**响应示例**：
```json
{
  "code": 0,
  "message": "下单成功",
  "data": {
    "order_id": 10001,
    "order_no": "ORD20240115123456",
    "pay_amount": 1438.00,
    "expire_time": "2024-01-15 12:44:56"
  }
}
```

### 7.3 订单支付
**接口**：`POST /orders/{id}/pay`

**描述**：发起订单支付,返回微信支付参数

**请求参数**：
| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| id | int | Y | 订单ID |
| pay_channel | int | Y | 支付渠道:1微信支付,2储值卡混合支付 |

**请求示例**：
```json
{
  "pay_channel": 1
}
```

**响应示例**：
```json
{
  "code": 0,
  "data": {
    "order_id": 10001,
    "order_no": "ORD20240115123456",
    "pay_amount": 1438.00,
    "pay_params": {
      "timeStamp": "1234567890",
      "nonceStr": "abcdefg",
      "package": "prepay_id=wx123456",
      "signType": "RSA",
      "paySign": "xxxxxxxx"
    }
  }
}
```

### 7.4 获取订单列表
**接口**：`GET /orders`

**描述**：获取用户订单列表

**请求参数**：
| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| status | string | N | 订单状态:pending/shipped/received/completed/cancelled/after_sale,默认全部 |
| page | int | N | 页码,默认1 |
| page_size | int | N | 每页数量,默认20 |

**响应示例**：
```json
{
  "code": 0,
  "data": {
    "list": [
      {
        "id": 10001,
        "order_no": "ORD20240115123456",
        "status": 20,
        "status_text": "待发货",
        "pay_status": 20,
        "total_amount": 1598.00,
        "pay_amount": 1438.00,
        "items": [
          {
            "product_title": "法式轻奢四件套",
            "product_image": "https://cdn.shiyuhome.com/prod1.jpg",
            "sku_specs": "1.8m/奶白",
            "quantity": 2,
            "unit_price": 799.00
          }
        ],
        "item_count": 2,
        "created_at": "2024-01-15 10:30:00",
        "payment_time": "2024-01-15 10:35:00"
      }
    ],
    "total": 10,
    "page": 1,
    "page_size": 20,
    "has_more": false
  }
}
```

### 7.5 获取订单详情
**接口**：`GET /orders/{id}`

**描述**：获取订单详情

**请求参数**：
| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| id | int | Y | 订单ID |

**响应示例**：
```json
{
  "code": 0,
  "data": {
    "id": 10001,
    "order_no": "ORD20240115123456",
    "status": 20,
    "status_text": "待发货",
    "pay_status": 20,
    "order_type": 1,
    "total_amount": 1598.00,
    "discount_amount": 100.00,
    "coupon_amount": 50.00,
    "points_amount": 10.00,
    "card_amount": 0,
    "freight_amount": 0,
    "pay_amount": 1438.00,
    "consignee_info": {
      "consignee": "张三",
      "phone_mask": "138****8888",
      "full_address": "江苏省苏州市工业园区星湖街328号"
    },
    "items": [
      {
        "id": 1,
        "product_id": 100,
        "sku_id": 1001,
        "product_title": "法式轻奢四件套",
        "product_image": "https://cdn.shiyuhome.com/prod1.jpg",
        "sku_specs": "1.8m/奶白",
        "unit_price": 799.00,
        "original_price": 1099.00,
        "discount_amount": 50.00,
        "quantity": 2,
        "subtotal": 1548.00,
        "refund_status": 0
      }
    ],
    "remark": "周末送货",
    "created_at": "2024-01-15 10:30:00",
    "payment_time": "2024-01-15 10:35:00",
    "delivery_time": null,
    "finish_time": null,
    "cancel_time": null,
    "auto_close_time": "2024-01-15 10:45:00",
    "shipments": [],
    "after_sales": []
  }
}
```

### 7.6 取消订单
**接口**：`POST /orders/{id}/cancel`

**描述**：取消订单(待付款状态)

**请求参数**：
| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| id | int | Y | 订单ID |
| cancel_reason | string | N | 取消原因 |

**请求示例**：
```json
{
  "cancel_reason": "不想买了"
}
```

**响应示例**：
```json
{
  "code": 0,
  "message": "取消成功"
}
```

### 7.7 确认收货
**接口**：`POST /orders/{id}/receive`

**描述**：确认收货

**请求参数**：
| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| id | int | Y | 订单ID |

**响应示例**：
```json
{
  "code": 0,
  "message": "确认成功"
}
```

### 7.8 获取物流信息
**接口**：`GET /orders/{id}/logistics`

**描述**：获取订单物流信息

**请求参数**：
| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| id | int | Y | 订单ID |

**响应示例**：
```json
{
  "code": 0,
  "data": {
    "shipments": [
      {
        "id": 1,
        "shipment_no": "SHIP20240115123456",
        "company_name": "顺丰速运",
        "tracking_no": "SF1234567890",
        "ship_status": 20,
        "shipped_at": "2024-01-15 15:00:00",
        "received_at": null,
        "tracks": [
          {
            "node_time": "2024-01-16 10:00:00",
            "node_status": "transit",
            "node_content": "快件已到达苏州园区营业点",
            "location": "苏州市"
          },
          {
            "node_time": "2024-01-15 18:00:00",
            "node_status": "shipped",
            "node_content": "快件已发货",
            "location": "杭州市"
          }
        ]
      }
    ]
  }
}
```

---

## 8. 售后模块

### 8.1 申请售后
**接口**：`POST /after-sales`

**描述**：申请售后

**请求参数**：
| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| order_id | int | Y | 订单ID |
| order_item_id | int | Y | 订单明细ID |
| type | int | Y | 售后类型:1仅退款,2退货退款,3换货 |
| reason_code | string | Y | 售后原因编码 |
| reason_desc | string | N | 补充说明 |
| evidence_urls | array | N | 凭证图片URL数组 |
| apply_amount | decimal | N | 申请退款金额(仅退款必填) |

**请求示例**：
```json
{
  "order_id": 10001,
  "order_item_id": 1,
  "type": 2,
  "reason_code": "quality",
  "reason_desc": "商品有破损",
  "evidence_urls": [
    "https://cdn.shiyuhome.com/evidence1.jpg"
  ]
}
```

**响应示例**：
```json
{
  "code": 0,
  "message": "申请成功",
  "data": {
    "after_sale_id": 1,
    "after_sale_no": "AS20240115123456"
  }
}
```

### 8.2 获取售后列表
**接口**：`GET /after-sales`

**描述**：获取用户售后单列表

**请求参数**：
| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| status | string | N | 售后状态:pending/returning/refunding/completed/rejected,默认全部 |
| page | int | N | 页码,默认1 |
| page_size | int | N | 每页数量,默认20 |

**响应示例**：
```json
{
  "code": 0,
  "data": {
    "list": [
      {
        "id": 1,
        "after_sale_no": "AS20240115123456",
        "type": 2,
        "type_text": "退货退款",
        "status": 20,
        "status_text": "待退货",
        "order": {
          "order_no": "ORD20240115123456",
          "product_title": "法式轻奢四件套",
          "product_image": "https://cdn.shiyuhome.com/prod1.jpg"
        },
        "apply_amount": 799.00,
        "created_at": "2024-01-15 16:00:00"
      }
    ],
    "total": 5,
    "page": 1,
    "page_size": 20,
    "has_more": false
  }
}
```

### 8.3 获取售后详情
**接口**：`GET /after-sales/{id}`

**描述**：获取售后单详情

**请求参数**：
| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| id | int | Y | 售后单ID |

**响应示例**：
```json
{
  "code": 0,
  "data": {
    "id": 1,
    "after_sale_no": "AS20240115123456",
    "type": 2,
    "type_text": "退货退款",
    "status": 20,
    "status_text": "待退货",
    "order": {
      "id": 10001,
      "order_no": "ORD20240115123456",
      "product_title": "法式轻奢四件套",
      "product_image": "https://cdn.shiyuhome.com/prod1.jpg",
      "sku_specs": "1.8m/奶白",
      "quantity": 1,
      "unit_price": 799.00
    },
    "reason_code": "quality",
    "reason_desc": "商品有破损",
    "evidence_urls": [
      "https://cdn.shiyuhome.com/evidence1.jpg"
    ],
    "apply_amount": 799.00,
    "audit_status": 20,
    "audit_remark": "同意退货",
    "return_address": {
      "consignee": "售后部",
      "phone": "0512-12345678",
      "full_address": "江苏省苏州市工业园区星湖街328号"
    },
    "return_shipment": null,
    "created_at": "2024-01-15 16:00:00",
    "logs": [
      {
        "action": "APPLY",
        "remark": "用户申请退货退款",
        "created_at": "2024-01-15 16:00:00"
      },
      {
        "action": "APPROVE",
        "remark": "审核通过",
        "created_at": "2024-01-15 16:30:00"
      }
    ]
  }
}
```

### 8.4 填写退货物流
**接口**：`POST /after-sales/{id}/return`

**描述**：填写退货物流信息

**请求参数**：
| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| id | int | Y | 售后单ID |
| company_name | string | Y | 物流公司 |
| tracking_no | string | Y | 运单号 |

**请求示例**：
```json
{
  "company_name": "顺丰速运",
  "tracking_no": "SF9876543210"
}
```

**响应示例**：
```json
{
  "code": 0,
  "message": "提交成功"
}
```

### 8.5 取消售后
**接口**：`POST /after-sales/{id}/cancel`

**描述**：取消售后申请

**请求参数**：
| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| id | int | Y | 售后单ID |

**响应示例**：
```json
{
  "code": 0,
  "message": "取消成功"
}
```

---

## 9. 个人中心模块

### 9.1 获取用户信息
**接口**：`GET /user/profile`

**描述**：获取用户个人信息

**请求参数**：无

**响应示例**：
```json
{
  "code": 0,
  "data": {
    "id": 1,
    "nickname": "诗语访客",
    "avatar_url": "https://cdn.shiyuhome.com/avatar.png",
    "phone_mask": "138****8888",
    "gender": 0,
    "member_level_code": "silver",
    "register_source": "wx_miniapp",
    "last_login_at": "2024-01-15 10:00:00",
    "created_at": "2024-01-01 10:00:00",
    "stats": {
      "order_count": 10,
      "favorite_count": 5,
      "footprint_count": 20,
      "points": 500,
      "coupon_count": 3,
      "card_balance": 0
    }
  }
}
```

### 9.2 更新用户信息
**接口**：`PUT /user/profile`

**描述**：更新用户个人信息

**请求参数**：
| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| nickname | string | N | 昵称 |
| avatar_url | string | N | 头像URL |
| gender | int | N | 性别:0未知,1男,2女 |

**请求示例**：
```json
{
  "nickname": "小王",
  "gender": 1
}
```

**响应示例**：
```json
{
  "code": 0,
  "message": "更新成功"
}
```

### 9.3 获取收藏列表
**接口**：`GET /user/favorites`

**描述**：获取用户收藏商品列表

**请求参数**：
| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| page | int | N | 页码,默认1 |
| page_size | int | N | 每页数量,默认20 |

**响应示例**：
```json
{
  "code": 0,
  "data": {
    "list": [
      {
        "id": 1,
        "product": {
          "id": 100,
          "title": "法式轻奢四件套",
          "main_image": "https://cdn.shiyuhome.com/prod1.jpg",
          "min_price": 599.00,
          "max_price": 1299.00,
          "sales_count": 1234,
          "stock_status": 1
        },
        "created_at": "2024-01-15 10:00:00"
      }
    ],
    "total": 5,
    "page": 1,
    "page_size": 20,
    "has_more": false
  }
}
```

### 9.4 添加/取消收藏
**接口**：`POST /user/favorites`

**描述**：添加或取消收藏

**请求参数**：
| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| product_id | int | Y | 商品ID |
| action | string | Y | 操作:add添加,remove取消 |

**请求示例**：
```json
{
  "product_id": 100,
  "action": "add"
}
```

**响应示例**：
```json
{
  "code": 0,
  "message": "收藏成功",
  "data": {
    "is_favorited": true,
    "favorite_count": 568
  }
}
```

### 9.5 获取浏览足迹
**接口**：`GET /user/footprints`

**描述**：获取用户浏览足迹

**请求参数**：
| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| page | int | N | 页码,默认1 |
| page_size | int | N | 每页数量,默认20 |

**响应示例**：
```json
{
  "code": 0,
  "data": {
    "list": [
      {
        "id": 1,
        "product": {
          "id": 100,
          "title": "法式轻奢四件套",
          "main_image": "https://cdn.shiyuhome.com/prod1.jpg",
          "min_price": 599.00,
          "max_price": 1299.00,
          "sales_count": 1234,
          "stock_status": 1
        },
        "viewed_at": "2024-01-15 10:00:00"
      }
    ],
    "total": 20,
    "page": 1,
    "page_size": 20,
    "has_more": false
  }
}
```

### 9.6 清空浏览足迹
**接口**：`DELETE /user/footprints`

**描述**：清空用户浏览足迹

**请求参数**：无

**响应示例**：
```json
{
  "code": 0,
  "message": "清空成功"
}
```

---

## 10. 会员与营销模块

### 10.1 获取积分明细
**接口**：`GET /user/points/logs`

**描述**：获取用户积分明细

**请求参数**：
| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| page | int | N | 页码,默认1 |
| page_size | int | N | 每页数量,默认20 |

**响应示例**：
```json
{
  "code": 0,
  "data": {
    "total_points": 1000,
    "available_points": 500,
    "frozen_points": 0,
    "expire_points": 0,
    "list": [
      {
        "id": 1,
        "change_type": 1,
        "change_type_text": "获取",
        "points": 100,
        "business_type": "order",
        "business_id": 10001,
        "remark": "下单赠送",
        "created_at": "2024-01-15 10:35:00"
      }
    ],
    "total": 10,
    "page": 1,
    "page_size": 20,
    "has_more": false
  }
}
```

### 10.2 获取优惠券列表
**接口**：`GET /user/coupons`

**描述**：获取用户优惠券列表

**请求参数**：
| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| status | string | N | 优惠券状态:available/used/expired,默认available |
| page | int | N | 页码,默认1 |
| page_size | int | N | 每页数量,默认20 |

**响应示例**：
```json
{
  "code": 0,
  "data": {
    "list": [
      {
        "id": 1,
        "coupon": {
          "id": 10,
          "name": "满100减50",
          "type": 1,
          "value": 50.00,
          "threshold": 100.00,
          "scope_type": 1,
          "use_rule_desc": "全场通用",
          "start_time": "2024-01-01 00:00:00",
          "end_time": "2024-12-31 23:59:59"
        },
        "status": 0,
        "status_text": "未使用",
        "receive_time": "2024-01-10 10:00:00",
        "expire_time": "2024-12-31 23:59:59"
      }
    ],
    "total": 5,
    "page": 1,
    "page_size": 20,
    "has_more": false
  }
}
```

### 10.3 领取优惠券
**接口**：`POST /coupons/{id}/receive`

**描述**：领取优惠券

**请求参数**：
| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| id | int | Y | 优惠券模板ID |

**响应示例**：
```json
{
  "code": 0,
  "message": "领取成功",
  "data": {
    "user_coupon_id": 1
  }
}
```

### 10.4 获取可领取优惠券
**接口**：`GET /coupons/available`

**描述**：获取可领取的优惠券列表

**请求参数**：
| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| page | int | N | 页码,默认1 |
| page_size | int | N | 每页数量,默认20 |

**响应示例**：
```json
{
  "code": 0,
  "data": {
    "list": [
      {
        "id": 10,
        "name": "满100减50",
        "type": 1,
        "value": 50.00,
        "threshold": 100.00,
        "scope_type": 1,
        "use_rule_desc": "全场通用",
        "start_time": "2024-01-01 00:00:00",
        "end_time": "2024-12-31 23:59:59",
        "total_count": 1000,
        "remain_count": 500,
        "receive_limit_per_user": 1,
        "received": false
      }
    ],
    "total": 10,
    "page": 1,
    "page_size": 20,
    "has_more": false
  }
}
```

---

## 11. 专题模块

### 11.1 获取专题详情
**接口**：`GET /topics/{id}`

**描述**：获取专题详情

**请求参数**：
| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| id | int | Y | 专题ID或专题编码 |

**响应示例**：
```json
{
  "code": 0,
  "data": {
    "id": 1,
    "topic_no": "wedding_2024",
    "title": "婚嫁专区",
    "subtitle": "精选婚嫁床品",
    "cover_image": "https://cdn.shiyuhome.com/topic1.jpg",
    "theme_type": "wedding",
    "description": "为您的婚礼精选床品",
    "content_json": {
      "floors": [
        {
          "type": "banner",
          "data": {
            "image_url": "https://cdn.shiyuhome.com/topic_banner1.jpg"
          }
        }
      ]
    },
    "products": [
      {
        "id": 100,
        "title": "婚庆四件套",
        "main_image": "https://cdn.shiyuhome.com/prod1.jpg",
        "min_price": 899.00,
        "max_price": 1999.00,
        "sales_count": 567,
        "recommend_reason": "喜庆大红色"
      }
    ],
    "start_time": "2024-01-01 00:00:00",
    "end_time": "2024-12-31 23:59:59",
    "created_at": "2024-01-01 00:00:00"
  }
}
```

---

## 12. 公共模块

### 12.1 上传图片
**接口**：`POST /upload/image`

**描述**：上传图片

**请求参数**：
| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| file | file | Y | 图片文件 |
| type | string | N | 图片类型:avatar/product/evidence,默认通用 |

**请求示例**：multipart/form-data

**响应示例**：
```json
{
  "code": 0,
  "message": "上传成功",
  "data": {
    "url": "https://cdn.shiyuhome.com/upload/xxx.jpg",
    "width": 800,
    "height": 800,
    "size": 102400
  }
}
```

### 12.2 获取配置信息
**接口**：`GET /config`

**描述**：获取全局配置信息(品牌信息、客服电话等)

**请求参数**：无

**响应示例**：
```json
{
  "code": 0,
  "data": {
    "brand": {
      "name": "诗语家居",
      "logo": "https://cdn.shiyuhome.com/logo.png",
      "phone": "15204083071",
      "promise": "48小时内发货"
    },
    "service": {
      "phone": "15204083071",
      "wechat": "shiyu_service"
    },
    "about": {
      "introduction": "诗语家居专注于高品质家纺产品...",
      "shipping_policy": "订单支付成功后48小时内发货...",
      "after_sale_policy": "支持7天无理由退货..."
    }
  }
}
```

---

## 13. 附录

### 13.1 订单状态映射

| 状态码 | 状态名称 | 前端显示 | 可执行操作 |
| --- | --- | --- | --- |
| 10 | 待付款 | 待付款 | 取消订单、支付 |
| 20 | 待发货 | 待发货 | - |
| 30 | 待收货 | 待收货 | 确认收货、查看物流 |
| 40 | 已完成 | 已完成 | 再次购买、评价 |
| 50 | 已取消 | 已取消 | - |
| 60 | 售后中 | 售后中 | 查看售后详情 |
| 70 | 已关闭 | 已关闭 | - |

### 13.2 售后状态映射

| 状态码 | 状态名称 | 前端显示 | 可执行操作 |
| --- | --- | --- | --- |
| 10 | 待审核 | 审核中 | 取消售后 |
| 20 | 待退货 | 待退货 | 填写物流、取消售后 |
| 30 | 待收货 | 商家收货中 | - |
| 40 | 退款中 | 退款中 | - |
| 50 | 已退款 | 已退款 | - |
| 60 | 已完成 | 已完成 | - |
| 70 | 已驳回 | 已驳回 | 重新申请 |

### 13.3 库存状态映射

| 状态码 | 状态名称 | 前端显示 |
| --- | --- | --- |
| 1 | 有货 | 现货 |
| 2 | 紧张 | 库存紧张 |
| 3 | 售罄 | 已售罄 |

### 13.4 支付状态映射

| 状态码 | 状态名称 |
| --- | --- |
| 10 | 未支付 |
| 20 | 已支付 |
| 30 | 支付失败 |
| 40 | 部分退款 |
| 50 | 全额退款 |

---

## 14. 更新记录

| 版本 | 日期 | 更新内容 | 更新人 |
| --- | --- | --- | --- |
| 1.0.0 | 2024-01-15 | 初始版本 | AI Assistant |

---

**文档维护方**：诗语家居技术团队  
**最后更新时间**：2024-01-15
