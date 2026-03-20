# 诗语家居数据库设计（V2）

## 1. 设计目标与原则
- 支持家纺类商品的多规格、多材质、多场景表达。
- 支持完整交易链路：购物车、下单、支付、发货、签收、售后。
- 支持用户资产：优惠券、积分、储值卡/礼品卡。
- 关键业务数据采用快照机制，确保历史订单可追溯。
- 金额字段统一使用 `DECIMAL(10,2)`，时间字段统一使用 `DATETIME`。
- 除流水表外，核心业务表统一建议包含 `created_at`、`updated_at`、`deleted` 或等价状态字段。

## 2. 用户与会员域

### 2.1 `users`（用户基础表）
| 字段名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| id | BIGINT | Y | 主键 |
| openid | VARCHAR(64) | Y | 微信 OpenID，唯一索引 |
| unionid | VARCHAR(64) | N | 微信 UnionID |
| nickname | VARCHAR(64) | N | 用户昵称，默认 `诗语访客` |
| avatar_url | VARCHAR(255) | N | 头像 URL |
| phone_cipher | VARCHAR(255) | N | 加密手机号 |
| phone_mask | VARCHAR(20) | N | 脱敏手机号 |
| gender | TINYINT | N | 0 未知，1 男，2 女 |
| status | TINYINT | Y | 1 正常，0 禁用 |
| last_login_at | DATETIME | N | 最后登录时间 |
| created_at | DATETIME | Y | 创建时间 |
| updated_at | DATETIME | Y | 更新时间 |

### 2.2 `user_addresses`（用户收货地址表）
| 字段名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| id | BIGINT | Y | 主键 |
| user_id | BIGINT | Y | 关联 `users.id` |
| consignee | VARCHAR(50) | Y | 收件人姓名 |
| phone_cipher | VARCHAR(255) | Y | 加密手机号 |
| phone_mask | VARCHAR(20) | Y | 脱敏手机号 |
| province | VARCHAR(50) | Y | 省 |
| city | VARCHAR(50) | Y | 市 |
| district | VARCHAR(50) | Y | 区 |
| detail_address | VARCHAR(200) | Y | 详细地址 |
| postal_code | VARCHAR(20) | N | 邮编 |
| tag | VARCHAR(20) | N | 标签，如家、公司 |
| longitude | DECIMAL(10,6) | N | 经度 |
| latitude | DECIMAL(10,6) | N | 纬度 |
| is_default | TINYINT | Y | 1 默认，0 非默认 |
| deleted | TINYINT | Y | 0 未删，1 已删 |
| created_at | DATETIME | Y | 创建时间 |
| updated_at | DATETIME | Y | 更新时间 |

### 2.3 `user_favorites`（用户收藏表）
| 字段名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| id | BIGINT | Y | 主键 |
| user_id | BIGINT | Y | 用户 ID |
| product_id | BIGINT | Y | 商品 SPU ID |
| created_at | DATETIME | Y | 收藏时间 |

### 2.4 `user_footprints`（浏览足迹表）
| 字段名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| id | BIGINT | Y | 主键 |
| user_id | BIGINT | Y | 用户 ID |
| product_id | BIGINT | Y | 商品 SPU ID |
| viewed_at | DATETIME | Y | 浏览时间 |

### 2.5 `search_histories`（搜索历史表）
| 字段名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| id | BIGINT | Y | 主键 |
| user_id | BIGINT | Y | 用户 ID |
| keyword | VARCHAR(100) | Y | 搜索关键词 |
| search_count | INT | Y | 搜索次数 |
| last_search_at | DATETIME | Y | 最近搜索时间 |

### 2.6 `user_points_accounts`（积分账户表）
| 字段名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| id | BIGINT | Y | 主键 |
| user_id | BIGINT | Y | 用户 ID，唯一 |
| total_points | INT | Y | 累计积分 |
| available_points | INT | Y | 可用积分 |
| frozen_points | INT | Y | 冻结积分 |
| updated_at | DATETIME | Y | 更新时间 |

### 2.7 `user_points_logs`（积分流水表）
| 字段名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| id | BIGINT | Y | 主键 |
| user_id | BIGINT | Y | 用户 ID |
| change_type | TINYINT | Y | 1 获取，2 抵扣，3 退款返还，4 过期 |
| points | INT | Y | 变动积分，正负值 |
| business_type | VARCHAR(30) | Y | 订单、活动、后台调整等 |
| business_id | BIGINT | N | 关联业务 ID |
| remark | VARCHAR(255) | N | 备注 |
| created_at | DATETIME | Y | 创建时间 |

### 2.8 `stored_value_cards`（储值卡/礼品卡表）
| 字段名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| id | BIGINT | Y | 主键 |
| card_no | VARCHAR(64) | Y | 卡号，唯一 |
| card_type | TINYINT | Y | 1 储值卡，2 礼品卡 |
| user_id | BIGINT | N | 持卡用户 ID |
| balance | DECIMAL(10,2) | Y | 当前余额 |
| status | TINYINT | Y | 1 未激活，2 可用，3 冻结，4 作废 |
| expire_time | DATETIME | N | 过期时间 |
| created_at | DATETIME | Y | 创建时间 |
| updated_at | DATETIME | Y | 更新时间 |

### 2.9 `stored_value_logs`（储值卡流水表）
| 字段名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| id | BIGINT | Y | 主键 |
| card_id | BIGINT | Y | 卡 ID |
| user_id | BIGINT | N | 用户 ID |
| change_type | TINYINT | Y | 1 充值，2 消费，3 退款返还，4 后台调整 |
| amount | DECIMAL(10,2) | Y | 变动金额 |
| business_type | VARCHAR(30) | Y | 订单、退款等 |
| business_id | BIGINT | N | 关联业务 ID |
| created_at | DATETIME | Y | 创建时间 |

## 3. 商品与内容域

### 3.1 `categories`（商品分类表）
| 字段名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| id | INT | Y | 主键 |
| parent_id | INT | N | 父分类 ID，一级分类可为空 |
| name | VARCHAR(50) | Y | 分类名称 |
| icon_url | VARCHAR(255) | N | 图标 URL |
| sort_order | INT | Y | 排序值 |
| status | TINYINT | Y | 1 显示，0 隐藏 |
| deleted | TINYINT | Y | 0 未删，1 已删 |
| created_at | DATETIME | Y | 创建时间 |
| updated_at | DATETIME | Y | 更新时间 |

### 3.2 `products`（商品 SPU 表）
| 字段名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| id | BIGINT | Y | 主键 |
| product_no | VARCHAR(64) | Y | 商品编码，唯一 |
| category_id | INT | Y | 分类 ID |
| title | VARCHAR(120) | Y | 商品标题 |
| subtitle | VARCHAR(255) | N | 副标题/卖点 |
| product_type | VARCHAR(30) | Y | 床盖、四件套、被芯、枕芯等 |
| style_tag | VARCHAR(50) | N | 风格标签，如婚嫁、极简 |
| season_tag | VARCHAR(30) | N | 季节标签，如春夏、秋冬、四季 |
| material_summary | VARCHAR(100) | N | 材质摘要 |
| main_image | VARCHAR(255) | Y | 主图 |
| min_price | DECIMAL(10,2) | Y | 最低售价 |
| max_price | DECIMAL(10,2) | Y | 最高售价 |
| sales_count | INT | Y | 累计销量 |
| sort_order | INT | Y | 排序 |
| is_new | TINYINT | Y | 是否新品 |
| is_hot | TINYINT | Y | 是否热销 |
| status | TINYINT | Y | 1 上架，0 下架 |
| deleted | TINYINT | Y | 0 未删，1 已删 |
| created_at | DATETIME | Y | 创建时间 |
| updated_at | DATETIME | Y | 更新时间 |

### 3.3 `product_media`（商品媒体表）
| 字段名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| id | BIGINT | Y | 主键 |
| product_id | BIGINT | Y | 商品 ID |
| media_type | TINYINT | Y | 1 主图，2 详情图，3 场景图，4 视频 |
| media_url | VARCHAR(255) | Y | 媒体地址 |
| sort_order | INT | Y | 排序 |
| created_at | DATETIME | Y | 创建时间 |

### 3.4 `product_detail_attrs`（商品详情属性表）
| 字段名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| id | BIGINT | Y | 主键 |
| product_id | BIGINT | Y | 商品 ID |
| attr_name | VARCHAR(50) | Y | 属性名，如材质、支数、适用季节 |
| attr_value | VARCHAR(255) | Y | 属性值 |
| sort_order | INT | Y | 排序 |
| created_at | DATETIME | Y | 创建时间 |

### 3.5 `product_spec_groups`（规格组表）
| 字段名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| id | BIGINT | Y | 主键 |
| product_id | BIGINT | Y | 商品 ID |
| name | VARCHAR(50) | Y | 规格组名称，如尺寸、颜色、材质 |
| sort_order | INT | Y | 排序 |
| created_at | DATETIME | Y | 创建时间 |

### 3.6 `product_spec_values`（规格值表）
| 字段名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| id | BIGINT | Y | 主键 |
| spec_group_id | BIGINT | Y | 规格组 ID |
| value | VARCHAR(50) | Y | 规格值，如 1.8m、奶白、天丝 |
| sort_order | INT | Y | 排序 |
| created_at | DATETIME | Y | 创建时间 |

### 3.7 `product_skus`（商品 SKU 表）
| 字段名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| id | BIGINT | Y | 主键 |
| product_id | BIGINT | Y | 商品 ID |
| sku_code | VARCHAR(64) | Y | SKU 编码，唯一 |
| image_url | VARCHAR(255) | N | 当前规格主图 |
| specs_json | JSON | Y | 规格快照，如 `{\"size\":\"1.8m\",\"color\":\"奶白\"}` |
| price | DECIMAL(10,2) | Y | 销售价 |
| original_price | DECIMAL(10,2) | N | 划线价 |
| cost_price | DECIMAL(10,2) | N | 成本价 |
| stock | INT | Y | 可售库存 |
| locked_stock | INT | Y | 锁定库存 |
| sales_count | INT | Y | 销量 |
| weight | DECIMAL(10,2) | N | 重量，便于物流计算 |
| status | TINYINT | Y | 1 启用，0 禁用 |
| created_at | DATETIME | Y | 创建时间 |
| updated_at | DATETIME | Y | 更新时间 |

### 3.8 `sku_spec_relations`（SKU 与规格值关联表）
| 字段名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| id | BIGINT | Y | 主键 |
| sku_id | BIGINT | Y | SKU ID |
| spec_group_id | BIGINT | Y | 规格组 ID |
| spec_value_id | BIGINT | Y | 规格值 ID |

## 4. 交易域

### 4.1 `cart_items`（购物车表）
| 字段名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| id | BIGINT | Y | 主键 |
| user_id | BIGINT | Y | 用户 ID |
| product_id | BIGINT | Y | 商品 SPU ID |
| sku_id | BIGINT | Y | 商品 SKU ID |
| quantity | INT | Y | 购买数量 |
| is_selected | TINYINT | Y | 1 已选，0 未选 |
| created_at | DATETIME | Y | 创建时间 |
| updated_at | DATETIME | Y | 更新时间 |

> 约束建议：`UNIQUE(user_id, sku_id)`，避免同一用户购物车内同一 SKU 重复插入。

### 4.2 `orders`（订单主表）
| 字段名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| id | BIGINT | Y | 主键 |
| order_no | VARCHAR(64) | Y | 订单号，唯一 |
| user_id | BIGINT | Y | 用户 ID |
| total_amount | DECIMAL(10,2) | Y | 商品总金额 |
| discount_amount | DECIMAL(10,2) | Y | 总优惠金额 |
| coupon_amount | DECIMAL(10,2) | Y | 优惠券抵扣 |
| points_amount | DECIMAL(10,2) | Y | 积分抵扣 |
| card_amount | DECIMAL(10,2) | Y | 储值卡抵扣 |
| freight_amount | DECIMAL(10,2) | Y | 运费 |
| pay_amount | DECIMAL(10,2) | Y | 实付金额 |
| status | TINYINT | Y | 10 待付款，20 待发货，30 待收货，40 已完成，50 已取消，60 售后中 |
| pay_channel | TINYINT | N | 1 微信支付，2 储值卡混合支付 |
| consignee_info | JSON | Y | 地址快照 |
| remark | VARCHAR(255) | N | 用户备注 |
| payment_time | DATETIME | N | 支付时间 |
| delivery_time | DATETIME | N | 发货时间 |
| finish_time | DATETIME | N | 完成时间 |
| cancel_time | DATETIME | N | 取消时间 |
| cancel_reason | VARCHAR(255) | N | 取消原因 |
| created_at | DATETIME | Y | 下单时间 |
| updated_at | DATETIME | Y | 更新时间 |

### 4.3 `order_items`（订单明细表）
| 字段名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| id | BIGINT | Y | 主键 |
| order_id | BIGINT | Y | 订单 ID |
| product_id | BIGINT | Y | 商品 ID 快照 |
| product_title | VARCHAR(120) | Y | 商品标题快照 |
| product_image | VARCHAR(255) | Y | 商品主图快照 |
| sku_id | BIGINT | Y | SKU ID 快照 |
| sku_specs | VARCHAR(255) | Y | 规格快照 |
| unit_price | DECIMAL(10,2) | Y | 购买时单价 |
| quantity | INT | Y | 购买数量 |
| subtotal | DECIMAL(10,2) | Y | 小计金额 |
| refund_status | TINYINT | Y | 0 无售后，1 售后中，2 已退款 |
| created_at | DATETIME | Y | 创建时间 |

### 4.4 `payment_records`（支付流水表）
| 字段名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| id | BIGINT | Y | 主键 |
| order_id | BIGINT | Y | 订单 ID |
| order_no | VARCHAR(64) | Y | 订单号 |
| pay_channel | TINYINT | Y | 支付渠道 |
| out_trade_no | VARCHAR(64) | Y | 商户支付单号 |
| transaction_id | VARCHAR(64) | N | 微信交易流水号 |
| pay_amount | DECIMAL(10,2) | Y | 支付金额 |
| pay_status | TINYINT | Y | 10 待支付，20 成功，30 失败，40 已退款 |
| callback_payload | JSON | N | 回调报文 |
| paid_at | DATETIME | N | 支付成功时间 |
| created_at | DATETIME | Y | 创建时间 |

### 4.5 `shipments`（发货表）
| 字段名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| id | BIGINT | Y | 主键 |
| order_id | BIGINT | Y | 订单 ID |
| company_code | VARCHAR(30) | Y | 物流公司编码 |
| company_name | VARCHAR(50) | Y | 物流公司名称 |
| tracking_no | VARCHAR(64) | Y | 运单号 |
| ship_status | TINYINT | Y | 10 待发货，20 已发货，30 已签收，40 异常 |
| shipped_at | DATETIME | N | 发货时间 |
| received_at | DATETIME | N | 签收时间 |
| created_at | DATETIME | Y | 创建时间 |
| updated_at | DATETIME | Y | 更新时间 |

### 4.6 `order_logs`（订单操作日志表）
| 字段名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| id | BIGINT | Y | 主键 |
| order_id | BIGINT | Y | 订单 ID |
| action | VARCHAR(50) | Y | 动作，如 `USER_PAY`、`ADMIN_SHIP` |
| operator_type | TINYINT | Y | 1 用户，2 管理员，3 系统 |
| operator_id | BIGINT | N | 操作人 ID |
| remark | VARCHAR(255) | N | 备注 |
| created_at | DATETIME | Y | 操作时间 |

### 4.7 `after_sale_orders`（售后单表）
| 字段名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| id | BIGINT | Y | 主键 |
| after_sale_no | VARCHAR(64) | Y | 售后单号，唯一 |
| order_id | BIGINT | Y | 订单 ID |
| order_item_id | BIGINT | Y | 订单明细 ID |
| user_id | BIGINT | Y | 用户 ID |
| type | TINYINT | Y | 1 仅退款，2 退货退款，3 换货 |
| reason_code | VARCHAR(30) | Y | 售后原因编码 |
| reason_desc | VARCHAR(255) | N | 补充说明 |
| evidence_urls | JSON | N | 凭证图片 |
| refund_amount | DECIMAL(10,2) | N | 申请退款金额 |
| status | TINYINT | Y | 10 待审核，20 待退货，30 待收货，40 已退款，50 已驳回，60 已完成 |
| refund_status | TINYINT | Y | 0 未退款，1 退款中，2 已退款 |
| created_at | DATETIME | Y | 创建时间 |
| updated_at | DATETIME | Y | 更新时间 |

### 4.8 `after_sale_logs`（售后日志表）
| 字段名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| id | BIGINT | Y | 主键 |
| after_sale_id | BIGINT | Y | 售后单 ID |
| action | VARCHAR(50) | Y | 动作，如 `APPLY`、`APPROVE`、`REFUND` |
| operator_type | TINYINT | Y | 1 用户，2 管理员，3 系统 |
| operator_id | BIGINT | N | 操作人 ID |
| remark | VARCHAR(255) | N | 备注 |
| created_at | DATETIME | Y | 创建时间 |

## 5. 营销与运营域

### 5.1 `coupons`（优惠券模板表）
| 字段名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| id | BIGINT | Y | 主键 |
| name | VARCHAR(100) | Y | 券名称 |
| type | TINYINT | Y | 1 满减，2 折扣 |
| value | DECIMAL(10,2) | Y | 面额或折扣值 |
| threshold | DECIMAL(10,2) | Y | 使用门槛 |
| scope_type | TINYINT | Y | 1 全场，2 指定分类，3 指定商品 |
| start_time | DATETIME | Y | 生效时间 |
| end_time | DATETIME | Y | 失效时间 |
| total_count | INT | Y | 发放总量 |
| remain_count | INT | Y | 剩余数量 |
| status | TINYINT | Y | 1 生效，0 失效 |
| created_at | DATETIME | Y | 创建时间 |
| updated_at | DATETIME | Y | 更新时间 |

### 5.2 `user_coupons`（用户领券表）
| 字段名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| id | BIGINT | Y | 主键 |
| user_id | BIGINT | Y | 用户 ID |
| coupon_id | BIGINT | Y | 优惠券模板 ID |
| status | TINYINT | Y | 0 未使用，1 已使用，2 已过期 |
| order_id | BIGINT | N | 核销订单 ID |
| receive_time | DATETIME | Y | 领取时间 |
| use_time | DATETIME | N | 使用时间 |
| expire_time | DATETIME | Y | 过期时间 |

### 5.3 `banners`（轮播图表）
| 字段名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| id | BIGINT | Y | 主键 |
| image_url | VARCHAR(255) | Y | 图片地址 |
| title | VARCHAR(100) | N | 标题 |
| link_type | VARCHAR(20) | Y | `product`、`category`、`topic`、`url` |
| link_value | VARCHAR(255) | Y | 跳转目标 |
| sort_order | INT | Y | 排序 |
| status | TINYINT | Y | 1 启用，0 停用 |
| start_time | DATETIME | N | 生效时间 |
| end_time | DATETIME | N | 失效时间 |
| created_at | DATETIME | Y | 创建时间 |
| updated_at | DATETIME | Y | 更新时间 |

### 5.4 `search_hot_keywords`（热门搜索词表）
| 字段名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| id | BIGINT | Y | 主键 |
| keyword | VARCHAR(100) | Y | 热搜词 |
| sort_order | INT | Y | 排序 |
| status | TINYINT | Y | 1 启用，0 停用 |
| created_at | DATETIME | Y | 创建时间 |

## 6. 后台与权限域

### 6.1 `admin_users`（后台用户表）
| 字段名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| id | BIGINT | Y | 主键 |
| username | VARCHAR(50) | Y | 登录名，唯一 |
| password_hash | VARCHAR(255) | Y | 密码摘要 |
| real_name | VARCHAR(50) | N | 姓名 |
| phone | VARCHAR(20) | N | 联系电话 |
| status | TINYINT | Y | 1 启用，0 禁用 |
| created_at | DATETIME | Y | 创建时间 |
| updated_at | DATETIME | Y | 更新时间 |

### 6.2 `admin_roles`（后台角色表）
| 字段名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| id | BIGINT | Y | 主键 |
| role_name | VARCHAR(50) | Y | 角色名称 |
| role_code | VARCHAR(50) | Y | 角色编码，唯一 |
| created_at | DATETIME | Y | 创建时间 |

### 6.3 `admin_user_roles`（后台用户角色关联表）
| 字段名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| id | BIGINT | Y | 主键 |
| admin_user_id | BIGINT | Y | 后台用户 ID |
| role_id | BIGINT | Y | 角色 ID |

### 6.4 `operation_logs`（后台操作日志表）
| 字段名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| id | BIGINT | Y | 主键 |
| operator_id | BIGINT | Y | 操作人 ID |
| module | VARCHAR(50) | Y | 模块，如商品、订单、售后 |
| action | VARCHAR(50) | Y | 操作类型 |
| target_id | BIGINT | N | 目标业务 ID |
| content | VARCHAR(500) | N | 操作内容 |
| created_at | DATETIME | Y | 创建时间 |

## 7. 索引与约束建议
- `users.openid`：唯一索引。
- `products(category_id, status, sort_order)`：联合索引，加速分类列表查询。
- `product_skus(product_id, status)`：联合索引，加速详情页 SKU 查询。
- `cart_items(user_id, sku_id)`：唯一索引。
- `orders.order_no`：唯一索引；`orders(user_id, status, created_at)`：联合索引。
- `order_items(order_id)`、`payment_records(order_id)`、`shipments(order_id)`：普通索引。
- `after_sale_orders(order_id, order_item_id, status)`：联合索引。
- `user_coupons(user_id, status, expire_time)`：联合索引。

## 8. 关键设计说明
1. 订单、订单明细、支付、地址、售后均需保留业务快照，避免商品改价、下架或规格变更影响历史数据。
2. 家纺类商品规格复杂，`specs_json` 仅作为快照展示；真实规格关系建议依赖 `product_spec_groups`、`product_spec_values`、`sku_spec_relations` 管理。
3. 若后续接入专题推荐或内容营销，可继续扩展 `topics`、`topic_products` 等内容表。
4. 若需要支持分销中心，可新增分销员、分销关系、佣金流水等独立模块，不建议直接混入订单主表。
