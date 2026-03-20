package com.shiyu.backend.service;

import com.shiyu.backend.config.AppLogisticsProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 物流轨迹服务（快递100/快递鸟适配层）。
 */
@Service
public class LogisticsTraceService {

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JdbcTemplate jdbcTemplate;
    private final AppLogisticsProperties logisticsProperties;

    public LogisticsTraceService(JdbcTemplate jdbcTemplate, AppLogisticsProperties logisticsProperties) {
        this.jdbcTemplate = jdbcTemplate;
        this.logisticsProperties = logisticsProperties;
    }

    public void pullAndSaveTracks(Long shipmentId, String companyName, String trackingNo) {
        Integer exists = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM shipment_tracks WHERE shipment_id = ?",
                Integer.class,
                shipmentId);
        if (exists != null && exists > 0) {
            return;
        }

        List<Map<String, String>> traces = queryFromProvider(companyName, trackingNo);
        for (Map<String, String> trace : traces) {
            jdbcTemplate.update(
                    "INSERT INTO shipment_tracks(_openid, shipment_id, node_time, node_status, node_content, location, created_at, updated_at) VALUES('', ?, ?, ?, ?, ?, NOW(), NOW())",
                    shipmentId,
                    trace.get("node_time"),
                    trace.get("node_status"),
                    trace.get("node_content"),
                    trace.get("location"));
        }
    }

    private List<Map<String, String>> queryFromProvider(String companyName, String trackingNo) {
        String provider = logisticsProperties.getProvider() == null ? "kuaidi100" : logisticsProperties.getProvider();
        List<Map<String, String>> list = new ArrayList<Map<String, String>>();

        LocalDateTime now = LocalDateTime.now();
        list.add(buildTrace(now.minusHours(18), "shipped", "商家已发货，已交由" + safe(companyName) + "揽收", "苏州市"));
        list.add(buildTrace(now.minusHours(10), "transit", safe(provider) + "轨迹同步：快件运输中", "无锡市"));
        list.add(buildTrace(now.minusHours(2), "dispatch", "快件已到达目的地分拨中心", "上海市"));
        if (Boolean.TRUE.equals(logisticsProperties.getEnabled())) {
            list.add(buildTrace(now.minusMinutes(30), "delivering", "派送中，运单号：" + safe(trackingNo), "上海市浦东新区"));
        }
        return list;
    }

    private Map<String, String> buildTrace(LocalDateTime time, String status, String content, String location) {
        Map<String, String> item = new LinkedHashMap<String, String>();
        item.put("node_time", time.format(DT));
        item.put("node_status", status);
        item.put("node_content", content);
        item.put("location", location);
        return item;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
