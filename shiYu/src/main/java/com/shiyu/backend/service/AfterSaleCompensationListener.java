package com.shiyu.backend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 售后退款异步补偿消费者。
 */
@Component
public class AfterSaleCompensationListener {

    private final MockTradeService mockTradeService;
    private final ObjectMapper objectMapper;

    public AfterSaleCompensationListener(MockTradeService mockTradeService, ObjectMapper objectMapper) {
        this.mockTradeService = mockTradeService;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = "${app.rabbit.queue-after-sale-refund-compensate:q.aftersale.refund.compensate}")
    public void onRefundCompensate(Message message, Channel channel) throws Exception {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        try {
            Map<String, Object> payload = objectMapper.readValue(body, new TypeReference<Map<String, Object>>() {
            });
            Long afterSaleId = payload.get("after_sale_id") == null ? null : Long.valueOf(String.valueOf(payload.get("after_sale_id")));
            String source = payload.get("source") == null ? "mq" : String.valueOf(payload.get("source"));
            if (afterSaleId != null) {
                mockTradeService.processAfterSaleRefundCompensation(afterSaleId, source, "异步补偿处理成功");
            }
            channel.basicAck(deliveryTag, false);
        } catch (Exception ex) {
            channel.basicNack(deliveryTag, false, false);
            throw ex;
        }
    }
}
