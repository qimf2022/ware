package com.shiyu.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shiyu.backend.common.BizCode;
import com.shiyu.backend.common.BizException;
import com.shiyu.backend.config.AppRabbitMqProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 售后退款异步补偿消息生产者。
 */
@Service
public class AfterSaleCompensationProducer {

    private final RabbitTemplate rabbitTemplate;
    private final AppRabbitMqProperties rabbitMqProperties;
    private final ObjectMapper objectMapper;

    public AfterSaleCompensationProducer(RabbitTemplate rabbitTemplate,
                                         AppRabbitMqProperties rabbitMqProperties,
                                         ObjectMapper objectMapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.rabbitMqProperties = rabbitMqProperties;
        this.objectMapper = objectMapper;
    }

    public void sendRefundCompensation(Map<String, Object> payload) {
        try {
            rabbitTemplate.convertAndSend(
                    rabbitMqProperties.getExchangeOrder(),
                    "aftersale.refund.compensate",
                    objectMapper.writeValueAsString(payload));
        } catch (Exception ex) {
            throw new BizException(BizCode.SYSTEM_ERROR, "退款补偿消息发送失败");
        }
    }
}
