# 诗语家居小程序 - 后端架构设计文档

## 1. 系统概述

### 1.1 项目背景
诗语家居是一个面向微信生态的家纺类电商小程序，核心经营床品、被子、枕头、凉席、婚嫁套件及专柜系列商品。后端系统需要支撑完整的电商业务闭环，包括商品管理、订单交易、支付结算、物流履约、售后服务、会员营销等核心业务模块。

### 1.2 业务特点
- **复杂规格管理**：家纺商品规格复杂，需要支持尺寸、颜色、材质、款式等多维度规格组合
- **高并发场景**：活动期间可能存在秒杀、拼团等高并发场景
- **交易敏感性**：涉及支付、退款等资金操作，对数据一致性和安全性要求高
- **运营灵活性**：需要支持灵活的促销活动、优惠券、积分等营销玩法

### 1.3 设计目标
- **高可用**：系统可用性达到 99.9%，核心业务链路无单点故障
- **高性能**：接口响应时间 < 200ms，支持 500+ 并发用户
- **可扩展**：采用微服务架构，支持按业务域独立扩展
- **安全性**：数据加密存储，支付安全可靠，防刷防作弊
- **可维护**：代码结构清晰，文档完善，易于运维

---

## 2. 系统架构设计

### 2.1 整体架构

```
┌─────────────────────────────────────────────────────────────┐
│                        微信小程序                             │
│                    (用户交互层)                               │
└────────────────────┬────────────────────────────────────────┘
                     │ HTTPS
┌────────────────────┴────────────────────────────────────────┐
│                     API Gateway                              │
│            (Nginx + 负载均衡)                                │
│  • 路由转发  • 限流熔断  • 认证鉴权  • 日志追踪              │
└────────────────────┬────────────────────────────────────────┘
                     │
    ┌────────────────┼────────────────┐
    │                │                │
┌───┴────┐      ┌───┴────┐      ┌───┴────┐
│ 用户服务│      │ 商品服务│      │ 订单服务│
└───┬────┘      └───┬────┘      └───┬────┘
    │                │                │
┌───┴────┐      ┌───┴────┐      ┌───┴────┐
│ 营销服务│      │ 支付服务│      │ 售后服务│
└───┬────┘      └───┬────┘      └───┬────┘
    │                │                │
    └────────────────┼────────────────┘
                     │
    ┌────────────────┼────────────────┐
    │                │                │
┌───┴────────┐  ┌───┴────────┐  ┌───┴────────┐
│  MySQL     │  │  Redis     │  │  对象存储  │
│ (主数据库) │  │ (缓存/队列)│  │  (图片/视频)│
└────────────┘  └────────────┘  └────────────┘
```

### 2.2 技术架构层次

#### 2.2.1 接入层
- **API Gateway**：统一入口，负责路由、限流、熔断、认证
- **负载均衡**：Nginx，支持多种负载均衡策略（轮询、权重、IP哈希）
- **HTTPS终结**：SSL证书管理，保证传输安全

#### 2.2.2 应用层
采用**微服务架构**，按业务域拆分服务：

| 服务名称 | 职责 | 核心功能 |
| --- | --- | --- |
| shiyu-user | 用户与会员管理 | 登录注册、用户信息、地址管理、收藏足迹 |
| shiyu-product | 商品与内容管理 | 商品CRUD、分类管理、规格管理、库存管理 |
| shiyu-order | 订单与交易管理 | 订单创建、状态流转、库存锁定、价格计算 |
| shiyu-payment | 支付与结算管理 | 微信支付、支付回调、退款处理 |
| shiyu-marketing | 营销活动管理 | 优惠券、积分、储值卡、专题活动 |
| shiyu-aftersale | 售后服务管理 | 售后申请、审核、退货、退款 |
| shiyu-logistics | 物流与发货管理 | 发货管理、物流查询、轨迹追踪 |
| shiyu-search | 搜索与推荐 | 商品搜索、关键词建议、推荐算法 |
| shiyu-notification | 消息通知 | 模板消息、订阅消息、短信通知 |
| shiyu-admin | 后台管理 | 商品管理、订单管理、用户管理、权限管理 |

#### 2.2.3 数据层
- **MySQL 8.0**：主数据库，存储业务数据（CloudBase MySQL）
- **Redis 6.0+**：缓存层，提升性能；消息队列，异步处理
- **腾讯云COS / 阿里云OSS**：对象存储，存储图片、视频等静态资源
- **ELK**：日志存储与分析（Elasticsearch + Logstash + Kibana）

### 2.3 部署架构

```
┌─────────────────────────────────────────────┐
│           腾讯云 CloudBase                  │
│  ┌───────────────────────────────────────┐  │
│  │      CloudRun (容器服务)              │  │
│  │  ┌─────┐ ┌─────┐ ┌─────┐ ┌─────┐   │  │
│  │  │服务1│ │服务2│ │服务3│ │服务N│   │  │
│  │  └─────┘ └─────┘ └─────┘ └─────┘   │  │
│  └───────────────────────────────────────┘  │
│  ┌───────────────────────────────────────┐  │
│  │      CloudBase MySQL                  │  │
│  │      (主从架构)                       │  │
│  └───────────────────────────────────────┘  │
│  ┌───────────────────────────────────────┐  │
│  │      Redis 集群                       │  │
│  └───────────────────────────────────────┘  │
│  ┌───────────────────────────────────────┐  │
│  │     对象存储 (COS / OSS)              │  │
│  └───────────────────────────────────────┘  │
└─────────────────────────────────────────────┘
```

---

## 3. 技术选型

### 3.1 后端技术栈（Java 8）

#### 3.1.1 开发语言与框架
- **语言**：Java 8 (JDK 1.8)
- **核心框架**：Spring Boot 2.7.x（兼容 Java 8）
- **微服务框架**：Spring Cloud 2021.x（可选）
- **Web框架**：Spring MVC
- **安全框架**：Spring Security + JWT
- **ORM框架**：MyBatis-Plus 3.5.x
- **数据库连接池**：Druid / HikariCP
- **API文档**：Swagger2 / Knife4j

**选型理由**：
- Java 8 是企业级应用主流版本，稳定成熟
- Spring Boot 2.7.x 完美兼容 Java 8，生态完善
- MyBatis-Plus 简化 MyBatis 开发，提高效率
- Swagger 生成接口文档，便于前后端协作

#### 3.1.2 数据库与缓存
- **关系数据库**：MySQL 8.0 (CloudBase MySQL)
- **缓存**：Redis 6.0+ (Cluster模式)
- **对象存储**：腾讯云COS / 阿里云OSS

**选型理由**：
- MySQL 成熟稳定，支持事务，适合电商业务
- Redis 提供高性能缓存和分布式锁能力
- CloudBase 提供一站式数据库解决方案，降低运维成本

#### 3.1.3 中间件
- **消息队列**：RabbitMQ / RocketMQ
- **任务调度**：XXL-Job / Elastic-Job
- **日志系统**：Logback + ELK
- **监控告警**：Prometheus + Grafana
- **分布式事务**：Seata（可选）
- **注册中心**：Nacos / Eureka（微服务架构）

### 3.2 开发工具

| 工具类型 | 工具名称 | 用途 |
| --- | --- | --- |
| IDE | IntelliJ IDEA | 开发工具 |
| 构建工具 | Maven 3.6+ | 项目构建、依赖管理 |
| 版本控制 | Git | 代码版本管理 |
| 代码检查 | SonarQube | 代码质量检查 |
| 接口测试 | Postman / JMeter | 接口测试、性能测试 |
| 容器化 | Docker | 应用容器化 |

### 3.3 第三方服务

| 服务类型 | 服务提供商 | 用途 |
| --- | --- | --- |
| 支付服务 | 微信支付 | 微信小程序支付 |
| 物流查询 | 快递100/快递鸟 | 物流轨迹查询 |
| 短信服务 | 腾讯云SMS | 订单通知、验证码 |
| 对象存储 | 腾讯云COS / 阿里云OSS | 图片、视频存储 |
| CDN加速 | 腾讯云CDN | 静态资源加速 |

---

## 4. 核心业务流程设计

### 4.1 用户登录注册流程

```
小程序端                     后端服务                    微信开放平台
  │                           │                             │
  ├─ wx.login() ────────────>│                             │
  │                           ├─ 调用微信API获取openid ────>│
  │                           │<──── 返回openid ─────────────┤
  │                           ├─ 查询用户是否存在           │
  │                           │  - 存在:返回token           │
  │                           │  - 不存在:创建用户,返回token │
  │<──── 返回token ───────────┤                             │
  │                           │                             │
```

**实现要点**：
```java
/**
 * 微信登录接口
 */
@Service
public class WechatLoginService {
    
    @Autowired
    private WechatApiClient wechatApiClient;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private JwtTokenUtil jwtTokenUtil;
    
    public LoginResultVO login(String code) {
        // 1. 调用微信API获取openid和session_key
        WechatSessionResult session = wechatApiClient.code2Session(code);
        String openid = session.getOpenid();
        
        // 2. 查询用户是否存在
        User user = userService.findByOpenid(openid);
        
        // 3. 不存在则创建用户
        if (user == null) {
            user = userService.createUser(openid, session.getUnionid());
        }
        
        // 4. 生成JWT Token
        String token = jwtTokenUtil.generateToken(user.getId(), openid);
        
        // 5. 返回登录结果
        return LoginResultVO.builder()
            .token(token)
            .userInfo(UserVO.from(user))
            .build();
    }
}
```

### 4.2 商品规格与SKU设计

```
商品SPU
  ├─ 规格组1:尺寸
  │   ├─ 1.5m
  │   ├─ 1.8m
  │   └─ 2.0m
  ├─ 规格组2:颜色
  │   ├─ 奶白
  │   ├─ 浅灰
  │   └─ 粉色
  └─ SKU矩阵
      ├─ SKU1: 1.5m + 奶白 = ¥599
      ├─ SKU2: 1.5m + 浅灰 = ¥599
      ├─ SKU3: 1.8m + 奶白 = ¥799
      └─ ...
```

**实体设计**：
```java
/**
 * 商品SPU
 */
@Data
@TableName("products")
public class Product {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String productNo;
    private Long categoryId;
    private String title;
    private String subtitle;
    private String mainImage;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Integer salesCount;
    private Integer stockStatus;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    @TableField(exist = false)
    private List<ProductSpecGroup> specGroups;
    
    @TableField(exist = false)
    private List<ProductSku> skuList;
}

/**
 * 规格组
 */
@Data
@TableName("product_spec_groups")
public class ProductSpecGroup {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long productId;
    private String name;
    private Integer sortOrder;
    
    @TableField(exist = false)
    private List<ProductSpecValue> values;
}

/**
 * SKU
 */
@Data
@TableName("product_skus")
public class ProductSku {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long productId;
    private String skuCode;
    private String specsJson; // JSON格式规格快照
    private BigDecimal price;
    private BigDecimal originalPrice;
    private Integer stock;
    private Integer lockedStock;
    private Integer status;
}
```

### 4.3 下单流程（核心）

```
用户                   订单服务          库存服务         支付服务         营销服务
 │                       │                 │                │                │
 ├─ 提交订单 ───────────>│                 │                │                │
 │                       ├─ 校验库存 ─────>│                │                │
 │                       │<── 库存充足 ────┤                │                │
 │                       ├─ 锁定库存 ─────>│                │                │
 │                       │<── 锁定成功 ────┤                │                │
 │                       ├─ 计算优惠 ───────────────────────────────────────>│
 │                       │<── 优惠金额 ──────────────────────────────────────┤
 │                       ├─ 创建订单       │                │                │
 │                       │  (状态:待付款)  │                │                │
 │<── 返回订单号 ─────────┤                 │                │                │
 │                       │                 │                │                │
 ├─ 发起支付 ───────────>│                 │                │                │
 │                       ├─ 创建支付单 ────────────────────>│                │
 │<── 支付参数 ───────────┤                 │                │                │
 │                       │                 │                │                │
 ├─ 完成支付 ────────────┼─────────────────┼───────────────>│                │
 │                       │                 │                ├─ 支付回调 ────>│
 │                       │<── 支付成功 ─────────────────────┤                │
 │                       ├─ 更新订单状态   │                │                │
 │                       │  (待付款->待发货)│                │                │
 │                       ├─ 扣减库存 ─────>│                │                │
 │                       │<── 扣减成功 ────┤                │                │
 │                       ├─ 发放积分 ───────────────────────────────────────>│
 │                       ├─ 核销优惠券 ─────────────────────────────────────>│
 │                       └─ 发送通知       │                │                │
```

**关键实现**：

#### 4.3.1 库存锁定（Redis分布式锁 + MySQL乐观锁）
```java
/**
 * 库存服务
 */
@Service
public class InventoryService {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private ProductSkuMapper skuMapper;
    
    @Autowired
    private StockReservationMapper reservationMapper;
    
    /**
     * 锁定库存
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean lockStock(Long skuId, Integer quantity, String orderNo) {
        String lockKey = "stock:lock:" + skuId;
        
        try {
            // 1. Redis分布式锁
            Boolean locked = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, orderNo, 30, TimeUnit.MINUTES);
            
            if (!locked) {
                return false;
            }
            
            // 2. 查询SKU
            ProductSku sku = skuMapper.selectById(skuId);
            if (sku == null || sku.getStock() < quantity) {
                return false;
            }
            
            // 3. MySQL乐观锁更新库存
            int rows = skuMapper.lockStock(skuId, quantity, sku.getVersion());
            if (rows == 0) {
                return false;
            }
            
            // 4. 创建库存锁定记录
            StockReservation reservation = new StockReservation();
            reservation.setOrderNo(orderNo);
            reservation.setSkuId(skuId);
            reservation.setLockedQty(quantity);
            reservation.setStatus(10); // 已锁定
            reservation.setExpireAt(LocalDateTime.now().plusMinutes(30));
            reservationMapper.insert(reservation);
            
            return true;
            
        } finally {
            // 释放Redis锁（根据业务场景决定是否立即释放）
        }
    }
    
    /**
     * 扣减库存（支付成功后）
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean deductStock(String orderNo) {
        // 1. 查询锁定记录
        List<StockReservation> reservations = reservationMapper
            .selectByOrderNo(orderNo);
        
        if (CollectionUtils.isEmpty(reservations)) {
            return false;
        }
        
        // 2. 扣减库存并更新锁定记录状态
        for (StockReservation reservation : reservations) {
            // 扣减实际库存
            int rows = skuMapper.deductStock(
                reservation.getSkuId(), 
                reservation.getLockedQty()
            );
            
            if (rows == 0) {
                throw new BusinessException("库存扣减失败");
            }
            
            // 更新锁定记录状态为已扣减
            reservation.setStatus(30);
            reservation.setDeductedAt(LocalDateTime.now());
            reservationMapper.updateById(reservation);
            
            // 记录库存流水
            inventoryLogMapper.insert(buildLog(reservation, "扣减"));
        }
        
        return true;
    }
}
```

#### 4.3.2 订单创建（幂等性保证）
```java
/**
 * 订单服务
 */
@Service
public class OrderService {
    
    @Autowired
    private OrderMapper orderMapper;
    
    @Autowired
    private OrderItemMapper orderItemMapper;
    
    @Autowired
    private InventoryService inventoryService;
    
    @Autowired
    private MarketingService marketingService;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    /**
     * 创建订单
     */
    @Transactional(rollbackFor = Exception.class)
    public OrderCreateResultVO createOrder(OrderCreateDTO dto) {
        // 1. 幂等性检查（防重复提交）
        String idempotentKey = "order:create:" + dto.getUserId();
        Boolean success = redisTemplate.opsForValue()
            .setIfAbsent(idempotentKey, "1", 5, TimeUnit.SECONDS);
        
        if (!success) {
            throw new BusinessException("请勿重复提交订单");
        }
        
        try {
            // 2. 校验购物车/商品
            List<CartItemVO> items = validateCartItems(dto);
            
            // 3. 锁定库存
            for (CartItemVO item : items) {
                boolean locked = inventoryService.lockStock(
                    item.getSkuId(), 
                    item.getQuantity(), 
                    null // 临时订单号
                );
                if (!locked) {
                    throw new BusinessException("商品库存不足：" + item.getProductName());
                }
            }
            
            // 4. 计算价格
            PriceCalculationVO price = calculatePrice(dto, items);
            
            // 5. 创建订单
            Order order = buildOrder(dto, items, price);
            orderMapper.insert(order);
            
            // 6. 创建订单明细
            List<OrderItem> orderItems = buildOrderItems(order.getId(), items);
            orderItemMapper.batchInsert(orderItems);
            
            // 7. 更新库存锁定的订单号
            inventoryService.bindOrderNo(items, order.getOrderNo());
            
            // 8. 锁定优惠券
            if (dto.getCouponId() != null) {
                marketingService.lockCoupon(dto.getUserId(), dto.getCouponId(), order.getId());
            }
            
            // 9. 返回结果
            return OrderCreateResultVO.builder()
                .orderId(order.getId())
                .orderNo(order.getOrderNo())
                .payAmount(order.getPayAmount())
                .expireTime(order.getAutoCloseTime())
                .build();
                
        } finally {
            // 删除幂等性key
            redisTemplate.delete(idempotentKey);
        }
    }
}
```

#### 4.3.3 支付回调处理（幂等性）
```java
/**
 * 支付回调服务
 */
@Service
public class PaymentCallbackService {
    
    @Autowired
    private PaymentRecordMapper paymentMapper;
    
    @Autowired
    private OrderService orderService;
    
    @Autowired
    private InventoryService inventoryService;
    
    /**
     * 处理支付成功回调
     */
    @Transactional(rollbackFor = Exception.class)
    public void handlePaySuccess(WechatPayCallbackDTO callback) {
        // 1. 验签
        if (!verifySignature(callback)) {
            throw new BusinessException("签名验证失败");
        }
        
        // 2. 幂等性检查（根据微信transaction_id）
        String transactionId = callback.getTransactionId();
        PaymentRecord record = paymentMapper.selectByTransactionId(transactionId);
        
        if (record != null && record.getPayStatus() == 20) {
            // 已经处理过，直接返回成功
            return;
        }
        
        // 3. 更新支付记录
        if (record == null) {
            record = paymentMapper.selectByOrderNo(callback.getOutTradeNo());
        }
        
        record.setTransactionId(transactionId);
        record.setPayStatus(20); // 已支付
        record.setPaidAt(LocalDateTime.now());
        record.setCallbackPayload(JSON.toJSONString(callback));
        paymentMapper.updateById(record);
        
        // 4. 更新订单状态
        orderService.updateOrderPaid(record.getOrderId());
        
        // 5. 扣减库存
        inventoryService.deductStock(record.getOrderNo());
        
        // 6. 发放积分、核销优惠券等异步处理
        asyncProcessAfterPaySuccess(record.getOrderId());
    }
}
```

### 4.4 售后流程

```
用户                   售后服务          订单服务          支付服务          库存服务
 │                       │                 │                │                │
 ├─ 申请售后 ───────────>│                 │                │                │
 │                       ├─ 校验订单状态 ─>│                │                │
 │                       │<── 校验通过 ────┤                │                │
 │                       ├─ 创建售后单     │                │                │
 │                       │  (状态:待审核)  │                │                │
 │<── 申请成功 ───────────┤                 │                │                │
 │                       │                 │                │                │
 │                       ├─ 运营审核       │                │                │
 │                       │  - 通过         │                │                │
 │                       │  (状态:待退货)  │                │                │
 │<── 审核通过 ───────────┤                 │                │                │
 │                       │                 │                │                │
 ├─ 填写退货物流 ────────>│                 │                │                │
 │                       ├─ 更新售后单     │                │                │
 │                       │  (状态:待收货)  │                │                │
 │<── 提交成功 ───────────┤                 │                │                │
 │                       │                 │                │                │
 │                       ├─ 商家收货确认   │                │                │
 │                       │  (状态:退款中)  │                │                │
 │                       ├─ 发起退款 ──────────────────────>│                │
 │                       │<── 退款成功 ────┤                │                │
 │                       ├─ 更新售后单     │                │                │
 │                       │  (状态:已退款)  │                │                │
 │                       ├─ 更新订单状态   │                │                │
 │                       ├─ 回退库存 ───────────────────────────────────────>│
 │<── 退款成功 ───────────┤                 │                │                │
```

### 4.5 库存回退流程

```java
/**
 * 售后退款 - 库存回退
 */
@Service
public class AfterSaleService {
    
    /**
     * 售后退款成功
     */
    @Transactional(rollbackFor = Exception.class)
    public void refundSuccess(Long afterSaleId) {
        AfterSaleOrder afterSale = afterSaleMapper.selectById(afterSaleId);
        
        // 1. 更新售后单状态
        afterSale.setStatus(50); // 已退款
        afterSale.setRefundTime(LocalDateTime.now());
        afterSaleMapper.updateById(afterSale);
        
        // 2. 更新订单明细退款状态
        orderItemMapper.updateRefundStatus(afterSale.getOrderItemId(), 2);
        
        // 3. 回退库存
        inventoryService.returnStock(
            afterSale.getOrderItemId(),
            afterSale.getApprovedAmount()
        );
        
        // 4. 回退优惠券
        if (afterSale.getCouponUsed()) {
            marketingService.returnCoupon(afterSale.getUserId(), afterSale.getCouponId());
        }
        
        // 5. 扣减积分
        if (afterSale.getPointsUsed() > 0) {
            marketingService.deductPoints(afterSale.getUserId(), afterSale.getPointsUsed());
        }
    }
}
```

---

## 5. 数据库设计说明

### 5.1 数据库设计参考
数据库设计详见 `databse.md` 文档，包含完整的表结构设计、索引设计、约束设计。

### 5.2 核心表设计要点

#### 5.2.1 订单相关表
```sql
-- 订单主表
CREATE TABLE `orders` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `order_no` VARCHAR(64) NOT NULL COMMENT '订单号',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `status` TINYINT NOT NULL COMMENT '订单状态',
  `pay_status` TINYINT NOT NULL COMMENT '支付状态',
  `total_amount` DECIMAL(10,2) NOT NULL COMMENT '商品总金额',
  `pay_amount` DECIMAL(10,2) NOT NULL COMMENT '实付金额',
  `consignee_info` JSON NOT NULL COMMENT '收货地址快照',
  `price_snapshot` JSON NOT NULL COMMENT '价格快照',
  `created_at` DATETIME NOT NULL,
  `updated_at` DATETIME NOT NULL,
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_user_status_created` (`user_id`, `status`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';
```

#### 5.2.2 库存锁定表
```sql
-- 库存锁定记录表
CREATE TABLE `stock_reservations` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `order_no` VARCHAR(64) NOT NULL COMMENT '订单号',
  `sku_id` BIGINT NOT NULL COMMENT 'SKU ID',
  `locked_qty` INT NOT NULL COMMENT '锁定数量',
  `status` TINYINT NOT NULL COMMENT '状态:10已锁定,20已释放,30已扣减',
  `expire_at` DATETIME NOT NULL COMMENT '锁定失效时间',
  `released_at` DATETIME COMMENT '释放时间',
  `deducted_at` DATETIME COMMENT '扣减时间',
  `created_at` DATETIME NOT NULL,
  KEY `idx_order_sku` (`order_no`, `sku_id`),
  KEY `idx_status_expire` (`status`, `expire_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存锁定记录表';
```

---

## 6. 接口设计规范

### 6.1 RESTful API规范
- **GET**：查询操作
- **POST**：创建操作
- **PUT**：更新操作
- **DELETE**：删除操作

### 6.2 响应格式

#### 统一响应对象
```java
/**
 * 统一响应对象
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApiResponse<T> {
    private Integer code;
    private String message;
    private T data;
    
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(0, "success", data);
    }
    
    public static <T> ApiResponse<T> error(Integer code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}
```

#### 分页响应对象
```java
/**
 * 分页响应对象
 */
@Data
public class PageResult<T> {
    private List<T> list;
    private Long total;
    private Integer page;
    private Integer pageSize;
    private Boolean hasMore;
}
```

### 6.3 全局异常处理

```java
/**
 * 全局异常处理器
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    /**
     * 业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public ApiResponse<Void> handleBusinessException(BusinessException e) {
        return ApiResponse.error(e.getCode(), e.getMessage());
    }
    
    /**
     * 参数校验异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Void> handleValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.joining(", "));
        return ApiResponse.error(10001, message);
    }
    
    /**
     * 系统异常
     */
    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return ApiResponse.error(500, "系统繁忙，请稍后重试");
    }
}
```

### 6.4 接口文档
详见 `frontapi.md` 文档，包含完整的接口定义、请求参数、响应格式。

---

## 7. 安全设计

### 7.1 认证与鉴权

#### 7.1.1 JWT认证
```java
/**
 * JWT工具类
 */
@Component
public class JwtTokenUtil {
    
    @Value("${jwt.secret}")
    private String secret;
    
    @Value("${jwt.expiration}")
    private Long expiration;
    
    /**
     * 生成Token
     */
    public String generateToken(Long userId, String openid) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("openid", openid);
        
        return Jwts.builder()
            .setClaims(claims)
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + expiration * 1000))
            .signWith(SignatureAlgorithm.HS512, secret)
            .compact();
    }
    
    /**
     * 解析Token
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
            .setSigningKey(secret)
            .parseClaimsJws(token)
            .getBody();
    }
    
    /**
     * 验证Token是否有效
     */
    public boolean validateToken(String token) {
        try {
            Claims claims = parseToken(token);
            return !claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return false;
        }
    }
}
```

#### 7.1.2 权限拦截器
```java
/**
 * 认证拦截器
 */
@Component
public class AuthenticationInterceptor implements HandlerInterceptor {
    
    @Autowired
    private JwtTokenUtil jwtTokenUtil;
    
    @Override
    public boolean preHandle(HttpServletRequest request, 
                            HttpServletResponse response, 
                            Object handler) throws Exception {
        // 从Header获取Token
        String token = request.getHeader("Authorization");
        if (StringUtils.isBlank(token) || !token.startsWith("Bearer ")) {
            throw new AuthenticationException("未登录");
        }
        
        token = token.substring(7);
        
        // 验证Token
        if (!jwtTokenUtil.validateToken(token)) {
            throw new AuthenticationException("Token已过期");
        }
        
        // 解析用户信息并存入ThreadLocal
        Claims claims = jwtTokenUtil.parseToken(token);
        Long userId = claims.get("userId", Long.class);
        String openid = claims.get("openid", String.class);
        
        UserContext.setUserId(userId);
        UserContext.setOpenid(openid);
        
        return true;
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, 
                                HttpServletResponse response, 
                                Object handler, Exception ex) {
        // 清理ThreadLocal
        UserContext.clear();
    }
}
```

### 7.2 数据安全

#### 7.2.1 敏感数据加密
```java
/**
 * AES加密工具类
 */
@Component
public class AesEncryptUtil {
    
    @Value("${encrypt.aes.key}")
    private String aesKey;
    
    /**
     * 加密
     */
    public String encrypt(String data) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            byte[] keyBytes = aesKey.getBytes(StandardCharsets.UTF_8);
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(keyBytes, 0, 16);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
            byte[] encrypted = cipher.doFinal(data.getBytes());
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("加密失败", e);
        }
    }
    
    /**
     * 解密
     */
    public String decrypt(String encryptedData) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            byte[] keyBytes = aesKey.getBytes(StandardCharsets.UTF_8);
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(keyBytes, 0, 16);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encryptedData));
            return new String(decrypted);
        } catch (Exception e) {
            throw new RuntimeException("解密失败", e);
        }
    }
}
```

#### 7.2.2 手机号脱敏
```java
/**
 * 用户Service
 */
@Service
public class UserService {
    
    @Autowired
    private AesEncryptUtil aesEncryptUtil;
    
    /**
     * 保存用户手机号
     */
    public void savePhone(Long userId, String phone) {
        User user = new User();
        user.setId(userId);
        // 加密存储
        user.setPhoneCipher(aesEncryptUtil.encrypt(phone));
        // 脱敏存储
        user.setPhoneMask(phone.substring(0, 3) + "****" + phone.substring(7));
        
        userMapper.updateById(user);
    }
    
    /**
     * 获取用户真实手机号
     */
    public String getPhone(Long userId) {
        User user = userMapper.selectById(userId);
        if (StringUtils.isBlank(user.getPhoneCipher())) {
            return null;
        }
        // 解密返回
        return aesEncryptUtil.decrypt(user.getPhoneCipher());
    }
}
```

### 7.3 防刷防作弊

#### 7.3.1 接口限流
```java
/**
 * 限流注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    int value() default 100; // QPS限制
    int timeout() default 1; // 超时时间（秒）
}

/**
 * 限流切面
 */
@Aspect
@Component
public class RateLimitAspect {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint point, RateLimit rateLimit) throws Throwable {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder
            .getRequestAttributes()).getRequest();
        
        String key = "rate_limit:" + request.getRequestURI() + ":" + UserContext.getUserId();
        
        Long count = redisTemplate.opsForValue().increment(key, 1);
        
        if (count == 1) {
            redisTemplate.expire(key, rateLimit.timeout(), TimeUnit.SECONDS);
        }
        
        if (count > rateLimit.value()) {
            throw new BusinessException("访问过于频繁，请稍后再试");
        }
        
        return point.proceed();
    }
}
```

#### 7.3.2 防重复提交
```java
/**
 * 防重复提交注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PreventDuplicate {
    int timeout() default 5; // 超时时间（秒）
}

/**
 * 防重复提交切面
 */
@Aspect
@Component
public class PreventDuplicateAspect {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Around("@annotation(preventDuplicate)")
    public Object around(ProceedingJoinPoint point, PreventDuplicate preventDuplicate) 
        throws Throwable {
        
        String key = "prevent_duplicate:" + point.getSignature().toLongString() 
            + ":" + UserContext.getUserId();
        
        Boolean success = redisTemplate.opsForValue()
            .setIfAbsent(key, "1", preventDuplicate.timeout(), TimeUnit.SECONDS);
        
        if (!success) {
            throw new BusinessException("请勿重复提交");
        }
        
        return point.proceed();
    }
}
```

### 7.4 SQL注入防护
- 使用 MyBatis-Plus 参数化查询
- 禁止拼接 SQL 语句
- 使用 `#{}` 占位符而非 `${}`

---

## 8. 性能优化方案

### 8.1 缓存策略

#### 8.1.1 Redis缓存设计
```java
/**
 * 商品缓存Service
 */
@Service
public class ProductCacheService {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private ProductMapper productMapper;
    
    private static final String PRODUCT_DETAIL_KEY = "product:detail:";
    private static final String PRODUCT_SKU_KEY = "product:sku:";
    
    /**
     * 获取商品详情（缓存）
     */
    public Product getProductDetail(Long productId) {
        String key = PRODUCT_DETAIL_KEY + productId;
        
        // 1. 查询缓存
        Product product = (Product) redisTemplate.opsForValue().get(key);
        
        if (product != null) {
            return product;
        }
        
        // 2. 查询数据库
        product = productMapper.selectDetailById(productId);
        
        if (product == null) {
            return null;
        }
        
        // 3. 写入缓存
        redisTemplate.opsForValue().set(key, product, 1, TimeUnit.HOURS);
        
        return product;
    }
    
    /**
     * 删除商品缓存
     */
    public void deleteProductCache(Long productId) {
        redisTemplate.delete(PRODUCT_DETAIL_KEY + productId);
        redisTemplate.delete(PRODUCT_SKU_KEY + productId);
    }
}
```

#### 8.1.2 多级缓存
```java
/**
 * 多级缓存（本地缓存 + Redis）
 */
@Service
public class MultiLevelCacheService {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    // 本地缓存（Caffeine）
    private Cache<String, Object> localCache = Caffeine.newBuilder()
        .maximumSize(1000)
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .build();
    
    /**
     * 获取数据（二级缓存）
     */
    public Object get(String key) {
        // 1. 本地缓存
        Object value = localCache.getIfPresent(key);
        if (value != null) {
            return value;
        }
        
        // 2. Redis缓存
        value = redisTemplate.opsForValue().get(key);
        if (value != null) {
            localCache.put(key, value);
            return value;
        }
        
        return null;
    }
    
    /**
     * 写入数据（二级缓存）
     */
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
        localCache.put(key, value);
    }
}
```

### 8.2 数据库优化

#### 8.2.1 索引优化
```sql
-- 订单查询索引
CREATE INDEX idx_orders_user_status_created 
ON orders(user_id, status, created_at);

-- 商品搜索索引
CREATE INDEX idx_products_category_status_sort 
ON products(category_id, status, sort_order);

-- 库存锁定查询索引
CREATE INDEX idx_stock_reservations_status_expire 
ON stock_reservations(status, expire_at);
```

#### 8.2.2 分页优化
```java
/**
 * 分页查询优化（避免深分页）
 */
@Service
public class OrderQueryService {
    
    /**
     * 基于游标的分页
     */
    public PageResult<OrderVO> queryOrders(Long userId, Long lastOrderId, Integer pageSize) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getUserId, userId)
               .eq(Order::getStatus, 20)
               .orderByDesc(Order::getId);
        
        // 游标分页（避免OFFSET）
        if (lastOrderId != null) {
            wrapper.lt(Order::getId, lastOrderId);
        }
        
        wrapper.last("LIMIT " + pageSize);
        
        List<Order> orders = orderMapper.selectList(wrapper);
        
        return PageResult.of(orders, pageSize);
    }
}
```

### 8.3 异步处理

#### 8.3.1 异步任务
```java
/**
 * 异步任务配置
 */
@Configuration
@EnableAsync
public class AsyncConfig {
    
    @Bean("asyncExecutor")
    public Executor asyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(1000);
        executor.setThreadNamePrefix("async-");
        executor.initialize();
        return executor;
    }
}

/**
 * 异步任务Service
 */
@Service
public class AsyncTaskService {
    
    @Async("asyncExecutor")
    public void sendOrderNotification(Long orderId) {
        // 发送订单通知（异步执行）
    }
    
    @Async("asyncExecutor")
    public void updateUserPoints(Long userId, Integer points) {
        // 更新用户积分（异步执行）
    }
}
```

#### 8.3.2 消息队列
```java
/**
 * RabbitMQ配置
 */
@Configuration
public class RabbitMqConfig {
    
    public static final String ORDER_QUEUE = "order.queue";
    public static final String ORDER_EXCHANGE = "order.exchange";
    
    @Bean
    public Queue orderQueue() {
        return new Queue(ORDER_QUEUE, true);
    }
    
    @Bean
    public DirectExchange orderExchange() {
        return new DirectExchange(ORDER_EXCHANGE);
    }
    
    @Bean
    public Binding orderBinding() {
        return BindingBuilder.bind(orderQueue())
            .to(orderExchange())
            .with("order.created");
    }
}

/**
 * 消息生产者
 */
@Service
public class OrderMessageProducer {
    
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    public void sendOrderCreatedMessage(Long orderId) {
        rabbitTemplate.convertAndSend(
            RabbitMqConfig.ORDER_EXCHANGE,
            "order.created",
            orderId
        );
    }
}

/**
 * 消息消费者
 */
@Component
@RabbitListener(queues = RabbitMqConfig.ORDER_QUEUE)
public class OrderMessageConsumer {
    
    @Autowired
    private AsyncTaskService asyncTaskService;
    
    @RabbitHandler
    public void handleOrderCreated(Long orderId) {
        // 异步处理订单创建后的任务
        asyncTaskService.sendOrderNotification(orderId);
        asyncTaskService.awardPoints(orderId);
    }
}
```

---

## 9. 项目结构

### 9.1 Maven多模块项目
```
shiyu-home
├── shiyu-common              # 公共模块
│   ├── common-core           # 核心工具类
│   ├── common-redis          # Redis工具
│   ├── common-log            # 日志工具
│   └── common-security       # 安全工具
├── shiyu-api                 # API接口定义
├── shiyu-user                # 用户服务
│   ├── user-api              # 用户API
│   └── user-service          # 用户服务实现
├── shiyu-product             # 商品服务
│   ├── product-api           # 商品API
│   └── product-service       # 商品服务实现
├── shiyu-order               # 订单服务
│   ├── order-api             # 订单API
│   └── order-service         # 订单服务实现
├── shiyu-payment             # 支付服务
├── shiyu-marketing           # 营销服务
├── shiyu-aftersale           # 售后服务
└── shiyu-admin               # 后台管理服务
```

### 9.2 单模块结构（简化版）
```
shiyu-home-server
├── src/main/java/com/shiyu/home
│   ├── ShiyuHomeApplication.java    # 启动类
│   ├── config/                      # 配置类
│   │   ├── MybatisPlusConfig.java   # MyBatis配置
│   │   ├── RedisConfig.java         # Redis配置
│   │   ├── SwaggerConfig.java       # Swagger配置
│   │   └── WebMvcConfig.java        # Web配置
│   ├── controller/                  # 控制器
│   │   ├── user/                    # 用户相关
│   │   ├── product/                 # 商品相关
│   │   ├── order/                   # 订单相关
│   │   └── admin/                   # 后台管理
│   ├── service/                     # 服务层
│   │   ├── user/
│   │   ├── product/
│   │   └── order/
│   ├── mapper/                      # 数据访问层
│   ├── entity/                      # 实体类
│   ├── dto/                         # 数据传输对象
│   ├── vo/                          # 视图对象
│   ├── enums/                       # 枚举类
│   ├── exception/                   # 异常类
│   ├── utils/                       # 工具类
│   └── aspect/                      # 切面类
├── src/main/resources
│   ├── application.yml              # 应用配置
│   ├── application-dev.yml          # 开发环境配置
│   ├── application-prod.yml         # 生产环境配置
│   └── mapper/                      # MyBatis XML
└── pom.xml                          # Maven配置
```

---

## 10. 配置管理

### 10.1 application.yml
```yaml
spring:
  profiles:
    active: dev
  application:
    name: shiyu-home-server
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 20MB

# 开发环境配置
---
spring:
  profiles: dev
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/shiyu_ware?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    password: root
    type: com.alibaba.druid.pool.DruidDataSource
    druid:
      initial-size: 5
      min-idle: 5
      max-active: 20
  redis:
    host: localhost
    port: 6379
    database: 0
    timeout: 3000
    lettuce:
      pool:
        max-active: 20
        max-wait: -1
        max-idle: 10
        min-idle: 5

server:
  port: 8080

mybatis-plus:
  mapper-locations: classpath:mapper/**/*.xml
  type-aliases-package: com.shiyu.home.entity
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl

jwt:
  secret: shiyu-home-jwt-secret-key-2024
  expiration: 604800  # 7天

encrypt:
  aes:
    key: shiyu-home-aes-key-32b

wechat:
  miniapp:
    app-id: your-app-id
    app-secret: your-app-secret
  pay:
    app-id: your-app-id
    mch-id: your-mch-id
    api-key: your-api-key
    notify-url: https://api.shiyuhome.com/api/v1/payment/callback

tencent:
  cos:
    secret-id: your-secret-id
    secret-key: your-secret-key
    bucket: your-bucket-name
    region: ap-shanghai

aliyun:
  oss:
    access-key-id: your-access-key-id
    access-key-secret: your-access-key-secret
    endpoint: oss-cn-shanghai.aliyuncs.com
    bucket: your-bucket-name


logging:
  level:
    com.shiyu.home: debug
  file:
    name: logs/shiyu-home.log
```

---

## 11. 测试方案

### 11.1 单元测试
```java
/**
 * 订单服务单元测试
 */
@SpringBootTest
@RunWith(SpringRunner.class)
public class OrderServiceTest {
    
    @Autowired
    private OrderService orderService;
    
    @Test
    public void testCreateOrder() {
        OrderCreateDTO dto = new OrderCreateDTO();
        dto.setUserId(1L);
        dto.setAddressId(1L);
        
        OrderCreateResultVO result = orderService.createOrder(dto);
        
        assertNotNull(result.getOrderId());
        assertNotNull(result.getOrderNo());
    }
}
```

### 11.2 接口测试
```java
/**
 * 订单接口测试
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class OrderControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    public void testGetOrderList() throws Exception {
        mockMvc.perform(get("/api/v1/orders")
                .header("Authorization", "Bearer " + getToken())
                .param("status", "pending")
                .param("page", "1")
                .param("page_size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.list").isArray());
    }
}
```

---

## 12. 部署方案

### 12.1 Docker部署
```dockerfile
# Dockerfile
FROM openjdk:8-jdk-alpine

LABEL maintainer="shiyu-home"

WORKDIR /app

COPY target/shiyu-home-server.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-Xms512m", "-Xmx1024m", "-jar", "app.jar"]
```

```yaml
# docker-compose.yml
version: '3.8'

services:
  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - JAVA_OPTS=-Xms512m -Xmx1024m
    depends_on:
      - mysql
      - redis
    networks:
      - shiyu-network
  
  mysql:
    image: mysql:8.0
    ports:
      - "3306:3306"
    environment:
      - MYSQL_ROOT_PASSWORD=root
      - MYSQL_DATABASE=shiyu_ware
    volumes:
      - mysql-data:/var/lib/mysql
    networks:
      - shiyu-network
  
  redis:
    image: redis:6-alpine
    ports:
      - "6379:6379"
    volumes:
      - redis-data:/data
    networks:
      - shiyu-network

volumes:
  mysql-data:
  redis-data:

networks:
  shiyu-network:
    driver: bridge
```

### 12.2 CloudBase部署
参见 CloudBase 官方文档，使用 CloudRun 部署 Java 应用。

---

## 13. 监控与运维

### 13.1 日志管理
- 使用 Logback 记录日志
- 日志级别：ERROR、WARN、INFO、DEBUG
- 日志文件按天滚动，保留30天
- 错误日志发送告警

### 13.2 性能监控
- 使用 Prometheus + Grafana 监控系统指标
- 监控项：CPU、内存、QPS、响应时间、错误率
- 设置告警阈值

### 13.3 健康检查
```java
/**
 * 健康检查接口
 */
@RestController
@RequestMapping("/health")
public class HealthController {
    
    @GetMapping
    public String health() {
        return "OK";
    }
}
```

---

## 14. 附录

### 14.1 技术栈清单

| 技术分类 | 技术选型 | 版本 |
| --- | --- | --- |
| 语言 | Java | 1.8 |
| 框架 | Spring Boot | 2.7.x |
| ORM | MyBatis-Plus | 3.5.x |
| 数据库 | MySQL | 8.0 |
| 缓存 | Redis | 6.0+ |
| 消息队列 | RabbitMQ | 3.8+ |
| 构建工具 | Maven | 3.6+ |
| 容器化 | Docker | 20.x |
| API文档 | Swagger2 | 2.9.2 |

### 14.2 开发规范

#### 14.2.1 基础规范
- 代码规范：阿里巴巴Java开发手册
- 命名规范：驼峰命名法
- 提交规范：使用Git提交模板

#### 14.2.2 注释规范

##### 类/接口注释
- 类级注释：所有类、接口、枚举必须包含 Javadoc，说明职责与用途
- 首句规范：以简洁动词开头（如"负责"、"提供"、"处理"），不以"该类/本类"等冗余表述开头
- 示例：
```java
/**
 * 订单服务。
 * 负责订单创建、状态流转、库存协调等核心交易逻辑。
 */
@Service
public class OrderService { }
```

##### 方法注释
- **强制要求**：
  - 所有 `public` 方法必须有 Javadoc
  - 所有方法参数必须标注 `@param`，说明参数含义
  - 所有非 `void` 返回值必须标注 `@return`，说明返回内容
  - 抛出 checked 异常必须标注 `@throws`
- **首句规范**：以动词开头（如"创建"、"查询"、"执行"），简洁明了
- 示例：
```java
/**
 * 创建订单并锁定库存。
 *
 * @param userId    用户ID
 * @param request   订单创建请求
 * @param addressId 收货地址ID
 * @return 订单创建结果
 * @throws BusinessException 库存不足或商品下架时抛出
 */
public OrderCreateResultVO createOrder(Long userId, OrderCreateRequest request, Long addressId) { }
```

##### 字段注释
- 实体类字段：必须标注业务含义（金额单位、状态枚举、是否可空）
- 配置类字段：说明配置项含义与默认值
- 常量字段：说明常量用途

##### 注解注释
- 自定义注解必须包含：
  - 用途说明
  - 适用范围（类/方法）
  - 默认行为
  - 涉及的请求头名称（如 `Authorization`、`X-Signature`）
- 示例：
```java
/**
 * 接口幂等注解。
 * 用于防止重复提交，需客户端传递 X-Idempotency-Key 请求头。
 * 适用范围：方法级别。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent { }
```

#### 14.2.3 功能开发注解说明规范
- 自定义注解必须包含用途说明、适用范围（类/方法）与默认行为
- 使用注解时需在控制器方法注明"为何需要该注解"（如鉴权、签名、幂等）
- 注解涉及请求头时，必须在注释中明确 Header 名称（如 `Authorization`、`X-Signature`、`X-Idempotency-Key`）

#### 14.2.4 实体类定义说明规范
- 实体类（Entity）必须补充类注释，说明对应表与核心职责
- 字段必须标注业务含义（金额单位、状态枚举、是否可空）
- 实体类仅承载持久化字段，不混入接口参数校验与视图展示逻辑
- DTO/VO 与 Entity 分层明确，禁止直接复用 Entity 作为对外请求响应对象

### 14.3 相关文档
- 需求文档：`prd.md`
- 设计文档：`design.md`
- 数据库设计：`databse.md`
- 接口文档：`frontapi.md`

---

## 15. 更新记录

| 版本 | 日期 | 更新内容 | 更新人 |
| --- | --- | --- | --- |
| 1.0.0 | 2024-01-15 | 初始版本 | AI Assistant |

---

**文档维护方**：诗语家居技术团队  
**最后更新时间**：2024-01-15
