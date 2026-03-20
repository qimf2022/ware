package com.shiyu.backend.service;

import com.shiyu.backend.common.BizCode;
import com.shiyu.backend.common.BizException;
import com.shiyu.backend.dto.UpdateUserProfileRequest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;


import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 用户域持久化服务。
 */
@Service
public class MockUserDomainService {

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final long MARKETING_CACHE_TTL_MS = 60_000L;

    private final JdbcTemplate jdbcTemplate;
    private final MockCatalogService mockCatalogService;

    private volatile List<Map<String, Object>> availableCouponsCache = Collections.emptyList();
    private volatile long availableCouponsCacheExpireAt = 0L;

    /**
     * 注入依赖。
     *
     * @param jdbcTemplate       数据库访问模板
     * @param mockCatalogService 商品域服务
     */
    public MockUserDomainService(JdbcTemplate jdbcTemplate, MockCatalogService mockCatalogService) {
        this.jdbcTemplate = jdbcTemplate;
        this.mockCatalogService = mockCatalogService;
    }

    /**
     * 处理登录态落库。
     *
     * @param userId 用户 ID
     */
    public void onLogin(Long userId, String nickName, String avatarUrl, Integer gender) {
        Long uid = requireUser(userId);
        String safeNickname = sanitizeNickname(nickName);
        String safeAvatarUrl = trimToNull(avatarUrl);
        Integer safeGender = normalizeGender(gender);

        ensureUser(uid, safeNickname, safeAvatarUrl, safeGender);
        jdbcTemplate.update(
                "UPDATE users SET last_login_at = NOW(), last_active_at = NOW(), updated_at = NOW(), " +
                        "nickname = CASE WHEN ? IS NULL THEN nickname WHEN nickname IS NULL OR nickname = '' OR nickname = '诗语访客' OR nickname = '诗语用户' OR nickname = '微信用户' OR nickname LIKE '微信用户%' THEN ? ELSE nickname END, " +
                        "avatar_url = CASE WHEN ? IS NULL THEN avatar_url ELSE ? END, " +
                        "gender = COALESCE(?, gender) WHERE id = ?",
                safeNickname,
                safeNickname,
                safeAvatarUrl,
                safeAvatarUrl,
                safeGender,
                uid);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long loginByWechatIdentity(String openid, String unionid, String nickName, String avatarUrl, Integer gender) {
        String safeOpenid = trimToNull(openid);
        if (safeOpenid == null) {
            throw new BizException(BizCode.AUTH_FAIL, "微信身份校验失败");
        }
        String safeUnionid = trimToNull(unionid);
        String safeNickname = sanitizeNickname(nickName);
        String effectiveNickname = safeNickname == null ? buildWechatDisplayNickname(safeOpenid) : safeNickname;
        String safeAvatarUrl = trimToNull(avatarUrl);
        Integer safeGender = normalizeGender(gender);

        List<Map<String, Object>> users = jdbcTemplate.queryForList("SELECT id FROM users WHERE openid = ? LIMIT 1", safeOpenid);
        if (!users.isEmpty()) {
            Long uid = toLong(users.get(0).get("id"));
            jdbcTemplate.update(
                    "UPDATE users SET last_login_at = NOW(), last_active_at = NOW(), updated_at = NOW(), " +
                            "unionid = COALESCE(?, unionid), " +
                            "nickname = CASE WHEN ? IS NULL THEN nickname WHEN nickname IS NULL OR nickname = '' OR nickname = '诗语访客' OR nickname = '诗语用户' OR nickname = '微信用户' OR nickname LIKE '微信用户%' THEN ? ELSE nickname END, " +
                            "avatar_url = CASE WHEN ? IS NULL THEN avatar_url ELSE ? END, " +
                            "gender = COALESCE(?, gender) WHERE id = ?",
                    safeUnionid,
                    effectiveNickname,
                    effectiveNickname,
                    safeAvatarUrl,
                    safeAvatarUrl,
                    safeGender,
                    uid);
            return uid;
        }

        try {
            jdbcTemplate.update(
                    "INSERT INTO users(_openid, openid, unionid, nickname, avatar_url, phone_mask, gender, member_level_code, status, register_source, last_login_at, last_active_at, created_at, updated_at) " +
                            "VALUES('', ?, ?, ?, COALESCE(?, 'https://cyberas-12.oss-cn-shanghai.aliyuncs.com/YHome/logo.png'), '138****8888', COALESCE(?, 0), 'silver', 1, 'wx_miniapp', NOW(), NOW(), NOW(), NOW())",
                    safeOpenid,
                    safeUnionid,
                    effectiveNickname,
                    safeAvatarUrl,
                    safeGender);
        } catch (DuplicateKeyException duplicateKeyException) {
            // 并发首次登录兜底
        }

        List<Map<String, Object>> inserted = jdbcTemplate.queryForList("SELECT id FROM users WHERE openid = ? LIMIT 1", safeOpenid);
        if (inserted.isEmpty()) {
            throw new BizException(BizCode.SYSTEM_ERROR, "登录失败，请稍后重试");
        }
        return toLong(inserted.get(0).get("id"));
    }



    /**
     * 获取用户资料。
     *
     * @param userId 用户 ID
     * @return 用户资料
     */
    public Map<String, Object> getProfile(Long userId) {
        Long uid = requireUser(userId);
//        uid= Long.valueOf(1);
        ensureUser(uid);

        List<Map<String, Object>> users = jdbcTemplate.queryForList(
                "SELECT id, nickname, avatar_url, phone_mask, gender, member_level_code, register_source, " +
                        "last_login_at, last_active_at, created_at FROM users WHERE id = ? LIMIT 1", uid);
        if (users.isEmpty()) {
            throw new BizException(BizCode.RESOURCE_NOT_FOUND, "用户不存在");
        }

        Map<String, Object> user = users.get(0);
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("id", user.get("id"));
        data.put("nickname", user.get("nickname"));
        data.put("avatar_url", user.get("avatar_url"));
        data.put("phone_mask", user.get("phone_mask"));
        data.put("gender", user.get("gender"));
        data.put("member_level_code", user.get("member_level_code"));
        data.put("register_source", user.get("register_source"));
        data.put("last_login_at", formatDateTime(user.get("last_login_at")));
        data.put("last_active_at", formatDateTime(user.get("last_active_at")));
        data.put("created_at", formatDateTime(user.get("created_at")));

        Map<String, Object> stats = new HashMap<String, Object>();
        stats.put("order_count", numberValue("SELECT COUNT(1) FROM orders WHERE user_id = ?", uid));
        stats.put("favorite_count", numberValue("SELECT COUNT(1) FROM user_favorites WHERE user_id = ? AND deleted = 0", uid));
        stats.put("footprint_count", numberValue("SELECT COUNT(1) FROM user_footprints WHERE user_id = ?", uid));
        stats.put("points", numberValue("SELECT COALESCE(available_points,0) FROM user_points_accounts WHERE user_id = ? LIMIT 1", uid));
        stats.put("coupon_count", numberValue("SELECT COUNT(1) FROM user_coupons WHERE user_id = ? AND status = 0", uid));
        stats.put("card_balance", numberDecimal("SELECT COALESCE(SUM(balance),0) FROM stored_value_cards WHERE user_id = ? AND status = 2", uid));
        data.put("stats", stats);
        return data;
    }

    /**
     * 更新用户资料。
     *
     * @param userId  用户 ID
     * @param request 更新请求
     */
    public void updateProfile(Long userId, UpdateUserProfileRequest request) {
        Long uid = requireUser(userId);
        ensureUser(uid);

        String nickname = sanitizeNickname(request.getNickname());
        String avatarUrl = request.getAvatarUrl() == null ? null : request.getAvatarUrl().trim();
        Integer gender = request.getGender();

        jdbcTemplate.update(
                "UPDATE users SET nickname = COALESCE(?, nickname), avatar_url = COALESCE(?, avatar_url), " +
                        "gender = COALESCE(?, gender), last_active_at = NOW(), updated_at = NOW() WHERE id = ?",
                emptyToNull(nickname), emptyToNull(avatarUrl), gender, uid);
    }

    /**
     * 获取收藏列表。
     *
     * @param userId   用户 ID
     * @param page     页码
     * @param pageSize 每页大小
     * @return 分页结果
     */
    public Map<String, Object> listFavorites(Long userId, int page, int pageSize) {
        Long uid = requireUser(userId);
        int safePage = Math.max(page, 1);
        int safePageSize = Math.max(pageSize, 1);
        int offset = (safePage - 1) * safePageSize;

        Integer total = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM user_favorites uf JOIN products p ON p.id = uf.product_id " +
                        "WHERE uf.user_id = ? AND uf.deleted = 0 AND p.status = 1 AND p.deleted = 0", Integer.class, uid);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT uf.id AS favorite_id, uf.created_at, p.id AS product_id FROM user_favorites uf " +
                        "JOIN products p ON p.id = uf.product_id " +
                        "WHERE uf.user_id = ? AND uf.deleted = 0 AND p.status = 1 AND p.deleted = 0 " +
                        "ORDER BY uf.created_at DESC LIMIT ? OFFSET ?",
                uid, safePageSize, offset);

        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> item = new HashMap<String, Object>();
            item.put("id", row.get("favorite_id"));
            item.put("product", mockCatalogService.productCard(((Number) row.get("product_id")).longValue()));
            item.put("created_at", formatDateTime(row.get("created_at")));
            list.add(item);
        }

        return pageResult(list, safePage, safePageSize, total == null ? 0 : total);
    }

    /**
     * 执行收藏操作。
     *
     * @param userId    用户 ID
     * @param productId 商品 ID
     * @param action    操作类型
     * @return 收藏结果
     */
    public Map<String, Object> favoriteAction(Long userId, Long productId, String action) {
        Long uid = requireUser(userId);
        ensureUser(uid);
        if (!mockCatalogService.existsProduct(productId)) {
            throw new BizException(BizCode.RESOURCE_NOT_FOUND, "商品不存在");
        }

        String normalizedAction = action == null ? "" : action.trim().toLowerCase(Locale.ROOT);
        boolean before = isFavorited(uid, productId);
        boolean after;

        if ("add".equals(normalizedAction)) {
            jdbcTemplate.update(
                    "INSERT INTO user_favorites(_openid, user_id, product_id, deleted, created_at, updated_at) " +
                            "VALUES('', ?, ?, 0, NOW(), NOW()) " +
                            "ON DUPLICATE KEY UPDATE deleted = 0, updated_at = NOW()",
                    uid, productId);
            after = true;
            if (!before) {
                mockCatalogService.adjustFavoriteCount(productId, 1);
            }
        } else if ("remove".equals(normalizedAction)) {
            int changed = jdbcTemplate.update(
                    "UPDATE user_favorites SET deleted = 1, updated_at = NOW() WHERE user_id = ? AND product_id = ? AND deleted = 0",
                    uid, productId);
            after = false;
            if (changed > 0) {
                mockCatalogService.adjustFavoriteCount(productId, -1);
            }
        } else {
            throw new BizException(BizCode.PARAM_ERROR, "action 仅支持 add/remove");
        }

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("is_favorited", after);
        data.put("favorite_count", mockCatalogService.adjustFavoriteCount(productId, 0));
        return data;
    }

    /**
     * 检查是否已收藏。
     *
     * @param userId    用户 ID
     * @param productId 商品 ID
     * @return 是否已收藏
     */
    public boolean isFavorited(Long userId, Long productId) {
        if (userId == null || productId == null) {
            return false;
        }
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM user_favorites WHERE user_id = ? AND product_id = ? AND deleted = 0",
                Integer.class, userId, productId);
        return count != null && count > 0;
    }

    /**
     * 写入浏览足迹。
     *
     * @param userId     用户 ID
     * @param productId  商品 ID
     * @param sourcePage 来源页面
     */
    public void addFootprint(Long userId, Long productId, String sourcePage) {
        Long uid = requireUser(userId);
        if (!mockCatalogService.existsProduct(productId)) {
            return;
        }
        jdbcTemplate.update("DELETE FROM user_footprints WHERE user_id = ? AND product_id = ?", uid, productId);
        jdbcTemplate.update(
                "INSERT INTO user_footprints(_openid, user_id, product_id, source_page, viewed_at) VALUES('', ?, ?, ?, NOW())",
                uid, productId, sourcePage);
    }

    /**
     * 获取浏览足迹。
     *
     * @param userId   用户 ID
     * @param page     页码
     * @param pageSize 每页大小
     * @return 分页结果
     */
    public Map<String, Object> listFootprints(Long userId, int page, int pageSize) {
        Long uid = requireUser(userId);
        int safePage = Math.max(page, 1);
        int safePageSize = Math.max(pageSize, 1);
        int offset = (safePage - 1) * safePageSize;

        Integer total = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM user_footprints WHERE user_id = ?", Integer.class, uid);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT uf.id, uf.product_id, uf.source_page, uf.viewed_at FROM user_footprints uf " +
                        "JOIN products p ON p.id = uf.product_id WHERE uf.user_id = ? AND p.status = 1 AND p.deleted = 0 " +
                        "ORDER BY uf.viewed_at DESC LIMIT ? OFFSET ?",
                uid, safePageSize, offset);

        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> item = new HashMap<String, Object>();
            item.put("id", row.get("id"));
            item.put("product", mockCatalogService.productCard(((Number) row.get("product_id")).longValue()));
            item.put("source_page", row.get("source_page"));
            item.put("viewed_at", formatDateTime(row.get("viewed_at")));
            list.add(item);
        }

        return pageResult(list, safePage, safePageSize, total == null ? 0 : total);
    }

    /**
     * 清空浏览足迹。
     *
     * @param userId 用户 ID
     */
    public void clearFootprints(Long userId) {
        Long uid = requireUser(userId);
        jdbcTemplate.update("DELETE FROM user_footprints WHERE user_id = ?", uid);
    }

    /**
     * 获取积分明细。
     *
     * @param userId   用户 ID
     * @param page     页码
     * @param pageSize 每页大小
     * @return 积分流水
     */
    public Map<String, Object> listPointLogs(Long userId, int page, int pageSize) {
        Long uid = requireUser(userId);
        ensureUser(uid);

        int safePage = Math.max(page, 1);
        int safePageSize = Math.max(pageSize, 1);
        int offset = (safePage - 1) * safePageSize;

        Map<String, Object> account = jdbcTemplate.queryForList(
                "SELECT total_points, available_points, frozen_points, expire_points FROM user_points_accounts WHERE user_id = ? LIMIT 1",
                uid).stream().findFirst().orElse(new HashMap<String, Object>());

        Integer total = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM user_points_logs WHERE user_id = ?", Integer.class, uid);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, change_type, points, business_type, business_id, remark, created_at " +
                        "FROM user_points_logs WHERE user_id = ? ORDER BY created_at DESC, id DESC LIMIT ? OFFSET ?",
                uid, safePageSize, offset);

        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> item = new HashMap<String, Object>();
            int changeType = toInt(row.get("change_type"));
            item.put("id", row.get("id"));
            item.put("change_type", changeType);
            item.put("change_type_text", pointsChangeTypeText(changeType));
            item.put("points", row.get("points"));
            item.put("business_type", row.get("business_type"));
            item.put("business_id", row.get("business_id"));
            item.put("remark", row.get("remark"));
            item.put("created_at", formatDateTime(row.get("created_at")));
            list.add(item);
        }

        Map<String, Object> data = pageResult(list, safePage, safePageSize, total == null ? 0 : total);
        data.put("total_points", toLongDefault(account.get("total_points"), 0L));
        data.put("available_points", toLongDefault(account.get("available_points"), 0L));
        data.put("frozen_points", toLongDefault(account.get("frozen_points"), 0L));
        data.put("expire_points", toLongDefault(account.get("expire_points"), 0L));
        return data;
    }

    /**
     * 获取用户优惠券。
     *
     * @param userId   用户 ID
     * @param status   状态
     * @param page     页码
     * @param pageSize 每页大小
     * @return 优惠券分页
     */
    public Map<String, Object> listUserCoupons(Long userId, String status, int page, int pageSize) {
        Long uid = requireUser(userId);
        ensureUser(uid);

        int safePage = Math.max(page, 1);
        int safePageSize = Math.max(pageSize, 1);
        int offset = (safePage - 1) * safePageSize;

        String statusSql = "";
        List<Object> args = new ArrayList<Object>();
        args.add(uid);

        String normalized = status == null ? "" : status.trim().toLowerCase(Locale.ROOT);
        if ("available".equals(normalized) || normalized.isEmpty()) {
            statusSql = " AND uc.status = 0 AND uc.expire_time >= NOW() ";
        } else if ("used".equals(normalized)) {
            statusSql = " AND uc.status = 1 ";
        } else if ("expired".equals(normalized)) {
            statusSql = " AND (uc.status = 2 OR (uc.status = 0 AND uc.expire_time < NOW())) ";
        }

        Integer total = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM user_coupons uc WHERE uc.user_id = ?" + statusSql,
                Integer.class,
                args.toArray());

        List<Object> queryArgs = new ArrayList<Object>(args);
        queryArgs.add(safePageSize);
        queryArgs.add(offset);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT uc.id, uc.status, uc.receive_time, uc.expire_time, uc.use_time, " +
                        "c.id AS coupon_id, c.name, c.type, c.value, c.threshold, c.scope_type, c.use_rule_desc, c.start_time, c.end_time " +
                        "FROM user_coupons uc JOIN coupons c ON c.id = uc.coupon_id " +
                        "WHERE uc.user_id = ?" + statusSql +
                        " ORDER BY uc.receive_time DESC, uc.id DESC LIMIT ? OFFSET ?",
                queryArgs.toArray());

        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> row : rows) {
            int dbStatus = toInt(row.get("status"));
            int effectiveStatus = dbStatus;
            if (dbStatus == 0 && isExpired(row.get("expire_time"))) {
                effectiveStatus = 2;
            }

            Map<String, Object> coupon = new HashMap<String, Object>();
            coupon.put("id", row.get("coupon_id"));
            coupon.put("name", row.get("name"));
            coupon.put("type", row.get("type"));
            coupon.put("value", row.get("value"));
            coupon.put("threshold", row.get("threshold"));
            coupon.put("scope_type", row.get("scope_type"));
            coupon.put("use_rule_desc", row.get("use_rule_desc"));
            coupon.put("start_time", formatDateTime(row.get("start_time")));
            coupon.put("end_time", formatDateTime(row.get("end_time")));

            Map<String, Object> item = new HashMap<String, Object>();
            item.put("id", row.get("id"));
            item.put("coupon", coupon);
            item.put("status", effectiveStatus);
            item.put("status_text", couponStatusText(effectiveStatus));
            item.put("receive_time", formatDateTime(row.get("receive_time")));
            item.put("expire_time", formatDateTime(row.get("expire_time")));
            if (row.get("use_time") != null) {
                item.put("use_time", formatDateTime(row.get("use_time")));
            }
            list.add(item);
        }

        return pageResult(list, safePage, safePageSize, total == null ? 0 : total);
    }

    /**
     * 获取可领取优惠券。
     *
     * @param userId   用户 ID（可空）
     * @param page     页码
     * @param pageSize 每页大小
     * @return 优惠券分页
     */
    public Map<String, Object> listAvailableCoupons(Long userId, int page, int pageSize) {
        int safePage = Math.max(page, 1);
        int safePageSize = Math.max(pageSize, 1);

        List<Map<String, Object>> all = loadAvailableCouponsFromCache();

        Set<Long> receivedCouponIds = new HashSet<Long>();
        if (userId != null && !all.isEmpty()) {
            StringBuilder idSql = new StringBuilder();
            List<Object> params = new ArrayList<Object>();
            params.add(userId);
            for (int i = 0; i < all.size(); i++) {
                if (i > 0) {
                    idSql.append(",");
                }
                idSql.append("?");
                params.add(toLong(all.get(i).get("id")));
            }
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT coupon_id FROM user_coupons WHERE user_id = ? AND coupon_id IN (" + idSql + ")",
                    params.toArray());
            for (Map<String, Object> row : rows) {
                receivedCouponIds.add(toLong(row.get("coupon_id")));
            }
        }

        List<Map<String, Object>> withState = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> item : all) {
            Map<String, Object> copied = new HashMap<String, Object>(item);
            copied.put("received", receivedCouponIds.contains(toLong(copied.get("id"))));
            withState.add(copied);
        }

        int total = withState.size();
        int from = Math.min((safePage - 1) * safePageSize, total);
        int to = Math.min(from + safePageSize, total);
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>(withState.subList(from, to));

        return pageResult(list, safePage, safePageSize, total);
    }

    /**
     * 领取优惠券。
     *
     * @param userId   用户 ID
     * @param couponId 券模板 ID
     * @return 领取结果
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> receiveCoupon(Long userId, Long couponId) {
        Long uid = requireUser(userId);
        ensureUser(uid);
        if (couponId == null) {
            throw new BizException(BizCode.PARAM_ERROR, "couponId 不能为空");
        }

        List<Map<String, Object>> coupons = jdbcTemplate.queryForList(
                "SELECT id, receive_limit_per_user, remain_count, end_time, start_time, status FROM coupons WHERE id = ? LIMIT 1",
                couponId);
        if (coupons.isEmpty()) {
            throw new BizException(BizCode.RESOURCE_NOT_FOUND, "优惠券不存在");
        }

        Map<String, Object> coupon = coupons.get(0);
        if (toInt(coupon.get("status")) != 1 || isNotStarted(coupon.get("start_time")) || isExpired(coupon.get("end_time"))) {
            throw new BizException(BizCode.BIZ_ERROR, "优惠券不可领取");
        }
        if (toLongDefault(coupon.get("remain_count"), 0L) <= 0) {
            throw new BizException(BizCode.BIZ_ERROR, "优惠券已领完");
        }

        Integer receivedCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM user_coupons WHERE user_id = ? AND coupon_id = ?",
                Integer.class,
                uid,
                couponId);
        int limit = toInt(coupon.get("receive_limit_per_user"));
        if (receivedCount != null && receivedCount >= Math.max(limit, 1)) {
            throw new BizException(BizCode.BIZ_ERROR, "已达到领取上限");
        }

        int changed = jdbcTemplate.update(
                "UPDATE coupons SET remain_count = remain_count - 1, updated_at = NOW() WHERE id = ? AND remain_count > 0",
                couponId);
        if (changed <= 0) {
            throw new BizException(BizCode.BIZ_ERROR, "优惠券已领完");
        }

        jdbcTemplate.update(
                "INSERT INTO user_coupons(_openid, user_id, coupon_id, source_type, status, expire_time, created_at, updated_at) " +
                        "VALUES('', ?, ?, 'manual', 0, ?, NOW(), NOW())",
                uid,
                couponId,
                coupon.get("end_time"));

        Number userCouponId = jdbcTemplate.queryForObject(
                "SELECT id FROM user_coupons WHERE user_id = ? AND coupon_id = ? ORDER BY id DESC LIMIT 1",
                Number.class,
                uid,
                couponId);

        evictAvailableCouponsCache();

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("user_coupon_id", userCouponId == null ? null : userCouponId.longValue());
        return data;
    }

    private List<Map<String, Object>> loadAvailableCouponsFromCache() {
        long now = System.currentTimeMillis();
        List<Map<String, Object>> cache = availableCouponsCache;
        if (!cache.isEmpty() && now < availableCouponsCacheExpireAt) {
            return new ArrayList<Map<String, Object>>(cache);
        }

        synchronized (this) {
            cache = availableCouponsCache;
            if (!cache.isEmpty() && now < availableCouponsCacheExpireAt) {
                return new ArrayList<Map<String, Object>>(cache);
            }

            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT id, name, type, value, threshold, scope_type, use_rule_desc, start_time, end_time, total_count, remain_count, receive_limit_per_user " +
                            "FROM coupons WHERE status = 1 AND remain_count > 0 AND start_time <= NOW() AND end_time >= NOW() " +
                            "ORDER BY id DESC");

            List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
            for (Map<String, Object> row : rows) {
                Map<String, Object> item = new HashMap<String, Object>();
                item.put("id", row.get("id"));
                item.put("name", row.get("name"));
                item.put("type", row.get("type"));
                item.put("value", row.get("value"));
                item.put("threshold", row.get("threshold"));
                item.put("scope_type", row.get("scope_type"));
                item.put("use_rule_desc", row.get("use_rule_desc"));
                item.put("start_time", formatDateTime(row.get("start_time")));
                item.put("end_time", formatDateTime(row.get("end_time")));
                item.put("total_count", row.get("total_count"));
                item.put("remain_count", row.get("remain_count"));
                item.put("receive_limit_per_user", row.get("receive_limit_per_user"));
                list.add(item);
            }

            availableCouponsCache = list;
            availableCouponsCacheExpireAt = now + MARKETING_CACHE_TTL_MS;
            return new ArrayList<Map<String, Object>>(list);
        }
    }

    private synchronized void evictAvailableCouponsCache() {
        availableCouponsCache = Collections.emptyList();
        availableCouponsCacheExpireAt = 0L;
    }

    private String pointsChangeTypeText(int changeType) {
        if (changeType == 1) {
            return "获取";
        }
        if (changeType == 2) {
            return "抵扣";
        }
        if (changeType == 3) {
            return "退款返还";
        }
        if (changeType == 4) {
            return "过期";
        }
        return "调整";
    }

    private String couponStatusText(int status) {
        if (status == 0) {
            return "未使用";
        }
        if (status == 1) {
            return "已使用";
        }
        if (status == 2) {
            return "已过期";
        }
        if (status == 3) {
            return "已锁定";
        }
        return "未知";
    }

    private boolean isExpired(Object dateTime) {
        LocalDateTime target = toDateTime(dateTime);
        return target != null && target.isBefore(LocalDateTime.now());
    }

    private boolean isNotStarted(Object dateTime) {
        LocalDateTime target = toDateTime(dateTime);
        return target != null && target.isAfter(LocalDateTime.now());
    }

    private LocalDateTime toDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Timestamp) {
            return ((Timestamp) value).toLocalDateTime();
        }
        if (value instanceof LocalDateTime) {
            return (LocalDateTime) value;
        }
        return null;
    }

    private int toInt(Object value) {
        if (value == null) {
            return 0;
        }
        return ((Number) value).intValue();
    }

    private long toLongDefault(Object value, long defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        return ((Number) value).longValue();
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        return ((Number) value).longValue();
    }

    private void ensureUser(Long userId) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM users WHERE id = ?", Integer.class, userId);
        if (count != null && count > 0) {
            return;
        }
        throw new BizException(BizCode.AUTH_FAIL, "用户不存在，请重新登录");
    }


    private void ensureUser(Long userId, String nickname, String avatarUrl, Integer gender) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM users WHERE id = ?", Integer.class, userId);
        if (count != null && count > 0) {
            return;
        }
        String openid = "mock_openid_" + userId;
        String seedNickname = sanitizeNickname(nickname);
        String seedAvatar = trimToNull(avatarUrl);
        Integer seedGender = normalizeGender(gender);
        jdbcTemplate.update(
                "INSERT INTO users(id, _openid, openid, nickname, avatar_url, phone_mask, gender, member_level_code, status, register_source, last_login_at, last_active_at, created_at, updated_at) " +
                        "VALUES(?, ?, ?, COALESCE(?, '诗语访客'), COALESCE(?, 'https://cyberas-12.oss-cn-shanghai.aliyuncs.com/YHome/logo.png'), '138****8888', COALESCE(?, 0), 'silver', 1, 'wx_miniapp', NOW(), NOW(), NOW(), NOW())",
                userId, openid, openid, seedNickname, seedAvatar, seedGender);
    }

    private Long requireUser(Long userId) {
        if (userId == null) {
            throw new BizException(BizCode.AUTH_FAIL, "用户未登录");
        }
        return userId;
    }

    private Integer normalizeGender(Integer gender) {
        if (gender == null) {
            return null;
        }
        return (gender >= 0 && gender <= 2) ? gender : null;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String sanitizeNickname(String nickname) {
        String value = trimToNull(nickname);
        if (value == null) {
            return null;
        }
        if ("微信用户".equals(value) || value.startsWith("微信用户")) {
            return null;
        }
        return value;
    }

    private String buildWechatDisplayNickname(String openid) {
        String safeOpenid = trimToNull(openid);
        if (safeOpenid == null) {
            return "微信用户";
        }
        int len = safeOpenid.length();
        String suffix = len <= 6 ? safeOpenid : safeOpenid.substring(len - 6);
        return "微信用户" + suffix;
    }



    private Map<String, Object> pageResult(List<Map<String, Object>> list, int page, int pageSize, int total) {

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("list", list);
        data.put("total", total);
        data.put("page", page);
        data.put("page_size", pageSize);
        data.put("has_more", page * pageSize < total);
        return data;
    }

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
        return value.toString();
    }

    private Object emptyToNull(String value) {
        if (value == null) {
            return null;
        }
        return value.trim().isEmpty() ? null : value;
    }

    private long numberValue(String sql, Long userId) {
        List<Number> rows = jdbcTemplate.query(sql, (rs, rowNum) -> (Number) rs.getObject(1), userId);
        if (rows.isEmpty() || rows.get(0) == null) {
            return 0L;
        }
        return rows.get(0).longValue();
    }

    private Object numberDecimal(String sql, Long userId) {
        List<Object> rows = jdbcTemplate.query(sql, (rs, rowNum) -> rs.getObject(1), userId);
        if (rows.isEmpty() || rows.get(0) == null) {
            return 0;
        }
        return rows.get(0);
    }


}
