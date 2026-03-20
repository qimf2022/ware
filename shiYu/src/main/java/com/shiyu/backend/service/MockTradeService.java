package com.shiyu.backend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shiyu.backend.common.BizCode;
import com.shiyu.backend.common.BizException;
import com.shiyu.backend.dto.AddressUpsertRequest;
import com.shiyu.backend.dto.AfterSaleApplyRequest;
import com.shiyu.backend.dto.AfterSaleReturnRequest;
import com.shiyu.backend.dto.OrderConfirmRequest;
import com.shiyu.backend.dto.OrderCreateRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 交易域持久化服务（购物车/地址/订单确认）。
 */
@Service
public class MockTradeService {

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final BigDecimal POINT_RATE = new BigDecimal("10");
    private static final int ORDER_PENDING_PAY = 10;
    private static final int ORDER_PENDING_SHIP = 20;
    private static final int ORDER_PENDING_RECEIVE = 30;
    private static final int ORDER_COMPLETED = 40;
    private static final int ORDER_CANCELLED = 50;
    private static final int ORDER_AFTER_SALE = 60;
    private static final int ORDER_CLOSED = 70;
    private static final int PAY_UNPAID = 10;
    private static final int PAY_SUCCESS = 20;
    private static final int LOCKED = 10;
    private static final int RELEASED = 20;
    private static final int DEDUCTED = 30;
    private static final int AFTER_SALE_PENDING_AUDIT = 10;
    private static final int AFTER_SALE_PENDING_RETURN = 20;
    private static final int AFTER_SALE_PENDING_RECEIVE = 30;
    private static final int AFTER_SALE_REFUNDING = 40;
    private static final int AFTER_SALE_REFUNDED = 50;
    private static final int AFTER_SALE_COMPLETED = 60;
    private static final int AFTER_SALE_REJECTED = 70;
    private static final int AFTER_SALE_AUDIT_PENDING = 10;
    private static final int AFTER_SALE_AUDIT_APPROVED = 20;
    private static final int AFTER_SALE_AUDIT_REJECTED = 30;
    private static final int AFTER_SALE_REFUND_NONE = 0;
    private static final int AFTER_SALE_REFUNDING_FLAG = 1;
    private static final int AFTER_SALE_REFUNDED_FLAG = 2;
    private static final int ORDER_ITEM_REFUND_NONE = 0;
    private static final int ORDER_ITEM_REFUND_PARTIAL = 1;
    private static final int ORDER_ITEM_REFUND_DONE = 2;

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final LogisticsTraceService logisticsTraceService;
    private final AfterSaleCompensationProducer afterSaleCompensationProducer;

    public MockTradeService(JdbcTemplate jdbcTemplate,
                            ObjectMapper objectMapper,
                            LogisticsTraceService logisticsTraceService,
                            AfterSaleCompensationProducer afterSaleCompensationProducer) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.logisticsTraceService = logisticsTraceService;
        this.afterSaleCompensationProducer = afterSaleCompensationProducer;
    }

    public Map<String, Object> getCart(Long userId) {
        Long uid = requireUser(userId);
        ensureUser(uid);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT ci.id, ci.product_id, ci.sku_id, ci.quantity, ci.is_selected, ci.invalid_reason, " +
                        "p.title, p.main_image, p.status AS product_status, p.deleted AS product_deleted, " +
                        "ps.specs_json, ps.price, ps.original_price, ps.stock, ps.status AS sku_status " +
                        "FROM cart_items ci " +
                        "JOIN products p ON p.id = ci.product_id " +
                        "JOIN product_skus ps ON ps.id = ci.sku_id " +
                        "WHERE ci.user_id = ? ORDER BY ci.updated_at DESC, ci.id DESC",
                uid);

        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
        List<Map<String, Object>> invalidItems = new ArrayList<Map<String, Object>>();
        int totalCount = 0;
        int selectedCount = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal selectedAmount = BigDecimal.ZERO;

        for (Map<String, Object> row : rows) {
            int quantity = toInt(row.get("quantity"));
            int isSelected = toInt(row.get("is_selected"));
            int productStatus = toInt(row.get("product_status"));
            int productDeleted = toInt(row.get("product_deleted"));
            int skuStatus = toInt(row.get("sku_status"));
            int stock = toInt(row.get("stock"));
            BigDecimal price = toDecimal(row.get("price"));

            String invalidReason = null;
            if (productDeleted == 1 || productStatus != 1) {
                invalidReason = "商品已下架";
            } else if (skuStatus != 1) {
                invalidReason = "规格已失效";
            } else if (stock <= 0) {
                invalidReason = "库存不足";
            } else if (quantity > stock) {
                invalidReason = "库存不足";
            }
            if (invalidReason == null && row.get("invalid_reason") != null) {
                invalidReason = String.valueOf(row.get("invalid_reason"));
            }

            BigDecimal subtotal = invalidReason == null ? price.multiply(new BigDecimal(quantity)).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("id", toLong(row.get("id")));

            Map<String, Object> product = new HashMap<String, Object>();
            product.put("id", toLong(row.get("product_id")));
            product.put("title", row.get("title"));
            product.put("main_image", row.get("main_image"));
            product.put("status", productStatus);
            product.put("shop_name", "诗语家居");
            product.put("brand_name", "诗语家居");
            item.put("product", product);

            Map<String, Object> sku = new HashMap<String, Object>();
            sku.put("id", toLong(row.get("sku_id")));
            sku.put("specs_json", parseJsonMap(String.valueOf(row.get("specs_json"))));
            sku.put("price", price);
            sku.put("original_price", toDecimal(row.get("original_price")));
            sku.put("discount_price", price);
            sku.put("stock", stock);
            sku.put("status", skuStatus);
            item.put("sku", sku);

            item.put("quantity", quantity);
            item.put("is_selected", invalidReason == null ? isSelected : 0);
            item.put("invalid_reason", invalidReason);
            item.put("subtotal", subtotal);

            totalCount += quantity;
            if (invalidReason == null) {
                totalAmount = totalAmount.add(subtotal);
                if (isSelected == 1) {
                    selectedCount += quantity;
                    selectedAmount = selectedAmount.add(subtotal);
                }
                items.add(item);
            } else {
                invalidItems.add(item);
            }
        }

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("items", items);
        data.put("invalid_items", invalidItems);
        data.put("total_count", totalCount);
        data.put("selected_count", selectedCount);
        data.put("total_amount", totalAmount.setScale(2, RoundingMode.HALF_UP));
        data.put("selected_amount", selectedAmount.setScale(2, RoundingMode.HALF_UP));
        return data;
    }

    public Map<String, Object> addCart(Long userId, Long productId, Long skuId, Integer quantity) {
        Long uid = requireUser(userId);
        ensureUser(uid);
        validateSku(productId, skuId);

        List<Map<String, Object>> exists = jdbcTemplate.queryForList(
                "SELECT id FROM cart_items WHERE user_id = ? AND sku_id = ? LIMIT 1", uid, skuId);
        Long cartId;
        if (exists.isEmpty()) {
            jdbcTemplate.update(
                    "INSERT INTO cart_items(_openid, user_id, product_id, sku_id, quantity, is_selected, created_at, updated_at) " +
                            "VALUES('', ?, ?, ?, ?, 1, NOW(), NOW())",
                    uid, productId, skuId, quantity);
            cartId = jdbcTemplate.queryForObject("SELECT id FROM cart_items WHERE user_id = ? AND sku_id = ? LIMIT 1", Long.class, uid, skuId);
        } else {
            cartId = toLong(exists.get(0).get("id"));
            jdbcTemplate.update(
                    "UPDATE cart_items SET quantity = quantity + ?, product_id = ?, is_selected = 1, invalid_reason = NULL, updated_at = NOW() " +
                            "WHERE id = ? AND user_id = ?",
                    quantity, productId, cartId, uid);
        }

        Number totalCount = jdbcTemplate.queryForObject("SELECT COALESCE(SUM(quantity),0) FROM cart_items WHERE user_id = ?", Number.class, uid);
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("cart_id", cartId);
        data.put("total_count", totalCount == null ? 0 : totalCount.intValue());
        return data;
    }

    public void updateCart(Long userId, Long cartId, Integer quantity, Integer isSelected) {
        Long uid = requireUser(userId);
        ensureCartOwner(uid, cartId);
        if (quantity == null && isSelected == null) {
            throw new BizException(BizCode.PARAM_ERROR, "quantity 或 is_selected 至少传一个");
        }
        if (quantity != null && quantity < 1) {
            throw new BizException(BizCode.PARAM_ERROR, "quantity 最小为 1");
        }
        if (isSelected != null && isSelected != 0 && isSelected != 1) {
            throw new BizException(BizCode.PARAM_ERROR, "is_selected 仅支持 0/1");
        }

        jdbcTemplate.update(
                "UPDATE cart_items SET quantity = COALESCE(?, quantity), is_selected = COALESCE(?, is_selected), updated_at = NOW() " +
                        "WHERE id = ? AND user_id = ?",
                quantity, isSelected, cartId, uid);
    }

    public void deleteCart(Long userId, List<Long> ids) {
        Long uid = requireUser(userId);
        if (CollectionUtils.isEmpty(ids)) {
            throw new BizException(BizCode.PARAM_ERROR, "ids 不能为空");
        }
        String placeholders = ids.stream().map(id -> "?").collect(Collectors.joining(","));
        List<Object> args = new ArrayList<Object>();
        args.add(uid);
        args.addAll(ids);
        jdbcTemplate.update("DELETE FROM cart_items WHERE user_id = ? AND id IN (" + placeholders + ")", args.toArray());
    }

    public void updateCartSelectAll(Long userId, Integer isSelected) {
        Long uid = requireUser(userId);
        if (isSelected == null || (isSelected != 0 && isSelected != 1)) {
            throw new BizException(BizCode.PARAM_ERROR, "is_selected 仅支持 0/1");
        }

        jdbcTemplate.update(
                "UPDATE cart_items ci " +
                        "JOIN products p ON p.id = ci.product_id " +
                        "JOIN product_skus ps ON ps.id = ci.sku_id " +
                        "SET ci.is_selected = ?, ci.updated_at = NOW() " +
                        "WHERE ci.user_id = ? AND p.deleted = 0 AND p.status = 1 AND ps.status = 1 AND ps.stock > 0 AND ci.quantity <= ps.stock",
                isSelected,
                uid);
    }

    public void updateCartBatch(Long userId, List<Long> ids, Integer quantity, Integer isSelected) {
        Long uid = requireUser(userId);
        if (CollectionUtils.isEmpty(ids)) {
            throw new BizException(BizCode.PARAM_ERROR, "ids 不能为空");
        }
        if (quantity == null && isSelected == null) {
            throw new BizException(BizCode.PARAM_ERROR, "quantity 或 is_selected 至少传一个");
        }
        if (quantity != null && quantity < 1) {
            throw new BizException(BizCode.PARAM_ERROR, "quantity 最小为 1");
        }
        if (isSelected != null && isSelected != 0 && isSelected != 1) {
            throw new BizException(BizCode.PARAM_ERROR, "is_selected 仅支持 0/1");
        }

        String placeholders = ids.stream().map(id -> "?").collect(Collectors.joining(","));
        List<Object> args = new ArrayList<Object>();
        args.add(quantity);
        args.add(isSelected);
        args.add(uid);
        args.addAll(ids);
        jdbcTemplate.update(
                "UPDATE cart_items SET quantity = COALESCE(?, quantity), is_selected = COALESCE(?, is_selected), updated_at = NOW() " +
                        "WHERE user_id = ? AND id IN (" + placeholders + ")",
                args.toArray());
    }

    public Map<String, Object> getCartRecommend(Long userId, Integer limit) {
        Long uid = requireUser(userId);
        ensureUser(uid);

        int safeLimit = limit == null || limit < 1 ? 6 : Math.min(limit, 20);
        List<Map<String, Object>> cartRows = jdbcTemplate.queryForList(
                "SELECT DISTINCT ci.product_id, p.category_id FROM cart_items ci " +
                        "JOIN products p ON p.id = ci.product_id " +
                        "WHERE ci.user_id = ?",
                uid);

        Set<Long> cartProductIds = new LinkedHashSet<Long>();
        Set<Integer> categoryIds = new LinkedHashSet<Integer>();
        for (Map<String, Object> row : cartRows) {
            cartProductIds.add(toLong(row.get("product_id")));
            categoryIds.add(toInt(row.get("category_id")));
        }

        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        if (!categoryIds.isEmpty()) {
            String categoryPlaceholders = categoryIds.stream().map(id -> "?").collect(Collectors.joining(","));
            List<Object> args = new ArrayList<Object>();
            StringBuilder sql = new StringBuilder(
                    "SELECT p.id, p.title, p.subtitle, p.main_image, p.min_price, p.max_price, p.original_min_price, p.sales_count, p.stock_status, p.category_id " +
                            "FROM products p WHERE p.status = 1 AND p.deleted = 0 AND p.category_id IN (")
                    .append(categoryPlaceholders)
                    .append(")");
            args.addAll(categoryIds);

            if (!cartProductIds.isEmpty()) {
                String productPlaceholders = cartProductIds.stream().map(id -> "?").collect(Collectors.joining(","));
                sql.append(" AND p.id NOT IN (").append(productPlaceholders).append(")");
                args.addAll(cartProductIds);
            }

            sql.append(" ORDER BY p.is_recommend DESC, p.sales_count DESC, p.sort_order ASC, p.id DESC LIMIT ?");
            args.add(safeLimit);
            list = jdbcTemplate.queryForList(sql.toString(), args.toArray());
        }

        if (list.isEmpty()) {
            List<Object> args = new ArrayList<Object>();
            StringBuilder sql = new StringBuilder(
                    "SELECT p.id, p.title, p.subtitle, p.main_image, p.min_price, p.max_price, p.original_min_price, p.sales_count, p.stock_status, p.category_id " +
                            "FROM products p WHERE p.status = 1 AND p.deleted = 0");
            if (!cartProductIds.isEmpty()) {
                String productPlaceholders = cartProductIds.stream().map(id -> "?").collect(Collectors.joining(","));
                sql.append(" AND p.id NOT IN (").append(productPlaceholders).append(")");
                args.addAll(cartProductIds);
            }
            sql.append(" ORDER BY p.is_recommend DESC, p.sales_count DESC, p.sort_order ASC, p.id DESC LIMIT ?");
            args.add(safeLimit);
            list = jdbcTemplate.queryForList(sql.toString(), args.toArray());
        }

        for (Map<String, Object> item : list) {
            item.put("category", categorySimple(toInt(item.get("category_id"))));
            item.remove("category_id");
        }

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("list", list);
        return data;
    }

    public Map<String, Object> checkCart(Long userId, List<Long> cartIds) {

        Long uid = requireUser(userId);
        ensureUser(uid);

        String sql = "SELECT ci.id, ci.sku_id, ci.quantity, p.status AS product_status, p.deleted AS product_deleted, " +
                "ps.stock, ps.status AS sku_status " +
                "FROM cart_items ci JOIN products p ON p.id = ci.product_id JOIN product_skus ps ON ps.id = ci.sku_id WHERE ci.user_id = ?";
        List<Object> args = new ArrayList<Object>();
        args.add(uid);
        if (!CollectionUtils.isEmpty(cartIds)) {
            String placeholders = cartIds.stream().map(id -> "?").collect(Collectors.joining(","));
            sql += " AND ci.id IN (" + placeholders + ")";
            args.addAll(cartIds);
        } else {
            sql += " AND ci.is_selected = 1";
        }
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, args.toArray());

        List<Map<String, Object>> checkItems = new ArrayList<Map<String, Object>>();
        int invalidCount = 0;
        for (Map<String, Object> row : rows) {
            int quantity = toInt(row.get("quantity"));
            int stock = toInt(row.get("stock"));
            int productStatus = toInt(row.get("product_status"));
            int productDeleted = toInt(row.get("product_deleted"));
            int skuStatus = toInt(row.get("sku_status"));

            String invalidReason = null;
            if (productDeleted == 1 || productStatus != 1) {
                invalidReason = "商品已下架";
            } else if (skuStatus != 1) {
                invalidReason = "规格已失效";
            } else if (quantity > stock) {
                invalidReason = "库存不足";
            }
            boolean valid = invalidReason == null;
            if (!valid) {
                invalidCount++;
            }

            Map<String, Object> item = new HashMap<String, Object>();
            item.put("cart_id", toLong(row.get("id")));
            item.put("sku_id", toLong(row.get("sku_id")));
            item.put("quantity", quantity);
            item.put("stock", stock);
            item.put("is_valid", valid);
            item.put("invalid_reason", invalidReason);
            checkItems.add(item);
        }

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("can_checkout", !checkItems.isEmpty() && invalidCount == 0);
        data.put("check_items", checkItems);
        data.put("invalid_count", invalidCount);
        return data;
    }

    public Map<String, Object> listAddresses(Long userId) {
        Long uid = requireUser(userId);
        ensureUser(uid);
        List<Map<String, Object>> list = jdbcTemplate.queryForList(
                "SELECT id, consignee, phone_mask, province, city, district, street, detail_address, tag, is_default, longitude, latitude " +
                        "FROM user_addresses WHERE user_id = ? AND deleted = 0 ORDER BY is_default DESC, updated_at DESC, id DESC",
                uid);
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("list", list);
        return data;
    }

    public Long createAddress(Long userId, AddressUpsertRequest request) {
        Long uid = requireUser(userId);
        ensureUser(uid);

        Integer isDefault = normalizeDefault(request.getIsDefault());
        if (isDefault == 1) {
            clearDefaultAddress(uid);
        }

        String phone = request.getPhone().trim();
        jdbcTemplate.update(
                "INSERT INTO user_addresses(_openid, user_id, consignee, phone_cipher, phone_mask, province, city, district, street, detail_address, postal_code, tag, longitude, latitude, is_default, deleted, created_at, updated_at) " +
                        "VALUES('', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, NOW(), NOW())",
                uid,
                trim(request.getConsignee()),
                phone,
                maskPhone(phone),
                trim(request.getProvince()),
                trim(request.getCity()),
                trim(request.getDistrict()),
                trim(request.getStreet()),
                trim(request.getDetailAddress()),
                trim(request.getPostalCode()),
                trim(request.getTag()),
                request.getLongitude(),
                request.getLatitude(),
                isDefault);

        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    public void updateAddress(Long userId, Long addressId, AddressUpsertRequest request) {
        Long uid = requireUser(userId);
        ensureAddressOwner(uid, addressId);

        Integer isDefault = normalizeDefault(request.getIsDefault());
        if (isDefault == 1) {
            clearDefaultAddress(uid);
        }

        String phone = request.getPhone().trim();
        jdbcTemplate.update(
                "UPDATE user_addresses SET consignee = ?, phone_cipher = ?, phone_mask = ?, province = ?, city = ?, district = ?, street = ?, " +
                        "detail_address = ?, postal_code = ?, tag = ?, longitude = ?, latitude = ?, is_default = ?, updated_at = NOW() " +
                        "WHERE id = ? AND user_id = ? AND deleted = 0",
                trim(request.getConsignee()),
                phone,
                maskPhone(phone),
                trim(request.getProvince()),
                trim(request.getCity()),
                trim(request.getDistrict()),
                trim(request.getStreet()),
                trim(request.getDetailAddress()),
                trim(request.getPostalCode()),
                trim(request.getTag()),
                request.getLongitude(),
                request.getLatitude(),
                isDefault,
                addressId,
                uid);
    }

    public void deleteAddress(Long userId, Long addressId) {
        Long uid = requireUser(userId);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, is_default FROM user_addresses WHERE id = ? AND user_id = ? AND deleted = 0 LIMIT 1",
                addressId, uid);
        if (rows.isEmpty()) {
            throw new BizException(BizCode.RESOURCE_NOT_FOUND, "地址不存在");
        }

        int wasDefault = toInt(rows.get(0).get("is_default"));
        jdbcTemplate.update("UPDATE user_addresses SET deleted = 1, is_default = 0, updated_at = NOW() WHERE id = ? AND user_id = ?", addressId, uid);

        if (wasDefault == 1) {
            List<Map<String, Object>> candidate = jdbcTemplate.queryForList(
                    "SELECT id FROM user_addresses WHERE user_id = ? AND deleted = 0 ORDER BY updated_at DESC, id DESC LIMIT 1", uid);
            if (!candidate.isEmpty()) {
                jdbcTemplate.update("UPDATE user_addresses SET is_default = 1, updated_at = NOW() WHERE id = ? AND user_id = ?",
                        toLong(candidate.get(0).get("id")), uid);
            }
        }
    }

    public void setDefaultAddress(Long userId, Long addressId) {
        Long uid = requireUser(userId);
        ensureAddressOwner(uid, addressId);
        clearDefaultAddress(uid);
        jdbcTemplate.update("UPDATE user_addresses SET is_default = 1, updated_at = NOW() WHERE id = ? AND user_id = ? AND deleted = 0", addressId, uid);
    }

    public Map<String, Object> confirmOrder(Long userId, OrderConfirmRequest request) {
        Long uid = requireUser(userId);
        ensureUser(uid);

        Map<String, Object> address = loadAddress(uid, request.getAddressId());
        List<Map<String, Object>> items = resolveConfirmItems(uid, request);
        if (items.isEmpty()) {
            throw new BizException(BizCode.BIZ_ERROR, "缺少可结算商品");
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (Map<String, Object> item : items) {
            totalAmount = totalAmount.add(toDecimal(item.get("subtotal")));
        }
        BigDecimal freightAmount = totalAmount.compareTo(new BigDecimal("99")) >= 0 ? BigDecimal.ZERO : new BigDecimal("10");

        BigDecimal couponAmount = calculateCouponAmount(uid, request.getCouponId(), totalAmount);

        int userPoints = getUserPoints(uid);
        int requestedPoints = request.getUsePoints() == null ? 0 : Math.max(0, request.getUsePoints());
        int maxUsablePoints = totalAmount.subtract(couponAmount).max(BigDecimal.ZERO).multiply(POINT_RATE).intValue();
        int realUsePoints = Math.min(Math.min(requestedPoints, userPoints), maxUsablePoints);
        BigDecimal pointsAmount = new BigDecimal(realUsePoints).divide(POINT_RATE, 2, RoundingMode.DOWN);

        BigDecimal cardAmount = calculateCardAmount(uid, request.getCardId(), totalAmount.subtract(couponAmount).subtract(pointsAmount));
        BigDecimal discountAmount = couponAmount.add(pointsAmount).add(cardAmount);
        BigDecimal payAmount = totalAmount.add(freightAmount).subtract(discountAmount).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

        Map<String, Object> price = new HashMap<String, Object>();
        price.put("total_amount", totalAmount.setScale(2, RoundingMode.HALF_UP));
        price.put("freight_amount", freightAmount.setScale(2, RoundingMode.HALF_UP));
        price.put("discount_amount", discountAmount.setScale(2, RoundingMode.HALF_UP));
        price.put("coupon_amount", couponAmount.setScale(2, RoundingMode.HALF_UP));
        price.put("points_amount", pointsAmount.setScale(2, RoundingMode.HALF_UP));
        price.put("card_amount", cardAmount.setScale(2, RoundingMode.HALF_UP));
        price.put("pay_amount", payAmount);

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("address", address);
        data.put("items", items);
        data.put("price", price);
        data.put("available_coupons", loadAvailableCoupons(uid, totalAmount));
        data.put("user_points", userPoints);
        data.put("points_to_money_rate", POINT_RATE.intValue());
        return data;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createOrder(Long userId, OrderCreateRequest request) {
        Long uid = requireUser(userId);
        ensureUser(uid);

        OrderConfirmRequest confirmRequest = new OrderConfirmRequest();
        confirmRequest.setCartIds(request.getCartIds());
        confirmRequest.setProductId(request.getProductId());
        confirmRequest.setSkuId(request.getSkuId());
        confirmRequest.setQuantity(request.getQuantity());
        confirmRequest.setAddressId(request.getAddressId());
        confirmRequest.setCouponId(request.getCouponId());
        confirmRequest.setUsePoints(request.getUsePoints());
        confirmRequest.setCardId(request.getCardId());

        Map<String, Object> confirmResult = confirmOrder(uid, confirmRequest);
        List<Map<String, Object>> items = castListMap(confirmResult.get("items"));
        if (CollectionUtils.isEmpty(items)) {
            throw new BizException(BizCode.BIZ_ERROR, "缺少可下单商品");
        }

        String sourceType = trim(request.getSourceType());
        if (sourceType == null) {
            throw new BizException(BizCode.PARAM_ERROR, "source_type 不能为空");
        }

        Map<String, Object> address = castMap(confirmResult.get("address"));
        Map<String, Object> price = castMap(confirmResult.get("price"));

        String orderNo = generateOrderNo();
        LocalDateTime expireAt = LocalDateTime.now().plusMinutes(10);

        String consigneeInfoJson = toJson(address);
        String priceSnapshotJson = toJson(price);

        jdbcTemplate.update(
                "INSERT INTO orders(_openid, order_no, user_id, order_type, total_amount, discount_amount, coupon_amount, points_amount, card_amount, freight_amount, pay_amount, status, pay_status, source_type, consignee_info, price_snapshot, remark, buyer_message, auto_close_time, created_at, updated_at) " +
                        "VALUES('', ?, ?, 1, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS JSON), CAST(? AS JSON), ?, ?, ?, NOW(), NOW())",
                orderNo,
                uid,
                toDecimal(price.get("total_amount")),
                toDecimal(price.get("discount_amount")),
                toDecimal(price.get("coupon_amount")),
                toDecimal(price.get("points_amount")),
                toDecimal(price.get("card_amount")),
                toDecimal(price.get("freight_amount")),
                toDecimal(price.get("pay_amount")),
                ORDER_PENDING_PAY,
                PAY_UNPAID,
                sourceType,
                consigneeInfoJson,
                priceSnapshotJson,
                trim(request.getRemark()),
                trim(request.getRemark()),
                Timestamp.valueOf(expireAt));

        Long orderId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        if (orderId == null) {
            throw new BizException(BizCode.SYSTEM_ERROR, "订单创建失败");
        }

        Set<Long> skuIds = new LinkedHashSet<Long>();
        for (Map<String, Object> item : items) {
            skuIds.add(toLong(item.get("sku_id")));
        }
        Map<Long, Map<String, Object>> skuMeta = loadSkuMeta(skuIds);

        for (Map<String, Object> item : items) {
            Long skuId = toLong(item.get("sku_id"));
            Integer quantity = toInt(item.get("quantity"));
            Map<String, Object> skuRow = skuMeta.get(skuId);
            if (skuRow == null) {
                throw new BizException(BizCode.RESOURCE_NOT_FOUND, "规格不存在");
            }

            lockStock(orderNo, orderId, skuId, quantity, expireAt);

            jdbcTemplate.update(
                    "INSERT INTO order_items(_openid, order_id, product_id, product_title, product_image, sku_id, sku_code, sku_specs, unit_price, original_price, discount_amount, quantity, subtotal, refund_status, created_at) " +
                            "VALUES('', ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, ?, ?, 0, NOW())",
                    orderId,
                    toLong(item.get("product_id")),
                    safe(item.get("product_title")),
                    safe(item.get("product_image")),
                    skuId,
                    safe(skuRow.get("sku_code")),
                    safe(item.get("sku_specs")),
                    toDecimal(item.get("unit_price")),
                    toDecimal(skuRow.get("original_price")),
                    quantity,
                    toDecimal(item.get("subtotal")));
        }

        if (!CollectionUtils.isEmpty(request.getCartIds())) {
            String placeholders = request.getCartIds().stream().map(id -> "?").collect(Collectors.joining(","));
            List<Object> args = new ArrayList<Object>();
            args.add(uid);
            args.addAll(request.getCartIds());
            jdbcTemplate.update("DELETE FROM cart_items WHERE user_id = ? AND id IN (" + placeholders + ")", args.toArray());
        }

        insertOrderLog(orderId, "create", 1, uid, "用户提交订单", null);

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("order_id", orderId);
        data.put("order_no", orderNo);
        data.put("pay_amount", toDecimal(price.get("pay_amount")));
        data.put("expire_time", expireAt.format(DT));
        return data;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> payOrder(Long userId, Long orderId, Integer payChannel) {
        Long uid = requireUser(userId);
        if (payChannel == null || (payChannel != 1 && payChannel != 2)) {
            throw new BizException(BizCode.PARAM_ERROR, "pay_channel 仅支持 1/2");
        }

        Map<String, Object> order = loadOrder(uid, orderId);
        Integer status = toInt(order.get("status"));
        Integer payStatus = toInt(order.get("pay_status"));
        if (status != ORDER_PENDING_PAY || payStatus != PAY_UNPAID) {
            throw new BizException(BizCode.BIZ_ERROR, "当前订单状态不可支付");
        }

        String orderNo = safe(order.get("order_no"));
        String outTradeNo = generateOutTradeNo();
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());

        jdbcTemplate.update(
                "UPDATE orders SET status = ?, pay_status = ?, pay_channel = ?, payment_time = ?, updated_at = NOW() WHERE id = ? AND user_id = ?",
                ORDER_PENDING_SHIP,
                PAY_SUCCESS,
                payChannel,
                now,
                orderId,
                uid);

        jdbcTemplate.update(
                "INSERT INTO payment_records(_openid, order_id, order_no, pay_channel, out_trade_no, transaction_id, pay_amount, pay_status, callback_payload, paid_at, created_at) " +
                        "VALUES('', ?, ?, ?, ?, ?, ?, 20, CAST(? AS JSON), ?, NOW())",
                orderId,
                orderNo,
                payChannel,
                outTradeNo,
                "WX" + outTradeNo,
                toDecimal(order.get("pay_amount")),
                toJson(Collections.singletonMap("mock", true)),
                now);

        List<Map<String, Object>> reservations = jdbcTemplate.queryForList(
                "SELECT id, sku_id, locked_qty FROM stock_reservations WHERE order_id = ? AND status = ?",
                orderId,
                LOCKED);
        for (Map<String, Object> reservation : reservations) {
            Long reservationId = toLong(reservation.get("id"));
            Long skuId = toLong(reservation.get("sku_id"));
            Integer qty = toInt(reservation.get("locked_qty"));

            Map<String, Object> sku = jdbcTemplate.queryForMap("SELECT stock, locked_stock FROM product_skus WHERE id = ? FOR UPDATE", skuId);
            int stockBefore = toInt(sku.get("stock"));
            int lockedBefore = toInt(sku.get("locked_stock"));
            if (lockedBefore < qty) {
                throw new BizException(BizCode.BIZ_ERROR, "锁定库存异常");
            }

            jdbcTemplate.update(
                    "UPDATE product_skus SET locked_stock = locked_stock - ?, sales_count = sales_count + ?, updated_at = NOW() WHERE id = ?",
                    qty,
                    qty,
                    skuId);

            jdbcTemplate.update(
                    "UPDATE stock_reservations SET status = ?, deducted_at = NOW(), updated_at = NOW() WHERE id = ?",
                    DEDUCTED,
                    reservationId);

            jdbcTemplate.update(
                    "INSERT INTO inventory_logs(_openid, sku_id, change_type, change_qty, stock_before, stock_after, locked_before, locked_after, business_type, business_id, remark, created_at) " +
                            "VALUES('', ?, 4, ?, ?, ?, ?, ?, 'order_pay', ?, ?, NOW())",
                    skuId,
                    -qty,
                    stockBefore,
                    stockBefore,
                    lockedBefore,
                    lockedBefore - qty,
                    orderId,
                    "支付扣减库存");
        }

        insertOrderLog(orderId, "pay", 1, uid, "用户支付成功", toJson(Collections.singletonMap("out_trade_no", outTradeNo)));

        Map<String, Object> payParams = new LinkedHashMap<String, Object>();
        payParams.put("timeStamp", String.valueOf(System.currentTimeMillis() / 1000));
        payParams.put("nonceStr", "nonce" + ThreadLocalRandom.current().nextInt(100000, 999999));
        payParams.put("package", "prepay_id=" + outTradeNo);
        payParams.put("signType", "RSA");
        payParams.put("paySign", "mock_pay_sign_" + outTradeNo.substring(Math.max(0, outTradeNo.length() - 10)));

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("order_id", orderId);
        data.put("order_no", orderNo);
        data.put("pay_amount", toDecimal(order.get("pay_amount")));
        data.put("pay_params", payParams);
        return data;
    }

    public Map<String, Object> listOrders(Long userId, String status, Integer page, Integer pageSize) {
        Long uid = requireUser(userId);
        int safePage = page == null || page < 1 ? 1 : page;
        int safePageSize = pageSize == null || pageSize < 1 ? 20 : Math.min(pageSize, 100);
        int offset = (safePage - 1) * safePageSize;

        List<Integer> statusFilter = resolveStatusFilter(status);

        String baseWhere = " FROM orders WHERE user_id = ?";
        List<Object> baseArgs = new ArrayList<Object>();
        baseArgs.add(uid);
        if (!CollectionUtils.isEmpty(statusFilter)) {
            String placeholders = statusFilter.stream().map(s -> "?").collect(Collectors.joining(","));
            baseWhere += " AND status IN (" + placeholders + ")";
            baseArgs.addAll(statusFilter);
        }

        Integer total = jdbcTemplate.queryForObject("SELECT COUNT(1)" + baseWhere, Integer.class, baseArgs.toArray());
        List<Object> listArgs = new ArrayList<Object>(baseArgs);
        listArgs.add(offset);
        listArgs.add(safePageSize);
        List<Map<String, Object>> orders = jdbcTemplate.queryForList(
                "SELECT id, order_no, status, pay_status, total_amount, pay_amount, created_at, payment_time " +
                        baseWhere + " ORDER BY created_at DESC, id DESC LIMIT ?, ?",
                listArgs.toArray());

        List<Long> orderIds = new ArrayList<Long>();
        for (Map<String, Object> row : orders) {
            orderIds.add(toLong(row.get("id")));
        }

        Map<Long, List<Map<String, Object>>> itemMap = new HashMap<Long, List<Map<String, Object>>>();
        if (!orderIds.isEmpty()) {
            String placeholders = orderIds.stream().map(id -> "?").collect(Collectors.joining(","));
            List<Map<String, Object>> itemRows = jdbcTemplate.queryForList(
                    "SELECT order_id, product_title, product_image, sku_specs, quantity, unit_price " +
                            "FROM order_items WHERE order_id IN (" + placeholders + ") ORDER BY id ASC",
                    orderIds.toArray());
            for (Map<String, Object> item : itemRows) {
                Long oid = toLong(item.get("order_id"));
                List<Map<String, Object>> list = itemMap.get(oid);
                if (list == null) {
                    list = new ArrayList<Map<String, Object>>();
                    itemMap.put(oid, list);
                }
                Map<String, Object> one = new LinkedHashMap<String, Object>();
                one.put("product_title", item.get("product_title"));
                one.put("product_image", item.get("product_image"));
                one.put("sku_specs", item.get("sku_specs"));
                one.put("quantity", toInt(item.get("quantity")));
                one.put("unit_price", toDecimal(item.get("unit_price")));
                list.add(one);
            }
        }

        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> row : orders) {
            Long oid = toLong(row.get("id"));
            List<Map<String, Object>> items = itemMap.get(oid);
            if (items == null) {
                items = new ArrayList<Map<String, Object>>();
            }

            int itemCount = 0;
            for (Map<String, Object> item : items) {
                itemCount += toInt(item.get("quantity"));
            }

            Map<String, Object> one = new LinkedHashMap<String, Object>();
            one.put("id", oid);
            one.put("order_no", row.get("order_no"));
            one.put("status", toInt(row.get("status")));
            one.put("status_text", statusText(toInt(row.get("status"))));
            one.put("pay_status", toInt(row.get("pay_status")));
            one.put("total_amount", toDecimal(row.get("total_amount")));
            one.put("pay_amount", toDecimal(row.get("pay_amount")));
            one.put("items", items);
            one.put("item_count", itemCount);
            one.put("created_at", formatDateTime(row.get("created_at")));
            one.put("payment_time", formatDateTime(row.get("payment_time")));
            list.add(one);
        }

        int totalValue = total == null ? 0 : total;
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("list", list);
        data.put("total", totalValue);
        data.put("page", safePage);
        data.put("page_size", safePageSize);
        data.put("has_more", safePage * safePageSize < totalValue);
        return data;
    }

    public Map<String, Object> getOrderDetail(Long userId, Long orderId) {
        Long uid = requireUser(userId);
        Map<String, Object> order = loadOrder(uid, orderId);

        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("id", orderId);
        data.put("order_no", order.get("order_no"));
        data.put("status", toInt(order.get("status")));
        data.put("status_text", statusText(toInt(order.get("status"))));
        data.put("pay_status", toInt(order.get("pay_status")));
        data.put("order_type", toInt(order.get("order_type")));
        data.put("total_amount", toDecimal(order.get("total_amount")));
        data.put("discount_amount", toDecimal(order.get("discount_amount")));
        data.put("coupon_amount", toDecimal(order.get("coupon_amount")));
        data.put("points_amount", toDecimal(order.get("points_amount")));
        data.put("card_amount", toDecimal(order.get("card_amount")));
        data.put("freight_amount", toDecimal(order.get("freight_amount")));
        data.put("pay_amount", toDecimal(order.get("pay_amount")));
        data.put("consignee_info", parseJsonMap(safe(order.get("consignee_info"))));

        List<Map<String, Object>> items = jdbcTemplate.queryForList(
                "SELECT id, product_id, sku_id, product_title, product_image, sku_specs, unit_price, original_price, discount_amount, quantity, subtotal, refund_status " +
                        "FROM order_items WHERE order_id = ? ORDER BY id ASC",
                orderId);
        data.put("items", items);
        data.put("remark", order.get("remark"));
        data.put("created_at", formatDateTime(order.get("created_at")));
        data.put("payment_time", formatDateTime(order.get("payment_time")));
        data.put("delivery_time", formatDateTime(order.get("delivery_time")));
        data.put("finish_time", formatDateTime(order.get("finish_time")));
        data.put("cancel_time", formatDateTime(order.get("cancel_time")));
        data.put("auto_close_time", formatDateTime(order.get("auto_close_time")));

        List<Map<String, Object>> shipments = jdbcTemplate.queryForList(
                "SELECT id, shipment_no, company_name, tracking_no, ship_status, shipped_at, received_at FROM shipments WHERE order_id = ? ORDER BY id DESC",
                orderId);
        data.put("shipments", shipments);

        List<Map<String, Object>> afterSales = jdbcTemplate.queryForList(
                "SELECT id, after_sale_no, status, type, created_at FROM after_sale_orders WHERE order_id = ? ORDER BY id DESC",
                orderId);
        data.put("after_sales", afterSales);
        return data;
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long userId, Long orderId, String cancelReason) {
        Long uid = requireUser(userId);
        Map<String, Object> order = loadOrder(uid, orderId);
        int status = toInt(order.get("status"));
        if (status != ORDER_PENDING_PAY) {
            throw new BizException(BizCode.BIZ_ERROR, "仅待付款订单可取消");
        }

        jdbcTemplate.update(
                "UPDATE orders SET status = ?, cancel_time = NOW(), cancel_reason = ?, updated_at = NOW() WHERE id = ? AND user_id = ?",
                ORDER_CANCELLED,
                trim(cancelReason),
                orderId,
                uid);

        releaseStockByOrder(orderId);
        insertOrderLog(orderId, "cancel", 1, uid, "用户取消订单", null);
    }

    @Transactional(rollbackFor = Exception.class)
    public void receiveOrder(Long userId, Long orderId) {
        Long uid = requireUser(userId);
        Map<String, Object> order = loadOrder(uid, orderId);
        int status = toInt(order.get("status"));
        if (status != ORDER_PENDING_RECEIVE) {
            throw new BizException(BizCode.BIZ_ERROR, "当前订单不可确认收货");
        }

        jdbcTemplate.update(
                "UPDATE orders SET status = ?, finish_time = NOW(), updated_at = NOW() WHERE id = ? AND user_id = ?",
                ORDER_COMPLETED,
                orderId,
                uid);
        jdbcTemplate.update(
                "UPDATE shipments SET ship_status = 30, received_at = NOW(), updated_at = NOW() WHERE order_id = ? AND ship_status = 20",
                orderId);
        insertOrderLog(orderId, "receive", 1, uid, "用户确认收货", null);
    }

    public Map<String, Object> getOrderLogistics(Long userId, Long orderId) {
        Long uid = requireUser(userId);
        loadOrder(uid, orderId);

        List<Map<String, Object>> shipments = jdbcTemplate.queryForList(
                "SELECT id, shipment_no, company_name, tracking_no, ship_status, shipped_at, received_at FROM shipments WHERE order_id = ? ORDER BY id DESC",
                orderId);

        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> shipment : shipments) {
            Long shipmentId = toLong(shipment.get("id"));
            List<Map<String, Object>> tracks = jdbcTemplate.queryForList(
                    "SELECT node_time, node_status, node_content, location FROM shipment_tracks WHERE shipment_id = ? ORDER BY node_time DESC, id DESC",
                    shipmentId);
            if (tracks.isEmpty()) {
                logisticsTraceService.pullAndSaveTracks(shipmentId,
                        safe(shipment.get("company_name")),
                        safe(shipment.get("tracking_no")));
                tracks = jdbcTemplate.queryForList(
                        "SELECT node_time, node_status, node_content, location FROM shipment_tracks WHERE shipment_id = ? ORDER BY node_time DESC, id DESC",
                        shipmentId);
            }

            List<Map<String, Object>> trackList = new ArrayList<Map<String, Object>>();
            for (Map<String, Object> track : tracks) {
                Map<String, Object> node = new LinkedHashMap<String, Object>();
                node.put("node_time", formatDateTime(track.get("node_time")));
                node.put("node_status", safe(track.get("node_status")));
                node.put("node_content", safe(track.get("node_content")));
                node.put("location", safe(track.get("location")));
                trackList.add(node);
            }

            Map<String, Object> one = new LinkedHashMap<String, Object>();
            one.put("id", shipmentId);
            one.put("shipment_no", shipment.get("shipment_no"));
            one.put("company_name", shipment.get("company_name"));
            one.put("tracking_no", shipment.get("tracking_no"));
            one.put("ship_status", toInt(shipment.get("ship_status")));
            one.put("shipped_at", formatDateTime(shipment.get("shipped_at")));
            one.put("received_at", formatDateTime(shipment.get("received_at")));
            one.put("tracks", trackList);
            result.add(one);
        }

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("shipments", result);
        return data;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> applyAfterSale(Long userId, AfterSaleApplyRequest request) {
        Long uid = requireUser(userId);
        Map<String, Object> order = loadOrder(uid, request.getOrderId());
        int orderStatus = toInt(order.get("status"));
        if (orderStatus == ORDER_PENDING_PAY || orderStatus == ORDER_CANCELLED || orderStatus == ORDER_CLOSED) {
            throw new BizException(BizCode.BIZ_ERROR, "当前订单不可申请售后");
        }

        Map<String, Object> orderItem = loadOrderItem(request.getOrderId(), request.getOrderItemId());
        int itemRefundStatus = toInt(orderItem.get("refund_status"));
        if (itemRefundStatus != ORDER_ITEM_REFUND_NONE) {
            throw new BizException(BizCode.BIZ_ERROR, "该商品已在售后中");
        }

        Integer type = request.getType();
        if (type == null || (type != 1 && type != 2 && type != 3)) {
            throw new BizException(BizCode.PARAM_ERROR, "type 仅支持 1/2/3");
        }

        BigDecimal maxAmount = toDecimal(orderItem.get("subtotal"));
        BigDecimal applyAmount = request.getApplyAmount() == null ? maxAmount : request.getApplyAmount();
        if (applyAmount.compareTo(BigDecimal.ZERO) < 0 || applyAmount.compareTo(maxAmount) > 0) {
            throw new BizException(BizCode.PARAM_ERROR, "apply_amount 不合法");
        }

        ensureNoActiveAfterSale(request.getOrderId(), request.getOrderItemId());

        String afterSaleNo = generateAfterSaleNo();

        jdbcTemplate.update(
                "INSERT INTO after_sale_orders(_openid, after_sale_no, order_id, order_item_id, user_id, type, reason_code, reason_desc, evidence_urls, apply_amount, approved_amount, status, refund_status, audit_status, audit_remark, refund_channel, created_at, updated_at) " +
                        "VALUES('', ?, ?, ?, ?, ?, ?, ?, CAST(? AS JSON), ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                afterSaleNo,
                request.getOrderId(),
                request.getOrderItemId(),
                uid,
                type,
                trim(request.getReasonCode()),
                trim(request.getReasonDesc()),
                toJson(request.getEvidenceUrls() == null ? new ArrayList<String>() : request.getEvidenceUrls()),
                applyAmount,
                applyAmount,
                AFTER_SALE_PENDING_AUDIT,
                AFTER_SALE_REFUND_NONE,
                AFTER_SALE_AUDIT_PENDING,
                "待审核",
                "original");

        Long afterSaleId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        if (afterSaleId == null) {
            throw new BizException(BizCode.SYSTEM_ERROR, "售后申请失败");
        }

        jdbcTemplate.update(
                "UPDATE order_items SET refund_status = ?, updated_at = NOW() WHERE id = ?",
                ORDER_ITEM_REFUND_PARTIAL,
                request.getOrderItemId());

        insertAfterSaleLog(afterSaleId, "APPLY", 1, uid, "用户提交售后申请", null);

        jdbcTemplate.update("UPDATE orders SET status = ?, updated_at = NOW() WHERE id = ?", ORDER_AFTER_SALE, request.getOrderId());


        Map<String, Object> data = new HashMap<String, Object>();
        data.put("after_sale_id", afterSaleId);
        data.put("after_sale_no", afterSaleNo);
        return data;
    }

    public Map<String, Object> listAfterSales(Long userId, String status, Integer page, Integer pageSize) {
        Long uid = requireUser(userId);
        int safePage = page == null || page < 1 ? 1 : page;
        int safePageSize = pageSize == null || pageSize < 1 ? 20 : Math.min(pageSize, 100);
        int offset = (safePage - 1) * safePageSize;

        List<Integer> statusFilter = resolveAfterSaleStatusFilter(status);

        String fromClause = " FROM after_sale_orders aso JOIN orders o ON o.id = aso.order_id JOIN order_items oi ON oi.id = aso.order_item_id WHERE aso.user_id = ?";
        List<Object> baseArgs = new ArrayList<Object>();
        baseArgs.add(uid);
        if (!CollectionUtils.isEmpty(statusFilter)) {
            String placeholders = statusFilter.stream().map(s -> "?").collect(Collectors.joining(","));
            fromClause += " AND aso.status IN (" + placeholders + ")";
            baseArgs.addAll(statusFilter);
        }

        Integer total = jdbcTemplate.queryForObject("SELECT COUNT(1)" + fromClause, Integer.class, baseArgs.toArray());

        List<Object> listArgs = new ArrayList<Object>(baseArgs);
        listArgs.add(offset);
        listArgs.add(safePageSize);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT aso.id, aso.after_sale_no, aso.type, aso.status, aso.apply_amount, aso.created_at, " +
                        "o.order_no, oi.product_title, oi.product_image " +
                        fromClause +
                        " ORDER BY aso.created_at DESC, aso.id DESC LIMIT ?, ?",
                listArgs.toArray());

        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> order = new LinkedHashMap<String, Object>();
            order.put("order_no", safe(row.get("order_no")));
            order.put("product_title", safe(row.get("product_title")));
            order.put("product_image", safe(row.get("product_image")));

            int type = toInt(row.get("type"));
            int oneStatus = toInt(row.get("status"));
            Map<String, Object> one = new LinkedHashMap<String, Object>();
            one.put("id", toLong(row.get("id")));
            one.put("after_sale_no", safe(row.get("after_sale_no")));
            one.put("type", type);
            one.put("type_text", afterSaleTypeText(type));
            one.put("status", oneStatus);
            one.put("status_text", afterSaleStatusText(oneStatus));
            one.put("order", order);
            one.put("apply_amount", toDecimal(row.get("apply_amount")));
            one.put("created_at", formatDateTime(row.get("created_at")));
            list.add(one);
        }

        int totalValue = total == null ? 0 : total;
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("list", list);
        data.put("total", totalValue);
        data.put("page", safePage);
        data.put("page_size", safePageSize);
        data.put("has_more", safePage * safePageSize < totalValue);
        return data;
    }

    public Map<String, Object> getAfterSaleDetail(Long userId, Long afterSaleId) {
        Long uid = requireUser(userId);
        Map<String, Object> afterSale = loadAfterSale(uid, afterSaleId);

        Map<String, Object> order = jdbcTemplate.queryForMap(
                "SELECT o.id, o.order_no, oi.product_title, oi.product_image, oi.sku_specs, oi.quantity, oi.unit_price " +
                        "FROM orders o JOIN order_items oi ON oi.order_id = o.id AND oi.id = ? WHERE o.id = ?",
                toLong(afterSale.get("order_item_id")),
                toLong(afterSale.get("order_id")));

        List<Map<String, Object>> logs = jdbcTemplate.queryForList(
                "SELECT action, remark, created_at FROM after_sale_logs WHERE after_sale_id = ? ORDER BY created_at ASC, id ASC",
                afterSaleId);

        List<Map<String, Object>> logList = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> log : logs) {
            Map<String, Object> one = new LinkedHashMap<String, Object>();
            one.put("action", safe(log.get("action")));
            one.put("remark", safe(log.get("remark")));
            one.put("created_at", formatDateTime(log.get("created_at")));
            logList.add(one);
        }

        List<Map<String, Object>> returnShipments = jdbcTemplate.queryForList(
                "SELECT company_name, tracking_no, status, shipped_at, received_at FROM after_sale_return_shipments WHERE after_sale_id = ? ORDER BY id DESC LIMIT 1",
                afterSaleId);

        Map<String, Object> returnShipment = null;
        if (!returnShipments.isEmpty()) {
            Map<String, Object> latest = returnShipments.get(0);
            returnShipment = new LinkedHashMap<String, Object>();
            returnShipment.put("company_name", safe(latest.get("company_name")));
            returnShipment.put("tracking_no", safe(latest.get("tracking_no")));
            returnShipment.put("status", toInt(latest.get("status")));
            returnShipment.put("shipped_at", formatDateTime(latest.get("shipped_at")));
            returnShipment.put("received_at", formatDateTime(latest.get("received_at")));
        }

        Map<String, Object> orderData = new LinkedHashMap<String, Object>();
        orderData.put("id", toLong(afterSale.get("order_id")));
        orderData.put("order_no", safe(order.get("order_no")));
        orderData.put("product_title", safe(order.get("product_title")));
        orderData.put("product_image", safe(order.get("product_image")));
        orderData.put("sku_specs", safe(order.get("sku_specs")));
        orderData.put("quantity", toInt(order.get("quantity")));
        orderData.put("unit_price", toDecimal(order.get("unit_price")));

        Map<String, Object> data = new LinkedHashMap<String, Object>();
        int type = toInt(afterSale.get("type"));
        int oneStatus = toInt(afterSale.get("status"));
        data.put("id", afterSaleId);
        data.put("after_sale_no", safe(afterSale.get("after_sale_no")));
        data.put("type", type);
        data.put("type_text", afterSaleTypeText(type));
        data.put("status", oneStatus);
        data.put("status_text", afterSaleStatusText(oneStatus));
        data.put("order", orderData);
        data.put("reason_code", safe(afterSale.get("reason_code")));
        data.put("reason_desc", safe(afterSale.get("reason_desc")));
        data.put("evidence_urls", parseJsonStringList(safe(afterSale.get("evidence_urls"))));
        data.put("apply_amount", toDecimal(afterSale.get("apply_amount")));
        data.put("audit_status", toInt(afterSale.get("audit_status")));
        data.put("audit_remark", safe(afterSale.get("audit_remark")));

        Map<String, Object> returnAddress = new LinkedHashMap<String, Object>();
        returnAddress.put("consignee", "诗语家居售后部");
        returnAddress.put("phone", "15204083071");
        returnAddress.put("full_address", "江苏省苏州市工业园区星湖街328号");
        data.put("return_address", returnAddress);

        data.put("return_shipment", returnShipment);
        data.put("created_at", formatDateTime(afterSale.get("created_at")));
        data.put("logs", logList);
        return data;
    }

    @Transactional(rollbackFor = Exception.class)
    public void auditAfterSale(Long userId, Long afterSaleId, Integer action, String remark) {
        Long uid = requireUser(userId);
        Map<String, Object> afterSale = loadAfterSaleById(afterSaleId);
        int status = toInt(afterSale.get("status"));
        if (status != AFTER_SALE_PENDING_AUDIT) {
            throw new BizException(BizCode.BIZ_ERROR, "当前售后单不可审核");
        }

        String finalRemark = trim(remark);
        if (finalRemark == null) {
            finalRemark = action != null && action == 1 ? "审核通过" : "审核驳回";
        }

        if (action != null && action == 1) {
            int type = toInt(afterSale.get("type"));
            int nextStatus = type == 1 ? AFTER_SALE_REFUNDING : AFTER_SALE_PENDING_RETURN;
            int refundStatus = type == 1 ? AFTER_SALE_REFUNDING_FLAG : AFTER_SALE_REFUND_NONE;
            jdbcTemplate.update(
                    "UPDATE after_sale_orders SET status = ?, refund_status = ?, audit_status = ?, audit_remark = ?, updated_at = NOW() WHERE id = ?",
                    nextStatus,
                    refundStatus,
                    AFTER_SALE_AUDIT_APPROVED,
                    finalRemark,
                    afterSaleId);
            insertAfterSaleLog(afterSaleId, "APPROVE", 2, uid, finalRemark, null);

            if (type == 1) {
                triggerAfterSaleRefundCompensation(afterSaleId, "audit_approve");
            }
        } else if (action != null && action == 2) {
            jdbcTemplate.update(
                    "UPDATE after_sale_orders SET status = ?, audit_status = ?, reject_reason = ?, audit_remark = ?, close_time = NOW(), updated_at = NOW() WHERE id = ?",
                    AFTER_SALE_REJECTED,
                    AFTER_SALE_AUDIT_REJECTED,
                    finalRemark,
                    finalRemark,
                    afterSaleId);
            jdbcTemplate.update(
                    "UPDATE order_items SET refund_status = ?, updated_at = NOW() WHERE id = ?",
                    ORDER_ITEM_REFUND_NONE,
                    toLong(afterSale.get("order_item_id")));
            insertAfterSaleLog(afterSaleId, "REJECT", 2, uid, finalRemark, null);
            refreshOrderStatusAfterAfterSale(toLong(afterSale.get("order_id")));
        } else {
            throw new BizException(BizCode.PARAM_ERROR, "action 仅支持 1/2");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void submitAfterSaleReturn(Long userId, Long afterSaleId, AfterSaleReturnRequest request) {
        Long uid = requireUser(userId);
        Map<String, Object> afterSale = loadAfterSale(uid, afterSaleId);
        int status = toInt(afterSale.get("status"));
        if (status != AFTER_SALE_PENDING_RETURN) {
            throw new BizException(BizCode.BIZ_ERROR, "当前售后单不可填写退货物流");
        }

        Map<String, Object> addressSnapshot = new LinkedHashMap<String, Object>();
        addressSnapshot.put("consignee", "诗语家居售后部");
        addressSnapshot.put("phone", "15204083071");
        addressSnapshot.put("full_address", "江苏省苏州市工业园区星湖街328号");

        jdbcTemplate.update(
                "INSERT INTO after_sale_return_shipments(_openid, after_sale_id, sender_type, company_name, tracking_no, address_snapshot, shipped_at, status, created_at, updated_at) " +
                        "VALUES('', ?, 1, ?, ?, CAST(? AS JSON), NOW(), 20, NOW(), NOW())",
                afterSaleId,
                trim(request.getCompanyName()),
                trim(request.getTrackingNo()),
                toJson(addressSnapshot));

        jdbcTemplate.update(
                "UPDATE after_sale_orders SET status = ?, updated_at = NOW() WHERE id = ?",
                AFTER_SALE_PENDING_RECEIVE,
                afterSaleId);

        insertAfterSaleLog(afterSaleId, "RETURN_SUBMIT", 1, uid, "用户已提交退货物流", null);

        jdbcTemplate.update(
                "UPDATE after_sale_orders SET status = ?, refund_status = ?, updated_at = NOW() WHERE id = ?",
                AFTER_SALE_REFUNDING,
                AFTER_SALE_REFUNDING_FLAG,
                afterSaleId);
        insertAfterSaleLog(afterSaleId, "RECEIVE_CONFIRM", 3, 0L, "系统确认收货，进入退款流程", null);
        triggerAfterSaleRefundCompensation(afterSaleId, "return_submit");
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancelAfterSale(Long userId, Long afterSaleId) {
        Long uid = requireUser(userId);
        Map<String, Object> afterSale = loadAfterSale(uid, afterSaleId);
        int status = toInt(afterSale.get("status"));
        if (status != AFTER_SALE_PENDING_AUDIT && status != AFTER_SALE_PENDING_RETURN && status != AFTER_SALE_PENDING_RECEIVE) {
            throw new BizException(BizCode.BIZ_ERROR, "当前售后单不可取消");
        }

        jdbcTemplate.update(
                "UPDATE after_sale_orders SET status = ?, audit_status = ?, reject_reason = ?, close_time = NOW(), updated_at = NOW() WHERE id = ?",
                AFTER_SALE_REJECTED,
                AFTER_SALE_AUDIT_REJECTED,
                "用户主动取消",
                afterSaleId);

        jdbcTemplate.update(
                "UPDATE order_items SET refund_status = ?, updated_at = NOW() WHERE id = ?",
                ORDER_ITEM_REFUND_NONE,
                toLong(afterSale.get("order_item_id")));

        insertAfterSaleLog(afterSaleId, "CANCEL", 1, uid, "用户取消售后申请", null);
        refreshOrderStatusAfterAfterSale(toLong(afterSale.get("order_id")));
    }

    private List<Map<String, Object>> resolveConfirmItems(Long userId, OrderConfirmRequest request) {
        List<Map<String, Object>> rows;
        if (!CollectionUtils.isEmpty(request.getCartIds())) {
            String placeholders = request.getCartIds().stream().map(id -> "?").collect(Collectors.joining(","));
            List<Object> args = new ArrayList<Object>();
            args.add(userId);
            args.addAll(request.getCartIds());
            rows = jdbcTemplate.queryForList(
                    "SELECT ci.product_id, ci.sku_id, ci.quantity, p.title, p.main_image, p.status AS product_status, p.deleted AS product_deleted, " +
                            "ps.specs_json, ps.price, ps.stock, ps.status AS sku_status " +
                            "FROM cart_items ci JOIN products p ON p.id = ci.product_id JOIN product_skus ps ON ps.id = ci.sku_id " +
                            "WHERE ci.user_id = ? AND ci.id IN (" + placeholders + ")",
                    args.toArray());
        } else if (request.getProductId() != null && request.getSkuId() != null && request.getQuantity() != null && request.getQuantity() > 0) {
            rows = jdbcTemplate.queryForList(
                    "SELECT ? AS product_id, ? AS sku_id, ? AS quantity, p.title, p.main_image, p.status AS product_status, p.deleted AS product_deleted, " +
                            "ps.specs_json, ps.price, ps.stock, ps.status AS sku_status " +
                            "FROM products p JOIN product_skus ps ON ps.id = ? AND ps.product_id = p.id WHERE p.id = ?",
                    request.getProductId(), request.getSkuId(), request.getQuantity(), request.getSkuId(), request.getProductId());
        } else {
            throw new BizException(BizCode.PARAM_ERROR, "cart_ids 或 立即购买参数必须传入");
        }

        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> row : rows) {
            int productStatus = toInt(row.get("product_status"));
            int productDeleted = toInt(row.get("product_deleted"));
            int skuStatus = toInt(row.get("sku_status"));
            int stock = toInt(row.get("stock"));
            int quantity = toInt(row.get("quantity"));
            if (productDeleted == 1 || productStatus != 1) {
                throw new BizException(BizCode.BIZ_ERROR, "存在已下架商品，无法结算");
            }
            if (skuStatus != 1) {
                throw new BizException(BizCode.BIZ_ERROR, "存在失效规格，无法结算");
            }
            if (quantity > stock) {
                throw new BizException(BizCode.BIZ_ERROR, "存在库存不足商品，无法结算");
            }

            BigDecimal unitPrice = toDecimal(row.get("price"));
            BigDecimal subtotal = unitPrice.multiply(new BigDecimal(quantity)).setScale(2, RoundingMode.HALF_UP);

            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("product_id", toLong(row.get("product_id")));
            item.put("sku_id", toLong(row.get("sku_id")));
            item.put("product_title", row.get("title"));
            item.put("product_image", row.get("main_image"));
            item.put("sku_specs", toSpecText(String.valueOf(row.get("specs_json"))));
            item.put("unit_price", unitPrice);
            item.put("quantity", quantity);
            item.put("subtotal", subtotal);
            items.add(item);
        }
        return items;
    }

    private Map<String, Object> loadAddress(Long userId, Long addressId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, consignee, phone_mask, province, city, district, street, detail_address FROM user_addresses " +
                        "WHERE id = ? AND user_id = ? AND deleted = 0 LIMIT 1",
                addressId, userId);
        if (rows.isEmpty()) {
            throw new BizException(BizCode.RESOURCE_NOT_FOUND, "收货地址不存在");
        }
        Map<String, Object> row = rows.get(0);
        Map<String, Object> address = new LinkedHashMap<String, Object>();
        address.put("id", row.get("id"));
        address.put("consignee", row.get("consignee"));
        address.put("phone_mask", row.get("phone_mask"));
        address.put("full_address", (safe(row.get("province")) + safe(row.get("city")) + safe(row.get("district")) +
                safe(row.get("street")) + safe(row.get("detail_address"))));
        return address;
    }

    private BigDecimal calculateCouponAmount(Long userId, Long userCouponId, BigDecimal totalAmount) {
        if (userCouponId == null) {
            return BigDecimal.ZERO;
        }
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT c.type, c.value, c.threshold FROM user_coupons uc JOIN coupons c ON c.id = uc.coupon_id " +
                        "WHERE uc.id = ? AND uc.user_id = ? AND uc.status = 0 AND c.status = 1 AND uc.expire_time >= NOW() LIMIT 1",
                userCouponId, userId);
        if (rows.isEmpty()) {
            return BigDecimal.ZERO;
        }
        Map<String, Object> row = rows.get(0);
        BigDecimal threshold = toDecimal(row.get("threshold"));
        if (totalAmount.compareTo(threshold) < 0) {
            return BigDecimal.ZERO;
        }
        int type = toInt(row.get("type"));
        BigDecimal value = toDecimal(row.get("value"));
        if (type == 1) {
            return value.min(totalAmount).setScale(2, RoundingMode.HALF_UP);
        }
        if (type == 2) {
            BigDecimal discount = totalAmount.multiply(new BigDecimal("100").subtract(value)).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            return discount.max(BigDecimal.ZERO);
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal calculateCardAmount(Long userId, Long cardId, BigDecimal remainAmount) {
        if (cardId == null || remainAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT balance FROM stored_value_cards WHERE id = ? AND user_id = ? AND status = 2 LIMIT 1",
                cardId, userId);
        if (rows.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal balance = toDecimal(rows.get(0).get("balance"));
        return balance.min(remainAmount).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    private List<Map<String, Object>> loadAvailableCoupons(Long userId, BigDecimal totalAmount) {
        return jdbcTemplate.queryForList(
                "SELECT uc.id, c.name, c.value, c.threshold, DATE_FORMAT(c.start_time, '%Y-%m-%d %H:%i:%s') AS start_time, " +
                        "DATE_FORMAT(c.end_time, '%Y-%m-%d %H:%i:%s') AS end_time " +
                        "FROM user_coupons uc JOIN coupons c ON c.id = uc.coupon_id " +
                        "WHERE uc.user_id = ? AND uc.status = 0 AND c.status = 1 AND uc.expire_time >= NOW() AND c.threshold <= ? " +
                        "ORDER BY c.value DESC, uc.id DESC",
                userId, totalAmount);
    }

    private int getUserPoints(Long userId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT COALESCE(available_points,0) AS available_points FROM user_points_accounts WHERE user_id = ? LIMIT 1",
                userId);
        if (rows.isEmpty()) {
            jdbcTemplate.update(
                    "INSERT INTO user_points_accounts(_openid, user_id, total_points, available_points, frozen_points, expire_points, updated_at) " +
                            "VALUES('', ?, 0, 0, 0, 0, NOW()) ON DUPLICATE KEY UPDATE updated_at = NOW()",
                    userId);
            return 0;
        }
        Number points = (Number) rows.get(0).get("available_points");
        return points == null ? 0 : Math.max(points.intValue(), 0);
    }

    private Map<String, Object> loadOrder(Long userId, Long orderId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, order_no, user_id, order_type, total_amount, discount_amount, coupon_amount, points_amount, card_amount, " +
                        "freight_amount, pay_amount, status, pay_status, source_type, pay_channel, consignee_info, price_snapshot, remark, " +
                        "payment_time, delivery_time, finish_time, cancel_time, auto_close_time, created_at " +
                        "FROM orders WHERE id = ? AND user_id = ? LIMIT 1",
                orderId,
                userId);
        if (rows.isEmpty()) {
            throw new BizException(BizCode.RESOURCE_NOT_FOUND, "订单不存在");
        }
        return rows.get(0);
    }

    private void lockStock(String orderNo, Long orderId, Long skuId, Integer quantity, LocalDateTime expireAt) {
        Map<String, Object> sku = jdbcTemplate.queryForMap("SELECT stock, locked_stock FROM product_skus WHERE id = ? FOR UPDATE", skuId);
        int stockBefore = toInt(sku.get("stock"));
        int lockedBefore = toInt(sku.get("locked_stock"));
        if (stockBefore < quantity) {
            throw new BizException(BizCode.BIZ_ERROR, "库存不足");
        }

        jdbcTemplate.update(
                "UPDATE product_skus SET stock = stock - ?, locked_stock = locked_stock + ?, updated_at = NOW() WHERE id = ?",
                quantity,
                quantity,
                skuId);

        jdbcTemplate.update(
                "INSERT INTO stock_reservations(_openid, order_no, order_id, sku_id, locked_qty, status, expire_at, created_at, updated_at) " +
                        "VALUES('', ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                orderNo,
                orderId,
                skuId,
                quantity,
                LOCKED,
                Timestamp.valueOf(expireAt));

        jdbcTemplate.update(
                "INSERT INTO inventory_logs(_openid, sku_id, change_type, change_qty, stock_before, stock_after, locked_before, locked_after, business_type, business_id, remark, created_at) " +
                        "VALUES('', ?, 2, ?, ?, ?, ?, ?, 'order_create', ?, ?, NOW())",
                skuId,
                -quantity,
                stockBefore,
                stockBefore - quantity,
                lockedBefore,
                lockedBefore + quantity,
                orderId,
                "下单锁定库存");
    }

    private void releaseStockByOrder(Long orderId) {
        List<Map<String, Object>> reservations = jdbcTemplate.queryForList(
                "SELECT id, sku_id, locked_qty FROM stock_reservations WHERE order_id = ? AND status = ?",
                orderId,
                LOCKED);

        for (Map<String, Object> reservation : reservations) {
            Long reservationId = toLong(reservation.get("id"));
            Long skuId = toLong(reservation.get("sku_id"));
            Integer qty = toInt(reservation.get("locked_qty"));

            Map<String, Object> sku = jdbcTemplate.queryForMap("SELECT stock, locked_stock FROM product_skus WHERE id = ? FOR UPDATE", skuId);
            int stockBefore = toInt(sku.get("stock"));
            int lockedBefore = toInt(sku.get("locked_stock"));
            if (lockedBefore < qty) {
                throw new BizException(BizCode.BIZ_ERROR, "锁定库存异常");
            }

            jdbcTemplate.update(
                    "UPDATE product_skus SET stock = stock + ?, locked_stock = locked_stock - ?, updated_at = NOW() WHERE id = ?",
                    qty,
                    qty,
                    skuId);

            jdbcTemplate.update(
                    "UPDATE stock_reservations SET status = ?, released_at = NOW(), updated_at = NOW() WHERE id = ?",
                    RELEASED,
                    reservationId);

            jdbcTemplate.update(
                    "INSERT INTO inventory_logs(_openid, sku_id, change_type, change_qty, stock_before, stock_after, locked_before, locked_after, business_type, business_id, remark, created_at) " +
                            "VALUES('', ?, 3, ?, ?, ?, ?, ?, 'order_cancel', ?, ?, NOW())",
                    skuId,
                    qty,
                    stockBefore,
                    stockBefore + qty,
                    lockedBefore,
                    lockedBefore - qty,
                    orderId,
                    "取消释放库存");
        }
    }

    private Map<Long, Map<String, Object>> loadSkuMeta(Set<Long> skuIds) {
        if (CollectionUtils.isEmpty(skuIds)) {
            return new HashMap<Long, Map<String, Object>>();
        }
        String placeholders = skuIds.stream().map(id -> "?").collect(Collectors.joining(","));
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, sku_code, original_price FROM product_skus WHERE id IN (" + placeholders + ")",
                skuIds.toArray());
        Map<Long, Map<String, Object>> map = new HashMap<Long, Map<String, Object>>();
        for (Map<String, Object> row : rows) {
            map.put(toLong(row.get("id")), row);
        }
        return map;
    }

    private void insertOrderLog(Long orderId, String action, Integer operatorType, Long operatorId, String remark, String extJson) {
        jdbcTemplate.update(
                "INSERT INTO order_logs(_openid, order_id, action, operator_type, operator_id, remark, ext_json, created_at) " +
                        "VALUES('', ?, ?, ?, ?, ?, CAST(? AS JSON), NOW())",
                orderId,
                action,
                operatorType,
                operatorId,
                remark,
                extJson == null ? "{}" : extJson);
    }

    private List<Integer> resolveStatusFilter(String status) {
        String value = trim(status);
        if (value == null || "all".equalsIgnoreCase(value)) {
            return null;
        }
        if ("pending".equalsIgnoreCase(value) || "to_pay".equalsIgnoreCase(value)) {
            return Collections.singletonList(ORDER_PENDING_PAY);
        }
        if ("to_ship".equalsIgnoreCase(value)) {
            return Collections.singletonList(ORDER_PENDING_SHIP);
        }
        if ("shipped".equalsIgnoreCase(value) || "to_receive".equalsIgnoreCase(value)) {
            return Collections.singletonList(ORDER_PENDING_RECEIVE);
        }
        if ("received".equalsIgnoreCase(value) || "completed".equalsIgnoreCase(value)) {
            return Collections.singletonList(ORDER_COMPLETED);
        }
        if ("cancelled".equalsIgnoreCase(value)) {
            List<Integer> list = new ArrayList<Integer>();
            list.add(ORDER_CANCELLED);
            list.add(ORDER_CLOSED);
            return list;
        }
        if ("after_sale".equalsIgnoreCase(value)) {
            return Collections.singletonList(ORDER_AFTER_SALE);
        }
        throw new BizException(BizCode.PARAM_ERROR, "status 参数不合法");
    }


    private String statusText(int status) {
        switch (status) {
            case ORDER_PENDING_PAY:
                return "待付款";
            case ORDER_PENDING_SHIP:
                return "待发货";
            case ORDER_PENDING_RECEIVE:
                return "待收货";
            case ORDER_COMPLETED:
                return "已完成";
            case ORDER_CANCELLED:
                return "已取消";
            case ORDER_AFTER_SALE:
                return "售后中";
            case ORDER_CLOSED:
                return "已关闭";
            default:
                return "未知";
        }
    }

    private String generateOrderNo() {
        return "ORD" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) +
                ThreadLocalRandom.current().nextInt(1000, 9999);
    }

    private String generateOutTradeNo() {
        return "PAY" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) +
                ThreadLocalRandom.current().nextInt(1000, 9999);
    }

    private String toJson(Object data) {
        try {
            return objectMapper.writeValueAsString(data == null ? new HashMap<String, Object>() : data);
        } catch (Exception ex) {
            throw new BizException(BizCode.SYSTEM_ERROR, "JSON 序列化失败");
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object value) {
        if (value == null) {
            return new HashMap<String, Object>();
        }
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> castListMap(Object value) {
        if (value == null) {
            return new ArrayList<Map<String, Object>>();
        }
        return (List<Map<String, Object>>) value;
    }

    private Map<String, Object> loadOrderItem(Long orderId, Long orderItemId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, order_id, subtotal, refund_status FROM order_items WHERE id = ? AND order_id = ? LIMIT 1",
                orderItemId,
                orderId);
        if (rows.isEmpty()) {
            throw new BizException(BizCode.RESOURCE_NOT_FOUND, "订单商品不存在");
        }
        return rows.get(0);
    }

    private Map<String, Object> loadAfterSale(Long userId, Long afterSaleId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, after_sale_no, order_id, order_item_id, user_id, type, reason_code, reason_desc, evidence_urls, apply_amount, approved_amount, status, refund_status, audit_status, audit_remark, created_at " +
                        "FROM after_sale_orders WHERE id = ? AND user_id = ? LIMIT 1",
                afterSaleId,
                userId);
        if (rows.isEmpty()) {
            throw new BizException(BizCode.RESOURCE_NOT_FOUND, "售后单不存在");
        }
        return rows.get(0);
    }

    private Map<String, Object> loadAfterSaleById(Long afterSaleId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, after_sale_no, order_id, order_item_id, user_id, type, reason_code, reason_desc, evidence_urls, apply_amount, approved_amount, status, refund_status, audit_status, audit_remark, created_at " +
                        "FROM after_sale_orders WHERE id = ? LIMIT 1",
                afterSaleId);
        if (rows.isEmpty()) {
            throw new BizException(BizCode.RESOURCE_NOT_FOUND, "售后单不存在");
        }
        return rows.get(0);
    }

    private void ensureNoActiveAfterSale(Long orderId, Long orderItemId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM after_sale_orders WHERE order_id = ? AND order_item_id = ? AND status IN (?, ?, ?, ?)",
                Integer.class,
                orderId,
                orderItemId,
                AFTER_SALE_PENDING_AUDIT,
                AFTER_SALE_PENDING_RETURN,
                AFTER_SALE_PENDING_RECEIVE,
                AFTER_SALE_REFUNDING);
        if (count != null && count > 0) {
            throw new BizException(BizCode.BIZ_ERROR, "该商品已有进行中的售后");
        }
    }

    private void insertAfterSaleLog(Long afterSaleId, String action, Integer operatorType, Long operatorId, String remark, String extJson) {
        jdbcTemplate.update(
                "INSERT INTO after_sale_logs(_openid, after_sale_id, action, operator_type, operator_id, remark, ext_json, created_at) " +
                        "VALUES('', ?, ?, ?, ?, ?, CAST(? AS JSON), NOW())",
                afterSaleId,
                action,
                operatorType,
                operatorId,
                remark,
                extJson == null ? "{}" : extJson);
    }

    private void triggerAfterSaleRefundCompensation(Long afterSaleId, String source) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("after_sale_id", afterSaleId);
        payload.put("source", source);
        payload.put("trigger_time", formatDateTime(LocalDateTime.now()));
        afterSaleCompensationProducer.sendRefundCompensation(payload);
    }

    @Transactional(rollbackFor = Exception.class)
    public void processAfterSaleRefundCompensation(Long afterSaleId, String source, String remark) {
        Map<String, Object> afterSale = loadAfterSaleById(afterSaleId);
        int status = toInt(afterSale.get("status"));
        if (status == AFTER_SALE_REFUNDED || status == AFTER_SALE_COMPLETED) {
            return;
        }
        if (status != AFTER_SALE_REFUNDING) {
            throw new BizException(BizCode.BIZ_ERROR, "当前售后单不在退款中");
        }

        Long orderId = toLong(afterSale.get("order_id"));
        Long orderItemId = toLong(afterSale.get("order_item_id"));
        BigDecimal amount = toDecimal(afterSale.get("approved_amount"));
        String finalRemark = trim(remark);
        if (finalRemark == null) {
            finalRemark = "异步补偿退款成功";
        }

        markAfterSaleRefundSuccess(afterSaleId, orderId, orderItemId, amount, 0L, finalRemark + "(" + safe(source) + ")");
    }

    private void markAfterSaleRefundSuccess(Long afterSaleId, Long orderId, Long orderItemId, BigDecimal amount, Long userId, String remark) {
        jdbcTemplate.update(
                "UPDATE after_sale_orders SET status = ?, refund_status = ?, refund_time = NOW(), close_time = NOW(), updated_at = NOW() WHERE id = ?",
                AFTER_SALE_REFUNDED,
                AFTER_SALE_REFUNDED_FLAG,
                afterSaleId);

        jdbcTemplate.update(
                "UPDATE order_items SET refund_status = ?, updated_at = NOW() WHERE id = ?",
                ORDER_ITEM_REFUND_DONE,
                orderItemId);

        insertAfterSaleLog(afterSaleId, "REFUND_SUCCESS", 3, 0L, remark, toJson(Collections.singletonMap("refund_amount", amount)));
        refreshOrderStatusAfterAfterSale(orderId);
        insertOrderLog(orderId, "after_sale_refund", 3, userId == null ? 0L : userId, "售后退款完成", null);
    }

    private void refreshOrderStatusAfterAfterSale(Long orderId) {
        Integer activeCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM after_sale_orders WHERE order_id = ? AND status IN (?, ?, ?, ?)",
                Integer.class,
                orderId,
                AFTER_SALE_PENDING_AUDIT,
                AFTER_SALE_PENDING_RETURN,
                AFTER_SALE_PENDING_RECEIVE,
                AFTER_SALE_REFUNDING);
        if (activeCount != null && activeCount > 0) {
            jdbcTemplate.update("UPDATE orders SET status = ?, updated_at = NOW() WHERE id = ?", ORDER_AFTER_SALE, orderId);
            return;
        }

        Map<String, Object> order = jdbcTemplate.queryForMap(
                "SELECT id, pay_status, finish_time, cancel_time FROM orders WHERE id = ?",
                orderId);
        int nextStatus;
        if (order.get("cancel_time") != null) {
            nextStatus = ORDER_CANCELLED;
        } else if (toInt(order.get("pay_status")) != PAY_SUCCESS) {
            nextStatus = ORDER_PENDING_PAY;
        } else if (order.get("finish_time") != null) {
            nextStatus = ORDER_COMPLETED;
        } else {
            Integer shipping = jdbcTemplate.queryForObject(
                    "SELECT COUNT(1) FROM shipments WHERE order_id = ? AND ship_status = 20",
                    Integer.class,
                    orderId);
            nextStatus = (shipping != null && shipping > 0) ? ORDER_PENDING_RECEIVE : ORDER_PENDING_SHIP;
        }
        jdbcTemplate.update("UPDATE orders SET status = ?, updated_at = NOW() WHERE id = ?", nextStatus, orderId);
    }

    private List<Integer> resolveAfterSaleStatusFilter(String status) {
        String value = trim(status);
        if (value == null) {
            return null;
        }
        if ("pending".equalsIgnoreCase(value)) {
            return Collections.singletonList(AFTER_SALE_PENDING_AUDIT);
        }
        if ("returning".equalsIgnoreCase(value)) {
            List<Integer> list = new ArrayList<Integer>();
            list.add(AFTER_SALE_PENDING_RETURN);
            list.add(AFTER_SALE_PENDING_RECEIVE);
            return list;
        }
        if ("refunding".equalsIgnoreCase(value)) {
            return Collections.singletonList(AFTER_SALE_REFUNDING);
        }
        if ("completed".equalsIgnoreCase(value)) {
            List<Integer> list = new ArrayList<Integer>();
            list.add(AFTER_SALE_REFUNDED);
            list.add(AFTER_SALE_COMPLETED);
            return list;
        }
        if ("rejected".equalsIgnoreCase(value)) {
            return Collections.singletonList(AFTER_SALE_REJECTED);
        }
        throw new BizException(BizCode.PARAM_ERROR, "status 参数不合法");
    }

    private String afterSaleTypeText(int type) {
        switch (type) {
            case 1:
                return "仅退款";
            case 2:
                return "退货退款";
            case 3:
                return "换货";
            default:
                return "未知";
        }
    }

    private String afterSaleStatusText(int status) {
        switch (status) {
            case AFTER_SALE_PENDING_AUDIT:
                return "审核中";
            case AFTER_SALE_PENDING_RETURN:
                return "待退货";
            case AFTER_SALE_PENDING_RECEIVE:
                return "商家收货中";
            case AFTER_SALE_REFUNDING:
                return "退款中";
            case AFTER_SALE_REFUNDED:
                return "已退款";
            case AFTER_SALE_COMPLETED:
                return "已完成";
            case AFTER_SALE_REJECTED:
                return "已驳回";
            default:
                return "未知";
        }
    }

    private String generateAfterSaleNo() {
        return "AS" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) +
                ThreadLocalRandom.current().nextInt(1000, 9999);
    }

    private void validateSku(Long productId, Long skuId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT p.status AS product_status, p.deleted AS product_deleted, ps.status AS sku_status, ps.stock " +
                        "FROM products p JOIN product_skus ps ON ps.id = ? AND ps.product_id = p.id WHERE p.id = ? LIMIT 1",
                skuId, productId);
        if (rows.isEmpty()) {
            throw new BizException(BizCode.RESOURCE_NOT_FOUND, "商品或规格不存在");
        }
        Map<String, Object> row = rows.get(0);
        if (toInt(row.get("product_deleted")) == 1 || toInt(row.get("product_status")) != 1 || toInt(row.get("sku_status")) != 1) {
            throw new BizException(BizCode.BIZ_ERROR, "商品已下架或规格失效");
        }
        if (toInt(row.get("stock")) <= 0) {
            throw new BizException(BizCode.BIZ_ERROR, "库存不足");
        }
    }

    private void ensureCartOwner(Long userId, Long cartId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM cart_items WHERE id = ? AND user_id = ?",
                Integer.class,
                cartId,
                userId);
        if (count == null || count <= 0) {
            throw new BizException(BizCode.RESOURCE_NOT_FOUND, "购物车项不存在");
        }
    }

    private void ensureAddressOwner(Long userId, Long addressId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM user_addresses WHERE id = ? AND user_id = ? AND deleted = 0",
                Integer.class,
                addressId,
                userId);
        if (count == null || count <= 0) {
            throw new BizException(BizCode.RESOURCE_NOT_FOUND, "地址不存在");
        }
    }

    private void clearDefaultAddress(Long userId) {
        jdbcTemplate.update("UPDATE user_addresses SET is_default = 0, updated_at = NOW() WHERE user_id = ? AND deleted = 0", userId);
    }

    private Map<String, Object> categorySimple(Integer categoryId) {
        if (categoryId == null) {
            return new HashMap<String, Object>();
        }
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, name FROM categories WHERE id = ? LIMIT 1",
                categoryId);
        if (rows.isEmpty()) {
            return new HashMap<String, Object>();
        }
        return rows.get(0);
    }

    private void ensureUser(Long userId) {

        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM users WHERE id = ?", Integer.class, userId);
        if (count != null && count > 0) {
            return;
        }
        String openid = "mock_openid_" + userId;
        jdbcTemplate.update(
                "INSERT INTO users(id, _openid, openid, nickname, avatar_url, phone_mask, gender, member_level_code, status, register_source, last_login_at, last_active_at, created_at, updated_at) " +
                        "VALUES(?, ?, ?, '诗语访客', 'https://cyberas-12.oss-cn-shanghai.aliyuncs.com/YHome/logo.png', '138****8888', 0, 'silver', 1, 'wx_miniapp', NOW(), NOW(), NOW(), NOW())",
                userId, openid, openid);
    }

    private Long requireUser(Long userId) {
        if (userId == null) {
            throw new BizException(BizCode.AUTH_FAIL, "用户未登录");
        }
        return userId;
    }

    private Integer normalizeDefault(Integer isDefault) {
        return isDefault != null && isDefault == 1 ? 1 : 0;
    }

    private String maskPhone(String phone) {
        String p = phone == null ? "" : phone.trim();
        if (p.length() < 7) {
            return p;
        }
        return p.substring(0, 3) + "****" + p.substring(p.length() - 4);
    }

    private String trim(String value) {
        if (value == null) {
            return null;
        }
        String v = value.trim();
        return v.isEmpty() ? null : v;
    }

    private int toInt(Object value) {
        if (value == null) {
            return 0;
        }
        return ((Number) value).intValue();
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        return ((Number) value).longValue();
    }

    private BigDecimal toDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        return new BigDecimal(String.valueOf(value));
    }

    private Map<String, Object> parseJsonMap(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception ex) {
            return new HashMap<String, Object>();
        }
    }

    private List<String> parseJsonStringList(String json) {
        if (json == null || json.trim().isEmpty()) {
            return new ArrayList<String>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (Exception ex) {
            return new ArrayList<String>();
        }
    }

    private String toSpecText(String specsJson) {
        Map<String, Object> map = parseJsonMap(specsJson);
        List<String> values = new ArrayList<String>();
        for (Object value : map.values()) {
            values.add(String.valueOf(value));
        }
        return String.join("/", values);
    }

    private String safe(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    @SuppressWarnings("unused")
    private String formatDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Timestamp) {
            return ((Timestamp) value).toLocalDateTime().format(DT);
        }
        if (value instanceof LocalDateTime) {
            return ((LocalDateTime) value).format(DT);
        }
        return String.valueOf(value);
    }
}
