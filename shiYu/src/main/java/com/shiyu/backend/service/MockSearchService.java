package com.shiyu.backend.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 搜索域持久化服务。
 */
@Service
public class MockSearchService {

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JdbcTemplate jdbcTemplate;
    private final MockCatalogService mockCatalogService;

    /**
     * 注入依赖。
     *
     * @param jdbcTemplate      数据库访问模板
     * @param mockCatalogService 商品域服务
     */
    public MockSearchService(JdbcTemplate jdbcTemplate, MockCatalogService mockCatalogService) {
        this.jdbcTemplate = jdbcTemplate;
        this.mockCatalogService = mockCatalogService;
    }

    /**
     * 获取热搜词。
     *
     * @return 热搜词列表
     */
    public Map<String, Object> hot() {
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("list", jdbcTemplate.queryForList(
                "SELECT id, keyword, sort_order FROM search_hot_keywords WHERE status = 1 ORDER BY sort_order ASC, id ASC LIMIT 20"));
        return data;
    }

    /**
     * 获取联想词。
     *
     * @param keyword 输入关键词
     * @param limit   限制数量
     * @return 联想词列表
     */
    public Map<String, Object> suggest(String keyword, int limit) {
        String normalized = keyword == null ? "" : keyword.trim();
        int safeLimit = Math.max(limit, 1);
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        if (!normalized.isEmpty()) {
            list = jdbcTemplate.queryForList(
                    "SELECT id, keyword, suggest_word, target_type, target_id FROM search_suggest_keywords " +
                            "WHERE status = 1 AND (keyword LIKE ? OR suggest_word LIKE ?) " +
                            "ORDER BY weight DESC, id ASC LIMIT ?",
                    normalized + "%", "%" + normalized + "%", safeLimit);
        }

        if (list.isEmpty() && !normalized.isEmpty()) {
            List<String> candidates = mockCatalogService.getSearchCandidates();
            int seq = 1;
            for (String candidate : candidates) {
                if (!candidate.toLowerCase(Locale.ROOT).contains(normalized.toLowerCase(Locale.ROOT))) {
                    continue;
                }
                Map<String, Object> row = new HashMap<String, Object>();
                row.put("id", seq++);
                row.put("keyword", normalized);
                row.put("suggest_word", candidate);
                row.put("target_type", "product");
                row.put("target_id", 0);
                list.add(row);
                if (list.size() >= safeLimit) {
                    break;
                }
            }
        }

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("list", list);
        return data;
    }

    /**
     * 写入搜索历史。
     *
     * @param userId  用户 ID
     * @param keyword 关键词
     */
    public void recordHistory(Long userId, String keyword) {
        if (userId == null || keyword == null || keyword.trim().isEmpty()) {
            return;
        }
        String normalized = keyword.trim();
        jdbcTemplate.update(
                "INSERT INTO search_histories(_openid, user_id, keyword, search_count, last_search_at) VALUES('', ?, ?, 1, NOW()) " +
                        "ON DUPLICATE KEY UPDATE search_count = search_count + 1, last_search_at = NOW()",
                userId, normalized);
    }

    /**
     * 获取搜索历史。
     *
     * @param userId 用户 ID
     * @param limit  数量限制
     * @return 搜索历史
     */
    public Map<String, Object> history(Long userId, int limit) {
        Map<String, Object> data = new HashMap<String, Object>();
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        if (userId == null) {
            data.put("list", list);
            return data;
        }

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, keyword, search_count, last_search_at FROM search_histories " +
                        "WHERE user_id = ? ORDER BY last_search_at DESC LIMIT ?",
                userId, Math.max(limit, 1));
        for (Map<String, Object> row : rows) {
            Map<String, Object> item = new HashMap<String, Object>();
            item.put("id", row.get("id"));
            item.put("keyword", row.get("keyword"));
            item.put("search_count", row.get("search_count"));
            Object time = row.get("last_search_at");
            item.put("last_search_at", time instanceof Timestamp ? ((Timestamp) time).toLocalDateTime().format(DT) : null);
            list.add(item);
        }
        data.put("list", list);
        return data;
    }

    /**
     * 清空搜索历史。
     *
     * @param userId 用户 ID
     */
    public void clearHistory(Long userId) {
        if (userId == null) {
            return;
        }
        jdbcTemplate.update("DELETE FROM search_histories WHERE user_id = ?", userId);
    }
}
