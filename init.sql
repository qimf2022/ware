-- 诗语家居数据库初始化脚本（V3）
-- 适用：CloudBase MySQL / MySQL 8+
-- 说明：
-- 1. 默认数据库名为 `shiyu_ware`，如需调整请修改下方 CREATE DATABASE / USE 语句。
-- 2. 为兼容 CloudBase MySQL 的权限与迭代式发布，本脚本默认不建立外键，改用索引 + 应用层约束保证一致性。
-- 3. 所有业务表均保留 `_openid` 字段，用于 CloudBase 按用户权限隔离。

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE DATABASE IF NOT EXISTS `shiyu_ware`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE `shiyu_ware`;

DROP TABLE IF EXISTS `operation_logs`;
DROP TABLE IF EXISTS `admin_user_roles`;
DROP TABLE IF EXISTS `admin_roles`;
DROP TABLE IF EXISTS `admin_users`;
DROP TABLE IF EXISTS `user_coupons`;
DROP TABLE IF EXISTS `coupon_scope_relations`;
DROP TABLE IF EXISTS `coupons`;
DROP TABLE IF EXISTS `after_sale_logs`;
DROP TABLE IF EXISTS `after_sale_return_shipments`;
DROP TABLE IF EXISTS `after_sale_orders`;
DROP TABLE IF EXISTS `order_logs`;
DROP TABLE IF EXISTS `shipment_tracks`;
DROP TABLE IF EXISTS `shipments`;
DROP TABLE IF EXISTS `inventory_logs`;
DROP TABLE IF EXISTS `stock_reservations`;
DROP TABLE IF EXISTS `payment_records`;
DROP TABLE IF EXISTS `order_items`;
DROP TABLE IF EXISTS `orders`;
DROP TABLE IF EXISTS `cart_items`;
DROP TABLE IF EXISTS `search_suggest_keywords`;
DROP TABLE IF EXISTS `search_hot_keywords`;
DROP TABLE IF EXISTS `product_relations`;
DROP TABLE IF EXISTS `recommend_items`;
DROP TABLE IF EXISTS `recommend_positions`;
DROP TABLE IF EXISTS `topic_products`;
DROP TABLE IF EXISTS `topics`;
DROP TABLE IF EXISTS `banners`;
DROP TABLE IF EXISTS `product_filter_values`;
DROP TABLE IF EXISTS `category_filter_attributes`;
DROP TABLE IF EXISTS `filter_attributes`;
DROP TABLE IF EXISTS `sku_spec_relations`;
DROP TABLE IF EXISTS `product_skus`;
DROP TABLE IF EXISTS `product_spec_values`;
DROP TABLE IF EXISTS `product_spec_groups`;
DROP TABLE IF EXISTS `product_detail_attrs`;
DROP TABLE IF EXISTS `product_media`;
DROP TABLE IF EXISTS `products`;
DROP TABLE IF EXISTS `categories`;
DROP TABLE IF EXISTS `stored_value_logs`;
DROP TABLE IF EXISTS `stored_value_cards`;
DROP TABLE IF EXISTS `user_points_logs`;
DROP TABLE IF EXISTS `user_points_accounts`;
DROP TABLE IF EXISTS `search_histories`;
DROP TABLE IF EXISTS `user_footprints`;
DROP TABLE IF EXISTS `user_favorites`;
DROP TABLE IF EXISTS `user_addresses`;
DROP TABLE IF EXISTS `users`;

-- -----------------------------------------------------
-- 4. 用户与会员域
-- -----------------------------------------------------

CREATE TABLE `users` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `_openid` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'CloudBase 用户标识',
  `openid` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '微信 OpenID',
  `unionid` VARCHAR(64) DEFAULT NULL COMMENT '微信 UnionID',
  `nickname` VARCHAR(64) DEFAULT '诗语访客' COMMENT '昵称',
  `avatar_url` VARCHAR(255) DEFAULT NULL COMMENT '头像 URL',
  `phone_cipher` VARCHAR(255) DEFAULT NULL COMMENT '加密手机号',
  `phone_mask` VARCHAR(20) DEFAULT NULL COMMENT '脱敏手机号',
  `gender` TINYINT UNSIGNED DEFAULT 0 COMMENT '0未知 1男 2女',
  `member_level_code` VARCHAR(30) DEFAULT NULL COMMENT '会员等级编码',
  `status` TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '1正常 0禁用',
  `register_source` VARCHAR(30) NOT NULL DEFAULT 'wx_miniapp' COMMENT '注册来源',
  `last_login_at` DATETIME DEFAULT NULL COMMENT '最后登录时间',
  `last_active_at` DATETIME DEFAULT NULL COMMENT '最后活跃时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_users_openid` (`openid`),
  KEY `idx_users_status` (`status`),
  KEY `idx_users_member_level` (`member_level_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户基础表';

CREATE TABLE `user_addresses` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `_openid` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'CloudBase 用户标识',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户 ID',
  `consignee` VARCHAR(50) NOT NULL COMMENT '收件人',
  `phone_cipher` VARCHAR(255) NOT NULL COMMENT '加密手机号',
  `phone_mask` VARCHAR(20) NOT NULL COMMENT '脱敏手机号',
  `province` VARCHAR(50) NOT NULL COMMENT '省',
  `city` VARCHAR(50) NOT NULL COMMENT '市',
  `district` VARCHAR(50) NOT NULL COMMENT '区',
  `street` VARCHAR(50) DEFAULT NULL COMMENT '街道',
  `detail_address` VARCHAR(200) NOT NULL COMMENT '详细地址',
  `postal_code` VARCHAR(20) DEFAULT NULL COMMENT '邮编',
  `tag` VARCHAR(20) DEFAULT NULL COMMENT '地址标签',
  `longitude` DECIMAL(10,6) DEFAULT NULL COMMENT '经度',
  `latitude` DECIMAL(10,6) DEFAULT NULL COMMENT '纬度',
  `is_default` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '1默认 0非默认',
  `deleted` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '0未删 1已删',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_addresses_user` (`user_id`),
  KEY `idx_user_addresses_default` (`user_id`, `is_default`),
  KEY `idx_user_addresses_deleted` (`user_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户收货地址表';

CREATE TABLE `user_favorites` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `_openid` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'CloudBase 用户标识',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户 ID',
  `product_id` BIGINT UNSIGNED NOT NULL COMMENT '商品 SPU ID',
  `deleted` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '0有效 1取消收藏',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_favorites_user_product` (`user_id`, `product_id`),
  KEY `idx_user_favorites_deleted` (`user_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户收藏表';

CREATE TABLE `user_footprints` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `_openid` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'CloudBase 用户标识',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户 ID',
  `product_id` BIGINT UNSIGNED NOT NULL COMMENT '商品 SPU ID',
  `source_page` VARCHAR(50) DEFAULT NULL COMMENT '来源页面',
  `viewed_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '浏览时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_footprints_user_viewed` (`user_id`, `viewed_at`),
  KEY `idx_user_footprints_product` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='浏览足迹表';

CREATE TABLE `search_histories` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `_openid` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'CloudBase 用户标识',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户 ID',
  `keyword` VARCHAR(100) NOT NULL COMMENT '搜索词',
  `search_count` INT UNSIGNED NOT NULL DEFAULT 1 COMMENT '搜索次数',
  `last_search_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最近搜索时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_search_histories_user_keyword` (`user_id`, `keyword`),
  KEY `idx_search_histories_last_search` (`user_id`, `last_search_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='搜索历史表';

CREATE TABLE `user_points_accounts` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `_openid` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'CloudBase 用户标识',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户 ID',
  `total_points` INT NOT NULL DEFAULT 0 COMMENT '累计积分',
  `available_points` INT NOT NULL DEFAULT 0 COMMENT '可用积分',
  `frozen_points` INT NOT NULL DEFAULT 0 COMMENT '冻结积分',
  `expire_points` INT NOT NULL DEFAULT 0 COMMENT '已过期待清理积分',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_points_accounts_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='积分账户表';

CREATE TABLE `user_points_logs` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `_openid` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'CloudBase 用户标识',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户 ID',
  `change_type` TINYINT UNSIGNED NOT NULL COMMENT '1获取 2抵扣 3退款返还 4过期 5调整',
  `points` INT NOT NULL COMMENT '正负积分值',
  `business_type` VARCHAR(30) NOT NULL COMMENT '业务类型',
  `business_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '关联业务 ID',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_points_logs_user_created` (`user_id`, `created_at`),
  KEY `idx_user_points_logs_business` (`business_type`, `business_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='积分流水表';

CREATE TABLE `stored_value_cards` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `_openid` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'CloudBase 用户标识',
  `card_no` VARCHAR(64) NOT NULL COMMENT '卡号',
  `card_type` TINYINT UNSIGNED NOT NULL COMMENT '1储值卡 2礼品卡',
  `user_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '持卡用户 ID',
  `card_name` VARCHAR(100) DEFAULT NULL COMMENT '卡名称',
  `balance` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '当前余额',
  `total_amount` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '初始总额',
  `status` TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '1未激活 2可用 3冻结 4作废 5已过期',
  `expire_time` DATETIME DEFAULT NULL COMMENT '过期时间',
  `activated_at` DATETIME DEFAULT NULL COMMENT '激活时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_stored_value_cards_card_no` (`card_no`),
  KEY `idx_stored_value_cards_user` (`user_id`),
  KEY `idx_stored_value_cards_status` (`status`, `expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='储值卡/礼品卡表';

CREATE TABLE `stored_value_logs` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `_openid` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'CloudBase 用户标识',
  `card_id` BIGINT UNSIGNED NOT NULL COMMENT '卡 ID',
  `user_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '用户 ID',
  `change_type` TINYINT UNSIGNED NOT NULL COMMENT '1充值 2消费 3退款返还 4调整 5激活',
  `amount` DECIMAL(10,2) NOT NULL COMMENT '变动金额',
  `balance_after` DECIMAL(10,2) NOT NULL COMMENT '变更后余额',
  `business_type` VARCHAR(30) NOT NULL COMMENT '业务类型',
  `business_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '关联业务 ID',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_stored_value_logs_card_created` (`card_id`, `created_at`),
  KEY `idx_stored_value_logs_business` (`business_type`, `business_id`),
  KEY `idx_stored_value_logs_user` (`user_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='储值卡流水表';

-- -----------------------------------------------------
-- 5. 商品、筛选与内容域
-- -----------------------------------------------------

CREATE TABLE `categories` (
  `id` INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `_openid` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'CloudBase 用户标识',
  `parent_id` INT UNSIGNED DEFAULT NULL COMMENT '父分类 ID',
  `name` VARCHAR(50) NOT NULL COMMENT '分类名称',
  `code` VARCHAR(50) NOT NULL COMMENT '分类编码',
  `icon_url` VARCHAR(255) DEFAULT NULL COMMENT '图标',
  `banner_url` VARCHAR(255) DEFAULT NULL COMMENT '分类头图',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序',
  `status` TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '1显示 0隐藏',
  `deleted` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '0未删 1已删',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_categories_code` (`code`),
  KEY `idx_categories_parent` (`parent_id`, `status`, `sort_order`),
  KEY `idx_categories_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品分类表';

CREATE TABLE `products` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `_openid` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'CloudBase 用户标识',
  `product_no` VARCHAR(64) NOT NULL COMMENT '商品编码',
  `category_id` INT UNSIGNED NOT NULL COMMENT '分类 ID',
  `title` VARCHAR(120) NOT NULL COMMENT '商品标题',
  `subtitle` VARCHAR(255) DEFAULT NULL COMMENT '卖点文案',
  `product_type` VARCHAR(30) NOT NULL COMMENT '商品类型',
  `material_summary` VARCHAR(100) DEFAULT NULL COMMENT '材质摘要',
  `main_image` VARCHAR(255) NOT NULL COMMENT '主图',
  `min_price` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '最低售价',
  `max_price` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '最高售价',
  `original_min_price` DECIMAL(10,2) DEFAULT NULL COMMENT '最低划线价',
  `sales_count` INT NOT NULL DEFAULT 0 COMMENT '累计销量',
  `favorite_count` INT NOT NULL DEFAULT 0 COMMENT '收藏数',
  `stock_status` TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '1有货 2紧张 3售罄',
  `is_new` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否新品',
  `is_hot` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否热销',
  `is_recommend` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否推荐',
  `status` TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '1上架 0下架',
  `deleted` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '0未删 1已删',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_products_product_no` (`product_no`),
  KEY `idx_products_category_status_sort` (`category_id`, `status`, `sort_order`),
  KEY `idx_products_status_flags_sort` (`status`, `is_new`, `is_hot`, `sort_order`),
  KEY `idx_products_deleted` (`deleted`),
  KEY `idx_products_recommend` (`status`, `is_recommend`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品 SPU 表';

CREATE TABLE `product_media` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `_openid` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'CloudBase 用户标识',
  `product_id` BIGINT UNSIGNED NOT NULL COMMENT '商品 ID',
  `media_type` TINYINT UNSIGNED NOT NULL COMMENT '1主图 2详情图 3场景图 4视频',
  `media_url` VARCHAR(255) NOT NULL COMMENT '媒体地址',
  `cover_url` VARCHAR(255) DEFAULT NULL COMMENT '视频封面',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_product_media_product` (`product_id`, `media_type`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品媒体表';

CREATE TABLE `product_detail_attrs` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `_openid` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'CloudBase 用户标识',
  `product_id` BIGINT UNSIGNED NOT NULL COMMENT '商品 ID',
  `attr_name` VARCHAR(50) NOT NULL COMMENT '属性名',
  `attr_value` VARCHAR(255) NOT NULL COMMENT '属性值',
  `group_name` VARCHAR(50) DEFAULT NULL COMMENT '属性分组',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_product_detail_attrs_product` (`product_id`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品详情属性表';

CREATE TABLE `product_spec_groups` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `_openid` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'CloudBase 用户标识',
  `product_id` BIGINT UNSIGNED NOT NULL COMMENT '商品 ID',
  `name` VARCHAR(50) NOT NULL COMMENT '规格组名称',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_product_spec_groups_product` (`product_id`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='规格组表';

CREATE TABLE `product_spec_values` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `_openid` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'CloudBase 用户标识',
  `spec_group_id` BIGINT UNSIGNED NOT NULL COMMENT '规格组 ID',
  `value` VARCHAR(50) NOT NULL COMMENT '规格值',
  `image_url` VARCHAR(255) DEFAULT NULL COMMENT '规格附图',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_product_spec_values_group` (`spec_group_id`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='规格值表';

CREATE TABLE `product_skus` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `_openid` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'CloudBase 用户标识',
  `product_id` BIGINT UNSIGNED NOT NULL COMMENT '商品 ID',
  `sku_code` VARCHAR(64) NOT NULL COMMENT 'SKU 编码',
  `image_url` VARCHAR(255) DEFAULT NULL COMMENT '当前规格主图',
  `specs_json` JSON NOT NULL COMMENT '规格快照',
  `price` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '销售价',
  `original_price` DECIMAL(10,2) DEFAULT NULL COMMENT '划线价',
  `cost_price` DECIMAL(10,2) DEFAULT NULL COMMENT '成本价',
  `stock` INT NOT NULL DEFAULT 0 COMMENT '可售库存',
  `locked_stock` INT NOT NULL DEFAULT 0 COMMENT '锁定库存',
  `warning_stock` INT NOT NULL DEFAULT 0 COMMENT '预警库存',
  `sales_count` INT NOT NULL DEFAULT 0 COMMENT '销量',
  `weight` DECIMAL(10,2) DEFAULT NULL COMMENT '重量kg',
  `volume` DECIMAL(10,2) DEFAULT NULL COMMENT '体积',
  `status` TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '1启用 0禁用',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_product_skus_sku_code` (`sku_code`),
  KEY `idx_product_skus_product_status` (`product_id`, `status`),
  KEY `idx_product_skus_stock` (`product_id`, `stock`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品 SKU 表';

CREATE TABLE `sku_spec_relations` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `_openid` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'CloudBase 用户标识',
  `sku_id` BIGINT UNSIGNED NOT NULL COMMENT 'SKU ID',
  `spec_group_id` BIGINT UNSIGNED NOT NULL COMMENT '规格组 ID',
  `spec_value_id` BIGINT UNSIGNED NOT NULL COMMENT '规格值 ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sku_spec_relations` (`sku_id`, `spec_group_id`, `spec_value_id`),
  KEY `idx_sku_spec_relations_value` (`spec_value_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='SKU 与规格值关联表';

CREATE TABLE `filter_attributes` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `_openid` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'CloudBase 用户标识',
  `attr_code` VARCHAR(50) NOT NULL COMMENT '筛选属性编码',
  `attr_name` VARCHAR(50) NOT NULL COMMENT '筛选属性名称',
  `input_type` TINYINT UNSIGNED NOT NULL COMMENT '1单选 2多选 3区间',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序',
  `status` TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '1启用 0禁用',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_filter_attributes_attr_code` (`attr_code`),
  KEY `idx_filter_attributes_status_sort` (`status`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='筛选属性定义表';

CREATE TABLE `category_filter_attributes` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `_openid` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'CloudBase 用户标识',
  `category_id` INT UNSIGNED NOT NULL COMMENT '分类 ID',
  `filter_attr_id` BIGINT UNSIGNED NOT NULL COMMENT '筛选属性 ID',
  `is_quick_filter` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '1是 0否',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_category_filter_attributes` (`category_id`, `filter_attr_id`),
  KEY `idx_category_filter_attributes_quick` (`category_id`, `is_quick_filter`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分类筛选项配置表';

CREATE TABLE `product_filter_values` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `_openid` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'CloudBase 用户标识',
  `product_id` BIGINT UNSIGNED NOT NULL COMMENT '商品 ID',
  `filter_attr_id` BIGINT UNSIGNED NOT NULL COMMENT '筛选属性 ID',
  `filter_value` VARCHAR(100) NOT NULL COMMENT '筛选值',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_product_filter_values_attr_value` (`filter_attr_id`, `filter_value`),
  KEY `idx_product_filter_values_product_attr` (`product_id`, `filter_attr_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品筛选值表';

-- -----------------------------------------------------
-- 6. 内容、专题与推荐域
-- -----------------------------------------------------

CREATE TABLE `banners` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `_openid` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'CloudBase 用户标识',
  `image_url` VARCHAR(255) NOT NULL COMMENT '图片地址',
  `title` VARCHAR(100) DEFAULT NULL COMMENT '标题',
  `subtitle` VARCHAR(255) DEFAULT NULL COMMENT '副标题',
  `link_type` VARCHAR(20) NOT NULL COMMENT '跳转类型',
  `link_value` VARCHAR(255) NOT NULL COMMENT '跳转值',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序',
  `status` TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
  `start_time` DATETIME DEFAULT NULL COMMENT '生效时间',
  `end_time` DATETIME DEFAULT NULL COMMENT '结束时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_banners_status_time_sort` (`status`, `start_time`, `end_time`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='轮播图表';

CREATE TABLE `topics` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `_openid` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'CloudBase 用户标识',
  `topic_no` VARCHAR(64) NOT NULL COMMENT '专题编码',
  `title` VARCHAR(100) NOT NULL COMMENT '专题标题',
  `subtitle` VARCHAR(255) DEFAULT NULL COMMENT '专题副标题',
  `cover_image` VARCHAR(255) NOT NULL COMMENT '头图',
  `theme_type` VARCHAR(30) NOT NULL COMMENT '主题类型',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '简介',
  `content_json` JSON DEFAULT NULL COMMENT '页面楼层配置快照',
  `start_time` DATETIME DEFAULT NULL COMMENT '开始时间',
  `end_time` DATETIME DEFAULT NULL COMMENT '结束时间',
  `status` TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_topics_topic_no` (`topic_no`),
  KEY `idx_topics_status_time_sort` (`status`, `start_time`, `end_time`, `sort_order`),
  KEY `idx_topics_theme` (`theme_type`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='专题活动表';

CREATE TABLE `topic_products` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `_openid` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'CloudBase 用户标识',
  `topic_id` BIGINT UNSIGNED NOT NULL COMMENT '专题 ID',
  `product_id` BIGINT UNSIGNED NOT NULL COMMENT '商品 ID',
  `recommend_reason` VARCHAR(255) DEFAULT NULL COMMENT '推荐理由',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_topic_products_topic_product` (`topic_id`, `product_id`),
  KEY `idx_topic_products_topic_sort` (`topic_id`, `sort_order`),
  KEY `idx_topic_products_product` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='专题商品关联表';

CREATE TABLE `recommend_positions` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `_openid` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'CloudBase 用户标识',
  `position_code` VARCHAR(50) NOT NULL COMMENT '推荐位编码',
  `position_name` VARCHAR(100) NOT NULL COMMENT '推荐位名称',
  `page_code` VARCHAR(50) NOT NULL COMMENT '页面编码',
  `biz_type` VARCHAR(30) NOT NULL COMMENT '业务类型',
  `status` TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_recommend_positions_code` (`position_code`),
  KEY `idx_recommend_positions_page` (`page_code`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='推荐位表';

CREATE TABLE `recommend_items` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `_openid` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'CloudBase 用户标识',
  `position_id` BIGINT UNSIGNED NOT NULL COMMENT '推荐位 ID',
  `biz_type` VARCHAR(30) NOT NULL COMMENT '业务类型',
  `biz_id` BIGINT UNSIGNED NOT NULL COMMENT '业务对象 ID',
  `ext_json` JSON DEFAULT NULL COMMENT '扩展展示配置',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序',
  `status` TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
  `start_time` DATETIME DEFAULT NULL COMMENT '生效时间',
  `end_time` DATETIME DEFAULT NULL COMMENT '失效时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_recommend_items_position_status_sort` (`position_id`, `status`, `sort_order`),
  KEY `idx_recommend_items_biz` (`biz_type`, `biz_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='推荐位内容表';

CREATE TABLE `product_relations` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `_openid` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'CloudBase 用户标识',
  `product_id` BIGINT UNSIGNED NOT NULL COMMENT '主商品 ID',
  `related_product_id` BIGINT UNSIGNED NOT NULL COMMENT '关联商品 ID',
  `relation_type` VARCHAR(30) NOT NULL COMMENT '关联类型',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序',
  `status` TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_product_relations_rel` (`product_id`, `related_product_id`, `relation_type`),
  KEY `idx_product_relations_product_type` (`product_id`, `relation_type`, `status`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品关联推荐表';

CREATE TABLE `search_hot_keywords` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `_openid` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'CloudBase 用户标识',
  `keyword` VARCHAR(100) NOT NULL COMMENT '热搜词',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序',
  `status` TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_search_hot_keywords_status_sort` (`status`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='热门搜索词表';

CREATE TABLE `search_suggest_keywords` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `_openid` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'CloudBase 用户标识',
  `keyword` VARCHAR(100) NOT NULL COMMENT '输入关键词',
  `suggest_word` VARCHAR(100) NOT NULL COMMENT '联想词',
  `target_type` VARCHAR(20) DEFAULT NULL COMMENT '目标类型',
  `target_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '跳转目标',
  `weight` INT NOT NULL DEFAULT 0 COMMENT '权重',
  `status` TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_search_suggest_keywords_keyword_weight` (`keyword`, `status`, `weight`),
  KEY `idx_search_suggest_keywords_target` (`target_type`, `target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='搜索联想词表';

-- -----------------------------------------------------
-- 7. 交易域
-- -----------------------------------------------------

CREATE TABLE `cart_items` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `_openid` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'CloudBase 用户标识',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户 ID',
  `product_id` BIGINT UNSIGNED NOT NULL COMMENT '商品 SPU ID',
  `sku_id` BIGINT UNSIGNED NOT NULL COMMENT 'SKU ID',
  `quantity` INT UNSIGNED NOT NULL DEFAULT 1 COMMENT '数量',
  `is_selected` TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '1已选 0未选',
  `invalid_reason` VARCHAR(100) DEFAULT NULL COMMENT '失效原因',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_cart_items_user_sku` (`user_id`, `sku_id`),
  KEY `idx_cart_items_user_selected` (`user_id`, `is_selected`, `updated_at`),
  KEY `idx_cart_items_product` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='购物车表';

CREATE TABLE `orders` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `_openid` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'CloudBase 用户标识',
  `order_no` VARCHAR(64) NOT NULL COMMENT '订单号',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户 ID',
  `order_type` TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '1普通订单 2活动订单',
  `total_amount` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '商品总金额',
  `discount_amount` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '总优惠金额',
  `coupon_amount` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '优惠券抵扣',
  `points_amount` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '积分抵扣',
  `card_amount` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '储值卡抵扣',
  `freight_amount` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '运费',
  `pay_amount` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '实付金额',
  `status` TINYINT UNSIGNED NOT NULL DEFAULT 10 COMMENT '10待付款 20待发货 30待收货 40已完成 50已取消 60售后中 70已关闭',
  `pay_status` TINYINT UNSIGNED NOT NULL DEFAULT 10 COMMENT '10未支付 20已支付 30支付失败 40部分退款 50全额退款',
  `source_type` VARCHAR(30) NOT NULL DEFAULT 'cart' COMMENT '来源类型',
  `pay_channel` TINYINT UNSIGNED DEFAULT NULL COMMENT '1微信支付 2储值卡混合支付',
  `consignee_info` JSON NOT NULL COMMENT '地址快照',
  `price_snapshot` JSON NOT NULL COMMENT '价格结算快照',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT '用户备注',
  `buyer_message` VARCHAR(255) DEFAULT NULL COMMENT '买家留言',
  `payment_time` DATETIME DEFAULT NULL COMMENT '支付时间',
  `delivery_time` DATETIME DEFAULT NULL COMMENT '发货时间',
  `finish_time` DATETIME DEFAULT NULL COMMENT '完成时间',
  `cancel_time` DATETIME DEFAULT NULL COMMENT '取消时间',
  `cancel_reason` VARCHAR(255) DEFAULT NULL COMMENT '取消原因',
  `close_time` DATETIME DEFAULT NULL COMMENT '关闭时间',
  `auto_close_time` DATETIME DEFAULT NULL COMMENT '自动关闭时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '下单时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_orders_order_no` (`order_no`),
  KEY `idx_orders_user_status_created` (`user_id`, `status`, `created_at`),
  KEY `idx_orders_pay_status_auto_close` (`pay_status`, `auto_close_time`),
  KEY `idx_orders_status_created` (`status`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单主表';

CREATE TABLE `order_items` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `_openid` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'CloudBase 用户标识',
  `order_id` BIGINT UNSIGNED NOT NULL COMMENT '订单 ID',
  `product_id` BIGINT UNSIGNED NOT NULL COMMENT '商品 ID 快照',
  `product_title` VARCHAR(120) NOT NULL COMMENT '商品标题快照',
  `product_image` VARCHAR(255) NOT NULL COMMENT '商品主图快照',
  `sku_id` BIGINT UNSIGNED NOT NULL COMMENT 'SKU ID 快照',
  `sku_code` VARCHAR(64) NOT NULL COMMENT 'SKU 编码快照',
  `sku_specs` VARCHAR(255) NOT NULL COMMENT '规格快照',
  `unit_price` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '下单单价',
  `original_price` DECIMAL(10,2) DEFAULT NULL COMMENT '划线价',
  `discount_amount` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '单项优惠金额',
  `quantity` INT UNSIGNED NOT NULL DEFAULT 1 COMMENT '购买数量',
  `subtotal` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '小计金额',
  `refund_status` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '0无售后 1售后中 2已退款 3已换货',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_order_items_order` (`order_id`),
  KEY `idx_order_items_sku` (`sku_id`),
  KEY `idx_order_items_refund_status` (`order_id`, `refund_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单明细表';

CREATE TABLE `payment_records` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `_openid` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'CloudBase 用户标识',
  `order_id` BIGINT UNSIGNED NOT NULL COMMENT '订单 ID',
  `order_no` VARCHAR(64) NOT NULL COMMENT '订单号',
  `pay_channel` TINYINT UNSIGNED NOT NULL COMMENT '支付渠道',
  `out_trade_no` VARCHAR(64) NOT NULL COMMENT '商户单号',
  `transaction_id` VARCHAR(64) DEFAULT NULL COMMENT '微信交易号',
  `pay_amount` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '支付金额',
  `pay_status` TINYINT UNSIGNED NOT NULL DEFAULT 10 COMMENT '10待支付 20成功 30失败 40已退款',
  `fail_reason` VARCHAR(255) DEFAULT NULL COMMENT '支付失败原因',
  `callback_payload` JSON DEFAULT NULL COMMENT '回调报文',
  `paid_at` DATETIME DEFAULT NULL COMMENT '支付成功时间',
  `refunded_at` DATETIME DEFAULT NULL COMMENT '退款时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_records_out_trade_no` (`out_trade_no`),
  KEY `idx_payment_records_order_pay_status` (`order_id`, `pay_status`),
  KEY `idx_payment_records_transaction` (`transaction_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支付流水表';

CREATE TABLE `stock_reservations` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `_openid` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'CloudBase 用户标识',
  `order_no` VARCHAR(64) NOT NULL COMMENT '订单号',
  `order_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '订单 ID',
  `sku_id` BIGINT UNSIGNED NOT NULL COMMENT 'SKU ID',
  `locked_qty` INT UNSIGNED NOT NULL COMMENT '锁定数量',
  `status` TINYINT UNSIGNED NOT NULL DEFAULT 10 COMMENT '10已锁定 20已释放 30已扣减',
  `expire_at` DATETIME NOT NULL COMMENT '锁定失效时间',
  `released_at` DATETIME DEFAULT NULL COMMENT '释放时间',
  `deducted_at` DATETIME DEFAULT NULL COMMENT '扣减时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_stock_reservations_order_sku_status` (`order_no`, `sku_id`, `status`),
  KEY `idx_stock_reservations_expire` (`status`, `expire_at`),
  KEY `idx_stock_reservations_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='库存锁定记录表';

CREATE TABLE `inventory_logs` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `_openid` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'CloudBase 用户标识',
  `sku_id` BIGINT UNSIGNED NOT NULL COMMENT 'SKU ID',
  `change_type` TINYINT UNSIGNED NOT NULL COMMENT '1入库 2锁定 3释放 4扣减 5退回 6调整',
  `change_qty` INT NOT NULL COMMENT '变动数量',
  `stock_before` INT NOT NULL COMMENT '变更前可售库存',
  `stock_after` INT NOT NULL COMMENT '变更后可售库存',
  `locked_before` INT NOT NULL COMMENT '变更前锁定库存',
  `locked_after` INT NOT NULL COMMENT '变更后锁定库存',
  `business_type` VARCHAR(30) NOT NULL COMMENT '业务类型',
  `business_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '关联业务 ID',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_inventory_logs_sku_created` (`sku_id`, `created_at`),
  KEY `idx_inventory_logs_business` (`business_type`, `business_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='库存流水表';

CREATE TABLE `shipments` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `_openid` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'CloudBase 用户标识',
  `order_id` BIGINT UNSIGNED NOT NULL COMMENT '订单 ID',
  `shipment_no` VARCHAR(64) NOT NULL COMMENT '发货单号',
  `company_code` VARCHAR(30) NOT NULL COMMENT '物流公司编码',
  `company_name` VARCHAR(50) NOT NULL COMMENT '物流公司名称',
  `tracking_no` VARCHAR(64) NOT NULL COMMENT '运单号',
  `ship_status` TINYINT UNSIGNED NOT NULL DEFAULT 10 COMMENT '10待发货 20已发货 30已签收 40异常',
  `receiver_name` VARCHAR(50) DEFAULT NULL COMMENT '收件人',
  `receiver_phone_mask` VARCHAR(20) DEFAULT NULL COMMENT '收件人脱敏手机号',
  `shipped_at` DATETIME DEFAULT NULL COMMENT '发货时间',
  `received_at` DATETIME DEFAULT NULL COMMENT '签收时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_shipments_shipment_no` (`shipment_no`),
  KEY `idx_shipments_order_ship_status` (`order_id`, `ship_status`),
  KEY `idx_shipments_tracking_no` (`tracking_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='发货表';

CREATE TABLE `shipment_tracks` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `_openid` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'CloudBase 用户标识',
  `shipment_id` BIGINT UNSIGNED NOT NULL COMMENT '发货表 ID',
  `node_time` DATETIME NOT NULL COMMENT '节点时间',
  `node_status` VARCHAR(30) NOT NULL COMMENT '节点状态',
  `node_content` VARCHAR(255) NOT NULL COMMENT '节点内容',
  `location` VARCHAR(100) DEFAULT NULL COMMENT '所在地',
  `operator` VARCHAR(50) DEFAULT NULL COMMENT '操作主体',
  `source_type` TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '1物流接口 2人工录入',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_shipment_tracks_shipment_time` (`shipment_id`, `node_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='物流轨迹表';

CREATE TABLE `order_logs` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `_openid` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'CloudBase 用户标识',
  `order_id` BIGINT UNSIGNED NOT NULL COMMENT '订单 ID',
  `action` VARCHAR(50) NOT NULL COMMENT '操作行为',
  `operator_type` TINYINT UNSIGNED NOT NULL COMMENT '1用户 2管理员 3系统',
  `operator_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '操作人 ID',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `ext_json` JSON DEFAULT NULL COMMENT '扩展信息',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`),
  KEY `idx_order_logs_order_created` (`order_id`, `created_at`),
  KEY `idx_order_logs_action` (`action`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单操作日志表';

-- -----------------------------------------------------
-- 8. 售后域
-- -----------------------------------------------------

CREATE TABLE `after_sale_orders` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `_openid` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'CloudBase 用户标识',
  `after_sale_no` VARCHAR(64) NOT NULL COMMENT '售后单号',
  `order_id` BIGINT UNSIGNED NOT NULL COMMENT '订单 ID',
  `order_item_id` BIGINT UNSIGNED NOT NULL COMMENT '订单明细 ID',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户 ID',
  `type` TINYINT UNSIGNED NOT NULL COMMENT '1仅退款 2退货退款 3换货',
  `reason_code` VARCHAR(30) NOT NULL COMMENT '售后原因编码',
  `reason_desc` VARCHAR(255) DEFAULT NULL COMMENT '补充说明',
  `evidence_urls` JSON DEFAULT NULL COMMENT '凭证图片',
  `apply_amount` DECIMAL(10,2) DEFAULT NULL COMMENT '申请退款金额',
  `approved_amount` DECIMAL(10,2) DEFAULT NULL COMMENT '审核通过金额',
  `status` TINYINT UNSIGNED NOT NULL DEFAULT 10 COMMENT '10待审核 20待退货 30待收货 40退款中 50已退款 60已完成 70已驳回',
  `refund_status` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '0未退款 1退款中 2已退款',
  `audit_status` TINYINT UNSIGNED NOT NULL DEFAULT 10 COMMENT '10待审核 20通过 30驳回',
  `audit_remark` VARCHAR(255) DEFAULT NULL COMMENT '审核备注',
  `reject_reason` VARCHAR(255) DEFAULT NULL COMMENT '驳回原因',
  `refund_channel` VARCHAR(30) DEFAULT NULL COMMENT '退款渠道',
  `refund_time` DATETIME DEFAULT NULL COMMENT '退款时间',
  `close_time` DATETIME DEFAULT NULL COMMENT '关闭时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_after_sale_orders_no` (`after_sale_no`),
  KEY `idx_after_sale_orders_order_item_status` (`order_id`, `order_item_id`, `status`),
  KEY `idx_after_sale_orders_user_status` (`user_id`, `status`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='售后单表';

CREATE TABLE `after_sale_return_shipments` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `_openid` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'CloudBase 用户标识',
  `after_sale_id` BIGINT UNSIGNED NOT NULL COMMENT '售后单 ID',
  `sender_type` TINYINT UNSIGNED NOT NULL COMMENT '1用户寄回 2商家换货寄出',
  `company_name` VARCHAR(50) NOT NULL COMMENT '物流公司',
  `tracking_no` VARCHAR(64) NOT NULL COMMENT '运单号',
  `address_snapshot` JSON DEFAULT NULL COMMENT '地址快照',
  `shipped_at` DATETIME DEFAULT NULL COMMENT '发货时间',
  `received_at` DATETIME DEFAULT NULL COMMENT '收货时间',
  `status` TINYINT UNSIGNED NOT NULL DEFAULT 10 COMMENT '10待寄出 20运输中 30已收货',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_after_sale_return_shipments_after_sale` (`after_sale_id`, `status`),
  KEY `idx_after_sale_return_shipments_tracking` (`tracking_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='售后退货物流表';

CREATE TABLE `after_sale_logs` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `_openid` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'CloudBase 用户标识',
  `after_sale_id` BIGINT UNSIGNED NOT NULL COMMENT '售后单 ID',
  `action` VARCHAR(50) NOT NULL COMMENT '操作行为',
  `operator_type` TINYINT UNSIGNED NOT NULL COMMENT '1用户 2管理员 3系统',
  `operator_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '操作人 ID',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `ext_json` JSON DEFAULT NULL COMMENT '扩展字段',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_after_sale_logs_after_sale_created` (`after_sale_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='售后日志表';

-- -----------------------------------------------------
-- 9. 营销与优惠域
-- -----------------------------------------------------

CREATE TABLE `coupons` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `_openid` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'CloudBase 用户标识',
  `coupon_no` VARCHAR(64) NOT NULL COMMENT '券模板编号',
  `name` VARCHAR(100) NOT NULL COMMENT '券名称',
  `type` TINYINT UNSIGNED NOT NULL COMMENT '1满减 2折扣',
  `value` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '面额或折扣值',
  `threshold` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '使用门槛',
  `scope_type` TINYINT UNSIGNED NOT NULL COMMENT '1全场 2指定分类 3指定商品',
  `can_stack` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否可叠加',
  `receive_limit_per_user` INT UNSIGNED NOT NULL DEFAULT 1 COMMENT '单用户可领取次数',
  `total_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '发放总量',
  `remain_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '剩余库存',
  `use_rule_desc` VARCHAR(255) DEFAULT NULL COMMENT '使用规则说明',
  `start_time` DATETIME NOT NULL COMMENT '生效时间',
  `end_time` DATETIME NOT NULL COMMENT '失效时间',
  `status` TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '1生效 0失效',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_coupons_coupon_no` (`coupon_no`),
  KEY `idx_coupons_status_time` (`status`, `start_time`, `end_time`),
  KEY `idx_coupons_scope` (`scope_type`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='优惠券模板表';

CREATE TABLE `coupon_scope_relations` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `_openid` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'CloudBase 用户标识',
  `coupon_id` BIGINT UNSIGNED NOT NULL COMMENT '优惠券 ID',
  `scope_type` TINYINT UNSIGNED NOT NULL COMMENT '2分类 3商品',
  `target_id` BIGINT UNSIGNED NOT NULL COMMENT '分类 ID 或商品 ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_coupon_scope_relations` (`coupon_id`, `scope_type`, `target_id`),
  KEY `idx_coupon_scope_relations_coupon_scope_target` (`coupon_id`, `scope_type`, `target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='优惠券适用范围表';

CREATE TABLE `user_coupons` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `_openid` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'CloudBase 用户标识',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户 ID',
  `coupon_id` BIGINT UNSIGNED NOT NULL COMMENT '优惠券模板 ID',
  `source_type` VARCHAR(30) NOT NULL COMMENT '来源类型',
  `status` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '0未使用 1已使用 2已过期 3已锁定',
  `order_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '核销订单 ID',
  `receive_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '领取时间',
  `lock_time` DATETIME DEFAULT NULL COMMENT '锁券时间',
  `use_time` DATETIME DEFAULT NULL COMMENT '使用时间',
  `expire_time` DATETIME NOT NULL COMMENT '过期时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_coupons_user_status_expire` (`user_id`, `status`, `expire_time`),
  KEY `idx_user_coupons_coupon` (`coupon_id`),
  KEY `idx_user_coupons_order` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户领券表';

-- -----------------------------------------------------
-- 10. 后台与权限域
-- -----------------------------------------------------

CREATE TABLE `admin_users` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `_openid` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'CloudBase 用户标识',
  `username` VARCHAR(50) NOT NULL COMMENT '登录名',
  `password_hash` VARCHAR(255) NOT NULL COMMENT '密码摘要',
  `real_name` VARCHAR(50) DEFAULT NULL COMMENT '姓名',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
  `status` TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '1启用 0禁用',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_admin_users_username` (`username`),
  KEY `idx_admin_users_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='后台用户表';

CREATE TABLE `admin_roles` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `_openid` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'CloudBase 用户标识',
  `role_name` VARCHAR(50) NOT NULL COMMENT '角色名称',
  `role_code` VARCHAR(50) NOT NULL COMMENT '角色编码',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_admin_roles_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='后台角色表';

CREATE TABLE `admin_user_roles` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `_openid` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'CloudBase 用户标识',
  `admin_user_id` BIGINT UNSIGNED NOT NULL COMMENT '后台用户 ID',
  `role_id` BIGINT UNSIGNED NOT NULL COMMENT '角色 ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_admin_user_roles_user_role` (`admin_user_id`, `role_id`),
  KEY `idx_admin_user_roles_role` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='后台用户角色关联表';

CREATE TABLE `operation_logs` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `_openid` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'CloudBase 用户标识',
  `operator_id` BIGINT UNSIGNED NOT NULL COMMENT '操作人 ID',
  `module` VARCHAR(50) NOT NULL COMMENT '模块',
  `action` VARCHAR(50) NOT NULL COMMENT '操作类型',
  `target_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '目标业务 ID',
  `content` VARCHAR(500) DEFAULT NULL COMMENT '操作内容',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_operation_logs_operator_created` (`operator_id`, `created_at`),
  KEY `idx_operation_logs_module_action` (`module`, `action`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='后台操作日志表';

SET FOREIGN_KEY_CHECKS = 1;
