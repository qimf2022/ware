package com.shiyu.backend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shiyu.backend.common.BizCode;
import com.shiyu.backend.common.BizException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;


import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 商品与分类持久化服务。
 */
@Service
public class MockCatalogService {

    private static final long TOPIC_CACHE_TTL_MS = 60_000L;
    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final Map<String, TopicCacheEntry> topicCache = new ConcurrentHashMap<String, TopicCacheEntry>();

    /**
     * 注入依赖。
     *
     * @param jdbcTemplate 数据库访问模板
     * @param objectMapper JSON 工具
     */
    public MockCatalogService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 获取首页配置。
     *
     * @return 首页配置
     */
    public Map<String, Object> getHomeConfig() {
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("banners", jdbcTemplate.queryForList(
                "SELECT id, image_url, title, link_type, link_value " +
                        "FROM banners WHERE status = 1 " +
                        "AND (start_time IS NULL OR start_time <= NOW()) " +
                        "AND (end_time IS NULL OR end_time >= NOW()) " +
                        "ORDER BY sort_order ASC, id ASC LIMIT 8"));
        data.put("categories", jdbcTemplate.queryForList(
                "SELECT id, name, code, icon_url FROM categories " +
                        "WHERE parent_id IS NULL AND status = 1 AND deleted = 0 " +
                        "ORDER BY sort_order ASC, id ASC LIMIT 8"));
        data.put("recommend_products", recommendProducts(6));
        return data;
    }

    /**
     * 获取首页推荐商品。
     *
     * @param page     页码
     * @param pageSize 每页大小
     * @return 分页结果
     */
    public Map<String, Object> getHomeRecommend(int page, int pageSize) {
        int safePage = Math.max(page, 1);
        int safePageSize = Math.max(pageSize, 1);
        int offset = (safePage - 1) * safePageSize;
        Integer total = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM products WHERE status = 1 AND deleted = 0 AND is_recommend = 1", Integer.class);
        List<Map<String, Object>> list = jdbcTemplate.queryForList(
                "SELECT id, title, subtitle, main_image, min_price, max_price, original_min_price, sales_count, stock_status, category_id " +
                        "FROM products WHERE status = 1 AND deleted = 0 AND is_recommend = 1 " +
                        "ORDER BY sort_order ASC, id DESC LIMIT ? OFFSET ?",
                safePageSize, offset);
        for (Map<String, Object> item : list) {
            item.put("category", categorySimple(toInt(item.get("category_id"))));
            item.remove("category_id");
        }
        return pageResult(list, safePage, safePageSize, total == null ? 0 : total, true);
    }

    /**
     * 获取分类列表。
     *
     * @param parentId 父分类 ID
     * @return 分类列表
     */
    public Map<String, Object> getCategories(Integer parentId) {
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        if (parentId == null) {
            List<Map<String, Object>> roots = jdbcTemplate.queryForList(
                    "SELECT id, name, code, icon_url, sort_order FROM categories " +
                            "WHERE parent_id IS NULL AND status = 1 AND deleted = 0 ORDER BY sort_order ASC, id ASC");
            for (Map<String, Object> root : roots) {
                Integer id = toInt(root.get("id"));
                root.put("children", jdbcTemplate.queryForList(
                        "SELECT id, name, code, icon_url, sort_order FROM categories " +
                                "WHERE parent_id = ? AND status = 1 AND deleted = 0 ORDER BY sort_order ASC, id ASC", id));
                list.add(root);
            }
        } else {
            list = jdbcTemplate.queryForList(
                    "SELECT id, name, code, icon_url, sort_order FROM categories " +
                            "WHERE parent_id = ? AND status = 1 AND deleted = 0 ORDER BY sort_order ASC, id ASC", parentId);
        }
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("list", list);
        return data;
    }

    /**
     * 获取商品列表。
     *
     * @param categoryId 分类 ID
     * @param keyword    搜索关键词
     * @param sort       排序方式
     * @param page       页码
     * @param pageSize   每页大小
     * @return 商品分页
     */
    public Map<String, Object> getProducts(Integer categoryId, String keyword, String sort, int page, int pageSize) {
        int safePage = Math.max(page, 1);
        int safePageSize = Math.max(pageSize, 1);
        int offset = (safePage - 1) * safePageSize;

        StringBuilder where = new StringBuilder(" WHERE p.status = 1 AND p.deleted = 0 ");
        List<Object> args = new ArrayList<Object>();
        if (categoryId != null) {
            where.append(" AND p.category_id = ? ");
            args.add(categoryId);
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            where.append(" AND (LOWER(p.title) LIKE ? OR LOWER(IFNULL(p.subtitle, '')) LIKE ?) ");
            String like = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
            args.add(like);
            args.add(like);
        }

        String orderBy = " ORDER BY p.sort_order ASC, p.id DESC ";
        if ("price_asc".equalsIgnoreCase(sort)) {
            orderBy = " ORDER BY p.min_price ASC, p.id DESC ";
        } else if ("price_desc".equalsIgnoreCase(sort)) {
            orderBy = " ORDER BY p.min_price DESC, p.id DESC ";
        } else if ("sales".equalsIgnoreCase(sort)) {
            orderBy = " ORDER BY p.sales_count DESC, p.id DESC ";
        } else if ("new".equalsIgnoreCase(sort)) {
            orderBy = " ORDER BY p.is_new DESC, p.id DESC ";
        }

        Integer total = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM products p " + where.toString(), Integer.class, args.toArray());

        List<Object> pageArgs = new ArrayList<Object>(args);
        pageArgs.add(safePageSize);
        pageArgs.add(offset);
        List<Map<String, Object>> list = jdbcTemplate.queryForList(
                "SELECT p.id, p.title, p.subtitle, p.main_image, p.min_price, p.max_price, p.original_min_price, " +
                        "p.sales_count, p.stock_status, p.category_id " +
                        "FROM products p " + where.toString() + orderBy + " LIMIT ? OFFSET ?",
                pageArgs.toArray());

        for (Map<String, Object> item : list) {
            item.put("category", categorySimple(toInt(item.get("category_id"))));
            item.remove("category_id");
        }

        Map<String, Object> data = pageResult(list, safePage, safePageSize, total == null ? 0 : total, true);
        data.put("filters", queryFilters(categoryId));
        return data;
    }

    /**
     * 获取商品详情。
     *
     * @param id          商品 ID
     * @param isFavorited 是否已收藏
     * @return 商品详情
     */
    public Map<String, Object> getProductDetail(Long id, boolean isFavorited) {
        List<Map<String, Object>> products = jdbcTemplate.queryForList(
                "SELECT id, product_no, category_id, title, subtitle, main_image, min_price, max_price, original_min_price, " +
                        "sales_count, favorite_count, stock_status, is_new, is_hot " +
                        "FROM products WHERE id = ? AND status = 1 AND deleted = 0", id);
        if (products.isEmpty()) {
            throw new BizException(BizCode.RESOURCE_NOT_FOUND, "商品不存在");
        }
        Map<String, Object> p = products.get(0);
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("id", p.get("id"));
        data.put("product_no", p.get("product_no"));
        data.put("title", p.get("title"));
        data.put("subtitle", p.get("subtitle"));
        data.put("main_image", p.get("main_image"));
        data.put("min_price", p.get("min_price"));
        data.put("max_price", p.get("max_price"));
        data.put("original_min_price", p.get("original_min_price"));
        data.put("sales_count", p.get("sales_count"));
        data.put("favorite_count", p.get("favorite_count"));
        data.put("stock_status", p.get("stock_status"));
        data.put("is_new", p.get("is_new"));
        data.put("is_hot", p.get("is_hot"));
        data.put("category", categorySimple(toInt(p.get("category_id"))));
        data.put("media", jdbcTemplate.queryForList(
                "SELECT id, media_type, media_url, cover_url, sort_order FROM product_media WHERE product_id = ? ORDER BY sort_order ASC, id ASC",
                id));
        data.put("detail_attrs", loadDetailAttrs(id));
        data.put("is_favorited", isFavorited);
        return data;
    }

    /**
     * 获取商品规格与 SKU。
     *
     * @param id 商品 ID
     * @return 规格与 SKU
     */
    public Map<String, Object> getProductSpecs(Long id) {
        if (!existsProduct(id)) {
            throw new BizException(BizCode.RESOURCE_NOT_FOUND, "商品不存在");
        }
        List<Map<String, Object>> groups = jdbcTemplate.queryForList(
                "SELECT id, name, sort_order FROM product_spec_groups WHERE product_id = ? ORDER BY sort_order ASC, id ASC", id);
        for (Map<String, Object> group : groups) {
            Long groupId = toLong(group.get("id"));
            group.put("values", jdbcTemplate.queryForList(
                    "SELECT id, value, image_url, sort_order FROM product_spec_values WHERE spec_group_id = ? ORDER BY sort_order ASC, id ASC",
                    groupId));
        }

        List<Map<String, Object>> skus = jdbcTemplate.queryForList(
                "SELECT id, sku_code, specs_json, price, original_price, stock, image_url, status " +
                        "FROM product_skus WHERE product_id = ? AND status = 1 ORDER BY id ASC", id);
        for (Map<String, Object> sku : skus) {
            Object raw = sku.get("specs_json");
            sku.put("specs_json", parseJsonMap(raw == null ? "{}" : raw.toString()));
        }

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("spec_groups", groups);
        data.put("skus", skus);
        return data;
    }

    /**
     * 获取商品推荐。
     *
     * @param id    商品 ID
     * @param limit 数量
     * @return 推荐列表
     */
    public Map<String, Object> getProductRecommend(Long id, int limit) {
        if (!existsProduct(id)) {
            throw new BizException(BizCode.RESOURCE_NOT_FOUND, "商品不存在");
        }
        int safeLimit = Math.max(limit, 1);
        List<Map<String, Object>> list = jdbcTemplate.queryForList(
                "SELECT p.id, p.title, p.subtitle, p.main_image, p.min_price, p.max_price, p.original_min_price, " +
                        "p.sales_count, p.stock_status, p.category_id, r.relation_type " +
                        "FROM product_relations r JOIN products p ON p.id = r.related_product_id " +
                        "WHERE r.product_id = ? AND r.status = 1 AND p.status = 1 AND p.deleted = 0 " +
                        "ORDER BY r.sort_order ASC, r.id ASC LIMIT ?",
                id, safeLimit);
        if (list.isEmpty()) {
            list = jdbcTemplate.queryForList(
                    "SELECT id, title, subtitle, main_image, min_price, max_price, original_min_price, sales_count, stock_status, category_id, 'bundle' AS relation_type " +
                            "FROM products WHERE status = 1 AND deleted = 0 AND id <> ? AND is_recommend = 1 " +
                            "ORDER BY sort_order ASC, id DESC LIMIT ?",
                    id, safeLimit);
        }
        for (Map<String, Object> item : list) {
            item.put("category", categorySimple(toInt(item.get("category_id"))));
            item.remove("category_id");
            item.put("recommend_reason", "搭配购买更优惠");
        }

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("list", list);
        return data;
    }

    /**
     * 获取专题详情。
     *
     * @param idOrCode 专题 ID 或编码
     * @return 专题详情
     */
    public Map<String, Object> getTopicDetail(String idOrCode) {
        String query = idOrCode == null ? "" : idOrCode.trim();
        if (query.isEmpty()) {
            throw new BizException(BizCode.PARAM_ERROR, "专题标识不能为空");
        }

        TopicCacheEntry cached = topicCache.get(query.toLowerCase(Locale.ROOT));
        long now = System.currentTimeMillis();
        if (cached != null && cached.getExpireAt() > now) {
            return cached.getData();
        }

        List<Map<String, Object>> rows;
        if (isNumeric(query)) {
            rows = jdbcTemplate.queryForList(
                    "SELECT id, topic_no, title, subtitle, cover_image, theme_type, description, content_json, start_time, end_time, created_at " +
                            "FROM topics WHERE id = ? AND status = 1 " +
                            "AND (start_time IS NULL OR start_time <= NOW()) " +
                            "AND (end_time IS NULL OR end_time >= NOW()) LIMIT 1",
                    Long.valueOf(query));
        } else {
            rows = jdbcTemplate.queryForList(
                    "SELECT id, topic_no, title, subtitle, cover_image, theme_type, description, content_json, start_time, end_time, created_at " +
                            "FROM topics WHERE topic_no = ? AND status = 1 " +
                            "AND (start_time IS NULL OR start_time <= NOW()) " +
                            "AND (end_time IS NULL OR end_time >= NOW()) LIMIT 1",
                    query);
        }
        if (rows.isEmpty()) {
            throw new BizException(BizCode.RESOURCE_NOT_FOUND, "专题不存在");
        }

        Map<String, Object> topic = rows.get(0);
        Long topicId = toLong(topic.get("id"));

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("id", topic.get("id"));
        data.put("topic_no", topic.get("topic_no"));
        data.put("title", topic.get("title"));
        data.put("subtitle", topic.get("subtitle"));
        data.put("cover_image", topic.get("cover_image"));
        data.put("theme_type", topic.get("theme_type"));
        data.put("description", topic.get("description"));

        Object contentJson = topic.get("content_json");
        data.put("content_json", contentJson == null ? new HashMap<String, Object>() : parseJsonMap(contentJson.toString()));

        List<Map<String, Object>> products = jdbcTemplate.queryForList(
                "SELECT p.id, p.title, p.main_image, p.min_price, p.max_price, p.sales_count, tp.recommend_reason " +
                        "FROM topic_products tp JOIN products p ON p.id = tp.product_id " +
                        "WHERE tp.topic_id = ? AND p.status = 1 AND p.deleted = 0 ORDER BY tp.sort_order ASC, tp.id ASC",
                topicId);
        data.put("products", products);

        data.put("start_time", formatDateTime(topic.get("start_time")));
        data.put("end_time", formatDateTime(topic.get("end_time")));
        data.put("created_at", formatDateTime(topic.get("created_at")));

        topicCache.put(query.toLowerCase(Locale.ROOT), new TopicCacheEntry(new HashMap<String, Object>(data), now + TOPIC_CACHE_TTL_MS));
        Object topicNo = topic.get("topic_no");
        if (topicNo != null) {
            topicCache.put(topicNo.toString().toLowerCase(Locale.ROOT), new TopicCacheEntry(new HashMap<String, Object>(data), now + TOPIC_CACHE_TTL_MS));
        }
        return data;
    }

    /**
     * 获取搜索候选词。
     *
     * @return 搜索词列表
     */
    public List<String> getSearchCandidates() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT title, subtitle FROM products WHERE status = 1 AND deleted = 0 ORDER BY id DESC LIMIT 200");
        List<String> words = new ArrayList<String>();
        for (Map<String, Object> row : rows) {
            if (row.get("title") != null) {
                words.add(row.get("title").toString());
            }
            if (row.get("subtitle") != null && !row.get("subtitle").toString().trim().isEmpty()) {
                words.add(row.get("subtitle").toString());
            }
        }
        return words;
    }

    /**
     * 查询商品是否存在。
     *
     * @param productId 商品 ID
     * @return 是否存在
     */
    public boolean existsProduct(Long productId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM products WHERE id = ? AND status = 1 AND deleted = 0", Integer.class, productId);
        return count != null && count > 0;
    }

    /**
     * 构造商品卡片。
     *
     * @param productId 商品 ID
     * @return 商品卡片
     */
    public Map<String, Object> productCard(Long productId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, title, main_image, min_price, max_price, sales_count, stock_status " +
                        "FROM products WHERE id = ? AND status = 1 AND deleted = 0", productId);
        if (rows.isEmpty()) {
            throw new BizException(BizCode.RESOURCE_NOT_FOUND, "商品不存在");
        }
        return rows.get(0);
    }

    /**
     * 调整收藏数量。
     *
     * @param productId 商品 ID
     * @param delta     变化值
     * @return 变更后收藏数
     */
    public long adjustFavoriteCount(Long productId, int delta) {
        int updated = jdbcTemplate.update(
                "UPDATE products SET favorite_count = CASE WHEN favorite_count + ? < 0 THEN 0 ELSE favorite_count + ? END " +
                        "WHERE id = ? AND status = 1 AND deleted = 0",
                delta, delta, productId);
        if (updated <= 0) {
            throw new BizException(BizCode.RESOURCE_NOT_FOUND, "商品不存在");
        }
        Number count = jdbcTemplate.queryForObject("SELECT favorite_count FROM products WHERE id = ?", Number.class, productId);
        return count == null ? 0L : count.longValue();
    }

    private List<Map<String, Object>> recommendProducts(int limit) {
        return jdbcTemplate.queryForList(
                "SELECT id, title, subtitle, main_image, min_price, max_price, original_min_price, sales_count, stock_status " +
                        "FROM products WHERE status = 1 AND deleted = 0 AND is_recommend = 1 " +
                        "ORDER BY sort_order ASC, id DESC LIMIT ?",
                Math.max(limit, 1));
    }

    private Map<String, Object> categorySimple(Integer categoryId) {
        if (categoryId == null) {
            return new HashMap<String, Object>();
        }
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, name FROM categories WHERE id = ? LIMIT 1", categoryId);
        if (rows.isEmpty()) {
            return new HashMap<String, Object>();
        }
        return rows.get(0);
    }

    private List<Map<String, Object>> loadDetailAttrs(Long productId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT group_name, attr_name, attr_value, sort_order, id FROM product_detail_attrs " +
                        "WHERE product_id = ? ORDER BY sort_order ASC, id ASC",
                productId);
        Map<String, List<Map<String, Object>>> grouped = new LinkedHashMap<String, List<Map<String, Object>>>();
        for (Map<String, Object> row : rows) {
            String groupName = row.get("group_name") == null || row.get("group_name").toString().trim().isEmpty()
                    ? "产品参数" : row.get("group_name").toString();
            if (!grouped.containsKey(groupName)) {
                grouped.put(groupName, new ArrayList<Map<String, Object>>());
            }
            Map<String, Object> item = new HashMap<String, Object>();
            item.put("attr_name", row.get("attr_name"));
            item.put("attr_value", row.get("attr_value"));
            grouped.get(groupName).add(item);
        }

        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (Map.Entry<String, List<Map<String, Object>>> entry : grouped.entrySet()) {
            Map<String, Object> group = new HashMap<String, Object>();
            group.put("group_name", entry.getKey());
            group.put("items", entry.getValue());
            result.add(group);
        }
        return result;
    }

    private List<Map<String, Object>> queryFilters(Integer categoryId) {
        String sql = "SELECT fa.id, fa.attr_code, fa.attr_name, fa.input_type " +
                "FROM filter_attributes fa WHERE fa.status = 1 ORDER BY fa.sort_order ASC, fa.id ASC";
        List<Map<String, Object>> attrs = jdbcTemplate.queryForList(sql);
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> attr : attrs) {
            Long attrId = toLong(attr.get("id"));
            StringBuilder valueSql = new StringBuilder(
                    "SELECT DISTINCT pfv.filter_value FROM product_filter_values pfv " +
                            "JOIN products p ON p.id = pfv.product_id WHERE pfv.filter_attr_id = ? AND p.status = 1 AND p.deleted = 0 ");
            List<Object> args = new ArrayList<Object>();
            args.add(attrId);
            if (categoryId != null) {
                valueSql.append("AND p.category_id = ? ");
                args.add(categoryId);
            }
            valueSql.append("ORDER BY pfv.filter_value ASC LIMIT 20");

            List<Map<String, Object>> values = jdbcTemplate.queryForList(valueSql.toString(), args.toArray());
            List<String> valueList = new ArrayList<String>();
            for (Map<String, Object> value : values) {
                if (value.get("filter_value") != null) {
                    valueList.add(value.get("filter_value").toString());
                }
            }

            Map<String, Object> item = new HashMap<String, Object>();
            item.put("attr_code", attr.get("attr_code"));
            item.put("attr_name", attr.get("attr_name"));
            item.put("input_type", attr.get("input_type"));
            item.put("values", valueList);
            result.add(item);
        }
        return result;
    }

    private Map<String, Object> pageResult(List<Map<String, Object>> list, int page, int pageSize, int total, boolean withHasMore) {
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("list", list);
        data.put("total", total);
        data.put("page", page);
        data.put("page_size", pageSize);
        if (withHasMore) {
            data.put("has_more", page * pageSize < total);
        }
        return data;
    }

    private Map<String, Object> parseJsonMap(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception ex) {
            return new HashMap<String, Object>();
        }
    }

    private Integer toInt(Object value) {
        if (value == null) {
            return null;
        }
        return ((Number) value).intValue();
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        return ((Number) value).longValue();
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

    private boolean isNumeric(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isDigit(value.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static class TopicCacheEntry {
        private final Map<String, Object> data;
        private final long expireAt;

        TopicCacheEntry(Map<String, Object> data, long expireAt) {
            this.data = data;
            this.expireAt = expireAt;
        }

        Map<String, Object> getData() {
            return new HashMap<String, Object>(data);
        }

        long getExpireAt() {
            return expireAt;
        }
    }
}
